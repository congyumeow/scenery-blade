package com.xingling.scenerystaff;

import com.xingling.scenerystaff.compat.ironsspells.IronsSpellsCompat;
import com.xingling.scenerystaff.registry.*;
import com.xingling.scenerystaff.sa.SaCharge;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
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
        // SA 充能值（玩家附件：持久化 + 自动同步到客户端）
        SaCharge.REGISTRY.register(modEventBus);

        // 铁魔法是可选联动。必须用 isLoaded 守卫把调用包起来：
        // NoClassDefFoundError；放在守卫里，这个类才只会在铁魔法确实存在时加载。
        if (ModList.get().isLoaded("irons_spellbooks")) {
            IronsSpellsCompat.register();
        }
    }
}
