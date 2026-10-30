package com.xingling.scenerystaff.network;

import com.xingling.scenerystaff.SceneryStaff;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * 本模组的网络通道（1.20.1 的 Forge {@code SimpleChannel}）。
 * <p>
 * 1.21 上用的是 {@code RegisterPayloadHandlersEvent} + {@code CustomPacketPayload} 那套；
 * 1.20.1 只有 SimpleChannel，因此通道本身由本类持有，各消息以自增 id 注册。
 * <p>
 * 现有消息：
 * <ol>
 *   <li>{@link SyncChargeMessage} —— 服务端 → 客户端，同步 SA 充能值（HUD 用）；</li>
 *   <li>{@link XuzhiSpaceRequestPayload} —— 客户端 → 服务端，请求进出虚质空间。</li>
 * </ol>
 */
public final class SceneryNetwork {

    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            SceneryStaff.prefix("main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals);

    private SceneryNetwork() {
    }

    /** 只允许在模组构造期调用一次 */
    public static void initialize() {
        int id = 0;
        CHANNEL.registerMessage(id++, SyncChargeMessage.class,
                SyncChargeMessage::encode,
                SyncChargeMessage::decode,
                SyncChargeMessage::handle);
        CHANNEL.registerMessage(id++, XuzhiSpaceRequestPayload.class,
                XuzhiSpaceRequestPayload::encode,
                XuzhiSpaceRequestPayload::decode,
                XuzhiSpaceRequestPayload::handle);
    }
}
