package com.xingling.scenerystaff.registry;

import com.xingling.scenerystaff.SceneryStaff;
import com.xingling.scenerystaff.entity.ErosionDomainEntity;
import com.xingling.scenerystaff.entity.GiantSwordEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class EntityRegistry {
    public static final DeferredRegister<EntityType<?>> REGISTRY =
            DeferredRegister.create(Registries.ENTITY_TYPE, SceneryStaff.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<ErosionDomainEntity>> EROSION_DOMAIN =
            REGISTRY.register("erosion_domain",
                    () -> EntityType.Builder.<ErosionDomainEntity>of(ErosionDomainEntity::new, MobCategory.MISC)
                            .sized(3.0F, 0.5F) // 范围大小，实际检测用半径
                            .build(SceneryStaff.prefix("erosion_domain").toString()));

    /**
     * 「巨剑」剑技召唤的巨剑实体。
     * <p>
     * {@code updateInterval(1)} 让位置每 tick 同步一次，配合原版插值可让 30 格/秒 的下落看起来很顺；
     * 碰撞由实体自身的 {@code noPhysics} 关闭，这里的 sized 只是视觉尺寸。
     */
    public static final DeferredHolder<EntityType<?>, EntityType<GiantSwordEntity>> GIANT_SWORD =
            REGISTRY.register("giant_sword",
                    () -> EntityType.Builder.<GiantSwordEntity>of(GiantSwordEntity::new, MobCategory.MISC)
                            .sized(4.0F, 8.0F)
                            .clientTrackingRange(8)
                            .updateInterval(1)
                            .build(SceneryStaff.prefix("giant_sword").toString()));
}
