package com.xingling.scenerystaff;

import com.xingling.scenerystaff.registry.*;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.fml.common.Mod;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(SceneryStaff.MODID)
public class SceneryStaff {
    // Define mod id in a common place for everything to reference
    public static final String MODID = "scenerystaff";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();

    public static ResourceLocation prefix(String path) {
        return ResourceLocation.parse(MODID + ":" + path);
    }

    public SceneryStaff(IEventBus modEventBus) {
        SoundRegistry.REGISTRY.register(modEventBus);
        BlockRegistry.REGISTRY.register(modEventBus);
        ItemRegistry.REGISTRY.register(modEventBus);
        ItemTabRegistry.REGISTRY.register(modEventBus);
        SERegistry.REGISTRY.register(modEventBus);
        SARegistry.REGISTRY.register(modEventBus);
        ComboStateRegistry.COMBO_STATE.register(modEventBus);
        EntityRegistry.REGISTRY.register(modEventBus);
        DataComponentRegistry.REGISTRY.register(modEventBus);
    }
}
