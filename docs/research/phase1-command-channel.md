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

命令通道 v1（place/dig/getblock/save 四 op）验收完毕。§5 判决的 P0 fixture 化（`noFirstBrick=`）与 P1 世界世代（`capturedWorldGeneration` + 信封 `targetGeneration`）已落码并实跑验收（2026-11-13，run `p0p1-gen`，见 §6）。Query `getinfo` 已落码并实跑验收（2026-11-13，run `getinfo-1`，见 §7）。写侧 `settime`/`setspawn` 已落码并实跑验收（2026-11-13，run `setw-1`，见 §8；setspawn 语义经用户判决纠正为 respawn 锚点）。写侧 `tp` 已落码并实跑验收（2026-11-13，run `tp-1`，见 §10：目视瞬移+边境之地远距+Pos[1]=y+1.62 磁盘对账+存退重进落 tp 点；setw-1 遗留的"实例归属"疑点闭环——当轮未执行重进，见教训 16）。批量写第二批 `fill`/`getblocks` + 顺手钉 `getplayer` 已落码并实跑验收（2026-11-13，run `fill-1`，见 §12：NO_CHANGE 协议 v2 首个活体实例、changed=220 同 id 短路实锤、存退重进 rle=1:324 持久化、FILL_MS 243 块含 save=9ms）。元数据批 `getmeta`/`setmeta`/`genscatter`/`listops` 已落码并实跑验收（2026-11-14，run `meta-1`+修复复验 `meta-2`，见 §13/§14：difficulty 对账、setmeta nibble 持久化、火把目视转向、散布正例计数精确、NO_CHANGE；含草地→石头基底翻案与预扫盒上界修正两条活体翻案）。待办：桌面壳"命令台+事件流"底部面板（VS Code 式，速记→封包翻译器）为壳 Phase 2 首件；mca/ 证据已归档各 run `mca-archive/`。

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

## 12. 批量写读 `fill` / `getblocks` + 快照 `getplayer`（2026-11-13，取证闭环 + 落码，实跑待验收）

**取证（readers 反汇编，%TEMP%\mcaut\readers）**：

- **写侧双路径实锤**：`World.a(IIII)Z`（setTileNoUpdate）=边界检查→`Chunk.a(IIII)Z`，零观察者零光照；`c(IIII)Z`（setBlockWithNotify）=`a(IIII)Z` ∧ 遍历 `World.u` 观察者 `a(III)V`（渲染标记）∧ `e(IIII)V`（6 邻格各一次 `i()`→`Block.b(world,x,y,z)`=updateLighting）。
- **案卷预告被推翻**：`Chunk.a(IIII)Z` 内部已含同 id 短路、旧块 prepareToRemove、heightmap 重算（`g()`→`f()`→**world.d 观察者区间通知**）、tile 移除钩、新块 onPlace、`d=true`（isModified→存盘）。"快路径（setTileNoUpdate+脏区标记）"实际只省 6 邻光照+逐块观察者标记——**省不出数量级**。判决：v1 fill=循环 `c(IIII)Z`（place/dig 同路径，正确性优先），快路径留实测后候选。
- **tick 预算**：cap=4096 块（16³），超限 REJECTED `volume-too-large`；游戏线程 `System.nanoTime()` 计时，`FILL_MS`/`GETBLOCKS_MS` 入 agent 日志——fill-1 实测数据回写后再议上调或快路径。
- **getblocks 读配方**：循环 `World.a(III)I`（getBlockId，游戏同语义）。副作用实锤：provider `a.a.c.a.b(II)` 对未加载坐标**就地生成整 chunk**（`Random.setSeed`+32768 byte[]+4×4 噪声网格全套）——读越界=生成卡顿面，cap 即限幅，不另设加载检查（v1 与游戏同语义）。
- **Health 实名**：`EntityLiving(a.c.e).W` public int，NBT `"Health"` save/load 双向绑定实锤，默认 10（无键时写 10）；顺钉 `Y`=HurtTime、`ab`=DeathTime、`ac`=AttackTime（v1 只取 Health）。`W` 在子类而 `World.c` 声明类型=Entity 基类→getplayer 按实例类惰性 `getField("W")`，失败→UnsupportedOp（execute 的 runOp 段扩捕 UnsupportedOp→UNSUPPORTED，不静默）。
- **NO_CHANGE（协议 v2 终态，§5 判决"全采"落地）**：仅 outbox 侧新增终态字符串，封包格式与 `protocolVersion=1` 校验不动（v1 归档账目不动）；唯一生产者=fill changed==0（`WorldOps.NoChange` 信号异常，message=完整 detail，save 跳过——磁盘态未动）。

