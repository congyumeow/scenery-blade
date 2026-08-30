package com.xingling.scenerystaff.se;

import com.xingling.scenerystaff.SceneryStaff;
import com.xingling.scenerystaff.registry.SERegistry;
import com.xingling.scenerystaff.registry.SoundRegistry;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.event.SlashBladeEvent;
import mods.flammpfeil.slashblade.registry.specialeffects.SpecialEffect;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
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
 * 命中目标叠层，叠满 10 层触发一次额外伤害并播放音效。
 */
@EventBusSubscriber(modid = SceneryStaff.MODID)
public class PolymerizationSE extends SpecialEffect {

    /** 目标身上聚合层数的 NBT 键（领域实体也共用该计数） */
    public static final String TAG_COUNT = "polymerization_count";
    /** 每满 10 层触发的额外伤害 */
    public static final float EXPLOSION_DAMAGE = 5.0F;
    /** 触发聚爆所需的层数 */
    private static final int MAX_STACK = 10;

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

        // 获取当前层数并累加
        int count = target.getPersistentData().getInt(TAG_COUNT);
        count++;
        target.getPersistentData().putInt(TAG_COUNT, count);

        // 每10层触发额外伤害
        if (count >= MAX_STACK) {
            target.getPersistentData().putInt(TAG_COUNT, 0); // 重置
            // 伤害来源取自攻击者
            if (attacker instanceof Player player) {
                target.hurt(player.damageSources().playerAttack(player), EXPLOSION_DAMAGE);
            } else {
                target.hurt(attacker.damageSources().magic(), EXPLOSION_DAMAGE);
            }
            // 播放音效
            if (target.level() instanceof ServerLevel serverLevel) {
                serverLevel.playSound(null, target.getX(), target.getY(), target.getZ(),
                        SoundRegistry.DENIA1.get(), target.getSoundSource(), 1.0F, 1.0F);
            }
        }
    }

    @Override
    public Component getDescription() {
        return Component.translatable(this.getDescriptionId()).withStyle(style -> style.withColor(0xFFAA00));
    }
}
