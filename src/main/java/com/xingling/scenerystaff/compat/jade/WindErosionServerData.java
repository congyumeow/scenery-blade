package com.xingling.scenerystaff.compat.jade;

import com.xingling.scenerystaff.SceneryStaff;
import com.xingling.scenerystaff.registry.MobEffectRegistry;
import com.xingling.scenerystaff.se.WindErosionEffect;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IServerDataProvider;

/**
 * 风蚀的服务端 → 客户端数据同步，写法与 {@link PolymerizationServerData} 保持一致。
 * <p>
 * <b>为什么不能直接让客户端读效果</b>：层数（effect 的 amplifier）确实是同步的，但
 * 「距离下一次衰减/结算还有多久」取决于实体的 {@code tickCount}，那是各端本地计数器，
 * 不随实体同步。所以由服务端把两个动作的<b>绝对发生时刻</b>换算出来发过去，
 * 客户端拿本地的 {@code getGameTime()} 一减就能连续倒计时。
 * <p>
 * 上限也不是固定值（默认 3、持有不屈命定之冠时 6），随层数一起发，供显示端做分母。
 */
public class WindErosionServerData implements IServerDataProvider<EntityAccessor> {

    public static final ResourceLocation UID = SceneryStaff.prefix("wind_erosion");

    public static final String TAG_STACKS = "WindErosionStacks";
    public static final String TAG_CAP = "WindErosionCap";
    public static final String TAG_DECAY_AT = "WindErosionDecayAt";

    @Override
    public void appendServerData(CompoundTag data, EntityAccessor accessor) {
        if (!(accessor.getEntity() instanceof LivingEntity living)) {
            return;
        }
        MobEffectInstance effect = living.getEffect(MobEffectRegistry.WIND_EROSION);
        if (effect == null) {
            return;
        }
        int stacks = effect.getAmplifier() + 1;
        if (stacks <= 0) {
            return;
        }

        CompoundTag persistent = living.getPersistentData();
        int cap = persistent.contains(WindErosionEffect.KEY_CAP)
                ? persistent.getInt(WindErosionEffect.KEY_CAP)
                : WindErosionEffect.CARTETHYIA_MAX_STACKS;

        int tick = living.tickCount;
        int untilDecay = WindErosionEffect.DECAY_INTERVAL_TICKS
                - Math.floorMod(tick, WindErosionEffect.DECAY_INTERVAL_TICKS);

        long now = living.level().getGameTime();
        data.putInt(TAG_STACKS, stacks);
        data.putInt(TAG_CAP, cap);
        data.putLong(TAG_DECAY_AT, now + untilDecay);
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }
}
