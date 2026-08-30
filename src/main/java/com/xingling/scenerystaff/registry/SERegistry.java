package com.xingling.scenerystaff.registry;

import com.xingling.scenerystaff.SceneryStaff;
import com.xingling.scenerystaff.se.PolymerizationSE;
import mods.flammpfeil.slashblade.registry.specialeffects.SpecialEffect;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class SERegistry {
    public static final DeferredRegister<SpecialEffect> REGISTRY =
            DeferredRegister.create(SpecialEffect.REGISTRY_KEY, SceneryStaff.MODID);

    public static final DeferredHolder<SpecialEffect, SpecialEffect> POLYMERIZATION =
            REGISTRY.register("polymerization", PolymerizationSE::new);
}
