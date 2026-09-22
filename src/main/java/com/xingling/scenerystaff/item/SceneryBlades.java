package com.xingling.scenerystaff.item;

import com.xingling.scenerystaff.SceneryStaff;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * 「布景之杖」系列拔刀剑的识别。
 * <p>
 * 这里刻意<b>不</b>引用 {@code SceneryStaffBladeRegistry} 的 {@code ResourceKey}：
 * 那个类的静态初始化会牵出 {@code SlashBladeDefinition} → {@code PropertiesDefinition} →
 * {@code ComboStateRegistry}，进而要求 Minecraft 注册表已经 bootstrap；
 * 为了两个 ResourceLocation 拉这么长的初始化链不值得。ID 必须与
 * {@code SceneryStaffBladeRegistry} 中的 key 保持一致。
 */
public final class SceneryBlades {

    /** 最终刀「布景之杖」 */
    private static final ResourceLocation FINAL_BLADE = SceneryStaff.prefix("scenery_staff");

    /** 下位刀「残景之杖」 */
    private static final ResourceLocation WORN_BLADE = SceneryStaff.prefix("scenery_staff_worn");

    private SceneryBlades() {
    }

    /** 是否为「布景之杖」系列的刀（最终刀或下位刀） */
    public static boolean isSceneryBlade(ItemStack stack) {
        return getSceneryBladeId(stack) != null;
    }

    /** 是否为最终刀「布景之杖」（唯一带侵蚀领域 SA 的那把） */
    public static boolean isFinalBlade(ItemStack stack) {
        return FINAL_BLADE.equals(getSceneryBladeId(stack));
    }

    /**
     * 取本模组的刀 ID；不是拔刀剑、或者不是本模组的刀时返回 {@code null}。
     * <p>
     * {@link ItemSlashBlade#getBladeId(ItemStack)} 由刀自身的翻译键解析而来，对无状态的刀会退回物品注册名，
     * 因此不会把别的模组的拔刀剑误判成本模组的刀。
     */
    private static ResourceLocation getSceneryBladeId(ItemStack stack) {
        if (!(stack.getItem() instanceof ItemSlashBlade blade)) {
            return null;
        }
        ResourceLocation id = blade.getBladeId(stack);
        return FINAL_BLADE.equals(id) || WORN_BLADE.equals(id) ? id : null;
    }
}
