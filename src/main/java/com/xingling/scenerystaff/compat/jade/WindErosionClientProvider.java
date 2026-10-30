package com.xingling.scenerystaff.compat.jade;

import com.xingling.scenerystaff.se.WindErosionEffect;
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
 * 客户端 tooltip：显示风蚀层数与倒计时，排版与 {@link PolymerizationClientProvider} 一致。
 * <p>
 * {@link #appendTooltip} 每帧都会被调用，而服务端只发了一次「绝对时刻」，
 * 因此这里用本地 {@code getGameTime()} 现算剩余时间——倒计时是连续走的，不需要服务端高频刷新。
 */
public class WindErosionClientProvider implements IEntityComponentProvider {

    @Override
    public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();
        if (!data.contains(WindErosionServerData.TAG_STACKS)) {
            return;
        }

        int stacks = data.getInt(WindErosionServerData.TAG_STACKS);
        if (stacks <= 0) {
            return;
        }
        int cap = data.contains(WindErosionServerData.TAG_CAP)
                ? data.getInt(WindErosionServerData.TAG_CAP)
                : WindErosionEffect.CARTETHYIA_MAX_STACKS;

        long now = accessor.getLevel().getGameTime();
        tooltip.add(Component.translatable("jade.scenerystaff.wind_erosion", stacks, cap)
                .withStyle(ChatFormatting.AQUA));

        // 只显示下一次衰减（每 15 秒掉一层）；排版与聚爆保持一致
        if (data.contains(WindErosionServerData.TAG_DECAY_AT)) {
            double decayIn = Math.max(0L, data.getLong(WindErosionServerData.TAG_DECAY_AT) - now) / 20.0D;
            tooltip.add(Component.translatable("jade.scenerystaff.wind_erosion_decay",
                            String.format(Locale.ROOT, "%.1f", decayIn))
                    .withStyle(ChatFormatting.GRAY));
        }
    }

    @Override
    public ResourceLocation getUid() {
        return WindErosionServerData.UID;
    }
}
