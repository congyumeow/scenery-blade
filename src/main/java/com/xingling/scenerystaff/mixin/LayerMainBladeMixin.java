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
