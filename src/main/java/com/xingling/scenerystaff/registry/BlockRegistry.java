package com.xingling.scenerystaff.registry;

import com.xingling.scenerystaff.SceneryStaff;
import com.xingling.scenerystaff.block.BirthdayCakeBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class BlockRegistry {
    public static final DeferredRegister<Block> REGISTRY =
            DeferredRegister.create(Registries.BLOCK, SceneryStaff.MODID);

    /**
     * 达妮娅的生日蛋糕（方块）：可放置，放置后空手右键切食，共 7 片。
     * 方块属性参考原版蛋糕（forceSolidOn / 0.5 硬度 / WOOL 音效 / 破坏推开）。
     */
    public static final DeferredHolder<Block, Block> DENIA_BIRTHDAY_CAKE =
            REGISTRY.register("denia_birthday_cake", () -> new BirthdayCakeBlock(
                    BlockBehaviour.Properties.of()
                            .forceSolidOn()
                            .strength(0.5F)
                            .sound(SoundType.WOOL)
                            .pushReaction(PushReaction.DESTROY)));
}
