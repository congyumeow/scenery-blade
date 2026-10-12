package com.xingling.scenerystaff.compat.jade;

import com.xingling.scenerystaff.SceneryStaff;
import net.minecraft.world.entity.LivingEntity;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

/**
 * Jade 兼容入口：把「聚合·聚爆」的层数与剩余衰减时间显示在生物 tooltip 上。
 * <p>
 * 这是整个兼容层里<b>唯一</b>由 Jade 主动扫描加载的类（{@link WailaPlugin} 注解）。
 * 未安装 Jade 时它不会被加载，本模组其它代码也从不引用它，
 * 因此没有 Jade 也能正常加载——build.gradle 中 Jade 是 {@code compileOnly}，
 * 既不打进本模组 jar，也不构成运行时依赖。
 * <p>
 * 注解的 {@code value} 留空，表示插件与主模组同属一个 jar。
 */
@WailaPlugin
public class PolymerizationJadePlugin implements IWailaPlugin {

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerEntityDataProvider(new PolymerizationServerData(), LivingEntity.class);
        SceneryStaff.LOGGER.info("[Jade] 聚合·聚爆 服务端数据同步已注册");
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerEntityComponent(new PolymerizationClientProvider(), LivingEntity.class);
        SceneryStaff.LOGGER.info("[Jade] 聚合·聚爆 tooltip 显示已注册");
    }
}
