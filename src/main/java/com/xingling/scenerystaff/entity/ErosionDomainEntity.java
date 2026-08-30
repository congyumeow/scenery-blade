package com.xingling.scenerystaff.entity;

import com.xingling.scenerystaff.registry.SoundRegistry;
import com.xingling.scenerystaff.se.PolymerizationSE;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

public class ErosionDomainEntity extends Entity {
    private int lifeTicks = 0;
    private int maxLife = 200; // 默认10秒
    private UUID ownerId;
    private static final int DAMAGE_INTERVAL = 20; // 每秒伤害一次
    private int timer = 0;
    private static final float DAMAGE_AMOUNT = 3.0F;
    private static final float RADIUS = 5.0F;

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

        // 每间隔造成伤害
        timer++;
        if (timer >= DAMAGE_INTERVAL) {
            timer = 0;
            applyAreaEffect();
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
        List<LivingEntity> targets = serverLevel.getEntitiesOfClass(LivingEntity.class, aabb,
                e -> e != owner && e.isAlive() && e.distanceToSqr(this) <= RADIUS * RADIUS);

        DamageSource damageSource = owner.damageSources().magic();
        for (LivingEntity target : targets) {
            // 造成伤害
            target.hurt(damageSource, DAMAGE_AMOUNT);
            // 附加聚爆层数（每次伤害+1层，与SE共享同一计数）
            int count = target.getPersistentData().getInt(PolymerizationSE.TAG_COUNT);
            count++;
            target.getPersistentData().putInt(PolymerizationSE.TAG_COUNT, count);
            // 如果达到10层，触发聚爆额外伤害（复用SE逻辑）
            if (count >= 10) {
                target.getPersistentData().putInt(PolymerizationSE.TAG_COUNT, 0);
                target.hurt(damageSource, PolymerizationSE.EXPLOSION_DAMAGE);
                serverLevel.playSound(null, target.getX(), target.getY(), target.getZ(),
                        SoundRegistry.DENIA1.get(), target.getSoundSource(), 1.0F, 1.0F);
            }
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
    // 无需重写 getAddEntityPacket：1.21.1 基类已有 getAddEntityPacket(ServerEntity) 默认实现
}
