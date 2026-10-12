package com.xingling.scenerystaff.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.xingling.scenerystaff.SceneryStaff;
import com.xingling.scenerystaff.sa.SaCharge;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

/**
 * 右下角的 SA 充能条。
 * <p>
 * 同一处位置只放<b>一条</b>充能条，内容跟着主手的刀走：
 * <ul>
 *   <li>手持布景之杖 —— 显示 SA_Denia 图标与蓝紫配色；</li>
 *   <li>手持启明星 —— 显示 SA_Aemeath 图标与白蓝配色；</li>
 *   <li>手持其它刀或物品 —— <b>完全不显示</b>。</li>
 * </ul>
 * 图标外圈紧贴一条进度弧（从正上方起算、<b>逆时针</b>推进），弧下垫着一条暗色轨道，
 * 因此只要手持对应刀，环就一直在；充满后整圈高亮并带脉冲泛光。
 * <p>
 * 几何关系（都是实测/推导出来的，改尺寸时保持这条关系即可）：
 * <pre>
 *   图标贴图里那个圆几乎铺满整张 256×256（过中心半径 127.5 px ≈ 半张贴图），
 *   所以图标在屏幕上的圆半径 = ICON / 2。
 *   要「图标圆与充能环之间没有距离」，就必须 ICON / 2 == BAR_IN。
 * </pre>
 * <p>
 * 1.20.1 上 HUD 还是 Forge 的「覆盖层」机制：{@link RegisterGuiOverlaysEvent} +
 * {@link IGuiOverlay}（1.21 才换成 GUI Layer）。这里注册在
 * {@link VanillaGuiOverlay#HOTBAR} 之上。
 *
 * <p>其它实现要点：
 * <ul>
 *   <li>轨道、进度弧与泛光都用 {@code RenderType.gui()}（POSITION_COLOR）手写四边形拼接，不需要额外贴图；</li>
 *   <li>图标必须用带缩放的 11 参数 {@code blit}：
 *       {@code blit(loc, x, y, w, h, u, v, uW, vH, texW, texH)}。
 *       7 参数的重载<b>不缩放</b>，会把 256×256 贴图左上角一小块原样画出来——图标内容在正中，
 *       结果就是「只有环没有图标」。</li>
 *   <li>图标是白色剪影（带 alpha），用 {@code RenderSystem.setShaderColor} 染色：白 × 色调 = 目标色。</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = SceneryStaff.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class SaChargeHud implements IGuiOverlay {

    /** 图标绘制尺寸。图标圆半径 = ICON / 2，必须等于 {@link #BAR_IN} 才能贴住进度弧 */
    private static final int ICON = 44;
    private static final int ICON_TEX = 256;

    /** 进度弧（同时是暗色轨道）的内外半径；内沿紧贴图标圆 */
    private static final float BAR_IN = ICON / 2.0F;      // 22
    private static final float BAR_OUT = BAR_IN + 3.0F;   // 25

    /** 充能条中心距屏幕右/下边缘（要大于泛光最大半径） */
    private static final int MARGIN = 40;

    /** 暗色轨道：够明显，保证手持时环一直在 */
    private static final int TRACK_COLOR = 0xAA0A0A12;

    /** 图标整体不透明度（贴图自身的 alpha 会在此基础上再相乘） */
    private static final float ICON_ALPHA = 0.95F;

    public SaChargeHud() {
    }

    @SubscribeEvent
    public static void onRegisterGuiOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAbove(VanillaGuiOverlay.HOTBAR.id(), "sa_charge", new SaChargeHud());
    }

    @Override
    public void render(net.minecraftforge.client.gui.overlay.ForgeGui gui, GuiGraphics g,
                       float partialTick, int screenWidth, int screenHeight) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || mc.screen != null) {
            return;
        }

        // 只认主手那把对应的充能条；拿别的刀或物品时整条不显示
        SaCharge.Charge charge = SaCharge.heldCharge(mc.player);
        if (charge == null) {
            return;
        }

        // 图标与配色都由 SaCharge.Charge 自带，这里不做「哪把刀配哪个图标」的判断，
        // 因此新增刀时本类无需改动。
        // 充能比例读客户端镜像值（服务端通过 SyncChargeMessage 推送），
        // 直接读玩家能力只会拿到 0 —— 能力不会自动同步。
        drawOne(g, screenWidth - MARGIN, screenHeight - MARGIN,
                charge.icon(), charge.color(),
                com.xingling.scenerystaff.client.ChargeClientCache.get(charge) / (float) charge.cost());
    }

    /**
     * 画一个充能单元：暗色轨道 + 逆时针进度弧 + 图标；充满时整圈脉冲泛光。
     *
     * @param ratio 0..1
     */
    private static void drawOne(GuiGraphics g, float cx, float cy, ResourceLocation icon, int color, float ratio) {
        float clamped = Mth.clamp(ratio, 0.0F, 1.0F);
        boolean full = clamped >= 1.0F;

        // 1) 暗色轨道：整圈常亮，保证「手持时环一直在」
        arc(g, cx, cy, BAR_IN, BAR_OUT, 0.0F, (float) (Math.PI * 2), TRACK_COLOR);

        // 2) 进度弧：从正上方开始逆时针推进（屏幕 y 轴朝下，故用负角）
        if (clamped > 0.0F) {
            float sweep = (float) (Math.PI * 2) * clamped;
            arc(g, cx, cy, BAR_IN, BAR_OUT, 0.0F, -sweep, (255 << 24) | color);
        }

        // 3) 充满：外圈脉冲泛光
        if (full) {
            float pulse = (float) (0.5D
                    + 0.5D * Math.sin((System.currentTimeMillis() % 1400L) / 1400.0D * Math.PI * 2.0D));
            int glowAlpha = (int) (70 + 110 * pulse);
            arc(g, cx, cy, BAR_OUT + 1.0F, BAR_OUT + 4.0F + 3.0F * pulse, 0.0F, (float) (Math.PI * 2),
                    (glowAlpha << 24) | color);
        }

        // 4) 先把弧线提交，再画图标：弧走 RenderType.gui()、图标走 guiTextured，
        //    两者共用一个缓冲，显式 flush 可以排除提交顺序/状态互相影响。
        g.flush();

        // 5) 图标：保持白色不染色，完整保留贴图自身的 alpha
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, ICON_ALPHA);
        int half = ICON / 2;
        g.blit(icon, Mth.floor(cx) - half, Mth.floor(cy) - half, ICON, ICON,
                0.0F, 0.0F, ICON_TEX, ICON_TEX, ICON_TEX, ICON_TEX);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    /**
     * 画一段圆环（annulus）。
     * <p>
     * 角度约定：0 = 正上方，正值 = 顺时针（因为屏幕 y 轴朝下，代码里用负角表示逆时针）。
     */
    private static void arc(GuiGraphics g, float cx, float cy, float rIn, float rOut,
                            float from, float to, int argb) {
        float span = Math.abs(to - from);
        if (span < 1.0E-4F) {
            return;
        }
        int steps = Math.max(2, (int) Math.ceil(span / (float) (Math.PI / 48.0)));
        Matrix4f matrix = g.pose().last().pose();
        VertexConsumer vc = g.bufferSource().getBuffer(RenderType.gui());
        for (int i = 0; i < steps; i++) {
            float a0 = from + (to - from) * i / steps;
            float a1 = from + (to - from) * (i + 1) / steps;
            float sin0 = (float) Math.sin(a0);
            float cos0 = (float) Math.cos(a0);
            float sin1 = (float) Math.sin(a1);
            float cos1 = (float) Math.cos(a1);
            vc.vertex(matrix, cx + sin0 * rIn, cy - cos0 * rIn, 0.0F).color(argb).endVertex();
            vc.vertex(matrix, cx + sin0 * rOut, cy - cos0 * rOut, 0.0F).color(argb).endVertex();
            vc.vertex(matrix, cx + sin1 * rOut, cy - cos1 * rOut, 0.0F).color(argb).endVertex();
            vc.vertex(matrix, cx + sin1 * rIn, cy - cos1 * rIn, 0.0F).color(argb).endVertex();
        }    }
}
