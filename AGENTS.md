# AGENTS.md — Creative Container

Minecraft **1.21.1 / NeoForge 21.1.248** 模组（modid `creativecontainer`）：一个**专供整合包**的终局方块——把「创造模式物品栏里的一切」变成一个可被 ME 存储总线读取的无限来源。

**没有任何硬依赖**：AE2 / ProjectE / Project Expansion 全是**可选联动**，未安装时模组照常加载（相关类只在对方存在时才被加载）。

## 当前状态（2026-09-27）

M0（骨架）/ M1（核心池机制）/ M2（GUI）/ M3（AE2 + ProjectE 联动）/ M4（资源与数据）已完成实现；M5 自动化验证完成——GameTest **26 项全绿**：dev 运行真实加载 **AE2 19.2.17**（联动 6 项，含真实 ME 网络端到端 2 项）与**真实 ProjectE 1.21.1-PE1.1.0**（cursemaven 引入，实机 EMC 1 项）。剩余见 `docs/02` 的「已知待办」：`runClient` 人工验收、首个 tag 与 Release。

改设计先改 `docs/` 再动代码；需求权威来源是 `docs/00`。

## 环境陷阱（必读）

- **WSL2 MTU 黑洞**：镜像网络模式下 eth MTU 1500 时超过 ~1400 字节的包被静默丢弃（大文件下载随机永久卡死）。本机已持久化修复（`/usr/local/sbin/wsl-fix-mtu.sh` 随 WSL 启动执行，所有 `eth*` MTU 1400）。Gradle 拉依赖卡住先查这个，不是反代。
- **反代与 maven 无关**：`S302_rules.ini` 只覆盖 github 等域，不含任何 maven 域；Gradle 依赖卡死不是它。反代的真实危害是 MITM github 域名用自签证书 → **Java 报 `PKIX path building failed` 而 curl 正常**。需要 Java/Gradle 访问 github 的步骤前提醒用户关闭反代，完成后提醒重新打开。
- **系统无 `java` 命令**：JDK 21 在 `~/.gradle/jdks/jdk-21.0.12.1+1`，构建必须 `export JAVA_HOME=/home/illager_aris/.gradle/jdks/jdk-21.0.12.1+1`。Gradle 8.8 wrapper 发行版已缓存。
- **WSLg 下 `runClient` 视角疯转**：解法是 `devmods/rdpmouse-neoforge-1.21.1-1.0.0.jar`（已在 `.gitignore`，经 `runtimeOnly files(...)` 接入，不进产物），游戏内 **F8** 切换 RDP 模式，光标触边按 **Alt** 回中。
- **`cmd | tail` 的退出码是 tail 的**：长构建把真实退出码写进日志再判断（`... > log 2>&1; echo "EXIT=$?" >> log`）。
- 需要核对任意 MC/NeoForge 类签名时免下载：`~/.gradle/jdks/jdk-21.0.12.1+1/bin/javap -cp ~/.gradle/caches/neoformruntime/intermediate_results/compiledWithNeoForge_*_output.jar <全限定类名>`；AE2 的签名可对 `<任意目录>/ae2-19.2.17.jar` 直接 javap。

## 硬事实（已实读核对，勿凭记忆改）

### AE2（证据见 `docs/01`）

