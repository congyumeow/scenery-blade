package com.xingling.scenerystaff.se;

import com.xingling.scenerystaff.SceneryStaff;
import com.xingling.scenerystaff.blade.SceneryBlades;
import com.xingling.scenerystaff.entity.ErosionDomainEntity;
import com.xingling.scenerystaff.registry.MobEffectRegistry;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 风蚀叠层：用「不屈命定之冠」造成伤害时，给目标叠一层风蚀。
 * <p>
 * 层数上限随施术者是否手持不屈命定之冠变化：
 * <ul>
 *   <li>平时 {@link WindErosionEffect#DEFAULT_MAX_STACKS} 层；</li>
 *   <li>手持不屈命定之冠时 {@link WindErosionEffect#CARTETHYIA_MAX_STACKS} 层。</li>
 * </ul>
 * 上限同时会在 {@link WindErosionEffect#applyEffectTick} 里按施术者<b>当前</b>手持物持续重算，
 * 因此换下刀后已叠的层数会被削回默认上限，而不是停在高位。
 * <p>
 * 1.21 上是 {@code LivingIncomingDamageEvent}；1.20.1 没有这个事件，对应的可取值版本是
 * {@code LivingHurtEvent}（护甲减免之后、实际扣血之前），对「造成伤害即叠层」的语义没有影响。
 */
@Mod.EventBusSubscriber(modid = SceneryStaff.MODID)
public final class WindErosionApplier {

    private WindErosionApplier() {
    }

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        if (!(event.getSource().getEntity() instanceof Player player)) {
            return;
        }
        // 侵蚀领域的每秒伤害同样把玩家记为归属，但它只是「站在领域里」的持续伤害，
        // 不应被当成一次攻击去叠风蚀——否则拿不屈命定之冠站进自家领域就会自动叠层。
        if (event.getSource().getDirectEntity() instanceof ErosionDomainEntity) {
            return;
        }
        ItemStack held = player.getMainHandItem();
        if (!SceneryBlades.isCartethyia(held)) {
            return;
        }
        if (event.getAmount() <= 0.0F) {
            return;
        }

        LivingEntity target = event.getEntity();
        int cap = WindErosionEffect.capFor(player);

        MobEffectInstance current = target.getEffect(MobEffectRegistry.WIND_EROSION.get());
        int currentAmplifier = current == null ? -1 : current.getAmplifier();
        int nextAmplifier = Math.min(currentAmplifier + 1, cap - 1);
        if (nextAmplifier <= currentAmplifier) {
            return; // 已到上限，无需刷新
        }

        // 无限时长（-1）：衰减由效果自身的 tick 手动处理，不走原版到期逻辑。
        // 注意必须走 setStacks：同名效果直接 addEffect 无法降级，统一入口避免以后踩坑。
        WindErosionEffect.setStacks(target, nextAmplifier);

        // 记下施术者：风蚀的伤害按【玩家】的生命与护甲结算（与聚爆一致），
        // 而效果本身不携带来源信息，只能写在目标身上供结算时取回。
        target.getPersistentData().putUUID(WindErosionEffect.KEY_CASTER, player.getUUID());
        // 一并记下本次生效的上限，供 Jade tooltip 当作分母显示
        target.getPersistentData().putInt(WindErosionEffect.KEY_CAP, cap);
    }
}
