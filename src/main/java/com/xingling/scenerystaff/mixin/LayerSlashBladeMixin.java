package com.xingling.scenerystaff.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.xingling.scenerystaff.client.standby.HotbarStandbyBlade;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 只截断 SlashBlade「把快捷栏第 1 格的刀当作待机刀渲染」这一条路径（且仅针对本模组的刀）。
 * <p>
 * {@code renderOffhandItem} 在副手为空时会转而调用 {@code renderHotbarItem}，把快捷栏第 0 格
 * 的刀渲染成待机刀——于是主手换成其他道具后，布景之杖看起来像装备到了副手，实际并未装备。
 * 这里在 HEAD 处取消该渲染。
 * <p>
 * 真正放进副手的刀走的是 {@code renderStandbyBlade(offhandStack)} 分支，不受本 mixin 影响，
 * 因此副手装备时仍会正常显示。
 * <p>
 * <b>为什么目标类写成字符串而不是 {@code LayerSlashBlade.class}</b>：拔刀剑 2.0.8 起把这批渲染方法
 * 从 {@code LayerMainBlade} 上移到了父类 {@code LayerSlashBlade}（2.0.8 / 2.1.9 均已确认）。
 * 写字符串就可以在<b>不把 {@code LayerSlashBlade} 加进编译依赖</b>的前提下注入它——
 * 本模组的编译前置仍是最低的 2.0.2，那个版本里根本没有这个类，写类字面量会直接编译失败。
 * <p>
 * {@code @Pseudo} 与 {@code require = 0} 是配套的软目标声明：2.0.7 及更早的拔刀剑里没有
 * {@code LayerSlashBlade} 这个类，没有 {@code @Pseudo} 时 mixin 会直接报缺失目标；
 * 类缺失时本 mixin 被跳过，改由 {@link LayerMainBladeMixin} 生效。两者只会有一个真正命中。
 * <p>
 * {@code remap = false}：SlashBlade 是模组类，其类名与方法名不需要（也不应该）经过混淆重映射。
 * <p>
 * {@code require = 0}：目标方法在未来版本里再次搬家时，本 mixin 安静失效而不是让游戏报
 * {@code InvalidInjectionException}（这正是 2.1.9 上玩家遇到的报错）。代价是失效无日志，
 * 因此注入成功时由 {@link HotbarStandbyBlade#logHookedOnce} 打一行 DEBUG 便于排查。
 */
@Pseudo
@Mixin(targets = "mods.flammpfeil.slashblade.client.renderer.layers.LayerSlashBlade", remap = false)
public class LayerSlashBladeMixin {

    @Inject(method = "renderHotbarItem", at = @At("HEAD"), cancellable = true, require = 0)
    private void scenerystaff$skipHotbarStandbyBlade(PoseStack matrixStack, MultiBufferSource bufferIn, int lightIn,
                                                     LivingEntity entity, CallbackInfo ci) {
        if (HotbarStandbyBlade.shouldSkipHotbarStandbyBlade(entity)) {
            HotbarStandbyBlade.logHookedOnce("mods.flammpfeil.slashblade.client.renderer.layers.LayerSlashBlade"
                    + "#renderHotbarItem");
            ci.cancel();
        }
    }
}
