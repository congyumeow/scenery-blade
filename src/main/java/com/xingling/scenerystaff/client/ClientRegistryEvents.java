package com.xingling.scenerystaff.client;

import com.xingling.scenerystaff.SceneryStaff;
import com.xingling.scenerystaff.client.renderer.entity.ErosionDomainRenderer;
import com.xingling.scenerystaff.client.renderer.entity.GiantSwordRenderer;
import com.xingling.scenerystaff.registry.EntityRegistry;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 客户端注册事件：为侵蚀领域与巨剑实体注册渲染器（否则实体生成时客户端会崩溃）。
 * <p>
 * 1.20.1 上事件订阅注解仍是 {@code @Mod.EventBusSubscriber}（1.21 才改成 {@code @EventBusSubscriber}），
 * {@code value = Dist.CLIENT} 保证仅客户端加载。
 */
@Mod.EventBusSubscriber(modid = SceneryStaff.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClientRegistryEvents {

    @SubscribeEvent
    public static void onRegisterRenderers(final EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(EntityRegistry.EROSION_DOMAIN.get(), ErosionDomainRenderer::new);
        event.registerEntityRenderer(EntityRegistry.GIANT_SWORD.get(), GiantSwordRenderer::new);
    }
}
