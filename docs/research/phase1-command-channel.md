# 案卷 — Phase 1 命令通道 v1 实跑（cmdchannel-1/2）

日期：2026-11-13 ｜ 设计案卷：[agent-command-channel.md](../architecture/agent-command-channel.md)（v2）｜ 日志：`integrations/attic/out/cmdchannel-{1,2}/`

## 1. 交付物

| 件 | 内容 |
|---|---|
| `MiniJson.java` | 严格子集解析器：顶层对象+一层 args；拒数组/null/浮点/重复键/尾随/深度>2/串>256/文件>4KB。零依赖 |
| `WorldOps.java` | 0327 反射操作面：`c(IIII)Z`/`a(III)I`/`a(Z)V`（与 FirstBrick 同源实名）；边界校验 y∈[1,127]、x/z±32000000、blockId∈[0,255]；UnsupportedOp/Rejected 双异常型 |
| `CommandChannel.java` | daemon 轮询线程（premain 起，250ms≈5tick）：tmp→inbox 消费、先移 processed(=ACCEPTED) 再 submit、outbox 原子终态（~id.json→id.json）；限额 ≤8/轮、积压>64 queue-full；会话内 seenIds 幂等；validate 返回具体拒绝原因 |
| 接线 | `MCWorldAgent` 解析 `cmd=`；`MCAHooks` 加 `tickCount()`；FirstBrick 加一次性闸门（终态置位、PROBE_FAIL 可重试——防 save 命令触发钩子重复补砖） |
| 无头断言 | 28/28（MiniJson 22 + validate 6，%TEMP%\mcaut，不入库） |

## 2. 实跑链

**run cmdchannel-1（jar v1）**：CHANNEL_START@28ms → WORLD_CREATED@28.2s（dir 捕获 saves，回溯两级解析 `CHANNEL_DIR C:\Users\GAME\AppData\Roaming\.attic\mca`）→ FIRST_BRICK_VERIFY_OK（Phase1 砖存续）。命令全绿：

- `getblock 0,71,0`→OK id=4；`dig 0,71,0 save=true`→OK dug saved=true；读回 id=0；**用户目视圆石消失（截图）**
- 批量失败路径：dup-id→`duplicate-id`（意外实锤，见教训 4）；重复键 JSON→`bad-json: duplicate key 'id'`；未知 op/协议版本 2→REJECTED（文案笼统→当场修 validate 返回原因）
- **同进程 Esc 回标题重进后双点读回"成功"——实为旧对象**（教训 1），本 run 持久化判定作废；place 持久化后由 run 2 的 g9 补实

**run cmdchannel-2（jar v2，双修复）**：WORLD_CREATED@50.6s（新实例@6f51b271）→ FIRST_BRICK_PLACED@52.1s（锚点自动补砖，预期内）。

- `dig 2,70,2`→**REJECTED "setBlockWithNotify returned false"**——挖空气无变化=false，语义与游戏一致（免费验证写操作失败路径）
- `getblock 9,71,9`→id=4：**place 持久化实锤**（跨进程）
- 地形探针：(2,2) 水柱（id=9@63/62）、(4,4)/(6,6) y70 空气、(-4,-4) 泥土 id=3@70
- `dig -4,70,-4 save=true`→OK；读回 0
- Esc 存退→**同进程重进**：`WORLD_SWITCHED old=@6f51b271 new=@3479b51f`（修复生效）→ 三点终验：`-4,70,-4=0`（**dig 持久化实锤**）、`9,71,9=4`、`0,71,0=4`（锚点补砖存续）

## 3. 教训（编号续 Phase 1）

8. **capturedWorld 一次性捕获是陷阱**：同进程重进世界产生新实例，句柄必须跟进最新活体（WORLD_SWITCHED 日志+恒更新）——否则通道读到退出时旧对象，持久化验证失真且写命令有脏盘风险。
9. **持久化探针必须避开 FirstBrick 锚点**：新进程锚点自动补砖会"修复"被挖的 0,71,0，污染 dig 验证。探针选独立坐标。
10. **setBlockWithNotify 无变化返回 false→REJECTED**：dig 空气/重复 place 均走此路，与游戏行为一致，协议语义自洽。
11. **inbox 按文件名序消费，非信封 id 序**：同批测试文件命名不当会让"重复 id"先入队——本次意外实锤幂等，但测试设计应显式控制。
12. **tmp/ 是客户端目录**：agent 只建 inbox/processed/outbox，CLI/壳自备 tmp。
13. **同进程"存退重进"是合法的验证捷径**（修复后）：省一次重启，且顺带覆盖 WORLD_SWITCHED 路径。kill 前确认回标题纪律不变。

