# Checklist — Phase 1 桌面壳（静态原型 + mock）

- [x] `pnpm dev` 在浏览器中启动，六区布局（TopBar/左/视口占位/右/底/状态栏）完整渲染，无控制台错误
- [x] `pnpm tauri dev` 打开 Tauri 原生窗并呈现同一布局（若 Rust 链接环境阻塞，记录具体报错并保留浏览器路径全绿）
- [x] 左/右/底面板可拖拽分隔条调尺寸，可折叠与恢复，视口区自动补足空间
- [x] 视口为占位符（网格线框 + 水印），未引入 Three.js 依赖
- [x] 三工作区（World Explorer / World Analysis / Runtime）可切换，共享同一应用壳，左面板内容随工作区变化
- [x] Inspector 显示 mock 选中对象字段；Console 滚动 mock 日志且可暂停；状态栏显示 mock 坐标与连接状态
- [x] mock 数据形状以真实资产为参照（infdev 20100327、World2、base36 区块路径、agent 字段名），且数据源接口与展示组件分离（存在明确的可替换真实数据边界）
- [x] 全局暗色桌面编辑器主题（shadcn-vue CSS 变量），无亮色 admin 默认残留；Console/状态栏使用等宽字体
- [x] 组件来自 shadcn-vue 基座，未自造基础控件
- [x] `.gitignore` 覆盖 node_modules/dist/target，构建产物未入库
- [x] plan.md §3.2 UI Prototype Stage 清单项已打勾，Electron 两项标注 CANCELLED（shell 钉板）
- [x] 截图与简短案卷记录落 `docs/ui/phase1-desktop-shell/`
- [x] 现有资产零破坏：`java/agent/`、`integrations/attic/` 未改动，Phase 0 提交历史完整
