package com.xingling.scenerystaff.registry;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

/**
 * 本模组写在刀上的自定义数据（1.21 的「数据组件」在 1.20.1 上的替代方案）。
 * <p>
 * 1.20.1 既没有 {@code DataComponentType}，物品栈也只有 NBT 一条路，因此强化次数改为写在
 * 刀自身的 {@code tag} 上，读写一律经过本类，调用方看不到底层存法。
 */
public final class DataComponentRegistry {

    /** 「达妮娅的回音频段」对一把刀已生效的强化次数（0 ~ 6） */
    private static final String TAG_WAVEBAND_LEVEL = "scenerystaff:waveband_level";

    private DataComponentRegistry() {
    }

    /** 读取强化次数；没有写过时返回 0 */
    public static int getWavebandLevel(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        CompoundTag tag = stack.getTag();
        return tag == null ? 0 : tag.getInt(TAG_WAVEBAND_LEVEL);
    }

    /** 写入强化次数（存在刀自身，交易/掉落都不会丢） */
    public static void setWavebandLevel(ItemStack stack, int level) {
        if (stack.isEmpty()) {
            return;
        }
        stack.getOrCreateTag().putInt(TAG_WAVEBAND_LEVEL, level);
    }
}
