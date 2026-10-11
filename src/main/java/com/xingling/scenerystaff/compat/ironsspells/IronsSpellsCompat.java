package com.xingling.scenerystaff.compat.ironsspells;

import com.xingling.scenerystaff.SceneryStaff;
import com.xingling.scenerystaff.blade.SceneryBlades;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.ISpellContainerMutable;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.registries.ComponentRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * 与「铁魔法」（Iron's Spells 'n Spellbooks）的联动：让本模组四把命名刀像法杖一样提供法术属性，
 * 并各自「自带」一发法术——布景之杖线为火焰箭，启明星线为烈焰冲锋。
 *
 * <h2>为什么这个类必须手动注册</h2>
 * 本类直接引用 {@code io.redspace.ironsspellbooks.*}。如果给它加 {@code @EventBusSubscriber}，
 * NeoForge 扫描时会<b>加载</b>它，玩家没装铁魔法时就会以 {@code NoClassDefFoundError} 崩游戏——
 * 方法体里的判断救不了（类加载发生在进入方法之前）。因此由 {@link SceneryStaff} 在
 * {@code ModList.get().isLoaded("irons_spellbooks")} 守卫内调用 {@link #register()}，
 * 类只会在铁魔法存在时才被加载。
 *
 * <h2>三件事</h2>
 * <ol>
 *   <li><b>法杖属性</b>：{@code ItemAttributeModifierEvent} 按物品栈追加 4 条铁魔法属性
 *       （火焰法术强度 / 通用法术强度 / 法力回复 / 最大法力）。上位档
 *       （布景之杖 / 永远的启明星）数值翻倍，下位档（残景之杖 / 启明星）为基础值。
 *       事件里传进来的默认修饰符就是 SlashBlade 自己算的攻击力与攻速，我们只是追加，互不冲突。</li>
 *   <li><b>制造时自带法术</b>：{@code ItemCraftedEvent} 里写入法术容器。</li>
 *   <li><b>兜底补齐</b>：{@code PlayerTickEvent.Post} 每秒扫一次背包，<b>只在容器里确实没有法术时</b>补写，
 *       以覆盖创造栏 / {@code /give} / 交易 / 拾取等非合成途径。</li>
 * </ol>
 *
 *
 * 同理，判断「容器里有没有法术」不能用 {@code isEmpty()}：它读的是 {@code activeSlots}，
 * 而 {@code create(...)} 造出来的容器该字段恒为 0，用它会永远判定为空、每秒重写一次。
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
     *   <li>启明星线（永远的启明星 / 启明星）→ <b>烈焰冲锋</b> {@code irons_spellbooks:burning_dash}
     *       （中文名取自铁魔法自带语言文件 {@code spell.irons_spellbooks.burning_dash}）。</li>
     * </ul>
     * 等级上限不写死：{@link AbstractSpell#getMaxLevel()} 由铁魔法的服务端配置决定，
     * 不同版本可能不同，写入前一律按法术自身的上下限 clamp，避免容器里出现越界等级。
     */
    private static final ResourceLocation FIREBOLT_ID =
            ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "firebolt");
    private static final ResourceLocation BURNING_DASH_ID =
            ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "burning_dash");

    /** 这把刀属于哪条线、该自带哪个法术 */
    private static ResourceLocation spellIdFor(ItemStack stack) {
        return SceneryBlades.isQimingxingBlade(stack) ? BURNING_DASH_ID : FIREBOLT_ID;
    }

    /** 四条属性的修饰符 ID（命名空间为本模组，避免与铁魔法自身冲突） */
    private static final ResourceLocation FIRE_POWER_MOD = SceneryStaff.prefix("iss_fire_spell_power");
    private static final ResourceLocation SPELL_POWER_MOD = SceneryStaff.prefix("iss_spell_power");
    private static final ResourceLocation MANA_REGEN_MOD = SceneryStaff.prefix("iss_mana_regen");
    private static final ResourceLocation MAX_MANA_MOD = SceneryStaff.prefix("iss_max_mana");

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

    // 自带烈焰冲锋的期望等级（写入前按法术自身上下限 clamp）
    private static final int SPELL_LEVEL_WORN = 5;
    private static final int SPELL_LEVEL_FINAL = 10;

    /** 兜底巡检间隔（tick）：20 tick = 1 秒 */
    private static final int ENSURE_INTERVAL_TICKS = 20;

    private IronsSpellsCompat() {
    }

    /** 只允许在铁魔法存在时调用：调用即触发本类加载，并挂上三个监听 */
    public static void register() {
        NeoForge.EVENT_BUS.register(IronsSpellsCompat.class);
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
        // 上位档（布景之杖 / 永远的启明星）翻倍，下位档（残景之杖 / 启明星）为基础值
        double mul = SceneryBlades.isTopTier(stack) ? 2.0D : 1.0D;

        event.addModifier(AttributeRegistry.FIRE_SPELL_POWER,
                new AttributeModifier(FIRE_POWER_MOD, FIRE_POWER * mul,
                        AttributeModifier.Operation.ADD_MULTIPLIED_BASE),
                EquipmentSlotGroup.MAINHAND);
        event.addModifier(AttributeRegistry.SPELL_POWER,
                new AttributeModifier(SPELL_POWER_MOD, SPELL_POWER * mul,
                        AttributeModifier.Operation.ADD_MULTIPLIED_BASE),
                EquipmentSlotGroup.MAINHAND);
        event.addModifier(AttributeRegistry.MANA_REGEN,
                new AttributeModifier(MANA_REGEN_MOD, MANA_REGEN * mul,
                        AttributeModifier.Operation.ADD_MULTIPLIED_BASE),
                EquipmentSlotGroup.MAINHAND);
        event.addModifier(AttributeRegistry.MAX_MANA,
                new AttributeModifier(MAX_MANA_MOD, MAX_MANA * mul,
                        AttributeModifier.Operation.ADD_VALUE),
                EquipmentSlotGroup.MAINHAND);
    }

    // ------------------------------------------------------------------ 2) 制造时自带

    @SubscribeEvent
    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        ensureInnateSpell(event.getCrafting());
    }

    // ------------------------------------------------------------------ 3) 兜底补齐

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
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
     * 判断「有无法术」用的是 {@code getSpellAtIndex(0) != SpellData.EMPTY}，<b>刻意不用 {@code isEmpty()}</b>：
     * 后者读的是 {@code activeSlots} 字段，而 {@code ISpellContainer.create(...)} 造出来的容器该字段恒为 0，
     * 用它就会永远判定为空、每秒重写一次，把玩家在奥术铁砧上改好的法术冲掉。
     */
    private static void ensureInnateSpell(ItemStack stack) {
        if (stack.isEmpty() || !SceneryBlades.isWavebandUpgradable(stack)) {
            return;
        }

        if (ISpellContainer.isSpellContainer(stack)) {
            ISpellContainer existing = ISpellContainer.get(stack);
            SpellData current = existing == null ? SpellData.EMPTY : existing.getSpellAtIndex(0);
            if (current != SpellData.EMPTY) {
                // 已有法术 → 通常原样保留（含玩家在铁砧上改过的），绝不覆盖。
                // 唯一例外：迁移早期写坏的容器——那时 mustEquip 传成了 true，会让刀拿在手上时反而不进轮盘。
                // 这种组合只可能出自我们那次错误写入：铁魔法自己的默认（getOrCreate）与铁砧改写都保持 false，
                // 且我们只碰单格容器（getMaxSpellCount()==1），不会误伤别的物品。
                if (existing != null && existing.mustEquip() && existing.getMaxSpellCount() == 1) {
                    writeContainer(stack, current.getSpell(), current.getLevel(), current.isLocked());
                    SceneryStaff.LOGGER.info("[铁魔法联动] 已修正刀上法术容器的 mustEquip 标记（保留原有 {} 级法术）",
                            current.getLevel());
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
        writeContainer(stack, spell, level, true);
    }

    /**
     * 写入（或重写）法术容器。
     * <p>
     * {@code ISpellContainer.create(int, boolean, boolean)} 的形参顺序是
     * <b>(maxSpells, spellWheel, mustEquip)</b>，两个布尔极易写反，含义也反直觉：
     * <ul>
     *   <li>{@code spellWheel = true}：法术进入法术轮盘（{@code SpellSelectionManager.initItem} 要求它为真）。</li>
     *   <li>{@code mustEquip = false}：允许主手/副手。若传 {@code true}，{@code initItem} 会直接
     *       <b>跳过</b>主手与副手——它的真实含义是「只在护甲/饰品栏生效」。铁魔法自己的
     *       {@code createImbuedContainer} 就是给武器传 {@code spellWheel=false, mustEquip=true}，
     *       也就是武器附法根本不进轮盘，只能用 sword 来源施法。</li>
     * </ul>
     * 第 4 个参数 {@code locked}：为 {@code true} 时可在铁砧上用同级或更高等级的同一法术卷轴升阶；
     * 换成别的法术不受它影响（铁魔法在容器满时会退回第 0 格替换）。
     * <p>
     * 这里刻意不用 {@code ISpellContainer.set(stack, ...)}：那个便捷方法直到铁魔法 3.15.0 才加入，
     * 而其字节码就是 {@code stack.set(ComponentRegistry.SPELL_CONTAINER, ...)}，两者完全等价；
     * 直写组件可在全部 1.21.1 版本上工作（3.9.1 起该组件就已存在），不会撞 NoSuchMethodError。
     */
    private static void writeContainer(ItemStack stack, AbstractSpell spell, int level, boolean locked) {
        ISpellContainerMutable mutable = ISpellContainer.create(1, true, false).mutableCopy();
        mutable.addSpellAtIndex(spell, level, 0, locked);
        stack.set(ComponentRegistry.SPELL_CONTAINER, mutable.toImmutable());
    }
}
