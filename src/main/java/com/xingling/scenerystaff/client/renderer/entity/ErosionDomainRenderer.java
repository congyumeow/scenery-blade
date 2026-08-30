package com.xingling.scenerystaff.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.xingling.scenerystaff.SceneryStaff;
import com.xingling.scenerystaff.entity.ErosionDomainEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

/**
 * 侵蚀领域实体渲染器：在地面绘制蓝紫色四芒星裂缝，裂缝正中央为缓慢旋转的发光
 * 虚质空间漩涡，用于直观展示领域的范围与存在。使用贴图四边形实现，效果稳定。
 */
@OnlyIn(Dist.CLIENT)
public class ErosionDomainRenderer extends EntityRenderer<ErosionDomainEntity> {

    private static final ResourceLocation STAR_TEXTURE = SceneryStaff.prefix("textures/entity/erosion_star.png");
    private static final ResourceLocation VOID_TEXTURE = SceneryStaff.prefix("textures/entity/xuzhikongj.png");

    /** 领域半径，与 {@link ErosionDomainEntity} 的伤害判定半径保持一致 */
    private static final float DOMAIN_RADIUS = 5.0F;
    /** 中央虚质空间漩涡的直径：仅覆盖星形中心的低透明度空洞 */
    private static final float VOID_SIZE = 2.2F;
    /** 领域持续时间（tick），与 entity.setDuration(200) 一致，用于淡入淡出 */
    private static final int TOTAL_LIFE = 200;

    public ErosionDomainRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(ErosionDomainEntity entity) {
        return STAR_TEXTURE;
    }

    @Override
    public void render(ErosionDomainEntity entity, float entityYaw, float partialTicks,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        int age = entity.tickCount;

        // 淡入 / 淡出，让领域出现与消失更柔和
        float fadeIn = Math.min(1.0F, age / 8.0F);
        float fadeOut = Math.max(0.0F, (TOTAL_LIFE - age) / 15.0F);
        float alpha = Math.min(fadeIn, fadeOut);
        if (alpha <= 0.0F) {
            return;
        }
        int alpha255 = (int) (alpha * 255.0F);

        // 薄雪层(snow_layer)是非实体方块，玩家会沉到下层地面（实体 Y = 地面顶），
        // 而雪层本体占据 0~0.125，会盖住效果。用 OUTLINE 射线（含非实体方块的轮廓形状）
        // 找到实际地表顶面（雪层/半砖/压力板等），让效果浮在其上。
        float localGroundY = getSurfaceHeight(entity) - (float) entity.getY();

        // 底层：中央虚质空间漩涡（垫在星形下方，星形实心部分会遮住其边缘）
        poseStack.pushPose();
        poseStack.translate(0.0, localGroundY + 0.02, 0.0);
        poseStack.mulPose(Axis.YP.rotationDegrees(age * 6.0F));
        VertexConsumer voidConsumer = buffer.getBuffer(RenderType.entityTranslucentEmissive(VOID_TEXTURE));
        addGroundQuad(voidConsumer, poseStack.last(), VOID_SIZE / 2.0F, alpha255, packedLight);
        poseStack.popPose();

        // 顶层：地面四芒星裂缝。使用 entityCutoutNoCull（alpha 测试 + 不剔除背面），
        // 正上方视角可见；中心低透明度空洞被剔除，透出下方的虚质空间。
        poseStack.pushPose();
        poseStack.translate(0.0, localGroundY + 0.04, 0.0);
        VertexConsumer starConsumer = buffer.getBuffer(RenderType.entityCutoutNoCull(STAR_TEXTURE));
        addGroundQuad(starConsumer, poseStack.last(), DOMAIN_RADIUS, alpha255, packedLight);
        poseStack.popPose();
    }

    /**
     * 从实体位置向下做 OUTLINE 射线，命中雪层/半砖/压力板/普通方块等的轮廓顶面。
     * OUTLINE 使用 {@code getShape}，能覆盖 COLLIDER 会穿透的非实体短方块（如 1 层雪）。
     * 未命中时回退到实体自身高度。
     */
    private float getSurfaceHeight(ErosionDomainEntity entity) {
        Level level = entity.level();
        Vec3 start = new Vec3(entity.getX(), entity.getY() + 0.5, entity.getZ());
        Vec3 end = new Vec3(entity.getX(), entity.getY() - 3.0, entity.getZ());
        BlockHitResult hit = level.clip(new ClipContext(start, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, entity));
        if (hit.getType() != HitResult.Type.MISS) {
            return (float) hit.getLocation().y;
        }
        return (float) entity.getY();
    }

    /**
     * 在 XZ 平面上绘制一张朝 +Y 的贴图四边形（中心位于局部原点，边长 half*2）。
     */
    private void addGroundQuad(VertexConsumer consumer, PoseStack.Pose pose, float half,
                               int alpha, int light) {
        Matrix4f matrix = pose.pose();
        consumer.addVertex(matrix, -half, 0.0F, -half)
                .setColor(255, 255, 255, alpha)
                .setUv(0.0F, 0.0F)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
        consumer.addVertex(matrix, half, 0.0F, -half)
                .setColor(255, 255, 255, alpha)
                .setUv(1.0F, 0.0F)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
        consumer.addVertex(matrix, half, 0.0F, half)
                .setColor(255, 255, 255, alpha)
                .setUv(1.0F, 1.0F)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
        consumer.addVertex(matrix, -half, 0.0F, half)
                .setColor(255, 255, 255, alpha)
                .setUv(0.0F, 1.0F)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }
}
