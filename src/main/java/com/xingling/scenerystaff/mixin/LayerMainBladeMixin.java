package com.xingling.scenerystaff.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.xingling.scenerystaff.SceneryStaff;
import mods.flammpfeil.slashblade.capability.slashblade.BladeStateAccess;
import mods.flammpfeil.slashblade.client.renderer.layers.LayerMainBlade;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 只截断 SlashBlade「把快捷栏第 1 格的刀当作待机刀渲染」这一条路径（且仅针对本模组的刀）。
 * <p>
 * {@code LayerMainBlade.renderOffhandItem} 在副手为空时会转而调用
 * {@code renderHotbarItem}，把快捷栏第 0 格的刀渲染成待机刀——于是主手换成其他道具后，
 * 布景之杖看起来像装备到了副手，实际并未装备。这里在 HEAD 处取消该渲染。
 * <p>
 * 真正放进副手的刀走的是 {@code renderStandbyBlade(offhandStack)} 分支，不受本 mixin 影响，
 * 因此副手装备时仍会正常显示。
 */
@Mixin(value = LayerMainBlade.class, remap = false)
public class LayerMainBladeMixin {

    @Inject(method = "renderHotbarItem", at = @At("HEAD"), cancellable = true)
    private void scenerystaff$skipHotbarStandbyBlade(PoseStack matrixStack, MultiBufferSource bufferIn, int lightIn,
                                                     LivingEntity entity, CallbackInfo ci) {
        if (!(entity instanceof Player player)) {
            return;
        }
        // 与目标方法一致的取值：快捷栏第 0 格
        ItemStack hotbarBlade = player.getInventory().getItem(0);
        if (hotbarBlade.isEmpty()) {
            return;
        }
        // 仅拦截本模组的刀（以刀模型命名空间判定），其他拔刀剑保持原有的待机刀表现
        boolean isSceneryBlade = BladeStateAccess.of(hotbarBlade)
                .flatMap(state -> state.getModel())
                .map(model -> model.getNamespace().equals(SceneryStaff.MODID))
                .orElse(false);
        if (isSceneryBlade) {
            ci.cancel();
        }
    }
}
