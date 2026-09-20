package com.xingling.scenerystaff.datagen.data.recipes;

import com.xingling.scenerystaff.SceneryStaff;
import com.xingling.scenerystaff.datagen.data.blades.SceneryStaffBladeRegistry;
import mods.flammpfeil.slashblade.recipe.SlashBladeIngredient;
import mods.flammpfeil.slashblade.recipe.SlashBladeShapedRecipeBuilder;
import mods.flammpfeil.slashblade.registry.SlashBladeItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.world.item.Items;

import java.util.concurrent.CompletableFuture;

public class SceneryStaffRecipeProvider extends RecipeProvider {
    public SceneryStaffRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> provider) {
        super(output, provider);
    }

    @Override
    protected void buildRecipes(RecipeOutput output) {
        // 下位替代刀：残景之杖（前中期过渡，材料降级：铁/金/绿宝石）
        SlashBladeShapedRecipeBuilder.shaped(SceneryStaffBladeRegistry.SCENERY_STAFF_WORN.location())
                .pattern(" AB")
                .pattern(" CD")
                .pattern("E  ")
                .define('A', Items.STICK)
                .define('B', Items.IRON_INGOT)
                .define('C', Items.IRON_INGOT)
                .define('D', Items.GOLD_INGOT)
                .define('E', Items.EMERALD)
                .unlockedBy("has_item", has(SlashBladeItems.SLASHBLADE.get()))
                .save(output);

        // 最终刀：布景之杖 —— 由下位刀「残景之杖」进阶合成。
        SlashBladeShapedRecipeBuilder.shaped(SceneryStaffBladeRegistry.SCENERY_STAFF.location())
                .pattern(" CB")
                .pattern(" AD")
                .pattern("E  ")
                .define('A', SlashBladeIngredient.of(SceneryStaff.prefix("scenery_staff_worn"))) // 残景之杖
                .define('B', Items.EMERALD)
                .define('C', Items.GOLD_INGOT)
                .define('D', Items.DIAMOND)
                .define('E', Items.NETHER_STAR)
                .unlockedBy("has_item", has(SlashBladeItems.SLASHBLADE.get()))
                .save(output);
    }
}
