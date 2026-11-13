# Phase 1 — "第一块砖"写侧接管案卷（infdev 20100327）

日期：2026-11-13 ｜ 状态：**全链跑通（run A/B/C2 + 失败路径实锤）** ｜ 前置：[phase0-agent-poc.md](./phase0-agent-poc.md)（CLI 定型命令见其 §10）

## 1. 目标与最小闭环

Phase 0 只观测不写入。本案卷钉死写侧最小闭环——"第一块砖"：

> agent 经把手①在游戏线程放一方块 → 显式 `saveWorld` 落盘 → 重进世界读回验证 → 失败路径语义成立。

设计约束全部继承 Phase 0：写纪律（只排队、tick 边界 drain）、G1（不可依赖游戏自动存）、G2（光照带齐合法数据）、old verifier 戒律。

## 2. 写侧混淆实名（T1，javap 字节码特征钉死）

`World = net.minecraft.a.a.f`（0327；0325 映射已漂移不可用）：

| 语义 | 实名 | 签名 | 备注 |
|---|---|---|---|
| setBlockWithNotify | `c` | `(IIII)Z` public final | 唯一写入口，自带光照/脏标记/`worldAccesses` 渲染通知 |
| getBlockId | `a` | `(III)I` public final | 读回验证 |
| spawnX / spawnZ | `g` / `i` | public int | NBT 锚点 SpawnX/SpawnZ，作放置坐标锚 |
| saveWorld | `a` | `(Z)V` private | Phase 0 已钉；显式存盘（G1） |

**方块选型**：cobblestone ID=4——无重力、无更新副作用，且 0327 地表不会自然生成
（stone ID=1 会地表暴露导致"已存在"误判，弃用）。地表顶块实测 `surface=2`（grass）。

## 3. 实现（零新字节码注入）

写侧**不新增任何织入**——全部经反射调用游戏自身方法，无 old verifier 风险面：

- `java/agent/src/com/mcworldagent/agent/FirstBrick.java`：反射 `getMethod("c",…)`/`getMethod("a",…)`，
  从出生点 (g,i) 起 y=100 向下探地表，顶块已是 4 → `FIRST_BRICK_VERIFY_OK`（重进读回分支）；
  否则目标格非空 → `OCCUPIED` 放弃；放置成功 → `PLACED` + 立即 `saveWorld(false)` → `SAVED`；
  任何异常（含 `NoSuchMethodException`）→ `FIRST_BRICK_UNSUPPORTED`，游戏零损伤。
- `MCAHooks.onSaveWorld`（首见活体 World 处）：仍在本栈帧内，**不直接执行**，
  `submit(FirstBrick.run(w))` 排队到下一 tick 边界 drain——与玩家挖/放方块同线程同方法，
  `worldAccesses` 渲染通知天然安全。

## 4. 实跑时间线（日志归档 `integrations/attic/out/first-brick{,-b,-failtest,-failtest2}/`）

启动 = Phase 0 §10 定型 CLI 命令（`attic-cli launch mca-poc` + `--java` + ASM `-Xbootclasspath/a` + `-javaagent`）。

| run | 目录 | 结果 |
|---|---|---|
| A | `first-brick/` | 新建 World2 → **PLACED (0,71,0) id=4 → SAVED**；用户目视圆石+渲染正常（通知链生效）→ Esc Save and quit 存盘 |
| B | `first-brick-b/` | 重进 World2 → **VERIFY_OK**（砖仍在，重进载入成功）；⚠️ 结束时被 StopCommand 强杀（用户仍在世界中）→ 埋下 run C 祸根 |
| C | `first-brick-failtest/` | 进 World2 即崩 `IndexOutOfBoundsException`（NBT 加载栈）——**非 agent 所致**，见 §6 |
| C2 | `first-brick-failtest2/` | 临时失败版 jar（写方法实名 `"c"`→`"z"`）：进世界正常 → **UNSUPPORTED NoSuchMethodException**，游戏无恙、tick 照常 → 失败语义实锤；用户 Esc 回标题后停 |

### run A 证据（`agent-hooks.log` verbatim）

```
[15521ms] WORLD_DIR dir=...\saves level.dat=false
[15528ms] WORLD_CREATED world=net.minecraft.a.a.f@5058db86 dir=...\saves
[15528ms] SUBMIT queued (runs on game thread at next tick)
[15528ms] SAVE_WORLD #1 id=5058db86 indirect=false        ← 构造器尾（把手①捕获点）
[17109ms] FIRST_BRICK_PLACED x=0 y=71 z=0 id=4 surface=2  ← tick 边界 drain 执行
[17109ms] SAVE_WORLD #2 id=5058db86 indirect=false        ← 显式存盘（G1 纪律）
[17122ms] FIRST_BRICK_SAVED saveWorld(false) 已调用
[17122ms] TICK 269 drained=1
[41209ms] SAVE_WORLD #3 id=5058db86 indirect=true         ← 用户 Esc: Save and quit
```

