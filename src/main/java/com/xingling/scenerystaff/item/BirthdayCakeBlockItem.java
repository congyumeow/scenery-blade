package com.xingling.scenerystaff.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

import java.util.List;

/**
 * 方块物品：达妮娅的生日蛋糕。
 * <p>
 * 放置后即为 {@link com.xingling.scenerystaff.block.BirthdayCakeBlock}，右键切片食用；
 * 物品本身不提供手持食用（食用方式与原版蛋糕一致）。
 * 描述行数与颜色规则见 {@link ItemDescriptions}。
 */
public class BirthdayCakeBlockItem extends BlockItem {

    private final String descKeyPrefix;

    public BirthdayCakeBlockItem(Block block, Properties properties, String descKeyPrefix) {
        super(block, properties);
        this.descKeyPrefix = descKeyPrefix;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        ItemDescriptions.append(tooltip, descKeyPrefix);
    }
}
