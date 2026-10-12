# scenery-blade — NeoForge 1.20.1 适配

SceneryBlade（布景之杖附属）在 **Minecraft 1.20.1 / NeoForge 47.x** 上的移植工程。

- 上游：<https://github.com/congyumeow/scenery-blade>（原工程为 Minecraft 1.21.1 / NeoForge 21.1.x）
- 目标平台：Minecraft 1.20.1，NeoForge `1.20.1-47.1.106`（FML 47.2.2，Java 17）
- 必需前置：SlashBlade: Resharped 1.20.1（`1.9.65`，CurseForge file 8090912）
- 可选联动：Jade 11.x（仅编译期）、Iron's Spells 'n Spellbooks 1.20.1（仅编译期）

## 依赖来源

工程内**不放任何 mod jar**，依赖全部走远程仓库解析：

| 依赖 | 坐标 | 仓库 |
| --- | --- | --- |
| SlashBlade: Resharped 1.9.65 | `curse.maven:slashblade-resharped-1022428:8090912` | CurseMaven |
| Jade 11.13.3 | `maven.modrinth:jade:xJQHCmWJ` | Modrinth |
| Iron's Spells 3.4.0.9 | `maven.modrinth:irons-spells-n-spellbooks:9dM8iFPD` | Modrinth |

- SlashBlade 用 **`modImplementation`**：必须让 legacyforge 对它做重映射；
- Jade 与铁魔法用 **`compileOnly`**：既不进本模组的 jar，也不成为运行时依赖，
  未安装时对应兼容类不会被加载（铁魔法额外加 `transitive = false`，不拖入它自己的前置）。

首次构建需要联网（并经代理）。

## 构建

```bash
./gradlew build          # 产出 build/libs/scenerystaff-1.20.1-1.0.3.jar（已重混淆）
./gradlew compileJava    # 只做编译检查
./gradlew runClient      # 启动客户端调试
./gradlew runData        # 重新生成数据包资源（src/generated/resources）
```

首次构建需要下载并反编译 Minecraft，耗时较长；`build/libs` 里的 jar 已经过 SRG 重混淆，可直接放进 `mods/`。

### 网络（重要）

本机**直连不通** `maven.neoforged.net` —— Java 会把它解析到连不通的地址，
实测报错为 `Connect to maven.neoforged.net:443 failed: Connection timed out: getsockopt`
（删掉依赖缓存后必现）。所以首次构建 / 更换依赖版本时必须能走通代理。

代理由一份 **Gradle init script 自动接管**，通常不需要手工做什么：

```
C:\Users\murasame\.gradle\init.d\scenerystaff-proxy.init.gradle          ← IDEA 用这个
E:\slashblade_mod\.gradle-home\init.d\scenerystaff-proxy.init.gradle    ← 终端脚本用这个
```

它由 Gradle 自己加载，因此**终端和 IDEA 都会生效**（IDEA 不经过任何包装脚本）。
探测顺序：Java 系统代理 → 注册表 `ProxyEnable/ProxyServer` → 注册表里已配但开关关闭的地址。
最后一步很关键：本机就是「代理在 `127.0.0.1:7897` 上可用、但 Windows 系统代理开关是关的」这种状态。
为避免误用被其它程序占用的端口，候选地址会**真发一次 `CONNECT` 并校验返回 2xx** 才算数。

构建日志里会打印一行 `[scenerystaff] 已启用代理 127.0.0.1:7897（来源 …）`，
用它确认代理是否真的生效。都探测不到时会明确提示退回直连。

#### 备用：包装脚本

若不想装 init script，也可以改用随工程提供的包装脚本手工注入代理：

```bash
./gradlew-with-proxy.bat build
```

#### 注意：init.d 目录里的文件都会被加载

Gradle 会执行 `<GRADLE_USER_HOME>/init.d/` 下**所有** `*.init.gradle`，
改名成 `xxx_disabled.init.gradle` 之类**并不会让它失效**（要彻底停用必须移出该目录）。
排查网络问题时若以为已禁用、实际仍在生效，会得到自相矛盾的结论。

#### 无法联网时

`--offline` 可以正常构建（依赖缓存已就绪），适合只改代码、不加依赖的快速迭代：

```bash
./gradlew --offline build
```

## 构建体系的关键决策

### 为什么用 ModDevGradle 的 `legacyforge` 插件

1.20.1 的 NeoForge 有两个硬约束：