## 4. 状态与下一步

命令通道 v1（place/dig/getblock/save 四 op）验收完毕。§5 判决的 P0 fixture 化（`noFirstBrick=`）与 P1 世界世代（`capturedWorldGeneration` + 信封 `targetGeneration`）已落码并实跑验收（2026-11-13，run `p0p1-gen`，见 §6）。Query `getinfo` 已落码并实跑验收（2026-11-13，run `getinfo-1`，见 §7）。写侧 `settime`/`setspawn` 已落码并实跑验收（2026-11-13，run `setw-1`，见 §8；setspawn 语义经用户判决纠正为 respawn 锚点）。写侧 `tp` 已落码并实跑验收（2026-11-13，run `tp-1`，见 §10：目视瞬移+边境之地远距+Pos[1]=y+1.62 磁盘对账+存退重进落 tp 点；setw-1 遗留的"实例归属"疑点闭环——当轮未执行重进，见教训 16）。待办：批量写（setTileNoUpdate+脏区标记）第二批；桌面壳"命令台+事件流"底部面板（VS Code 式，速记→封包翻译器）为壳 Phase 2 首件；mca/ 证据已归档各 run `mca-archive/`。

## 5. 外部评审吸收与 v2 待办（2026-11-13，ChatGPT review 判决）

| 评审意见 | 判决 | 落档待办 |
|---|---|---|
| WorldSession 五字段对象（worldInstance/saveDir/generation/createdAtTick/lifecycleState） | **采纳内核、砍无证据外壳**：0327 拿不到 LOADING/UNLOADING 可靠信号（无卸载钩子，saveWorld 仅在存盘触发），lifecycleState 违反"行为必须来自取证"戒律；saveDir/createdAtTick 已有等价物 | **P1**：`capturedWorldGeneration`（WORLD_SWITCHED 时自增）+ 命令可选 `targetGeneration`——普通命令"执行时绑定当前世界"，会话定向命令带世代，不符 REJECTED。防"提交-执行跨世界切换"窄竞态。**已实现（2026-11-13）**：MCAHooks 世代计数初值 1、WORLD_SWITCHED 分支自增并记入日志；CommandChannel validate 仅形态校验（非整数→REJECTED），执行时（tick 边界）比对不符→`generation-mismatch expected=X actual=Y`；无字段=绑定当前世界，两者不混同 |
| setBlockWithNotify false ≠ 异常，增设 NO_CHANGE 态 | **全采**：dig 空气/重复 place = "调用成功但无变化"，混入 REJECTED（请求非法）污染语义。v1 归档账目不动 | **v2 协议修订**：终态扩为 OK/NO_CHANGE/REJECTED/UNSUPPORTED/ERROR |
| FirstBrick 应标记为 PoC Fixture，自动写入须可关 | **全采且提为 P0**（成本最低收益立现：一个 agent 参数，复用 `disable=` 解析路径；本 run 已被锚点污染咬过一次，见教训 9） | **P0**：`noFirstBrick=` 启动参数；后续 dig/批量写/清世界测试一律带参跑。**已实现（2026-11-13）**：premain 解析 `noFirstBrick=true`（风格对齐现有 key=value），置 MCAHooks 静态开关；onSaveWorld first 分支跳过 submit 并记 `FIRST_BRICK_SKIPPED`（WORLD_CREATED 日志保留）；不带参=默认行为不变 |
| 优先级：生命周期+World Properties 先于批量写 | **接受**，与我们上轮结论同向；补取证前提：tp 在 0327 必须走 `setPosition`（AABB-first 管线）+ 同步 lastTick/prev，配方已钉 ingame-console §7 | 排序：P0 fixture 化 → P1 世代 → settime/setspawn → tp → 批量写 → 命令台 |
| 命令台订阅 Agent 事件（WORLD_CREATED/COMMAND_ACCEPTED/…） | **接受且几乎免费**：所列事件全部已是 hooks 日志行，面板事件流=tail 日志+watch outbox，无需另造事件系统 | 并入独立案卷 [shell-command-panel.md](../architecture/shell-command-panel.md) |

## 6. P0/P1 实跑验收（2026-11-13，run `integrations/attic/out/p0p1-gen/`）

启动参数追加 `cmd=<run>\mca;noFirstBrick=true`，游戏自然退出 code=0，证据归档 `mca-archive/`。全绿：

