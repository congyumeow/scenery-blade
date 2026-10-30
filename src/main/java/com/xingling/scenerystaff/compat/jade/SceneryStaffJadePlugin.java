package com.xingling.scenerystaff.compat.jade;

import com.xingling.scenerystaff.SceneryStaff;
import net.minecraft.world.entity.LivingEntity;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

/**
 * Jade 兼容入口：把本模组的两个「层数型」状态显示在生物 tooltip 上。
 * <ul>
 *   <li><b>聚合·聚爆</b>：层数 / 上限 + 下次衰减倒计时；</li>
 *   <li><b>风蚀</b>：层数 / 上限 + 下次结算与下次衰减倒计时。</li>
 * </ul>
 * 两者都走同一套结构：服务端 {@code IServerDataProvider} 发数据 → 客户端
 * {@code IEntityComponentProvider} 渲染，原因是「剩余时间」依赖实体本地 tickCount，
 * 各端不同步，只能由服务端把绝对时刻换算好发过来。
 * <p>
 * 这是整个兼容层里由 Jade 主动扫描加载的类（{@link WailaPlugin} 注解）。
 * 未安装 Jade 时它不会被加载，本模组其它代码也从不引用它，因此没有 Jade 也能正常加载——
 * build.gradle 中 Jade 是 {@code compileOnly}，既不打进本模组 jar，也不构成运行时依赖。
 */
@WailaPlugin
public class SceneryStaffJadePlugin implements IWailaPlugin {

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerEntityDataProvider(new PolymerizationServerData(), LivingEntity.class);
        registration.registerEntityDataProvider(new WindErosionServerData(), LivingEntity.class);
        SceneryStaff.LOGGER.info("[Jade] 聚爆 / 风蚀 服务端数据同步已注册");
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerEntityComponent(new PolymerizationClientProvider(), LivingEntity.class);
        registration.registerEntityComponent(new WindErosionClientProvider(), LivingEntity.class);
        SceneryStaff.LOGGER.info("[Jade] 聚爆 / 风蚀 tooltip 显示已注册");
    }
}
