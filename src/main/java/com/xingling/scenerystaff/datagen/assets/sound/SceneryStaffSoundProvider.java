package com.xingling.scenerystaff.datagen.assets.sound;

import com.xingling.scenerystaff.SceneryStaff;
import net.minecraft.data.PackOutput;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.common.data.SoundDefinition;
import net.neoforged.neoforge.common.data.SoundDefinitionsProvider;
import net.neoforged.neoforge.registries.DeferredHolder;

public abstract class SceneryStaffSoundProvider extends SoundDefinitionsProvider {
    protected SceneryStaffSoundProvider(PackOutput output, ExistingFileHelper helper) {
        super(output, SceneryStaff.MODID, helper);
    }

    public void generateNewSound(DeferredHolder<SoundEvent, SoundEvent> event, String basePath) {
        SoundDefinition def = SoundDefinition.definition()
                .with(SoundDefinition.Sound.sound(SceneryStaff.prefix(basePath),
                        SoundDefinition.SoundType.SOUND).volume(1.0f).pitch(1.0f));
        this.add(event, def);
    }

}
