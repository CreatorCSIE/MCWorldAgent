# 设计案卷 — Agent 外部命令通道（载体对比与协议草案）

日期：2026-11-13 ｜ 状态：**已按外部评审提案修订（v2），载体 A 待最终拍板** ｜ 前置：[phase0](../research/phase0-agent-poc.md) §10、[phase1](../research/phase1-first-brick.md)、[ingame-console](ingame-console.md)

## 1. 需求与决策背景

第一块砖的 `submit()` 由 WORLD_CREATED 自动触发，是一次性演示。主线（用户拍板）：
**新增命令操作面**（挖除、时间、出生点、传送、批量写…），通道第一个客户是 **CLI**（人工/脚本可直发），
桌面壳与游戏内控制台（[ingame-console](ingame-console.md)）是后续客户。

硬约束（继承既有纪律）：

1. **tick 边界执行**：外部命令一律经 `MCAHooks.submit()` 队列，游戏线程 drain。
2. **零新字节码注入**：命令执行体走反射，不扩织入面。（v2 注：轮询线程是 agent 侧普通线程，非织入钩，不违此律。）
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
| REJECTED | 参数/前置条件不满足（含协议版本、未知 op） |
| UNSUPPORTED | 当前版本签名不符（对齐织入层三态，不静默） |
| ERROR | 执行异常 |
| （ACCEPTED 滞留） | agent 在接收后、执行前崩溃——CLI 的诊断信号 |

save 策略（评审采纳，按操作类型）：

| 类型 | 默认 |
|---|---|
| 写（place/dig/settime/setspawn） | `save=true` |
| 读（getblock/getinfo） | 忽略 save |
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
| getinfo | 字段直读 `worldTime=b` / `randSeed=x`(private) / `spawnX/Y/Z=g/h/i` | 实名已钉（saveWorld NBT 键绑定 + `h()` 每 tick b+1、b%200 触发 autosave，run p0p1-gen 取证） | n/a（只读；实跑 run getinfo-1：time Δ=tick Δ、seed 对账 level.dat 命中） |
| settime | `worldTime` 字段 `b` 直写 | **取证闭环（全量 javap 303 类）**：`b` 外部读点仅 Chunk 存盘 LastUpdate；渲染每帧 `client.a.b.a(lerp)`→`f.b(F)F`→`glRotatef` 实时算零缓存——**无通知面，写字段即生效** | level.dat "Time" 写路径 L138（字节码） |
| setspawn | `spawnX/Y/Z` = `g/h/i` 直写 | **取证闭环+活体纠正（run setw-1）**：respawn 锚点——死亡重生消费该值（实锤：重生落新点）；重进世界从 `Player.Pos` 恢复、不落 spawn；不动当前玩家（移动属 tp） | L135-137（字节码）+ ✅ run setw-1 磁盘对账 |
| tp | `Entity.setPosition`+lastTick/prev | 配方已定（AABB-first） | `Player.Pos` 持久化已实锤（run setw-1 level.dat dump）；剩写方生效面待验 |
| save | `saveWorld(false)` | 已实锤 | ✅ |

批量写（setTileNoUpdate+脏区标记）第二批。

## 7. 戒律（不变项）

坐标前校验世界加载（tick 计数阈值）；agent 不直接写世界文件——一切经游戏方法；每 run 的 mca/ 目录随 `out/<run>/` 归档；kill 前确认用户回标题（phase1 §6）。

## 8. 拍板状态

1. ~~封包格式~~ **JSON 严格子集**（手写解析：固定字段、拒重复键/错类型/未知版本；args 限平铺标量，零第三方依赖）——评审确认。
2. ~~save 默认~~ **按操作类型**（§3 表）——评审确认。
3. ~~轮询节奏~~ **每 5 tick + 单次限量 + 轮询线程**（§5）——评审确认。
4. **载体 A 最终拍板**——评审已按 A 展开，待用户一句确认即可开工实现。
