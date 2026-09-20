package com.xingling.scenerystaff.registry;

import com.xingling.scenerystaff.SceneryStaff;
import com.xingling.scenerystaff.item.DescribedItem;
import com.xingling.scenerystaff.item.XuzhiItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;
import java.util.Optional;

public class ItemRegistry {
    public static final DeferredRegister<Item> REGISTRY =
            DeferredRegister.create(Registries.ITEM, SceneryStaff.MODID);

    /** 虚质：合成布景之杖系列拔刀剑的媒介材料，堆叠上限 64 */
    public static final DeferredHolder<Item, Item> XUZHI =
            REGISTRY.register("xuzhi", () -> new XuzhiItem(new Item.Properties().stacksTo(64)));

    /**
     * 达妮娅的回音频段：特殊道具，由虚质合成。
     * 描述键前缀 {@code item.scenerystaff.denia_waveband.desc.}，堆叠上限 16。
     */
    public static final DeferredHolder<Item, Item> DENIA_WAVEBAND =
            REGISTRY.register("denia_waveband", () -> new DescribedItem(
                    new Item.Properties().stacksTo(16), "item.scenerystaff.denia_waveband.desc."));

    /**
     * 达妮娅的生日蛋糕：食物。
     * <p>
     * 饱食度恢复参考原版蛋糕（2 饥饿值 / 0.1 饱和度），右键直接食用；
     * 食用后获得<b>附魔金苹果</b>的三项效果：生命恢复 II（20s）、抗性提升 I（5min）、抗火 I（5min）。
     * <b>暂不提供放置为方块的能力</b>（蛋糕方块与方块实体尚未建模）。
     */
    public static final DeferredHolder<Item, Item> DENIA_BIRTHDAY_CAKE =
            REGISTRY.register("denia_birthday_cake", () -> new DescribedItem(
                    new Item.Properties()
                            .stacksTo(64)
                            .food(new FoodProperties(
                                    2,          // 饥饿值：参考原版蛋糕
                                    0.1F,       // 饱和度
                                    false,      // 非随时可食
                                    1.6F,       // 食用耗时（秒）
                                    Optional.empty(),
                                    List.of(
                                            // ——— 附魔金苹果效果 ———
                                            new FoodProperties.PossibleEffect(
                                                    () -> new MobEffectInstance(MobEffects.REGENERATION, 400, 1), 1.0F),
                                            new FoodProperties.PossibleEffect(
                                                    () -> new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 6000, 0), 1.0F),
                                            new FoodProperties.PossibleEffect(
                                                    () -> new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 6000, 0), 1.0F)
                                    ))),
                    "item.scenerystaff.denia_birthday_cake.desc."));
}
