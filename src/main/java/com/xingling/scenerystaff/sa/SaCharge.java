package com.xingling.scenerystaff.sa;

import com.mojang.serialization.Codec;
import com.xingling.scenerystaff.SceneryStaff;
import com.xingling.scenerystaff.blade.SceneryBlades;
import com.xingling.scenerystaff.registry.SARegistry;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.Map;

/**
 * SA 充能（共鸣解放式）。
 * <p>
 * 两把最终刀的剑技不再「冷却到点就能放」，而是各自攒一条充能：攒满才能放，放完清空。
 * 未攒满时由 {@link SaChargeGate} 把剑技换成内置幻影刃。
 * <p>
 * 充能值存在玩家的 {@link AttachmentType} 上，一次拿到四件事：
 * {@code serialize} 持久化、{@code sync} 自动同步到客户端（HUD 直接读，无需自定义网络包）、
 * 挂玩家身上多人天然隔离、千分比之外的整数点位免浮点误差。
 * <p>
 * <b>释放所需充能不写死在刀上</b>：按 SA 的注册 ID 查 {@link #COST_BY_SA}，
 * 查不到就用 {@link #DEFAULT_COST}。以后要给别的刀配不同数值，加一行映射即可。
 * 增长规则同理都是类顶部常量。
 */
@EventBusSubscriber(modid = SceneryStaff.MODID)
public final class SaCharge {

    /** 未单独配置的 SA 默认需要多少充能 */
    public static final int DEFAULT_COST = 125;

    /** 各 SA 的释放所需充能（按 SA 注册 ID 配置） */
    private static final Map<ResourceLocation, Integer> COST_BY_SA = Map.of(
            SARegistry.EROSION_DOMAIN.getId(), 125,
            SARegistry.GIANT_SWORD.getId(), 125);

    /** 自然回复检查间隔（tick）与每次增量 */
    private static final int NATURAL_INTERVAL_TICKS = 20;
    private static final int NATURAL_GAIN = 1;      // 约 0.8%/秒（按 125 计）

    /** 命中敌人 */
    private static final int GAIN_ON_HIT = 3;       // 2.4%：约 42 次命中充满
    /** 击杀敌人 */
    private static final int GAIN_ON_KILL = 15;     // 12%
    /** 受到伤害 */
    private static final int GAIN_ON_HURT = 2;