**落码**：`WorldOps`：`MAX_REGION_VOLUME=4096`、`normalizeRegion`（两角 validateCoords+min/max 归一+long 乘法防溢出）、`fill`、`getblocks`（RLE 遍历序 y→x→z，run=`id:count` 竖线分隔）、`getPlayer`（g/h/i+j/k/l+m/n+W）。`CommandChannel`：KNOWN_OPS+三 op（→11）；runOp 三分支（fill 写侧默认 save=true，读侧忽略）；execute 扩捕 NoChange→NO_CHANGE、UnsupportedOp→UNSUPPORTED；FILL_MS/GETBLOCKS_MS 计时日志。MCAHooks/MCWorldAgent/MiniJson/FirstBrick 零改动。

**无头断言**：65/65（53 旧+12 新：三 op validate 放行、反角归一化、4096 恰好放行、4352 拒绝、y=0 角点拒绝、fill changed 计数、零变更 NoChange、RLE 单 run+维度串、getplayer 全字段快照、no-player 拒绝；FakeWorld 写/读返回值可配置面+FakeEntity 带 W）。

**实跑 run `fill-1`（2026-11-13，全项通过，退出码 0，mca-archive/ 归档）**：

- 基线：`gi1` OK；`gp1` OK——**health=18 活体实锤**（W 实名：上轮重生扣血值，非默认 10）。
- 正向：`fill0` 石砖地基 9×1×9 changed=81；`fill1` 圆石 9×3×9 **changed=220**（243 中 23 块原地形已是圆石——同 id 短路实锤，与 Chunk.setTile 取证一致）；`gb1` 读回 `getblocks 9x4x9 rle=4:81|1:243`（81+243=324 精确）；目视通过（用户确认平台可见）。
- **NO_CHANGE 协议 v2 首个活体实例**：`fill2` 同区域同 id → `status=NO_CHANGE, changed=0`，save 跳过。
- 失败路径：`fill3` 4352 块 → REJECTED `volume-too-large: 4352 > 4096`；`fill4` y2=200 → REJECTED `y out of range [1,127]: 200`。
- 批量挖除：`fill5` blockId=0 changed=324 → `gb5` `rle=0:324`（dig 语义批量成立）。
- 持久化：`fill6` 复原圆石 changed=324 saved=true（**7ms**）→ 存退重进（WORLD_SWITCHED gen=2，新实例）→ `gb6` **`rle=1:324` 原样读回**；`gp2` health=20（重生后自然回血）、位置恢复落平台。
- **tick 预算数据**：FILL_MS 243 块含 save=9ms、324 块含 save=7ms，其余亚毫秒（ms=0）；GETBLOCKS_MS 324 块读=0ms——cap 4096 内单 tick 无压力，快路径（v2 候选）暂无必要性证据。
- 发令戒律补记：`powershell -File` 传含引号/逗号的 JSON 参数会被剥离破坏（bad-json @54 两连 REJECTED）——实跑发令改用内嵌 JSON 的 cmds.txt 批处理（tmp→inbox 同卷 rename，UTF8 无 BOM 不变）。

## 13. 元数据批 `getmeta` / `setmeta` / `genscatter` / `listops`（2026-11-14，取证翻案 + 落码，实跑待验收）

Spec：`.trae/specs/meta-tree/`。判决变更：原批 gentree **砍除**——复核翻案三条：

1. **`World.f(III)I` = getBrightness，非气温**（旧笔记作废）：`Chunk.c(IIII)I` 读 h=SkyLight 减环境值（World.q）再与 i=BlockLight 取 max；`g(III)F` 只是亮度经 `t[]` 曲线查表。BlockSapling.updateTick 首行 `world.f(x,y+1,z)>=9` 是**光照门槛**。
2. **0327 sapling 不长树**：meta<15 → `b(IIII)V` 静默 meta+1（20% 概率、`nextInt(5)==0`）；meta≥15 → `setTileNoUpdate(0)` 再 `setTileNoUpdate(this.blockID=6)`——原地重放，幂等。树结构生成在 0327 不存在。
3. **`WorldGenTrees`（`a.a.c.b.b`）名不副实**：椭球曲线散布器（中心 `(x+8±2sinθ, y+2..4, z+8±2cosθ)` 16 步，静默 `a(IIII)Z` 写构造器 id，恒返 true）。~~命中格仅当现为 grass~~ **meta-1 实跑翻案**：基底检查=`x.i=r(1,1)`=**stone id 1**（`x(int,Material)` 构造器 `c[id]=this` 实锤）——该类实为**原版矿石团生成器**（地形生成 `a.a.c.a` 以 id 14/15/16/aj 调用它撒金/铁/煤/aj 矿），0327 无原生花草散布路径（树=内联死类 `b.c`）。**换 genscatter** 调此真生成器（0327 唯一原生"结构生成"，语义=石头基底散布任意 blockId）。

