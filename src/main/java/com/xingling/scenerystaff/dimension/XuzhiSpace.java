package com.xingling.scenerystaff.dimension;

import com.xingling.scenerystaff.SceneryStaff;
import com.xingling.scenerystaff.blade.SceneryBlades;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

/**
 * 「虚质空间」维度的进入/离开。
 * <p>
 * 维度本身是纯数据驱动的（{@code data/scenerystaff/dimension/xuzhi_space.json} +
 * {@code dimension_type/xuzhi_space.json}）：末地类环境、超平坦、地基是 3 层{@code scenerystaff:xuzhi_block}，
 * 世界最低层 {@code min_y = 0}，因此地基占据 Y=0..2、玩家脚踩在 <b>Y=3</b>。
 * <p>
 * <b>进入条件</b>：主手或副手持有两把终盘刀之一（布景之杖 / 永远的启明星），
 * 判定复用 {@link SceneryBlades#isTopTier}。<b>同一个按键负责进入与离开</b>。
 * <p>
 * <b>返回位置</b>存在玩家的持久化数据里（维度 id + 坐标 + 朝向），因此从哪个维度进的
 * 就回哪个维度，不假定一定是主世界。
 */
public final class XuzhiSpace {

    /** 虚质空间的维度键 */
    public static final ResourceKey<Level> DIMENSION =
            ResourceKey.create(Registries.DIMENSION, SceneryStaff.prefix("xuzhi_space"));

    /** 落点：地基顶面之上（Y=0..2 是地基，脚踩 Y=3） */
    public static final double SPAWN_X = 0.5D;
    public static final double SPAWN_Y = 3.0D;
    public static final double SPAWN_Z = 0.5D;

    private static final String KEY_DIM = "xuzhi_return_dim";
    private static final String KEY_X = "xuzhi_return_x";
    private static final String KEY_Y = "xuzhi_return_y";
    private static final String KEY_Z = "xuzhi_return_z";
    private static final String KEY_YAW = "xuzhi_return_yaw";
    private static final String KEY_PITCH = "xuzhi_return_pitch";

    /**
     * 上一次<b>离开虚质空间时</b>所在的坐标。
     * <p>
     * 与上面的 {@code return_*} 是两套东西，方向相反：
     * <ul>
     *   <li>{@code return_*} —— 进入时记下「空间外的位置」，离开时送回这里；</li>
     *   <li>{@code last_*} —— 离开时记下「空间内的位置」，<b>下次再进时落在那里</b>。</li>
     * </ul>
     * 没有 {@code last_*} 时（第一次进入）才回落到固定的出生点。
     */
    private static final String KEY_LAST_X = "xuzhi_last_x";
    private static final String KEY_LAST_Y = "xuzhi_last_y";
    private static final String KEY_LAST_Z = "xuzhi_last_z";
    private static final String KEY_LAST_YAW = "xuzhi_last_yaw";
    private static final String KEY_LAST_PITCH = "xuzhi_last_pitch";

    private XuzhiSpace() {
    }

    /**
     * 处理一次按键请求：在虚质空间内则返回原地，否则进入。
     * <p>
     * 只应在服务端调用；持有条件不满足时静默忽略（客户端也做了同样的判断，
     * 但服务端必须自己再校验一次，否则改客户端就能随便传送）。
     */
    public static void toggle(ServerPlayer player) {
        if (!SceneryBlades.isHolding(player, SceneryBlades::isTopTier)) {
            return;
        }

        ServerLevel current = player.serverLevel();
        if (current.dimension().equals(DIMENSION)) {
            leave(player, current);
        } else {
            enter(player, current);
        }
    }

    private static void enter(ServerPlayer player, ServerLevel from) {
        ServerLevel target = player.server.getLevel(DIMENSION);
        if (target == null) {
            SceneryStaff.LOGGER.warn("[虚质空间] 维度未加载，无法进入（检查 dimension/dimension_type JSON 是否被打包）");
            return;
        }

        // 落点：优先回到上次离开虚质空间时的位置，第一次进入才用固定出生点
        CompoundTag data = player.getPersistentData();
        double spawnX = data.contains(KEY_LAST_X) ? data.getDouble(KEY_LAST_X) : SPAWN_X;
        double spawnY = data.contains(KEY_LAST_Y) ? data.getDouble(KEY_LAST_Y) : SPAWN_Y;
        double spawnZ = data.contains(KEY_LAST_Z) ? data.getDouble(KEY_LAST_Z) : SPAWN_Z;
        float spawnYaw = data.contains(KEY_LAST_YAW) ? data.getFloat(KEY_LAST_YAW) : player.getYRot();
        float spawnPitch = data.contains(KEY_LAST_PITCH) ? data.getFloat(KEY_LAST_PITCH) : player.getXRot();

        // 记下返回点（维度 + 坐标 + 朝向），离开时原路送回
        data.putString(KEY_DIM, from.dimension().location().toString());
        data.putDouble(KEY_X, player.getX());
        data.putDouble(KEY_Y, player.getY());
        data.putDouble(KEY_Z, player.getZ());
        data.putFloat(KEY_YAW, player.getYRot());
        data.putFloat(KEY_PITCH, player.getXRot());

        player.teleportTo(target, spawnX, spawnY, spawnZ, spawnYaw, spawnPitch);
    }

    private static void leave(ServerPlayer player, ServerLevel from) {
        CompoundTag data = player.getPersistentData();

        // 先记下「这次离开时在空间内的位置」，下次进入就落在那里
        data.putDouble(KEY_LAST_X, player.getX());
        data.putDouble(KEY_LAST_Y, player.getY());
        data.putDouble(KEY_LAST_Z, player.getZ());
        data.putFloat(KEY_LAST_YAW, player.getYRot());
        data.putFloat(KEY_LAST_PITCH, player.getXRot());

        ResourceLocation dimId = ResourceLocation.tryParse(data.getString(KEY_DIM));
        ServerLevel target = dimId == null ? null : player.server.getLevel(ResourceKey.create(Registries.DIMENSION, dimId));
        if (target == null) {
            // 返回维度不存在（例如存档被改过）：退回该维度的默认出生点
            ServerLevel overworld = player.server.overworld();
            player.teleportTo(overworld, overworld.getSharedSpawnPos().getX() + 0.5D,
                    overworld.getSharedSpawnPos().getY(), overworld.getSharedSpawnPos().getZ() + 0.5D,
                    player.getYRot(), player.getXRot());
            return;
        }

        player.teleportTo(target,
                data.getDouble(KEY_X), data.getDouble(KEY_Y), data.getDouble(KEY_Z),
                data.getFloat(KEY_YAW), data.getFloat(KEY_PITCH));
    }
}
