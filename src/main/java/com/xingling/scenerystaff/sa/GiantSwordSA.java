package com.xingling.scenerystaff.sa;

import com.xingling.scenerystaff.blade.SceneryIds;
import com.xingling.scenerystaff.entity.GiantSwordEntity;
import com.xingling.scenerystaff.registry.EntityRegistry;
import com.xingling.scenerystaff.registry.SoundRegistry;
import mods.flammpfeil.slashblade.init.DefaultResources;
import mods.flammpfeil.slashblade.registry.combo.ComboState;
import mods.flammpfeil.slashblade.slasharts.SlashArts;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 「巨剑」剑技（启明星专属）。
 * <p>
 * 与侵蚀领域同构：这个版本 SlashBlade 的 {@link SlashArts} 没有 {@code release()}，
 * 剑技行为由 ComboState 驱动 —— 剑技映射到 {@code scenerystaff:giant_sword} 连击状态，
 * 由该状态的 TimeLineTickAction 在第 0 帧回调 {@link #doGiantSword(LivingEntity)}。
 * <p>
 * 释放时：播 Aemeath 语音 → 按「刀攻击力 × {@link GiantSwordEntity#DAMAGE_MULTIPLIER}」算好这一发伤害 →
 * 在玩家脚下地表生成 {@link GiantSwordEntity}（离地 30 格，1 秒后落地）。
 */
public class GiantSwordSA extends SlashArts {

    /** 落点距玩家的水平距离（格）：准星方向 3 格 */
    public static final double LAND_DISTANCE = 3.0D;

    public GiantSwordSA() {
        super(entity -> SceneryIds.GIANT_SWORD);
    }

    /**
     * 本剑技的连击状态。
     * <p>
     * 与 {@link ErosionDomainSA#createComboState()} 同理：剑技的行为编排随剑技走，
     * 注册表只负责注册。
     */
    public static ComboState createComboState() {
        return ComboState.Builder.newInstance()
                .startAndEnd(0, 15)
                .priority(50)
                .motionLoc(DefaultResources.ExMotionLocation)
                .next(entity -> mods.flammpfeil.slashblade.registry.ComboStateRegistry.NONE.getId())
                .nextOfTimeout(entity -> mods.flammpfeil.slashblade.registry.ComboStateRegistry.NONE.getId())
                .addTickAction(ComboState.TimeLineTickAction.getBuilder()
                        .put(0, GiantSwordSA::doGiantSword)
                        .build())
                .build();
    }

    /** 由连击状态的时间轴回调。只应在服务端调用。 */
    public static void doGiantSword(LivingEntity user) {
        if (!(user.level() instanceof ServerLevel serverLevel)) return;
        if (!(user instanceof Player player)) return;

        // 充能门控在 SaChargeGate（PerformSlashArtEvent）里完成：
        // 未攒满时那边已经把连击状态换成幻影刃，本方法根本不会被调用；攒满时那边负责清零。
        // 这里只负责真正的召唤。

        // 释放语音（Aemeath.ogg）
        serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundRegistry.AEMEATH.get(), player.getSoundSource(), 1.0F, 1.0F);

        // AOE 伤害 = 释放瞬间的刀攻击力 × GiantSwordEntity.DAMAGE_MULTIPLIER（当前 3.4）。
        // 取 ATTACK_DAMAGE 属性：它汇总了刀自身的基础攻击（含耀魂带来的提升）以及力量等增益；
        // 在这里算好后存进实体，所以下落途中换装不会改变这一发伤害。
        double damage = player.getAttributeValue(Attributes.ATTACK_DAMAGE)
                * GiantSwordEntity.DAMAGE_MULTIPLIER;

        GiantSwordEntity sword = new GiantSwordEntity(EntityRegistry.GIANT_SWORD.get(), serverLevel);

        // 落点：玩家准星方向水平 3 格处。
        // 只取朝向的水平分量（俯仰角不参与），落点高度再由该处的地表射线决定。
        Vec3 look = player.getViewVector(1.0F);
        double horizontal = Math.sqrt(look.x * look.x + look.z * look.z);
        double dirX = horizontal < 1.0E-4D ? 0.0D : look.x / horizontal;
        double dirZ = horizontal < 1.0E-4D ? 0.0D : look.z / horizontal;
        double landX = player.getX() + dirX * LAND_DISTANCE;
        double landZ = player.getZ() + dirZ * LAND_DISTANCE;

        sword.setup(player, damage, landX, landZ, findGroundY(serverLevel, player, landX, landZ));
        serverLevel.addFreshEntity(sword);
    }

    /**
     * 在落点处向下射线取地表高度。用 OUTLINE（取 {@code getShape}）而不是 COLLIDER，
     * 这样雪层、半砖、压力板这类非实体短方块也能正确作为落点。
     */
    private static double findGroundY(ServerLevel level, Player player, double x, double z) {
        Vec3 start = new Vec3(x, player.getY() + 0.5D, z);
        Vec3 end = new Vec3(x, player.getY() - GiantSwordEntity.FALL_HEIGHT, z);
        BlockHitResult hit = level.clip(new ClipContext(start, end,
                ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        return hit.getType() != HitResult.Type.MISS ? hit.getLocation().y : player.getY();
    }
}
