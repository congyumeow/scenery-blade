package com.xingling.scenerystaff.entity;

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

/**
 * 「启明星」剑技召唤的巨剑。
 * <p>
 * 生命周期（全程服务端驱动，客户端只按实体位置渲染）：
 * <ol>
 *   <li><b>下落</b>：从落点上方 {@link #FALL_HEIGHT} 格处生成，{@link #FALL_TICKS} tick（1 秒）匀速落到地面；</li>
 *   <li><b>落地</b>：在落点做<b>一次</b>半径 {@link #AOE_RADIUS} 格的 AOE 结算（<b>没有持续伤害</b>）；</li>
 *   <li><b>停留</b>：插在原地 {@link #GROUND_LIFE_TICKS} tick（5 秒）后消失。</li>
 * </ol>
 * <p>
 * AOE 伤害来自<b>释放瞬间</b>持刀者的刀攻击力 × {@link #DAMAGE_MULTIPLIER}（3.4），
 * 在生成时算好存进实体，因此下落途中换装不会改变这次伤害。
 * <p>
 * 归属沿用侵蚀领域的写法（原版「间接魔法」）：击杀记在玩家头上，成就/统计正常触发，
 * 并且与 {@code magic} 一样属于 {@code #minecraft:bypasses_armor}，无视护甲。
 * <p>
 * <b>不叠加聚爆层数</b>（按需求：落地只做这一次伤害）。
 * <p>
 * 碰撞：{@link #noPhysics} 为真、不可被选中，既不会推动实体也不会破坏地形；
 * 它只是"看起来插在地上"，落点高度在生成时用射线取地表得到。
 */
public class GiantSwordEntity extends Entity {

    /** 生成位置离地高度（格） */
    public static final int FALL_HEIGHT = 30;
    /** 下落耗时（tick）：1 秒 */
    public static final int FALL_TICKS = 20;
    /** 落地后停留时间（tick）：5 秒 */
    public static final int GROUND_LIFE_TICKS = 5 * 20;
    /** 落地 AOE 半径（格） */
    public static final float AOE_RADIUS = 10.0F;
    /** AOE 伤害系数：刀攻击力 × 3.4 */
    public static final float DAMAGE_MULTIPLIER = 3.4F;
    /**
     * 剑长（格）：渲染缩放与实体碰撞箱都取自这一个值。
     * <p>
     * {@code EntityRegistry} 的 {@code sized} 与 {@code GiantSwordRenderer} 的目标长度
     * 以前各写一份 {@code 8.0F}，属于必须人工同步的隐患，现统一引用本常量。
     */
    public static final float SWORD_LENGTH = 8.0F;

    private static final int PHASE_FALLING = 0;
    private static final int PHASE_GROUND = 1;

    private int phase = PHASE_FALLING;
    /** 阶段内计时：下落阶段用于插值，落地阶段用于存活倒计时 */
    private int phaseTicks = 0;

    private double damage = 0.0D;
    private double fallFromY = 0.0D;
    private double landY = 0.0D;
    private UUID ownerId;

    public GiantSwordEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    /**
     * 生成时由服务端调用。
     *
     * @param owner  施术者（决定伤害归属）
     * @param damage 这一发 AOE 的伤害（已按刀攻击力 × {@link #DAMAGE_MULTIPLIER} 算好）
     * @param landX  落点 X（由剑技按准星方向算出）
     * @param landZ  落点 Z
     * @param landY  落点地面高度（由服务端在落点处向下射线取得）
     */
    public void setup(Player owner, double damage, double landX, double landZ, double landY) {
        this.ownerId = owner.getUUID();
        this.damage = damage;
        this.landY = landY;
        this.fallFromY = landY + FALL_HEIGHT;
        this.setPos(landX, this.fallFromY, landZ);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            return;
        }

        phaseTicks++;

