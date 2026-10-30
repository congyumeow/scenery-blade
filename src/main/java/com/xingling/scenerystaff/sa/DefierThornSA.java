package com.xingling.scenerystaff.sa;

import com.xingling.scenerystaff.blade.SceneryIds;
import com.xingling.scenerystaff.entity.TargetFilters;
import com.xingling.scenerystaff.registry.MobEffectRegistry;
import com.xingling.scenerystaff.registry.SoundRegistry;
import com.xingling.scenerystaff.se.WindErosionEffect;
import mods.flammpfeil.slashblade.init.DefaultResources;
import mods.flammpfeil.slashblade.registry.combo.ComboState;
import mods.flammpfeil.slashblade.slasharts.SlashArts;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Random;

/**
 * 「自由骑士之舞」剑技（不屈命定之冠专属）。
 * <p>
 * 与另外两个剑技同构：SlashBlade 这个版本的 {@link SlashArts} 没有 {@code release()}，
 * 剑技行为由 ComboState 驱动——SA 映射到 {@code scenerystaff:defier_thorn} 连击状态，
 * 由该状态的时间轴第 0 帧回调 {@link #doFreeKnightDance(LivingEntity)}。
 * <p>
 * <b>范围</b>：以玩家<b>视角水平正前方</b>为轴的长方体——宽 10 格（左右各 5）、
 * 长 100 格、高 ±10 格（以玩家脚底为基准）。
 * <b>为什么只取水平朝向</b>：若连俯仰角一起用，抬头/低头时这块 100 格长的区域会插进地里或斜指天空，
 * 判定会变得无法预期；水平朝向 + 独立的世界竖直范围，手感最稳定。
 * <p>
 * <b>伤害</b>：基础值 = 玩家最大生命值 × {@value #MAX_HEALTH_DAMAGE_RATIO}（即 150%），
 * 再按<b>目标身上</b>的风蚀层数增伤：1~3 层 +30%、4 层 +40%、5 层 +50%、6 层 +60%。
 * 无风蚀层数时无增伤。
 * <p>
 * 伤害类型走 {@code magic}（属于 {@code #bypasses_armor}，无视护甲），
 * 与聚爆/巨剑一致，且把玩家挂为伤害归属，击杀与统计正常计入。
 * <p>
 * 充能 125 点；未攒满时由 {@link SaChargeGate} 换成内置幻影刃，本方法不会被调用。
 */
public class DefierThornSA extends SlashArts {

    /** 基础伤害占玩家最大生命值的比例：150% */
    public static final float MAX_HEALTH_DAMAGE_RATIO = 1.5F;

    /** 区域宽度（格）：左右各 {@link #HALF_WIDTH} */
    public static final double HALF_WIDTH = 5.0D;
    /** 区域长度（格） */
    public static final double LENGTH = 100.0D;
    /** 区域高度（格）：上下各 {@link #HALF_HEIGHT} */
    public static final double HALF_HEIGHT = 10.0D;

    private static final Random RANDOM = new Random();

    public DefierThornSA() {
        super(entity -> SceneryIds.DEFIER_THORN);
    }

    /** 本剑技的连击状态（编排随剑技走，注册表只负责注册） */
    public static ComboState createComboState() {
        return ComboState.Builder.newInstance()
                .startAndEnd(0, 15)
                .priority(50)
                .motionLoc(DefaultResources.ExMotionLocation)
                .next(entity -> mods.flammpfeil.slashblade.registry.ComboStateRegistry.NONE.getId())
                .nextOfTimeout(entity -> mods.flammpfeil.slashblade.registry.ComboStateRegistry.NONE.getId())
                .addTickAction(ComboState.TimeLineTickAction.getBuilder()
                        .put(0, DefierThornSA::doFreeKnightDance)
                        .build())
                .build();
    }

    /** 由连击状态的时间轴回调。只应在服务端调用。 */
    public static void doFreeKnightDance(LivingEntity user) {
        if (!(user.level() instanceof ServerLevel serverLevel)) return;
        if (!(user instanceof Player player)) return;

        Vec3 forward = horizontalForward(player);
        Vec3 side = new Vec3(-forward.z, 0.0D, forward.x);
        double originX = player.getX();
        double originY = player.getY();
        double originZ = player.getZ();

        // 释放语音：三条轮播，随机取一条
        SoundEvent[] voices = {
                SoundRegistry.CARTETHYIA1.get(),
                SoundRegistry.CARTETHYIA2.get(),
                SoundRegistry.CARTETHYIA3.get()
        };
        serverLevel.playSound(null, originX, originY, originZ,
                voices[RANDOM.nextInt(voices.length)], player.getSoundSource(), 1.0F, 1.0F);

        AABB box = sweepBox(originX, originY, originZ, forward, side);
        List<LivingEntity> targets = serverLevel.getEntitiesOfClass(LivingEntity.class, box,
                e -> e != player && e.isAlive()
                        && TargetFilters.isAbilityTarget(e, player)
                        && insideRegion(e, originX, originY, originZ, forward, side));

        float base = player.getMaxHealth() * MAX_HEALTH_DAMAGE_RATIO;
        for (LivingEntity target : targets) {
            float bonus = windErosionBonus(target);
            float damage = base * (1.0F + bonus);
            if (damage > 0.0F) {
                target.hurt(player.damageSources().source(DamageTypes.MAGIC, player), damage);
            }
        }

        spawnTrail(serverLevel, originX, originY, originZ, forward, side);
    }

