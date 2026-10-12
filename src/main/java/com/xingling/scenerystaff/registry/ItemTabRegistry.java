package com.xingling.scenerystaff.registry;

import com.xingling.scenerystaff.SceneryStaff;
import com.xingling.scenerystaff.blade.BladeStates;
import mods.flammpfeil.slashblade.SlashBladeCreativeGroup;
import mods.flammpfeil.slashblade.registry.SlashBladeItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import static com.xingling.scenerystaff.SceneryStaff.MODID;

public class ItemTabRegistry {
    public static final DeferredRegister<CreativeModeTab> REGISTRY =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final RegistryObject<CreativeModeTab> SCENERY_TAB =
            REGISTRY.register("scenerystaff_tab", () -> CreativeModeTab.builder()
                    .withTabsBefore(SlashBladeCreativeGroup.SLASHBLADE_GROUP.getId())
                    .title(Component.translatable("itemGroup.scenerystaff_tab")).icon(() -> {
                        ItemStack stack = new ItemStack(SlashBladeItems.SLASHBLADE.get());
                        BladeStates.ifPresent(stack, s -> {
                            s.setTexture(SceneryStaff.prefix("model/scenery/scenery_staff.png"));
                            s.setModel(SceneryStaff.prefix("model/scenery/scenery.obj"));
                        });
                        return stack;
                    }).displayItems((params, output) -> {
                        output.accept(ItemRegistry.XUZHI.get());
                        output.accept(ItemRegistry.DENIA_WAVEBAND.get());
                        output.accept(ItemRegistry.DENIA_BIRTHDAY_CAKE.get());
                        output.accept(ItemRegistry.HUSHENFU.get());
                        output.accept(ItemRegistry.AEMEATH_WAVEBAND.get());
                    })
                    .build());
}
