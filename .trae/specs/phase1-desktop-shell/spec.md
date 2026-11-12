# Phase 1 桌面壳（Tauri 静态原型 + mock 数据） Spec

## Why

plan.md §3.2 "UI Prototype Stage" 要求在 Java Core 集成之前先用静态原型验证应用布局与交互模型（Blender/VS Code 混合工作区范式）。桌面壳是后续所有里程碑（真数据视口、agent 面板、编辑工作流）的地基；壳的取舍已拍板：**Tauri 单壳，不走 Attic 式双壳，不做 Electron 评估**。

## What Changes

- 新建前端 monorepo 地基：`apps/desktop`（Vue 3 + TypeScript + Vite + Tauri 2）与 `packages/ui`（shadcn-vue 组件基座 + 暗色桌面编辑器主题 token）。
- 实现主窗六区布局：Top Bar / 左面板 / 中心视口（**占位**）/ 右 Inspector / 底部 Console / 状态栏，面板可拖宽、可折叠。
- 实现三工作区切换：World Explorer / World Analysis / Runtime，全部由 **mock 数据**驱动（假世界树、假 Inspector 字段、假日志流、假连接状态）。
- `pnpm tauri dev` 下验证布局（环境已具备：cargo/rustc 1.99、node v25、pnpm 10）。
- plan.md §3.2 UI Prototype Stage 清单对应项打勾；"Evaluate Electron compatibility" 两项标记 **取消（shell 钉板）**。

**不在本里程碑范围**：Three.js 真实渲染、Java Core/agent 通信、真实世界数据、版本登记表 UI、编辑操作。视口只做占位符（网格线框风格），这符合 §3.2 "without requiring real Minecraft data" 的原始定义。

## Impact

- Affected specs：Phase 0（已闭环，无冲突）；本 spec 是 plan.md §3.1/§3.2/§9 的首个实现落点。
- Affected code：新增 `apps/desktop/`、`packages/ui/`、根 `pnpm-workspace.yaml`、`package.json`；修改 `plan.md`（清单打勾）、`.gitignore`（node/dist/target 忽略）。
- 现有资产零改动：`java/agent/`、`integrations/attic/` 不受影响。

## ADDED Requirements

### Requirement: 静态桌面壳可运行
系统 SHALL 提供可通过 `pnpm dev`（浏览器）与 `pnpm tauri dev`（桌面窗）启动的 Vue 3 + Tauri 应用，加载即呈现六区布局。

#### Scenario: 启动验证
- **WHEN** 在仓库根执行 `pnpm tauri dev`
- **THEN** Tauri 窗口打开，六区布局完整渲染，无控制台错误，缩放窗口布局自适应

### Requirement: 面板可调整可折叠
系统 SHALL 提供拖拽分隔条调整面板宽度/高度、双击或按钮折叠/展开左右面板与底部面板的能力（Blender 式工作区纪律）。

#### Scenario: 交互验证
- **WHEN** 拖动左面板分隔条并点击折叠按钮
- **THEN** 面板宽度实时变化且视口区自动补足；折叠后该区消失、展开恢复

### Requirement: 三工作区切换 + mock 数据
系统 SHALL 提供 World Explorer / World Analysis / Runtime 三工作区切换（共享同一应用壳），各区内容全部来自类型化 mock 数据（`packages/ui` 或 app 内 `mock/` 模块），并预留真实数据替换边界（数据源接口与展示组件分离）。

#### Scenario: 工作区验证
- **WHEN** 切换三个工作区标签
- **THEN** 左面板内容随工作区变化（世界树/分析图层列表/agent 状态），Inspector 显示选中 mock 对象的字段，Console 滚动 mock 日志，状态栏显示 mock 坐标与连接状态

### Requirement: 暗色桌面编辑器主题
系统 SHALL 以 shadcn-vue 为组件基座建立暗色主题 token（桌面编辑器风格，非 admin dashboard 亮色默认），约定字体与排版层级。

#### Scenario: 主题验证
- **WHEN** 应用启动
- **THEN** 全局为暗色主题，组件来自 shadcn-vue（Button/Tabs/ScrollArea/Separator 等），无默认亮色残留

## MODIFIED Requirements

（无——本项目尚无已发布能力被修改）

## REMOVED Requirements

### Requirement: Electron 兼容性评估（§3.2 原型清单末两项）
**Reason**: shell 钉板为 Tauri only（plan.md §11 "Desktop shell DECIDED"），不再走双壳。
**Migration**: 相关清单项在 plan.md 中标注 CANCELLED 而非删除，保留决策痕迹。
