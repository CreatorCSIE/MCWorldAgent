# 设计案卷 — Agent 外部命令通道（载体对比与协议草案）

日期：2026-11-13 ｜ 状态：**已按外部评审提案修订（v2），载体 A 待最终拍板** ｜ 前置：[phase0](../research/phase0-agent-poc.md) §10、[phase1](../research/phase1-first-brick.md)、[ingame-console](ingame-console.md)

## 1. 需求与决策背景

第一块砖的 `submit()` 由 WORLD_CREATED 自动触发，是一次性演示。主线（用户拍板）：
**新增命令操作面**（挖除、时间、出生点、传送、批量写…），通道第一个客户是 **CLI**（人工/脚本可直发），
桌面壳与游戏内控制台（[ingame-console](ingame-console.md)）是后续客户；桌面壳 Workbench 模型与分层排期判决见 [workbench-model](workbench-model.md)（2026-11-14）。

硬约束（继承既有纪律）：

1. **tick 边界执行**：外部命令一律经 `MCAHooks.submit()` 队列，游戏线程 drain。
2. **命令执行面零注入 + 受限构造器补全**：命令执行体走反射，不扩织入面。（v2 注：轮询线程是 agent 侧普通线程，非织入钩，不违此律。**NEW_CTOR 豁免**=指纹表 `expectedHits:0`（类在但 `<init>` 被混淆管线剥离的死代码实体类）才触发四指令构造器补全（先 super 合法序列，无分支零 StackMapTable 新增），父链缺失→VerifyError→spawn UNSUPPORTED 优雅降级，jar 零改动——首例 Giant 复活（giant-1 实跑全绿），泛化背景=0413/0414 全生物构造器丢失+**0227/0313 更严峻（实体功能被大砍，版本扩展批调研）**的跨版本复活能力，见 [phase1](../research/phase1-command-channel.md) §20。**EXIT 观察钩**（hud-overlay 批首例，phase1 §23）=RETURN 前注入观察回调（GuiIngame.renderGameOverlay→onHud 绘 XYZ+javaagent 行，showFPS 门、零 GL 状态改动），属观察面非命令执行面——本律不变。）
3. **证据链文化**：命令与结果必须落文件、按 run 归档。
4. **崩溃隔离**：通道侧故障不得拖垮游戏；游戏崩溃（含 exit 0 假象）通道侧要能察觉。
5. **CLI 通路现状**：attic-cli 是会话宿主，stdin 被占用——**stdin 转发路线排除**。

## 2. 载体对比

| 维度 | A. 文件队列（tmp/inbox/processed/outbox） | B. localhost socket |
|---|---|---|
| 写纪律贴合 | 轮询线程收令→解析→入 submit 队列，游戏线程只碰内存 | 需 accept 线程，同等模型 |
| CLI 人机工程 | **echo 一行 JSON 即发令**；结果文件可 tail；PowerShell 零依赖 | 需客户端工具包装 |
| 证据链 | 命令/结果即文件，run 归档零成本 | 需另写落盘镜像 |
| 崩溃语义 | 游戏崩：outbox 停在半截（ACCEPTED 滞留=已接收未执行）；外部进程零连累 | 连接断实时感知，但 agent 网络线程=新崩溃面 |
| 延迟 | 轮询间隔级（250ms 上限，够用） | 毫秒级（当前无需求） |
| Windows 文件锁 | tmp→inbox 同卷 rename 近原子，读侧不见半截 | 无此问题 |
| 安全面 | 目录权限即边界（gameDir 下） | 本机任意进程可入令，需 token |
| 实现量 | agent 侧 ~150 行（线程+解析+入队）；CLI 侧 0 | agent+独立 CLI 客户端 |

**结论：A 为 v1 载体**（评审确认）。协议做成载体无关，壳/MCP 阶段可换传输——A 不锁死 B。

## 3. 协议（v2，含评审采纳项）