取证钉面（javap dump + 可读树双向对榫）：

- `d(III)I`=getBlockMetadata（Chunk.b(III)I 读 nibble g，索引 `x<<11|z<<7|y`）；`b(IIII)V`=`h(IIII)Z`→`Chunk.b(IIII)V` 仅 nibble+dirty **零通知实锤**——可读树名 `setBlockMetadataWithNotify` 系 deobf 误猜。
- setmeta 通知复刻（对照 `c(IIII)Z` L699 配方）：观察者 `World.u`（private List）逐个 `a(III)V` + `e(IIII)V`=6 邻各一次 `i()`→`Block.b(f,III)`=updateLighting（第 4 参 id 被 i 忽略）。同 id 走 c() 被 Chunk 短路返 false 不通知——必须手动复刻。观察者方法按实例类惰性 getMethod（接口 `a.a.a` 名不硬编码，getplayer W 同模式）。
- genscatter 运行时补通知：游戏写=静默（地形期无渲染可通知），已加载 chunk 上直调会留脏渲染——预扫影响盒~~[x+6..x+10]×[y+2..y+4]×[z+6..z+10]（75 格上界）~~ **meta-1 实跑修正为真实上界 [x+4..x+12]×[y..y+5]×[z+4..z+12]（486 格）**：中心线盒只覆盖曲线中心，椭球半径 `(sin+1)*rnd+1∈[1,3)` 半轴 + `(int)` 截断向零使单元循环外扩至 ±1~2 格（活体实锤：meta-1 gs6 changed=11 但读回盒外尚存 1 花=旧盒漏计漏通知）→复扫 diff→变更格逐个通知；changed=0→NO_CHANGE（生产者扩为 {fill, genscatter}）。可选 seed 参数=确定性散布。
- 难度取证归档（用户判决：GUI 已覆盖，**不开 setdiff**，只补进 getinfo）：`World.d:I`=difficultySetting，真源=`GameSettings.difficulty`（options.txt，默认 2，`&3` 值域 0-3），主循环每帧强制同步（Minecraft L763）；消费面=peaceful 回血+伤害缩放（0→清零/1→/3+1/3→×3/2）+peaceful 杀怪；**客户端全局不进 level.dat**。

盘点更正（后续批次候选）：零新取证=getlight（`f(III)I` 亮度读）+setlook/setmotion（Entity m/n、j/k/l 直写，tp 已证全链）；需定向取证=物品栏 give/inv（InventoryPlayer 混淆类未定位）。天气 0327 不存在。

- 落码：WorldOps（resolve 扩 5 面 + getMeta/setMeta/genScatter/getInfo difficulty）、CommandChannel（KNOWN_OPS→15 + 四分支 + LISTOPS 静态表）。钩子面零改动。
- 无头回归：MiniJsonTest **迁 package mcaut**（假生成器需以真 FQCN `net.minecraft.a.a.c.b.b` 上 classpath，默认包类不可被命名包引用）——**79/79 全绿**（65 旧+14 新：四 op validate 放行、setmeta 往返+通知计数、meta 越界/坐标拒绝、genscatter 正路径 changed=2+逐格通知+seed 回显、无草地 NO_CHANGE、id 越界拒绝）。跑法：`javac -encoding UTF-8 -cp java\agent\out\classes -d %TEMP%\mcaut\out %TEMP%\mcaut\mcaut\MiniJsonTest.java %TEMP%\mcaut\net\minecraft\a\a\c\b\b.java`；`java -cp "java\agent\out\classes;%TEMP%\mcaut\out" mcaut.MiniJsonTest`。

## 14. 元数据批实跑（2026-11-14，run `meta-1` + 修复复验 `meta-2`）

**run meta-1**（`integrations/attic/out/meta-1/`，`noFirstBrick=true`，用户手工存退重进经 WORLD_SWITCHED gen=2 日志独立复核）——除 genscatter 外全绿，但**活体翻案两条**：

