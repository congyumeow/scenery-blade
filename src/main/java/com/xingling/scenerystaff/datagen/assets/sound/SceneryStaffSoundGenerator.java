package com.xingling.scenerystaff.datagen.assets.sound;

import com.xingling.scenerystaff.registry.SoundRegistry;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public class SceneryStaffSoundGenerator extends SceneryStaffSoundProvider {
    public SceneryStaffSoundGenerator(PackOutput output, ExistingFileHelper helper) {
        super(output, helper);
    }

    @Override
    public void registerSounds() {
        generateNewSound(SoundRegistry.DENIA1, "denia1");
        generateNewSound(SoundRegistry.DENIA2, "denia2");
        generateNewSound(SoundRegistry.DENIA3, "denia3");
    }
}
