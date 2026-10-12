package com.xingling.scenerystaff.network;

import com.xingling.scenerystaff.SceneryStaff;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.util.function.Supplier;

/**
 * 本模组的网络通道。
 * <p>
 * 只为一件事存在：把服务端的 SA 充能值推给客户端，让 HUD 能显示进度。
 * 1.21 上这件事由 {@code AttachmentType.sync} 代劳；1.20.1 的能力系统不会自动同步，
 * 因此需要一条显式的同步包。
 */
public final class SyncChargeMessage {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            SceneryStaff.prefix("main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals);

    private final int domainCharge;
    private final int giantSwordCharge;

    public SyncChargeMessage(int domainCharge, int giantSwordCharge) {
        this.domainCharge = domainCharge;
        this.giantSwordCharge = giantSwordCharge;
    }

    public int domainCharge() {
        return this.domainCharge;
    }

    public int giantSwordCharge() {
        return this.giantSwordCharge;
    }

    public static void register() {
        CHANNEL.registerMessage(0, SyncChargeMessage.class,
                SyncChargeMessage::encode,
                SyncChargeMessage::decode,
                SyncChargeMessage::handle);
    }

    /** 服务端调用：把玩家当前的两条充能值同步给本人 */
    public static void syncTo(ServerPlayer player, int domainCharge, int giantSwordCharge) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new SyncChargeMessage(domainCharge, giantSwordCharge));
    }

    public static void encode(SyncChargeMessage message, FriendlyByteBuf buffer) {
        buffer.writeVarInt(message.domainCharge);
        buffer.writeVarInt(message.giantSwordCharge);
    }

    public static SyncChargeMessage decode(FriendlyByteBuf buffer) {
        return new SyncChargeMessage(buffer.readVarInt(), buffer.readVarInt());
    }

    public static void handle(SyncChargeMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            if (FMLEnvironment.dist != Dist.CLIENT) {
                return;
            }
            // 用类名字符串做一次性加载：客户端专属类不能出现在专用服务端的方法签名里
            try {
                Class.forName("com.xingling.scenerystaff.client.ChargeClientCache")
                        .getMethod("accept", int.class, int.class)
                        .invoke(null, message.domainCharge, message.giantSwordCharge);
            } catch (ReflectiveOperationException e) {
                LOGGER.warn("[SA 充能] 客户端缓存写入失败", e);
            }
        });
        context.setPacketHandled(true);
    }
}
