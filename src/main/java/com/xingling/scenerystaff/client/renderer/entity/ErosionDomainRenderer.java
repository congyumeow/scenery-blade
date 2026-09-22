package com.xingling.scenerystaff.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.xingling.scenerystaff.SceneryStaff;
import com.xingling.scenerystaff.entity.ErosionDomainEntity;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
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
 * 侵蚀领域实体渲染器。
 * <p>
 * 三张贴图都平铺在地面上（不做朝向相机的广告牌、也不自转），从下往上叠三层：
 * <ul>
 *   <li>星形本体（{@link #STAR_BACK_TEXTURE}）：贴地的实心星形，作为裂隙的底；</li>
 *   <li>黑洞（{@link #VOID_TEXTURE}）：按 {@link #VOID_SCALE} 缩放，alpha 已按星形本体裁剪。
 *       它仍然平铺在地面上，但会绕 Y 轴旋转，让贴图的下边缘始终朝向角色——就像地面上的一张画，
 *       永远正对着看的人；</li>
 *   <li>发光边缘（{@link #STAR_TEXTURE}）：平铺在最上面，画出裂隙的发光边。</li>
 * </ul>
 * 提交顺序"本体 → 黑洞 → 发光边缘"（半透明类型切换时会先提交上一批，顺序是确定的）。
 */
@OnlyIn(Dist.CLIENT)
public class ErosionDomainRenderer extends EntityRenderer<ErosionDomainEntity> {

    /** 裂隙的发光边缘 */
    private static final ResourceLocation STAR_TEXTURE = SceneryStaff.prefix("textures/entity/star.png");
    /** 裂隙的星形本体（底） */
    private static final ResourceLocation STAR_BACK_TEXTURE = SceneryStaff.prefix("textures/entity/star_back.png");
    /** 黑洞（已按星形本体裁剪） */
    private static final ResourceLocation VOID_TEXTURE = SceneryStaff.prefix("textures/entity/blackhole.png");

    /** 领域半径，与 {@link ErosionDomainEntity} 的伤害判定半径保持一致 */
    private static final float DOMAIN_RADIUS = 10.0F;
    /** 星形本体离地高度（格），避免与地表共面闪烁 */
    private static final float BODY_HEIGHT = 0.02F;
    /** 黑洞离地高度（格），压在星形本体之上 */
    private static final float VOID_HEIGHT = 0.04F;
    /** 发光边缘离地高度（格），压在黑洞之上 */
    private static final float EDGE_HEIGHT = 0.06F;
    /** 黑洞贴图相对裂隙的缩放（贴图本身是 20 格见方，这里按比例缩小显示） */
    private static final float VOID_SCALE = 0.1F;
    /** 领域持续时间（tick），与 {@link ErosionDomainEntity#DURATION_TICKS} 一致，用于淡入淡出 */
    private static final int TOTAL_LIFE = ErosionDomainEntity.DURATION_TICKS;

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

        // 找到实际地表顶面（雪层/半砖/压力板等），让效果浮在其上。
        float localGroundY = getSurfaceHeight(entity) - (float) entity.getY();

        // 第一层：星形本体，贴地铺开，作为裂隙的底。
        poseStack.pushPose();
        poseStack.translate(0.0, localGroundY + BODY_HEIGHT, 0.0);
        VertexConsumer bodyConsumer = buffer.getBuffer(RenderType.entityTranslucentEmissive(STAR_BACK_TEXTURE));
        addGroundQuad(bodyConsumer, poseStack.last(), DOMAIN_RADIUS, alpha255, packedLight);
        poseStack.popPose();

        // 第二层：黑洞，仍然平铺在地面上，但绕 Y 轴转到让贴图下边缘朝向角色。
        poseStack.pushPose();
        poseStack.translate(0.0, localGroundY + VOID_HEIGHT, 0.0);
        poseStack.mulPose(Axis.YP.rotationDegrees(lowerEdgeYaw(entity)));
        VertexConsumer voidConsumer = buffer.getBuffer(RenderType.entityTranslucentEmissive(VOID_TEXTURE));
        addGroundQuad(voidConsumer, poseStack.last(), DOMAIN_RADIUS * VOID_SCALE, alpha255, packedLight);
        poseStack.popPose();

        // 第三层：发光边缘，平铺在最上面。
        poseStack.pushPose();
        poseStack.translate(0.0, localGroundY + EDGE_HEIGHT, 0.0);
        VertexConsumer edgeConsumer = buffer.getBuffer(RenderType.entityTranslucentEmissive(STAR_TEXTURE));
        addGroundQuad(edgeConsumer, poseStack.last(), DOMAIN_RADIUS, alpha255, packedLight);
        poseStack.popPose();
    }

    /**
     * 计算让黑洞贴图的下边缘朝向相机（角色）所需的 Y 轴旋转角。
     * 贴图的下边缘在局部 +Z 方向，绕 Y 轴转 θ 后 +Z 指向 (sinθ, 0, cosθ)，
     * 令它等于"实体指向相机"的水平方向即可。
     */
    private float lowerEdgeYaw(ErosionDomainEntity entity) {
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        Vec3 cameraPos = camera.getPosition();
        double dx = cameraPos.x - entity.getX();
        double dz = cameraPos.z - entity.getZ();
        if (dx * dx + dz * dz < 1.0E-6) {
            return 0.0F;
        }
        return (float) Math.toDegrees(Math.atan2(dx, dz));
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
     * 贴图的下边缘（v=1）落在局部 +Z 一侧。
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
