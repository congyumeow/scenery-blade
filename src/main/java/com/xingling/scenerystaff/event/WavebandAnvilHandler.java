package com.xingling.scenerystaff.event;

import com.xingling.scenerystaff.SceneryStaff;
import com.xingling.scenerystaff.item.SceneryBlades;
import com.xingling.scenerystaff.item.WavebandUpgrade;
import com.xingling.scenerystaff.registry.ItemRegistry;
import mods.flammpfeil.slashblade.event.SlashBladeEvent;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AnvilUpdateEvent;

/**
 * 「达妮娅的回音频段」通过铁砧强化本模组拔刀剑。
 * <p>
 * 机制完全照搬拔刀剑自身「耀魂铁锭」那一套（{@code RefineHandler}）：监听 {@link AnvilUpdateEvent}，
 * 铁砧左槽放本模组的刀、右槽放回音频段即可出现结果。
 * <ul>
 *   <li>事件在 {@code AnvilMenu#createResult()} 中、原版逻辑之前触发；只要 {@code output} 非空，
 *       原版逻辑就会被跳过。本处理器使用默认优先级（NORMAL），先于拔刀剑 {@code RefineHandler}
 *       的 LOW 执行，因此两边不会打架。</li>
 *   <li>攻击力加成不写进 {@code ATTRIBUTE_MODIFIERS} 组件，而是挂在
 *       {@link SlashBladeEvent.UpdateAttackEvent} 上——因为拔刀剑的伤害完全由
 *       {@code ItemSlashBlade#getDefaultAttributeModifiers} 动态计算并在此事件中放行修改，
 *       写组件反而会被 <b>自带组件优先</b> 的取值逻辑顶掉。</li>
 * </ul>
 */
@EventBusSubscriber(modid = SceneryStaff.MODID)
public class WavebandAnvilHandler {

    @SubscribeEvent
    public static void onAnvilUpdate(AnvilUpdateEvent event) {
        // 已有其它处理器给出结果时让位（与拔刀剑 RefineHandler 的写法一致）
        if (!event.getOutput().isEmpty()) {
            return;
        }

        ItemStack left = event.getLeft();
        ItemStack right = event.getRight();
        if (left.isEmpty() || right.isEmpty()) {
            return;
        }
        // 右槽必须是回音频段，左槽必须是本模组的刀
        if (!right.is(ItemRegistry.DENIA_WAVEBAND.get())) {
            return;
        }
        if (!SceneryBlades.isSceneryBlade(left)) {
            return;
        }

        int level = WavebandUpgrade.getLevel(left);
        if (level >= WavebandUpgrade.MAX_LEVEL) {
            // 已满强化：不产出结果，铁砧输出槽留空
            return;
        }

        ItemStack result = left.copy();
        WavebandUpgrade.setLevel(result, level + 1);

        event.setOutput(result);
        event.setMaterialCost(1);                       // 消耗 1 个回音频段
        event.setCost(WavebandUpgrade.ANVIL_LEVEL_COST); // 经验等级
    }

    /**
     * 把强化带来的攻击力加成加到刀的基础伤害上。
     * <p>
     * 事件里的 {@code newDamage} 是<b>属性修正值</b>（玩家自身基础 1 点之外的部分），
     * 因此这里直接加上「基础攻击力 × 15% × 等级」即可，最终面板攻击力也正好增加同样的数值。
     */
    @SubscribeEvent
    public static void onUpdateAttack(SlashBladeEvent.UpdateAttackEvent event) {
        ItemStack blade = event.getBlade();
        if (!SceneryBlades.isSceneryBlade(blade)) {
            return;
        }
        double bonus = WavebandUpgrade.attackBonus(blade, event.getSlashBladeState());
        if (bonus <= 0.0D) {
            return;
        }
        event.setNewDamage(event.getNewDamage() + bonus);
    }
}
