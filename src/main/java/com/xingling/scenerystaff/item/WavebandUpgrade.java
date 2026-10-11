package com.xingling.scenerystaff.item;

import com.xingling.scenerystaff.registry.DataComponentRegistry;
import com.xingling.scenerystaff.registry.ItemRegistry;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

/**
 * 回音频段对刀进行铁砧强化时的全部数值与判定，集中在这里方便调参。
 * <p>
 * <b>两条强化线各管一把刀</b>：
 * <ul>
 *   <li>达妮娅的回音频段 → 布景之杖系列（最终刀 / 下位刀）；</li>
 *   <li>爱弥斯的回音频段 → 启明星。</li>
 * </ul>
 * 两者的强化方式与数值完全一致：
 * <ul>
 *   <li>每次强化：消耗 1 个回音频段 + {@link #ANVIL_LEVEL_COST} 级经验；</li>
 *   <li>攻击力：每次 +{@link #ATTACK_BONUS_PER_LEVEL} 的<b>基础攻击力</b>，线性叠加；</li>
 *   <li>暴击：未强化 {@link #CRIT_CHANCE_BASE} / {@link #CRIT_DAMAGE_BASE}，
 *       满级（{@link #MAX_LEVEL} 次）{@link #CRIT_CHANCE_MAX} / {@link #CRIT_DAMAGE_MAX}，线性增长。</li>
 * </ul>
 * 「哪些刀能被强化」由 {@link SceneryBlades#isWavebandUpgradable} 判定，
 * 「哪条配哪把」由 {@link #matches} 判定，这里只关心数值。
 */
public final class WavebandUpgrade {

    /** 强化次数上限 */
    public static final int MAX_LEVEL = 6;

    /** 每次强化增加的基础攻击力比例 */
    public static final float ATTACK_BONUS_PER_LEVEL = 0.15F;

    /** 每次强化需要的经验等级 */
    public static final long ANVIL_LEVEL_COST = 5L;

    /** 未强化时的暴击率与暴击伤害倍率（1.50 = 150%） */
    public static final double CRIT_CHANCE_BASE = 0.20D;
    public static final double CRIT_DAMAGE_BASE = 1.50D;

    /** 满级时的暴击率与暴击伤害倍率 */
    public static final double CRIT_CHANCE_MAX = 0.80D;
    public static final double CRIT_DAMAGE_MAX = 2.80D;

    private WavebandUpgrade() {
    }

    /**
     * 这条回音频段能不能强化这把刀。
     * <p>
     * 达妮娅的 → 布景之杖系列；爱弥斯的 → 启明星。
     */
    public static boolean matches(ItemStack waveband, ItemStack blade) {
        if (waveband.is(ItemRegistry.DENIA_WAVEBAND.get())) {
            return SceneryBlades.isSceneryBlade(blade);
        }
        if (waveband.is(ItemRegistry.AEMEATH_WAVEBAND.get())) {
            return SceneryBlades.isQimingxingBlade(blade);
        }
        return false;
    }

    /** 读取一把刀上已有的强化次数（无组件 / 非本模组的刀一律为 0） */
    public static int getLevel(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        Integer raw = stack.get(DataComponentRegistry.WAVEBAND_LEVEL.get());
        if (raw == null) {
            return 0;
        }
        return Mth.clamp(raw, 0, MAX_LEVEL);
    }

    public static void setLevel(ItemStack stack, int level) {
        stack.set(DataComponentRegistry.WAVEBAND_LEVEL.get(), Mth.clamp(level, 0, MAX_LEVEL));
    }

    /** 当前强化等级带来的攻击力加成（= 基础攻击力 × 15% × 等级） */
    public static double attackBonus(ItemStack stack, ISlashBladeState state) {
        if (state == null) {
            return 0.0D;
        }
        return attackBonusFor(getLevel(stack), state.getBaseAttackModifier());
    }

    /** 纯数值部分：等级 → 攻击力加成，便于离线核对（不依赖物品栈） */
    public static double attackBonusFor(int level, float baseAttack) {
        int clamped = Mth.clamp(level, 0, MAX_LEVEL);
        return clamped <= 0 ? 0.0D : baseAttack * ATTACK_BONUS_PER_LEVEL * clamped;
    }

    /** 纯数值部分：等级 → 暴击率（0.20 线性升到 0.80） */
    public static double critChanceFor(int level) {
        int clamped = Mth.clamp(level, 0, MAX_LEVEL);
        return CRIT_CHANCE_BASE + (CRIT_CHANCE_MAX - CRIT_CHANCE_BASE) * clamped / MAX_LEVEL;
    }

    /** 纯数值部分：等级 → 暴击伤害倍率（1.50 线性升到 2.80） */
    public static double critDamageFor(int level) {
        int clamped = Mth.clamp(level, 0, MAX_LEVEL);
        return CRIT_DAMAGE_BASE + (CRIT_DAMAGE_MAX - CRIT_DAMAGE_BASE) * clamped / MAX_LEVEL;
    }

    /** 刀当前的暴击率 */
    public static double critChanceOf(ItemStack stack) {
        return critChanceFor(getLevel(stack));
    }

    /** 刀当前的暴击伤害倍率 */
    public static double critDamageOf(ItemStack stack) {
        return critDamageFor(getLevel(stack));
    }
}
