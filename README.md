# MCWorldAgent - 远古 Minecraft 世界接管与编辑工具集 (Legacy Minecraft World Agent & Toolkit)

`MCWorldAgent` 是一个**面向 Minecraft 官方 1.0 之前历史版本（Classic、Indev、Infdev、Alpha、Beta）的世界探索、分析、编辑与运行时控制工具集**——那些世界格式已不被现代工具理解、引擎行为只能回到原始字节码里求证的时代。

名字里的 "Agent" 是双关：既指 **Java Agent**（进程内插桩，本项目的主接管通路），也指 **AI Agent**（对外暴露 MCP 等接口，让智能体能在"活的游戏"与"死的数据"之上工作）。

> **免责声明**：本项目是独立开发的第三方开源工具，并非 Minecraft 官方产品，未获 Microsoft、Mojang Studios 或任何第三方平台的赞助、认可或背书。
> *Independently developed third-party open-source toolkit. Not an official Minecraft product; not approved or endorsed by Microsoft or Mojang Studios.*

它的核心理念是**不重造引擎，接管原引擎**：世界语义不靠现代知识的合理猜测，而是钉死在原始 jar 的字节码与可读源码树上——

```text
数据侧                          运行时侧
原始版本 jar + 可读源码树        Java Agent（premain 织入）
├── 混淆实名逐版本 javap 钉死     ├── 严格指纹匹配，不符即 UNSUPPORTED 跳过
└── 世界格式（散件 .dat 等）      └── 把手①活体 World + tick 汇线 + 写纪律
            ↓                            ↓
   只做三件事：取证 · 织入 · 落盘验证——每个"支持"都有可复跑的证据链
```

> **戒律**：任何"现代 Minecraft 应该是这样"的念头一律不进代码——行为必须来自源码取证、字节码扫描或可复现实跑；宁可报告"不支持"，不做不确定的变换。构造期 `this` 不可作静态实参（old verifier）这类教训，一条一个坑，全部归档。

完整开发计划与架构见 [plan.md](./plan.md)，Phase 0 取证案卷见 [docs/research/phase0-agent-poc.md](./docs/research/phase0-agent-poc.md)，Phase 1 写侧案卷见 [docs/research/phase1-first-brick.md](./docs/research/phase1-first-brick.md)，命令通道案卷见 [docs/research/phase1-command-channel.md](./docs/research/phase1-command-channel.md)。

---

## 🌟 核心能力特性

### 已可用（Phase 0 观测 + Phase 1 写侧接管与命令通道，infdev 20100327 实跑钉死）

- **Java Agent 全链插桩**：`premain` + ASM 9.7 运行时织入，五钩指纹表（类名+方法名+描述符+access+期望命中数）严格两遍式匹配——先验指纹再注入，不符整类跳过并记 `UNSUPPORTED`，零静默错钩；注入全为对称压弹，不新增控制流。
- **把手①（活体世界捕获）**：`World` 构造器入口捕获存档目录 + `saveWorld` 入口捕获活体 `World` 对象引用（绕开 old verifier 对构造期 `this` 的禁令），后续世界读写、显式存盘全部经此把手在游戏线程执行。
- **tick 汇线与写纪律**：`Minecraft.runTick` 每 tick 回调，世界修改只排入队列、于 tick 边界 drain 执行——与游戏逻辑单线程串行，杜绝并发写世界的事故面。
- **Attic 借力（零改动）**：启动主入口 `attic-cli`（Attic 仓内开发控制面，launch 即会话宿主：游戏流直通 stdout/stderr、退出码随游戏）：`-javaagent` 经 `--jvm-arg` **尾注入**（落在 legacyfix javaagent 之后——观测层必须建在修复层最终产物之上，实例档案 `launchArgs` 的位置在 legacyfix 之前，非规范用法）；`--java` 钉死 JDK 旁路自动探测。备用入口是 Phase 0 的 harness（直接 `require` `@attic/runtime` dist 自行 spawn，保留用于顺序/环境类实验）。两条路 Attic 仓库均保持零改动。
- **观测闭环与证据链**：transform 日志（织入/类加载轨迹）+ hooks 日志（把手/tick/存盘/区块/光照事件，带毫秒时间戳）+ JVM 退出 SUMMARY 计数行；日志按 run 归档，`--dry-run` / `--no-agent` 基线对照 / `disable=<hookId>` 逐钩排除，全链可复跑。
- **三缺口实证结论**（详见案卷）：G1 发行运行时的自动存被 legacyfix `AutoSavePatch` 改为 5 分钟节律——接管者必须经把手显式存盘；G2 0327 光照数据信任存档（`lightRecalc == chunkMiss` 不变量），写盘带齐合法数据即零重算；G3 关键 UI 类在 agent 下加载无恙。
- **写入性接管"第一块砖"（Phase 1，零新字节码注入）**：经把手①在 tick 边界反射调用游戏自身 `setBlockWithNotify`（0327 实名 `c(IIII)Z`）在出生点放置圆石 → 显式 `saveWorld` 落盘 → 重进读回验证砖仍在（渲染通知链同步生效，用户目视实锤）；签名不符则记 `UNSUPPORTED` 且游戏零损伤——失败语义与织入层同纪律。写侧实名表与完整证据链见 Phase 1 案卷。
- **命令通道 v1（Phase 1，2026-11-13 实跑）**：文件队列协议（tmp→inbox→processed=ACCEPTED→outbox 终态；protocolVersion/幂等去重/严格 JSON 子集/限额防积压），独立 daemon 线程解析、tick 边界执行；`place`/`dig`/`getblock`/`save` 四 op 全绿——**dig 与 place 均跨进程/跨重进持久化实锤**，四种 REJECTED 失败路径现场命中，并修复"同进程重进读旧 World"句柄缺陷（WORLD_SWITCHED）。设计案卷 [agent-command-channel.md](./docs/architecture/agent-command-channel.md)，实跑案卷 [phase1-command-channel.md](./docs/research/phase1-command-channel.md)。

