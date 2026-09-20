package com.xingling.scenerystaff.datagen.data.blades;

import com.xingling.scenerystaff.SceneryStaff;
import com.xingling.scenerystaff.registry.ItemTabRegistry;
import com.xingling.scenerystaff.registry.SARegistry;
import com.xingling.scenerystaff.registry.SERegistry;
import mods.flammpfeil.slashblade.item.SwordType;
import mods.flammpfeil.slashblade.registry.SlashArtsRegistry;
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

    /**
     * 下位替代刀：残景之杖。
     * 与最终刀同一外观系列、同一合成模板，但材料降级、数值削弱、不带专属 SE，
     * 剑技使用 SlashBlade 自带垂直波刃，作为前中期过渡刀使用。
     */
    public static final ResourceKey<SlashBladeDefinition> SCENERY_STAFF_WORN =
            ResourceKey.create(SlashBladeDefinition.REGISTRY_KEY, SceneryStaff.prefix("scenery_staff_worn"));

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
                                .baseAttackModifier(14)
                                .maxDamage(200)
                                .addSpecialEffect(SERegistry.POLYMERIZATION.getId())   // 添加SE
                                .slashArtsType(SARegistry.EROSION_DOMAIN.getId())      // 设置SA
                                .build(),
                        List.of(
                                new EnchantmentDefinition(getEnchantment(bootstrap, Enchantments.POWER), 5),
                                new EnchantmentDefinition(getEnchantment(bootstrap, Enchantments.UNBREAKING), 3),
                                new EnchantmentDefinition(getEnchantment(bootstrap, Enchantments.LOOTING), 7),
                                new EnchantmentDefinition(getEnchantment(bootstrap, Enchantments.SMITE), 5),
                                new EnchantmentDefinition(getEnchantment(bootstrap, Enchantments.SHARPNESS), 5)
                        ),
                        ItemTabRegistry.SCENERY_TAB.getId()
                )
        );

        // 下位替代刀：残景之杖（前中期过渡；无专属 SE，剑技为原版垂直波刃）
        bootstrap.register(
                SCENERY_STAFF_WORN,
                new SlashBladeDefinition(
                        // name 与数据包 key 保持一致（翻译键 item.scenerystaff.scenery_staff_worn）
                        SceneryStaff.prefix("scenery_staff_worn"),
                        RenderDefinition.Builder.newInstance()
                                .textureName(SceneryStaff.prefix("model/scenery/scenery_staff.png"))
                                .modelName(SceneryStaff.prefix("model/scenery/scenery.obj"))
                                .effectColor(0x7FA8C0)
                                .build(),
                        PropertiesDefinition.Builder.newInstance()
                                .defaultSwordType(List.of(SwordType.BEWITCHED))
                                .baseAttackModifier(7)
                                .maxDamage(100)
                                .slashArtsType(SlashArtsRegistry.WAVE_EDGE.getId())
                                .build(),
                        List.of(
                                new EnchantmentDefinition(getEnchantment(bootstrap, Enchantments.SHARPNESS), 2),
                                new EnchantmentDefinition(getEnchantment(bootstrap, Enchantments.UNBREAKING), 2)
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
