# 设计案卷 — 桌面端 Workbench 模型（VSCodium-inspired 提案复核判决）

状态：**判决落档**（2026-11-14，用户拍板=分层采纳）｜ 来源：外部提案（ChatGPT）"以 VSCodium 交互模型为蓝本、Vue 3 + shadcn-vue 自实现 Workbench、Three.js 核心 3D Editor"｜ 复核对照：[phase1-desktop-shell notes](../ui/phase1-desktop-shell/notes.md)（原型已验收）、[shell-command-panel](./shell-command-panel.md)（Phase 2 首件）、[ingame-console](./ingame-console.md)（§5 战略结论）

## 1. 复核结论：方向追认，非新转向

提案与既有决策同构的部分（**采纳并确认**）：

- **技术栈**：Vue 3 + TS + Tailwind + shadcn-vue + Tauri 2 = 已验收原型现状（notes §1），无需变更。
- **Workbench / Editor / World API 三层分离**：与既有 `WorldSource` 接口纪律同构（展示组件只依赖接口、mock 转正接口不动、Editor 不持 Java Core 内部对象）。Editor 注册制=agent 侧"指纹表+注册表"文化的前端镜像——新增 Structure Locator Editor 只注册类型，不改 Workbench。**此为提案最有价值部分，定为桌面端架构不变量。**
- **Three.js=内置原生 Editor，Blender=外部 3D 工作流**：与现有边界一致；Blender 不参与日常 WorldSession、不嵌入 Vue。
- **多编辑器类型承载**（3D Viewport / NBT / Chunk / Entity / Properties / Analysis）：与三工作区（World Explorer / World Analysis / Runtime）既有划分兼容，Activity Bar 语义不变。

## 2. 修正三条（不采纳提案原文）

1. **Tauri 不是"可选"**：单壳 Tauri 已 DECIDED（Electron 评估 CANCELLED，`pnpm tauri dev` 两轮开窗验证）。提案"后续再决定是否需要"系信息缺失，驳回。
2. **"不做 Dashboard"批评对象不存在**：已验收原型即六区 Workbench 骨架（TopBar/左面板/视口/Inspector/Console/状态栏），非 Dashboard 堆页面。
3. **七项交互能力优先级倒挂**：多 Editor Group 分屏、Tab 跨组拖放、Command Palette、Layout Persistence 均为壳打磨项；桌面端当前**零真数据**（无 Three.js 真渲染、无 agent 通信）。按"先真数据后打磨"排序，见 §3。

## 3. 分层排期（用户判决=分层采纳）

| 层 | 内容 | 依赖 | 备注 |
|---|---|---|---|
| **T1 命令台** | [shell-command-panel](./shell-command-panel.md) 全案：速记翻译器+双源轮询（hooks.log + outbox）+kill×存盘竞态 UI 化 | 通道 33 op 对接面全具备，**agent 侧零依赖** | 首件功能件，先于一切 Workbench 升级 |
| **T2 Editor 系统** | Editor 注册制重构（三层分离落地）+ agent 只读导出真世界 → Three.js 真渲染（notes §7 原案）+ Pinia（Workbench 状态）/ VueUse（快捷键）引入 | T1；chunk 导出面（新 agent 批，另立 spec）。**2026-11-14 定性修正：plan.md §12 垂直切片唯缺步 2/3/4（读世界+3D 显示）=T2 为里程碑收口件非壳打磨；导出批硬约束=只读禁隐式生成（audit-0327 §6，chunkExists 门控/直读盘）** | Pinia/VueUse 到本层才进，防过早抽象（"不建 packages/ui"同纪律） |
| **T3 Workbench 升级** | 多 Editor Groups 分屏、Tab 跨组拖放、Command Palette、Layout Persistence | T2（≥2 种真 Editor 才有分屏需求） | 提案 §3 七项中后四项全部归此层 |

## 4. 队列与边界（不变项）

- **命令通道主队列修正（2026-11-14 用户判决）**：**0327 World 接口覆盖审计**升为开工项（全表面 javap 对照 33 op→三分判决表：已覆盖/缺口→值得 op/缺口→不做有因；吸收 grow/harvest 存废+红石/biome/结构/TE 全集/chunk 面疑点）；**版本扩展试点=跨阶段版本各选代表**（Classic/Indev/Infdev 后段/Alpha/Beta 具体选版待审计完单独拍板），**0227/0313 降次优先**；桌面端 T1 可与 agent 批并行（互不依赖），T2/T3 不挤占通道取证产能。
- **ingame-console 战略结论维持**：游戏内控制台（输入壳）优先于外部壳的判决不改——桌面命令台=操作者工具，游戏内=玩家/演示路径，两者共存（shell-command-panel §5）。
- client-ops 永不进队列；committer/实跑批准等戒律照旧。

## 5. 后续动作

- 开工 T1 时另立 spec（`.trae/specs/`），本卷仅作架构判决；
- T2 的 Editor 注册制接口设计（注册表形状、Editor 生命周期、WorldSource→World API 桥）在 T1 验收后开研究卷。

## 6. 二次提案复核（2026-11-14，对象驱动 UI 提案，用户判决=追认+Registry 源 B 路线）

外部提案（ChatGPT 第二轮：领域划分/对象 Inspector/视口三态/HUD 分层/布局示意/Operation Registry 五态）复核结论——**无战略冲突，全部素材落 T2 层，T1/T2/T3 排期不变**：

- **追认项**：①§1 领域划分表（World/Properties/Terrain/Player/Inventory/Entity/TE/Runtime/Observability 九域）= T2 Editor 注册制的**分区输入**，与 architecture §6 op 表天然同构；②对象驱动 Inspector 的数据契约**全部已有实跑钉版真值**（Furnace 3 槽+BurnTime/CookTime private 字段面=te-1、Player pos/motion/rotation/health=tp/llm/give 批、entitylist kind/坐标=obs-1）——Inspector=这些 op 输出的编辑器化，非新能力面；③视口三态（Inspect/Select Region/Analyze）与区域语义 op 匹配；④HUD 轻量/桌面全功能/Runtime 低层三分=ingame-console §5 与 shell-command-panel §5 既有结论的重述，对 hud-overlay 实现描述逐条准确。
- **钉版事实 UI 化**：getblocks **非零副作用**（provider 对未加载坐标就地生成整 chunk，cap 4096=限幅，architecture 已钉）→ 区域操作预览必须显示潜在 chunk 生成影响。T2 需求细节。
- **判决（本轮唯一新增架构件）**：Operation Registry 五态元数据（Available/Applicable/Destructive/Persistent/Transient）的数据源=**路线 B：listops v2 协议演进**——agent 侧结构化能力元数据（op→domain/persistence/destructive/version-gate），列为 **T2 前置 agent 批**（纯读侧零注入，另立 spec，协议格式走通道案卷——"面板只跟随协议"纪律）。理由：能力元数据唯一权威必须在 agent 侧，与"结构面以 jar 为准"同文化；UI 硬编码（路线 A）=版本扩展后的漂移源，驳回。Persistent/Transient 语义的 ground truth 已由实跑钉版（实体即失/TE 持久/Player 独立段/i-frame+playsound 瞬态），v2 能力表即这些事实的结构化转写。
- 排期影响：T1（命令台）不变先行；listops v2 批在 T2 开工前落通道队列（与版本扩展批的相对次序届时单独拍板）。
