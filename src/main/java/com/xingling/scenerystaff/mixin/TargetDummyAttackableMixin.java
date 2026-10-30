package com.xingling.scenerystaff.mixin;

import com.xingling.scenerystaff.SceneryStaff;
import com.xingling.scenerystaff.entity.TargetFilters;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 让「试炼假人」（{@code dummmmmmy:target_dummy}）成为拔刀剑的合法攻击目标。
 * <p>
 * <b>为什么需要在拔刀剑里开口子</b>：拔刀剑的攻击目标由
 * {@code TargetSelector$AttackablePredicate#test} 决定，其中有一条硬规则——
 * <pre>
 * if (!SlashBladeConfig.FRIENDLY_ENABLE.get() &amp;&amp; !(entity instanceof Enemy)) return false;
 * </pre>
 * 而 {@code friendly_enable} 默认是 <b>false</b>（本模组测试实例也是如此），于是所有
 * 非 {@code Enemy} 的 {@code Mob} 都被排除。假人是普通 {@code Mob}、不是 {@code Enemy}，
 * 因此拔刀剑的范围攻击根本收集不到它——表现为「对着假人怎么挥都没有伤害」。
 * <p>
 * <b>为什么不直接把 {@code friendly_enable} 改成 true</b>：那会让所有非敌对生物
 * （牛、羊、猪……）都变成可被拔刀剑随意砍杀的对象，是全局行为改动。这里只在筛选器
 * 最前面补一个白名单分支，仅放行假人，其它生物的可攻击性完全不变。
 * <p>
 * {@code @Pseudo} + {@code require = 0}：目标类是拔刀剑的模组类，编译期不依赖它
 * （用字符串声明目标），类缺失或方法改名时本 mixin 安静失效，不会让游戏启动失败。
 * 注入成功时打一行日志便于排查。
 * <p>
 * {@code remap = false}：拔刀剑是模组类，类名与方法名不应经过混淆重映射。
 */
@Pseudo
@Mixin(targets = "mods.flammpfeil.slashblade.util.TargetSelector$AttackablePredicate", remap = false)
public class TargetDummyAttackableMixin {

    private static boolean scenerystaff$logged;

    @Inject(method = "test(Lnet/minecraft/world/entity/LivingEntity;)Z",
            at = @At("HEAD"), cancellable = true, require = 0)
    private void scenerystaff$allowTargetDummy(LivingEntity entity, CallbackInfoReturnable<Boolean> cir) {
        if (TargetFilters.isTargetDummy(entity)) {
            if (!scenerystaff$logged) {
                scenerystaff$logged = true;
                SceneryStaff.LOGGER.info("[兼容] 已放行「试炼假人」作为拔刀剑攻击目标");
            }
            cir.setReturnValue(true);
        }
    }
}
