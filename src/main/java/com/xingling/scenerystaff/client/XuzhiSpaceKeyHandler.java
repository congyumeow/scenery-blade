package com.xingling.scenerystaff.client;

import com.xingling.scenerystaff.SceneryStaff;
import com.xingling.scenerystaff.blade.SceneryBlades;
import com.xingling.scenerystaff.network.XuzhiSpaceRequestPayload;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.network.PacketDistributor;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

/**
 * 「进入/离开虚质空间」的按键。
 * <p>
 * 默认是 <b>'</b>（单引号，{@code GLFW_KEY_APOSTROPHE}），玩家可在「选项 → 控制 → 按键绑定」
 * 里改键——因为用的是 {@link KeyMapping}，改键与冲突检测都是原版行为，不需要自己处理。
 * <p>
 * 按键只在<b>持有两把终盘刀之一</b>时才会发包；服务端还会再校验一次，
 * 所以即使客户端被改动也无法无刀传送。
 */
@EventBusSubscriber(modid = SceneryStaff.MODID, value = Dist.CLIENT)
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
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        // consumeClick 会清空本 tick 的点击次数，因此用 while 把连点也吃掉
        while (OPEN_XUZHI_SPACE.consumeClick()) {
            if (SceneryBlades.isHolding(mc.player, SceneryBlades::isTopTier)) {
                PacketDistributor.sendToServer(new XuzhiSpaceRequestPayload());
            }
        }
    }
}
