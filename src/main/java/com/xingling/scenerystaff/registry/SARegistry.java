package com.xingling.scenerystaff.registry;

import com.xingling.scenerystaff.SceneryStaff;
import com.xingling.scenerystaff.sa.ErosionDomainSA;
import com.xingling.scenerystaff.sa.GiantSwordSA;
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

    /**
     * 巨剑：启明星专属剑技。同样映射到一个连击状态（{@code scenerystaff:giant_sword}），
     * 由该状态的时间轴回调召唤巨剑并播放 Aemeath 语音（见 GiantSwordSA）。
     */
    public static final DeferredHolder<SlashArts, SlashArts> GIANT_SWORD =
            REGISTRY.register("giant_sword", GiantSwordSA::new);
}