| 约束 | 说明 |
| --- | --- |
| 坐标是 `net.neoforged:forge:1.20.1-47.1.x` | 不是 `net.neoforged:neoforge`（那个从 20.2 才有） |
| 运行期用 **SRG** 名，开发期用**官方（Mojang）名** | 1.20.1 及以下以 SRG 作中间名 |

- **ModDevGradle 普通插件**只支持 NeoForge 21.0+，装不上；
- **NeoGradle 7** 能解析这个坐标，但它对 1.20.1 不会把编译产物换成官方名
  （`getApplyCompiledMappingsTaskBuilder` 在 NeoGradle 7.1.39 里没有任何调用点），
  结果是编译期只能看到 SRG 名，官方名风格的代码一律报「找不到符号」；
- **ModDevGradle 的 `legacyforge` 插件**正是为 1.17~1.20.1 准备的：开发期用官方名，
  `jar` 任务自动 reobf 回 SRG。所以本工程用它。

注意它必须用 **`neoForgeVersion`** 而不是 `version`：

```groovy
legacyForge {
    enable {
        neoForgeVersion = "1.20.1-47.1.106"   // 走 net.neoforged:forge
        // version = "…"                      // 走 net.minecraftforge:forge（Forge 分支）
    }
}
```

`version` 会把 group 写成 `net.minecraftforge`，于是解析出来的是
「universal jar」，运行配置再去里面找 userdev 的 `config.json` 就会失败
（`Failed to read userdev config file from …-universal.jar`）。`neoForgeVersion`
才会按 `net.neoforged:forge` 解析并加载正确的 userdev。

### `mods.toml` 用的是 FML 47 的老格式

- 依赖声明用 **`mandatory=true/false`**，不是 1.21 的 `type="required"/"optional"`
  （写成 `type` 会得到 `Missing required field mandatory in dependency`）；
- 装载器的依赖 modId 仍是 **`forge`**（`neoforge` 从 20.2 才开始使用），版本范围按 47 起算。

### 资源里的模型加载器命名空间