| 断言 | 证据 |
|---|---|
| P0 零自动写入 | `WORLD_CREATED … generation=1` + `FIRST_BRICK_SKIPPED`；`SUMMARY chunkMiss=0 lightRecalc=0`（重进既有世界纯读回特征）；getblock 读回 id=4 系历史砖，本 run 未新放 |
| 世代定向命中 | `targetGeneration:1` → OK；无字段普通命令 → OK（执行时绑定语义并存） |
| 世代不符拒绝 | `targetGeneration:99` → REJECTED `generation-mismatch expected=99 actual=1`；形态非法 `"1"` → REJECTED `targetGeneration must be integer`（validate 层，tick=0 未入队） |
| 真实切换后旧会话失效 | 存退重进 → `WORLD_SWITCHED … generation=2`；`targetGeneration:1` → REJECTED `expected=1 actual=2`；`targetGeneration:2` → OK |

无头断言同步扩至 36/36（validate 三态 + 世代计数 + 跳过/回归）。下一件按 §5 排序：settime/setspawn（先 javap 钉混淆名）。

## 7. Query 提级：`getinfo` 只读快照（2026-11-13，用户改判先于 settime/setspawn）

**判决理由**：读侧零风险（不动世界、无 NO_CHANGE 语义纠缠）、命令台面板的前置依赖、且取证顺带把写侧名字也钉了。

**取证**（javap `net.minecraft.a.a.f` @ `inf-20100327.jar`，反汇编存档 `%TEMP%\mcaut\world_disasm.txt`）：

| 字段 | 语义 | 证据链 |
|---|---|---|
| `public long b` | worldTime | saveWorld NBT 键 "Time" 绑定；`h()`（restartTimeOfDay）偏移 102-108 每 tick `b+1`；`b%200==0` 触发 `a(Z)V` autosave——三重钉死 |
| `private long x` | randSeed | NBT "RandomSeed" |
| `public int g/h/i` | spawnX/Y/Z | NBT 同名键（g/i 系 FirstBrick 旧钉，h 本轮补齐） |

**实现**：`WorldOps.resolve()` 扩字段探测（NoSuchField→UNSUPPORTED，响亮失败纪律不变）；`getInfo()` 字段直读返回平铺快照 `time=… spawnX=… spawnY=… spawnZ=… seed=…`；`CommandChannel` KNOWN_OPS + runOp 读分支（无坐标校验、无存盘、忽略 save）。

**验证**：无头 39/39（新增 getinfo validate、resolve(Object)→UnsupportedOp 负例、FakeWorld 指纹替身快照断言）。

**实跑验收**（2026-11-13，run `integrations/attic/out/getinfo-1/`，`noFirstBrick=true` 启动，退出 code=0，证据归档 `mca-archive/`）。全绿：

| 断言 | 证据 |
|---|---|
| 零自动写入前置 | `WORLD_CREATED … generation=1` + `FIRST_BRICK_SKIPPED`；`SUMMARY chunkMiss=0 lightRecalc=0`（纯读回特征） |
| 快照字段齐全 | `getinfo` → OK `time=11302 spawnX=0 spawnY=64 spawnZ=0 seed=-4455164984174024073`；spawn x/z=0 与 FirstBrick 锚点同轴 |
| 世代定向并存 | `targetGeneration:1` → OK（detail 与同刻普通命令逐字节一致） |
| worldTime 每 tick +1 | 二次 `getinfo`：time 11302→11708（Δ406）恰等于钩子 tick 计数 724→1130（Δ406） |
| seed 磁盘对账 | level.dat（World2）gzip 解压后偏移 44 命中 NBT 大端 `RandomSeed` = `-4455164984174024073`，与 getinfo 返回一致 |

**连带收益**：settime/setspawn 的混淆名已钉（`b` 与 `g/h/i` 读写同字段），"写 worldTime 后是否需通知面"查证已随 §8 闭环。

## 8. 写侧 `settime` / `setspawn`（2026-11-13，通知面取证闭环 + 落码）

