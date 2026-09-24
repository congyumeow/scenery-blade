package com.xingling.scenerystaff.client.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.xingling.scenerystaff.SceneryStaff;
import mods.flammpfeil.slashblade.client.renderer.model.obj.Face;
import mods.flammpfeil.slashblade.client.renderer.model.obj.GroupObject;
import mods.flammpfeil.slashblade.client.renderer.model.obj.Vertex;
import mods.flammpfeil.slashblade.client.renderer.model.obj.WavefrontObject;
import mods.flammpfeil.slashblade.event.client.RenderOverrideEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

/**
 * 水晶球的「球面映射」渲染（对应 MMD/PMX 的 {@code Spa}）。
 * <p>
 * <b>1. 为什么需要自己画</b><br>
 * PMX 里球的贴图走的是 <b>スフィアマップ（sphere map）</b>：UV 不是从模型来的，而是由
 * <b>视空间法线</b>换算出来的。OBJ 没有这个概念，导出时球的 UV 会退化
 * （本模型实测 760 个面的 UV 全部等于同一个点），游戏里自然采不到那张气泡图。
 * 所以球在模型里被拆成独立组（见 {@link #BALL_GROUP_BY_TARGET}），
 * SlashBlade 只渲染具名组（blade / sheath / …）画不到它，改由本类接管。
 * <p>
 * <b>2. 球面映射</b><br>
 * 逐顶点把 OBJ 的模型空间法线转到<b>视图空间</b>（两步，见 {@link SphereMap#viewNormal}），
 * 再套 {@link SphereMap#uv}。镜头转动时法线跟着转，图案就像反射一样钉在视线上
 * ——这就是「无论从哪个角度看都是同一张图」的来源。
 * <p>
 * <b>3. 混合方式</b><br>
 * PMX 面板上的「乗算」指的是<b>球面贴图与材质基色的混合</b>，不是与背景相乘。
 * 本例中材质的 {@code Tex} 是空的、材質色是白色，于是
 * {@code 白 × 气泡贴图 = 气泡贴图}，再按材质的 α 叠到场景上——所以这里用普通的
 * <b>alpha 混合</b>即可。（早先误做成「直接与背景相乘」，乘法只能让画面变暗，
 * 结果是一颗发黑的球。）
 * <p>
 * 贴图 {@code crystal_ball.png} 的 alpha 已由美术画好（球心约 0.6、粉白边缘约 0.93、
 * 圆外约 30% 全透明），因此默认直接使用贴图自身的 alpha；{@link #ALPHA_SCALE} 可整体缩放。
 */
@EventBusSubscriber(modid = SceneryStaff.MODID, value = Dist.CLIENT)
public class CrystalBallRenderer {

    /** 气泡贴图（带 alpha 通道，圆外透明） */
    public static final ResourceLocation TEXTURE =
            SceneryStaff.prefix("textures/entity/crystal_ball.png");

    /**
     * 整体透明度缩放。
     * <p>
     * 1.0 = 直接使用贴图自身的 alpha（观感最接近参考图）；
     * 0.5 = 再乘上 PMX 材質色的 α=0.5，会更通透但更淡。
     */
    public static final float ALPHA_SCALE = 1.0F;

    /**
     * 渲染目标 → 球所在的组名。
     * <p>
     * 模型里球有多份副本（刀身 / 物品图标 / 破损图标），拆组时分别命名，
     * 这里按当前渲染的是哪一组取对应那份。
     * <p>
     * 另有 {@code ball_effect} / {@code ball_fragment} 两份：它们是从充能特效与碎裂特效的
     * 组里拆出来的，只为了让那些渲染路径不再冒出一颗静态球，<b>刻意不在这里映射</b>
     * （否则同一帧会和刀身那份重复绘制）。
     */
    private static final Map<String, String> BALL_GROUP_BY_TARGET = Map.of(
            "blade", "ball",
            "item_blade", "ball_item",
            "item_damaged", "ball_item_damaged");

    private static final Map<WavefrontObject, Map<String, Optional<BallMesh>>> MESH_CACHE = new WeakHashMap<>();

