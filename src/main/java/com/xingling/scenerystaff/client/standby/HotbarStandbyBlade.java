package com.xingling.scenerystaff.client.standby;

import com.xingling.scenerystaff.blade.SceneryBlades;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * 「不要把快捷栏第 0 格的刀当作待机刀渲染」这条判定的<b>唯一实现</b>。
 * <p>
 * 为什么单独抽一个类：这条判定要由两个 mixin 分别注入（{@code LayerMainBladeMixin} 与
 * {@code LayerSlashBladeMixin}），但判定逻辑只应有一处——否则日后改判定要改两遍，
 * 漏一处就是两处行为不一致。两个 mixin 各自的适用场景见其类注释。
 * <p>
 * <b>为什么不能放在 {@code com.xingling.scenerystaff.mixin} 包里</b>：那个包被
 * {@code scenerystaff.mixins.json} 声明为 mixin 包，Mixin 会拒绝任何对包内类的直接引用
 * （{@code IllegalClassLoadError: ... is in a defined mixin package ... and cannot be referenced
 * directly}）。注入后的回调恰好要直接调用本类，所以它必须待在 mixin 包<b>之外</b>。
 *
 * @see #shouldSkipHotbarStandbyBlade(LivingEntity)
 */
public final class HotbarStandbyBlade {

    /**
     * 日志器。这里刻意用 {@code LoggerFactory} 直接取，而不是引用本模组主类上的
     * {@code LOGGER} 字段：本类在 mixin 回调里被调用，不该顺手把主类的
     * 静态初始化一起拖进来。
     */
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("scenerystaff");

    private static boolean loggedHooked;

    private HotbarStandbyBlade() {
    }

    /**
     * 是否应当取消这次「待机刀」渲染。
     * <p>
     * 取值与目标方法保持一致：只看快捷栏第 0 格。命中后调用方用 {@code ci.cancel()}
     * 截断整条渲染路径。
     *
     * @param entity 被渲染的生物，通常是玩家
     * @return {@code true} 表示快捷栏第 0 格是<b>本模组的刀</b>，应跳过这次渲染
     */
    public static boolean shouldSkipHotbarStandbyBlade(LivingEntity entity) {
        if (!(entity instanceof Player player)) {
            return false;
        }
        ItemStack hotbarBlade = player.getInventory().getItem(0);
        if (hotbarBlade.isEmpty()) {
            return false;
        }
        // 仅拦截本模组的刀（以刀模型命名空间判定），其他拔刀剑保持原有的待机刀表现。
        // 判定走 SceneryBlades.isOurModel：这里要的是「模型属于本模组」，
        // 与 SceneryBlades.isSceneryBlade（按注册 ID、只认布景之杖线）不是一回事，
        // 用错会漏掉启明星线的两把刀。
        return SceneryBlades.isOurModel(hotbarBlade);
    }

    /**
     * 注入成功时的自检日志，整个进程只打一行 {@code DEBUG}。
     * <p>
     * 一条注入是否生效在本模组里是容易判断错的：拔刀剑换版本后目标方法可能换类，
     * mixin 会<b>静默</b>不生效（{@code require = 0}，见两个 mixin 的说明），
     * 表现为「副手没装备却显示待机刀」这个 bug 又回来了，而日志里一个字都没有。
     * 调试时打开 DEBUG 就能直接看出这次到底注上了哪个版本的目标类。
     *
     * @param target 本次生效的目标类全名
     */
    public static void logHookedOnce(String target) {
        if (!loggedHooked) {
            loggedHooked = true;
            LOGGER.debug("[scenerystaff] 待机刀渲染拦截已注入 {}", target);
        }
    }
}
