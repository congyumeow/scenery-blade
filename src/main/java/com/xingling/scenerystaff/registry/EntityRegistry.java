package com.xingling.scenerystaff.registry;

import com.xingling.scenerystaff.SceneryStaff;
import com.xingling.scenerystaff.entity.ErosionDomainEntity;
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
}
