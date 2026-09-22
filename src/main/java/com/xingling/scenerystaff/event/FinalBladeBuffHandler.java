package com.xingling.scenerystaff.event;

import com.xingling.scenerystaff.SceneryStaff;
import com.xingling.scenerystaff.item.SceneryBlades;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = SceneryStaff.MODID)
public class FinalBladeBuffHandler {

    /** 力量 III：等级 = 放大器 + 1 */
    private static final int STRENGTH_AMPLIFIER = 2;

    /** 生命回复 II：等级 = 放大器 + 1 */
    private static final int REGENERATION_AMPLIFIER = 1;

    /** 单次施加的效果时长（tick）：2 秒。收刀后最多 2 秒内消失 */
    private static final int DURATION_TICKS = 40;

    /** 剩余时长低于该值才重新施加（tick）：1 秒 */
    private static final int REFRESH_BELOW_TICKS = 20;

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) {
            return;
        }
        if (player.isSpectator()) {
            return;
        }
        if (!isHoldingFinalBlade(player)) {
            return;
        }

        keepEffect(player, MobEffects.DAMAGE_BOOST, STRENGTH_AMPLIFIER);
        keepEffect(player, MobEffects.REGENERATION, REGENERATION_AMPLIFIER);
    }

    /** 主手或副手拿着最终刀都算「持有」 */
    private static boolean isHoldingFinalBlade(Player player) {
        return SceneryBlades.isFinalBlade(player.getMainHandItem())
                || SceneryBlades.isFinalBlade(player.getOffhandItem());
    }

    /**
     * 维持一个效果：没有、快过期、或者等级不够时补一次。
     * <p>
     * {@code ambient=false, visible=false, showIcon=true}：不冒药水粒子，但右上角保留图标，
     * 玩家能看出增益还在。
     */
    private static void keepEffect(Player player, Holder<MobEffect> effect, int amplifier) {
        MobEffectInstance current = player.getEffect(effect);
        if (current != null) {
            // 玩家自己有更强的同类效果：不覆盖
            if (current.getAmplifier() > amplifier) {
                return;
            }
            // 同类同等级且剩余时间还够：不重复施加，省掉无谓的同步包
            if (current.getAmplifier() == amplifier && current.getDuration() > REFRESH_BELOW_TICKS) {
                return;
            }
        }
        player.addEffect(new MobEffectInstance(effect, DURATION_TICKS, amplifier, false, false, true));
    }
}
