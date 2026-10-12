package com.xingling.scenerystaff.datagen.assets.sound;

import com.xingling.scenerystaff.SceneryStaff;
import net.minecraft.data.PackOutput;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.common.data.SoundDefinition;
import net.minecraftforge.common.data.SoundDefinitionsProvider;

import java.util.function.Supplier;

/**
 * sounds.json 的数据生成基类。
 * <p>
 * 1.20.1 的 {@link SoundDefinitionsProvider} 已经实现了 {@code getName()} / {@code run()}，
 * 只留下 {@code registerSounds()} 给子类，因此这里只补一个「注册项 → 定义」的便捷方法。
 * <p>
 * 注意本类<b>不能再声明成 abstract</b>：那样会让下一层的具体子类在 javac 眼里继承不到
 * 接口侧的 {@code getName()} / {@code run()}（会报 "未覆盖 DataProvider 中的抽象方法"）。
 * 中间类保持具体，具体子类只需实现 {@code registerSounds()}。
 */
public class SceneryStaffSoundProvider extends SoundDefinitionsProvider {
    protected SceneryStaffSoundProvider(PackOutput output, ExistingFileHelper helper) {
        super(output, SceneryStaff.MODID, helper);
    }

    /**
     * 1.20.1 的 {@code SoundDefinitionsProvider} 只有
     * {@code add(Supplier<SoundEvent>, ...)} 这一个「懒取值」重载（1.21 才加了 DeferredHolder 版），
     * 因此这里收 {@link Supplier}：{@code RegistryObject} 本身就是它的实现。
     */
    public void generateNewSound(Supplier<SoundEvent> event, String basePath) {
        SoundDefinition def = SoundDefinition.definition()
                .with(SoundDefinition.Sound.sound(SceneryStaff.prefix(basePath),
                        SoundDefinition.SoundType.SOUND).volume(1.0f).pitch(1.0f));
        this.add(event, def);
    }

    /** 覆盖一层以给出稳定、唯一的名字 */
    @Override
    public String getName() {
        return "Scenery Staff Sound Definitions";
    }

    @Override
    public void registerSounds() {
        // 由具体子类提供
    }
}
