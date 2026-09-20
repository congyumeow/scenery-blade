package com.xingling.scenerystaff.sa;

import com.xingling.scenerystaff.SceneryStaff;
import com.xingling.scenerystaff.entity.ErosionDomainEntity;
import com.xingling.scenerystaff.registry.EntityRegistry;
import com.xingling.scenerystaff.registry.SoundRegistry;
import mods.flammpfeil.slashblade.slasharts.SlashArts;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.Random;

/**
 * 侵蚀领域剑技。
 * 该版本 SlashBlade 的 SlashArts 没有 release() 方法，剑技行为通过 ComboState
 * 驱动：剑技的普通/完美变式都指向 {@code scenerystaff:erosion_domain} 连击状态，
 * 由该状态的 TimeLineTickAction 在释放时生成领域实体（参考 SlashBlade 内置剑技写法）。
 */
public class ErosionDomainSA extends SlashArts {
    private static final Random RANDOM = new Random();

    public ErosionDomainSA() {
        // 普通/完美变式统一进入 erosion_domain 连击状态
        super(entity -> SceneryStaff.prefix("erosion_domain"));
    }

    /**
     * 由连击状态的时间轴回调：播放随机音效并在玩家位置生成领域实体（仅服务端）。
     */
    public static void doErosionDomain(LivingEntity user) {
        if (!(user.level() instanceof ServerLevel serverLevel)) return;
        if (!(user instanceof Player player)) return;

        // 播放随机音效
        SoundEvent[] sounds = {
                SoundRegistry.DENIA1.get(),
                SoundRegistry.DENIA2.get(),
                SoundRegistry.DENIA3.get()
        };
        SoundEvent chosen = sounds[RANDOM.nextInt(sounds.length)];
        serverLevel.playSound(null, user.getX(), user.getY(), user.getZ(),
                chosen, user.getSoundSource(), 1.0F, 1.0F);

        // 生成领域实体（生成位置随玩家）
        ErosionDomainEntity domain = new ErosionDomainEntity(EntityRegistry.EROSION_DOMAIN.get(), serverLevel);
        domain.setPos(player.getX(), player.getY(), player.getZ());
        domain.setOwner(player);
        domain.setDuration(ErosionDomainEntity.DURATION_TICKS); // 30 秒 (20 ticks/sec)
        serverLevel.addFreshEntity(domain);
    }
}
