package com.xingling.scenerystaff.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.xingling.scenerystaff.SceneryStaff;
import com.xingling.scenerystaff.entity.GiantSwordEntity;
import mods.flammpfeil.slashblade.client.renderer.model.BladeModelManager;
import mods.flammpfeil.slashblade.client.renderer.model.obj.WavefrontObject;
import mods.flammpfeil.slashblade.client.renderer.util.BladeRenderState;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

import java.util.HashSet;
import java.util.Set;

/**
 * 巨剑实体渲染器。
 * <p>
 * 渲染部分<b>完全委托给 SlashBlade 自己的公开入口</b>
 * {@link BladeRenderState#renderOverrided(ItemStack, WavefrontObject, String, ResourceLocation, PoseStack, MultiBufferSource, int)}：
 * 手持刀（{@code LayerMainBlade}）、召唤剑（{@code SummonedSwordRenderer}）、
 * 掉落刀（{@code BladeItemEntityRenderer}）走的都是这一个入口，
 * 颜色、RenderType、渲染覆盖事件、附魔光泽全部由它内部处理。
 * <p>
 * 之前几轮失败的教训（都已验证）：
 * <ul>
 *   <li>不能用原版 {@code RenderType.entityCutoutNoCull(path)}：它会擅自补 {@code textures/} 前缀与 {@code .png}。</li>
 *   <li>不能用非自发的 {@code getSlashBladeBlend} 版本：SlashBlade 的刀一律是自发光，
 *       受光照版本在地下/夜晚方块光为 0 时会渲染成接近全黑。</li>
 *   <li>{@code WavefrontObject.tessellate*} 不接收颜色参数、也不读 {@link BladeRenderState} 的静态颜色，
 *       自己拼调用时顶点颜色会保持默认值 —— 这就是"有形状但全黑/看不见"的来源。</li>
 * </ul>
 * <p>
 * 模型朝向与落点补偿（这两个是自己算的）：
 * <ul>
 *   <li><b>剑尖朝下</b>：模型长轴是 X，实测剑尖在 <b>-X</b> 端，绕 Z 轴 +90° 使 -X 指向世界 -Y。</li>
 *   <li><b>剑尖落在地面</b>：模型原点在握柄、剑尖距原点 {@link #MODEL_TIP_OFFSET} 单位，
 *       旋转后整把剑向下伸出约 6.37 格，故先沿 +Y 抬高 {@link #TIP_LIFT}（单位是格，须在 scale 之前）。</li>
 * </ul>
 */
@OnlyIn(Dist.CLIENT)
public class GiantSwordRenderer extends EntityRenderer<GiantSwordEntity> {

    /** 与上位刀「永远的启明星」共用的模型与贴图（贴图是完整路径，SlashBlade 的刀贴图不走 textures/ 目录） */
    private static final ResourceLocation MODEL = SceneryStaff.prefix("model/qimingxing/qimingxing.obj");
    private static final ResourceLocation TEXTURE = SceneryStaff.prefix("model/qimingxing/qimingxing_extra.png");

    /** 目标剑长（格）：与实体碰撞箱同源，见 {@link GiantSwordEntity#SWORD_LENGTH} */
    private static final float TARGET_LENGTH = GiantSwordEntity.SWORD_LENGTH;
    /** 模型长轴（X 轴）跨度：实测 -266.34 .. 68.03，合计 334.37 */
    private static final float MODEL_LENGTH = 334.37F;
    /** 缩放系数 ≈ 0.0239（约 SlashBlade 默认刀身缩放 0.0075 的 3.2 倍） */
    private static final float SCALE = TARGET_LENGTH / MODEL_LENGTH;

    /** 剑尖距模型原点的距离（沿 -X，单位与模型一致） */
    private static final float MODEL_TIP_OFFSET = 266.34F;
    /** 抬高量（格）：把剑尖从实体位置下方拨回实体位置 */
    private static final float TIP_LIFT = MODEL_TIP_OFFSET * SCALE;

    /** 只画剑身本体这一组 */
    private static final String TARGET = "blade";

    /** 每个实体只打一次诊断日志，便于定位"有粒子没模型"这类问题 */
    private static final Set<Integer> DIAGNOSED = new HashSet<>();

    public GiantSwordRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(GiantSwordEntity entity) {
        return TEXTURE;
    }

    @Override
    public void render(GiantSwordEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        WavefrontObject model = BladeModelManager.getInstance().getModel(MODEL);

        poseStack.pushPose();
        // 顺序即"从外到内"：先抬高（单位是格），再摆正朝向，最后按模型单位缩放。
        poseStack.translate(0.0D, TIP_LIFT, 0.0D);
        poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F));
        poseStack.scale(SCALE, SCALE, SCALE);

        if (DIAGNOSED.add(entity.getId())) {
            Matrix4f m = poseStack.last().pose();
            SceneryStaff.LOGGER.info(
                    "[巨剑] render 首次 id={} model={} 姿态原点=({}, {}, {}) 实体位置=({}, {}, {})",
                    entity.getId(), model != null,
                    m.m30(), m.m31(), m.m32(),
                    entity.getX(), entity.getY(), entity.getZ());
        }

        if (model != null) {
            // 用<b>非自发光</b>那一版：自发光版本走 rendertype_entity_translucent_emissive，
            // 而该着色器程序里没有 Sampler2（自发光贴图槽），自发光部分退化成纯白，
            // 再叠加加法混合 ⇒ 整把剑过曝发白、纹理细节被冲掉。
            // 非自发光版本用普通透明混合并按常规采样贴图，贴图能正常显示。
            BladeRenderState.setCol(0xFFFFFF, false);
            BladeRenderState.renderOverrided(ItemStack.EMPTY, model, TARGET, TEXTURE,
                    poseStack, buffer, packedLight);
        }

        poseStack.popPose();
    }
}
