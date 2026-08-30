package com.xingling.scenerystaff.datagen.data.blades;

import com.xingling.scenerystaff.SceneryStaff;
import com.xingling.scenerystaff.registry.ItemTabRegistry;
import com.xingling.scenerystaff.registry.SARegistry;
import com.xingling.scenerystaff.registry.SERegistry;
import mods.flammpfeil.slashblade.item.SwordType;
import mods.flammpfeil.slashblade.registry.slashblade.EnchantmentDefinition;
import mods.flammpfeil.slashblade.registry.slashblade.PropertiesDefinition;
import mods.flammpfeil.slashblade.registry.slashblade.RenderDefinition;
import mods.flammpfeil.slashblade.registry.slashblade.SlashBladeDefinition;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;

import java.util.List;


public class SceneryStaffBladeRegistry {
    public static final ResourceKey<SlashBladeDefinition> SCENERY_STAFF =
            ResourceKey.create(SlashBladeDefinition.REGISTRY_KEY, SceneryStaff.prefix("scenery_staff"));

    public static void registerAll(BootstrapContext<SlashBladeDefinition> bootstrap) {
        bootstrap.register(
                SCENERY_STAFF,
                new SlashBladeDefinition(
                        // name 与数据包 key 保持一致（决定翻译键 item.scenerystaff.scenery_staff）
                        SceneryStaff.prefix("scenery_staff"),
                        RenderDefinition.Builder.newInstance()
                                .textureName(SceneryStaff.prefix("model/scenery/scenery_staff.png"))
                                .modelName(SceneryStaff.prefix("model/scenery/scenery.obj"))
                                .effectColor(0xAABBCC)
                                .build(),
                        PropertiesDefinition.Builder.newInstance()
                                .defaultSwordType(List.of(SwordType.BEWITCHED))
                                .baseAttackModifier(12)
                                .maxDamage(200)
                                .addSpecialEffect(SERegistry.POLYMERIZATION.getId())   // 添加SE
                                .slashArtsType(SARegistry.EROSION_DOMAIN.getId())      // 设置SA
                                .build(),
                        List.of(
                                new EnchantmentDefinition(getEnchantment(bootstrap, Enchantments.POWER), 2),
                                new EnchantmentDefinition(getEnchantment(bootstrap, Enchantments.UNBREAKING), 3)
                        ),
                        ItemTabRegistry.SCENERY_TAB.getId()
                )
        );
    }

    private static Holder<Enchantment> getEnchantment(BootstrapContext<SlashBladeDefinition> bootstrap,
                                  ResourceKey<Enchantment> key) {
        return bootstrap.lookup(Registries.ENCHANTMENT).getOrThrow(key);
    }
}