        if (phase == PHASE_FALLING) {
            // 1 秒内匀速从 fallFromY 落到 landY（位置每 tick 都被同步，客户端按原版插值渲染）
            double progress = Math.min(1.0D, phaseTicks / (double) FALL_TICKS);
            this.setPos(this.getX(), fallFromY + (landY - fallFromY) * progress, this.getZ());
            if (phaseTicks >= FALL_TICKS) {
                this.setPos(this.getX(), landY, this.getZ());
                this.phase = PHASE_GROUND;
                this.phaseTicks = 0;
                applyLandingEffect();
            }
            return;
        }

        // 落地阶段：不再造成任何伤害，停留 5 秒后消失
        if (phaseTicks > GROUND_LIFE_TICKS) {
            this.discard();
        }
    }

    /** 落地瞬间的一次性 AOE 结算（不叠聚爆、不破坏地形、无持续伤害） */
    private void applyLandingEffect() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        Player owner = getOwner();

        DamageSource source = owner != null
                ? owner.damageSources().indirectMagic(this, owner)
                : this.damageSources().magic();

        AABB aabb = this.getBoundingBox().inflate(AOE_RADIUS);
        // 只打「当前会攻击玩家」的生物，与侵蚀领域保持一致的判定
        List<LivingEntity> targets = serverLevel.getEntitiesOfClass(LivingEntity.class, aabb,
                e -> e != owner && e.isAlive()
                        && (e instanceof Enemy
                            || (e instanceof NeutralMob nm && owner != null && nm.isAngryAt(owner))
                            || (e instanceof Mob mob && owner != null && mob.getTarget() == owner))
                        && e.distanceToSqr(this) <= AOE_RADIUS * AOE_RADIUS);

        float amount = (float) this.damage;
        for (LivingEntity target : targets) {
            target.hurt(source, amount);
        }

        spawnLandingParticles(serverLevel);
    }

    /** 落地冲击的视觉：一圈向外扩散的冲击粒子 + 中心亮蓝能量 */
    private void spawnLandingParticles(ServerLevel serverLevel) {
        int ring = Math.max(16, (int) (AOE_RADIUS * 8));
        for (int i = 0; i < ring; i++) {
            double ang = i * (Math.PI * 2.0 / ring);
            double x = this.getX() + Math.cos(ang) * AOE_RADIUS;
            double z = this.getZ() + Math.sin(ang) * AOE_RADIUS;
            serverLevel.sendParticles(ParticleTypes.GLOW, x, this.getY() + 0.15, z, 1, 0, 0.02, 0, 0.0);
            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, x, this.getY() + 0.35, z, 1, 0, 0.06, 0, 0.25);
        }
        serverLevel.sendParticles(ParticleTypes.EXPLOSION, this.getX(), this.getY() + 0.2, this.getZ(),
                3, 0.4, 0.2, 0.4, 0.0);
        serverLevel.sendParticles(ParticleTypes.SOUL, this.getX(), this.getY() + 0.5, this.getZ(),
                6, 0.5, 0.6, 0.5, 0.03);
    }

    @Nullable
    private Player getOwner() {
        if (ownerId == null) {
            return null;
        }
        if (this.level() instanceof ServerLevel serverLevel) {
            return serverLevel.getPlayerByUUID(ownerId);
        }
        return null;
    }

    // NeoForge 1.21.1：defineSynchedData 需要 Builder 参数（本实体无同步数据，位置本身即渲染所需）
    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
        this.phase = compound.getInt("Phase");
        this.phaseTicks = compound.getInt("PhaseTicks");
        this.damage = compound.getDouble("Damage");
        this.fallFromY = compound.getDouble("FallFromY");
        this.landY = compound.getDouble("LandY");
        if (compound.hasUUID("Owner")) {
            this.ownerId = compound.getUUID("Owner");
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {
        compound.putInt("Phase", this.phase);
        compound.putInt("PhaseTicks", this.phaseTicks);
        compound.putDouble("Damage", this.damage);
        compound.putDouble("FallFromY", this.fallFromY);
        compound.putDouble("LandY", this.landY);
        if (this.ownerId != null) {
            compound.putUUID("Owner", this.ownerId);
        }
    }
}
