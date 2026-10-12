package com.xingling.scenerystaff.blade;

import com.xingling.scenerystaff.SceneryStaff;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * 本模组命名刀的识别。
 * <p>
 * 全部判定都来自下面的 {@link #SPECS} <b>单一事实表</b>：一把刀属于哪条线、是上位还是下位、
 * 有没有暴击、能不能被回音频段强化，都写成数据，谓词只做查表派生。
 * <p>
 * 刀的注册名一律取自 {@link SceneryIds}。注意本类里同时出现
 * {@code Line.QIMINGXING}（枚举常量，表示「启明星线」）与 {@code SceneryIds.QIMINGXING}
 * （{@code ResourceLocation}，表示「永远的启明星」这把刀），二者同名不同物，务必带前缀书写。
 */
public final class SceneryBlades {

    /** 两条刀线：布景之杖线、启明星线 */
    public enum Line { SCENERY, QIMINGXING }

    /** 档位：上位（终盘刀）与下位 */
    public enum Tier { FINAL, WORN }

    /**
     * 一把命名刀的完整身份。
     *
     * @param id       刀的注册 ID
     * @param line     属于哪条线
     * @param tier     上位还是下位
     * @param crit     是否带暴击特性（目前只有两把上位刀有）
     * @param waveband 是否可被回音频段强化（四把都可以）
     */
    public record Spec(ResourceLocation id, Line line, Tier tier, boolean crit, boolean waveband) {
    }

    /** 全部命名刀。新增一把刀＝在 {@link SceneryIds} 加常量、在这里加一行，其余谓词自动跟着变。 */
    private static final List<Spec> SPECS = List.of(
            new Spec(SceneryIds.SCENERY_STAFF, Line.SCENERY, Tier.FINAL, true, true),
            new Spec(SceneryIds.SCENERY_STAFF_WORN, Line.SCENERY, Tier.WORN, false, true),
            new Spec(SceneryIds.QIMINGXING, Line.QIMINGXING, Tier.FINAL, true, true),
            new Spec(SceneryIds.QIMINGXING_WORN, Line.QIMINGXING, Tier.WORN, false, true));

    private static final Map<ResourceLocation, Spec> BY_ID = SPECS.stream()
            .collect(Collectors.toUnmodifiableMap(Spec::id, spec -> spec));

    private SceneryBlades() {
    }

    // ------------------------------------------------------------------ 基础查询

    /** 取这把刀的档案；不是拔刀剑、或者不是本模组的命名刀时返回 {@code null}。 */
    public static Spec specOf(ItemStack stack) {
        ResourceLocation id = bladeIdOf(stack);
        return id == null ? null : BY_ID.get(id);
    }

    /**
     * 取刀的注册 ID；不是拔刀剑时返回 {@code null}。
     * <p>
     * {@link ItemSlashBlade#getBladeId(ItemStack)} 由刀自身的翻译键解析而来，对无状态的刀会退回物品注册名，
     * 因此不会把别的模组的拔刀剑误判成本模组的刀。
     */
    private static ResourceLocation bladeIdOf(ItemStack stack) {
        return stack.getItem() instanceof ItemSlashBlade blade ? blade.getBladeId(stack) : null;
    }

    private static boolean is(ItemStack stack, ResourceLocation id) {
        return id.equals(bladeIdOf(stack));
    }

    private static Line lineOf(ItemStack stack) {
        Spec spec = specOf(stack);
        return spec == null ? null : spec.line();
    }

    // ------------------------------------------------------------------ 线

    /** 是否为「布景之杖」线（上位或下位）。注意<b>不含</b>启明星线。 */
    public static boolean isSceneryBlade(ItemStack stack) {
        return lineOf(stack) == Line.SCENERY;
    }

    /** 是否为「启明星」线（上位或下位） */
    public static boolean isQimingxingBlade(ItemStack stack) {
        return lineOf(stack) == Line.QIMINGXING;
    }

    // ------------------------------------------------------------------ 具体刀

    /** 是否为布景之杖线的上位刀「布景之杖」（唯一带侵蚀领域 SA 的那把） */
    public static boolean isFinalBlade(ItemStack stack) {
        return is(stack, SceneryIds.SCENERY_STAFF);
    }

    /**
     * 是否为启明星线的上位刀「永远的启明星」。
     * <p>
     * 巨剑剑技、聚爆 13 层上限、暴击特性都只认这一把；下位刀见 {@link #isQimingxingWorn(ItemStack)}。
     */
    public static boolean isQimingxing(ItemStack stack) {
        return is(stack, SceneryIds.QIMINGXING);
    }

    /** 是否为启明星线的下位刀「启明星」 */
    public static boolean isQimingxingWorn(ItemStack stack) {
        return is(stack, SceneryIds.QIMINGXING_WORN);
    }

    /** 是否为基础刀「潮汐」 */
    public static boolean isChaoxi(ItemStack stack) {
        return is(stack, SceneryIds.CHAOXI);
    }

    // ------------------------------------------------------------------ 档位与特性

    /**
     * 是否为「上位档」：布景之杖 / 永远的启明星。
     * <p>
     * 铁魔法联动按这一档给双倍法杖属性，下位档（残景之杖 / 启明星）给基础值。
     */
    public static boolean isTopTier(ItemStack stack) {
        Spec spec = specOf(stack);
        return spec != null && spec.tier() == Tier.FINAL;
    }

    /**
     * 是否为「带有暴击特性的刀」：两把上位刀。
     * <p>
     * 由 {@link Spec#crit()} 独立决定——<b>不是</b> {@link #isTopTier} 的同义词。
     */
    public static boolean hasCrit(ItemStack stack) {
        Spec spec = specOf(stack);
        return spec != null && spec.crit();
    }

    /**
     * 是否为「可被回音频段强化的刀」：四把命名刀都算。
     * <p>
     * 具体哪条回音频段配哪条线由 {@code WavebandUpgrade#matches} 判定。
     */
    public static boolean isWavebandUpgradable(ItemStack stack) {
        Spec spec = specOf(stack);
        return spec != null && spec.waveband();
    }

    // ------------------------------------------------------------------ 持有与外观

    /**
     * 主手<b>或副手</b>是否拿着符合条件的刀。
     * <p>
     * 注意并非所有地方都用双手判定：暴击与 SA 充能刻意只认<b>主手</b>
     * （攻击与 HUD 显示都以主手为准），那两处不应改用本方法。
     *
     * @param entity 玩家或任意生物，允许为 {@code null}
     * @param blade  刀的判定，通常传 {@link #isFinalBlade} / {@link #isQimingxing} 之类
     */
    public static boolean isHolding(LivingEntity entity, Predicate<ItemStack> blade) {
        return entity != null
                && (blade.test(entity.getMainHandItem()) || blade.test(entity.getOffhandItem()));
    }

    /**
     * 是否为「模型来自本模组」的刀。
     * <p>
     * <b>与 {@link #isSceneryBlade} 不是一回事</b>，两者名字相近但语义不同，用错会漏掉启明星线：
     * <ul>
     *   <li>{@link #isSceneryBlade} —— 按刀的<b>注册 ID</b> 判定，<b>只认布景之杖线两把</b>；</li>
     *   <li>本方法 —— 按刀的<b>模型命名空间</b>判定，<b>本模组四把刀都算</b>。</li>
     * </ul>
     * 待机刀渲染拦截要的是「这是不是我们的刀」，所以用本方法（见 {@code LayerMainBladeMixin}）。
     */
    public static boolean isOurModel(ItemStack stack) {
        return BladeStates.of(stack)
                .flatMap(state -> state.getModel())
                .map(model -> model.getNamespace().equals(SceneryStaff.MODID))
                .orElse(false);
    }
}