- **ME 存储总线读取的是相邻方块的 `AECapabilities.ME_STORAGE`**：`StorageBusPart` 的邻接查询就是 `new PartAdjacentApi<>(this, AECapabilities.ME_STORAGE)`（javap -c 实证）。因此本模组**只注册该方块能力**，不需要网格节点/频道/供电。若日后有人想「改成节点方案」，先读 `docs/01`。
- 只 import `appeng.api.**`；AE2 制品在 **Maven Central**（`org.appliedenergistics:appliedenergistics2:19.2.17`，唯一传递依赖 guideme）。本模组用 **`compileOnly` + `runtimeOnly`**（dev 跑真实 AE2），发布产物**不含** AE2 依赖声明。
- **未装 AE2 时不能让 JVM 碰到 `appeng.**`**：所有 AE2 类型集中在 `registry.Ae2CapabilityRegistration`、`interop.ae2.*`、`gametest.Ae2TestSupport`、`gametest.Ae2NetworkTestSupport`，只在 `InteropHooks.ae2Available()` 为真时被加载（GameTest 类本身不含 AE2 类型，缺 AE2 时日志跳过）。
- **真实网络端到端的搭法**（`Ae2NetworkTestSupport`，AE2 自家 testplots 同款）：`appeng.api.parts.PartHelper.setPart(level, pos, side, player, item)` 可编程放置部件并自动创建线缆总线宿主；线缆也是部件（`AEParts.GLASS_CABLE.item(AEColor.TRANSPARENT)` 放在任意面即成为中心线缆）；能源用 `AEBlocks.CREATIVE_ENERGY_CELL`（无需控制器，ad-hoc 网络通道预算 8）；导出总线 `getConfig().addFilter(ItemLike)` 设置过滤。

### ProjectE / Project Expansion

- 物品 EMC 是 **`long`**（`EMCMappingHandler` 的 `Object2LongMap<ItemInfo>`，`@Range(0, Long.MAX_VALUE)`；自定义 EMC 文件用 `nonNegativeLong()`）。Project Expansion 的 **`SetEmcCMDMixin` 只改提示文案**，`SetEmcCMD` 参数仍是 `LongArgumentType.longArg(0, Long.MAX_VALUE)`；其 BigInteger 只用于 EMC 存储（`IEmcStorageBigInteger`）与玩家知识库。**结论：1.0×10²² 无法落地**，装 Project Expansion 时写 `Long.MAX_VALUE`。
- 写入路径与 `/projecte setemc` 同源：`CustomEMCParser.init(registries)` → `addToFile(NSSItem, long)` → `flush(registries)`（`init` 内部会先 `flush` 再读文件，所以必须按这个顺序调）。
- **EMC 计算时机（2026-09-27 实机验证纠错）**：EMC 在两条路径重算——① `PECore#addReloadListeners` 注册的**数据包加载阶段**监听器（专用服务器在线程启动前加载资源，这次计算**早于一切服务器事件**，`ServerAboutToStartEvent` 写入必然赶不上它）；② `PECore#dataPackSync` ← `OnDatapackSyncEvent`（`/reload` 及**每个玩家加入**）。所以生产环境首启后玩家一加入即生效；`ServerStartedEvent` 回读不一致时，本模组自行 post `OnDatapackSyncEvent(playerList, null)`（与 `/reload` 群发路径相同）触发重算实现自愈，无玩家场景也立即生效。
- **dev 引入真实 ProjectE**：cursemaven（`maven { url 'https://cursemaven.com' }` + `runtimeOnly 'curse.maven:projecte-226410:6611984'`，1.21.1-PE1.1.0，**MIT**，依赖仅 minecraft+neoforge）。仅 dev 运行时，不编译、不打包。本机 **forgecdn 直连（mediafilez.forgecdn.net）不可达**（连接超时），必须走 cursemaven。ProjectE 的 UUID Checker/VersionChecker 遥测线程在本机会因反代 MITM 报 SSL 错误——自身已捕获，无害，grep 日志时排除。
- ProjectE 是 CurseForge 独占（无 Maven 制品、GitHub 无 release jar）→ 本模组对它的所有调用走**反射**，不引入编译期依赖。projecte modid = `projecte`，Project Expansion = `projectexpansion`。

### 1.21.1 / NeoForge 21.1.248 API

