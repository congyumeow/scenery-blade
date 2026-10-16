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
 * 针对 {@code LayerSlashBlade} 的<b>前瞻性</b>注入：目标类目前<b>并不存在</b>。
 *
 * <h2>现状（已核实）</h2>
 * 待机刀渲染方法 {@code renderHotbarItem} 在当前两个可用版本里都定义在
 * {@code LayerMainBlade} 上，且该类都是直接 {@code extends RenderLayer}：
 * <ul>
 *   <li>1.20.1 线的拔刀剑 1.9.65；</li>
 *   <li>1.21.1 线的拔刀剑（1.21.1 分支开发期所用的源码）。</li>
 * </ul>
 * 两个版本里都<b>没有</b> {@code LayerSlashBlade} 这个类。
 *
 * <h2>这个 mixin 的作用</h2>
 * 它是一份「上游日后把这批渲染方法上移到父类」的保险：届时不改代码即可自动接管。
 * 因此 {@code require = 0} 是刻意为之 —— 目标不存在时必须安静跳过，而不是加载失败。
 *
 * <h2>代价（务请注意）</h2>
 * 这也意味着上游真的换了位置、或改了方法名时，{@code LayerMainBladeMixin} 不会报错，
 * 修复会安静失效，只在开启 DEBUG 时留下一行日志。排查「副手没装备却显示待机刀」
 * 这类问题时，先确认日志里到底注上了哪个目标类。
 *
 * @see HotbarStandbyBlade#logHookedOnce(String)
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
