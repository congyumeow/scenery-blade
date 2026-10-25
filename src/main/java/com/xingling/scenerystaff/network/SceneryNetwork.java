package com.xingling.scenerystaff.network;

import com.xingling.scenerystaff.SceneryStaff;
import com.xingling.scenerystaff.dimension.XuzhiSpace;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 本模组的网络通道。
 * <p>
 * 目前只有一条：按键请求进出虚质空间。服务端的处理逻辑全部在
 * {@link XuzhiSpace#toggle}，这里只负责解码与切到服务端线程执行。
 */
@EventBusSubscriber(modid = SceneryStaff.MODID)
public final class SceneryNetwork {

    private SceneryNetwork() {
    }

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(
                XuzhiSpaceRequestPayload.TYPE,
                XuzhiSpaceRequestPayload.CODEC,
                SceneryNetwork::handleXuzhiSpaceRequest);
    }

    private static void handleXuzhiSpaceRequest(XuzhiSpaceRequestPayload payload, IPayloadContext context) {
        // enqueueWork：从网络线程切回服务端主线程，传送与玩家状态读写都必须在主线程
        context.enqueueWork(() -> {
            if (context.player() instanceof net.minecraft.server.level.ServerPlayer player) {
                XuzhiSpace.toggle(player);
            }
        });
    }
}
