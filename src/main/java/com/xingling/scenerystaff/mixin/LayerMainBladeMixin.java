package com.xingling.scenerystaff.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.xingling.scenerystaff.client.standby.HotbarStandbyBlade;
import mods.flammpfeil.slashblade.client.renderer.layers.LayerMainBlade;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 老版本拔刀剑（{@code LayerMainBlade} 自己持有渲染方法）一侧的待机刀渲染拦截。
 * <p>
 * 判定逻辑在 {@link HotbarStandbyBlade}，这里只负责定位注入点。
 * <p>
 * <b>版本适配</b>：拔刀剑 2.0.2～2.0.7 的 {@code renderHotbarItem} 就在
 * {@code LayerMainBlade} 上，本 mixin 生效；2.0.8 起该方法上移到父类
 * {@code LayerSlashBlade}（2.0.8 / 2.1.9 都已确认），本 mixin 找不到目标而安静跳过，
 * 改由 {@link LayerSlashBladeMixin} 生效。两者只会有一个真正命中，不需要额外判断版本号。
 * <p>
 * {@code remap = false}：SlashBlade 是模组类，其方法名不需要（也不应该）经过混淆重映射。
 * <p>
 * {@code require = 0}：与 {@link LayerSlashBladeMixin} 同理——目标方法搬家时宁可静默失效，
 * 也不要让整个游戏在模组构造阶段抛 {@code InvalidInjectionException}。
 * 本 mixin 的 HEAD 注入曾经以默认的 {@code require = 1} 运行，在拔刀剑 2.1.9 上会直接报错，
 * 玩家看到的「加载模组时就崩」就是它。
 */
@Mixin(value = LayerMainBlade.class, remap = false)
public class LayerMainBladeMixin {

    @Inject(method = "renderHotbarItem", at = @At("HEAD"), cancellable = true, require = 0)
    private void scenerystaff$skipHotbarStandbyBlade(PoseStack matrixStack, MultiBufferSource bufferIn, int lightIn,
                                                     LivingEntity entity, CallbackInfo ci) {
        if (HotbarStandbyBlade.shouldSkipHotbarStandbyBlade(entity)) {
            HotbarStandbyBlade.logHookedOnce("mods.flammpfeil.slashblade.client.renderer.layers.LayerMainBlade"
                    + "#renderHotbarItem");
            ci.cancel();
        }
    }
}