| 断言 | 证据 |
|---|---|
| getinfo difficulty 对账 | `difficulty=0` ↔ gameDir options.txt `difficulty:0`；seed/spawn 与历史 run 一致（setw-1 的 spawn=10/70/10 持久存续） |
| listops 自表 | 15 op 全列出 |
| place 火把+getmeta | `placed id=50`→meta=0（该格 nibble 原值即 0；place 经 `c(IIII)Z` **不动 meta 面**，与 meta-2 残值 5 互证） |
| setmeta 转向+读回 | `meta 0 -> 1`→getmeta=1 |
| setmeta 越界拒绝 | `meta:16` → REJECTED `meta out of range [0,15]: 16` |
| **setmeta nibble 持久化** | 存退重进（WORLD_SWITCHED gen=2）后 getmeta=1——Chunk nibble 段随 chunk 存盘读回实锤 |
| genscatter 草地前提**作废** | 花盒 4 个 seed 全 NO_CHANGE；扩盒读回仍无花→静态复核翻案（§13 条目 3：基底=stone 非 grass） |
| **旧预扫盒漏计实锤** | 对准石头层 seed=11→changed=11，但中心线盒读回 0 花（全被游戏弹出）、扩盒盒外存活 1 花=**写在旧 75 格外**（漏计+漏通知） |

**修复**（jar 重建+无头 79/79 复跑）：genScatter 预扫盒改真实上界 `[x+4..x+12]×[y..y+5]×[z+4..z+12]`（486 格，§13 通知条目）。

**run meta-2**（`integrations/attic/out/meta-2/`，修复版 jar，全绿）：

| 断言 | 证据 |
|---|---|
| 正例计数精确 | 石头层 seed=24/25 → changed=4+3=7；扩盒读回**恰 7 个 id=4**（RLE 逐段核对），上界完整覆盖实锤 |
| NO_CHANGE（石头语义） | 平台上方盒先 getblocks 探明无 stone（3/2/4/0 混合）→ scatter → NO_CHANGE changed=0 |
| 火把目视转向 | place (-2,71,-1) 立于草地 id=2→setmeta **旧值=5**（该格历史残留 nibble——place 不动 meta 面，与 meta-1 gm1=0 互证）→meta=1 用户目视**倾斜火把可见**=通知复刻生效 |
| 存退重进持久化 | WORLD_SWITCHED gen=2 后散布 7 格 RLE 与退出前**逐字节一致**；火把 id=50+meta=1 双持久 |

## 15. 教训续（meta-1/2）

17. **静态取证必须给"活体反例"留复核位**：散布器基底 grass→stone 翻案源于 4 个 seed 零命中的"概率不信"——若满足于 NO_CHANGE 字面语义就漏掉；复核路径=反汇编比对字段真值（`x.i=r(1,1)`+构造器 `c[id]=this` 双钉）。同理"影响盒上界"只算了曲线中心，漏了椭球半径外扩——**写侧上界=中心线±半径半轴+截断向零**，活体读回盒外残花是免费实锤。
18. **无支撑装饰会被游戏随机 tick 弹出**：花(37)写进石头 15 tick 内 10/11 消失、悬空火把(50)跨存退消失——0327 装饰块有自毁 tick；且 `setTile(0)` 不清 nibble（弹出后 getmeta 仍读回旧值=meta 持久化验证的意外红利）。验收写侧 op 用**稳定块**（圆石 4），装饰块只做通知/朝向目视。
19. **同坐标重放 seed 会因基底被改而变果**：meta-1 用过的石头层 seed=11 写 11 花，弹出后变 11 空气格——meta-2 同区换 seed 才正例。散布类 op 的"确定性"仅在基底未动时成立。

## 16. Populate 触发链取证（2026-11-14，用户线索"TerrainPopulated"）

用户线索（归属已更正）：`TerrainPopulated` 标签系 **Infdev 20100330（0327 的下一个构建）** 才引入——chunk NBT 有此后"为假则调一次 populate"，取代 0327 的裸重放。**0327 实跑取证：无此标签**——全 jar 零 "Terrain" 字符串；Chunk NBT 键全集=xPos/zPos/LastUpdate/Blocks/Data/SkyLight/BlockLight/HeightMap/TileEntities（`e_d.a/b(NBT)` 双向）；Chunk 类亦无 populated 布尔（仅 `d`=dirty）。

0327 的等价护栏是另一套（`e_c.b(II)`=getOrCreateChunk 链，全 jar 唯一 populate 触发点，javap 实锤）：

