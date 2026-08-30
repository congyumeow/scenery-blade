package com.xingling.scenerystaff.registry;

import com.xingling.scenerystaff.SceneryStaff;
import com.xingling.scenerystaff.sa.ErosionDomainSA;
import mods.flammpfeil.slashblade.slasharts.SlashArts;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class SARegistry {
    public static final DeferredRegister<SlashArts> REGISTRY =
            DeferredRegister.create(SlashArts.REGISTRY_KEY, SceneryStaff.MODID);

    /**
     * 侵蚀领域：剑技映射到本模组注册的 erosion_domain 连击状态，
     * 由该状态的 TimeLineTickAction 生成领域实体（见 ErosionDomainSA）。
     */
    public static final DeferredHolder<SlashArts, SlashArts> EROSION_DOMAIN =
            REGISTRY.register("erosion_domain", ErosionDomainSA::new);
}
