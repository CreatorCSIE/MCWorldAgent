# Phase 0 — Java Agent 全链 PoC 案卷（infdev 20100327）

日期：2026-11-12 ｜ 状态：**全链跑通（run4 完整成功）** ｜ Spec：`.trae/specs/phase0-agent-poc/`

## 1. 目标与路线

在 Attic 启动的 infdev 20100327 客户端上，用 JVM `-javaagent` 跑通垂直切片：
premain → 严格指纹织入 → 把手①（捕获活体 World+存档目录）→ tick 汇线 → 日志落盘，
并自答三缺口 G1（autosave 节律）/ G2（light 语义）/ G3（LoadingScreenRenderer 类加载）。

启动途径 = 路线 A：harness 直接 `require` Attic 的 `@attic/runtime` CJS dist →
`dryRunLaunch` 拿 LaunchConfig → `extraJvmArgs` 注入 `-javaagent` → 自行 spawn。**Attic 仓库零改动**（`git status` 干净，已复核）。

## 2. 钉死的事实

| 项 | 值 |
|---|---|
| versionId | `inf-20100327` |
| jar SHA1 | `47b1b32430a211520993552ba0a5e00c1af44724`（betacraft 原始件，入位 `%APPDATA%\.attic\versions\`，harness 闸门校验） |
| Java | `C:\Program Files\Java\jdk1.8.0_202\bin\java.exe` |
| gameDir | `%APPDATA%\.attic\default instance` |
| mainClass | `uk.betacraft.legacyfix.applet.AppletLauncher`（0327 jar 无 main，applet 形态） |
| class 版本 | major=49（**old verifier**，见 §5 教训） |
| ASM | asm-9.7，SHA1 `073d7b3086e14beb604ced229c302feff6449723`（build.ps1 下载+校验） |
| legacyfix | Phase 0 时 `26.11-attic.8`；2026-11-13 起钉版 `26.11-attic.10`（含 CtClass 池头插修复，§10）。规范顺序=legacyfix 在前、我们的在后（transformer 链顺序=挂载顺序） |

0327 混淆实名（javap 全量扫描钉死；0325 映射已漂移不可用。可读树：
`E:\4k\github-projects\minecraft-verification-analysis\src-minecraft\src-minecraft-infdev-20100327`）：

| 语义名 | 实名 | 关键签名 | 树引用 |
|---|---|---|---|
| World | `net.minecraft.a.a.f` | ctor `(Ljava/io/File;Ljava/lang/String;)V` public；saveWorld `a(Z)V` private | World.java L93/L131 |
| Minecraft | `net.minecraft.client.d` | runTick `h()V` private | Minecraft.java L578 |
| ChunkProviderLoadOrGenerate | `net.minecraft.a/a/a/e/c` | loadChunk `d(II)Lnet/minecraft/a/a/e/d;` private | ChunkProviderLoadOrGenerate.java L88 |
| Chunk | `net.minecraft.a.a.e.d` | generateHeightMap `a()V` public final 0x11 | Chunk.java L276 |
| LoadingScreenRenderer | `net.minecraft.client.a` | — | — |
| NBTTagCompound | `com.a.a.c` | — | — |

## 3. 实现

- `java/agent/src/com/mcworldagent/agent/`：
  - `MCWorldAgent.java` — premain 解析 `log=/hooks=/disable=/dump=`，注册 transformer，shutdown hook 写 SUMMARY；
  - `HookTargets.java` — 五钩指纹表（类名+方法名+描述符+access+期望命中数）；
  - `Transformers.java` — 两遍式：先验指纹（不符→`UNSUPPORTED`+整类跳过，零静默错钩），符合才注入；
    注入全为对称压弹（不新增控制流），`COMPUTE_MAXS` 不重算帧，避免触发 `getCommonSuperClass` 类加载；
  - `MCAHooks.java` — 捕获面（只收 Object/基本类型）+ tick 汇线 drain 队列（写纪律：改动只排入队列，游戏线程 tick 边界执行）。
- `java/agent/build.ps1` — javac(JDK8)+jar 手工构建，无 Maven/Gradle。
- `integrations/attic/launch-poc.cjs` — 六步 harness（require→SHA1 闸门→dryRunLaunch→classpath 追加 agent+ASM→spawn→日志捕获）；开关 `--dry-run/--timeout/--no-legacyfix/--no-agent`，env `MCA_AGENT_EXTRA` 透传 agent 调试参数。

## 4. 实跑时间线（日志归档 `integrations/attic/out/run1..run4/`）

| run | 结果 |
|---|---|
| run1 | 菜单态 5920 tick 稳定，三钩 HOOKED（超时 kill，未进世界） |
| run2 | 秒退（环境抖动） |
| run3 | 点 SinglePlayer 后游戏干净退出（exit 0）——基线对照（`--no-agent`）正常进世界 ⇒ 问题在注入 |
| 离线复现 | TransformDump + HeadlessWorldTest 钉死真凶：`VerifyError: (class: net/minecraft/a/a/f, method: <init>) Incompatible argument to function` |
| run4 | **全链成功**：建世界→游玩 146s→保存退标题→重进同世界→游玩 97s→保存退标题→关窗 exit 0，SUMMARY 落盘 |

## 5. 关键教训：old verifier 拒绝构造期 this 作实参

major=49 走老校验器：构造器内 `super()` 完成前 `this` 处于 uninitialized 状态，
`ALOAD_0` 把它作为 `INVOKESTATIC` 实参传给 hook → **VerifyError**（这正是 run3 "点 SinglePlayer 即退" 的根因，
异常被 applet 启动器吞掉表现为干净退出）。

**把手①重设计（两半捕获）**：
- `World.<init>(File,String)` 入口只注入 `ALOAD_1`（引用槽，合法）→ `onWorldDir(File)V` 记存档目录；
- `World.saveWorld(Z)V` 入口注入 `ALOAD_0 + ILOAD_1` → `onSaveWorld(Object,Z)V` 捕获活体 World——
  构造器尾必调 `saveWorld(false)`（树 World.java L120），saveWorld 是普通方法、this 已初始化，安全。

无头验证（HeadlessWorldTest，挂 agent 直接构造 World）：`WORLD-OK`，capturedWorld/capturedSaveDir 双非空。

## 6. run4 证据（`out/run4/agent-hooks.log` 关键行，verbatim）

```
[43347ms] WORLD_DIR dir=C:\Users\GAME\AppData\Roaming\.attic\default instance\saves level.dat=false
[43354ms] WORLD_CREATED world=net.minecraft.a.a.f@3c2369f9 dir=...\saves
[43354ms] SAVE_WORLD #1 id=3c2369f9 indirect=false        ← 构造器尾（新 World）
[89035ms] LIGHT_RECALC #1 … [89300ms] LIGHT_RECALC #10    ← 新 chunk 生成（日志封顶 10 条）
[189463ms] SAVE_WORLD #2 id=3c2369f9 indirect=true        ← Save-and-quit（saveWorldIndirectly）
[387425ms] WORLD_DIR …（重进同世界）
[387426ms] SAVE_WORLD #3 id=cb6d740 indirect=false        ← 构造器尾（第二个 World 实例）
[484757ms] SAVE_WORLD #4 id=cb6d740 indirect=true         ← 第二次 Save-and-quit
[491182ms] SUMMARY ticks=9702 saveWorld=4 chunkEnter=2804 chunkHit=2165 chunkMiss=639 lightRecalc=639
```

transform 日志：五钩全 `HOOKED`（`Minecraft.runTick` 198ms；`World.saveWorld`/`World.<init>` 924ms；
`loadChunk`/`generateHeightMap` 43.3s 类加载时）；`LOAD net/minecraft/client/a` @ 888ms。

落盘：run4 的世界为 `saves\World2`（22:27 创建，`level.dat` 存在 + 1729 个 chunk `.dat`，mtime 全部落在 run4 时段，base36 散件目录、非 .mcregion）。
`saves\World1`（290 dats）系 2026-11-7 遗留旧世界，本次零改动，勿混淆。

## 7. 三缺口答案

### G1 — autosave 节律：**发行运行时没有 10 秒级自动存；间隔被 legacyfix 改为 5 分钟**

- 原始字节码：`World.h()`（restartTimeOfDay，Minecraft.runTick 每 tick 非暂停时调用，javap `net.minecraft.client.d` 偏移 1101 实证）含
  `++worldTime; if (worldTime % 200 == 0) saveWorld(false)`（javap 偏移 101-126，`ldc2_w 200l / lrem / invokespecial a:(Z)V`）⇒ 原始节律 ≈10s。
- 但 legacyfix 的 `uk.betacraft.legacyfix.patch.impl.misc.AutoSavePatch`（javassist）自述：
  *"Changes the interval of world autosaving to (by default) 5 minutes"* ⇒ 运行时模数被改写为 6000 tick。
- 实测自洽：两段游玩（146s、97s）均 <5min，周期存零触发；全程仅 4 次 saveWorld = 构造器尾×2 + 退出存×2。
- **对 MCWorldAgent 的设计结论：不得依赖游戏自动存。改动生效须经把手①在游戏线程显式调 `saveWorld`（排队进 tick 边界），或接受退档存。**

### G2 — light 语义：**信任存档，仅数据缺失才回退重算**

- 静态：`Chunk.readChunkNBTData` 直读 `SkyLight/BlockLight/HeightMap` 信任存档；仅当
  `heightMap==null || !skyLightMap.isValid()` 才回退 `generateHeightMap`（字节码 123-165）。
- 实测不变量：`lightRecalc(639) == chunkMiss(639)` —— 每个磁盘未命中（新生成）chunk 恰好一次重算；
  磁盘载入/内存命中的 chunk（chunkHit=2165，含缓存命中）**零重算**；重进同世界后无 LIGHT_RECALC 风暴。
- **结论：agent 写区块后只要带齐合法光照/高度数据落盘，重进不会触发重算；反之（置空 heightMap）会。**

### G3 — LoadingScreenRenderer 类加载：**无问题**

- `net.minecraft.client.a` 在挂 agent 的实跑 888ms 即 LOAD（transform 日志 L807），早于世界创建，织入器全类旁观不干扰。

## 8. 复现步骤

```powershell
powershell -File java/agent/build.ps1                      # 构建 agent jar（ASM 自动下载+SHA1 校验）
node integrations/attic/launch-poc.cjs --timeout 600       # 起游戏（SHA1 闸门→dryRunLaunch→spawn）
# 手动：SinglePlayer → Create New World → 走动 → Esc: Save and quit to title → 关窗
```

调试开关：`--dry-run`（只出 LaunchConfig）、`--no-agent`（基线对照）、`--no-legacyfix`、
`MCA_AGENT_EXTRA=disable=<hookId>;dump=<dir>`（逐钩禁用/织入后字节码落盘）。

## 9. 遗留与下一步

- `chunkHit` 含 in-memory 缓存命中，非纯磁盘读；后续如需区分磁盘 IO，钩 `AnvilChunkLoader`/readChunk 层。
- 测量层缺口：磁盘 chunk 总数（World2=1729）> 钩子观测的 `loadChunk` miss（639），说明 0327 的生成/载入还有旁路 `ChunkProviderLoadOrGenerate.loadChunk` 的路径（如上层 provider 批量生成、构造器 bulk-load）；G2 的 `lightRecalc==chunkMiss` 不变量在观测层成立，但绝对数受钩层覆盖度限制，Phase 1 如需精确磁盘账目应改钩 `World.getChunkFromChunkCoords` 层。
- AutoSavePatch 的 6000 常量系补丁自述+行为自洽推定，**已定性为挂 legacyfix 这个 javaagent 的已知代价，接受、不再钉死**（设计结论不依赖精确常量：反正不能指望游戏自动存）。
- 把手①已可用：下一步（Phase 1）在 `onTick` drain 队列上执行世界修改 + 显式 `saveWorld` 落盘验证。

## 10. CLI 通路升级（2026-11-13，`out/cli-run/`）

Attic 落地 `attic-cli` 后，本 PoC 的启动通路从自制 harness 升级为 CLI。过程中钉死一个新缺陷并促成姊妹仓库修复：

**缺陷链（run5/E1 实验）**：CLI 实例档案 `launchArgs` 的组序在 legacyfix javaagent **之前**；
legacyfix `Agent.java` 的 load-time CtTransformer 链本意是基于输入字节透传（`ByteArrayClassPath(currentBuffer)`），
但 `appendClassPath` 把输入源加在**尾**、`new ClassPool(true)` 的系统路径含客户端 jar 在**前**，
`get()` 先命中 jar 原始字节 → 被 patch 的类（Minecraft）遭 wholesale 重建，**静默吞掉前置 agent 的织入**
（run5：runTick HOOKED 但 0 触发；无 patch 类 World/Chunk/CPLG 不受影响）。E1 单变量实验（仅交换顺序）复现。

**双修复**：
1. legacyfix-attic `attic.10`（09537dc）：`insertClassPath` 头插输入字节——顺序不再致命（安全网）。
2. Attic CLI：`--jvm-arg`（可重复，尾追加到 legacyfix 之后）+ `--java`（钉死 JDK，旁路自动探测的 431 漂移）。
   规范顺序仍是 **legacyfix 先、观测层后**（观测建立在修复层的最终产物上，且不污染其模式匹配），
   `--jvm-arg` 是该顺序在 CLI 通路的唯一载体。

**ASM 依赖**：thin jar 不含 ASM，CLI 的 classpath 由 runtime 生成不可动——用
`--jvm-arg "-Xbootclasspath/a:<asm-9.7.jar>"` 走系统类加载器，无需 fat jar。

**定型命令（默认启动通路）**：

```powershell
cd e:\4k\github-projects\Attic-Minecraft-Launcher\apps\tauri\sidecar
node dist/cli.cjs launch mca-poc `
  --java "C:\Program Files\Java\jdk1.8.0_202\bin\java.exe" `
  --jvm-arg "-Xbootclasspath/a:E:\4k\github-projects\MCWorldAgent\java\agent\libs\asm-9.7.jar" `
  --jvm-arg "-javaagent:E:\4k\github-projects\MCWorldAgent\java\agent\out\mcworldagent-agent.jar=log=<out>\agent-transform.log;hooks=<out>\agent-hooks.log"
```

**实跑对账（重进 World2，游戏自然退出 code=0）**：五钩全 HOOKED（runTick @190ms 实调用，TICK 行 20TPS）；
`SUMMARY ticks=2634 saveWorld=3 chunkEnter=2017 chunkHit=1975 chunkMiss=42 lightRecalc=42`——
G2 不变量在 CLI 通路复现；CLI 会话语义（游戏流直通、退出码传播）验证成立。

**harness 定位**：`launch-poc.cjs` 降级为备用/实验路径（其 `--java`/`--agent-first`/`--out` 开关保留，
顺序与环境类实验仍用它）。