- **缓存命中直接返回零副作用**；未命中→磁盘载入 `d(II)` 或就地生成 `provider.b(II)`，随后按 4 个对角各查"三邻在场"（如 `a(x+1,z+1)&&a(x,z+1)&&a(x+1,z)`）→ `a(this,x,z)` 接口转发 → `c_a.a(e.a,II)`=populate。**从盘载入的 chunk 同样走此链**——非"一生一次"，而是"每次出缓存重取即重跑"。
- populate 体首条指令=`Random.setSeed(x*318279123 + z*919871212)`（**纯坐标派生，世界 seed 被覆写不参与**；零标志检查）——矿石散布对 chunk 坐标**确定性幂等**：重跑同 seed、同石头基底→写同位，不重复生矿。0327 没有 TerrainPopulated，靠确定性本身当幂等锁。
- 4 段散布首段取 `x.x.ar`=煤 id 16（余为金 14/铁 15/aj），尾随 `b.c`=树空壳——与 §13 翻案一致。

**活体对账**：meta-2 存退重进散布 7 格 RLE 逐字节一致=重进触发重 populate 亦无污染（同位重写 or 我方圆石已替换石头→游戏矿 seed 跳过节余格）。教训 19 获佐证：游戏自身 populate 同样"确定性仅在基底未动时成立"。**对 agent 的含义**：genscatter 非"触发游戏生成"而是同函数手动重放；手动补通知仍必需——游戏 populate 发生在 chunk 挂载时刻（整块随后上传渲染），我们在运行时改已加载 chunk。

## 17. 亮度/朝向/速度批 `getlight` / `setlook` / `setmotion`（2026-11-14，零新取证落码，实跑 `llm-1` 全绿）

**取证**：本批三面全部落在已钉面，零新 javap——`World.f(III)I`=getBrightness（meta 轮翻案钉死，§13：Chunk.c(IIII)I 读 h=SkyLight 减环境值 `World.q` 后与 i=BlockLight 取 max，纯读 0-15）；Entity `m/n`=yaw/pitch、`j/k/l`=motion、玩家句柄 `World.c`（tp 轮全字段，§10）。

**协议判决**（MiniJson 严格子集拒浮点）：
- `setlook(yaw,pitch)`：**整数角度**，yaw∈[0,360)、pitch∈[-90,90]，越界 REJECTED；直写字段即生效（零通知面——tp 轮 `b(DDDFF)` 设 m/n 渲染/瞄准直读同证）。
- `setmotion(vx,vy,vz)`：**整数千分位**，|v|≤10000（=±10 格/tick），直写 `j/k/l = v/1000`；旧值按千分位回显供回滚。
- **纠正**：spec 表曾记 `j/k/l` 为 "public float"——活体真值 **double**（tp 轮清速度 `setDouble` 全绿；若 float，`Field.setDouble` 必抛 IllegalArgumentException）。以代码为准。

**持久化**：Player NBT `Rotation`/`Motion` 随 saveWorld 落盘（setw-1/tp-1 已证 Player compound 独立持久化）；写侧默认 save=true。零新终态（直写字段恒 OK，无 NO_CHANGE 生产者新增）。

**验证**：build 零错；无头 **95/95**（+16 断言：FakeWorld 补 `f(III)I` 面；getlight 回显/坐标校验；setlook 旧→新、yaw 360/-1 与 pitch ±91 拒绝、拒绝不改值、边界 yaw=0/pitch=90 接受；setmotion 千分位转换、±10001 拒绝、no-player、边界 ±10000 接受）。

**实跑 `llm-1`（2026-11-14 批准，World2 活体，全绿）**：
- getlight：地表头顶 `light=15`、地下 y=5 `light=0`（st1 先置 time=6000 保白天）✓
- setlook：`yaw 11384.945 -> 180 pitch 90 -> 0` 目视转身 + getplayer 回读 180/0 ✓；`yaw=360` → REJECTED `yaw-out-of-range` ✓
- setmotion：`vy=20000` → REJECTED ✓；`vy=+5000` **站方块写速度不被 mover 首 tick 清零**（疑点定案）——上抛 y 71→130→151，motY 5→2.647→1.431 重力衰减自洽；副作用=落差 ~84 格**摔死**（和平难度照摔），教训：上抛量级 ±0.5-1.0 格/tick 足够，5 格/tick 是致死弹道
- 死亡重生语义（活体新钉）：respawn=新 EntityPlayer 构造器路径，**不读 NBT Rotation**（yaw 归 0）、位置=spawnXYZ（本图出生点在水面→溺亡二连，用户目视确认）
- 持久化：setlook 270/45+save → 存退重进（WORLD_SWITCHED gen=2/3）→ getplayer 回读原始累积值 6164.3955/90（活体 yaw 是累积浮点，非归一化）→ agent save → **盘面 `Player/Rotation=list<5>[6164.3955, 90.0]`、`Pos=list<6>[-1.2603, 72.62, -1.7]`（眼高语义）、`Motion=list<6>[0, -0.08, 0]` 逐项一致** ✓

