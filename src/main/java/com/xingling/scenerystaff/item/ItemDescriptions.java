package com.xingling.scenerystaff.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.List;

/**
 * 物品多行描述的共用工具。
 * <p>
 * 描述键为 {@code <prefix>N}（N 从 1 开始），实际行数由语言文件中存在的条目决定
 * （遇到不存在的键即停止，因此增删描述行无需改代码）。
 * 颜色优先由语言文件中的 § 格式代码控制，这里设置的样式仅作兜底。
 */
public final class ItemDescriptions {

    private static final int MAX_DESC_LINES = 8;

    private ItemDescriptions() {
    }

    public static void append(List<Component> tooltip, String descKeyPrefix) {
        for (int i = 1; i <= MAX_DESC_LINES; i++) {
            String key = descKeyPrefix + i;
            MutableComponent line = Component.translatable(key);
            if (line.getString().equals(key)) {
                break;
            }
            tooltip.add(line.withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }
}
