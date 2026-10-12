package com.xingling.scenerystaff.event;

import com.xingling.scenerystaff.SceneryStaff;
import com.xingling.scenerystaff.blade.SceneryBlades;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 本模组拔刀剑的暴击。
 * <p>
 * 暴击率与暴击伤害来自手上那把刀的强化等级（见 {@link com.xingling.scenerystaff.blade.WavebandUpgrade}）：
 * 未强化 20% / 150%，满级（6 次）80% / 280%，线性增长。
 * 强化等级存在刀上，所以哪条回音频段强化的都一样。
 * <p>
 * <b>只有手持两把终盘刀（布景之杖 / 启明星）时才触发</b>：暴击是这两把刀自身的特性，
 * 不会外溢到别的武器，也不作用于下位刀「残景之杖」。
 * <p>
 * 实现走 {@link LivingHurtEvent}（可以改伤害数值）——1.21 上对应的是
 * {@code LivingIncomingDamageEvent}，1.20.1 上没有那个事件；
 * 判定通过后直接把这一发伤害乘上暴击倍率，并补上原版暴击的粒子与音效。
 */
@Mod.EventBusSubscriber(modid = SceneryStaff.MODID)
public final class BladeCritHandler {

    private BladeCritHandler() {
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
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

        // 一次取面板：判定与倍率同源，不会出现「按 A 等级判定、按 B 等级结算」
        com.xingling.scenerystaff.blade.WavebandUpgrade.CritStats crit =
                com.xingling.scenerystaff.blade.WavebandUpgrade.critOf(held);
        if (player.getRandom().nextDouble() >= crit.chance()) {
            return;
        }

        event.setAmount((float) (event.getAmount() * crit.damage()));

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
