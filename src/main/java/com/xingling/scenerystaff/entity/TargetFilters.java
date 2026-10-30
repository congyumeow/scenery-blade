package com.xingling.scenerystaff.entity;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;

/**
 * 「哪些生物算剑技的合法目标」的统一判定。
 * <p>
 * 规则与侵蚀领域、巨剑 AOE 原先各自内联的那段一致，额外加入<b>训练假人</b>：
 * 「试炼假人」（{@code dummmmmmy:target_dummy}）既不是 {@link Enemy}、也不是
 * {@link NeutralMob}，也不会把玩家设成 {@code target}，按原规则永远打不到，而它恰恰是
 * 用来测伤害的对象。
 * <p>
 * <b>用实体类型 id 而不是 {@code instanceof}</b>：本模组不把 dummmmmmy 列为编译期依赖，
 * 直接引用它的类会在未安装该 mod 时抛 {@code NoClassDefFoundError}。按注册名比较则完全
 * 没有这个风险，未安装时永远不会匹配。
 */
public final class TargetFilters {

    /** 试炼假人的实体类型 id */
    private static final String TARGET_DUMMY_ID = "dummmmmmy:target_dummy";

    private TargetFilters() {
    }

    /**
     * 剑技（AOE）是否应当命中这个生物。
     *
     * @param entity 候选目标
     * @param owner  施术者，允许为 {@code null}
     */
    public static boolean isAbilityTarget(LivingEntity entity, Player owner) {
        if (entity instanceof Enemy) {
            return true;
        }
        if (entity instanceof NeutralMob neutral && owner != null && neutral.isAngryAt(owner)) {
            return true;
        }
        if (entity instanceof Mob mob && owner != null && mob.getTarget() == owner) {
            return true;
        }
        return isTargetDummy(entity);
    }

    /** 是否为训练假人（按实体类型注册名判断，不依赖该 mod 的类） */
    public static boolean isTargetDummy(LivingEntity entity) {
        return TARGET_DUMMY_ID.equals(
                entity.getType().builtInRegistryHolder().key().location().toString());
    }
}
