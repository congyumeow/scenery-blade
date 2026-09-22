package com.xingling.scenerystaff.item;

import com.xingling.scenerystaff.registry.DataComponentRegistry;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

/**
 * 「达妮娅的回音频段」对刀进行铁砧强化时的全部数值与判定，集中在这里方便调参。
 *
 * <ul>
 *   <li>每次强化：消耗 1 个回音频段 + {@link #ANVIL_LEVEL_COST} 级经验；</li>
 *   <li>攻击力：每次 +15% <b>基础攻击力</b>（即刀的 {@code baseAttackModifier}），线性叠加；</li>
 *   <li>最终刀额外：每次「侵蚀领域」冷却 -0.8 秒；</li>
 *   <li>上限 {@link #MAX_LEVEL} 次（最终刀满级 35s - 4.8s = 30.2s）。</li>
 * </ul>
 *
 * 哪些刀算「本模组的刀」由 {@link SceneryBlades} 负责判定，这里只关心数值。
 */
public final class WavebandUpgrade {

    /** 强化次数上限 */
    public static final int MAX_LEVEL = 6;

    /** 每次强化增加的基础攻击力比例 */
    public static final float ATTACK_BONUS_PER_LEVEL = 0.15F;

    /** 每次强化为最终刀减少的领域冷却（tick）：0.8 秒 */
    public static final int DOMAIN_COOLDOWN_REDUCTION_TICKS = 16;

    /** 每次强化需要的经验等级 */
    public static final long ANVIL_LEVEL_COST = 5L;

    private WavebandUpgrade() {
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

    /** 按强化等级缩短后的领域冷却；非最终刀保持原值 */
    public static int domainCooldownTicks(ItemStack stack, int baseTicks) {
        if (!SceneryBlades.isFinalBlade(stack)) {
            return baseTicks;
        }
        return domainCooldownTicksFor(getLevel(stack), baseTicks);
    }

    /** 纯数值部分：等级 → 攻击力加成，便于离线核对（不依赖物品栈） */
    public static double attackBonusFor(int level, float baseAttack) {
        int clamped = Mth.clamp(level, 0, MAX_LEVEL);
        return clamped <= 0 ? 0.0D : baseAttack * ATTACK_BONUS_PER_LEVEL * clamped;
    }

    /** 纯数值部分：等级 → 缩短后的领域冷却，便于离线核对（不依赖物品栈） */
    public static int domainCooldownTicksFor(int level, int baseTicks) {
        return Math.max(0, baseTicks - Mth.clamp(level, 0, MAX_LEVEL) * DOMAIN_COOLDOWN_REDUCTION_TICKS);
    }
}
