package com.xingling.scenerystaff.registry;

import com.mojang.serialization.Codec;
import com.xingling.scenerystaff.SceneryStaff;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 本模组的数据组件（1.21 的物品 NBT 替代方案）。
 */
public class DataComponentRegistry {

    public static final DeferredRegister.DataComponents REGISTRY =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, SceneryStaff.MODID);

    /**
     * 「达妮娅的回音频段」对一把刀已生效的强化次数（0 ~ 6）。
     * <p>
     * 存在刀上而不是存在玩家身上：强化跟着刀走，交易/掉落都不会丢。
     * 需要 {@code persistent} 才能写进物品存档，{@code networkSynchronized} 才能同步给客户端做 tooltip。
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> WAVEBAND_LEVEL =
            REGISTRY.registerComponentType("waveband_level",
                    builder -> builder.persistent(Codec.INT)
                            .networkSynchronized(ByteBufCodecs.VAR_INT));
}