WORLD_CREATED→PLACED 间隔 1.58s = 地表探测 + 新 chunk 生成期，drain 恰落在 tick 269 边界。

### run B 证据（重进读回，verbatim）

```
[8251ms]  WORLD_CREATED world=net.minecraft.a.a.f@164c091d …
[8251ms]  SUBMIT queued (runs on game thread at next tick)
[9784ms]  FIRST_BRICK_VERIFY_OK x=0 y=71 z=0 id=4（上一 run 放置的砖仍在，重进载入成功）
[9784ms]  TICK 116 drained=1
```

读回分支走 `getBlockId a(III)I`，磁盘 `.dat` → 内存 → agent 反射读，闭环成立（用户并目视确认圆石还在）。

## 5. 失败路径语义（run C2）

把 `FirstBrick` 的写方法实名临时配错（`"c"`→`"z"`）重编译，实跑：

```
[8520ms]  SUBMIT queued (runs on game thread at next tick)
[10002ms] FIRST_BRICK_UNSUPPORTED java.lang.NoSuchMethodException: net.minecraft.a.a.f.z(int, int, int, int)
[10002ms] TICK 128 drained=1
```

进世界正常不崩、任务被 drain、异常被捕获记日志、tick 流照常 20TPS——
**签名不符 → 显式报告 UNSUPPORTED，零静默、零损伤**，与 Phase 0 织入层的"宁可不支持，不做不确定变换"同语义。
源码随后已还原为 `"c"` 并重编译（jar 与源码一致）。

## 6. run C 崩溃归因（教训）

进 World2 即崩（`game-stream.log` verbatim 栈顶）：

```
java.lang.IndexOutOfBoundsException: Index: 0, Size: 0
	at com.a.a.f.a(Unknown Source)                ← NBT 列表读取
	at net.minecraft.a.c.b.d(Unknown Source)
	at net.minecraft.a.a.f.a(Unknown Source)      ← World 载入
	at net.minecraft.client.d.a → …runTick
```

**判定：非 agent 所致**——hooks 日志无 `FIRST_BRICK_*`/`UNSUPPORTED`，崩在同一 `runTick` 的**加载阶段**、
drain 之前（FirstBrick 尚未执行）；CLI 崩溃分类器抓到 `category=index-out-of-bounds`，
但 exit code=0（0327 崩溃路径 `System.exit(0)`，与 Phase 0 run3 同形态——分类靠流日志，不靠退出码）。

**真凶**：run B 被 StopCommand 强杀时用户仍在世界中，kill×存盘竞态偶发损坏 `level.dat`/chunk NBT。
C2 用同一存档重进即正常，佐证损坏是偶发写竞态而非存档必然损坏。

> **纪律（新增）**：强杀游戏进程（StopCommand/taskkill）前，**必须确认用户已退回标题界面**
> （即游戏已完成 Save-and-quit 存盘）。此教训优先级同 G1。

## 7. 版本登记表更新

infdev 20100327 写侧操作**已验证**：`setBlockWithNotify` 放置（渲染通知正常）、显式 `saveWorld` 落盘、
重进读回、失败路径语义。plan.md §2.1 与 Phase 1 相关条目已回写。

## 8. 复现步骤

```powershell
powershell -File java/agent/build.ps1     # 构建（源码当前=还原后的正式版）
# 启动：Phase 0 案卷 §10 定型 CLI 命令（out 目录换名即可）
#   手动：SinglePlayer → 新建/进入 World2 → 出生点旁可见圆石 → Esc: Save and quit → 重进验证 VERIFY_OK
```

## 9. 遗留与下一步

- 外部提交通道：`submit()` 目前由 WORLD_CREATED 自动触发一次性任务；接桌面壳/MCP 需外部指令→agent 的通道（plan §4.5 剩余项）。
- 钩层下探：批量写与磁盘账目精确化需钩 `World.getChunkFromChunkCoords` 层（Phase 0 §9）。
- 写侧覆盖面：本闭环只钉了 `setBlockWithNotify` 单点；元数据（level.dat 字段）、实体写入未动。
- World2 现为"有砖"验证世界，保留勿删；如需干净复跑，新建世界即可（放置锚是出生点，自动适配）。
