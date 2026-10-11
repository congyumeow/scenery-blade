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

    /** 独立新刀「启明星」 */
    private static final ResourceLocation QIMINGXING = SceneryStaff.prefix("qimingxing");

    /** 启明星线的下位刀「启明星」（上位刀为「永远的启明星」） */
    private static final ResourceLocation QIMINGXING_WORN = SceneryStaff.prefix("qimingxing_worn");

    /** 基础刀「潮汐」 */
    private static final ResourceLocation CHAOXI = SceneryStaff.prefix("chaoxi");

    private SceneryBlades() {
    }

    /**
     * 是否为「启明星」线的上位刀：永远的启明星。
     * <p>
     * 刻意与 {@link #isSceneryBlade(ItemStack)} 分开：那个方法决定侵蚀领域、铁魔法联动等
     * 只属于「布景之杖」系列的逻辑，启明星线不应该被卷进去。
     * 巨剑剑技、聚爆 13 层上限、暴击特性都只认这一把；下位刀见 {@link #isQimingxingWorn(ItemStack)}。
     */
    public static boolean isQimingxing(ItemStack stack) {
        return matches(stack, QIMINGXING);
    }

    /** 是否为「启明星」线的下位刀：启明星（旧贴图那把） */
    public static boolean isQimingxingWorn(ItemStack stack) {
        return matches(stack, QIMINGXING_WORN);
    }

    /** 是否为「启明星」线的任意一把刀（上位或下位） */
    public static boolean isQimingxingBlade(ItemStack stack) {
        return isQimingxing(stack) || isQimingxingWorn(stack);
    }

    private static boolean matches(ItemStack stack, ResourceLocation id) {
        if (!(stack.getItem() instanceof ItemSlashBlade blade)) {
            return false;
        }
        return id.equals(blade.getBladeId(stack));
    }

    /** 是否为基础刀「潮汐」 */
    public static boolean isChaoxi(ItemStack stack) {
        if (!(stack.getItem() instanceof ItemSlashBlade blade)) {
            return false;
        }
        return CHAOXI.equals(blade.getBladeId(stack));
    }

    /** 是否为「布景之杖」系列的刀（最终刀或下位刀） */
    public static boolean isSceneryBlade(ItemStack stack) {
        return getSceneryBladeId(stack) != null;
    }

    /**
     * 是否为「可被回音频段强化的刀」：布景之杖系列 + 启明星。
     * <p>
     * 两条强化线各管一把刀——达妮娅的回音频段强化布景之杖系列，
     * 爱弥斯的回音频段强化启明星。具体哪条配哪把由 {@code WavebandUpgrade#matches} 判定，
     * 这里只回答「这把刀能不能被强化」。
     */
    public static boolean isWavebandUpgradable(ItemStack stack) {
        return isSceneryBlade(stack) || isQimingxingBlade(stack);
    }

    /**
     * 是否为「带有暴击特性的刀」：两把终盘刀「布景之杖」与「启明星」。
     * <p>
     * 刻意小于 {@link #isWavebandUpgradable(ItemStack)}：下位刀「残景之杖」同样能被回音频段强化、
     * 同样吃攻击力加成，但不具备暴击特性。
     */
    public static boolean hasCrit(ItemStack stack) {
        return isFinalBlade(stack) || isQimingxing(stack);
    }

    /**
     * 是否为「上位档」的刀：布景之杖 / 永远的启明星。
     * <p>
     * 铁魔法联动按这一档给双倍法杖属性，下位档（残景之杖 / 启明星）给基础值。
     */
    public static boolean isTopTier(ItemStack stack) {
        return isFinalBlade(stack) || isQimingxing(stack);
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