1.20.1 的 OBJ 模型加载器注册名是 **`forge:obj`**（1.21 才是 `neoforge:obj`），
\`assets/scenerystaff/models/block/denia_cake_*.json\` 已相应改写。

### FML 与 Minecraft 自带库需要显式声明

`legacyforge` 只把「Minecraft + NeoForge 合并后的那一份 jar」放进编译类路径，
下面这些都得自己加 `compileOnly`（它们运行时由加载器 / Minecraft 提供）：

- `net.neoforged.fancymodloader:language-java`（`@Mod` / `@Mod.EventBusSubscriber` 注解在这里）
- `net.neoforged.fancymodloader:core` / `events` / `loader`（事件总线）
- `org.joml:joml`、`com.mojang:datafixerupper`、`com.mojang:brigadier`、`io.netty:netty-buffer`
- `org.spongepowered:mixin`、`org.slf4j:slf4j-api`、`com.google.code.findbugs:jsr305`

### 刀状态能力要经过 `BladeStates` 收口

1.20.1 的 SlashBlade 还没有 `BladeStateAccess.of(stack)`，只有
`stack.getCapability(ItemSlashBlade.BLADESTATE)`（`LazyOptional`）。
新增的 `blade/BladeStates` 把它包成 `Optional`，其余代码照 1.21.1 的写法用。
另外 SlashBlade 是以 SRG 名发布的，依赖必须走 `modImplementation` 让 legacyforge 重映射。

## 相对 1.21.1 的移植要点

| 主题 | 1.21.1 写法 | 本工程（1.20.1）写法 |
| --- | --- | --- |
| 构建插件 | `net.neoforged.moddev` | `net.neoforged.moddev.legacyforge` |
| 依赖坐标 | `net.neoforged:neoforge:21.1.x` | `net.neoforged:forge:1.20.1-47.1.x` |
| 元数据文件 | `META-INF/neoforge.mods.toml` | `META-INF/mods.toml`（依赖 modId = `forge`） |
| **mixin 注册方式** | `neoforge.mods.toml` 里的 `[[mixins]] config=...` | **jar MANIFEST 的 `MixinConfigs` 属性**，见下节 |
| 注册项类型 | `DeferredHolder` | `RegistryObject` |
| 事件注解 | `@EventBusSubscriber` | `@Mod.EventBusSubscriber` |
| 玩家 tick | `PlayerTickEvent.Post` | `TickEvent.PlayerTickEvent`(END) |
| 可改伤害事件 | `LivingIncomingDamageEvent` | `LivingHurtEvent` |
| 玩家登录 / 攻击事件 | `getEntity()` 返回 `Entity` | 已直接返回 `Player`（不能再模式匹配） |
| 物品自定义数据 | `DataComponentType` | `ItemStack#getTag / getOrCreateTag` |
| 玩家持久数据 | `AttachmentType`（自动同步） | Forge 能力 `PlayerChargeCapability` + `SyncChargeMessage` 同步包 |
| HUD | `RegisterGuiLayersEvent` + GUI Layer | `RegisterGuiOverlaysEvent` + `IGuiOverlay` |
| 数据生成 | `BootstrapContext` / `RecipeOutput` | `BootstapContext` / `Consumer<FinishedRecipe>` |
| 配方生成器构造器 | `RecipeProvider(PackOutput, lookup)` | `RecipeProvider(PackOutput)` |
| 数据包目录 | `advancements/ recipes/ loot_tables/` | 单数：`advancement/ recipe/ loot_table/` |
| 方块 codec | `Block#simpleCodec` + `codec()` | 1.20.1 没有，直接不写 |
| 方块交互 | `useItemOn` / `useWithoutItem` | 统一 `use` |
| tooltip | `appendHoverText(..., TooltipContext, ...)` | `appendHoverText(..., Level, ...)` |
| 刀名 / 描述键 | `getDescriptionId(stack)` → 可直接用 `Item` 的 | 必须用 **SlashBlade 的重载** `ItemSlashBlade#getDescriptionId(ItemStack)`；`Item` 那个不带参数的返回物品注册名 |
| 刀状态能力 | `BladeStateAccess.of(stack)` | `stack.getCapability(ItemSlashBlade.BLADESTATE)`，由 `blade/BladeStates` 收口 |
| 附魔定义 | `EnchantmentDefinition(Holder, lvl)` | `(Enchantment 的 ResourceLocation, lvl)` |
| 属性修饰符 | `AttributeModifier(ResourceLocation, …)` | `AttributeModifier(UUID, name, …)`，且事件**按槽位各触发一次**，必须自己过滤槽位 |
| 药水效果 | `Holder<MobEffect>` | `MobEffect` |
| 实体同步数据 | `defineSynchedData(Builder)` | `defineSynchedData()` |
| 顶点写入 | `addVertex/setColor/setUv/…` | `vertex/color/uv/overlayCoords/uv2/normal/endVertex` |
| 声音数据生成 | `add(DeferredHolder, …)` | `add(Supplier, …)` |
| 铁魔法法术容器 | 单一代 API | **两代 API 并存**，见下节 |

## 铁魔法联动：两条线、两代互不兼容的容器 API

1.20.1 上的铁魔法分成两条发布线，法术容器 API **互不兼容**：

| 加载器 | 可用版本 | 容器 API |
| --- | --- | --- |
| **NeoForge** | 最高到 **3.4.0.9**（3.16.3 不支持 NeoForge，只支持 Forge） | 旧一代 |
| **Forge** | 后续版本继续维护，如 3.16.3 | 新一代 |

两代 API 的具体差别：

| | 旧一代（3.4.0.9） | 新一代（3.16.3） |
| --- | --- | --- |
| 可变容器类型 | 没有 `ISpellContainerMutable` | 有 |
| 增删法术 | `ISpellContainer` 上的实例方法，要额外传 `ItemStack`，并靠 `save(stack)` 落盘 | `ISpellContainerMutable` 上的方法 |
| 写入物品 | 实例 `save(stack)` | 静态 `ISpellContainer.set(stack, c)` |
| 槽位类型 | `SpellData` | `SpellSlot`（访问器同名但是另一个类） |

**一个编译产物没法同时静态引用两代符号** —— 引用 `ISpellContainerMutable` 就编译不过旧版本，
引用旧版实例方法就编译不过新版本。所以这里的做法是：

- 静态只依赖两代都有的符号（`isSpellContainer` / `get` / `mustEquip` / `getMaxSpellCount` /
  `AbstractSpell#getSpellResource`）；
- 版本敏感的部分（槽位读取、增删法术、写入物品）收进 `SpellContainers` 的**反射**实现，
  加载时探测一次 `ISpellContainerMutable` 是否存在，选中 `ModernOps` 或 `LegacyOps`；
