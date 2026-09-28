# Creative Container

> 一个**专供整合包**的 Minecraft 终局方块：把「创造模式物品栏里的一切」变成一个可被自动化设备抽取的无限来源。

**Minecraft 1.21.1 · NeoForge 21.1.248 · 无硬依赖（AE2 / ProjectE / Project Expansion 均为可选联动）**

当前版本 **v0.1.1**（2026-09-28）：无默认配方、管道输出指定（R 键）、拼音搜索、AE2 风浅色界面。

## 它是什么

「创造模式容器」是一个带 GUI 的方块。你在 GUI 里像创造模式物品栏那样搜索（支持拼音）、翻页、取出任意物品，也可以把物品**放进容器的池子**；放进池子的物品从此**不限量**——外部设备可以从容器里**按需抽取**，永远抽不完：AE2 ME 存储总线在总线 UI 里指定物品即可抽任意池内物品；通用物流管道（漏斗、Pipez、Create 等）抽取的则是 GUI 里用 R 键指定的那个「管道输出」物品。

它不产出资源，也不主动注入网络：**抽取永远由外部设备发起**，因此不会变成「开机即无限 EMC」的失控机器。

## 玩法与规则

| 行为 | 说明 |
| --- | --- |
| 打开 GUI | 空手右键方块（创造式界面：左侧搜索 + 分页物品网格；右侧容器池 + 玩家背包） |
| 从创造池取物 | 左键取 1 个，Shift + 左键取一整组 |
| 把物品放进容器池 | 右键创造网格里的物品（右键 = 加入容器池；槽位数量由配置决定，默认 108，上限 4320） |
| 从容器池取物 | 左键池中物品取 1 个，Shift + 左键取一组 |
| 移出容器池 | 右键池中物品 |
| 指定管道输出 | 悬停池槽按 **R**（可在控制设置改键）：被指定槽位显示金色选定框，通用物流管道只会从它抽取；再按一次取消 |
| 报告数量 N | GUI 右侧「报告数量」输入框，范围 **1 – 2147483647**，回车或关界面生效 |
| 搜索 | 支持注册 id 与显示名，并支持**拼音**（全拼与首字母，如 `zsj` 匹配钻石剑）；无需 Just Enough Characters，装有 JEC 时行为一致互不干扰 |

### 与 ME 存储总线（AE2）

把 ME 存储总线贴在方块任意一面即可：存储总线读取方块的 ME 库存，**池中每种物品都按 N 个报告**，并可被抽出到网络。

- 本模组**不需要**网格节点、频道或供电——它只是一个被动库存源（依据：AE2 的 `StorageBusPart` 通过 `AECapabilities.ME_STORAGE` 读取相邻方块，见 `docs/01`）。
- **未指定物品的抽取一律被拒绝**：AE2 侧 `extract` 必须带物品键（在总线 UI 里指定即可抽任意池内物品）。
- 插入会被拒绝（返回「一个也没收下」），避免无限源吞掉物品。

### 与通用物流管道（漏斗、Pipez、Create 等）

走 `IItemHandler` 的设备从容器抽取时：**只有被 R 键指定的「管道输出」槽位会出货**（该物品无限供给、池不减少），其余槽位一律拒绝——管道无需任何过滤配置，遍历槽位即可拿到指定物品。每个容器的池与指定相互独立。

### 与 ProjectE / Project Expansion

| 环境 | EMC 值 |
| --- | --- |
| 仅 ProjectE | **999,999,999,999,999,999** |
| ProjectE + Project Expansion | **9,223,372,036,854,775,807**（`Long.MAX_VALUE`） |

ProjectE 的物品 EMC 是 64 位 `long`：Project Expansion 的 BigInteger 只覆盖 EMC *存储*（EMC 链接/继电器/收集器）与玩家知识库，物品 EMC 参数仍是 `long`。因此同时安装 Project Expansion 时，本模组写入表中的 `Long.MAX_VALUE`。技术证据见 `docs/01`。

EMC 值由本模组自动写入 ProjectE 的自定义 EMC（`config/ProjectE/custom_emc.json`，与 `/projecte setemc` 同一条路径），因此**首次启动即生效**，且 `/reload`、重启后依然保留。

## 获取方式

**本模组不随包提供任何合成配方**（v0.1.1 起，由整合包作者自行配置）：

- 创造模式物品栏的「创造模式容器」标签页直接拿取；
- 或 `/give @s creativecontainer:creative_container`；
- 整合包作者：在数据包 `data/<命名空间>/recipe/` 里为 `creativecontainer:creative_container` 写配方即可，本模组不占用、不冲突任何配方命名空间。

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
./gradlew runGameTestServer  # 无头机制测试（34 项，含真实 AE2 ME 网络与真实 ProjectE EMC 联动）
./gradlew runClient          # 手动验收 GUI
```

## 许可与第三方库

本项目以 **LGPL-3.0** 发布（见 `LICENSE`）。

引用的第三方库：

| 库 | 版本 | 许可 | 用途 | 分发方式 |
| --- | --- | --- | --- | --- |
| [PinIn](https://github.com/Towdium/PinIn) | 1.6.0 | MIT（Copyright (c) 2019 Juntong Liu） | 拼音搜索（全拼/首字母匹配） | **vendored**：源码与字典随本仓库 `pinyin/` 包分发（仅改包名，代码零改动），MIT 许可全文见 [`pinyin/LICENSE-PinIn.txt`](pinyin/LICENSE-PinIn.txt)，并随发布 jar 内 `META-INF/licenses/PinIn-LICENSE.txt` 一并分发 |

按 MIT 要求，PinIn 的版权声明与许可文本随本模组的所有副本分发（仓库与发布 jar 均包含）；除此之外本模组**不打包、不声明任何其它第三方依赖**——NeoForge / Minecraft 为运行平台；AE2、ProjectE、Project Expansion 仅为运行时**可选联动**（未安装时本模组照常加载），不以任何形式随 jar 分发。
