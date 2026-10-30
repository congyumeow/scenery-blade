package com.xingling.scenerystaff.registry;

import com.xingling.scenerystaff.SceneryStaff;
import com.xingling.scenerystaff.se.WindErosionEffect;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 状态效果注册表。
 * <p>
 * 目前只有一个「风蚀」——它需要引擎按实体持续 tick 才能做到「每 3 秒结算、每 15 秒衰减」，
 * 因此做成真正的 {@link MobEffect} 而不是像聚爆那样把层数记在持久化数据里。
 */
public class MobEffectRegistry {

    public static final DeferredRegister<MobEffect> REGISTRY =
            DeferredRegister.create(Registries.MOB_EFFECT, SceneryStaff.MODID);

    /** 风蚀：层数由 effect 的 amplifier 表示（amplifier 0 = 1 层） */
    public static final DeferredHolder<MobEffect, MobEffect> WIND_EROSION =
            REGISTRY.register("wind_erosion", WindErosionEffect::new);
}
