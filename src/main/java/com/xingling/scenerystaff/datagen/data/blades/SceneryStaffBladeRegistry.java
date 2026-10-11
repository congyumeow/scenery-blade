package com.xingling.scenerystaff.datagen.data.blades;

import com.xingling.scenerystaff.SceneryStaff;
import com.xingling.scenerystaff.blade.SceneryIds;
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
            ResourceKey.create(SlashBladeDefinition.REGISTRY_KEY, SceneryIds.SCENERY_STAFF);

    /**
     * 下位替代刀：残景之杖。
     * 与最终刀同一外观系列、同一合成模板，但材料降级、数值削弱、不带专属 SE，
     * 剑技使用 SlashBlade 自带垂直波刃，作为前中期过渡刀使用。
     */
    public static final ResourceKey<SlashBladeDefinition> SCENERY_STAFF_WORN =
            ResourceKey.create(SlashBladeDefinition.REGISTRY_KEY, SceneryIds.SCENERY_STAFF_WORN);

    /**
     * 新刀：启明星。
     * 使用外部导入的模型与贴图（assets/scenerystaff/model/qimingxing/），
     * 与布景之杖系列没有剧情/合成上的关联，作为一把独立的命名刀注册。
     */
    public static final ResourceKey<SlashBladeDefinition> QIMINGXING =
            ResourceKey.create(SlashBladeDefinition.REGISTRY_KEY, SceneryIds.QIMINGXING);

    /**
     * 启明星线的下位刀：启明星。
     * <p>
     * 与上位刀「永远的启明星」共用同一套模型（{@code qimingxing.obj}），但用原先那张贴图
     * {@code qimingxing.png}（上位刀改用 {@code qimingxing_extra.png}）；
     * 数值、剑技、附魔都按「残景之杖之于布景之杖」的同一比例下调，作为前中期过渡刀。
     * <p>
     * ID 用 {@code qimingxing_worn}（对应 {@code scenery_staff_worn} 的命名习惯），
     * 而上位刀沿用 {@code qimingxing} 这个 ID 不变——改名只动显示名，避免已存档的刀失效。
     */
    public static final ResourceKey<SlashBladeDefinition> QIMINGXING_WORN =
            ResourceKey.create(SlashBladeDefinition.REGISTRY_KEY, SceneryIds.QIMINGXING_WORN);

    /**
     * 基础刀：潮汐。
     * <p>
     * 模型与贴图来自拔刀剑：重锋的解包资源（assets/scenerystaff/model/chaoxi/），
     * 分组名本就是 SlashBlade 规范（blade/sheath/item_blade/...），未做分组改动。
     * 作为合成下位刀「残景之杖」的中间材料。
     * <p>
     * <b>必须带附魔</b>：实测 SlashBlade 判定「妖刀」与附魔相关——不带附魔时新刀会显示为「封」，
     * 且不享受妖刀特性（离开主手回复耐久）。本模组另外三把刀都带附魔，这里对齐给
     * 力量 II + 耐久 III。
     */
    public static final ResourceKey<SlashBladeDefinition> CHAOXI =
            ResourceKey.create(SlashBladeDefinition.REGISTRY_KEY, SceneryIds.CHAOXI);

    public static void registerAll(BootstrapContext<SlashBladeDefinition> bootstrap) {
        bootstrap.register(
                SCENERY_STAFF,
                new SlashBladeDefinition(
                        // name 与数据包 key 保持一致（决定翻译键 item.scenerystaff.scenery_staff）
                        SceneryIds.SCENERY_STAFF,
                        RenderDefinition.Builder.newInstance()
                                .textureName(SceneryStaff.prefix("model/scenery/scenery_staff.png"))
                                .modelName(SceneryStaff.prefix("model/scenery/scenery.obj"))
                                .effectColor(0xAABBCC)
                                .build(),
                        PropertiesDefinition.Builder.newInstance()
                                .defaultSwordType(List.of(SwordType.BEWITCHED))
                                .baseAttackModifier(20)
                                .maxDamage(500)
                                .addSpecialEffect(SERegistry.POLYMERIZATION.getId())   // 添加SE
                                .slashArtsType(SARegistry.EROSION_DOMAIN.getId())      // 设置SA
                                .build(),
                        List.of(
                                new EnchantmentDefinition(getEnchantment(bootstrap, Enchantments.POWER), 10),
                                new EnchantmentDefinition(getEnchantment(bootstrap, Enchantments.UNBREAKING), 3),
                                new EnchantmentDefinition(getEnchantment(bootstrap, Enchantments.LOOTING), 7),
                                new EnchantmentDefinition(getEnchantment(bootstrap, Enchantments.SMITE), 10),
                                new EnchantmentDefinition(getEnchantment(bootstrap, Enchantments.SHARPNESS), 5)
                        ),
                        ItemTabRegistry.SCENERY_TAB.getId()
                )
        );

        // 下位替代刀：残景之杖
        bootstrap.register(
                SCENERY_STAFF_WORN,
                new SlashBladeDefinition(
                        // name 与数据包 key 保持一致（翻译键 item.scenerystaff.scenery_staff_worn）
                        SceneryIds.SCENERY_STAFF_WORN,
                        RenderDefinition.Builder.newInstance()
                                .textureName(SceneryStaff.prefix("model/scenery/scenery.png"))
                                .modelName(SceneryStaff.prefix("model/scenery/scenery.obj"))
                                .effectColor(0x7FA8C0)
                                .build(),
                        PropertiesDefinition.Builder.newInstance()
                                .defaultSwordType(List.of(SwordType.BEWITCHED))
                                .baseAttackModifier(10)
                                .maxDamage(200)
                                .slashArtsType(SlashArtsRegistry.WAVE_EDGE.getId())
                                .build(),
                        List.of(
                                new EnchantmentDefinition(getEnchantment(bootstrap, Enchantments.SHARPNESS), 3),
                                new EnchantmentDefinition(getEnchantment(bootstrap, Enchantments.UNBREAKING), 3),
                                new EnchantmentDefinition(getEnchantment(bootstrap, Enchantments.POWER), 5),
                                new EnchantmentDefinition(getEnchantment(bootstrap, Enchantments.SMITE), 5)
                        ),
                        ItemTabRegistry.SCENERY_TAB.getId()
                )
        );

        // 上位刀：永远的启明星
        bootstrap.register(
                QIMINGXING,
                new SlashBladeDefinition(
                        // name 与数据包 key 保持一致（翻译键 item.scenerystaff.qimingxing → 永远的启明星）
                        SceneryIds.QIMINGXING,
                        RenderDefinition.Builder.newInstance()
                                .textureName(SceneryStaff.prefix("model/qimingxing/qimingxing_extra.png"))
                                .modelName(SceneryStaff.prefix("model/qimingxing/qimingxing.obj"))
                                .effectColor(0xC8E8FF)
                                .build(),
                        PropertiesDefinition.Builder.newInstance()
                                .defaultSwordType(List.of(SwordType.BEWITCHED))
                                .baseAttackModifier(15)
                                .maxDamage(350)
                                .addSpecialEffect(SERegistry.POLYMERIZATION.getId())   // 永远的启明星命中同样累积聚爆层数（并享有 13 层上限）
                                .slashArtsType(SARegistry.GIANT_SWORD.getId())        // 剑技：永远的启明星（召唤巨剑）
                                .build(),
                        List.of(
                                new EnchantmentDefinition(getEnchantment(bootstrap, Enchantments.POWER), 10),
                                new EnchantmentDefinition(getEnchantment(bootstrap, Enchantments.UNBREAKING), 3),
                                new EnchantmentDefinition(getEnchantment(bootstrap, Enchantments.LOOTING), 7),
                                new EnchantmentDefinition(getEnchantment(bootstrap, Enchantments.SMITE), 10),
                                new EnchantmentDefinition(getEnchantment(bootstrap, Enchantments.SHARPNESS), 5)
                        ),
                        ItemTabRegistry.SCENERY_TAB.getId()
                )
        );

        // 下位刀：启明星
        bootstrap.register(
                QIMINGXING_WORN,
                new SlashBladeDefinition(
                        // 翻译键 item.scenerystaff.qimingxing_worn → 启明星
                        SceneryIds.QIMINGXING_WORN,
                        RenderDefinition.Builder.newInstance()
                                .textureName(SceneryStaff.prefix("model/qimingxing/qimingxing_base.png"))
                                .modelName(SceneryStaff.prefix("model/qimingxing/qimingxing.obj"))
                                .effectColor(0xA8C8E0)   // 比上位刀略暗的青白
                                .build(),
                        PropertiesDefinition.Builder.newInstance()
                                .defaultSwordType(List.of(SwordType.BEWITCHED))
                                .baseAttackModifier(8)
                                .maxDamage(180)
                                .slashArtsType(SlashArtsRegistry.WAVE_EDGE.getId())   // 尚不能召唤巨剑，只挥波刃
                                .build(),
                        List.of(
                                new EnchantmentDefinition(getEnchantment(bootstrap, Enchantments.SHARPNESS), 3),
                                new EnchantmentDefinition(getEnchantment(bootstrap, Enchantments.UNBREAKING), 3),
                                new EnchantmentDefinition(getEnchantment(bootstrap, Enchantments.POWER), 5),
                                new EnchantmentDefinition(getEnchantment(bootstrap, Enchantments.SMITE), 5)
                        ),
                        ItemTabRegistry.SCENERY_TAB.getId()
                )
        );

        // 基础刀：潮汐（下位刀「残景之杖」的合成材料）
        bootstrap.register(
                CHAOXI,
                new SlashBladeDefinition(
                        SceneryIds.CHAOXI,
                        RenderDefinition.Builder.newInstance()
                                .textureName(SceneryStaff.prefix("model/chaoxi/chaoxi.png"))
                                .modelName(SceneryStaff.prefix("model/chaoxi/chaoxi.obj"))
                                .effectColor(0x3A6EA5)   // 潮汐的深蓝
                                .build(),
                        PropertiesDefinition.Builder.newInstance()
                                .defaultSwordType(List.of(SwordType.BEWITCHED))
                                .baseAttackModifier(7)
                                .maxDamage(120)
                                .slashArtsType(SlashArtsRegistry.WAVE_EDGE.getId())   // 波刃，契合"潮汐"
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
