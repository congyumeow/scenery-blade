package com.xingling.scenerystaff.datagen.data.recipes;

import com.xingling.scenerystaff.SceneryStaff;
import com.xingling.scenerystaff.datagen.data.blades.SceneryStaffBladeRegistry;
import com.xingling.scenerystaff.registry.ItemRegistry;
import mods.flammpfeil.slashblade.recipe.RequestDefinition;
import mods.flammpfeil.slashblade.recipe.SlashBladeIngredient;
import mods.flammpfeil.slashblade.recipe.SlashBladeShapedRecipeBuilder;
import mods.flammpfeil.slashblade.registry.SlashBladeItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.world.item.Items;

import java.util.concurrent.CompletableFuture;

public class SceneryStaffRecipeProvider extends RecipeProvider {
    public SceneryStaffRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> provider) {
        super(output, provider);
    }

    @Override
    protected void buildRecipes(RecipeOutput output) {
        // 基础刀：潮汐（下位刀「残景之杖」的合成材料）
        // 主体是利刀「白鞘」；两种蓝色材料就地可取，不要求跑去深海找海晶系列
        SlashBladeShapedRecipeBuilder.shaped(SceneryStaffBladeRegistry.CHAOXI.location())
                .pattern(" A ")
                .pattern("BDB")
                .pattern(" C ")
                .define('D', SlashBladeIngredient.of(SlashBladeItems.SLASHBLADE_WHITE.get(),
                        RequestDefinition.Builder.newInstance().build())) // 利刀「白鞘」（正中间）
                .define('A', Items.BLUE_DYE)
                .define('B', Items.IRON_INGOT)
                .define('C', Items.BLUE_WOOL)
                .unlockedBy("has_item", has(SlashBladeItems.SLASHBLADE.get()))
                .save(output);

        // 下位替代刀：残景之杖
        SlashBladeShapedRecipeBuilder.shaped(SceneryStaffBladeRegistry.SCENERY_STAFF_WORN.location())
                .pattern(" A ")
                .pattern("BDB")
                .pattern("C C")
                // 正中间：基础刀「潮汐」，需已累积 100 杀敌数
                .define('D', SlashBladeIngredient.of(RequestDefinition.Builder.newInstance()
                        .name(SceneryStaff.prefix("chaoxi"))
                        .killCount(100)
                        .build()))
                .define('A', Items.CHERRY_SAPLING)
                .define('B', ItemRegistry.XUZHI.get())
                .define('C', Items.IRON_INGOT)
                .unlockedBy("has_item", has(SlashBladeItems.SLASHBLADE.get()))
                .save(output);

        // 最终刀：布景之杖
        SlashBladeShapedRecipeBuilder.shaped(SceneryStaffBladeRegistry.SCENERY_STAFF.location())
                .pattern(" C ")
                .pattern("BAB")
                .pattern("DED")
                .define('A', ItemRegistry.DENIA_WAVEBAND.get())      // 达妮娅的回音频段（正中间）
                .define('B', Items.DIAMOND)
                .define('C', ItemRegistry.DENIA_BIRTHDAY_CAKE.get()) // 达妮娅的生日蛋糕（上方中间）
                .define('D', Items.CRYING_OBSIDIAN)
                // 下位刀：需已累积 1000 杀敌数 + 20000 耀魂（ProudSoul）
                .define('E', SlashBladeIngredient.of(RequestDefinition.Builder.newInstance()
                        .name(SceneryStaff.prefix("scenery_staff_worn"))
                        .killCount(1000)
                        .proudSoul(20000)
                        .build()))
                .unlockedBy("has_item", has(SlashBladeItems.SLASHBLADE.get()))
                .save(output);

        // 启明星线的下位刀：启明星（与残景之杖同构，顶上换青金石）
        SlashBladeShapedRecipeBuilder.shaped(SceneryStaffBladeRegistry.QIMINGXING_WORN.location())
                .pattern(" L ")
                .pattern("AXA")
                .pattern("B B")
                .define('L', Items.LAPIS_LAZULI)                     // 上方中间
                .define('A', ItemRegistry.XUZHI.get())
                .define('B', Items.IRON_INGOT)
                // 正中间：基础刀「潮汐」，需已累积 100 杀敌数（与残景之杖统一）
                .define('X', SlashBladeIngredient.of(RequestDefinition.Builder.newInstance()
                        .name(SceneryStaff.prefix("chaoxi"))
                        .killCount(100)
                        .build()))
                .unlockedBy("has_item", has(SlashBladeItems.SLASHBLADE.get()))
                .save(output);

        // 启明星线的上位刀：永远的启明星（与布景之杖同构）
        SlashBladeShapedRecipeBuilder.shaped(SceneryStaffBladeRegistry.QIMINGXING.location())
                .pattern(" C ")
                .pattern("BAB")
                .pattern("DED")
                .define('A', ItemRegistry.AEMEATH_WAVEBAND.get())    // 爱弥斯的回音频段（正中间）
                .define('B', Items.DIAMOND)
                .define('C', ItemRegistry.HUSHENFU.get())            // 爱弥斯的护身符（上方中间）
                .define('D', Items.CRYING_OBSIDIAN)
                // 下位刀：启明星，需已累积 1000 杀敌数 + 20000 耀魂（ProudSoul）
                .define('E', SlashBladeIngredient.of(RequestDefinition.Builder.newInstance()
                        .name(SceneryStaff.prefix("qimingxing_worn"))
                        .killCount(1000)
                        .proudSoul(20000)
                        .build()))
                .unlockedBy("has_item", has(SlashBladeItems.SLASHBLADE.get()))
                .save(output);

        // 爱弥斯的护身符：3 线（上中 + 中左 + 中右）+ 底部中间 1 钻石
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ItemRegistry.HUSHENFU.get())
                .pattern(" A ")
                .pattern("A A")
                .pattern(" C ")
                .define('A', Items.STRING)
                .define('C', Items.DIAMOND)
                .unlockedBy("has_diamond", has(Items.DIAMOND))
                .save(output);

        // 虚质
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ItemRegistry.XUZHI.get())
                .pattern("SDS")
                .pattern("DSD")
                .pattern("SDS")
                .define('D', Items.DIAMOND)
                .define('S', Items.CHERRY_SAPLING)
                .unlockedBy("has_cherry_sapling", has(Items.CHERRY_SAPLING))
                .save(output);

        // 达妮娅的回音频段
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ItemRegistry.DENIA_WAVEBAND.get())
                .pattern(" A ")
                .pattern("A A")
                .pattern(" A ")
                .define('A', ItemRegistry.XUZHI.get())
                .unlockedBy("has_xuzhi", has(ItemRegistry.XUZHI.get()))
                .save(output);

        // 爱弥斯的回音频段：虚质环绕 + 爱弥斯的护身符（正中间）
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ItemRegistry.AEMEATH_WAVEBAND.get())
                .pattern(" A ")
                .pattern("ABA")
                .pattern(" A ")
                .define('A', ItemRegistry.XUZHI.get())
                .define('B', ItemRegistry.HUSHENFU.get())
                .unlockedBy("has_hushenfu", has(ItemRegistry.HUSHENFU.get()))
                .save(output);

        // 达妮娅的生日蛋糕
        ShapedRecipeBuilder.shaped(RecipeCategory.FOOD, ItemRegistry.DENIA_BIRTHDAY_CAKE.get())
                .pattern("ABC")
                .pattern("DED")
                .pattern("FFF")
                .define('A', Items.SWEET_BERRIES)
                .define('B', Items.TORCH)
                .define('C', Items.COCOA_BEANS)
                .define('D', Items.SUGAR)
                .define('E', Items.EGG)
                .define('F', Items.WHEAT)
                .unlockedBy("has_wheat", has(Items.WHEAT))
                .save(output);
    }
}