**通知面查证**（全量 javap `-c` 303 类 → `%TEMP%\mcaut\readers\`，跨类引用格式 `net/minecraft/a/a/f.b:J`）：

| 字段 | 全部读点 | 判决 |
|---|---|---|
| `b`（worldTime） | 类内：构造器（NBT 读）、`h()`（+1/tick）、`a(Z)V`（存盘）、**`b(F)F`（昼夜插值）**；类外唯一：`Chunk.a(NBT)` 写 LastUpdate | 渲染入口 `client.a.b.a(lerp)` 每帧调 `world.b(lerp)×360→glRotatef`——太阳旋转实时算**零缓存**，直写字段即生效 |
| `g/h/i`（spawnXYZ） | 类外唯一：`EntityPlayer(World)` 构造器 → `b(DDDFF)` 定位 | 无通知面；**活体纠正**：该读点仅覆盖新玩家路径，重进世界从 `Player.Pos` 恢复——spawn 的活消费路径=死亡重生（见教训 14） |

**实现**：`WorldOps` 新增 `setTime`（返回旧值供回滚）/`setSpawn`/`validateTime`（time≥0，0 为合法起点）；spawn 三 Field 从探测升级为缓存（getInfo 不再逐次 getField）。`CommandChannel` runOp 加非坐标写分支（置于坐标管线之前），`longArg` 承接 time 的 long 域，存盘经 `saveSuffix` 与 place/dig 同语义（默认 true）。

**验证**：无头 45/45（validateTime 负例、setTime 旧值+写入、setSpawn 三字段、getInfo 写后回读互证、两 op validate 放行）。

**实跑验收**（2026-11-13，run `integrations/attic/out/setw-1/`，`noFirstBrick=true` 启动，退出 code=0，`SUMMARY saveWorld=4`（3 命令存盘+1 退出存盘）`chunkMiss=0`，证据归档 `mca-archive/`）。全绿：

| 断言 | 证据 |
|---|---|
| settime 正例 | `time 12993 -> 1000 saved=true`（旧值回滚信息）；getinfo 回读 time=1137 从新值连续推进 |
| **渲染实时性（终极证据）** | 用户目视：天色由夜跳变为白天——零缓存判决被活体证实 |
| settime 失败路径 | `time:-5` → REJECTED `time must be >= 0: -5`；缺参 → REJECTED `arg 'time' must be integer` |
| setspawn 正例 | `spawn set x=10 y=70 z=10 saved=true`；getinfo 回读一致 |
| setspawn 越界 | `y:200` → REJECTED `y out of range [1,127]: 200` |
| 磁盘对账 | level.dat：`Time`=1296（=sp-set 存盘瞬间推算值 1295±tick 差）、`SpawnX/Y/Z`=10/70/10 |
| **setspawn 语义实锤** | 死亡→Respawn 落在 (10,70,10)（用户目视，离旧出生点 14 格）——见教训 14 |
| 世代拒绝（顺带） | 存退重进后 `targetGeneration:2` → REJECTED `expected=2 actual=1`；无字段命令正常 OK |

**语义纠正（用户判决，先于实跑）**：setspawn=**respawn 锚点**，不是玩家当前位置。静态取证"EntityPlayer 构造器读 g/h/i"只覆盖新玩家路径；活体实跑证明重进世界玩家从 level.dat `Player` compound（Pos/Motion/Health/Inventory 独立持久化，dump 实锤）恢复——重进落原位是**正常行为**。消费 spawnXYZ 的活路径=死亡重生。移动当前玩家是 tp 的活，边界不变。

## 9. 教训续（setw-1）

14. **op 语义必须由活体路径定义，不能只靠静态读点**：`g/h/i` 唯一类外读点=EntityPlayer 构造器 ≠ "重进世界会落新出生点"。spawn 的活消费路径是死亡重生；玩家位置另有独立持久化（`Player.Pos`）。
15. **level.dat 键扫描防子串陷阱**：`IndexOf("Time")` 先命中 `DeathTime`；NBT 键匹配须带名长前缀（`00 04 Time`）定位。

## 10. 写侧 `tp`（2026-11-13，取证闭环 + 落码 + 实跑验收 run `tp-1`）

**取证（全量 javap 303 类，%TEMP%\mcaut\readers）**：

- Entity 基类=`net.minecraft.a.c.b`（EntityPlayer=`a.c.e.a` extends `a.c.e` extends `b`）。位置面全 public：`d/e/f`=lastTick、`g/h/i`=pos、`j/k/l`=motion、`m/n`=yaw/pitch；`v`=0.6 宽、`w`=1.8 高、`u`=眼偏移（EntityPlayer 置 1.62f）；`q`=AABB。**`A/B/C` 三个 double 全 jar 零读写点=死字段**，戒律里"同步 prev"实为 lastTick（d/e/f），无 prev 三元组。
- `a(DDD)V`（protected final）=setPosition：写 g/h/i + 以 (x±0.3, y±0.9, z±0.3) 重建 AABB——AABB-first 实锤。
- `b(DDDFF)V`（public final）=setPositionAndRotation：`d=g=x; e=h=y+u; f=i=z; m/n` 重设，末尾调 a()。**公开可用，且正是游戏自身的恢复入口**。
- **配方判决**：游戏 NBT 载入路径 `Entity.d(com.a.a.c)` = 直写 g/h/i←Pos、j/k/l←Motion、m/n←Rotation → 调 `b(g,h,i,m,n)`。tp 逐字节复刻此配方="重进世界恢复位置"同路径，行为可预期。mover 每 tick 自愈重算 `pos.y=AABB.bottom+u−O`（站立稳态：脚=AABB 底、眼=pos.y）。
- y 语义：`b()` 内 h=y+u → **tp 到 (x,y,z) 后脚落 y 平面、眼在 y+1.62**（与载入配方一致，非 bug 是游戏语义）。
- **玩家句柄零新钩子**：`World.c` public 字段（类型 Entity），**EntityPlayer 构造器自设 `world.c=this`**（此前"303 类无写方"系 grep 漏了类名前缀形态 `Field net/minecraft/a/a/f.c:`）。saveWorld 写 Player←`player.c()`（Pos=g/h/i 原值）；读回暂存 `world.y`，`World.a()` 交 `player.d()`。

**落码**：`WorldOps.resolve` 扩展（World.c + Entity b(DDDFF)V + g..n 字段，签名不符→UNSUPPORTED 纪律不变）；`WorldOps.tp`（no-player→REJECTED；直写块中心 x+0.5/z+0.5、清 motion、经 b() 同步 lastTick+AABB；返回旧位供回滚）。`CommandChannel`：KNOWN_OPS+tp；**save 默认 false**（§3 临时态语义，显式 `save=true` 才落盘——与 settime/setspawn 的默认存盘不同）。MCAHooks/MCWorldAgent/MiniJson 零改动。

**无头断言**：53/53（新增 8：validate 放行、no-player 拒绝、旧→新返回串、块中心 +0.5、h=y+u、lastTick 同步、motion 清零、朝向保持；FakeEntity 复刻 b() 语义）。

**实跑验收（2026-11-13，run `integrations/attic/out/tp-1/`，noFirstBrick=true）**：

- WORLD_CREATED@25.3s gen=1（default instance\World2）；gi1 基线 OK。
- **tp1 OK**：(-3.27, 75.37, -6.14) → (15,75,15)，用户目视瞬移确认；负例三连全中：y=200→`y out of range`、缺 z→`arg 'z' must be integer`、x=1.5→`bad-json: float not allowed`。
- **tp5 远距 OK**：→ (12550814, 90, 12550814) 边境之地，用户目视"到了，看到了"；旧位回读 y=73.62=72+1.62（眼高语义活体再证，x 微漂 12550806.9=碰撞修正）。
- **tp6 save=true**：→ (0,64,0)，detail 带 saved=true；level.dat（gzip NBT，`00 03 Pos`+type6+count3 模式）对账 **Pos=[0.30, 65.62, 0.675]**——Pos[1]=64+1.62 磁盘实锤。
- **存退重进存续**：Esc 回标题重进 World2，落 tp 点（非出生锚点 10,70,10）——tp+save 持久化闭环；gi3 OK、time 连续。
- **实例归属疑点闭环**（原 setw-1 记账"存退重进无 WORLD_SWITCHED"）：本次真重进出现 `WORLD_SWITCHED old=@2d96265d new=@dd3cc45 generation=2`——同进程重进=新实例+世代+1（与 cmdchannel-2 一致）。setw-1"无 WORLD_SWITCHED"由用户证言坐实：**当时根本没执行重进**。关窗 code=0，SUMMARY ticks=7841。

## 11. 教训续（tp-1）

16. **需要用户手工操作的验证步骤必须拿到"操作确已执行"的明确回执再判定**：setw-1 的"存退重进无 WORLD_SWITCHED"疑点，根因只是当轮与本轮首次都未真正执行重进——验证语义（"我做了 X"）与操作事实之间要用日志独立复核（WORLD_SWITCHED/SUMMARY 行），不匹配即重跑，不记疑点。
17. **tp 眼高语义（0327）**：`b(DDDFF)` 内 h=y+u(1.62)，tp 到 (x,y,z) 后脚落 y 平面、眼 y+1.62，level.dat `Player.Pos[1]` 即存眼高——对账时 Pos[1]−1.62=tp y，勿按"Pos[1]≠y 即写歪"误判。
