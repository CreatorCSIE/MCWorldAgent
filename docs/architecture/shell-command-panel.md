# 设计案卷 — 桌面壳命令台面板（VS Code 式底部面板）

状态：**已实现（2026-11-14 T1 落码，静态验证全过：无头 128/128+浏览器走查 6/6+cargo check；实跑 `t1-shell-1` 待批准）**｜ 落地 spec=[.trae/specs/shell-t1-command-panel/spec.md](../../.trae/specs/shell-t1-command-panel/spec.md)（含决策完备化与实跑修订）｜ 日期：2026-11-13
依赖：[agent-command-channel.md](./agent-command-channel.md)（v1 已实跑）、[ingame-console.md](./ingame-console.md)（游戏内控制台为长期替代方向，本面板是其外部过渡形态）
来源：用户提议"在桌面壳接入类似 VSCode 的下方终端+日志二合一功能"

## 1. 形态

照搬 VS Code 底部面板语义：

- **面板两标签**：`命令台`（输入行 + 历史）｜`事件流`（合流日志）
- **状态栏一条**：世界名 / tick 数 / 通道目录 / 游戏进程状态（存活/标题/世界中）

## 2. 命令台 = 速记翻译器（不是真 PTY）

输入速记 → 壳补全封包（`protocolVersion`、生成唯一 `id`、`save` 按 op 默认表）→ tmp→inbox 原子投递：

```text
place 2 71 2 4      → {"protocolVersion":1,"id":"<ts>-<seq>","op":"place","args":{"x":2,"y":71,"z":2,"blockId":4},"save":true}
dig 0 71 0          → op=dig（save 默认 true）
getblock 0 71 0     → op=getblock（只读，save 忽略）
save                → op=save
```

速记→封包映射表 = 通道案卷 §6 操作表的镜像，**零新协议**。未知 op/参数不足在壳侧预检并提示，但 agent 侧校验仍是唯一权威（壳预检只为体验）。

## 3. 事件流 = 双源合流

- **源一**：tail `agent-hooks.log`（WORLD_CREATED/WORLD_SWITCHED/SAVE_WORLD/CHANNEL_ACCEPT/CHANNEL_DONE/TICK 边界/崩溃分类行）——评审建议的"命令台订阅 Agent 事件"全部已是日志行，**无需另造事件系统**
- **源二**：watch `outbox/*.json`（ACCEPTED→终态变化），按 `id` 对齐回命令台输入行做回声（输入行状态：已投递→ACCEPTED→OK/REJECTED/…）
- Windows 下 rename 语义对 fs event 不稳，**两源都用轮询**（与 agent 侧同构，250ms 级）

## 4. 纪律延伸（把口头约定做成 UI 状态）

- 壳发的每条命令进 run 归档：命令台历史 = 案卷素材（与 CLI 重定向同等待遇）
- **kill×存盘竞态教训（Phase 1 案卷）UI 化**："停止游戏"按钮根据事件流推断状态——世界中标红需先"请 Esc 存退"，标题界面才允许直接停；宁可按钮不可用，不做静默强杀
- 探针坐标避开 FirstBrick 锚点（教训 9）：命令台默认提示语可配置化，fixture 参数 `noFirstBrick=`（评审 P0）由壳在启动命令里默认带上

## 5. 边界与不做的事

- 面板**不**做真 PTY/进程 stdin 注入（stdin 路线已被通道案卷排除：CLI 占用）
- 面板**不**反向修改 Core 命令协议（评审同见）：协议演进走通道案卷，面板只跟随
- 不做游戏窗口内渲染（那是 ingame-console 的 HUD/ConsoleScreen 方向；两者共存：壳面板=操作者工具，游戏内控制台=玩家/演示路径）

## 6. 实现要点（Tauri 壳内）

- 前端：面板组件（标签页 + 虚拟滚动日志 + 输入行历史↑↓）；速记解析器（纯函数，可单测）
- 后端（Rust 或 Tauri command）：文件轮询 watch（inbox/outbox/hooks.log 增量读，记录 offset）、封包生成（id=时间戳+序号，幂等友好）、tmp→rename 投递
- 与 `apps/shell` Phase 1 静态原型合并布局：底部面板可折叠，视口（未来 Three.js）在上

## 7. 排期

通道 v1 已具备全部对接面（协议/目录/状态语义），面板无 agent 侧依赖，可与 P0 fixture 化并行开工；顺序建议：P0 `noFirstBrick=`（半天）→ 面板骨架 → settime/setspawn/tp（javap 钉名后同步进速记表）。
