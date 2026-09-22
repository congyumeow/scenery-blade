package com.xingling.scenerystaff.se;

import com.xingling.scenerystaff.SceneryStaff;
import com.xingling.scenerystaff.registry.SERegistry;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.event.SlashBladeEvent;
import mods.flammpfeil.slashblade.registry.specialeffects.SpecialEffect;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/**
 * 聚合·聚爆（Polymerization）
 * <p>
 * 参照 SlashBlade Resharped 内置特效 WitherEdge 的实现方式：
 * 特效类本身仅负责注册与描述，行为通过 {@link EventBusSubscriber} 监听
 * {@link SlashBladeEvent.HitEvent} 实现，命中时检查刀上是否带有本特效。
 * 命中目标叠层，叠满 {@link #MAX_STACK} 层触发一次额外伤害（<b>不播语音</b>）。
 * <p>
 * 层数记在目标实体的持久化数据上，并附带最后一次叠层的时间戳：<b>超过 {@link #DECAY_TICKS}
 * 没有再叠层，当前层数减少 1 层</b>（每过一个 {@link #DECAY_TICKS} 再减 1，直到清零）。
 * 衰减采用「读取时结算」而不是逐实体 tick 轮询，因此没有任何常驻开销。
 */
@EventBusSubscriber(modid = SceneryStaff.MODID)
public class PolymerizationSE extends SpecialEffect {

    /** 目标身上聚合层数的 NBT 键（领域实体也共用该计数） */
    public static final String TAG_COUNT = "polymerization_count";
    /** 最后一次叠层的游戏刻（用于衰减结算与 Jade 显示剩余时间） */
    public static final String TAG_LAST = "polymerization_last_tick";
    /** 每满 {@link #MAX_STACK} 层触发的额外伤害 */
    public static final float EXPLOSION_DAMAGE = 5.0F;
    /** 触发聚爆所需的层数 */
    public static final int MAX_STACK = 10;
    /** 衰减间隔：15 秒内没有再叠层，当前层数 -1 */
    public static final int DECAY_TICKS = 15 * 20;

    public PolymerizationSE() {
        super(0); // 需求经验等级，0为无要求
    }

    @SubscribeEvent
    public static void onHit(SlashBladeEvent.HitEvent event) {
        ISlashBladeState state = event.getSlashBladeState();
        // 只有佩戴了本特效的拔刀剑命中才生效
        if (!state.hasSpecialEffect(SERegistry.POLYMERIZATION.getId())) return;

        LivingEntity target = event.getTarget();
        if (target == null || target.level().isClientSide()) return;
        LivingEntity attacker = event.getUser();

        // 伤害来源取自攻击者
        DamageSource source = attacker instanceof Player player
                ? target.damageSources().playerAttack(player)
                : target.damageSources().magic();
        addStack(target, source);
    }

    /**
     * 给目标叠一层聚合；叠满 {@link #MAX_STACK} 层时触发一次额外伤害并清零。
     * <p>
     * 侵蚀领域（{@code ErosionDomainEntity}）与本特效命中共用这一份逻辑。
     * 只应在服务端调用。
     * <p>
     * 这里<b>不播任何音效</b>：DENIA1~3 是达妮娅的语音，只应在展开侵蚀领域时触发
     * （见 {@code ErosionDomainSA}）。
     */
    public static void addStack(LivingEntity target, DamageSource explosionSource) {
        CompoundTag data = target.getPersistentData();
        int count = getEffectiveCount(target) + 1;

        if (count >= MAX_STACK) {
            data.remove(TAG_COUNT);
            data.remove(TAG_LAST);
            target.hurt(explosionSource, EXPLOSION_DAMAGE);
            return;
        }

        data.putInt(TAG_COUNT, count);
        data.putLong(TAG_LAST, target.level().getGameTime());
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
            // 旧数据没有时间戳：视为刚叠上，不做衰减
            return Math.min(count, MAX_STACK);
        }
        return decayedCount(count, entity.level().getGameTime() - last);
    }

    /**
     * 纯数值部分：给定层数与「距最后一次叠层经过了多少 tick」，算出衰减后的层数。
     * <p>
     * 规则：每满 {@link #DECAY_TICKS} 层数 -1，最多减到 0。分离成纯函数便于离线核对（不依赖实体）。
     */
    public static int decayedCount(int count, long elapsedTicks) {
        long clamped = Math.min(count, MAX_STACK);
        if (clamped <= 0L) {
            return 0;
        }
        long decayed = Math.max(0L, elapsedTicks) / DECAY_TICKS;
        return (int) Math.max(0L, clamped - decayed);
    }

    /**
     * 纯数值部分：给定经过的 tick，算出距离<b>下一次</b>衰减还剩多少 tick。
     * <p>
     * 衰减是读取时结算的，时间戳锚点不会前移，所以这里取模而不是简单相减：
     * 例如经过 310 tick 时层数已经在 300 tick 那一刻掉过一层，距下一次（600 tick）还有 290 tick。
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
