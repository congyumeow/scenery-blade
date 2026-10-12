package com.xingling.scenerystaff.registry;

import com.xingling.scenerystaff.SceneryStaff;
import com.xingling.scenerystaff.se.PolymerizationSE;
import mods.flammpfeil.slashblade.registry.specialeffects.SpecialEffect;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class SERegistry {
    public static final DeferredRegister<SpecialEffect> REGISTRY =
            DeferredRegister.create(SpecialEffect.REGISTRY_KEY, SceneryStaff.MODID);

    public static final RegistryObject<SpecialEffect> POLYMERIZATION =
            REGISTRY.register("polymerization", PolymerizationSE::new);
}
