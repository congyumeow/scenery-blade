package com.xingling.scenerystaff.sa;

import com.xingling.scenerystaff.SceneryStaff;
import mods.flammpfeil.slashblade.event.SlashBladeEvent;
import mods.flammpfeil.slashblade.registry.SlashArtsRegistry;
import mods.flammpfeil.slashblade.slasharts.SlashArts;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/**
 * SA 充能门控：把「冷却到点」换成「充能攒满」，两把最终刀共用同一套逻辑。
 * <p>
 * 接管点是 {@link SlashBladeEvent.PerformSlashArtEvent} —— SlashBlade 在释放 SA 时会先算好连击状态、
 * 再发这个事件，监听方可以改掉这个状态。于是：
 * <ul>
 *   <li><b>充能已满 + 真正放出</b>：放行，同时把充能清零（这就是新的「冷却」）；</li>
 *   <li><b>充能未满 + 真正放出</b>：把连击状态换成拔刀剑内置的幻影刃
 *       （{@code slashblade:drive_horizontal}），并提示当前充能百分比。</li>
 * </ul>
 * <p>
 * 释放方式保持 SlashBlade 原生：<b>长按右键</b>蓄力，本模组没有绑定任何额外按键。
 * <p>
 * 注意「真正放出」的判定：SlashBlade 的 {@code doChargeAction} 无论蓄力够不够都会发事件，
 * 蓄力不足时 {@code type = Fail}、{@code doArts(Fail)} 返回 {@code ComboStateRegistry.NONE}，
 * 剑技根本不会生成。因此这里只在 {@code Success/Jackpot/Super} 时才做替换与提示，空点保持安静
 * ——否则玩家随手一点就会被提示，观感就是「上手就在充能中」。
 */
@EventBusSubscriber(modid = SceneryStaff.MODID)
public final class SaChargeGate {

    /** 充能未满时替换成的内置剑技：幻影刃 */
    private static final ResourceLocation PHANTOM_EDGE = SlashArtsRegistry.DRIVE_HORIZONTAL.getId();

    private SaChargeGate() {
    }

    @SubscribeEvent
    public static void onPerformSlashArt(SlashBladeEvent.PerformSlashArtEvent event) {
        LivingEntity user = event.getEntityLiving();
        if (user.level().isClientSide() || !(user instanceof Player player)) {
            return;
        }

        // 只接管本模组的 SA（按 SA 注册 ID 查充能表），其它刀/其它 SA 保持原样
        SaCharge.Charge charge = SaCharge.forSa(event.getSlashBladeState().getSlashArtsKey());
        if (charge == null) {
            return;
        }

        SlashArts.ArtsType type = event.getType();
        boolean released = type == SlashArts.ArtsType.Success
                || type == SlashArts.ArtsType.Jackpot
                || type == SlashArts.ArtsType.Super;

        if (!SaCharge.isFull(player, charge)) {
            // 充能未满：换成幻影刃并提示进度
            if (!released) {
                return;
            }
            SlashArts phantomEdge = SlashArtsRegistry.REGISTRY.get(PHANTOM_EDGE);
            if (phantomEdge != null) {
                event.setComboState(phantomEdge.doArts(type, user));
            }
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.displayClientMessage(
                        Component.translatable(charge.chargingKey(), SaCharge.percent(player, charge)), true);
            }
            return;
        }

        if (!released) {
            return;
        }

        // 充能已满且确实放出：清空充能
        SaCharge.clear(player, charge);
    }
}
