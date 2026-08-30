package com.xingling.scenerystaff.registry;

import com.xingling.scenerystaff.SceneryStaff;
import mods.flammpfeil.slashblade.SlashBladeCreativeGroup;
import mods.flammpfeil.slashblade.capability.slashblade.BladeStateAccess;
import mods.flammpfeil.slashblade.registry.SlashBladeItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import static com.xingling.scenerystaff.SceneryStaff.MODID;

public class ItemTabRegistry {
    public static final DeferredRegister<CreativeModeTab> REGISTRY =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> SCENERY_TAB =
            REGISTRY.register("scenerystaff_tab", () -> CreativeModeTab.builder()
                    .withTabsBefore(SlashBladeCreativeGroup.SLASHBLADE_GROUP.getId())
                    .title(Component.translatable("itemGroup.scenerystaff_tab")).icon(() -> {
                        ItemStack stack = new ItemStack(SlashBladeItems.SLASHBLADE.get());
                        BladeStateAccess.of(stack).ifPresent(s -> {
                            s.setTexture(SceneryStaff.prefix("model/scenery/scenery_staff.png"));
                            s.setModel(SceneryStaff.prefix("model/scenery/scenery.obj"));
                        });
                        return stack;
                    }).displayItems((params, output) -> {})
                    .build());
}
