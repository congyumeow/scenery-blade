package com.xingling.scenerystaff.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * 带多行描述的通用物品。
 * <p>
 * 描述键前缀在构造时传入（如 {@code "item.scenerystaff.xuzhi.desc."}），
 * 行数与颜色规则见 {@link ItemDescriptions}。
 */
public class DescribedItem extends Item {

    private final String descKeyPrefix;

    public DescribedItem(Properties properties, String descKeyPrefix) {
        super(properties);
        this.descKeyPrefix = descKeyPrefix;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        ItemDescriptions.append(tooltip, descKeyPrefix);
    }
}