- `Slot.x` / `Slot.y` 是 **`public final`**：槽位坐标只能在构造时给 → 布局常量集中在 `menu.CreativeContainerLayout`，Menu 决定槽位坐标、Screen 只画背景。
- `Block#getRenderShape`、`Block#getCloneItemStack(LevelReader, ...)` 已过时且默认行为即所需 → **不要重写**（项目开了 `-Xlint:deprecation`，警告即噪声）。
- 方块提示签名：`appendHoverText(ItemStack, Item.TooltipContext, List<Component>, TooltipFlag)`（`TooltipContext` 是 `Item` 的内部类）。
- 自定义容器 GUI：`IMenuTypeExtension.create(IContainerFactory)`；`openMenu(provider, pos)` 自动写 BlockPos；客户端 Screen 用 mod bus `RegisterMenuScreensEvent`；`AbstractContainerMenu` 没有 `getTitle()`。
- `@EventBusSubscriber` 的 `bus` 属性已废弃 → 客户端监听写在主类构造器的 `FMLEnvironment.dist == Dist.CLIENT` 守卫里（`CCClient.register(modEventBus)`），dedicated server 不加载客户端类。
- 创造标签页条目必须**非 AIR 且 count == 1**（NeoForge 包装 `CreativeModeTab.Output` 会抛异常）→ 有 GameTest 重放该路径。
- 服务端 `ResourceManager` 读不到 `assets/`（只扫 `data/`）→ 资源守卫走 classpath 直读 `Class.getResourceAsStream("/assets/...")`。
- 数据包路径：`data/<ns>/recipe/`（单数）、`loot_table/blocks/`（单数）、`structure/*.nbt`（**必须二进制 gzip NBT**，`.snbt` 只在 IDE 的 `gameteststructures/` 生效且丢命名空间；生成脚本 `tools/make_empty_structure.py`）。

### GameTest 纪律

- `helper.setBlock/getBlockEntity` 收**结构局部坐标**；`helper.getLevel()` 上的世界坐标查询要用 `helper.absolutePos(local)`（混用表象是「刚 setBlock 却读到空气」，且不报错）。
- `makeMockServerPlayerInLevel()`（已过时）生成在**世界出生点**：任何距离/`stillValid` 检查前必须 `teleportTo(level, absX+0.5, absY+1, absZ+0.5, 0, 0)`。
- GameTest mock 玩家是 vanilla 连接：**不要**给 mock 玩家发 mod payload（会抛 `UnsupportedOperationException`）。
- **装了 ProjectE 后 `makeMockServerPlayerInLevel()` 不可用**：ProjectE 在 `OnDatapackSyncEvent` 向加入的玩家推 `sync_world_transmutations`，而该事件在 `placeNewPlayer` 字节码偏移 470 触发、早于玩家注册（689/731/1006）——异常中止登录，玩家不存在，catch 后恢复也无效。正解（`CreativeContainerTestSupport.makeMockServerPlayer`）：手工构造——`new ServerPlayer(server, level, profile, ClientInformation.createDefault())` + 裸 `Connection(SERVERBOUND)` 塞进 `EmbeddedChannel` + `new ServerGamePacketListenerImpl(server, connection, player, CommonListenerCookie.createInitial(profile, false))`，全程不走登录。**不需要连接的场合（如 `PartHelper.setPart` 的 player 参数）用 `FakePlayerFactory.getMinecraft(level)` 更干净**（AE2 testplots 同款）。
- 全绿 ≠ 无异常：跑完必须 grep 日志的 `ERROR`/`exception`。
- 新增测试需要模板：`python3 tools/make_empty_structure.py` 生成 `smoke`(3³) 与 `interop`(7×5×7)。

## 工作约定

- 对话与文档用简体中文；**源代码标识符一律英文，禁止拼音**。
- 文档除 `AGENTS.md` / `README.md` 外一律放 `docs/`。
- 每完成一个批次立即 `git push origin main`，`gh run list --repo illagerCPR/creative-container --commit <sha>` + `gh run watch <runId> --exit-status` 等绿；**每批次完成后停下等用户明确指令**，不自动连跑。
- 版本号唯一来源：`gradle.properties` 的 `mod_version`。
- commit/tag 用全局 GPG 签名；推送必须用 GitHub 隐私邮箱 `63698328+illagerCPR@users.noreply.github.com`。
- 验证分层：机制正确性进 GameTest，观感与手感靠 `runClient` 人工验收。
- 不使用子代理（用户偏好）：调查与核对在本会话内自己完成。
- 涉及计算机交互的任务完成后，询问用户是否把跨项目可复用经验存入 OpenViking 记忆。
