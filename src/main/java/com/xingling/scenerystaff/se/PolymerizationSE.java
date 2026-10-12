package com.xingling.scenerystaff.se;

import com.xingling.scenerystaff.SceneryStaff;
import com.xingling.scenerystaff.blade.SceneryBlades;
import com.xingling.scenerystaff.registry.SERegistry;
import mods.flammpfeil.slashblade.event.SlashBladeEvent;
import mods.flammpfeil.slashblade.registry.specialeffects.SpecialEffect;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 聚合·聚爆（Polymerization）
 * <p>
 * 参照 SlashBlade Resharped 内置特效 WitherEdge 的实现方式：
 * 特效类本身仅负责注册与描述，行为通过 {@code @Mod.EventBusSubscriber} 监听
 * {@link SlashBladeEvent.HitEvent} 实现，命中时检查刀上是否带有本特效。
 * <p>
 * <b>引爆条件</b>：层数达到 {@link #triggerStack(LivingEntity)} 即引爆，<b>并清除层数</b>，
 * 下一轮重新从 1 层叠起。默认是叠满 {@link #MAX_STACK}（10）层；手持「启明星」时提前到第
 * {@link #QIMINGXING_TRIGGER_STACK} + 1 层（也就是层数大于 5）。
 * <p>
 * <b>伤害结算</b>：聚爆伤害<b>与施术者的最大生命值和当前护甲值相关</b>，
 * 且结算层数取的是<b>层数上限</b>而不是引爆时的实际层数——所以提前引爆不会削弱伤害。
 * 倍率以 {@link #ANCHOR_STACK} 层为 1.0，每多 {@link #DOUBLING_LAYERS} 层翻一倍。
 * <p>
 * <b>无视护甲</b>：聚爆走原版 {@code minecraft:magic} 伤害类型。
 * <p>
 * 层数记在目标实体的持久化数据上，并附带最后一次叠层的时间戳：<b>超过 {@link #DECAY_TICKS}
 * 没有再叠层，当前层数减少 1 层</b>。衰减采用「读取时结算」而不是逐实体 tick 轮询，因此没有任何常驻开销。
 */
@Mod.EventBusSubscriber(modid = SceneryStaff.MODID)
public class PolymerizationSE extends SpecialEffect {

    /** 目标身上聚合层数的 NBT 键（领域实体也共用该计数） */
    public static final String TAG_COUNT = "polymerization_count";
    /** 最后一次叠层的游戏刻（用于衰减结算与 Jade 显示剩余时间） */
    public static final String TAG_LAST = "polymerization_last_tick";
    /** 最近一次叠层时生效的层数上限，供 Jade 显示做分母 */
    public static final String TAG_CAP = "polymerization_cap";

    /** 默认的层数上限（用于伤害结算） */
    public static final int MAX_STACK = 10;
    /** 手持「启明星」时的层数上限 */
    public static final int QIMINGXING_MAX_STACK = 13;
    /** 手持「启明星」时：层数<b>大于</b>该值（也就是叠到第 6 层）即引爆 */
    public static final int QIMINGXING_TRIGGER_STACK = 5;
    /** 伤害锚点：结算层数等于该值时，聚爆伤害就是基础伤害本身 */
    public static final int ANCHOR_STACK = 10;
    /** 结算层数每比锚点多该值层数，伤害翻一倍（于是结算层数 13 正好是 10 的 2 倍） */
    public static final float DOUBLING_LAYERS = 3.0F;
    /** 基础聚爆伤害 = 施术者最大生命值 × 该系数 + 施术者护甲值 × {@link #ARMOR_FACTOR} */
    public static final float HEALTH_FACTOR = 0.25F;
    /** 护甲项系数：护甲值每 2 点折算 1 点基础伤害 */
    public static final float ARMOR_FACTOR = 0.5F;
    /** 拿不到施术者时的兜底基础伤害 */
    public static final float FALLBACK_BASE_DAMAGE = 10.0F;
    /** 衰减间隔：15 秒内没有再叠层，当前层数 -1 */
    public static final int DECAY_TICKS = 15 * 20;

    public PolymerizationSE() {
        super(0); // 需求经验等级，0为无要求
    }

    @SubscribeEvent
    public static void onHit(SlashBladeEvent.HitEvent event) {
        // 两种来源都算「带本特效」：
        //  1) 刀状态里登记了本特效（定义里带 SE 的刀，例如布景之杖）；
        //  2) 攻击用的就是启明星——直接按刀本体判定。
        // 之所以要看 (2)：刀的特效列表是<b>物品栈上缓存的状态</b>，只在刀生成时从命名刀定义解析一次。
        // 旧存档里已经存在的启明星，其状态是「定义加上这个 SE 之前」解析出来的，
        // 特效列表里不会有本特效；只认状态的话它永远叠不上层。
        boolean byState = event.getSlashBladeState().hasSpecialEffect(SERegistry.POLYMERIZATION.getId());
        boolean byBlade = SceneryBlades.isQimingxing(event.getBlade());
        if (!byState && !byBlade) return;

        LivingEntity target = event.getTarget();
        if (target == null || target.level().isClientSide()) return;
        LivingEntity attacker = event.getUser();

        // 聚爆伤害无视护甲：用原版 magic 伤害类型（属于 #minecraft:bypasses_armor），
        // 同时把攻击者作为来源实体挂上，击杀归属与统计仍然记在攻击者头上。
        // 1.20.1 上 DamageSources#source(ResourceKey, Entity) 不存在，等价写法是 magic() 之后
        // 用自定义来源——这里直接用 target 的 magic 来源并交由调用方归属（见 addStack 的 attacker 参数）。
        DamageSource source = attacker != null
                ? attacker.damageSources().magic()
                : target.damageSources().magic();
        addStack(target, source, attacker);
    }

    /**
     * 当前生效的层数上限。
     * <p>
     * <b>这里是「提高上限」的唯一接缝</b>：默认 {@link #MAX_STACK}，手持「启明星」时为
     * {@link #QIMINGXING_MAX_STACK}。后续再加入别的途径时也只改这一个方法，
     * 伤害结算（{@link #explosionDamage(LivingEntity)}）会自动跟着变。
     */
    public static int maxStack(LivingEntity attacker) {
        return SceneryBlades.isHolding(attacker, SceneryBlades::isQimingxing) ? QIMINGXING_MAX_STACK : MAX_STACK;
    }

    /**
     * 当前生效的引爆层数：层数<b>达到</b>该值即引爆。
     * <p>
     * 默认与层数上限一致（{@link #MAX_STACK}，即叠满 10 层引爆）；
     * 手持「启明星」时为 {@link #QIMINGXING_TRIGGER_STACK} + 1 —— 也就是层数大于 5（叠到第 6 层）就引爆。
     * 注意引爆层数与伤害结算层数是两件事：结算始终按 {@link #maxStack(LivingEntity)} 算。
     */
    public static int triggerStack(LivingEntity attacker) {
        return SceneryBlades.isHolding(attacker, SceneryBlades::isQimingxing) ? QIMINGXING_TRIGGER_STACK + 1 : MAX_STACK;
    }

    /**
     * 给目标叠一层聚合；层数<b>达到 {@link #triggerStack(LivingEntity)}</b> 时触发一次聚爆<b>并清除层数</b>。
     * <p>
     * 侵蚀领域（{@code ErosionDomainEntity}）与本特效命中共用这一份逻辑。
     * 只应在服务端调用。
     * <p>
     * 这里<b>不播任何音效</b>：DENIA1~3 是达妮娅的语音，只应在展开侵蚀领域时触发。
     *
     * @param attacker 施术者：决定层数上限与聚爆伤害（最大生命值 / 当前护甲值）；允许为 {@code null}
     */
    public static void addStack(LivingEntity target, DamageSource explosionSource, LivingEntity attacker) {
        CompoundTag data = target.getPersistentData();
        int cap = maxStack(attacker);
        int count = Math.min(getEffectiveCount(target) + 1, cap);

        if (count >= triggerStack(attacker)) {
            // 引爆：清除层数，下一轮重新从 1 层叠起。
            // 伤害按「层数上限」结算，所以提前引爆不会削弱伤害（启明星就是这种情况）。
            data.remove(TAG_COUNT);
            data.remove(TAG_LAST);
            target.hurt(explosionSource, explosionDamage(attacker));
            return;
        }

        data.putInt(TAG_COUNT, count);
        data.putInt(TAG_CAP, cap);
        data.putLong(TAG_LAST, target.level().getGameTime());
    }

    // ------------------------------------------------------------------ 聚爆伤害（纯数值，便于离线核对）

    /**
     * 纯数值：基础聚爆伤害 = 最大生命值 × {@link #HEALTH_FACTOR} + 护甲值 × {@link #ARMOR_FACTOR}。
     * <p>
     * 护甲取的是<b>当前</b>护甲值（{@code LivingEntity#getArmorValue()}）：
     * 没有穿任何提供护甲的装备时就是 0，此时只剩生命值那一项。
     * 系数取默认值时：20 血 / 10 甲 = 10.0，20 血 / 20 甲 = 15.0，20 血 / 0 甲 = 5.0。
     * <p>
     * 最后钳到 {@code >= 0}：护甲值可以被效果压成负数，若放任下去可能算出负伤害。
     */
    public static float baseExplosionDamage(float maxHealth, float armorValue) {
        return Math.max(0.0F, maxHealth * HEALTH_FACTOR + armorValue * ARMOR_FACTOR);
    }

    /** 施术者视角的基础聚爆伤害；施术者为 {@code null} 时用 {@link #FALLBACK_BASE_DAMAGE} */
    public static float baseExplosionDamage(LivingEntity attacker) {
        return attacker == null
                ? FALLBACK_BASE_DAMAGE
                : baseExplosionDamage(attacker.getMaxHealth(), attacker.getArmorValue());
    }

    /**
     * 纯数值：结算层数对应的伤害倍率。
     * <p>
     * 锚点 {@link #ANCHOR_STACK} 层为 {@code 1.0}，每多 {@link #DOUBLING_LAYERS} 层翻一倍——
     * 于是结算层数 13 时正好是 10 的 <b>2 倍</b>（16 层 4 倍、19 层 8 倍……）。
     */
    public static float layerMultiplier(int layers) {
        return (float) Math.pow(2.0, (layers - ANCHOR_STACK) / (double) DOUBLING_LAYERS);
    }

    /** 纯数值：给定结算层数与施术者属性，算出这一发聚爆的伤害 = 基础伤害 × 层数倍率 */
    public static float explosionDamage(int layers, float maxHealth, float armorValue) {
        return baseExplosionDamage(maxHealth, armorValue) * layerMultiplier(layers);
    }

    /**
     * 施术者视角：这一发聚爆的实际伤害。
     * <p>
     * <b>结算层数取的是当前层数上限</b>（{@link #maxStack(LivingEntity)}），不是引爆时的实际层数。
     */
    public static float explosionDamage(LivingEntity attacker) {
        return baseExplosionDamage(attacker) * layerMultiplier(maxStack(attacker));
    }

    /**
     * 读取目标当前<b>有效</b>层数：先按「每 {@link #DECAY_TICKS} 未叠层则 -1」结算衰减，不写回数据。
     * <p>
     * 采用读取时结算而不是常驻 tick 轮询，代价只发生在真正需要这个数值的地方
     * （叠层时、Jade 悬停请求数据时）。
     */
    public static int getEffectiveCount(LivingEntity entity) {
        CompoundTag data = entity.getPersistentData();
        int count = data.getInt(TAG_COUNT);
        if (count <= 0) {
            return 0;
        }
        long last = data.getLong(TAG_LAST);
        if (last <= 0L) {
            // 旧数据没有时间戳：视为刚叠上，不做衰减（上限在叠层时施加，这里不截断）
            return count;
        }
        return decayedCount(count, entity.level().getGameTime() - last);
    }

    /**
     * 纯数值部分：给定层数与「距最后一次叠层经过了多少 tick」，算出衰减后的层数。
     * <p>
     * 规则：每满 {@link #DECAY_TICKS} 层数 -1，最多减到 0。
     * <p>
     * <b>这里刻意不按 {@link #MAX_STACK} 钳制</b>：上限是在叠层时按 {@link #maxStack(LivingEntity)}
     * 施加的，存储层保持原值——日后加入提高上限的途径时，才不会被这里的默认上限截断。
     */
    public static int decayedCount(int count, long elapsedTicks) {
        if (count <= 0) {
            return 0;
        }
        long decayed = Math.max(0L, elapsedTicks) / DECAY_TICKS;
        return (int) Math.max(0L, (long) count - decayed);
    }

    /**
     * 纯数值部分：给定经过的 tick，算出距离<b>下一次</b>衰减还剩多少 tick。
     * <p>
     * 衰减是读取时结算的，时间戳锚点不会前移，所以这里取模而不是简单相减。
     * 返回值恒在 {@code (0, DECAY_TICKS]} 区间内。
     */
    public static long ticksUntilDecay(long elapsedTicks) {
        long elapsed = Math.max(0L, elapsedTicks);
        return DECAY_TICKS - (elapsed % DECAY_TICKS);
    }

    /** 最后一次叠层的游戏刻；没有记录时返回当前时刻（即「刚刚叠过」） */
    public static long getLastStackTick(LivingEntity entity) {
        long last = entity.getPersistentData().getLong(TAG_LAST);
        return last > 0L ? last : entity.level().getGameTime();
    }

    /** 距离下一次衰减还剩多少 tick（0 ~ {@link #DECAY_TICKS}） */
    public static long getTicksUntilDecay(LivingEntity entity) {
        return ticksUntilDecay(entity.level().getGameTime() - getLastStackTick(entity));
    }

    @Override
    public Component getDescription() {
        return Component.translatable(this.getDescriptionId()).withStyle(style -> style.withColor(0xFFAA00));
    }
}
