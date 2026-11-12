# 调研结论对接备忘录（plan.md × 可行性调研案卷）

> 日期：2026-11-12
> 案卷：[minecraft-classic-to-infdev-internal-level-api-external-reuse-analysis.md](file:///E:/4k/github-projects/minecraft-verification-analysis/minecraft-classic-to-infdev-internal-level-api-external-reuse-analysis.md)（下称 MD，引用记作 §n）
> 定位：plan.md 是开发方向，MD 是已核证据。本文只做三件事：**模块对榫、强制修正、缺口移交**。不重复两边内容。

---

## 1. 模块对榫（plan 模块 ↔ 案卷结论）

| plan 模块 | 案卷结论 | 状态 |
|---|---|---|
| §3.3 Java Core / §4.4 Standalone Generator | 分窗 headless 配方已钉死：Classic（stub LWJGL 劫持＋形状反射，§1.2）、indev 0124~0223（`LevelGenerator(IProgressUpdate)`＋`LevelLoader`，§2）、0227~0325（`ChunkProviderGenerate()` 无参＋`provideChunk→byte[]`，全案最简，见本文 §6a）、0327~（`World(File,String)`，§4）。"复用原始生成逻辑而非重实现"＝§1.3 保真原则（反编译树有伪影，必须跑原始字节码） | ✓ 直接可落 |
| §4.5 Java Agent | premain 选型与案卷结论一致（§8.1：transform 期常量指纹白送定位、`final` 对字节码转换不是概念）。"strict matching / prefer reporting unsupported hook"＝§8.3 三层兜底＋"绝不静默错钩"。§8.2 四手法（引用捕获/回调装饰/写点插桩/tick 汇线）即 version-specific hooks 的实现菜单 | ✓ 已互证 |
| §3.4 World Data Model | "不强迫人工等价物"有实证背书：indev `World.load` 派生数据（heightMap/light/spawn）载入重算（§7.1）；0227~0325 懒 chunk＝ABSENT/LOADING 状态的真实原型（§3）；"missing chunk ≠ 生成请求"正是该案卷 pump 语义的镜像 | ✓ |
| §4.2 World Storage Engine | 三容器格式全钉死：Classic gzip raw（magic `656127880`＋索引序 `(y*h+z)*w+x`＋写序 w/h/d vs 读序 w/d/h 坑，§1.2）、mclevel NBT 标签全集（§2）、Alpha 目录 level.dat+.mcregion（§4）。reader/writer 可各自独立实现，但**必须回灌原版 loader 对拍**（§5 末段：无存档窗口的产出合法性只能对 0124/0327 实物验证） | ✓＋纪律 |
| §7 Data Ownership | agent 写纪律两铁律（§8.4）：写必须回游戏线程（tick 汇线，~1 tick 延迟）；单人客户端即权威、无校验层。与 plan"Agent 模式下运行进程是状态所有者、不并发写文件"完全一致——补充：**agent 模式的合法写路径是内存 setBlock，不是文件** | ✓ |
| §4.9/§3.5 Rendering | plan 选 Three.js/WebGL2 ＝案卷讨论中的"路线 R（自研渲染器吃数据）"。案卷另有"路线 O（原版渲染类透传，像素级保真含 bug）"未列入 plan——见 §6d 约束，暂不作里程碑，留作未来高保真模式 | ✓（O 记档） |
| §4.10 MCP / World API | §9 终版能力矩阵（三条腿：纯数据/headless/agent × 六项能力）可直接作为 World API 的操作面清单与 capability 字段来源 | ✓ |
| §4.8 Analyzer 集成 | 种子时间线是分析类工具的硬前提：slime/monolith/stronghold 类"按种子推导"的分析器**只对 0327~ 成立**（§0：更早无种子概念） | ⚠ 入 registry |

## 2. 案卷对 plan 的强制修正（registry 必须编码的事实）

1. **两个无存档窗口**（§0/§3/§5）：indev 1223~0110（管线未建）与 infdev 0227~0325（管线掏空）。plan §2 表格"Indev=Finite world storage"只对 0124~0223 成立。Version Compatibility Registry 的存储字段需要 `NO_STORAGE` 态；这两个窗口的"保存"＝agent 外挂容器（§8.4，独占能力）或工具自备容器（schema 选型见 §6b）。
2. **种子可复现性**：可复现生成自 0327~ 起（`RandomSeed` 入 level.dat，§4）；Classic/indev/早 infdev 生成器全为裸 `new Random()`（§0 种子扫描，15 树实证）。Generator profile 必须按版本标注 reproducible: true/false。
3. **indev 实体读档空壳**（§2/§7.1）：`loadEntity` 返回 null——mclevel 的 `Entities` 标签游戏自己读档即丢。Entity 模型不得对 indev 承诺实体往返（方块级可保，`TileEntities` 活）。
4. **实时通道唯一性**（§7.3）：MP 协议在 Indev~Infdev~a1.0.9 整段断（server-index §0.10~0.13），文件轮询依赖 autosave（？未核）。**这一时代的实时只有 agent 一条路**——plan 以 agent 为中心的设计恰好是唯一解，可写入 rationale。
5. **jar 三态准入**（§11 第 5 条＋项目惯例）：registry 只收原始件；高仿/重建件（c0.0.13a-dev 系）与 mod 件（0408/a1.2.7/b1.9.jar 等）不得标记 supported；§8.3 的"表命中但指纹不符"检测器可作准入校验实现。
6. **历史世界能力边界**（§7.2）：无存档窗口的特定历史世界物理不可重建（任何路线）；里程碑与文案不得承诺"复活任意老世界"。

## 3. 垂直切片选版建议（plan §12）

里程碑九步（开档→读→3D→agent 运行→找玩家→安全操作→存盘验证）的最短全链路版本：

- **首选 infdev 20100327**：存储接口最简（`World(File,String)` 构造即读写，§4）＋种子活（可复现演示）＋agent 把手①（构造拦截同时拿活体 World＋存档目录）＋mcregion 是 MCR 直系祖先（向 Alpha/Beta 自然延伸）。
- 次选 indev 20100213：mclevel 活＋`IProgressUpdate` 原生回调（把手②演示位）＋实体在内存可读（文件读不出的 agent 读得出——正好演示两条模式的差异）。
- Classic 不建议作首发：stub 劫持链（§11 第 1 条）未 PoC，风险叠加。

## 4. Phase 0 缺口移交（案卷欠账 → plan 任务）

| 缺口 | 出处 | 建议 plan 落点 |
|---|---|---|
| **Alpha/Beta 全段未调研**（0327 后 World/种子/实体/格式演化未逐树核；耦合面预期变大） | §11 第 2 条 | plan 目标含 Alpha/Beta，Phase 0 必须补这段（0413/0611/0617/0701~0721/a1.0.x/b1.x 接口面＋MCR 演化） |
| Classic 全链 headless PoC（锚点+stub+形状反射） | §11 第 1 条 | Phase 0 "Test Java Agent instrumentation" 同批 |
| agent 全链 PoC（premain/织入/Proxy/汇线） | §11 第 6 条 | Phase 0 |
| 0327~ autosave 有无与节律 | §11 第 7 条 | 文件轮询通道前提；agent 插 `saveWorld` 实测自答 |
| `.mcregion` light 语义（load 重算 or 信任存档） | §11 第 8 条 | 存储引擎 writer 设计前提（若信任存档，改方块须清 light） |
| `LoadingScreenRenderer` 系静态助手类加载实测 | §11 第 3 条 | transform 日志直接可答 |

**缺口调研的执行入口**：上述欠账（尤其 Alpha/Beta 全段）不必另起炉灶，直接在考古工作区按其运行手册 [README.md](file:///E:/4k/github-projects/minecraft-verification-analysis/README.md) 开工——版本存在性以 [version-index.md](file:///E:/4k/github-projects/minecraft-verification-analysis/indexes/version-index.md)（客户端）／[server-index.md](file:///E:/4k/github-projects/minecraft-verification-analysis/indexes/server-index.md)（服务器，实物为外置链）为唯一权威；jar 实物基地 `%APPDATA%\.betacraft\versions\`（引用前 SHA1 对表，三态准入）；探查顺序先源码树后字节码（README §0 第 8/9 条）。Alpha/Beta 段现成起点：`src-minecraft` 已有 Alpha 系与 Beta 系 26 棵可读树＋Late-Infdev（0413~0630）全段，且 zone 存档格式案、isom 预览器案、独石柱案、地牢生成史、实体体系拆装案等既有案卷已覆盖部分接口面，可直接作线索复用。**无树兜底**：树仅覆盖部分版本（索引 306 行 vs 树 112 棵），目标版本无树时"先树后字节码"退化为直接字节码勘察——SHA1 对表原始件→`jar tf` 核混淆布局→`javap -p -c` 钉 offset，可读性不足再上 `tools/cfr-0.152.jar` 反编译＋`client0218.tiny`/树内 `client.tiny` 挂映射（案卷 §0 的混淆实物核对即此路数；§6 常量指纹/锚点游走策略正是为无树勘察设计的）。

## 5. 渲染栈红利（plan 架构对案卷遗留问题的消解）

案卷讨论期悬置的"LWJGL 世代/双栈"问题（§6e）在 plan 架构下**自动消解**：UI/渲染在 Tauri WebView（Three.js/WebGL2），Java Core 只做生成/存储/agent——**工具进程不需要任何真 LWJGL**，只需 §1.2 的 stub（自编译、零 native）；真 LWJGL 只存在于游戏进程（agent 宿主）。路线 O（原版渲染透传）若未来要做，也应挂在 agent 侧（游戏进程内离屏 FBO→帧回传），而非 Java Core。

## 6. 讨论补充结论（已定方向、未写入案卷正文）

以下五则来自案卷落卷后的同轮讨论，结论级但案卷正文未含，先记于此，待回填 MD：

a. **0227~0325 headless 配方**：`ChunkProviderGenerate()` 无参构造＋`provideChunk(ChunkPosition)→byte[]` 直吐方块数组——不收 World、不收回调、零 GL，全时间线最干净生成器；工具循环 pump 任意区域即可。
b. **外挂存档 schema 选型**：mclevel 固定容器装不下无限世界（这正是 0327 重建格式的历史原因），故 0227~0325 的外挂档只能采 **Alpha 目录 schema**；接法取"工具侧外挂"（agent 流出→工具写容器→回载注 chunks map），不做游戏侧逆移植 mod（产物＝mod 件，三态出局；前人逆移植 mod 的存在佐证方向可行）。保真陷阱三条：`RandomSeed` 不得造假（写 synthetic 标记）、派生字段清零靠目标版本重算、实体 best-effort。
c. **无种子后门**：`java.util.Random` 是 48-bit 可逆 LCG——观测两次连续输出即还原状态，回拨调用计数即得等效种子；工具/agent 侧可把 `new Random()` 改写为 `new Random(注入值)`，给无种子窗口补"指定种子生成"。限制：agent/工具必须在场于生成时刻；多 Random 实例（rand/rand2/World.random）须全家桶；历史世界依旧不可复活。
d. **渲染双路线**：R＝自研渲染器吃数据（＝plan Three.js 路线，跨平台、codec 漂移风险由对拍纪律兜底）；O＝原版渲染类透传（像素级保真含 bug，需 compat profile/LWJGL 世代供给，实验档）。
e. **LWJGL 三档供给**（游戏侧运行时是可插拔槽位）：全 stub no-op（生成 headless）→ 真实现 shim（若做 O）→ 整包 LWJGL2（O 的一等化选项）。GL11 FQCN 在 LWJGL2/3 间签名兼容是 O 单栈化的依赖点，未实测。

---

**一句话结论**：plan 的两大技术押注（premain agent＋原始 JAR 复用）与案卷全部实证结论同向，无冲突；需要 plan 立即吸收的是 §2 六条 registry 硬事实与 §4 六条 Phase 0 缺口（尤其 **Alpha/Beta 调研空白**——它是 plan 目标与案卷边界之间唯一真正的断层）。
