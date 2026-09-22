package com.xingling.scenerystaff.compat.jade;

import com.xingling.scenerystaff.SceneryStaff;
import com.xingling.scenerystaff.se.PolymerizationSE;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IServerDataProvider;

/**
 * 服务端 → 客户端的数据同步。
 * <p>
 * 层数存在实体的持久化数据里，那是<b>纯服务端</b>的，客户端读不到，所以必须走 Jade 的
 * {@link IServerDataProvider} 通道，客户端再用 {@link snownee.jade.api.Accessor#getServerData()} 取回。
 * <p>
 * 这里发的是<b>原始层数与最后一次叠层时刻</b>，而不是此刻的有效层数：
 * 客户端拿这两个值就能用 {@link PolymerizationSE#decayedCount} /
 * {@link PolymerizationSE#ticksUntilDecay} 现算出「层数」与「倒计时」，
 * 于是 tooltip 每帧都是实时的，不依赖 Jade 重新请求服务端数据的频率。
 */
public class PolymerizationServerData implements IServerDataProvider<EntityAccessor> {

    public static final ResourceLocation UID = SceneryStaff.prefix("polymerization");

    @Override
    public void appendServerData(CompoundTag data, EntityAccessor accessor) {
        if (!(accessor.getEntity() instanceof LivingEntity living)) {
            return;
        }
        CompoundTag persistent = living.getPersistentData();
        int raw = persistent.getInt(PolymerizationSE.TAG_COUNT);
        if (raw <= 0) {
            return;
        }
        // 已经衰减光的就不必发送——客户端算出来也是 0，不会显示
        if (PolymerizationSE.getEffectiveCount(living) <= 0) {
            return;
        }
        data.putInt(PolymerizationSE.TAG_COUNT, Math.min(raw, PolymerizationSE.MAX_STACK));
        data.putLong(PolymerizationSE.TAG_LAST, persistent.getLong(PolymerizationSE.TAG_LAST));
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }
}
