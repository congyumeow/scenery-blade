package com.xingling.scenerystaff.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.xingling.scenerystaff.SceneryStaff;
import com.xingling.scenerystaff.blade.SceneryBlades;
import com.xingling.scenerystaff.network.SceneryNetwork;
import com.xingling.scenerystaff.network.XuzhiSpaceRequestPayload;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/**
 * 「进入/离开虚质空间」的按键。
 * <p>
 * 默认是 <b>'</b>（单引号，{@code GLFW_KEY_APOSTROPHE}），玩家可在「选项 → 控制 → 按键绑定」
 * 里改键——因为用的是 {@link KeyMapping}，改键与冲突检测都是原版行为，不需要自己处理。
 * <p>
 * 按键只在<b>持有终盘刀</b>时才会发包；服务端还会再校验一次，
 * 所以即使客户端被改动也无法无刀传送。
 * <p>
 * 1.20.1 上没有 {@code ClientTickEvent.Post}，用 {@link TickEvent.ClientTickEvent} 的 END 阶段。
 */
@Mod.EventBusSubscriber(modid = SceneryStaff.MODID, value = Dist.CLIENT)
public final class XuzhiSpaceKeyHandler {

    /** 按键分类（可在语言文件里本地化） */
    private static final String CATEGORY = "key.categories.scenerystaff";

    public static final KeyMapping OPEN_XUZHI_SPACE = new KeyMapping(
            "key.scenerystaff.xuzhi_space",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_APOSTROPHE,
            CATEGORY);

    private XuzhiSpaceKeyHandler() {
    }

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(OPEN_XUZHI_SPACE);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        // consumeClick 会清空本 tick 的点击次数，因此用 while 把连点也吃掉
        while (OPEN_XUZHI_SPACE.consumeClick()) {
            if (SceneryBlades.isHolding(mc.player, SceneryBlades::isTopTier)) {
                SceneryNetwork.CHANNEL.sendToServer(new XuzhiSpaceRequestPayload());
            }
        }
    }
}
