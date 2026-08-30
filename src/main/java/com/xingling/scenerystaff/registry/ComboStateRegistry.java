package com.xingling.scenerystaff.registry;

import com.xingling.scenerystaff.SceneryStaff;
import com.xingling.scenerystaff.sa.ErosionDomainSA;
import mods.flammpfeil.slashblade.init.DefaultResources;
import mods.flammpfeil.slashblade.registry.combo.ComboState;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 本模组的连击状态注册表。
 * 剑技由 ComboState 驱动（参考 SlashBlade 内置剑技的写法）：
 * erosion_domain 状态在时间轴第 0 帧生成侵蚀领域，然后回归 slashblade:none。
 */
public class ComboStateRegistry {
    public static final DeferredRegister<ComboState> COMBO_STATE =
            DeferredRegister.create(ComboState.REGISTRY_KEY, SceneryStaff.MODID);

    public static final DeferredHolder<ComboState, ComboState> EROSION_DOMAIN = COMBO_STATE.register("erosion_domain",
            () -> ComboState.Builder.newInstance()
                    .startAndEnd(0, 15)
                    .priority(50)
                    .motionLoc(DefaultResources.ExMotionLocation)
                    .next(entity -> mods.flammpfeil.slashblade.registry.ComboStateRegistry.NONE.getId())
                    .nextOfTimeout(entity -> mods.flammpfeil.slashblade.registry.ComboStateRegistry.NONE.getId())
                    .addTickAction(ComboState.TimeLineTickAction.getBuilder()
                            .put(0, ErosionDomainSA::doErosionDomain)
                            .build())
                    .build());
}
