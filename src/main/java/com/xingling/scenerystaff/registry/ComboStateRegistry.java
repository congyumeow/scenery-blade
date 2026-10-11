package com.xingling.scenerystaff.registry;

import com.xingling.scenerystaff.SceneryStaff;
import com.xingling.scenerystaff.blade.SceneryIds;
import com.xingling.scenerystaff.sa.ErosionDomainSA;
import com.xingling.scenerystaff.sa.GiantSwordSA;
import mods.flammpfeil.slashblade.registry.combo.ComboState;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 本模组的连击状态注册表。
 * <p>
 * 剑技由 ComboState 驱动（参考 SlashBlade 内置剑技的写法）：状态在时间轴第 0 帧执行剑技本体，
 * 然后回归 {@code slashblade:none}。
 * <p>
 * <b>本类只做注册</b>：连击状态的行为编排（时间轴、优先级、动作帧、回归目标）由各剑技自己提供
 * （{@link ErosionDomainSA#createComboState()} / {@link GiantSwordSA#createComboState()}）。
 * 原先这些编排内联在本类里，于是「改剑技行为」要动注册表，而注册表又反向引用剑技类，
 * 职责与依赖方向都是拧的。
 */
public class ComboStateRegistry {

    public static final DeferredRegister<ComboState> COMBO_STATE =
            DeferredRegister.create(ComboState.REGISTRY_KEY, SceneryStaff.MODID);

    /** 剑技「蚀域」：第 0 帧生成侵蚀领域 */
    public static final DeferredHolder<ComboState, ComboState> EROSION_DOMAIN =
            COMBO_STATE.register(SceneryIds.EROSION_DOMAIN_PATH, ErosionDomainSA::createComboState);

    /** 剑技「永远的启明星」：第 0 帧播 Aemeath 语音并召唤巨剑 */
    public static final DeferredHolder<ComboState, ComboState> GIANT_SWORD =
            COMBO_STATE.register(SceneryIds.GIANT_SWORD_PATH, GiantSwordSA::createComboState);
}
