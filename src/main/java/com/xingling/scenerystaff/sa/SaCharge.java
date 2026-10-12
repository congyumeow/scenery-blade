package com.xingling.scenerystaff.sa;

import com.xingling.scenerystaff.SceneryStaff;
import com.xingling.scenerystaff.blade.SceneryBlades;
import com.xingling.scenerystaff.network.SyncChargeMessage;
import com.xingling.scenerystaff.registry.SARegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;

/**
 * SA 充能（共鸣解放式）。
 * <p>
 * 两把最终刀的剑技不再「冷却到点就能放」，而是各自攒一条充能：攒满才能放，放完清空。
 * 未攒满时由 {@link SaChargeGate} 把剑技换成内置幻影刃。
 * <p>
 * 充能<b>只由战斗行为累积</b>——命中 {@link #GAIN_ON_HIT} / 击杀 {@link #GAIN_ON_KILL} /
 * 受伤 {@link #GAIN_ON_HURT}，不随时间自然增长；且只在手持对应那把刀时累积。
 * <p>
 * 充能值存在玩家的 {@link PlayerChargeCapability} 上：随玩家存档持久化、多人天然隔离。
 * 客户端 HUD 显示的那份由 {@link SyncChargeMessage} 推送（1.20.1 的能力不会自动同步）。
 * <p>
 * <b>释放所需充能不写死在刀上</b>：按 SA 的注册 ID 查 {@link #COST_BY_SA}，
 * 查不到就用 {@link #DEFAULT_COST}。
 */
@Mod.EventBusSubscriber(modid = SceneryStaff.MODID)
public final class SaCharge {

    /** 未单独配置的 SA 默认需要多少充能 */
    public static final int DEFAULT_COST = 125;

    /** 各 SA 的释放所需充能（按 SA 注册 ID 配置） */
    private static final Map<ResourceLocation, Integer> COST_BY_SA = Map.of(
            SARegistry.EROSION_DOMAIN.getId(), 125,
            SARegistry.GIANT_SWORD.getId(), 125);

    /** 命中敌人 */
    private static final int GAIN_ON_HIT = 3;       // 2.4%：约 42 次命中充满
    /** 击杀敌人 */
    private static final int GAIN_ON_KILL = 15;     // 12%
    /** 受到伤害 */
    private static final int GAIN_ON_HURT = 2;

    /** 蚀域图标（白色剪影，配色由 {@link Charge#color()} 决定） */
    private static final ResourceLocation DENIA_ICON = SceneryStaff.prefix("textures/gui/sa_denia.png");
    /** 巨剑图标（同上） */
    private static final ResourceLocation AEMEATH_ICON = SceneryStaff.prefix("textures/gui/sa_aemeath.png");

    /** 蚀域（布景之杖） */
    public static final Charge DOMAIN = new Charge(SARegistry.EROSION_DOMAIN.getId(),
            "message.scenerystaff.domain_charging", DENIA_ICON, 0x8A7CFF);

    /** 巨剑（启明星） */
    public static final Charge GIANT_SWORD = new Charge(SARegistry.GIANT_SWORD.getId(),
            "message.scenerystaff.giant_sword_charging", AEMEATH_ICON, 0x9FD8FF);

    private SaCharge() {
    }

    /**
     * 一条充能条：它需要什么、怎么显示，全部收在这里。
     * <p>
     * 把<b>表现数据</b>（图标、配色）也放进能力定义，是为了让消费方（HUD）不必再判断
     * 「哪把刀配哪个图标」——HUD 只负责把拿到的东西画出来。
     *
     * @param saId        SA 的注册 ID，用于查表决定所需充能
     * @param chargingKey 未充满时的提示文本键
     * @param icon        HUD 图标（白色剪影贴图）
     * @param color       HUD 配色（进度弧、轨道高亮、充满泛光）
     */
    public record Charge(ResourceLocation saId, String chargingKey, ResourceLocation icon, int color) {

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

    /** 取充能值（0..cost） */
    public static int get(Player player, Charge charge) {
        return Mth.clamp(rawGet(player, charge), 0, charge.cost());
    }

    private static int rawGet(Player player, Charge charge) {
        return PlayerChargeCapability.get(player)
                .map(data -> charge == DOMAIN ? data.getDomainCharge() : data.getGiantSwordCharge())
                .orElse(0);
    }

    private static void rawSet(Player player, Charge charge, int value) {
        PlayerChargeCapability.get(player).ifPresent(data -> {
            if (charge == DOMAIN) {
                data.setDomainCharge(value);
            } else {
                data.setGiantSwordCharge(value);
            }
        });
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

    /** 增加充能（服务端；自动钳制到本条的上限，并把新值推给该玩家） */
    public static void add(Player player, Charge charge, int amount) {
        if (player.level().isClientSide()) {
            return;
        }
        int current = get(player, charge);
        int next = Mth.clamp(current + amount, 0, charge.cost());
        if (next != current) {
            rawSet(player, charge, next);
            push(player);
        }
    }

    /** 清空充能（释放 SA 成功时调用） */
    public static void clear(Player player, Charge charge) {
        if (player.level().isClientSide()) {
            return;
        }
        rawSet(player, charge, 0);
        push(player);
    }

    /** 把两条充能值同步给该玩家本人 */
    public static void push(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            SyncChargeMessage.syncTo(serverPlayer,
                    Mth.clamp(rawGet(player, DOMAIN), 0, DOMAIN.cost()),
                    Mth.clamp(rawGet(player, GIANT_SWORD), 0, GIANT_SWORD.cost()));
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

    /** 命中敌人 */
    @SubscribeEvent
    public static void onAttack(AttackEntityEvent event) {
        // 1.20.1 的 AttackEntityEvent#getEntity() 已经是 Player
        Player player = event.getEntity();
        if (player.level().isClientSide()) {
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

    /**
     * 受到伤害。
     * <p>
     * 1.21 上是 {@code LivingIncomingDamageEvent}；1.20.1 上没有这个事件，
     * 对应的可改数值版本是 {@code LivingHurtEvent}（护甲减免之后、实际扣血之前触发），
     * 对「受伤即攒能」的语义没有影响。
     */
    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide()) {
            return;
        }
        Charge charge = heldCharge(player);
        if (charge != null) {
            add(player, charge, GAIN_ON_HURT);
        }
    }

    /** 玩家登录时先把当前充能推一次，避免 HUD 从 0 开始显示 */
    @SubscribeEvent
    public static void onLogin(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event) {
        // 1.20.1 的 PlayerLoggedInEvent#getEntity() 已经是 Player（1.21 改成 Entity），
        // 因此这里不需要再用 instanceof 做模式匹配。
        push(event.getEntity());
    }

    /** 死亡重生后能力数据被复制到新实体，补推一次 */
    @SubscribeEvent
    public static void onClone(net.minecraftforge.event.entity.player.PlayerEvent.Clone event) {
        PlayerChargeCapability.get(event.getOriginal())
                .ifPresent(old -> PlayerChargeCapability.get(event.getEntity())
                        .ifPresent(data -> {
                            data.copyFrom(old);
                            push(event.getEntity());
                        }));
    }
}
