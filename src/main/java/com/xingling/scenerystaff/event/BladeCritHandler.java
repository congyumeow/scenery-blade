package com.xingling.scenerystaff.event;

import com.xingling.scenerystaff.SceneryStaff;
import com.xingling.scenerystaff.item.SceneryBlades;
import com.xingling.scenerystaff.item.WavebandUpgrade;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * 本模组拔刀剑的暴击。
 * <p>
 * 暴击率与暴击伤害来自手上那把刀的强化等级（见 {@link WavebandUpgrade}）：
 * 未强化 20% / 150%，满级（6 次）80% / 280%，线性增长。
 * 强化等级存在刀上，所以哪条回音频段强化的都一样——达妮娅的强化布景之杖、爱弥斯的强化启明星，
 * 两者共用同一套数值。
 * <p>
 * <b>只有手持两把终盘刀（布景之杖 / 启明星）时才触发</b>：暴击是这两把刀自身的特性，
 * 不会外溢到别的武器，也不作用于下位刀「残景之杖」。
 * <p>
 * 实现走 {@link LivingIncomingDamageEvent}（可以改伤害数值），
 * 判定通过后直接把这一发伤害乘上暴击倍率，并补上原版暴击的粒子与音效。
 */
@EventBusSubscriber(modid = SceneryStaff.MODID)
public final class BladeCritHandler {

    private BladeCritHandler() {
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        if (!(event.getSource().getEntity() instanceof Player player)) {
            return;
        }

        ItemStack held = player.getMainHandItem();
        if (!SceneryBlades.hasCrit(held)) {
            return;
        }

        int level = WavebandUpgrade.getLevel(held);
        if (player.getRandom().nextDouble() >= WavebandUpgrade.critChanceFor(level)) {
            return;
        }

        event.setAmount((float) (event.getAmount() * WavebandUpgrade.critDamageFor(level)));

        // 原版暴击的视觉反馈，让玩家能看出这一下是暴击
        if (event.getEntity().level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.CRIT,
                    event.getEntity().getX(),
                    event.getEntity().getY() + event.getEntity().getBbHeight() * 0.5D,
                    event.getEntity().getZ(),
                    6, 0.3D, 0.3D, 0.3D, 0.15D);
            serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 0.8F, 1.2F);
        }
    }
}
