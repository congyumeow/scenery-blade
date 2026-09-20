package com.xingling.scenerystaff.item;

import net.minecraft.world.item.Item;

/**
 * 虚质（Xuzhi）
 * <p>
 * 合成布景之杖系列拔刀剑所需的媒介材料，堆叠上限 64。
 * 描述逻辑与颜色见 {@link DescribedItem}（键前缀 {@code item.scenerystaff.xuzhi.desc.}）。
 */
public class XuzhiItem extends DescribedItem {

    public XuzhiItem(Properties properties) {
        super(properties, "item.scenerystaff.xuzhi.desc.");
    }
}
