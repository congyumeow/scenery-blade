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
        // 下位替代刀：残景之杖
        // A=樱花树苗（上方中间）、B=虚质、C=铁锭；D=前置刀「利刀「白鞘」」（杀敌 ≥ 50），位于正中间。
        SlashBladeShapedRecipeBuilder.shaped(SceneryStaffBladeRegistry.SCENERY_STAFF_WORN.location())
                .pattern(" A ")
                .pattern("BDB")
                .pattern("CBC")
                .define('D', SlashBladeIngredient.of(SlashBladeItems.SLASHBLADE_WHITE.get(),
                        RequestDefinition.Builder.newInstance().killCount(50).build())) // 利刀「白鞘」，杀敌 ≥ 50（正中间）
                .define('A', Items.CHERRY_SAPLING)
                .define('B', ItemRegistry.XUZHI.get())
                .define('C', Items.IRON_INGOT)
                .unlockedBy("has_item", has(SlashBladeItems.SLASHBLADE.get()))
                .save(output);

        // 最终刀：布景之杖
        //   .  生日蛋糕  .      （左上、右上留空）
        //  钻石  回音频段  钻石
        // 哭泣黑曜石 下位刀 哭泣黑曜石
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

        // 虚质：4 颗钻石（上下左右）+ 5 个樱花树苗（四角与中心），米字型排列。
        // 仅注册物品与配方，暂未接入任何拔刀剑配方。
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ItemRegistry.XUZHI.get())
                .pattern("SDS")
                .pattern("DSD")
                .pattern("SDS")
                .define('D', Items.DIAMOND)
                .define('S', Items.CHERRY_SAPLING)
                .unlockedBy("has_cherry_sapling", has(Items.CHERRY_SAPLING))
                .save(output);

        // 达妮娅的回音频段：4 个虚质呈十字排列（上下左右），中心留空。
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ItemRegistry.DENIA_WAVEBAND.get())
                .pattern(" A ")
                .pattern("A A")
                .pattern(" A ")
                .define('A', ItemRegistry.XUZHI.get())
                .unlockedBy("has_xuzhi", has(ItemRegistry.XUZHI.get()))
                .save(output);

        // 达妮娅的生日蛋糕：参考原版蛋糕配方，两个奶桶分别替换为甜浆果与可可豆。
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