    public static final DeferredRegister<AttachmentType<?>> REGISTRY =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, SceneryStaff.MODID);

    private static final DeferredHolder<AttachmentType<?>, AttachmentType<Integer>> DOMAIN_TYPE =
            register("domain_charge");
    private static final DeferredHolder<AttachmentType<?>, AttachmentType<Integer>> GIANT_SWORD_TYPE =
            register("giant_sword_charge");

    /** 蚀域图标（白色剪影，配色由 {@link Charge#color()} 决定） */
    private static final ResourceLocation DENIA_ICON = SceneryStaff.prefix("textures/gui/sa_denia.png");
    /** 巨剑图标（同上） */
    private static final ResourceLocation AEMEATH_ICON = SceneryStaff.prefix("textures/gui/sa_aemeath.png");

    /** 蚀域（布景之杖） */
    public static final Charge DOMAIN = new Charge(DOMAIN_TYPE, SARegistry.EROSION_DOMAIN.getId(),
            "message.scenerystaff.domain_charging", DENIA_ICON, 0x8A7CFF);

    /** 巨剑（启明星） */
    public static final Charge GIANT_SWORD = new Charge(GIANT_SWORD_TYPE, SARegistry.GIANT_SWORD.getId(),
            "message.scenerystaff.giant_sword_charging", AEMEATH_ICON, 0x9FD8FF);

    private SaCharge() {
    }

    private static DeferredHolder<AttachmentType<?>, AttachmentType<Integer>> register(String name) {
        return REGISTRY.register(name, () -> AttachmentType.builder(() -> 0)
                .serialize(Codec.INT)
                .sync(ByteBufCodecs.VAR_INT)
                .build());
    }

    /**
     * 一条充能条：它需要什么、怎么显示，全部收在这里。
     * <p>
     * 把<b>表现数据</b>（图标、配色）也放进能力定义，是为了让消费方（HUD）不必再判断
     * 「哪把刀配哪个图标」——HUD 只负责把拿到的东西画出来。这样新增一把刀时，
     * 只要在 {@link SARegistry} 注册 SA、在这里加一条 {@code Charge}（上限非 125 时再补一行
     * {@link #COST_BY_SA}），HUD 一行都不用改。
     *
     * @param type        玩家附件（持久化 + 自动同步）
     * @param saId        SA 的注册 ID，用于查表决定所需充能
     * @param chargingKey 未充满时的提示文本键
     * @param icon        HUD 图标（白色剪影贴图，会按 {@code color} 之外的自身 alpha 绘制）
     * @param color       HUD 配色（进度弧、轨道高亮、充满泛光）
     */
    public record Charge(DeferredHolder<AttachmentType<?>, AttachmentType<Integer>> type,
                         ResourceLocation saId, String chargingKey,
                         ResourceLocation icon, int color) {

        /** 本条充能的释放所需值 */
        public int cost() {
            return costFor(saId);
        }
    }

    /** 按 SA 注册 ID 取所需充能；未配置则用 {@link #DEFAULT_COST} */
    public static int costFor(ResourceLocation saId) {
        return COST_BY_SA.getOrDefault(saId, DEFAULT_COST);
    }

    /** 按 SA 注册 ID 找对应的充能条；不是本模组的 SA 时返回 {@code null} */
    public static Charge forSa(ResourceLocation saId) {
        if (DOMAIN.saId().equals(saId)) {
            return DOMAIN;
        }
        if (GIANT_SWORD.saId().equals(saId)) {
            return GIANT_SWORD;
        }
        return null;
    }

    // ------------------------------------------------------------------ 读写

    /** 取充能值（0..cost）。客户端同样可读——附件已自动同步 */
    public static int get(Player player, Charge charge) {
        return Mth.clamp(player.getData(charge.type().get()), 0, charge.cost());
    }

    /** 是否已充满 */
    public static boolean isFull(Player player, Charge charge) {
        return get(player, charge) >= charge.cost();
    }

    /** 充能进度 0..1（HUD 用） */
    public static float ratio(Player player, Charge charge) {
        return get(player, charge) / (float) charge.cost();
    }

    /** 百分比整数（提示文本用） */
    public static int percent(Player player, Charge charge) {
        return get(player, charge) * 100 / charge.cost();
    }

    /** 增加充能（服务端；自动钳制到本条的上限） */
    public static void add(Player player, Charge charge, int amount) {
        if (player.level().isClientSide()) {
            return;
        }
        int current = get(player, charge);
        int next = Mth.clamp(current + amount, 0, charge.cost());
        if (next != current) {
            player.setData(charge.type().get(), next);
        }
    }

    /** 清空充能（释放 SA 成功时调用） */
    public static void clear(Player player, Charge charge) {
        if (!player.level().isClientSide()) {
            player.setData(charge.type().get(), 0);
        }
    }

    /**
     * 取当前主手刀对应的充能条；不是本模组的两把最终刀时返回 {@code null}。
     * <p>
     * 充能只涨在「手上正拿着的那把」上——与鸣潮按角色各自攒能量一致。
     */
    public static Charge heldCharge(Player player) {
        ItemStack stack = player.getMainHandItem();
        if (SceneryBlades.isFinalBlade(stack)) {
            return DOMAIN;
        }
        if (SceneryBlades.isQimingxing(stack)) {
            return GIANT_SWORD;
        }
        return null;
    }

    // ------------------------------------------------------------------ 增长

    /**
     * 自然回复。
     * <p>
     * 注：回音频段的强化效果已改为暴击率/暴击伤害（见 {@code WavebandUpgrade} 与
     * {@code BladeCritHandler}），因此这里不再有充能速度加成。
     */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide() || player.tickCount % NATURAL_INTERVAL_TICKS != 0) {
            return;
        }
        Charge charge = heldCharge(player);
        if (charge != null && !isFull(player, charge)) {
            add(player, charge, NATURAL_GAIN);
        }
    }

    /** 命中敌人 */
    @SubscribeEvent
    public static void onAttack(AttackEntityEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide()) {
            return;
        }
        Charge charge = heldCharge(player);
        if (charge != null) {
            add(player, charge, GAIN_ON_HIT);
        }
    }

    /** 击杀敌人（只给玩家击杀计入） */
    @SubscribeEvent
    public static void onKill(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof Player player) || player.level().isClientSide()) {
            return;
        }
        Charge charge = heldCharge(player);
        if (charge != null) {
            add(player, charge, GAIN_ON_KILL);
        }
    }

    /** 受到伤害 */
    @SubscribeEvent
    public static void onHurt(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide()) {
            return;
        }
        Charge charge = heldCharge(player);
        if (charge != null) {
            add(player, charge, GAIN_ON_HURT);
        }
    }
}
