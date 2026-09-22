package com.xingling.scenerystaff.compat.jade;

import com.xingling.scenerystaff.se.PolymerizationSE;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

import java.util.Locale;

/**
 * 客户端 tooltip：显示聚合层数与「距离下一次衰减」的剩余时间。
 * <p>
 * 层数与倒计时都由服务端发来的「原始层数 + 最后一次叠层时刻」现算，
 * 用的是与游戏逻辑完全相同的 {@link PolymerizationSE} 纯函数，因此不会出现两套规则对不上的情况。
 * Jade 每帧都会调用 {@link #appendTooltip}，倒计时是连续走的。
 * 客户端世界时间由原版时间包驱动，与服务器误差在 1 秒内。
 */
public class PolymerizationClientProvider implements IEntityComponentProvider {

    @Override
    public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();
        if (!data.contains(PolymerizationSE.TAG_COUNT)) {
            return;
        }

        long elapsed = Math.max(0L, accessor.getLevel().getGameTime() - data.getLong(PolymerizationSE.TAG_LAST));
        int count = PolymerizationSE.decayedCount(data.getInt(PolymerizationSE.TAG_COUNT), elapsed);
        if (count <= 0) {
            return; // 已经衰减光了
        }

        tooltip.add(Component.translatable("jade.scenerystaff.polymerization",
                count, PolymerizationSE.MAX_STACK).withStyle(ChatFormatting.GOLD));

        tooltip.add(Component.translatable("jade.scenerystaff.polymerization_decay",
                        String.format(Locale.ROOT, "%.1f", PolymerizationSE.ticksUntilDecay(elapsed) / 20.0F))
                .withStyle(ChatFormatting.GRAY));
    }

    @Override
    public ResourceLocation getUid() {
        return PolymerizationServerData.UID;
    }
}
