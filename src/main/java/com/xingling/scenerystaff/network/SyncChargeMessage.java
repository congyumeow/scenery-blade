package com.xingling.scenerystaff.network;

import com.xingling.scenerystaff.SceneryStaff;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.util.function.Supplier;

/**
 * 服务端 → 客户端：把各条 SA 充能值推给本人，HUD 靠它显示进度。
 * <p>
 * 1.21 上这件事由 {@code AttachmentType.sync} 代劳；1.20.1 的能力系统不会自动同步，
 * 因此需要一条显式的同步包。通道与注册集中在 {@link SceneryNetwork}。
 */
public final class SyncChargeMessage {

    private static final Logger LOGGER = LogUtils.getLogger();

    private final int domainCharge;
    private final int giantSwordCharge;
    private final int defierThornCharge;

    public SyncChargeMessage(int domainCharge, int giantSwordCharge, int defierThornCharge) {
        this.domainCharge = domainCharge;
        this.giantSwordCharge = giantSwordCharge;
        this.defierThornCharge = defierThornCharge;
    }

    public int domainCharge() {
        return this.domainCharge;
    }

    public int giantSwordCharge() {
        return this.giantSwordCharge;
    }

    public int defierThornCharge() {
        return this.defierThornCharge;
    }

    /** 服务端调用：把玩家当前各条充能值同步给本人 */
    public static void syncTo(ServerPlayer player, int domainCharge, int giantSwordCharge, int defierThornCharge) {
        SceneryNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new SyncChargeMessage(domainCharge, giantSwordCharge, defierThornCharge));
    }

    public static void encode(SyncChargeMessage message, FriendlyByteBuf buffer) {
        buffer.writeVarInt(message.domainCharge);
        buffer.writeVarInt(message.giantSwordCharge);
        buffer.writeVarInt(message.defierThornCharge);
    }

    public static SyncChargeMessage decode(FriendlyByteBuf buffer) {
        return new SyncChargeMessage(buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt());
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
                        .getMethod("accept", int.class, int.class, int.class)
                        .invoke(null, message.domainCharge, message.giantSwordCharge, message.defierThornCharge);
            } catch (ReflectiveOperationException e) {
                LOGGER.warn("[SA 充能] 客户端缓存写入失败", e);
            }
        });
        context.setPacketHandled(true);
    }
}
