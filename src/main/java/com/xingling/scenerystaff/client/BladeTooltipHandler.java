package com.xingling.scenerystaff.client;

import com.xingling.scenerystaff.SceneryStaff;
import com.xingling.scenerystaff.blade.SceneryBlades;
import com.xingling.scenerystaff.blade.WavebandUpgrade;
import com.xingling.scenerystaff.item.ItemDescriptions;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.List;

/**
 * 为「布景之杖」系列命名刀添加物品描述（tooltip）。
 * <p>
 * SlashBlade 本身不会在物品 tooltip 中显示命名刀的 {@code .desc}（该键仅供 JEI 使用），
 * 因此这里通过 {@link ItemTooltipEvent} 读取
 * {@code <刀的翻译键>.desc.N} 并追加到 tooltip 末尾；行数由语言文件中存在的条目决定。
 * <p>
 * 颜色优先由语言文件中的 § 格式代码控制，此处样式仅作兜底。
 */
@EventBusSubscriber(modid = SceneryStaff.MODID, value = Dist.CLIENT)
public class BladeTooltipHandler {

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (!(stack.getItem() instanceof ItemSlashBlade)) {
            return;
        }
        // 命名刀的翻译键形如 item.<命名空间>.<名称>（如 item.scenerystaff.scenery_staff）
        String translationKey = stack.getItem().getDescriptionId(stack);
        if (!translationKey.startsWith("item." + SceneryStaff.MODID + ".")) {
            return;
        }

        String prefix = translationKey + ".desc.";
        List<Component> tooltip = event.getToolTip();

        // 基础暴击要显示在「在主手时」属性块里、描述之前。
        // 属性块由原版与各模组（如铁魔法的法术强度）分别追加，行数不固定，
        // 所以不能按固定行号插入；这里记下「描述开始之前」的下标，末尾再按它插回去。
        int baseCritIndex = tooltip.size();

        // 描述行交给 ItemDescriptions：那里的「读到不存在的键即停止」是同一条规则，
        // 本类原先自己又写了一遍同样的循环，属于两个真相源。
        ItemDescriptions.append(tooltip, prefix);

        appendWavebandUpgrade(tooltip, stack);

        // 基础暴击（未强化时的底值）：只给两把终盘刀显示
        if (SceneryBlades.hasCrit(stack)) {
            WavebandUpgrade.CritStats base = WavebandUpgrade.critForLevel(0);
            tooltip.add(baseCritIndex, Component.translatable("tooltip.scenerystaff.crit_base",
                    base.chancePercent() + "%", base.damagePercent() + "%")
                    .withStyle(ChatFormatting.GRAY));
        }
    }

    /**
     * 追加回音频段的强化信息（强化等级、攻击力加成、暴击率 / 暴击伤害）。
     * <p>
     * 数值全部来自 {@link WavebandUpgrade}，与铁砧强化、攻击力事件、暴击事件共用同一份常量，改一处即可。
     */
    private static void appendWavebandUpgrade(List<Component> tooltip, ItemStack stack) {
        int level = WavebandUpgrade.getLevel(stack);
        if (level <= 0) {
            return;
        }
        tooltip.add(Component.translatable("tooltip.scenerystaff.waveband_level",
                level, WavebandUpgrade.MAX_LEVEL).withStyle(ChatFormatting.AQUA));

        int percent = Math.round(level * WavebandUpgrade.ATTACK_BONUS_PER_LEVEL * 100.0F);
        tooltip.add(Component.translatable("tooltip.scenerystaff.waveband_attack", percent + "%")
                .withStyle(ChatFormatting.GRAY));

        // 强化带来的<b>增量</b>（相对未强化基线），不是最终值——最终值见属性块里的基础暴击。
        // 基线同样取自 critForLevel(0)，与判定侧同一个来源。
        if (SceneryBlades.hasCrit(stack)) {
            WavebandUpgrade.CritStats crit = WavebandUpgrade.critOf(stack);
            WavebandUpgrade.CritStats base = WavebandUpgrade.critForLevel(0);
            tooltip.add(Component.translatable("tooltip.scenerystaff.waveband_crit",
                    "+" + (crit.chancePercent() - base.chancePercent()) + "%",
                    "+" + (crit.damagePercent() - base.damagePercent()) + "%")
                    .withStyle(ChatFormatting.GRAY));
        }
    }
}