### 规划中（见 plan.md 路线图）

- **写侧扩展（Phase 1 后续）**：~~外部提交通道~~ ✅ 命令通道 v1 已实跑（见上）；剩余：钩层下探（`getChunkFromChunkCoords`）、元数据与实体写入、settime/setspawn/tp（待 javap 钉混淆名）、批量写、桌面壳命令台面板。
- **版本适配器矩阵**：逐版本（c 系列 / indev / alpha / beta）钉死混淆实名与存储格式，落版本兼容登记表——只有验证过的操作才标记支持。
- **Standalone 模式**：离线读写世界文件与无头生成，与 Agent 模式共享世界数据模型。
- **桌面壳与外部接口**：Vue 3 + Tauri 工作台、Three.js 视口、MCP Server 供 AI Agent 接入。

### 血统

启动通路借力 [Attic Corner Launcher](https://github.com/CreatorCSIE/Attic-Corner-Launcher)（同一维护者的历史版本启动器）——早期经其 `@attic/runtime` 库，现收敛到其 `attic-cli` 无头控制面；0327 可读源码树与字节码取证工作台见姊妹仓库 `minecraft-verification-analysis`。

---

## 🧭 开发状态

| 阶段 | 内容 | 状态 |
|---|---|---|
| Phase 0 | Java Agent 全链 PoC（infdev 20100327 × Attic harness） | ✅ 完成（2026-11-12）：建世界→游玩→重进同世界→正常退出全链跑通，五钩 HOOKED，三缺口自答落档 |
| Phase 1 | 写入性接管（setBlock + 显式存盘 + 重进验证）与钩层下探 | 🔶 第一块砖 ✅ + 命令通道 v1 ✅（2026-11-13）：place/dig/getblock/save 实跑、dig+place 持久化实锤、失败路径全命中；钩层下探与 op 扩展（settime/setspawn/tp/批量写）待做 |
| Phase 2+ | 版本适配器矩阵 / Standalone 模式 / 桌面壳 / MCP | 📋 见 plan.md |

---

## 💻 运行环境要求

### 1. Java（游戏侧与 agent 侧）

- **JDK/JRE 8**（远古版本的运行时与 agent 编译目标都是 Java 8；class major=49 走 old verifier，织入纪律按此约束设计）。
- agent 构建直接用 `javac`+`jar`，**无 Maven/Gradle**；ASM 9.7 由构建脚本自动下载并 SHA1 校验。

### 2. Node.js（harness 侧）

- **Node.js ≥ 20**，且本机已安装并运行过 [Attic Corner Launcher](https://github.com/CreatorCSIE/Attic-Corner-Launcher)（需要其 `attic-cli` 无头入口可用，及 `%APPDATA%/.attic` 数据根中的版本 jar）。

### 3. 操作系统

- 当前 PoC 在 **Windows** 实跑验证；路径解析与 spawn 逻辑平台相关，Linux/macOS 适配未做。

---

## 🚀 如何构建与运行

### 1. 构建 agent + 实跑 PoC

```powershell
# 构建 java agent（下载校验 ASM → javac → 打 jar，含 premain 冒烟）
powershell -File java/agent/build.ps1

# 默认通路：attic-cli（在 Attic 仓 apps/tauri/sidecar 下；先 --dry-run 核对 LaunchConfig）
node dist/cli.cjs launch mca-poc --dry-run
node dist/cli.cjs launch mca-poc `
  --java "C:\Program Files\Java\jdk1.8.0_202\bin\java.exe" `
  --jvm-arg "-Xbootclasspath/a:E:\4k\github-projects\MCWorldAgent\java\agent\libs\asm-9.7.jar" `
  --jvm-arg "-javaagent:E:\4k\github-projects\MCWorldAgent\java\agent\out\mcworldagent-agent.jar=log=<out>\agent-transform.log;hooks=<out>\agent-hooks.log"
#   手动：SinglePlayer → 进世界 → 走动 → Esc: Save and quit to title → 进程随游戏退出（code 0）
#   注意：ASM 经 -Xbootclasspath/a 注入（CLI 的 classpath 由 runtime 生成，不可加件）

# 备用/实验通路：Phase 0 harness（顺序与环境类实验，如 --agent-first / --java / --no-agent）
node integrations/attic/launch-poc.cjs --dry-run
node integrations/attic/launch-poc.cjs --timeout 600
$env:MCA_AGENT_EXTRA = 'disable=RUN_TICK;dump=C:\tmp\mca-dump'   # 逐钩禁用 / 织入后字节码落盘
```

日志落 `integrations/attic/out/<run>/`：`agent-transform.log`（织入轨迹）、`agent-hooks.log`（把手/tick/存盘事件+SUMMARY）、`game-stream.log`（CLI 通路游戏流）。历次 run 归档于 `out/run*/`、`out/cli-run/`。

### 2. 仓库结构 (Repository Layout)

```text
MCWorldAgent
├── plan.md                 分阶段开发计划与架构
├── java/
│   └── agent/              Java Agent：premain/指纹表/织入器/把手/日志（build.ps1 一键构建）
├── integrations/
│   └── attic/              launch-poc.cjs —— Phase 0 harness（备用/实验通路；默认走 attic-cli）
├── docs/
│   └── research/           取证案卷（phase0-agent-poc.md、phase1-first-brick.md、phase1-command-channel.md、research-integration.md）
├── apps/ · packages/ · java/core|adapters|storage|generator · integrations/mcp
│                           规划骨架（见 plan.md，随阶段落地）
└── LICENSE / README.md
```

### 3. 数据目录

复用 Attic 数据根：版本 jar 在 `%APPDATA%\.attic\versions\`，游戏实例（含 `saves\`）在 `%APPDATA%\.attic\default instance\`。本仓库不分发任何游戏 jar——首次实跑的 jar 由 Attic 下载链落位，harness 只做 SHA1 校验与就位。

---

## ❓ 常见问题（FAQ）

### Q1：为什么不做自己的启动器，而是借力 Attic？

远古版本的启动难题（Java 探测、legacyfix 挂载、Applet 容器参数、natives 池）Attic 已系统化解决。早期 Attic 没有 CLI，本项目以 harness 直接调用其 runtime 库保持零耦合；如今 Attic 已落地 `attic-cli` 内部开发控制面（无头启动链第二入口），本项目的启动通路收敛到 CLI——spawn 一个进程、拿纯游戏流和真实退出码，连 harness 都不再是必需品。两条路 Attic 都保持零改动。

### Q2：agent 会不会改坏我的存档？

Phase 0 的 agent 只观测不写入；Phase 1 起写侧已开禁，但一切修改排队到 tick 边界、由游戏自身方法执行（`setBlockWithNotify` 原路），显式存盘走 `saveWorld` 原路——第一块砖实跑证明放置→存盘→重进读回闭环无损。两条额外戒律：接管一个活实例前照旧备份 `saves\`（戒律第 5 条：绝不同时写同一个世界）；**强杀游戏进程前必须确认已退回标题界面**——在第一世界中强杀会引发 kill×存盘竞态，偶发损坏 NBT（Phase 1 run C 实锤教训）。

### Q3：支持哪些版本？

实跑验证目前仅 **infdev 20100327**（混淆实名、散件 `.dat` 存储、光照语义均已钉死归档）。版本映射会漂移（0325 的映射在 0327 已失效），每个版本都要重新 javap 钉指纹——"支持"以版本兼容登记表为准，未登记即不支持。

### Q4：日志在哪？怎么复现整条链？

`integrations/attic/out/`（见上文"如何构建与运行"）。案卷 [docs/research/phase0-agent-poc.md](./docs/research/phase0-agent-poc.md) 含完整证据行与复现步骤；SUMMARY 行在 JVM 正常退出时落盘，超时 kill 不触发。

---

## 📄 许可证与版权声明 (License & Copyright)

- 本项目仅供 Minecraft 历史版本研究、怀旧与技术交流使用。
- 源码采用 **[GNU General Public License v3 (GPL v3)](LICENSE)** 进行强制开源。
- **第三方内容免责声明**：
  - 本仓库**不包含、不分发任何官方 Minecraft 游戏 `.jar` 客户端包**；客户端 jar 由 Attic/Betacraft 数据链落位本地数据目录。
  - 本项目所涉及的 Minecraft 游戏资产、商标与品牌版权均归 **Mojang Studios / Microsoft** 所有。
