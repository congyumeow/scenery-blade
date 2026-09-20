package com.xingling.scenerystaff.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * 带多行描述的通用物品。
 * <p>
 * 描述键为 {@code <descKeyPrefix>N}（N 从 1 开始），实际行数由语言文件中存在的条目决定
 * （遇到不存在的键即停止，因此增删描述行无需改代码）。
 * <p>
 * 颜色优先由语言文件中的 § 格式代码控制（§ 由原版渲染时解析，优先级高于组件样式），
 * 此处设置的样式仅作兜底。
 */
public class DescribedItem extends Item {

    /** 描述最大行数 */
    private static final int MAX_DESC_LINES = 8;

    private final String descKeyPrefix;

    public DescribedItem(Properties properties, String descKeyPrefix) {
        super(properties);
        this.descKeyPrefix = descKeyPrefix;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
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
