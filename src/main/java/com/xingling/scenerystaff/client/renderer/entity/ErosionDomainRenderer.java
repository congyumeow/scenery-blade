package com.xingling.scenerystaff.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.xingling.scenerystaff.SceneryStaff;
import com.xingling.scenerystaff.entity.ErosionDomainEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * 侵蚀领域实体渲染器：领域本身不可见，因此不渲染任何内容，
 * 仅注册一个空的渲染器避免客户端 "No renderer registered" 崩溃。
 */
@OnlyIn(Dist.CLIENT)
public class ErosionDomainRenderer extends EntityRenderer<ErosionDomainEntity> {

    public ErosionDomainRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(ErosionDomainEntity entity) {
        // 不会被实际使用（render 为空），仅满足 EntityRenderer 抽象方法
        return SceneryStaff.prefix("textures/entity/erosion_domain.png");
    }

    @Override
    public void render(ErosionDomainEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        // 不渲染任何内容
    }
}