**NBT 新钉事实（llm-1 盘面取证顺带）**：
1. **0327 的 `Pos`/`Motion`/`Rotation` 是 `TAG_List`（type 9，元素 6/6/5），不是现代格式的 double/float 数组（tag 11/12）**——Entity.c 用 `a:([D)Lcom/a/a/f`（f=ListTag，getTypeId=9）。盘面对账工具按数组读会静默漏掉。
2. **saveWorld 的 Player compound 是条件写**：`getfield c(player) / ifnull 124`——`World.c==null` 时整段 Player 不落盘。游戏**载入期即调一次 saveWorld**（更新 LastPlayed/SizeOnDisk，此刻玩家尚未构造）→ level.dat 短暂退化为 125B 无 Player；当次会话位置靠内存 `World.y`（load 时读入的 Player NBT）恢复，不受影响。风险窗口：载入后、首次带玩家存盘前被强杀 → 盘面 Player 丢失，下次进世界=出生点（教训 1 家族新变体=**存盘时序窗**）。agent 会话内 save op 恒有 c≠null，安全。

## 18. 物品栏批 `give` / `inv` / `sethealth`（2026-11-14，javap 全量反汇编取证 + 落码，实跑 `give-1` 待批准）

**取证**（spec [.trae/specs/give-inv-health/spec.md](../../.trae/specs/give-inv-health/spec.md) §1 全表实锤）：类链 Entity=`a.c.b`←EntityLiving=`a.c.e`←EntityHuman=`a.c.e.a`←EntityPlayer=`client.g.a`（单人不另设类；`a.c.c`=Mob 非玩家，不 shadow 本批字段）。物品栏=EntityHuman `public b`=InventoryPlayer(`a.c.e.b`)：`a[36]` 主槽/`b[4]` 盔甲/`c` 当前槽/`a(ItemStack)Z`=addStack（同 id 且 dmg==0 才合并、单栈 64、false=装不下）。ItemStack=`net.minecraft.a.b.k`（a=Count/c=id/d=Damage；ctor `(II)`/`(III)`）；注册表 `i.b[]` 按 id 直索引含 null 空洞——**give 必须注册表校验**（游戏侧 `a()Li;=i.b[c]` 直索引，放行坏 id=后续 NPE）。持久化：EntityPlayer `a(NBT)` 写 "Inventory"（主槽 Slot 0..35、盔甲 100..103）→save 即落盘。

**sethealth 判决**：直写 `W` 绕过 `b(I)V` 治疗钳制（20 上限；W<=0 不治疗）→op 侧自校验 0..20 **拒绝而非截断**；HUD `client.a_d` 每帧直读 W→即时生效零通知面。置 0=原版死亡流程接管（deathTicks>20→`k()`=die 玩家覆写空操作→`client.d` 弹死亡屏→按钮重生重建玩家 W=20）——**agent 不代打 respawn**；持久化 0 后重进=载入 Health=0 再次死亡屏（原版语义，不特殊处理）。

**实现**：三 op 全惰性解析（eHealth 同模式：execute 期按实例类解析，NoSuch*→UNSUPPORTED 不静默），`resolve(Class)` 主段零改动；ItemStack 类经主槽数组**分量类型**取得（零硬编码类名）。协议沿用整数子集：`give(itemId,count[,dmg 缺省 0])`（"缺键=默认"面同 genscatter.seed）、`inv` 纯读、`sethealth(0..20)`；give/sethealth 写侧 saveSuffix，inv 忽略 save；KNOWN_OPS→21。零新终态。

**验证**：build 零错；无头 **114/114**（+19 断言：FakeEntity 补 `b`=FakeInv{a[36]/b[4]/c/addStack 替身}、FakeStack 双构造器；**假注册表类 `net.minecraft.a.b.i` 真 FQCN 上测试 classpath**（假散布器同法），含 null 空洞槽——覆盖越界 999/洞 id 2/未注册 0 三类 bad-itemid；give 正常/合并/dmg 路径/满包 inventory-full/越界不改值；inv empty/混合槽对账 `0=64x5 3=1x35 d2 A0=1x298 cur=2`；sethealth 旧值回显、0/20 边界、-1/21 拒绝不截断、no-player）。

