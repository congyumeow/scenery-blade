package com.xingling.scenerystaff.client;

import com.xingling.scenerystaff.SceneryStaff;
import com.xingling.scenerystaff.client.renderer.entity.ErosionDomainRenderer;
import com.xingling.scenerystaff.registry.EntityRegistry;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * 客户端注册事件：为侵蚀领域实体注册渲染器（否则实体生成时客户端会崩溃）。
 * 参照 SlashBlade Resharped 的 ClientHandler 写法，value = Dist.CLIENT
 * 保证仅客户端加载，事件按类型自动路由到 mod 事件总线。
 */
@EventBusSubscriber(modid = SceneryStaff.MODID, value = Dist.CLIENT)
public class ClientRegistryEvents {

    @SubscribeEvent
    public static void onRegisterRenderers(final EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(EntityRegistry.EROSION_DOMAIN.get(), ErosionDomainRenderer::new);
    }
}
