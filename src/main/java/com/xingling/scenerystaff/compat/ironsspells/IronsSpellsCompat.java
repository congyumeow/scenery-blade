package com.xingling.scenerystaff.compat.ironsspells;

import com.xingling.scenerystaff.SceneryStaff;
import com.xingling.scenerystaff.blade.SceneryBlades;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ItemAttributeModifierEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * 与「铁魔法」（Iron's Spells 'n Spellbooks）的联动：让本模组四把命名刀像法杖一样提供法术属性，
 * 并各自「自带」一发法术——布景之杖线为火焰箭，启明星线为烈焰冲锋。
 *
 * <h2>为什么这个类必须手动注册</h2>
 * 本类直接引用 {@code io.redspace.ironsspellbooks.*}。如果给它加 {@code @Mod.EventBusSubscriber}，
 * Forge 扫描时会<b>加载</b>它，玩家没装铁魔法时就会以 {@code NoClassDefFoundError} 崩游戏——
 * 方法体里的判断救不了（类加载发生在进入方法之前）。因此由 {@link SceneryStaff} 在
 * {@code ModList.get().isLoaded("irons_spellbooks")} 守卫内调用 {@link #register()}，
 * 类只会在铁魔法存在时才被加载。
 *
 * <h2>三件事</h2>
 * <ol>
 *   <li><b>法杖属性</b>：{@code ItemAttributeModifierEvent} 按物品栈追加 4 条铁魔法属性
 *       （火焰法术强度 / 通用法术强度 / 法力回复 / 最大法力）。上位档
 *       （布景之杖 / 永远的启明星）数值翻倍，下位档（残景之杖 / 启明星）为基础值。</li>
 *   <li><b>制造时自带法术</b>：{@code ItemCraftedEvent} 里写入法术容器。</li>
 *   <li><b>兜底补齐</b>：玩家 tick（END 阶段）每秒扫一次背包，<b>只在容器里确实没有法术时</b>补写，
 *       以覆盖创造栏 / {@code /give} / 交易 / 拾取等非合成途径。</li>
 * </ol>
 *
 * <h2>与奥术铁砧共存（关键约束）</h2>
 * 铁魔法奥术铁砧的「灌注」分支判定 {@code Utils.canImbue(base)}：只要物品栈上已有法术容器
 * （且不是卷轴/法术书）就允许灌注，用卷轴把刀上的法术换掉；容器满时它会退回到第 0 格替换，
 * 所以 1 格容器也能换成<b>别的</b>法术。也正因如此，<b>本类绝不能把等级强制写回</b>——
 * 一旦巡检发现「等级不对」就改写，玩家在铁砧上的改动会被每秒覆盖。故所有写入都遵循
 * 「已存在就绝不触碰」。
 */
public final class IronsSpellsCompat {

    /**
     * 自带法术<b>按线区分</b>：
     * <ul>
     *   <li>布景之杖线（布景之杖 / 残景之杖）→ <b>火焰箭</b> {@code irons_spellbooks:firebolt}；</li>
     *   <li>启明星线（永远的启明星 / 启明星）→ <b>烈焰冲锋</b> {@code irons_spellbooks:burning_dash}。</li>
     * </ul>
     * 等级上限不写死：{@link AbstractSpell#getMaxLevel()} 由铁魔法的服务端配置决定，
     * 不同版本可能不同，写入前一律按法术自身的上下限 clamp，避免容器里出现越界等级。
     */
    private static final ResourceLocation FIREBOLT_ID =
            new ResourceLocation("irons_spellbooks", "firebolt");
    private static final ResourceLocation BURNING_DASH_ID =
            new ResourceLocation("irons_spellbooks", "burning_dash");

    /** 这把刀属于哪条线、该自带哪个法术 */
    private static ResourceLocation spellIdFor(ItemStack stack) {
        return SceneryBlades.isQimingxingBlade(stack) ? BURNING_DASH_ID : FIREBOLT_ID;
    }

    /**
     * 四条属性的修饰符 ID（命名空间为本模组，避免与铁魔法自身冲突）。
     * <p>
     * 1.20.1 的 {@link AttributeModifier} 仍以 {@link UUID} 作身份（1.21 才改成 {@code ResourceLocation}），
     * 因此这里把 ResourceLocation 稳定地映射成 name-based UUID：既保留「命名空间 + 路径」的可读来源，
     * 又保证同一个修饰符每次拿到同一个 UUID。
     */
    private static final UUID FIRE_POWER_UUID = modifierUuid("iss_fire_spell_power");
    private static final UUID SPELL_POWER_UUID = modifierUuid("iss_spell_power");
    private static final UUID MANA_REGEN_UUID = modifierUuid("iss_mana_regen");
    private static final UUID MAX_MANA_UUID = modifierUuid("iss_max_mana");

    private static final String FIRE_POWER_NAME = SceneryStaff.MODID + ".iss_fire_spell_power";
    private static final String SPELL_POWER_NAME = SceneryStaff.MODID + ".iss_spell_power";
    private static final String MANA_REGEN_NAME = SceneryStaff.MODID + ".iss_mana_regen";
    private static final String MAX_MANA_NAME = SceneryStaff.MODID + ".iss_max_mana";

    /**
     * <p>
     * 三条百分比属性用 {@code ADD_MULTIPLIED_BASE}——它们的属性基础值都是 1.0，
     * 所以 0.15 就是 +15%，与铁魔法自己法杖 {@code StaffTier} 的写法一致；
     * {@code max_mana} 基础值是 100、用 {@code ADD_VALUE} 加绝对值。
     * <p>
     * 注意铁魔法是<b>相乘</b>结算的（{@code getEntityPowerMultiplier = 配置倍率 × 通用法术强度 × 学派法术强度}），
     * 所以最终刀对火系法术的实际加成是 {@code 1.10 × 1.30 = 1.43}（+43%），不是 +40%。
     */
    private static final double FIRE_POWER = 0.15D;
    private static final double SPELL_POWER = 0.05D;
    private static final double MANA_REGEN = 0.25D;
    private static final double MAX_MANA = 150.0D;

    // 自带法术的期望等级（写入前按法术自身上下限 clamp）
    private static final int SPELL_LEVEL_WORN = 5;
    private static final int SPELL_LEVEL_FINAL = 10;

    /** 兜底巡检间隔（tick）：20 tick = 1 秒 */
    private static final int ENSURE_INTERVAL_TICKS = 20;

    private IronsSpellsCompat() {
    }

    /** 由 {@code ResourceLocation} 稳定生成 name-based UUID（同一路径永远同一个 UUID） */
    private static UUID modifierUuid(String path) {
        return UUID.nameUUIDFromBytes(
                (SceneryStaff.prefix(path).toString()).getBytes(StandardCharsets.UTF_8));
    }

    /** 只允许在铁魔法存在时调用：调用即触发本类加载，并挂上三个监听 */
    public static void register() {
        MinecraftForge.EVENT_BUS.register(IronsSpellsCompat.class);
        SceneryStaff.LOGGER.info("[铁魔法联动] 已启用：法杖属性 + 自带法术（布景之杖线火焰箭 / 启明星线烈焰冲锋；下位档 {} 级 / 上位档 {} 级）",
                SPELL_LEVEL_WORN, SPELL_LEVEL_FINAL);
    }

    // ------------------------------------------------------------------ 1) 法杖属性

    @SubscribeEvent
    public static void onItemAttributeModifier(ItemAttributeModifierEvent event) {
        ItemStack stack = event.getItemStack();
        if (!SceneryBlades.isWavebandUpgradable(stack)) {
            return;
        }

        // 关键：1.20.1 的 ItemAttributeModifierEvent 是「按槽位各触发一次」的
        // （事件自带 getSlotType()，addModifier 只收 (属性, 修饰符) 没有槽位参数），
        // 不加判断就会把法杖属性同时挂到主手、副手、头、胸、腿、脚六个槽上——
        // Tooltip 里会多出「在副手时 / 戴在头上时 / 穿在身上时 / 穿在腿上时 / 穿在脚上时」。
        // 1.21 那边用的是 EquipmentSlotGroup.MAINHAND，只落在主手，
        // 所以这里同样只放行 MAINHAND，保持与 1.21.1 一致的外观。
        if (event.getSlotType() != EquipmentSlot.MAINHAND) {
            return;
        }

        // 上位档（布景之杖 / 永远的启明星）翻倍，下位档（残景之杖 / 启明星）为基础值
        double mul = SceneryBlades.isTopTier(stack) ? 2.0D : 1.0D;

        event.addModifier(AttributeRegistry.FIRE_SPELL_POWER.get(),
                new AttributeModifier(FIRE_POWER_UUID, FIRE_POWER_NAME, FIRE_POWER * mul,
                        AttributeModifier.Operation.MULTIPLY_BASE));
        event.addModifier(AttributeRegistry.SPELL_POWER.get(),
                new AttributeModifier(SPELL_POWER_UUID, SPELL_POWER_NAME, SPELL_POWER * mul,
                        AttributeModifier.Operation.MULTIPLY_BASE));
        event.addModifier(AttributeRegistry.MANA_REGEN.get(),
                new AttributeModifier(MANA_REGEN_UUID, MANA_REGEN_NAME, MANA_REGEN * mul,
                        AttributeModifier.Operation.MULTIPLY_BASE));
        event.addModifier(AttributeRegistry.MAX_MANA.get(),
                new AttributeModifier(MAX_MANA_UUID, MAX_MANA_NAME, MAX_MANA * mul,
                        AttributeModifier.Operation.ADDITION));
    }

    // ------------------------------------------------------------------ 2) 制造时自带

    @SubscribeEvent
    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        ensureInnateSpell(event.getCrafting());
    }

    // ------------------------------------------------------------------ 3) 兜底补齐

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Player player = event.player;
        if (player.level().isClientSide() || player.tickCount % ENSURE_INTERVAL_TICKS != 0) {
            return;
        }
        // Inventory 的 0~35 是主背包、36~39 护甲、40 副手，正好覆盖双手
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ensureInnateSpell(player.getInventory().getItem(i));
        }
    }

    /**
     * 让刀「自带」其所属线的法术（布景之杖线火焰箭 / 启明星线烈焰冲锋），并顺带迁移早期写错的容器。
     * <p>
     * 判断「有无法术」刻意<b>不</b>用 {@code isEmpty()}：旧版铁魔法里 {@code create(...)} 造出来的容器
     * {@code activeSpellCount} 恒为 0，用它就会永远判定为空、每秒重写一次，把玩家在奥术铁砧上改好的法术冲掉。
     * 这里改为看真正的第一个槽（新旧两代的具体取值由 {@link SpellContainers} 负责）。
     */
    private static void ensureInnateSpell(ItemStack stack) {
        if (stack.isEmpty() || !SceneryBlades.isWavebandUpgradable(stack)) {
            return;
        }

        SpellContainers.Ops ops = SpellContainers.ops();
        if (ISpellContainer.isSpellContainer(stack)) {
            ISpellContainer existing = ISpellContainer.get(stack);
            if (existing != null && ops.hasSpell(existing)) {
                // 已有法术 → 原样保留，绝不覆盖（含玩家在铁砧上改过的）。
                // 唯一例外：迁移早期写坏的容器——那时 mustEquip 传成了 true，会让刀拿在手上时反而不进轮盘。
                // 这种组合只可能出自我们那次错误写入：铁魔法自己的默认（getOrCreate）与铁砧改写都保持 false，
                // 且我们只碰单格容器，不会误伤别的物品。
                if (existing.mustEquip() && existing.getMaxSpellCount() == 1) {
                    // 容器里那个法术可能已经被玩家在铁砧上换成别的了，所以直接读第一个槽，
                    // 而不是按本门派法术 ID 去找（找不到时的空值分支反而更容易写错）。
                    SpellContainers.SlotData current = ops.first(existing);
                    if (current != null) {
                        ops.write(stack, current.spell(), current.level(), current.locked());
                        SceneryStaff.LOGGER.info("[铁魔法联动] 已修正刀上法术容器的 mustEquip 标记（保留原有 {} 级法术）",
                                current.level());
                    }
                }
                return;
            }
        }

        AbstractSpell spell = SpellRegistry.getSpell(spellIdFor(stack));
        if (spell == null) {
            return;
        }
        int wanted = SceneryBlades.isTopTier(stack) ? SPELL_LEVEL_FINAL : SPELL_LEVEL_WORN;
        int level = Mth.clamp(wanted, spell.getMinLevel(), spell.getMaxLevel());
        ops.write(stack, spell, level, true);
    }
}
