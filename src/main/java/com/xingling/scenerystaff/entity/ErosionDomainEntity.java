package com.xingling.scenerystaff.entity;

import com.xingling.scenerystaff.se.PolymerizationSE;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

public class ErosionDomainEntity extends Entity {
    /** 领域持续时间（tick）：30 秒（20 tick/秒） */
    public static final int DURATION_TICKS = 600;

    private int lifeTicks = 0;
    private int maxLife = DURATION_TICKS;
    private UUID ownerId;
    private static final int DAMAGE_INTERVAL = 20; // 每秒伤害一次
    private int timer = 0;
    private static final float DAMAGE_AMOUNT = 3.0F;
    private static final float RADIUS = 10.0F;

    public ErosionDomainEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public void setOwner(Player owner) {
        this.ownerId = owner.getUUID();
    }

    public void setDuration(int ticks) {
        this.maxLife = ticks;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) return;

        lifeTicks++;
        if (lifeTicks > maxLife) {
            this.discard();
            return;
        }

        // 播放领域粒子（蓝紫色，让范围与存在感更直观）
        spawnDomainParticles();

        // 每间隔造成伤害
        timer++;
        if (timer >= DAMAGE_INTERVAL) {
            timer = 0;
            applyAreaEffect();
        }
    }

    /**
     * 领域粒子：边界一圈 GLOW 蓝白光环 + 内部 PORTAL 蓝紫飘散 + 中央 SOUL/SPARK 能量，
     * 在原有四芒星裂缝贴图基础上增强蓝紫色氛围（参照图2 的紫蓝粒子感）。
     */
    private void spawnDomainParticles() {
        if (!(this.level() instanceof ServerLevel serverLevel)) return;
        int t = this.tickCount;

        // 边界一圈蓝白发光粒子（沿伤害半径圆周，明确 10 格边界）
        if (t % 6 == 0) {
            double base = t * 0.04;
            int ring = Math.max(12, (int) (RADIUS * 1.6));
            for (int i = 0; i < ring; i++) {
                double ang = base + i * (Math.PI * 2.0 / ring);
                double x = this.getX() + Math.cos(ang) * RADIUS;
                double z = this.getZ() + Math.sin(ang) * RADIUS;
                serverLevel.sendParticles(ParticleTypes.GLOW, x, this.getY() + 0.15, z, 1, 0, 0.02, 0, 0.0);
            }
        }
        // 内部蓝紫 PORTAL 飘散粒子
        if (t % 8 == 0) {
            for (int i = 0; i < 5; i++) {
                double a = this.level().random.nextDouble() * Math.PI * 2.0;
                double r = this.level().random.nextDouble() * RADIUS * 0.9;
                double x = this.getX() + Math.cos(a) * r;
                double z = this.getZ() + Math.sin(a) * r;
                serverLevel.sendParticles(ParticleTypes.PORTAL, x, this.getY() + 0.3, z, 1, 0, 0.06, 0, 0);
            }
        }
        // 中央青蓝 SOUL + 亮蓝 ELECTRIC_SPARK 能量
        if (t % 12 == 0) {
            serverLevel.sendParticles(ParticleTypes.SOUL, this.getX(), this.getY() + 0.4, this.getZ(),
                    2, 0.3, 0.4, 0.3, 0.02);
            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, this.getX(), this.getY() + 0.5, this.getZ(),
                    2, 0.3, 0.4, 0.3, 0.3);
        }
    }

    @Nullable
    private Player getOwner() {
        if (ownerId == null) return null;
        if (this.level() instanceof ServerLevel serverLevel) {
            // 区块卸载重载或服务器重启后按 UUID 重新解析
            return serverLevel.getPlayerByUUID(ownerId);
        }
        return null;
    }

    private void applyAreaEffect() {
        if (!(this.level() instanceof ServerLevel serverLevel)) return;
        Player owner = getOwner();
        if (owner == null) return;

        AABB aabb = this.getBoundingBox().inflate(RADIUS);
        // 只对"当前会攻击玩家"的生物造成伤害与叠加聚爆：
        //  - 原生敌对生物（Enemy：僵尸/骷髅/苦力怕等宿主生物）
        //  - 已进入敌对/激怒状态的中立生物（NeutralMob：铁傀儡、末影人、猪灵、僵尸猪人、狼等）
        //  - 当前以玩家为目标、或最近被玩家伤害的生物
        List<LivingEntity> targets = serverLevel.getEntitiesOfClass(LivingEntity.class, aabb,
                e -> e != owner && e.isAlive()
                        && (e instanceof Enemy
                            || (e instanceof NeutralMob nm && nm.isAngryAt(owner))
                            || (e instanceof Mob mob && mob.getTarget() == owner))
                        && e.distanceToSqr(this) <= RADIUS * RADIUS);

        DamageSource damageSource = owner.damageSources().magic();
        for (LivingEntity target : targets) {
            // 造成伤害
            target.hurt(damageSource, DAMAGE_AMOUNT);
            // 附加聚爆层数（每次伤害 +1 层，与 SE 共享同一计数；叠满由 addStack 触发额外伤害与音效）
            PolymerizationSE.addStack(target, damageSource);
        }
    }

    // NeoForge 1.21.1：defineSynchedData 需要 Builder 参数（本实体无同步数据，留空）
    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
        this.maxLife = compound.getInt("MaxLife");
        this.lifeTicks = compound.getInt("LifeTicks");
        if (compound.hasUUID("Owner")) {
            this.ownerId = compound.getUUID("Owner");
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {
        compound.putInt("MaxLife", this.maxLife);
        compound.putInt("LifeTicks", this.lifeTicks);
        if (this.ownerId != null) {
            compound.putUUID("Owner", this.ownerId);
        }
    }
}
