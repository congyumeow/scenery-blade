package com.xingling.scenerystaff.datagen;

import com.xingling.scenerystaff.SceneryStaff;
import com.xingling.scenerystaff.datagen.assets.sound.SceneryStaffSoundGenerator;
import com.xingling.scenerystaff.datagen.data.blades.SceneryStaffBladeRegistry;
import com.xingling.scenerystaff.datagen.data.recipes.SceneryStaffRecipeProvider;
import mods.flammpfeil.slashblade.registry.slashblade.SlashBladeDefinition;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.DatapackBuiltinEntriesProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * 数据生成入口。
 * <p>
 * 1.20.1 上事件注解是 {@code @Mod.EventBusSubscriber}（1.21 改成 {@code @EventBusSubscriber}），
 * 并且 {@link GatherDataEvent} 位于 {@code net.minecraftforge.data.event} 包。
 */
@Mod.EventBusSubscriber(modid = SceneryStaff.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class SceneryStaffDataGen {

    @SubscribeEvent
    public static void dataGen(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();
        PackOutput output = generator.getPackOutput();
        ExistingFileHelper helper = event.getExistingFileHelper();

        // 客户端数据（声音）
        generator.addProvider(event.includeClient(), new SceneryStaffSoundGenerator(output, helper));

        // 服务器数据（合成配方）
        generator.addProvider(event.includeServer(), new SceneryStaffRecipeProvider(output, lookupProvider));

        // 服务器数据（命名刀数据包注册表，注册本模组的五把刀）
        final RegistrySetBuilder bladeBuilder = new RegistrySetBuilder()
                .add(SlashBladeDefinition.REGISTRY_KEY, SceneryStaffBladeRegistry::registerAll);
        generator.addProvider(event.includeServer(),
                new DatapackBuiltinEntriesProvider(output, lookupProvider, bladeBuilder, Set.of(SceneryStaff.MODID)));
    }
}
