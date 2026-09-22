package com.xingling.scenerystaff.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CakeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 达妮娅的生日蛋糕（方块形态）。
 * <p>
 * 食用方式与原版蛋糕完全一致：放置后空手右键方块吃一片，共 7 片（bites 0~6），吃完消失。
 * 与原版蛋糕的区别：
 * <ul>
 *   <li>每片恢复量按<b>附魔金苹果</b>（4 饥饿值 / 1.2 饱和度），而非原版蛋糕的 2 / 0.1；</li>
 *   <li>食用后获得<b>附魔金苹果的三项效果</b>：生命恢复 II（20s）、抗性提升 I（5min）、抗火 I（5min）。</li>
 * </ul>
 * 另外禁用了"插蜡烛"交互（原版会把方块替换成蜡烛蛋糕）。
 */
public class BirthdayCakeBlock extends CakeBlock {

    /** 与父类同类型（MapCodec 泛型不变，不能协变），仅把工厂方法换成子类 */
    public static final MapCodec<CakeBlock> CODEC = Block.<CakeBlock>simpleCodec(BirthdayCakeBlock::new);

    /** 每片恢复的饥饿值 —— 按附魔金苹果 */
    private static final int NUTRITION = 4;
    /** 每片恢复的饱和度 —— 按附魔金苹果 */
    private static final float SATURATION = 1.2F;

    public BirthdayCakeBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<CakeBlock> codec() {
        return CODEC;
    }

    /** 不参与原版"插蜡烛 → 变成蜡烛蛋糕"的交互 */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hitResult) {
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    /** 空手右键：吃一片（保持与原版蛋糕相同的判定与预测流程） */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hitResult) {
        if (level.isClientSide) {
            if (eatSlice(level, pos, state, player).consumesAction()) {
                return InteractionResult.SUCCESS;
            }
            if (player.getItemInHand(InteractionHand.MAIN_HAND).isEmpty()) {
                return InteractionResult.CONSUME;
            }
        }
        return eatSlice(level, pos, state, player);
    }

    /** 吃一片：恢复量按附魔金苹果，并在服务端给予对应效果 */
    private static InteractionResult eatSlice(Level level, BlockPos pos, BlockState state, Player player) {
        if (!player.canEat(false)) {
            return InteractionResult.PASS;
        }

        player.awardStat(net.minecraft.stats.Stats.EAT_CAKE_SLICE);
        player.getFoodData().eat(NUTRITION, SATURATION);

        if (!level.isClientSide) {
            // 附魔金苹果的三项效果
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 400, 1));
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 6000, 0));
            player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 6000, 0));
        }

        int bites = state.getValue(BITES);
        level.gameEvent(player, GameEvent.EAT, pos);
        if (bites < MAX_BITES) {
            level.setBlock(pos, state.setValue(BITES, bites + 1), 3);
        } else {
            level.removeBlock(pos, false);
            level.gameEvent(player, GameEvent.BLOCK_DESTROY, pos);
        }
        return InteractionResult.SUCCESS;
    }
}
