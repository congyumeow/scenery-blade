package com.xingling.scenerystaff.registry;

import com.xingling.scenerystaff.SceneryStaff;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class SoundRegistry {
    public static final DeferredRegister<SoundEvent> REGISTRY =
            DeferredRegister.create(Registries.SOUND_EVENT, SceneryStaff.MODID);

    public static final DeferredHolder<SoundEvent, SoundEvent> DENIA1 = createEvent("denia1");
    public static final DeferredHolder<SoundEvent, SoundEvent> DENIA2 = createEvent("denia2");
    public static final DeferredHolder<SoundEvent, SoundEvent> DENIA3 = createEvent("denia3");

    private static DeferredHolder<SoundEvent, SoundEvent> createEvent(String name) {
        return REGISTRY.register(name, () -> SoundEvent.createVariableRangeEvent(SceneryStaff.prefix(name)));
    }
}
