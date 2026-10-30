package com.xingling.scenerystaff.event;

import com.xingling.scenerystaff.SceneryStaff;
import com.xingling.scenerystaff.blade.SceneryBlades;
import mods.flammpfeil.slashblade.capability.slashblade.BladeStateAccess;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.entity.EntityDrive;
import mods.flammpfeil.slashblade.entity.EntitySlashEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

/**
 * 把本模组几把终盘刀打出的刀光（{@link EntitySlashEffect} / {@link EntityDrive}）染成刀的 effectColor。
 * <p>
 * <b>为什么需要这一层</b>：拔刀剑自己生成这些实体时，颜色要么在 {@code AttackManager.doSlash}
 * 里写死成 {@code 0xFFFFFF}（普通挥击的刀光），要么由内置剑技状态传进去（幻影刃走
 * {@code Drive.doSlash}，颜色是调用方给的常量）。所以充能不足被我们替换成
 * {@code slashblade:drive_horizontal} 时，出来的刀光是纯白的，和刀本身的配色无关。
 * <p>
 * 与其去逆向每个内置剑技状态用的是哪个颜色常量，这里在实体加入世界时统一覆写：
 * 只要发起者是手持本模组终盘刀的生物，就把颜色改成刀状态里记录的 {@code effectColor}。
 * 这样普通挥击、幻影刃、以及以后任何走这两个实体的剑技都会自动跟着刀的颜色走。
 * <p>
 * <b>只在服务端覆写</b>：{@code COLOR} 是同步数据（{@code EntityDataAccessor}），
 * 服务端改完会自动同步到客户端；客户端再改属于无用功。
 * <p>
 * 覆写时机可靠：{@code Drive.doSlash} / {@code AttackManager.doSlash} 都是先 {@code setColor}
 * 再 {@code addFreshEntity}，而本事件在加入世界时触发，因此必定后于原颜色写入。
 */
@EventBusSubscriber(modid = SceneryStaff.MODID)
public final class BladeSlashColorHandler {

    private BladeSlashColorHandler() {
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        Entity entity = event.getEntity();

        Entity shooter;
        if (entity instanceof EntityDrive drive) {
            shooter = drive.getShooter();
        } else if (entity instanceof EntitySlashEffect slashEffect) {
            shooter = slashEffect.getShooter();
        } else {
            return;
        }
        if (!(shooter instanceof LivingEntity living)) {
            return;
        }

        ItemStack stack = living.getMainHandItem();
        if (!isGraduationBlade(stack)) {
            return;
        }
        int color = effectColorOf(stack);
        if (color < 0) {
            return;
        }

        if (entity instanceof EntityDrive drive) {
            drive.setColor(color);
        } else if (entity instanceof EntitySlashEffect slashEffect) {
            slashEffect.setColor(color);
        }
    }

    /** 三把终盘刀：布景之杖 / 永远的启明星 / 不屈命定之冠 */
    private static boolean isGraduationBlade(ItemStack stack) {
        return SceneryBlades.isFinalBlade(stack)
                || SceneryBlades.isQimingxing(stack)
                || SceneryBlades.isCartethyia(stack);
    }

    /** 取刀状态里的 effectColor（RGB）；取不到或不是刀时返回 -1 */
    private static int effectColorOf(ItemStack stack) {
        return BladeStateAccess.of(stack)
                .map(ISlashBladeState::getEffectColor)
                .map(c -> c.getRGB() & 0xFFFFFF)
                .orElse(-1);
    }
}
