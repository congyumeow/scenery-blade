package com.xingling.scenerystaff.registry;

import com.xingling.scenerystaff.SceneryStaff;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class SoundRegistry {
    public static final DeferredRegister<SoundEvent> REGISTRY =
            DeferredRegister.create(Registries.SOUND_EVENT, SceneryStaff.MODID);

    public static final RegistryObject<SoundEvent> DENIA1 = createEvent("denia1");
    public static final RegistryObject<SoundEvent> DENIA2 = createEvent("denia2");
    public static final RegistryObject<SoundEvent> DENIA3 = createEvent("denia3");

    /**
     * 启明星「巨剑」剑技的释放语音（{@code assets/scenerystaff/sounds/aemeath.ogg}）。
     */
    public static final RegistryObject<SoundEvent> AEMEATH = createEvent("aemeath");

    /** 不屈命定之冠「自由骑士之舞」的释放语音（三条轮播，释放时随机取一条） */
    public static final RegistryObject<SoundEvent> CARTETHYIA1 = createEvent("cartethyia1");
    public static final RegistryObject<SoundEvent> CARTETHYIA2 = createEvent("cartethyia2");
    public static final RegistryObject<SoundEvent> CARTETHYIA3 = createEvent("cartethyia3");

    private static RegistryObject<SoundEvent> createEvent(String name) {
        return REGISTRY.register(name, () -> SoundEvent.createVariableRangeEvent(SceneryStaff.prefix(name)));
    }
}