**实跑 `give-1` 已完成全绿**（2026-11-14，World2，difficulty=0；流水归档 [cmds.txt](../../integrations/attic/out/give-1/cmds.txt)）：listops 21 项生效；give 55 x1/x64（用户指定齿轮 id 替换木板方案）、bad-itemid(999)/count 越界(0) 拒绝无副作用；工具 256-260 全注册、dmg=10 耐久放行→inv ` d10` 回读→**存退重进后 d10 仍在**（Damage 持久化闭环）；sethealth 10→HUD 半心、21 拒绝、置 0→**死亡屏弹出目视确认→点 Respawn→health=20 出生点**；满包 36 槽灌满后第 37 项 `inventory-full`；level.dat 盘面 NBT 与内存逐项一致（Slot 0..35/id/Count/Damage）。

**实跑新钉四条**：
1. **真实 addStack 会分栈**：gi2 x64 进 1x55 栈→`0=64x55 1=1x55`（先补满旧栈、余量开新槽）——无头假类是简化版（装不下即 false 不分栈），WorldOps 只检查 boolean 返回值，不受影响。
2. **和平回血活体实锤**：difficulty=0 下 10→20 约 10 秒（每 20 tick b(1)）；重进后 health=20 是回血而非载入面缺陷（退前已回满并存盘）。
3. **死亡重生清空物品栏**：Respawn=重建 EntityPlayer（iv4=empty），0327 无 keep-inventory；新实例经 `World.c` live 读自动生效，零钩补。
4. **0327 level.dat 根 compound 无 `Level` 包装**（Player/Time/SizeOnDisk 直接在根下，与后世版本格式不同）——临时 node dumper 在 chunk Data 区解析翻车，但 Player/Inventory 区原始字节直读完成对账。

## 19. 实体面批 `explode` / `spawn` / `drop`（2026-11-14，源码树+javap 双路取证 + 落码，实跑 `sed-1` 待批准）

**取证**（spec [.trae/specs/spawn-explode-drop/spec.md](../../.trae/specs/spawn-explode-drop/spec.md) §1）：本批启用**反编译源码树** `E:\4k\github-projects\minecraft-verification-analysis\src-minecraft\src-minecraft-infdev-20100327`（234 文件全语义名）做语义面，混淆实名仍 javap 指纹钉——双路交叉。入口：World `a(Entity,DDDF)V`=createExplosion（Explosion 类=空标记，16³ 壳射线+方块阻力采样全在 World 方法内；source=null 判等实现零 NPE 但**伤害不豁免玩家**）；World `a(Entity)V`=spawnEntityInWorld=loadedEntityList.add（主循环每 tick 遍历=加入即活）；Entity(World) 构造零副作用（仅 worldObj+pos(0,0,0)）。**混淆实体类表=字符串常量判决法**（贴图路径/实体名/getEntityName 串实锤）：pig=`a.c.b.c`、sheep=`a.c.b.b`、creeper=`a.c.a.d`、skeleton=`a.c.a.c`、zombie=`a.c.a.f`、spider=`a.c.a.b`（六 mob `(World)V`）、tnt=`a.c.c.a`（`(World,FFF)V` fuse=80 内置）、item=`a.c.c.b`（`(World,DDD,ItemStack)V`）；giant 无 `(World)V` 构造器排除；层级注记：混淆包重排致旧判读偏差（`a.c.c`=EntityCreature 非 Mob、`a.c.a.e`=EntityMonster）——不影响本批（只依赖具体类构造器+Entity 基类面）。**0327 saveWorld 不写实体**（Infdev 特性）→spawn/drop=瞬态（save 忽略、detail 无 saved= 防误导），explode 破坏方块持久。tileentity 面（Furnace=`a.a.b.a.b`/Chest=`a.a.b.a.c`/TE 基=`a.a.b.a.a`，World `h(III)`/`a(III+TE)V`/`i(III)` 三件套已钉）留下批。

**实现**：三 op 全惰性解析（resolveEntity：explode/spawnEntity 两 Method + kind→Constructor 缓存表；Entity 基类=player 字段声明类型、ItemStack 类=主槽分量类型，零硬编码 FQCN 纪律延伸）；`spawn`=白名单查表→Class.forName→构造→setPosRot(x+0.5,y,z+0.5,0,0)（tnt 特例 float 构造后同调用无害幂等）→spawnEntityInWorld；`drop` 校验复用 give 面同序同串（no-player→count→dmg→bad-itemid）+isCtor2/3 复用；`explode` size∈[0,10] 整数（invoke 转 float）。KNOWN_OPS→**24**；explode 写侧 saveSuffix，spawn/drop 瞬态忽略 save。零新终态。

