package com.xingling.scenerystaff.datagen.data.recipes;

import com.xingling.scenerystaff.datagen.data.blades.SceneryStaffBladeRegistry;
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
        SlashBladeShapedRecipeBuilder.shaped(SceneryStaffBladeRegistry.SCENERY_STAFF.location())
                .pattern(" AB")
                .pattern(" CD")
                .pattern("E  ")
                .define('A', Items.STICK)
                .define('B', Items.EMERALD)
                .define('C', Items.GOLD_INGOT)
                .define('D', Items.DIAMOND)
                .define('E', Items.NETHER_STAR)
                .unlockedBy("has_item", has(SlashBladeItems.SLASHBLADE.get()))
                .save(output);
    }
}
