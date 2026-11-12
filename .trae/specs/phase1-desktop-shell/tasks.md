# Tasks

- [x] Task 1: Monorepo 地基
  - [x] SubTask 1.1: 根 `package.json` + `pnpm-workspace.yaml`（apps/*、packages/*）
  - [x] SubTask 1.2: `apps/desktop`：Vite + Vue 3 + TypeScript 脚手架，`pnpm dev` 可起空白页
  - [x] SubTask 1.3: `.gitignore` 增补 node_modules / dist / src-tauri/target
- [x] Task 2: Tauri 2 接入（依赖 Task 1）
  - [x] SubTask 2.1: `apps/desktop/src-tauri` 初始化（tauri.conf.json、identifier、窗口尺寸 1280x800）
  - [x] SubTask 2.2: `pnpm tauri dev` 打开原生窗并加载 Vite dev server
  - [x] 风险注记：若链接期缺 MSVC Build Tools 导致 cargo 编译失败，记录阻塞并保证 `pnpm dev` 浏览器路径完整可用（Tauri 验证顺延）
- [x] Task 3: 组件基座与主题（可与 Task 2 并行，依赖 Task 1）
  - [x] SubTask 3.1: `packages/ui`：shadcn-vue 初始化（Tailwind + CSS 变量主题）
  - [x] SubTask 3.2: 暗色桌面编辑器主题 token（背景分层级、强调色、等宽字体用于 Console/状态栏）
  - [x] SubTask 3.3: 引入本里程碑所需组件：Button/Tabs/ScrollArea/Separator/Resizable（或等效）/Tooltip
- [x] Task 4: 六区布局壳（依赖 Task 1、3）
  - [x] SubTask 4.1: 主窗网格：TopBar / Left / Viewport / Right / Bottom / StatusBar
  - [x] SubTask 4.2: 分隔条拖拽调宽/高 + 折叠/展开（左、右、底部三区）
  - [x] SubTask 4.3: 视口占位符（网格线框风格 + "3D viewport placeholder" 水印，不引 Three.js）
  - [x] SubTask 4.4: 窗口缩放自适应、最小尺寸约束
- [x] Task 5: 三工作区 + mock 数据（依赖 Task 4）
  - [x] SubTask 5.1: mock 数据模块：类型定义（WorldInfo/ChunkInfo/Entity/AgentStatus）+ 假数据（以真实 World2 的形状为参照：base36 目录、1729 chunk 计数等）
  - [x] SubTask 5.2: 工作区切换（TopBar 或左侧顶部）：World Explorer / World Analysis / Runtime 共享同一壳
  - [x] SubTask 5.3: 左面板三态内容：世界树（Explorer）/ 分析图层列表（Analysis）/ agent 连接卡（Runtime）
  - [x] SubTask 5.4: 右 Inspector：显示当前 mock 选中对象字段
  - [x] SubTask 5.5: 底部 Console：mock 日志流（可暂停滚动）；状态栏：mock 坐标/模式/连接状态
  - [x] SubTask 5.6: 数据源接口与展示组件分离（真实数据替换边界：`useWorldSource()` 组合式函数返回 mock 实现）
- [x] Task 6: 验证与文档回写（依赖 Task 2、4、5）
  - [x] SubTask 6.1: `pnpm tauri dev` 下走查 checklist 全部项；截图归档 `docs/ui/phase1-desktop-shell/`
  - [x] SubTask 6.2: plan.md §3.2 UI Prototype Stage 打勾（Electron 两项标 CANCELLED）、§11 表 UI 行更新状态
  - [x] SubTask 6.3: 案卷式简短记录：`docs/ui/phase1-desktop-shell/notes.md`（布局决策、mock 边界、遗留）

# Task Dependencies

- Task 2、3 依赖 Task 1，且相互可并行
- Task 4 依赖 Task 1、3；Task 5 依赖 Task 4；Task 6 依赖 Task 2、4、5
- 里程碑边界：本 spec 不含 Three.js 真实渲染与任何 Java/agent 通信（第二里程碑另立 spec）