    private static final Vector3f NORMAL = new Vector3f();
    /** 相机朝向的共轭：把「相机相对的世界空间」法线转到视图空间 */
    private static final Quaternionf CAMERA_CONJUGATE = new Quaternionf();

    /**
     * 渲染类型：手搓一份，着色器用 SlashBlade 自己渲染刀身贴图时用的
     * {@code RENDERTYPE_ITEM_ENTITY_TRANSLUCENT_CULL_SHADER}。
     * <p>
     * 不用原版 {@code entityTranslucent}：它用的 {@code rendertype_entity_translucent}
     * 着色器里有 {@code if (color.a < 0.1) discard;}，而气泡贴图有 32% 全透明像素
     * （气泡圆外那块），球会被挖出一圈硬边缺口，看起来像"叠了两张图"。
     */
    private static final RenderType RENDER_TYPE = RenderType.create("scenerystaff_crystal_ball",
            DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.TRIANGLES, 1536, true, true,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.RENDERTYPE_ITEM_ENTITY_TRANSLUCENT_CULL_SHADER)
                    .setOutputState(RenderStateShard.MAIN_TARGET)
                    .setTextureState(new RenderStateShard.TextureStateShard(TEXTURE, false, true))
                    .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                    .setCullState(RenderStateShard.CULL)
                    .setLightmapState(RenderStateShard.LIGHTMAP)
                    .setOverlayState(RenderStateShard.OVERLAY)
                    .setWriteMaskState(RenderStateShard.COLOR_DEPTH_WRITE)
                    .createCompositeState(true));

    @SubscribeEvent
    public static void onRenderOverride(RenderOverrideEvent event) {
        String groupName = BALL_GROUP_BY_TARGET.get(event.getTarget());
        if (groupName == null) {
            return;
        }
        WavefrontObject model = event.getModel();
        Map<String, Optional<BallMesh>> perModel = MESH_CACHE.get(model);
        if (perModel == null) {
            perModel = new HashMap<>();
            MESH_CACHE.put(model, perModel);
        }
        Optional<BallMesh> cached = perModel.get(groupName);
        if (cached == null) {
            cached = Optional.ofNullable(build(groupName, model));
            perModel.put(groupName, cached);
        }
        if (cached.isEmpty()) {
            return;
        }

        PoseStack.Pose pose = event.getPoseStack().last();
        VertexConsumer consumer = event.getBuffer().getBuffer(RENDER_TYPE);
        int light = event.getPackedLightIn();
        Matrix3f normalMatrix = pose.normal();
        int alpha = Math.max(0, Math.min(255, Math.round(ALPHA_SCALE * 255.0F)));

        // 球面映射要的是视图空间法线。pose.normal() 只到「相机相对的世界空间」
        // （世界轴对齐、不含相机朝向），必须再乘相机朝向的共轭才是真正的视图空间。
        Minecraft.getInstance().gameRenderer.getMainCamera().rotation().conjugate(CAMERA_CONJUGATE);

        BallMesh mesh = cached.get();
        float[] uv = new float[2];
        for (int f = 0; f < mesh.faceCount; f++) {
            int start = mesh.faceStart[f];
            int size = mesh.faceSize[f];
            // 三角扇拆分（三角形原样，四边形拆成两片）
            for (int i = 1; i + 1 < size; i++) {
                vertex(consumer, pose, mesh, start, light, normalMatrix, uv, alpha);
                vertex(consumer, pose, mesh, start + i, light, normalMatrix, uv, alpha);
                vertex(consumer, pose, mesh, start + i + 1, light, normalMatrix, uv, alpha);
            }
        }
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, BallMesh mesh, int corner, int light,
                               Matrix3f normalMatrix, float[] uv, int alpha) {
        float x = mesh.pos[corner * 3];
        float y = mesh.pos[corner * 3 + 1];
        float z = mesh.pos[corner * 3 + 2];
        float nx = mesh.nrm[corner * 3];
        float ny = mesh.nrm[corner * 3 + 1];
        float nz = mesh.nrm[corner * 3 + 2];

        // 模型空间法线 -> 相机相对的世界空间 -> 视图空间（两步，缺一不可，见 SphereMap#viewNormal）
        SphereMap.viewNormal(nx, ny, nz, normalMatrix, CAMERA_CONJUGATE, NORMAL);

        SphereMap.uv(NORMAL.x, NORMAL.y, NORMAL.z, uv);

        consumer.addVertex(pose, x, y, z);
        // 材质基色是白色：白 × 气泡贴图 = 气泡贴图，所以顶点色保持白
        consumer.setColor(255, 255, 255, alpha);
        consumer.setUv(uv[0], uv[1]);
        consumer.setOverlay(OverlayTexture.NO_OVERLAY);
        consumer.setLight(light);
        consumer.setNormal(pose, nx, ny, nz);
    }

    /** 从模型里取出球组的几何（顶点 + 法线），只做一次 */
    private static BallMesh build(String groupName, WavefrontObject model) {
        GroupObject group = null;
        for (GroupObject candidate : model.groupObjects) {
            if (candidate.name != null && candidate.name.equalsIgnoreCase(groupName)) {
                group = candidate;
                break;
            }
        }
        if (group == null || group.faces.isEmpty()) {
            return null;
        }

        List<float[]> corners = new ArrayList<>();
        List<Integer> starts = new ArrayList<>();
        List<Integer> sizes = new ArrayList<>();
        for (Face face : group.faces) {
            Vertex[] vs = face.vertices;
            int n = vs.length;
            if (n < 3) {
                continue;
            }
            starts.add(corners.size());
            sizes.add(n);
            for (int i = 0; i < n; i++) {
                Vertex v = vs[i];
                float nx;
                float ny;
                float nz;
                Vertex vn = face.vertexNormals != null && i < face.vertexNormals.length ? face.vertexNormals[i] : null;
                if (vn != null) {
                    nx = vn.x;
                    ny = vn.y;
                    nz = vn.z;
                } else {
                    // 兜底：用面法线
                    Vertex a = vs[0];
                    Vertex b = vs[1];
                    Vertex c = vs[2];
                    float ux = b.x - a.x;
                    float uy = b.y - a.y;
                    float uz = b.z - a.z;
                    float wx = c.x - a.x;
                    float wy = c.y - a.y;
                    float wz = c.z - a.z;
                    nx = uy * wz - uz * wy;
                    ny = uz * wx - ux * wz;
                    nz = ux * wy - uy * wx;
                    float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
                    if (len < 1.0E-6F) {
                        nx = 0.0F;
                        ny = 0.0F;
                        nz = 1.0F;
                    } else {
                        nx /= len;
                        ny /= len;
                        nz /= len;
                    }
                }
                corners.add(new float[]{v.x, v.y, v.z, nx, ny, nz});
            }
        }
        if (corners.isEmpty()) {
            return null;
        }

        BallMesh mesh = new BallMesh();
        mesh.pos = new float[corners.size() * 3];
        mesh.nrm = new float[corners.size() * 3];
        for (int i = 0; i < corners.size(); i++) {
            float[] c = corners.get(i);
            mesh.pos[i * 3] = c[0];
            mesh.pos[i * 3 + 1] = c[1];
            mesh.pos[i * 3 + 2] = c[2];
            mesh.nrm[i * 3] = c[3];
            mesh.nrm[i * 3 + 1] = c[4];
            mesh.nrm[i * 3 + 2] = c[5];
        }
        mesh.faceCount = sizes.size();
        mesh.faceStart = new int[mesh.faceCount];
        mesh.faceSize = new int[mesh.faceCount];
        for (int i = 0; i < mesh.faceCount; i++) {
            mesh.faceStart[i] = starts.get(i);
            mesh.faceSize[i] = sizes.get(i);
        }
        SceneryStaff.LOGGER.info("[水晶球] 已接管 {}：{} 个面（球面映射 + alpha 混合）", groupName, mesh.faceCount);
        return mesh;
    }

    /** 扁平化的球面几何（每个角点：位置 + 法线） */
    private static final class BallMesh {
        float[] pos;
        float[] nrm;
        int[] faceStart;
        int[] faceSize;
        int faceCount;
    }
}
