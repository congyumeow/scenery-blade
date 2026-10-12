package com.xingling.scenerystaff.sa;

import com.xingling.scenerystaff.SceneryStaff;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;

/**
 * SA 充能的玩家能力（1.21 的 {@code AttachmentType} 在 1.20.1 上的替代方案）。
 * <p>
 * 1.20.1 既没有 {@code AttachmentType}，也没有「给玩家挂自动持久化数据」的通用入口，
 * 因此用 Forge 的能力系统自己实现：{@link ICapabilitySerializable} 让数据随玩家存档
 * 自动读写，挂在玩家实体上天然多人隔离。
 * <p>
 * 数据本体是两条整数充能（蚀域 / 巨剑），分别对应两把终盘刀。
 * 客户端读到的值来自服务端同步 —— 其入口是 {@code SaChargeSync}，服务端每次改动后推一次。
 */
public final class PlayerChargeCapability {

    /** 能力注册名 */
    public static final ResourceLocation ID = SceneryStaff.prefix("sa_charge");

    public static final Capability<ChargeData> CAPABILITY =
            CapabilityManager.get(new CapabilityToken<ChargeData>() {
            });

    private static final String TAG_DOMAIN = "DomainCharge";
    private static final String TAG_GIANT_SWORD = "GiantSwordCharge";

    private PlayerChargeCapability() {
    }

    /** 取玩家身上的充能数据；没有（非玩家 / 尚未挂上）时返回 {@link LazyOptional#empty()} */
    public static LazyOptional<ChargeData> get(Player player) {
        return player.getCapability(CAPABILITY);
    }

    // ------------------------------------------------------------------ 能力注册

    @Mod.EventBusSubscriber(modid = SceneryStaff.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Registration {

        private Registration() {
        }

        @SubscribeEvent
        public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
            event.register(ChargeData.class);
        }
    }

    // ------------------------------------------------------------------ 挂到玩家

    @Mod.EventBusSubscriber(modid = SceneryStaff.MODID)
    public static final class Attachment {

        private Attachment() {
        }

        @SubscribeEvent
        public static void onAttachCapabilities(AttachCapabilitiesEvent<net.minecraft.world.entity.Entity> event) {
            if (!(event.getObject() instanceof Player player)) {
                return;
            }
            Provider provider = new Provider(player);
            event.addCapability(ID, provider);
            // 玩家被复制（死亡重生 / 跨维度）时把旧数据搬过去
            event.addListener(provider::invalidate);
        }
    }

    // ------------------------------------------------------------------ 数据本体

    /** 两条充能值（0 ~ cost，读的时候按上限钳制） */
    public static final class ChargeData {

        private int domainCharge;
        private int giantSwordCharge;

        public int getDomainCharge() {
            return this.domainCharge;
        }

        public void setDomainCharge(int value) {
            this.domainCharge = value;
        }

        public int getGiantSwordCharge() {
            return this.giantSwordCharge;
        }

        public void setGiantSwordCharge(int value) {
            this.giantSwordCharge = value;
        }

        public void copyFrom(ChargeData other) {
            this.domainCharge = other.domainCharge;
            this.giantSwordCharge = other.giantSwordCharge;
        }
    }

    /** 能力提供者：把 {@link ChargeData} 序列化进玩家的 NBT */
    public static final class Provider implements ICapabilitySerializable<CompoundTag> {

        private final ChargeData data = new ChargeData();
        private final LazyOptional<ChargeData> optional = LazyOptional.of(() -> this.data);

        public Provider(Player player) {
        }

        @Override
        public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction side) {
            return CAPABILITY.orEmpty(capability, this.optional);
        }

        @Override
        public CompoundTag serializeNBT() {
            CompoundTag tag = new CompoundTag();
            tag.putInt(TAG_DOMAIN, this.data.getDomainCharge());
            tag.putInt(TAG_GIANT_SWORD, this.data.getGiantSwordCharge());
            return tag;
        }

        @Override
        public void deserializeNBT(CompoundTag tag) {
            this.data.setDomainCharge(tag.getInt(TAG_DOMAIN));
            this.data.setGiantSwordCharge(tag.getInt(TAG_GIANT_SWORD));
        }

        public void invalidate() {
            this.optional.invalidate();
        }
    }
}
