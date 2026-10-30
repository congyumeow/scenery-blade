package com.xingling.scenerystaff.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 客户端 → 服务端：请求进入或离开虚质空间。
 * <p>
 * 空负载（不需要携带任何数据）——服务端一律以玩家当前状态自行判定方向与落点，
 * 客户端只是「按了一下键」这个事实的传递者。
 * <p>
 * 1.20.1 的网络栈是 Forge 的 {@code SimpleChannel}（没有 1.21 的 {@code CustomPacketPayload}
 * 与 {@code StreamCodec}），因此这里退化成一对静态的 {@link #encode} / {@link #decode}。
 */
public record XuzhiSpaceRequestPayload() {

    /** 空负载，不写任何字段 */
    public static void encode(XuzhiSpaceRequestPayload message, FriendlyByteBuf buffer) {
        // 无负载
    }

    public static XuzhiSpaceRequestPayload decode(FriendlyByteBuf buffer) {
        return new XuzhiSpaceRequestPayload();
    }

    /** 服务端处理：切回服务端主线程再传送 */
    public static void handle(XuzhiSpaceRequestPayload message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            net.minecraft.server.level.ServerPlayer player = context.getSender();
            if (player != null) {
                com.xingling.scenerystaff.dimension.XuzhiSpace.toggle(player);
            }
        });
        context.setPacketHandled(true);
    }
}
