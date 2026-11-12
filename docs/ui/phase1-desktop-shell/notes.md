# Phase 1 桌面壳（静态原型 + mock）案卷

日期：2026-11-12 · Spec：`.trae/specs/phase1-desktop-shell/`（已验收）· 范围：plan.md §3.2 "UI Prototype Stage"

## 1. 交付形态

`apps/desktop`（pnpm workspace）：Vue 3.5 + Vite 8 + TS 5.9 + Tailwind v4 + shadcn-vue（reka-nova）+ Tauri 2。六区布局（TopBar / 左面板 / 视口占位 / Inspector / Console / 状态栏）+ 三工作区（World Explorer / World Analysis / Runtime）+ 类型化 mock。视口为 CSS 网格线框占位，**无 Three.js、无任何 Java/agent 通信**（有意边界）。

## 2. 关键决策

- **单壳 Tauri**：不走 Attic 双壳，Electron 评估项 CANCELLED（plan.md §11 "Desktop shell DECIDED"）。
- **不建 packages/ui**：本里程碑组件全部收敛在 apps/desktop/src，跨包抽象等真实复用需求出现再做（防过早抽象）。
- **mock 形状 = 真实资产形状**：`infdev 20100327`、`World2`、base36 散件路径、chunk 1729、Phase 0 真实钩名（world-ctor/save-world/run-tick/load-chunk/gen-height，逐条对照 HookTargets.java）、把手①两半 capturedSaveDir/capturedWorld、G1 legacyfix 5 分钟代价。mock 转正时接口不动。
- **数据边界**：展示组件只依赖 `WorldSource` 接口（`src/mock/types.ts`），`useWorldSource()` 当前返回 mock 单例。真实通道（Java Core / agent bridge）接进来时替换实现即可。

## 3. 踩坑：折叠按钮静默失效（reka-ui 2.11 + shadcn-vue 模板）

症状：点 Collapse 无任何反应、无报错。根因：shadcn-vue 生成的 `ResizablePanel.vue` 调 `useForwardExpose()` 但**丢弃返回值**；reka-ui 2.11 的实现要求把返回的 `forwardRef` 绑到子组件 `:ref` 上，子级 exposed（`collapse/expand/isCollapsed`）才会合并进包装组件。未绑定 → `api.collapse` 为 undefined → App.vue 的可选链 `api?.collapse?.()` 静默吞掉。
修复：`const { forwardRef } = useForwardExpose()` + `<SplitterPanel :ref="forwardRef">`（见 `src/components/ui/resizable/ResizablePanel.vue`）。
教训：**可选链调用暴露方法是静默失败的温床**——跨组件 API 调用失败要能炸出来；浏览器走查发现"点击无反应"必须做单步定测（此前一轮走查因连点多次 collapse/expand 互相抵消产生过误报，单步复测才钉死）。

## 4. 验证记录（浏览器走查，窗口 1280+）

| 项 | 结果 | 证据 |
|---|---|---|
| 六区布局暗色渲染、控制台零错误 | PASS | `mcworldagent-desktop-editor-fullpage.jpg` |
| Console mock 日志流递增、暂停/恢复 | PASS | 86→88 停增、88→122 恢复 |
| 三工作区切换、左面板内容联动 | PASS | `mcworldagent-runtime-workspace.jpg` |
| 树点选 → Inspector 字段 | PASS | chunk (0,0) 字段渲染 |
| 状态栏坐标/tick 递增 | PASS | TICK 1132→1412 |
| 左/右/底折叠+展开（修复后） | PASS | 399→54px、417→54px、269→35px，视口补足 |
| `pnpm --filter desktop build`（vue-tsc） | PASS | 2757 modules，零错误 |
| `pnpm tauri dev` 开窗 | PASS | 见 §5 |

## 5. Tauri 验证

- 地基阶段（T2）：cargo 首次编译 2m33s，`MainWindowTitle = "MCWorldAgent"` 进程存活。
- 布局完成后复跑（同日）：增量编译后 Tauri 窗再次开窗成功（PID 27636，title "MCWorldAgent"），加载完整六区布局版前端；物理拖拽确认建议直接在该窗口人工试（验收时窗口保留在桌面）。

## 6. 遗留（不阻塞验收）

1. **物理拖拽未自动化验证**：browser 工具无原生 drag，合成 pointer 事件不被 reka splitter 捕获；折叠与尺寸分配机制已验，拖拽把手渲染正常，建议人工在 Tauri 窗里拖一次确认。
2. 面板默认尺寸为百分比（22/23/23%），与 280/300/160px 仅在 1280 附近近似；需要精确像素默认时改 sizeUnit。
3. `main.css` 顶部 shadcn-vue CLI 注入的 Google Fonts 远程 `@import`（Geist）在离线 Tauri 静默回退系统字体，后续本地化。
4. Console 折叠会卸载订阅（展开时 history 回放）；mock source interval 常驻不清理——原型阶段接受，真实通道接入时重定。
5. run3 走查曾报"折叠后视口未补足"，修复后单步定测证实折叠链路当时整体失效——该观察系同一 bug 的误读，已闭环。

## 7. 下一里程碑（另立 spec）

agent 只读导出真世界（chunk 方块数组→JSON）→ Three.js 视口真渲染；Console 数据源换真 agent 日志流。