目录布局（默认 `<gameDir>\mca\`，agent 参数 `cmd=<dir>` 可覆盖，覆盖路径须规范化并校验归属，禁指向 `saves\`）：

```text
mca/
├── tmp/        CLI 写这里，关闭文件后 rename 进 inbox/（同卷原子）
├── inbox/      agent 只消费此目录的 *.json，按文件名序（<epochms>-<rand>.json）
├── processed/  读取成功即移入 = ACCEPTED（不代表已执行）
└── outbox/     <id>.json 状态记录：ACCEPTED 临时态，执行后原子替换终态
```

命令封包：

```json
{"protocolVersion":1,"id":"1717000000123-a1b2","op":"place","args":{"x":0,"y":71,"z":0,"blockId":4},"save":true}
```

- `protocolVersion`：**必填**，未知版本一律 REJECTED（防新 CLI×旧 agent 静默互误解）。
- `id`：CLI 生成**全局唯一**（epochms+rand）；会话内以 processed/ 文件名去重，重复 id 返回已有状态不重执行；跨 run 去重边界=单次 run（processed 随 run 归档）。**CLI 超时≠失败**——先查 outbox 再决定是否重发。
- 参数方块 id 命名 `blockId`（避免与 envelope `id` 混淆，评审抓对）。
- `targetGeneration`：**可选**（P1 世界世代，2026-11-13 已实现）。带=会话定向：执行时（tick 边界）比对 `capturedWorldGeneration`（初值 1，WORLD_SWITCHED 自增），不符 REJECTED `generation-mismatch expected=X actual=Y`，防"旧世界提交、新世界激活后误执行"；不带=执行时绑定当前世界（CLI 普通命令语义），两者不混同。

状态生命周期（评审采纳：**接收≠执行**）：

| 状态 | 含义 |
|---|---|
| ACCEPTED | 已读取移入 processed/，等待 tick 执行（outbox 临时态） |
| OK | 游戏线程执行成功 |
| NO_CHANGE | 执行成功但零变化（协议 v2，2026-11-13 落；生产者={fill 同 id 区域, genscatter 盒内无石头}，save 跳过——磁盘态未动；v1 归档账目不动） |
| REJECTED | 参数/前置条件不满足（含协议版本、未知 op） |
| UNSUPPORTED | 当前版本签名不符（对齐织入层三态，不静默） |
| ERROR | 执行异常 |
| （ACCEPTED 滞留） | agent 在接收后、执行前崩溃——CLI 的诊断信号 |

save 策略（评审采纳，按操作类型）：

| 类型 | 默认 |
|---|---|
| 写（place/dig/settime/setspawn/fill/setmeta/genscatter/setlook/setmotion/give/sethealth/explode） | `save=true`（零变更 NO_CHANGE 时跳过存盘） |
| 读（getblock/getinfo/getblocks/getplayer/getmeta/getlight/listops/inv） | 忽略 save |
| 瞬态（spawn/drop/invulnerable） | 忽略 save——**0327 saveWorld 不写实体**（Infdev 特性，spec §1 实锤），实体恒临时态存退即失；heartsLife 亦不进 NBT；detail 不拼 saved= 防误导 |
| 临时态（tp） | `save=false`，显式要求才存 |
| `save` 独立 op | 显式刷盘用 |

注意：**"调了 saveWorld ≠ 全状态持久化"**——覆盖面按操作逐版本实锤（§6 清单）。

## 4. 可靠性规则（评审四条 + 补强）

1. **原子提交与消费**：tmp→rename→inbox；agent 读取成功**先移 processed/ 再 submit()**；移入=ACCEPTED，执行结果以 outbox 终态为准。
2. **幂等**：见 §3 id 规则。
3. **错误不阻塞队列**：每条独立校验独立处理，坏 JSON/非法参数→REJECTED/ERROR 落 outbox 后继续下一条；限额：单文件 ≤4KB、单次轮询 ≤8 条、积压超 64 条拒绝新投递（REJECTED:queue-full）。
4. **不信任任意路径**：`cmd=` 覆盖需规范化+归属校验；文件权限是基础边界而非本机强认证，多客户端需求出现时再加会话令牌。

## 5. 轮询与线程模型（v2 修订）

- **每 5 tick 轮询**（20TPS 下等待上限 250ms），单次轮询读取限量（§4-3）。
- **文件 I/O 与解析在独立 daemon 轮询线程**（premain 期创建，全异常捕获只记日志不外溢）；游戏线程只 dequeue 已验证的 Runnable——磁盘慢不卡 tick。原案"零新增常驻线程"说法由评审修正，采纳；线程安全前提已核：`MCAHooks.QUEUE=ConcurrentLinkedQueue`、`capturedWorld=volatile`。
- 世界未就绪（把手①空/首 tick 前）：轮询线程照常收但一律 REJECTED（not-ready），不静默吞。

## 6. 首批操作面

| op | 实现 | 取证状态 | 持久化实锤 |
|---|---|---|---|
| place | `setBlockWithNotify c(IIII)Z` | 实名已钉 | ✅ run B 读回 |
| dig | 同上 blockId=0 | 同入口（PlayerController L27 游戏自删路径） | 待实跑（并入第二块砖） |
| getblock | `getBlockId a(III)I` | 实名已钉 | n/a |
| getinfo | 字段直读 `worldTime=b` / `randSeed=x`(private) / `spawnX/Y/Z=g/h/i` / `difficulty=d`（difficultySetting，主循环每帧=GameSettings.difficulty，客户端全局不进 level.dat，2026-11-14 补） | 实名已钉（saveWorld NBT 键绑定 + `h()` 每 tick b+1、b%200 触发 autosave，run p0p1-gen 取证） | n/a（只读；实跑 run getinfo-1：time Δ=tick Δ、seed 对账 level.dat 命中） |
| settime | `worldTime` 字段 `b` 直写 | **取证闭环（全量 javap 303 类）**：`b` 外部读点仅 Chunk 存盘 LastUpdate；渲染每帧 `client.a.b.a(lerp)`→`f.b(F)F`→`glRotatef` 实时算零缓存——**无通知面，写字段即生效** | level.dat "Time" 写路径 L138（字节码） |
| setspawn | `spawnX/Y/Z` = `g/h/i` 直写 | **取证闭环+活体纠正（run setw-1）**：respawn 锚点——死亡重生消费该值（实锤：重生落新点）；重进世界从 `Player.Pos` 恢复、不落 spawn；不动当前玩家（移动属 tp） | L135-137（字节码）+ ✅ run setw-1 磁盘对账 |
| tp | 直写 `g/h/i`=块中心(x+0.5,y,z+0.5)+清 `j/k/l` → `b(DDDFF)V` setPositionAndRotation（同步 `d/e/f` lastTick+重建 AABB） | **取证闭环（全量 javap 303 类）**：Entity 基类 `a.c.b`：d/e/f=lastTick、g/h/i=pos、j/k/l=motion、m/n=yaw/pitch（全 public）；玩家句柄=`World.c` public 字段（EntityPlayer 构造器自设 `world.c=this`，零新钩子）；配方=游戏 NBT 载入同路径（`Entity.d`←Pos 后调 `b()`）；`b()` 内 h=y+u（u=1.62 眼偏移）→ 脚落 y 平面、眼 y+1.62；mover 每 tick 自愈重算 `pos.y=AABB.bottom+u−O`；`A/B/C` 死字段零读写，无需同步 | `Player.Pos` 持久化已实锤（run setw-1）；✅ run tp-1：目视瞬移+边境之地远距+Pos[1]=y+1.62 磁盘对账+存退重进落 tp 点 |
| save | `saveWorld(false)` | 已实锤 | ✅ |
| fill | 循环 `setBlockWithNotify c(IIII)Z`（args x1/y1/z1/x2/y2/z2/blockId 全 int；反角自动归一化；体积 cap 4096=16³，超限 REJECTED `volume-too-large`；changed=0→NO_CHANGE）。取证推翻"快路径"预期：Chunk.setTile 内部已含 heightmap 重算+tile 移除+onPlace+isModified 脏位，`a(IIII)Z` 只省 6 邻光照与逐块观察者标记——省不出数量级，v1 正确性优先，快路径留实测后候选 | **取证闭环（readers 反汇编）** | ✅ run fill-1：243 块含 save=9ms、324 块含 save=7ms——cap 内单 tick 无压力 |
| getblocks | 循环 `getBlockId a(III)I`（args x1/y1/z1/x2/y2/z2；同 cap；RLE 编码遍历序 y→x→z，run=id:count 竖线分隔） | **取证闭环**：provider `b(II)` 对未加载坐标**就地生成整 chunk**（地形噪声全套）——与游戏同语义，cap 即限幅 | n/a（只读） |
| getplayer | 直读 Entity `g/h/i`+`j/k/l`+`m/n` + EntityLiving `W`=Health（public int，NBT "Health" 双向绑定实锤，默认 10）；W 在子类→execute 期按实例类惰性 getField，失败→UNSUPPORTED | **取证闭环** | n/a（只读） |
| getmeta | `getBlockMetadata d(III)I`（args x/y/z；Chunk.b(III)I 读 nibble g，索引 `x<<11|z<<7|y`） | **取证闭环** | n/a（只读） |
| setmeta | `b(IIII)V` 静默写（h(IIII)Z→Chunk.b(IIII)V 仅 nibble+dirty，零通知实锤——sapling 生长即此面）+ 手动复刻 `c(IIII)Z` 通知段（观察者 `World.u` a(III)V + `e(IIII)V` 光照 6 邻）——同 id 走 c() 被 Chunk 短路不通知；meta∈[0,15] 否则 REJECTED `meta-out-of-range` | **取证闭环**（可读树名 `setBlockMetadataWithNotify` 系 deobf 误猜，javap 实锤静默） | 写侧 save=true；nibble 随 Chunk 存盘 "Data" |
| genscatter | 调真生成器 `a.a.c.b.b(id).a(f,Random,III)Z`（0327 唯一原生"结构生成"：椭球曲线把 id 撒在 **stone** 上——meta-1 实跑翻案，基底 `x.i`=id 1 石头，该类实为原版矿石团生成器，c_a 以矿 id 调用；名 WorldGenTrees 系误，sapling 在 0327 不长树）；args x/y/z/blockId+可选 seed（确定性，仅基底未动时成立）；游戏写=静默（populate 触发链取证见 phase1 §16：0327 无 TerrainPopulated NBT，护栏=getOrCreateChunk 三邻规则+坐标 seed 确定性幂等），运行时补：影响盒真实上界 `[x+4..x+12]×[y..y+5]×[z+4..z+12]`（486 格=中心线±椭球半径半轴+截断向零；meta-1 实锤旧 75 格盒漏计漏通知）预扫→复扫 diff→变更格逐个通知；changed=0→NO_CHANGE；类惰性 forName 失败→UNSUPPORTED | **取证闭环**（BlockSapling 翻案：`f(III)I`=getBrightness 非气温，光照≥9+20% 概率+meta 幂等重放；散布器基底翻案见 phase1 §13/§14） | 写侧 save=true；✅ 实跑验收 run meta-1+meta-2（2026-11-14） |
| listops | agent 侧自省（KNOWN_OPS+参数表静态串），零游戏调用；世界未捕获仍 not-ready（v1 从简） | n/a | n/a（只读） |
| getlight | `getBrightness f(III)I`（args x/y/z；Chunk.c(IIII)I 读 h=SkyLight 减环境值 `World.q` 后与 i=BlockLight 取 max，返回 0-15；纯读——光照不进存盘对账，由方块表实时算） | **已钉**（meta 轮翻案，phase1 §13；本批零新取证） | n/a（只读） |
| setlook | Entity `m/n`=yaw/pitch（public float）直写；args yaw∈[0,360)、pitch∈[-90,90] **整数角度**（MiniJson 拒浮点），越界 REJECTED；零通知面——渲染/瞄准直读字段（tp 轮 `b(DDDFF)` 设 m/n 即生效同证） | **已钉**（tp 轮全字段，phase1 §10；本批零新取证） | Player NBT "Rotation" 随存盘（setw-1/tp-1 Player compound 独立持久化已证） |
| setmotion | Entity `j/k/l`=motion（**public double**——tp 轮清速度 setDouble 活体实锤）直写 `v/1000`；args vx/vy/vz **整数千分位**，\|v\|≤10000（=±10 格/tick），越界 REJECTED `v?-out-of-range`；mover 每 tick 由 AABB 自愈重算 pos——写速度后物理自洽 | **已钉**（tp 轮全字段；本批零新取证） | Player NBT "Motion" 随存盘（同上） |
| give | `EntityHuman.b`=InventoryPlayer(=a.c.e.b) `a(ItemStack)Z`=addStack；args itemId/count（+可选 dmg，缺省 0）；count∈[1,64]、dmg∈[0,32767] 否则 REJECTED；**注册表 `net.minecraft.a.b.i.b[]` 直索引校验**（越界/null 空洞→REJECTED `bad-itemid`——游戏侧 `a()Li;=i.b[c]` 直索引，放行即 NPE）；dmg==0 走 `(II)V` 否则 `(III)V`（ItemStack=`net.minecraft.a.b.k`：a=Count/c=id/d=Damage）；addStack false→REJECTED `inventory-full`（原栈未消耗，可 inv 对账）；**真实 addStack 会分栈**（give-1 实跑：x64 进 1x55 栈→补满 64+余量开新槽；op 只检查 boolean，不受影响）；ItemStack 类经主槽数组分量类型取得，零硬编码类名；全惰性解析（eHealth 同模式）失败→UNSUPPORTED | **已钉**（give-inv-health 批 javap 全量反汇编，spec §1） | EntityPlayer `a(NBT)` 写 "Inventory"（主槽 Slot 0..35、盔甲 100..103，每项 id/Count/Damage）→save 即落盘；不校验"可手持渲染"面（注册表非 null 即放行，怪物蛋等渲染异常属原版行为）；**非工具也可赋 dmg=原版语义**（dmg 仅影响 addStack 合并（只对 dmg==0）与工具耐久条渲染，无危害不限制——give-1 实跑满包场景即故意用齿轮+dmg 占槽） |
| inv | 遍历 InventoryPlayer `a[36]`+`b[4]`：非空槽输出 `槽=数量xID[ d伤害]`（主槽 0..35、盔甲 A0..A3）+ `cur=当前快捷槽`；全空→`empty`；纯读忽略 save | **已钉**（同上） | n/a（只读） |
| sethealth | EntityLiving `W`=Health 直写（public int，getplayer 轮已钉）；args health∈[0,20] 否则 REJECTED——**直写绕过 `b(I)V` 治疗钳制**（20 上限；死亡中不治疗），op 侧自校验为拒绝而非截断；HUD `client.a_d` 每帧直读 W→即时生效零通知面；置 0=原版死亡流程接管（deathTicks→死亡屏 `client.d` 弹出→按钮重生重建玩家 W=20），**agent 不代打 respawn**；give-1 实跑新钉：和平 difficulty=0 回血极快（10→20 约 10s，"重进后 20"多为回血非载入缺陷）、**重生清空物品栏**（新实例无 keep-inventory） | **已钉**（同上；W 面 getplayer 轮实锤） | Player NBT "Health" 双向绑定（getplayer 轮已钉）；持久化 0 后重进=载入 Health=0 再次死亡屏（原版语义，不特殊处理） |
| explode | World `a(Entity,DDDF)V`=createExplosion(Entity source,x,y,z,float size) 调 `null` source（区域取实体遍历判等实现=**null 零 NPE**，源码 L1180 实锤；Explosion 类=空标记，16³ 壳射线+方块阻力采样逻辑全在 World 方法内）；args x/y/z/size **整数**，size∈[0,10] 否则 REJECTED（size=10 射线采样 ~40 万块读，单 tick 掉帧不崩溃——实跑计时后议收紧）；**伤害不豁免玩家**（原版语义，实跑远距离戒律）；破坏方块进方块表 | **已钉**（spawn-explode-drop 批源码+javap 双路，spec §1） | 写侧 saveSuffix（默认 true）——破坏方块持久；音效/粒子瞬态不进 NBT |
| spawn | kind 白名单九项（pig/sheep/creeper/skeleton/zombie/spider/tnt→混淆类表 `a.c.b.c`/`a.c.b.b`/`a.c.a.d`/`a.c.a.c`/`a.c.a.f`/`a.c.a.b`/`a.c.c.a`，**字符串常量判决**：贴图路径/实体名实锤；**giant**=`a.c.a.a` 经 NEW_CTOR 构造器补全复活（giant-1 目视 10 倍巨僵尸，RenderGiantZombie scale 6.0）；**mob**=`a.c.c` EntityCreature 基类（构造器健在，渲染一级父类回退=ModelBiped 人形，用户判决"非正常生物但可生成"））；配方=构造 `(World)`（零副作用实锤）→setPosRot(x+0.5,y,z+0.5)（tnt 特例 `(World,FFF)V` fuse=80 内置，setPosRot 同调用无害幂等）→World `a(Entity)V`=spawnEntityInWorld=loadedEntityList.add——**主循环每 tick 遍历该表，加入即活零新钩**；arrow/painting 面留后续 | **已钉**（同上+§20） | **瞬态**：0327 saveWorld 不写实体=存退即失（save 忽略，detail 无 saved=；giant/mob 复验同） |
| drop | EntityItem=`a.c.c.b` 构造 `(World,DDD,ItemStack)`（位置=格中心）→spawnEntityInWorld；count/dmg/注册表校验复用 give 面（同序同串：no-player→count→dmg→bad-itemid）；ItemStack 复用 isCtor2/3；走近由原版拾取面（addStack 同规则）收起 | **已钉**（同上） | **瞬态**（同 spawn；被拾取则进 Inventory 随 save 持久） |
| invulnerable | Entity `K`=heartsLife（public int，原版 i-frame 计时器）直写；args ticks∈[0,32767]；**`K>10` 时 EntityPlayer.attackEntityFrom 入口拒绝全部伤害源**（近战/箭/火焰/坠落/虚空/爆炸同走单入口，源码+javap 双路实锤）；每 tick 自减 1=限时语义（≥11 免疫、0 取消、32767≈27 分钟）；副作用=HUD 心闪烁（GuiIngame 读同字段，仅视觉） | **已钉**（sed-1 批前用户判决"玩家不死先"，spec §5） | **瞬态**：heartsLife 不进 NBT，save 忽略 |
| entitylist | World `i()List`=getLoadedEntityList 遍历；kind/pos/health 全 **per-instance** 解析（Entity 面缓存 Field/Method 跨类复用=TE 按类缓存同族坑，per-instance 走继承链两世界等价）；输出 `N=kind@x,y,z[h=H]`（非 Living 无 W 省略 health——**先探测后写防半截残留**，无头网住的 bug①）；空表=`none`；**spawn/drop 的账面对账面**（此前只能目视） | **已钉**（零取证五 op 批，全部落 §10/§17/物品栏批已钉面） | n/a（只读） |
| playsound | World `a(DDD,String,F,F)V`=playSoundEffect（createExplosion L762 调用点实锤参数序）；name≤64 透传**不建白名单**（不存在音效=客户端查表 miss 静默 no-op）；vol∈[0,10]/pitch∈[0,20] **整数十分位**（MiniJson 整数子集） | **已钉**（同上） | **瞬态**：音效不进 NBT，save 忽略 |
| setarmor | InventoryPlayer `b[4]` 盔甲槽数组元素**直写替换**（slot∈[0,4)）；校验复用 give 面同序同串；**不校验 ItemArmor**（注册表非 null 即放行=give 同戒律，非盔甲=无减伤不崩溃）；减伤链 getPlayerArmorValue（EntityPlayer L205）活体减伤目视 v1 跳过（需关免疫，风险不对等，spec §3.3） | **已钉**（give 批 §1 面） | 随 "Inventory" NBT 持久（Slot 100..103，写侧 saveSuffix） |
| clearinv | 主槽 `a[36]`+盔甲 `b[4]` 全 null；**不可逆**（无 undo 面，detail 固定 `cleared 36+4`） | **已钉**（give 批 §1 面） | 写侧 saveSuffix（空背包落盘） |
| replace | 区域条件替换：normalizeRegion（cap 4096 复用）+validateBlockId+`oldId==newId→same-id 拒绝`；仅 getBlock==oldId 格 c(IIII)Z；零变更→**NoChange**（fill 同语义跳 save） | **已钉**（fill 批 §12 面） | 写侧 saveSuffix（changed>0 落盘） |
| getchest | World `h(III)`=getBlockTileEntity→null REJECTED `no-tileentity`；**0327 原版地雷（te-1 双实锤）**：TE 缺失且方块=空气时 `Chunk.d(III)` 重建路径 NPE（`blocksList[0]=null`）——teHandle 捕获 ITE(cause=NPE) 映射 `no-tileentity`，协议面不露 ERROR；IInventory 三方法（`a()I`/`a(I)Lk;`/`a(ILk;)V`）**从实例类直取+按 TE 类名缓存**（chest/furnace 异类 Method 不通用=无头首跑实锤；不取接口对象=假类 javac 结构限制案，无头/活体同构）；输出 `slots=N 槽=数量xID[ d伤害]`/empty；**通用面=对 chest/furnace 皆可**（gc8 活体 `slots=3`）；纯读忽略 save | **已钉**（tileentity-batch 批源码+javap，spec §1） | n/a（只读） |
| setchest | slot∈[0,size) 否则 REJECTED `slot-out-of-range`；clear=true 置空槽（与 itemId/count 互斥 clear 优先）；否则校验复用 give 面同序同串（count→dmg→bad-itemid，**TE 面零 player 依赖**=ensureRegistry 独立件）；setInventorySlotContents **直替换无合并**（原版语义）；ItemStack 构造复用 isCtor2/3（resolveTe 共享缓存） | **已钉**（同上） | 写侧 saveSuffix——**TE 随 chunk "TileEntities"+Items+BurnTime/CookTime 持久**（与实体不落盘对照面，te-1 断言） |
| getfurnace | 类型判=**字段面**（`f`=BurnTime/`h`=CookTime 仅 Furnace 类存在且系 **private**→getDeclaredField+setAccessible，randSeed 先例；getInvName 返回 "Chest"=**原版笔误不可信**）：NoSuchField→REJECTED `not-furnace`；输出 `furnace burn=B cook=C burning=bool 槽...`；点燃/冶炼=updateEntity 原版链（喂 slot1 燃料下 tick 自动吞：wood 300t/煤 1600t；cook 满 200 冶炼 6 配方；翻转 61↔62 换块重挂 TE）——**agent 不代烧**；纯读 | **已钉**（同上） | n/a（只读；炉态持久见 setchest 行） |

批量写第二批已实装并验收（2026-11-13 落码，实跑 run `fill-1` 全项通过——NO_CHANGE 活体实例、同 id 短路 changed=220、存退重进持久化、FILL_MS 243 块含 save=9ms，见 [phase1-command-channel](../research/phase1-command-channel.md) §12）。第三批（getmeta/setmeta/genscatter/listops，2026-11-14 落码，无头 79/79）取证与判决变更见 [phase1-command-channel](../research/phase1-command-channel.md) §13，实跑验收 run `meta-1`+修复复验 `meta-2` 全项通过（difficulty 对账、setmeta nibble 持久化、火把目视转向、散布计数精确、NO_CHANGE；活体翻案=散布器基底 stone+预扫盒上界 486 格，见 §14/§15），见 [phase1-command-channel](../research/phase1-command-channel.md) §14。第四批（getlight/setlook/setmotion，2026-11-14 落码，**零新取证**——三面全部已钉面；协议=整数角度/整数千分位，无头 95/95）取证引用与 motion=double 纠正见 [phase1-command-channel](../research/phase1-command-channel.md) §17，实跑验收 `llm-1` 已完成全绿（2026-11-14：vy 不被首 tick 清零、存退重进 Rotation 盘面一致、新钉 0327 Player NBT=TAG_List 与 saveWorld 的 c==null 条件写窗）。第五批（give/inv/sethealth 物品栏三 op，2026-11-14 落码，KNOWN_OPS→21；javap 全量反汇编取证见 [.trae/specs/give-inv-health/spec.md](../../.trae/specs/give-inv-health/spec.md) §1 与 [phase1-command-channel](../research/phase1-command-channel.md) §18；无头 114/114）实跑 `give-1` **已完成全绿**（2026-11-14：齿轮 55 + 工具 256-260 耐久 d10 持久化闭环、满包/越界拒绝无副作用、置 0 死亡屏→Respawn 目视；新钉真实 addStack 分栈、和平回血、重生清空物品栏、0327 level.dat 根无 Level 包装）。第六批（explode/spawn/drop/invulnerable 实体面四 op，2026-11-14 落码，KNOWN_OPS→**25**——invulnerable 为 sed-1 批前用户判决"玩家不死先"增量，spec §5；源码树语义+javap 混淆指纹双路取证见 [.trae/specs/spawn-explode-drop/spec.md](../../.trae/specs/spawn-explode-drop/spec.md) §1 与 [phase1-command-channel](../research/phase1-command-channel.md) §19；无头 129/129）实跑 `sed-1` **已完成全绿**（2026-11-14，Easy=启动前 options.txt 切换；spawn 七 kind 上台活体+敌对围攻零掉血=免疫实锤、tnt/explode 破坏跨存退持久、实体存退即失、i-frame 瞬态钉版；新钉水面筑台找地/实体碰撞盒推人/水下爆炸照常；见 §19）；第七批（**NEW_CTOR 受限构造器补全**=戒律唯一豁免，用户判决 B 路线+0413/0414 泛化启示，见 §20 与 [.trae/specs/spawn-explode-drop/spec.md](../../.trae/specs/spawn-explode-drop/spec.md) §6；无头 130/130）实跑 `giant-1` **已完成全绿**（2026-11-14：CTOR-INJECTED 懒加载注入实锤+巨僵尸/人形基类双可见+存退即失复验，spawn kind 现九项）；第八批（tileentity 三 op getchest/setchest/getfurnace，2026-11-14 落码，KNOWN_OPS→**28**；源码+javap 双路取证见 [.trae/specs/tileentity-batch/spec.md](../../.trae/specs/tileentity-batch/spec.md) §1 与 [phase1-command-channel](../research/phase1-command-channel.md) §21；无头 144/144——含按类缓存结构缺陷修复实锤）实跑 `te-1` **已完成全绿**（2026-11-14 跨 3 会话：place 自动建 TE 活体实锤、冶炼闭环出铁锭 265、TE 持久双验（会话内+跨进程）、IInventory 通用面对炉成立；新钉=Chunk.d 空气位 NPE 原版地雷（teHandle 已映射 REJECTED，回归 145/145）、**存盘时序窗后果升级=IOOBE+玩家数据重置**（用户实锤，再启动自愈）、暂停停 TE tick 纪律；见 §21）；第九批（**零取证五 op** entitylist/playsound/setarmor/clearinv/replace，2026-11-14 落码，KNOWN_OPS→**33**——全部落已钉面（llm-1 同模式），spec [.trae/specs/zero-evidence-five/spec.md](../../.trae/specs/zero-evidence-five/spec.md) + [phase1 §22](../research/phase1-command-channel.md)；无头 160/160——又网住两真 bug：半截 `[h=` 残留（先探测后写修复）、假注册表无 55 用例改 35）实跑 `obs-1` **已完成全绿**（2026-11-14：entitylist N=4→存退 N=1 即失、kind=贴图路径活体钉版（/char.png、/mob/pig.png）、EntityItem null+无 health 省略分支、playsound 不存在音效静默、setarmor A0 闭环、clearinv/replace 持久+NO_CHANGE 三态；见 §22）；第十批（**hud-overlay** GuiIngame EXIT 钩观察面，2026-11-14 落码——织入新模式 `EXIT=4`（RETURN 前观察注入），**零新 op**（KNOWN_OPS 仍 33）；织入面反射链 GuiIngame=**`client.c.m`**（首版误钩 d.b=无关计时类，实跑教训：绘制调用全成功但画面无字=钩在 tick 路径；字符串常量扫描+类体 javap 才钉 GUI 类，见 [phase1 §23](../research/phase1-command-channel.md)）；XYZ+javaagent 面=showFPS 门+原版调试列排版（XYZ y=42；y=52 头行 `javaagents:`+y=62 起逐项 `- jar名`）+shortAgent 压缩（用户判决改造覆盖 spec 布局）；无头 164/164）实跑 `hud-1` **已验收通过**（2026-11-14 第七启动：伪色修复=EXIT 点零 GL 状态改动——glEnable 会激活准星段残留反色 blendFunc，教训入 §23；目视文字干净、showFPS 关全消）；client-ops 按用户判决不进队列。

## 6.1 现代命令面对照备忘（2026-11-20 落，对象=正式版 26.3）

调研方法：piston-data 26.3 client.jar（**无混淆**）直读 `net/minecraft/server/commands/` 全 100 命令类 + javap 字面量反汇编（jar 暂存件在案卷外，判决以本备忘为准）。时间线实据（用户供）：`/time` 的 clock/timeline/timemarker/rate/pause 子系= **26.1 Snapshot 3** 加入——"现代"自身半衰期以月计，移动靶论据由推测升级为实据。

**命名判决（三层）**：

1. **协议 op 名不改**——35 op 中现代对应仅覆盖 12（同名 4：fill/tp/give/playsound；异名 8：setblock/summon/spawnpoint/time/save-all/rotate/help≈/data get≈），其余 ~23 现代无对应；改名对齐不了多数面，且词汇权威在案卷 §6。
2. **壳别名八条**（速记翻译器体验层，零协议）：`setblock→place`、`summon→spawn`、`spawnpoint→setspawn`、`time set <n>→settime`（**收窄形式**——现代 /time 是子命令族，仅数值 set 映射；其余子词壳侧预检提示"query 语义=getinfo"）、`teleport→tp`、`rotate→setlook`、`save-all→save`、`data get→getblock`。
3. **clear 陷阱（唯一硬冲突，显式裁决）**：现代 `/clear`=清玩家背包（=我们 `clearinv`）；我们 canonical `clear`=杀地面物（现代等价=`/kill @e[type=item]`）。别名表**不引入** clear 现代映射，canonical 语义保留——未来游戏内控制台玩家肌肉记忆默认现代语义，此处最易咬人。

**格式判决**：现代四支柱（resource location 名表/选择器 targets/相对坐标+浮点/SNBT）对早期版本**不可后向移植**（无数据地基：数字 id 即原生注册表、单人无选择器、MiniJson 整数子集实跑钉版、无 SNBT）；但我们的 **id+data 格式可前向移植**（1.13+ registry 内部仍有序数，游戏自带 legacy id+data→blockstate 转换表=结构升级同件）——协议格式不阻塞未来伸向现代版本。候选小增量：place 可选尾参 meta（id+data 一元化，向后兼容）。

**time pause 取证补充（2026-11-20，26.3 字节码链闭环）**：26.x 把日周期**对象化**为独立 clock 子系统——`ClockState=(totalTicks,partialTick,rate,paused)`（SavedData 落盘，暂停态跨会话）、每维度经 `DimensionType.defaultClock` 绑 clock、睡觉改 `moveToTimeMarker(WAKE_UP_FROM_SLEEP)`；`ServerLevel.tickTime()` 反汇编实锤**只推进 gameTime+TimerQueue**（日周期已不经此处）。故 `/time pause`=仅冻结该 clock 的 totalTicks（日/月角度、天光、query 读数定格；实体/冶炼/随机刻/红石照常），其可行性**根植于 clock 与 tick 流解耦的现代架构**。老版本不可移植：0327 日周期与 tick 流是**同一计数器**（worldTime=b 每 tick+1、b%200 驱动 autosave——p0p1-gen 取证钉），"冻昼夜不冻实体"语义不存在；硬做两条路全违戒（每 tick 回写 b=通道无常驻任务语义 / 钩 b++=新注入面+打断 autosave 节律与存盘取证链）。**判决：time pause 不进候选表**——"现代命令依赖现代架构面"标本，反向加强移动靶论据（对齐 26.3 time 面=要求 clock 对象化，在册版本全无）。

**逐版权益候选表（排除判决必须带版本定语——一刀切"现代有老版本无"已被证伪）**：

| 候选 | 0327 | 版本门控 | 取证状态 |
|---|---|---|---|
| kill（实体） | 可做 | — | **零新取证**（chair-1 setEntityDead 泛化，去 instanceof EntityItem 过滤；补 spawn 无撤销的洞） |
| clone（区域复制含 meta） | 可做 | — | **零新取证**（getblocks+place/setmeta 纯组合，cap 4096 同 fill；fixture 构建直接削活体批成本） |
| particle（可视标记） | 待验 | — | 需新取证（0327 Effect/加粒子入口） |
| setdifficulty | 可做 | — | 字段已钉（GameSettings.difficulty 客户端全局，**不进 level.dat** 语义须标清） |
| weather | 待验 | **b1.5+ 确认存在**（用户钉） | 0327 有无 raining 字段待 javap |
| gamemode | 不存在 | **b1.8+**（creative 旗标） | 版本门控候选 |
| xp | 不存在 | **b1.8+**（经验字段，getplayer 同族） | 版本门控候选 |
| effect | 不存在 | **b1.9-pre2+**（MobEffect 挂 EntityLiving；与 invulnerable=hurtResistantTime 不同面不互替） | 版本门控候选 |
| nbt get/set（泛化 TE/实体） | 方向件 | — | 归 T2/listops v2 同族，不单独排 |

其余排除项按同纪律重查（**scoreboard 疑似 b1.5 已有，不得沿用全局排除**）；排除表定稿形态=逐版三分式：不存在 / 版本门控候选(≥X) / 已排除有因。**listops v2 添一条硬论据**：version-gate 字段的机器可读形态正是路线 B（agent 侧能力表）的本职——此类事实写进 UI 硬编码必漂移。

**自我纠错两则入档**：/explode、/bone_meal 在 vanilla 命令面**不存在**（Bukkit 遗产，26.3 全 jar 扫描零命中）——前轮口误，经实证翻正。

## 7. 戒律（不变项）

坐标前校验世界加载（tick 计数阈值）；agent 不直接写世界文件——一切经游戏方法；每 run 的 mca/ 目录随 `out/<run>/` 归档；kill 前确认用户回标题（phase1 §6）。

## 8. 拍板状态

1. ~~封包格式~~ **JSON 严格子集**（手写解析：固定字段、拒重复键/错类型/未知版本；args 限平铺标量，零第三方依赖）——评审确认。
2. ~~save 默认~~ **按操作类型**（§3 表）——评审确认。
3. ~~轮询节奏~~ **每 5 tick + 单次限量 + 轮询线程**（§5）——评审确认。
4. **载体 A 最终拍板**——评审已按 A 展开，待用户一句确认即可开工实现。
