package com.xingling.scenerystaff.sa;

import com.xingling.scenerystaff.SceneryStaff;
import com.xingling.scenerystaff.registry.SARegistry;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.event.SlashBladeEvent;
import mods.flammpfeil.slashblade.registry.SlashArtsRegistry;
import mods.flammpfeil.slashblade.slasharts.SlashArts;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 侵蚀领域的冷却控制。
 * <p>
 * SlashBlade 在释放 SA 时会计算好连击状态、再发出 {@link SlashBladeEvent.PerformSlashArtEvent}，
 * 监听方可以改掉这个连击状态。于是这里做两件事：
 * <ul>
 *   <li>刀上的 SA 是侵蚀领域、且不在冷却中：放行，同时开始 {@link #COOLDOWN_TICKS} 的冷却；</li>
 *   <li>还在冷却中：把连击状态换成拔刀剑内置的幻影刃
 *       （{@code slashblade:drive_horizontal}），并提示剩余秒数。</li>
 * </ul>
 */
@EventBusSubscriber(modid = SceneryStaff.MODID)
public class ErosionDomainCooldown {

    /** 领域冷却（tick）：35 秒 */
    public static final int COOLDOWN_TICKS = 35 * 20;

    /** 冷却期间改用的内置 SA：幻影刃 */
    private static final ResourceLocation PHANTOM_EDGE = SlashArtsRegistry.DRIVE_HORIZONTAL.getId();

    /** 每个玩家的冷却结束时刻（等级游戏时间） */
    private static final Map<UUID, Long> READY_AT = new ConcurrentHashMap<>();

    @SubscribeEvent
    public static void onPerformSlashArt(SlashBladeEvent.PerformSlashArtEvent event) {
        LivingEntity user = event.getEntityLiving();
        if (user.level().isClientSide()) return;

        // 只接管本模组的侵蚀领域 SA，其它刀/其它 SA 保持原样
        ISlashBladeState state = event.getSlashBladeState();
        if (!SARegistry.EROSION_DOMAIN.getId().equals(state.getSlashArtsKey())) return;

        long now = user.level().getGameTime();
        long readyAt = READY_AT.getOrDefault(user.getUUID(), Long.MIN_VALUE);

        if (now < readyAt) {
            // 冷却中：改用内置的幻影刃
            SlashArts phantomEdge = SlashArtsRegistry.REGISTRY.get(PHANTOM_EDGE);
            if (phantomEdge != null) {
                SlashArts.ArtsType type = event.getType();
                event.setComboState(phantomEdge.doArts(type == null ? SlashArts.ArtsType.Success : type, user));
            }
            if (user instanceof ServerPlayer player) {
                long remainSeconds = (readyAt - now + 19L) / 20L;
                player.displayClientMessage(
                        Component.translatable("message.scenerystaff.domain_cooldown", remainSeconds), true);
            }
            return;
        }

        READY_AT.put(user.getUUID(), now + COOLDOWN_TICKS);
    }
}
