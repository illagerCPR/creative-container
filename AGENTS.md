# AGENTS.md — Creative Container

Minecraft **1.21.1 / NeoForge 21.1.248** 模组（modid `creativecontainer`）：一个**专供整合包**的终局方块——把「创造模式物品栏里的一切」变成一个可被 ME 存储总线读取的无限来源。

**没有任何硬依赖**：AE2 / ProjectE / Project Expansion 全是**可选联动**，未安装时模组照常加载（相关类只在对方存在时才被加载）。

## 当前状态（2026-09-28）

**已发布 `v0.1.1`**（2026-09-28，GitHub Release 附产物 jar）。M0–M6 全部完成：GameTest **34 项全绿**——dev 运行真实加载 **AE2 19.2.17**（联动 6 项，含真实 ME 网络端到端 2 项）与**真实 ProjectE 1.21.1-PE1.1.0**（cursemaven 引入，实机 EMC 1 项）；`runClient` 人工验收通过（v0.1.0 三轮 + v0.1.1 用户实测）。v0.1.1 内容：**无默认配方**（整合包自行配置）、**每池一个「管道出口槽」**（GUI 悬停池槽按 R 指定/取消、金色选定框、`IItemHandler` 仅出口槽出货）、**拼音搜索**（vendor PinIn 1.6.0）、**AE2 风浅色 UI**（池上/背包下、右页居中）、**输入框聚焦吞功能键**。后续功能见 `docs/02` 的「已知待办」。

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

### 核心池实现（勿凭旧印象改）

- **池容量是配置项**：`CCConfig.POOL_SLOTS`（默认 108，范围 9–4320）。`CreativeItemPool` 的 filled 槽始终是连续前缀 `[0, filledSlots)`；读档时有效容量 = `max(配置, 已存条目数)`（配置调小不影响已存档容器）。
- **查找走哈希索引**：`item -> 槽位下标列表`（`CreativeItemPool.byItem`）只用于缩小候选，最终相等判断仍是 `isSameItemSameComponents`——组件变体（两把不同附魔剑）因此天然共存；不要把最终判断改成 `equals`/HashMap 语义。
- **每池一个「管道出口槽」**（v0.1.1）：`CreativeItemPool.designatedSlot`（NBT `DesignatedSlot`，默认 -1=全拒）。`CreativePoolItemHandler.extractItem(slot,...)` **仅当 slot==designatedSlot 时放行**（无限供给、池不减少），其余槽一律拒——管道（漏斗/Pipez/Create）零配置遍历即可，但只拿得到出口物品；`extractItemMatching` 不受出口限制。出口随压缩重排跟随物品（`repointDesignatedSlot`），物品被移除即回到 -1。指定/取消 = `BE.toggleDesignatedSlot`（空槽无操作），GUI 按 R（`KeyMapping`，默认 `R`）发 `SelectPoolSlotPayload`。
- **客户端镜像走增量 payload**：`PoolDeltaPayload`（server→client，`sendToPlayersTrackingChunk`，GameTest 无跟踪玩家即 no-op；**协议版本 "2"**，record 含 `designatedSlot`）。加物品=单槽 delta、改 N/改出口=仅数量形 delta、移除（触发压缩）=clearFirst 全量重排。chunk 加载仍走原版 getUpdateTag 全量快照（含 `TAG_DESIGNATED`）。**不要**改回 `sendBlockUpdated` 整包推送（4320 槽时一次改动≈MB 级包）。
- **AE2 ME 侧不受出口槽限制**：`CreativeContainerMeStorage.extract` 仍按「带物品键即可抽任意池内物品」（用户确认的口径，总线 UI 自带物品指定）。
- **无默认配方**（v0.1.1 起）：`data/<ns>/recipe/` 不随模组分发，GameTest 断言 classpath 上不存在；获取途径 = 创造标签页 / `/give`。

### PinIn（拼音搜索，v0.1.1 vendor）

- **vendor 在 `pinyin/` 包**（`me.towdium.pinin` → 仅改包名，16 个源文件 + `data.txt` 字典约 295KB，MIT，源 = JitPack `com.github.Towdium:PinIn:1.6.0`；Maven Central **无**此制品）。`data.txt` 用 `PinIn.class.getResourceAsStream("data.txt")` 相对类路径加载 → **改包名必须连带移动 resources 路径**，否则 NPE。
- PinIn 对非汉字字符**大小写敏感**（`contains("Diamond Sword","diamond")==false`）→ `CreativeItemIndex` 的 searchKey 与查询统一 lowercase 后进索引。预建索引 `TreeSearcher<Entry>(Logic.CONTAIN, pinIn)` 惰性构建、rebuild 后失效重建；查询结果需自行按 id 排序（树遍历序不稳定）。
- JEC（Just Enough Characters）**只 hook JEI 搜索框、无通用 API**——自己 GUI 的拼音搜索只能 vendor PinIn，两者共存互不干扰。

### 1.21.1 / NeoForge 21.1.248 API

- `Slot.x` / `Slot.y` 是 **`public final`**：槽位坐标只能在构造时给 → 布局常量集中在 `menu.CreativeContainerLayout`，Menu 决定槽位坐标、Screen 只画背景。
- **`BaseEntityBlock.getRenderShape` 默认 `RenderShape.INVISIBLE`**（它假设 BE 用渲染器画自己）——普通方块模型的 `BaseEntityBlock` 子类**必须**覆写 `getRenderShape(BlockState)` 返回 `MODEL`，否则放置后整个方块不可见（只剩选中框，物品形态却正常渲染）。该 1 参签名上游标了 `@Deprecated`，但覆写不产生 lint 警告、也无替代。有 GameTest 守卫（`placedBlockRendersItsModel`）。
- `Block#getCloneItemStack(LevelReader, ...)` 已过时且默认行为即所需 → 不要重写。
- **`GuiGraphics.blit` 的短重载按 256×256 纹理采样**：非 256 尺寸贴图必须用 `(x, y, float u, float v, w, h, texW, texH)` 重载并传真实尺寸，否则 396×222 的背景被水平平铺 1.55 倍、18×18 的槽位贴图被涂抹成单像素色块（t2 截图实锤）。
- **`AbstractContainerScreen.mouseClicked` 所有路径都返回 true**（含「点在界面外」的收尾 return）：自定义点击区域必须**先于** `super.mouseClicked` 处理，否则永远收不到点击（表象：界面上的自定义按钮/网格点了没反应，GameTest 直测菜单逻辑却全绿）。
- **`AbstractContainerScreen.keyPressed` 的功能键检查（E 关界面 / Q 丢弃 / 1-9 换位）发生在 `super` 链之后**，而 `EditBox.keyPressed` 对普通字母返回 false → 自定义 Screen 带搜索框时**输入框聚焦按 E 会关界面**（v0.1.1a 实锤）。修法：聚焦时先手动调 focused EditBox 的 `keyPressed`，再除 ESC 一律 `return true` 吞掉；`charTyped` 独立成事件，吞 `keyPressed` 不影响字符输入。`KeyMapping` 拦截必须避开输入框聚焦态；`keyPressed` 拿不到光标 → 用 `mouseMoved` 记录悬停坐标。
- `ItemStack.getTooltipLines` 是**三参** `(Item.TooltipContext, Player, TooltipFlag)`（`TooltipContext.of(Level)` 构造）；`TooltipFlag.NORMAL` 是接口静态字段；`GuiGraphics.renderTooltip(Font, List<Component>, Optional<TooltipComponent>, x, y)` 与 `renderOutline(x,y,w,h,color)` 可用于自绘 tooltip 行与选定框。
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
