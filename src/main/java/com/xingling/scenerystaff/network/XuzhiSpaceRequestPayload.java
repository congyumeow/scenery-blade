package com.xingling.scenerystaff.network;

import com.xingling.scenerystaff.SceneryStaff;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * 客户端 → 服务端：请求进入或离开虚质空间。
 * <p>
 * 空负载（不需要携带任何数据）——服务端一律以玩家当前状态自行判定方向与落点，
 * 客户端只是「按了一下键」这个事实的传递者。
 */
public record XuzhiSpaceRequestPayload() implements CustomPacketPayload {

    public static final Type<XuzhiSpaceRequestPayload> TYPE =
            new Type<>(SceneryStaff.prefix("xuzhi_space_request"));

    public static final StreamCodec<RegistryFriendlyByteBuf, XuzhiSpaceRequestPayload> CODEC =
            StreamCodec.unit(new XuzhiSpaceRequestPayload());

    @Override
    public Type<XuzhiSpaceRequestPayload> type() {
        return TYPE;
    }
}
