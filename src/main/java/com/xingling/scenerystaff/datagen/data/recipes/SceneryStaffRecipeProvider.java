package com.xingling.scenerystaff.datagen.data.recipes;

import com.xingling.scenerystaff.SceneryStaff;
import com.xingling.scenerystaff.datagen.data.blades.SceneryStaffBladeRegistry;
import mods.flammpfeil.slashblade.recipe.RequestDefinition;
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
        // 下位替代刀：残景之杖
        // 前置刀改为「利刀「白鞘」」——比无铭「无名」更容易获得，
        // 避免部分整合包删改/禁用无名刀配方后无法进阶；并附带 50 杀敌数门槛。
        SlashBladeShapedRecipeBuilder.shaped(SceneryStaffBladeRegistry.SCENERY_STAFF_WORN.location())
                .pattern(" I ")
                .pattern("GDG")
                .pattern(" E ")
                .define('D', SlashBladeIngredient.of(SlashBladeItems.SLASHBLADE_WHITE.get(),
                        RequestDefinition.Builder.newInstance().killCount(50).build())) // 利刀「白鞘」，杀敌 ≥ 50（正中间）
                .define('I', Items.IRON_INGOT)
                .define('G', Items.GOLD_INGOT)
                .define('E', Items.EMERALD)
                .unlockedBy("has_item", has(SlashBladeItems.SLASHBLADE.get()))
                .save(output);

        // 最终刀：布景之杖 —— 由下位刀「残景之杖」进阶合成。
        SlashBladeShapedRecipeBuilder.shaped(SceneryStaffBladeRegistry.SCENERY_STAFF.location())
                .pattern(" CB")
                .pattern(" AD")
                .pattern("E  ")
                // 残景之杖：需已累积 1000 杀敌数 + 20000 耀魂（ProudSoul）
                .define('A', SlashBladeIngredient.of(RequestDefinition.Builder.newInstance()
                        .name(SceneryStaff.prefix("scenery_staff_worn"))
                        .killCount(1000)
                        .proudSoul(20000)
                        .build()))
                .define('B', Items.EMERALD)
                .define('C', Items.GOLD_INGOT)
                .define('D', Items.DIAMOND)
                .define('E', Items.NETHER_STAR)
                .unlockedBy("has_item", has(SlashBladeItems.SLASHBLADE.get()))
                .save(output);
    }
}