**验证**：build 零错；无头 **125/125**（+11 断言：假实体八类**真混淆 FQCN 上测试 classpath**（假散布器/假注册表同法，extends 去 final 的 FakeEntity）+FakeWorld 补 `a(Entity)V`/`a(Entity,DDDF)V` 记录面；用例=explode null-source 参数记录+size 0/10/-1/11 边界、spawn 七 kind 构造+落位（g=x+0.5、**h=y+1.62 眼偏移经 setPosRot**）+世界表+bad-kind(giant)、drop EntityItem 收 ItemStack（id/count/dmg 对账）+瞬态拒绝不改表）。无头两处翻案注记：①FakeEntity.b 复刻眼偏移致 `h=64+1.62f` 浮点精度陷阱（断言容差 1e-5）；②EntityItem 替身须复刻构造内 setPosition（g/h/i 直写无眼偏移，与 spawn 经 setPosRot 的路径不同）。

**实跑 `sed-1` 已完成全绿**（2026-11-14，World2，Easy=启动前 options.txt difficulty:1；流水归档 [cmds.txt](../../integrations/attic/out/sed-1/cmds.txt)）：listops 25 项生效；spawn 七 kind 上台活体目视（pig/sheep 游走、敌对四怪围攻而玩家**零掉血**=invulnerable 免疫实锤、tnt fuse 80 tick 自爆掀台 24 块）；giant bad-kind/size=11/ticks=-1 全 REJECTED 无副作用；explode size=4 定向爆台 25 块破坏，**存退重进 gb 逐项一致=破坏持久**；**实体存退即失**（重进目视台净）；drop 齿轮落地旋转目视。

**实跑新钉五条**：
1. **水面筑台=命令通道自造地面**：World2 出生区为海洋（西 1792 全水扫描实锤），玩家反复落水溺亡——`fill` 筑石台（49 块 changed=49）后 spawn/tp/爆测全部上台进行，"找地面"从地形问题降维成 op 组合拳。
2. **实体碰撞盒推人**：六怪围台把用户推下水（活体物理交互，spawn 的敌对不止是观赏物——实跑站位戒律加一条）。
3. **Easy 夜间自然刷怪围攻**：重进后 health=2（iframes 14=持续重置中）——难度切换前提闭环（和平=怪即死+不刷，Easy=刷怪+可打），invulnerable 的实战价值当场验证。
4. **水下爆炸仍破坏方块**：tnt/explode 台实际在水位线下，破坏照常（0327 爆炸无防水判决）。
5. **i-frame 瞬态钉版**：存退重进后 `iframes 0 -> 30000`（新实例 K=0=heartsLife 不进 Player NBT）；同会话内 K 跨 tick 连续自减（28017 读数=非重进，曾短暂疑持久，重进实验排除）。

**批前增量（用户判决"玩家不死先"+Giant 质询，2026-11-14）**：
1. **新 op `invulnerable(ticks)`（KNOWN_OPS→25）**：Entity `K`=heartsLife（public int，原版 i-frame 计时器）直写——`EntityPlayer.attackEntityFrom` L188 `K>heartsHalvesLife/2(=10) → return false` **拒绝全部伤害源**（伤害入口全搜=单入口实锤：怪物近战/箭 EntityArrow L127/火焰 EntityLiving L95/坠落 L300/虚空 Entity L150/157/346/爆炸同路）；每 tick 自减 1（L112）→限时免疫语义（≥11 生效、0 取消、32767≈27 分钟）；副作用=HUD 心闪烁（GuiIngame L47 读同字段，仅视觉）；不进 NBT=瞬态。无头 +4=**129/129**。
2. **和平抹杀敌对实锤**：`EntityMonster.onUpdate` L27 `difficultySetting==0 → setEntityDead()`——和平下 spawn 的 creeper/skeleton/zombie/spider **首 tick 即死**（pig/sheep/tnt 不受影响）；`World.d` 每帧被 `gameSettings.difficulty` 覆写（Minecraft L763）→**写 d 无效**，切换=启动前 `<gameDir>\options.txt difficulty:0→1`（Easy，GameSettings L142 读入）——sed-1 前置。
3. **源码树地位翻案（用户实锤）**：`src-minecraft-infdev-20100327` 为**补构造器过编译的改装 build**（EntityGiant(World) 即后补；运行时 jar `a.c.a.a` 仅默认无参构造，javap 全量复核）——判决=语义面可用（本批 i-frame/抹杀/入口结论全部经 javap 反汇编交叉验证），**结构面（构造器/字段存在性）一律以运行时 jar 为准**。Giant 维持排除不测（白名单外 bad-kind 上游拦截+强构造 worldObj=null NPE，无信息增量）。
