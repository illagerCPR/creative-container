# Creative Container

> 一个**专供整合包**的 Minecraft 终局方块：把「创造模式物品栏里的一切」变成一个可被自动化设备抽取的无限来源。

**Minecraft 1.21.1 · NeoForge 21.1.248 · 无硬依赖（AE2 / ProjectE / Project Expansion 均为可选联动）**

## 它是什么

「创造模式容器」是一个带 GUI 的方块。你在 GUI 里像创造模式物品栏那样搜索、翻页、取出任意物品，也可以把物品**放进容器的池子**；放进池子的物品从此**不限量**——外部设备（ME 存储总线、管道、漏斗）可以从容器里**按需抽取**，永远抽不完。

它不产出资源，也不主动注入网络：**抽取永远由外部设备发起**，因此不会变成「开机即无限 EMC」的失控机器。

## 玩法与规则

| 行为 | 说明 |
| --- | --- |
| 打开 GUI | 空手右键方块（创造式界面：左侧搜索 + 分页物品网格；右侧玩家背包 + 容器池） |
| 从创造池取物 | 左键取 1 个，Shift + 左键取一整组 |
| 把物品放进容器池 | 右键创造网格里的物品（右键 = 加入容器池；槽位数量由配置决定，默认 108，上限 4320） |
| 从容器池取物 | 左键池中物品取 1 个，Shift + 左键取一组 |
| 移出容器池 | 右键池中物品 |
| 报告数量 N | GUI 右侧「报告数量」输入框，范围 **1 – 2147483647**，回车或关界面生效 |

### 与 ME 存储总线（AE2）

把 ME 存储总线贴在方块任意一面即可：存储总线读取方块的 ME 库存，**池中每种物品都按 N 个报告**，并可被抽出到网络。

- 本模组**不需要**网格节点、频道或供电——它只是一个被动库存源（依据：AE2 的 `StorageBusPart` 通过 `AECapabilities.ME_STORAGE` 读取相邻方块，见 `docs/01`）。
- **未指定物品的抽取一律被拒绝**：AE2 侧 `extract` 必须带物品键；通用 `IItemHandler` 侧因为 `extractItem(slot, amount, ...)` 无法指定物品，直接返回空。
- 插入会被拒绝（返回「一个也没收下」），避免无限源吞掉物品。

### 与 ProjectE / Project Expansion

| 环境 | EMC 值 |
| --- | --- |
| 仅 ProjectE | **999,999,999,999,999,999** |
| ProjectE + Project Expansion | **9,223,372,036,854,775,807**（`Long.MAX_VALUE`） |

ProjectE 的物品 EMC 是 64 位 `long`：Project Expansion 的 BigInteger 只覆盖 EMC *存储*（EMC 链接/继电器/收集器）与玩家知识库，物品 EMC 参数仍是 `long`。因此同时安装 Project Expansion 时，本模组写入表中的 `Long.MAX_VALUE`。技术证据见 `docs/01`。

EMC 值由本模组自动写入 ProjectE 的自定义 EMC（`config/ProjectE/custom_emc.json`，与 `/projecte setemc` 同一条路径），因此**首次启动即生效**，且 `/reload`、重启后依然保留。

## 获取方式

默认配方（可被整合包用数据包覆盖或删除）：

```
N D N       N = 下界之星
D E D       D = 下界合金块
N D N       E = 龙蛋
```

## 配置

`config/creativecontainer-common.toml`：

| 键 | 默认 | 说明 |
| --- | --- | --- |
| `general.defaultReportedAmount` | `2147483647` | 新放置方块的默认报告数量（每个方块可在 GUI 中单独设置） |
| `general.acceptInsertions` | `false` | 是否接受外部插入（默认拒绝：无限源不应吞物品） |
| `general.poolSlots` | `108`（9–4320） | 每个容器池的槽位数量；已存档容器保持其更大的既有容量，降低配置不影响它们 |

## 构建

```bash
export JAVA_HOME=/path/to/jdk-21
./gradlew build              # 产物在 build/libs/
./gradlew runGameTestServer  # 无头机制测试（含 AE2 联动测试）
./gradlew runClient          # 手动验收 GUI
```

## 许可

LGPL-3.0（见 `LICENSE`）。