- 反射只发生在「每秒一次」的背包巡检与真正写入法术时，不在热路径。

编译基线取 NeoForge 这条线的 **3.4.0.9**（Modrinth `9dM8iFPD`），两者都验证过可编译，
切换版本时改 `build.gradle` 里那个版本号即可。

### 属性修饰符必须按槽位过滤
1.20.1 的 `ItemAttributeModifierEvent` 是**「按槽位各触发一次」**的：Forge 通过
`IForgeItem#getAttributeModifiers(EquipmentSlot, ItemStack)` 逐个槽位询问，每次询问都发一次事件。
事件自带 `getSlotType()`，而 `addModifier` 只收 `(属性, 修饰符)`、**没有槽位参数**。

1.21 用的是 `EquipmentSlotGroup.MAINHAND`，天然只落在主手；移植时如果照抄成
「无脑 addModifier」，法杖属性会被同时挂到主手、副手、头、胸、腿、脚六个槽上，
Tooltip 里就会多出「在副手时 / 戴在头上时 / 穿在身上时 / 穿在腿上时 / 穿在脚上时」。

所以这里显式判断 `getSlotType()` 只放行 `MAINHAND`，与 1.21.1 的显示保持一致。

### 刀的翻译键必须走 SlashBlade 的重载

命名刀的显示名与描述键都来自**刀状态里的 `translationKey`**，而这个名字在 1.20.1 上只能通过
SlashBlade 自己的重载拿到：

```java
ItemSlashBlade blade = (ItemSlashBlade) stack.getItem();
String key = blade.getDescriptionId(stack);   // item.scenerystaff.scenery_staff
```

**不能用** `stack.getItem().getDescriptionId()`（不带 ItemStack 的 `Item` 版）——
它返回的是物品注册名，所有拔刀剑都是同一个 `item.slashblade.slashblade`，
于是 `item.scenerystaff.` 前缀判断对四把命名刀全部不成立，描述整段静默失效
（潮汐本来就没写描述，所以看不出差别，容易误判成「只有潮汐是好的」）。

### mixin 必须靠 jar MANIFEST 注册（1.20.1 的关键差异）

1.20.1 的 FML **只从 jar 的 `MixinConfigs` MANIFEST 属性**收集 mixin 配置：

```groovy
tasks.named('jar', Jar).configure {
    manifest {
        attributes('MixinConfigs': "${mod_id}.mixins.json")   // ← 关键
    }
}
```

上游 1.21.1 是在 `mods.toml` 里写 `[[mixins]] config=...` 的，**这套在 1.20.1 上完全无效**
（Forge 官方 mods.toml 规范里没有这一项，它属于 NeoForge 的新格式）。
失效时的表现极其隐蔽：

- mixin 配置**不会注册**（`Registering mixin config: scenerystaff.mixins.json` 不出现）；
- 注入**不生效**，游戏照常启动；
- 而且**不报任何错**——没有 `MixinApplyError`、没有崩溃、没有警告。

本工程的 `LayerMainBladeMixin`（拦截 SlashBlade 把快捷栏第 0 格刀当待机刀渲染）就因此长期失效，
表现为「主手拿别的刀时，快捷栏第 1 格的刀被额外渲染成待机刀 / 看起来装备到了副手」。

排查这类问题的可靠手段：看日志里有没有
`[mixin/]: Registering mixin config: <你的>.mixins.json` 与
`[mixin/]: Mixing <你的Mixin> from ... into <目标类>` 两行。
另外注意：**不能把重混淆后的生产 jar 直接丢进 dev 环境测试** ——
dev 用的是官方名，不认 SRG 名，会以 `NoSuchFieldError: f_xxxxx_` 崩溃（那是测试方式的问题，不是 mod 的问题）。

## 目录

```
src/main/java/com/xingling/scenerystaff/
├── SceneryStaff.java              主入口（注册 + 可选联动守卫）
├── blade/                         刀识别、强化数值、刀状态能力访问
├── block/ item/ registry/         方块、物品与各注册表
├── sa/ se/                        剑技（含 SA 充能体系）、特殊效果
├── entity/                        侵蚀领域、巨剑实体
├── event/                         暴击、铁砧强化、解封兜底、终盘增益
├── client/                        渲染器、HUD、tooltip
├── compat/                        Jade / 铁魔法兼容层
├── datagen/                       数据生成
└── mixin/                         待机刀渲染拦截
```