    /**
     * 目标身上的风蚀层数对应的增伤比例。
     * <p>
     * 1~3 层统一 +30%（这三层是「默认上限」档），4/5/6 层逐级 +40%/+50%/+60%。
     * 超过 6 层按 6 层算（正常情况下不可能，上限已钳制）。
     */
    public static float windErosionBonus(LivingEntity target) {
        MobEffectInstance effect = target.getEffect(MobEffectRegistry.WIND_EROSION);
        int stacks = effect == null ? 0 : effect.getAmplifier() + 1;
        if (stacks <= 0) {
            return 0.0F;
        }
        if (stacks <= 3) {
            return 0.30F;
        }
        return switch (Math.min(stacks, WindErosionEffect.CARTETHYIA_MAX_STACKS)) {
            case 4 -> 0.40F;
            case 5 -> 0.50F;
            default -> 0.60F;
        };
    }

    /** 玩家朝向的水平单位向量；垂直俯视/仰视时退回面朝方向，避免除以 0 */
    private static Vec3 horizontalForward(Player player) {
        Vec3 look = player.getViewVector(1.0F);
        double horizontal = Math.sqrt(look.x * look.x + look.z * look.z);
        if (horizontal < 1.0E-4D) {
            double yaw = Math.toRadians(player.getYRot());
            return new Vec3(-Math.sin(yaw), 0.0D, Math.cos(yaw));
        }
        return new Vec3(look.x / horizontal, 0.0D, look.z / horizontal);
    }

    /** 取长方体的外接 AABB，作为实体检索的预筛（只扫这条走廊，不扫 200×200 的大方块） */
    private static AABB sweepBox(double x, double y, double z, Vec3 forward, Vec3 side) {
        double[][] corners = {
                {x + side.x * HALF_WIDTH, z + side.z * HALF_WIDTH},
                {x - side.x * HALF_WIDTH, z - side.z * HALF_WIDTH},
                {x + forward.x * LENGTH + side.x * HALF_WIDTH, z + forward.z * LENGTH + side.z * HALF_WIDTH},
                {x + forward.x * LENGTH - side.x * HALF_WIDTH, z + forward.z * LENGTH - side.z * HALF_WIDTH}
        };
        double minX = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE;
        double minZ = Double.MAX_VALUE;
        double maxZ = -Double.MAX_VALUE;
        for (double[] c : corners) {
            minX = Math.min(minX, c[0]);
            maxX = Math.max(maxX, c[0]);
            minZ = Math.min(minZ, c[1]);
            maxZ = Math.max(maxZ, c[1]);
        }
        return new AABB(minX, y - HALF_HEIGHT, minZ, maxX, y + HALF_HEIGHT, maxZ);
    }

    /** 精确判定：前向距离 0..100、横向 |≤5|、竖直 |≤10| */
    private static boolean insideRegion(LivingEntity entity, double originX, double originY, double originZ,
                                        Vec3 forward, Vec3 side) {
        double dx = entity.getX() - originX;
        double dz = entity.getZ() - originZ;
        double forwardDistance = dx * forward.x + dz * forward.z;
        double lateral = Math.abs(dx * side.x + dz * side.z);
        double centerY = entity.getY() + entity.getBbHeight() * 0.5D;
        double vertical = Math.abs(centerY - (originY + 0.9D));
        return forwardDistance >= 0.0D && forwardDistance <= LENGTH
                && lateral <= HALF_WIDTH
                && vertical <= HALF_HEIGHT;
    }

    /** 沿攻击方向铺一条粒子轨迹，给这记横跨 100 格的范围技一点可见反馈 */
    private static void spawnTrail(ServerLevel level, double x, double y, double z, Vec3 forward, Vec3 side) {
        int steps = 60;
        for (int i = 0; i <= steps; i++) {
            double t = LENGTH * i / steps;
            for (double offset : new double[]{-HALF_WIDTH, HALF_WIDTH}) {
                double px = x + forward.x * t + side.x * offset;
                double pz = z + forward.z * t + side.z * offset;
                level.sendParticles(ParticleTypes.SWEEP_ATTACK, px, y + 1.0D, pz, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            }
        }
    }
}
