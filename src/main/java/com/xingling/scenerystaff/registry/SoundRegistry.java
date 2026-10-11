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

    /**
     * 启明星「巨剑」剑技的释放语音。
     * <p>
     * ⚠ 对应的音频资源 {@code assets/scenerystaff/sounds/aemeath.ogg} 目前缺失
     * （重命名时代码误判大小写而删除），数据生成里那一行也暂时注释着；
     * 把文件放回 sounds 目录后恢复该行并重跑 runData 即可，
     * 在那之前这个 SoundEvent 没有 sounds.json 定义，播放时只会静默失败、不会报错。
     */
    public static final DeferredHolder<SoundEvent, SoundEvent> AEMEATH = createEvent("aemeath");

    private static DeferredHolder<SoundEvent, SoundEvent> createEvent(String name) {
        return REGISTRY.register(name, () -> SoundEvent.createVariableRangeEvent(SceneryStaff.prefix(name)));
    }
}
