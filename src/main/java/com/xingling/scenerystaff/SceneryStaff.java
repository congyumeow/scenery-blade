package com.xingling.scenerystaff;

import com.xingling.scenerystaff.compat.ironsspells.IronsSpellsCompat;
import com.xingling.scenerystaff.network.SyncChargeMessage;
import com.xingling.scenerystaff.registry.*;
import com.xingling.scenerystaff.sa.SaCharge;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(SceneryStaff.MODID)
public class SceneryStaff {
    // Define mod id in a common place for everything to reference
    public static final String MODID = "scenerystaff";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();

    /**
     * 本模组的 {@link ResourceLocation} 工厂。
     * <p>
     * 1.20.1 上 {@code ResourceLocation} 还没有静态工厂方法（{@code parse} / {@code fromNamespaceAndPath}
     * 是 1.21 才有的），命名空间为 {@code minecraft} 时构造器还会拒绝，因此统一走这里。
     */
    public static ResourceLocation prefix(String path) {
        return new ResourceLocation(MODID, path);
    }

    public SceneryStaff() {
        // 1.20.1 上 @Mod 的构造器不注入 IEventBus，需要从 FMLJavaModLoadingContext 取
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        SoundRegistry.REGISTRY.register(modEventBus);
        BlockRegistry.REGISTRY.register(modEventBus);
        ItemRegistry.REGISTRY.register(modEventBus);
        ItemTabRegistry.REGISTRY.register(modEventBus);
        SERegistry.REGISTRY.register(modEventBus);
        SARegistry.REGISTRY.register(modEventBus);
        ComboStateRegistry.COMBO_STATE.register(modEventBus);
        EntityRegistry.REGISTRY.register(modEventBus);

        // 玩家能力（SA 充能值：持久化在玩家身上）
        MinecraftForge.EVENT_BUS.register(SaCharge.class);

        // 网络通道：SA 充能的服务端 → 客户端同步（1.20.1 的能力不会自动同步）
        SyncChargeMessage.register();

        // 铁魔法是可选联动。必须用 isLoaded 守卫把调用包起来：
        // 否则加载 SceneryStaff 时会尝试解析 IronsSpellsCompat，未安装铁魔法就 NoClassDefFoundError；
        // 放在守卫里，这个类才只会在铁魔法确实存在时加载。
        if (ModList.get().isLoaded("irons_spellbooks")) {
            IronsSpellsCompat.register();
        }
    }
}
