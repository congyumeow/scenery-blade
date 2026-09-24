package com.xingling.scenerystaff.client.render;

import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * 球面映射（sphere map）的数学——对应 MMD/PMX 的 {@code Spa}。
 * <p>
 * 纯函数、只依赖 JOML，不碰任何 MC 类，便于离线核对。
 */
public final class SphereMap {

    private SphereMap() {
    }

    /**
     * 把「模型空间法线」转到<b>视图空间</b>。
     * <p>
     * <b>必须两步</b>：
     * <ol>
     *   <li>{@code poseNormal}（{@code PoseStack.Pose#normal()}）把法线转到
     *       「相机相对的世界空间」——这个空间是<b>世界轴对齐</b>的，相机朝向不在其中；</li>
     *   <li>{@code cameraConjugate}（相机朝向的四元数共轭）再把它转到真正的视图空间。</li>
     * </ol>
     * 少第二步的话，图案会被锚定在世界方向上：从不同角度看会看到图案被劈成两半、
     * 前后各一片，完全不像球面反射（实测正对镜头的表面会偏离贴图中心约 0.32）。
     *
     * @param nx 模型空间法线 x
     * @param ny 模型空间法线 y
     * @param nz 模型空间法线 z
     * @param poseNormal      pose 的法线矩阵
     * @param cameraConjugate 相机朝向的共轭（视图矩阵的旋转部分）
     * @param out             输出：视图空间单位法线
     */
    public static void viewNormal(float nx, float ny, float nz, Matrix3f poseNormal, Quaternionf cameraConjugate,
                                  Vector3f out) {
        out.set(nx, ny, nz).mul(poseNormal).rotate(cameraConjugate);
        if (out.lengthSquared() < 1.0E-8F) {
            out.set(0.0F, 0.0F, 1.0F);
        }
        out.normalize();
    }

    /**
     * 视图空间单位法线 → 贴图坐标。
     * <p>
     * 用的是 MMD 实际采用的形式 {@code uv = n.xy · 0.5 + 0.5}：可见半球的法线圆盘
     * 正好铺满整张贴图。不要换用 OpenGL 的 {@code SPHERE_MAP} 公式
     * （{@code m = √(nx²+ny²+(nz+1)²)} 那一版），它会把内容压进半径 0.354 的圆盘里，
     * 而这张气泡贴图是内切满方图的（实测气泡半径 0.983），用那版会明显缩小一圈。
     * <p>
     * 公式刻意<b>只用 (nx, ny)</b>：正对镜头的表面采到贴图中心，掠射边缘采到贴图外圈，
     * 这才是球面反射的观感，也是「怎么转都像同一张图」的来源。副作用是背面法线
     * （nz&lt;0）会得到相同 UV，所以必须配合背面剔除。
     */
    public static void uv(float nx, float ny, float nz, float[] out) {
        out[0] = nx * 0.5F + 0.5F;
        // MC 的贴图 V 轴朝下，所以这里对 y 取反；若发现上下颠倒，把负号去掉即可
        out[1] = 0.5F - ny * 0.5F;
    }
}
