package com.xingling.scenerystaff.registry;

import com.xingling.scenerystaff.SceneryStaff;
import com.xingling.scenerystaff.item.BirthdayCakeBlockItem;
import com.xingling.scenerystaff.item.DescribedItem;
import com.xingling.scenerystaff.item.XuzhiItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ItemRegistry {
    public static final DeferredRegister<Item> REGISTRY =
            DeferredRegister.create(Registries.ITEM, SceneryStaff.MODID);

    /** 虚质：合成布景之杖系列拔刀剑的媒介材料，堆叠上限 64 */
    public static final RegistryObject<Item> XUZHI =
            REGISTRY.register("xuzhi", () -> new XuzhiItem(new Item.Properties().stacksTo(64)));

    /**
     * 达妮娅的回音频段：特殊道具，由虚质合成。
     * 描述键前缀 {@code item.scenerystaff.denia_waveband.desc.}，堆叠上限 16。
     */
    public static final RegistryObject<Item> DENIA_WAVEBAND =
            REGISTRY.register("denia_waveband", () -> new DescribedItem(
                    new Item.Properties().stacksTo(16), "item.scenerystaff.denia_waveband.desc."));

    /**
     * 达妮娅的生日蛋糕（方块物品）。
     * <p>
     * 对应方块 {@link BlockRegistry#DENIA_BIRTHDAY_CAKE}：放置后与原版蛋糕一样空手右键切片食用（共 7 片），
     * 每片恢复 4 饥饿值 / 1.2 饱和度（附魔金苹果数值）并获得其三项效果。
     */
    public static final RegistryObject<Item> DENIA_BIRTHDAY_CAKE =
            REGISTRY.register("denia_birthday_cake", () -> new BirthdayCakeBlockItem(
                    BlockRegistry.DENIA_BIRTHDAY_CAKE.get(),
                    new Item.Properties().stacksTo(64),
                    "item.scenerystaff.denia_birthday_cake.desc."));

    /**
     * 爱弥斯的护身符：纪念/任务道具。
     * <p>
     * 描述键前缀 {@code item.scenerystaff.hushenfu.desc.}，不可堆叠、史诗品质（对应 5 星）。
     */
    public static final RegistryObject<Item> HUSHENFU =
            REGISTRY.register("hushenfu", () -> new DescribedItem(
                    new Item.Properties().stacksTo(1).rarity(Rarity.EPIC),
                    "item.scenerystaff.hushenfu.desc."));

    /**
     * 爱弥斯的回音频段：与「达妮娅的回音频段」对应的强化材料，用于强化启明星。
     * <p>
     * 两条强化线各管一把刀：达妮娅的回音频段 → 布景之杖系列；爱弥斯的回音频段 → 启明星。
     * 描述键前缀 {@code item.scenerystaff.aemeath_waveband.desc.}，堆叠上限 16。
     */
    public static final RegistryObject<Item> AEMEATH_WAVEBAND =
            REGISTRY.register("aemeath_waveband", () -> new DescribedItem(
                    new Item.Properties().stacksTo(16), "item.scenerystaff.aemeath_waveband.desc."));
}
