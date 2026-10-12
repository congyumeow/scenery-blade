package com.xingling.scenerystaff.blade;

import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.LazyOptional;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * 刀的 {@link ISlashBladeState} 能力访问的<b>唯一入口</b>。
 * <p>
 * 1.21.1 的 SlashBlade 提供了 {@code BladeStateAccess.of(stack)} 这个静态便捷方法；1.20.1 上还没有，
 * 只有 {@code stack.getCapability(ItemSlashBlade.BLADESTATE)} 直接返回 {@link LazyOptional}。
 * 本类把那套 {@code LazyOptional} 调用收口成与 1.21.1 相同的 {@link Optional} 形式，
 * 于是其余代码（tooltip、解封兜底、模型判定、创造栏图标）不必各自处理 capability 的样板。
 * <p>
 * 只应在确认 {@code stack} 是拔刀剑时依赖返回值；非拔刀剑会拿到 {@link Optional#empty()}。
 */
public final class BladeStates {

    private BladeStates() {
    }

    /** 取刀的持久状态；不是拔刀剑 / 没有能力时为空 */
    public static Optional<ISlashBladeState> of(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof ItemSlashBlade)) {
            return Optional.empty();
        }
        LazyOptional<ISlashBladeState> optional = stack.getCapability(ItemSlashBlade.BLADESTATE);
        return optional.isPresent() ? Optional.of(optional.orElseThrow(NullPointerException::new)) : Optional.empty();
    }

    /** 有状态时执行 {@code action}（存在即读取，不做任何写回判定） */
    public static void ifPresent(ItemStack stack, Consumer<ISlashBladeState> action) {
        of(stack).ifPresent(action);
    }
}
