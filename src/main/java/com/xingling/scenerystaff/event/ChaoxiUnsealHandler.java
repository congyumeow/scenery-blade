package com.xingling.scenerystaff.event;

import com.xingling.scenerystaff.SceneryStaff;
import com.xingling.scenerystaff.blade.SceneryBlades;
import mods.flammpfeil.slashblade.capability.slashblade.BladeStateAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * 「潮汐」的解封。
 * <p>
 * SlashBlade 里「妖」与「封」是<b>两个不同的状态位</b>
 *（见 {@code SlashBladeDefinition} 按 {@code SwordType} 分三种初始化的写法）：
 * <ul>
 *   <li>{@code setDefaultBewitched(true)} —— 妖刀，只有 {@code SwordType.from} 读它，决定显示；</li>
 *   <li>{@code setSealed(true)} —— 未开封，被 {@code ItemSlashBlade} 与
 *       {@code SlashBladeEventHandler} 读取，<b>妖刀"离开主手回复耐久"的门槛就在这个位上</b>。</li>
 * </ul>
 * 所以要同时把 sealed 清掉、把 defaultBewitched 立起来。
 * <p>
 * 触发时机：合成产出立即处理 + 每 {@link #ENSURE_INTERVAL_TICKS} tick 扫一遍背包兜底
 *（沿用 {@code IronsSpellsCompat.ensureInnateSpell} 的"定期补齐"思路）。
 * 注意创造模式物品栏里的图标是<b>临时构造的预览栈</b>，不经过背包，因此那里的显示取决于
 * 定义本身；取到背包里之后就会被这里修正。
 */
@EventBusSubscriber(modid = SceneryStaff.MODID)
public final class ChaoxiUnsealHandler {

    /** 兜底扫描间隔（tick） */
    private static final int ENSURE_INTERVAL_TICKS = 20;

    private ChaoxiUnsealHandler() {
    }

    /** 合成产出时立即处理 */
    @SubscribeEvent
    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        fix(event.getCrafting());
    }

    /** 兜底：定期扫背包（0~35 主背包、36~39 护甲、40 副手） */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide() || player.tickCount % ENSURE_INTERVAL_TICKS != 0) {
            return;
        }
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            fix(player.getInventory().getItem(i));
        }
    }

    /**
     * 把「潮汐」的状态修正为妖刀（sealed=false + defaultBewitched=true）。
     * <p>
     * 新刀已经通过在定义里附带附魔（力量 + 耐久）达成妖刀，这里是给<b>旧存档里已经存在的</b>
     * 潮汐兜底——定义改动不会追溯修改已存在的刀。正常情况下前面的 getter 判断会让本方法
     * 什么都不做，不会产生额外的数据同步。
     */
    private static void fix(ItemStack stack) {
        if (!SceneryBlades.isChaoxi(stack)) {
            return;
        }
        BladeStateAccess.of(stack).ifPresent(state -> {
            if (state.isSealed()) {
                state.setSealed(false);
            }
            if (!state.isDefaultBewitched()) {
                state.setDefaultBewitched(true);
            }
        });
    }
}
