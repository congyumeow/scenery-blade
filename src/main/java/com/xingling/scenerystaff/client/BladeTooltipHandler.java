package com.xingling.scenerystaff.client;

import com.xingling.scenerystaff.SceneryStaff;
import com.xingling.scenerystaff.item.SceneryBlades;
import com.xingling.scenerystaff.item.WavebandUpgrade;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.List;
import java.util.Locale;

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

    private static final int MAX_DESC_LINES = 8;

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
        for (int i = 1; i <= MAX_DESC_LINES; i++) {
            String key = prefix + i;
            MutableComponent line = Component.translatable(key);
            // 语言文件中不存在该键时原样返回键名 —— 视为描述结束
            if (line.getString().equals(key)) {
                break;
            }
            tooltip.add(line.withStyle(ChatFormatting.LIGHT_PURPLE));
        }

        appendWavebandUpgrade(tooltip, stack);
    }

    /**
     * 追加「达妮娅的回音频段」的强化信息（强化等级、攻击力加成、最终刀的领域冷却缩减）。
     * <p>
     * 数值全部来自 {@link WavebandUpgrade}，与铁砧强化和攻击力事件共用同一份常量，改一处即可。
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

        if (SceneryBlades.isFinalBlade(stack)) {
            float seconds = level * WavebandUpgrade.DOMAIN_COOLDOWN_REDUCTION_TICKS / 20.0F;
            tooltip.add(Component.translatable("tooltip.scenerystaff.waveband_domain_cd",
                    String.format(Locale.ROOT, "%.1f", seconds)).withStyle(ChatFormatting.GRAY));
        }
    }
}
