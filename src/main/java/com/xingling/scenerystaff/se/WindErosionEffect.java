package com.xingling.scenerystaff.se;

import com.xingling.scenerystaff.blade.SceneryBlades;
import com.xingling.scenerystaff.registry.MobEffectRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * 风蚀：由「不屈命定之冠」造成伤害时叠加的持续伤害效果。
 * <p>
 * <b>为什么是真正的 MobEffect</b>：需求里的「每 3 秒结算一次伤害、每 15 秒掉一层」需要
 * <b>随实体持续计时</b>。用 {@link MobEffect} 由引擎驱动，既省掉自建的 tick 扫描，
 * 又天然获得状态图标显示；层数直接借用 effect 的 amplifier（amplifier 0 = 1 层）。
 * <p>
 * <b>时长设为无限</b>：原版效果到期只会整个消失，做不到「只掉一层」。因此这里挂一个
 * 无限时长的实例，衰减在 tick 里手动处理，归零时再移除。
 * <p>
 * <b>为什么不直接把无限时长当成问题</b>：{@code MobEffectInstance#tick} 对无限时长效果
 * 传给 {@code isDurationEffectTick} 的是 {@code entity.tickCount}（不是 -1），
 * 因此 {@code tickCount % 20 == 0} 每秒都会成立，调度是正常的。
 * <p>
 * <b>降级必须「先移除再添加」</b>：{@code MobEffectInstance#update} 合并同名效果时只保留
 * <b>更高的 amplifier</b>，直接 {@code addEffect(更低的 amplifier)} 会被旧的高层数覆盖，
 * 表现为「衰减计时到了但层数不掉」。所以下方 {@link #setStacks} 一律先 remove 再 add。
 */
public class WindErosionEffect extends MobEffect {

    /** 默认层数上限 */
    public static final int DEFAULT_MAX_STACKS = 3;
    /** 手持不屈命定之冠时的层数上限 */
    public static final int CARTETHYIA_MAX_STACKS = 6;

    /** 伤害结算间隔（tick）：3 秒 */
    public static final int SETTLE_INTERVAL_TICKS = 3 * 20;
    /** 衰减间隔（tick）：15 秒掉一层 */
    public static final int DECAY_INTERVAL_TICKS = 15 * 20;

    /**
     * 伤害倍率的锚点层数：<b>风蚀 3 层 = 聚爆 10 层 = 1.0 倍</b>。
     * <p>
     * 两个系统的层数量级不同（聚爆 10/13 层，风蚀最多 6 层），若直接套用聚爆的公式，
     * 风蚀的倍率会一直落在 0.10~0.40，几乎打不出伤害。因此这里单独设一个锚点：
     * 风蚀 {@code 2 ^ ((层数 - 3) / 3)}——3 层为 1.0 倍，6 层正好 2.0 倍，
     * 与「聚爆 10 层为 1.0、13 层为 2.0」完全对等。
     */
    public static final int ANCHOR_STACK = 3;

    /**
     * 中招者身上记录的「施术者 UUID」。
     * <p>
     * 风蚀是挂在中招者身上的持续效果，效果本身不携带施术者信息，而伤害必须按<b>施术者</b>
     * （也就是玩家）的生命与护甲计算——因此叠层时把攻击者 UUID 写在目标身上，结算时再取回来。
     */
    public static final String KEY_CASTER = "scenerystaff_wind_erosion_caster";

    /**
     * 中招者身上记录的「当前生效的层数上限」。
     * <p>
     * 上限不是固定值（默认 3、持有不屈命定之冠时 6），而 Jade 的 tooltip 需要一个分母。
     * 该值每次 tick 都会按施术者<b>当前</b>手持的刀重新计算并写回，所以换刀后分母会跟着变。
     */
    public static final String KEY_CAP = "scenerystaff_wind_erosion_cap";

    public WindErosionEffect() {
        // 有害效果，颜色取偏青的风感色
        super(MobEffectCategory.HARMFUL, 0x7FE0D8);
    }

    /** 施术者当前该遵守的层数上限：手持不屈命定之冠 6 层，否则 3 层 */
    public static int capFor(Player caster) {
        return caster != null && SceneryBlades.isCartethyia(caster.getMainHandItem())
                ? CARTETHYIA_MAX_STACKS
                : DEFAULT_MAX_STACKS;
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        // 无限时长效果收到的是 entity.tickCount，因此每秒驱动一次
        return duration % 20 == 0;
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide()) {
            return;
        }

        Player caster = resolveCaster(entity);
        int cap = resolveCap(entity, caster);
        entity.getPersistentData().putInt(KEY_CAP, cap);

        // 层数超过当前上限（例如换下了不屈命定之冠）：削到上限，本 tick 不再结算
        if (amplifier > cap - 1) {
            setStacks(entity, cap - 1);
            return;
        }

        int stacks = amplifier + 1;
        int tick = entity.tickCount;

        // 每 3 秒结算一次风蚀伤害（不消耗层数）
        if (tick % SETTLE_INTERVAL_TICKS == 0) {
            // 伤害按【施术者】的生命与护甲计算（与聚爆一致），而不是中招者的属性。
            // 施术者下线或 UUID 失效时退回用中招者自身属性，保证效果仍能继续结算。
            float maxHealth = caster != null ? caster.getMaxHealth() : entity.getMaxHealth();
            float armorValue = caster != null ? caster.getArmorValue() : entity.getArmorValue();

            // 公式与聚爆同源，但锚点单独取 ANCHOR_STACK：
            // 风蚀 3 层 = 聚爆 10 层 = 1.0 倍，两者翻倍步长相同（每 3 层一次）。
            float base = PolymerizationSE.baseExplosionDamage(maxHealth, armorValue);
            double multiplier = Math.pow(2.0,
                    (stacks - ANCHOR_STACK) / (double) PolymerizationSE.DOUBLING_LAYERS);
            float damage = (float) (base * multiplier);
            if (damage > 0.0F) {
                entity.hurt(entity.damageSources().magic(), damage);
            }
        }

        // 每 15 秒衰减一层；归零则移除效果
        if (tick % DECAY_INTERVAL_TICKS == 0) {
            setStacks(entity, amplifier - 1);
        }
        return;
    }

    /**
     * 直接把层数设为指定 amplifier。
     * <p>
     * <b>必须先移除再添加</b>：同名效果合并时 {@code MobEffectInstance#update} 只保留更高的
     * amplifier，直接 {@code addEffect} 一个更低的层数不会生效。{@code amplifier < 0} 表示移除。
     */
    public static void setStacks(LivingEntity entity, int amplifier) {
        entity.removeEffect(MobEffectRegistry.WIND_EROSION.get());
        if (amplifier >= 0) {
            entity.addEffect(new MobEffectInstance(MobEffectRegistry.WIND_EROSION.get(), -1, amplifier,
                    false, false, true));
        }
    }

    /** 当前生效的上限：施术者在线时按其手持物现算，否则沿用最后一次记录的值 */
    private static int resolveCap(LivingEntity entity, Player caster) {
        if (caster != null) {
            return capFor(caster);
        }
        CompoundTag data = entity.getPersistentData();
        return data.contains(KEY_CAP) ? data.getInt(KEY_CAP) : CARTETHYIA_MAX_STACKS;
    }

    /** 从目标身上取回施术者；取不到（下线、跨维度、数据丢失）时返回 {@code null} */
    private static Player resolveCaster(LivingEntity target) {
        CompoundTag data = target.getPersistentData();
        if (!data.hasUUID(KEY_CASTER)) {
            return null;
        }
        if (target.level() instanceof ServerLevel serverLevel) {
            return serverLevel.getPlayerByUUID(data.getUUID(KEY_CASTER));
        }
        return null;
    }
}
