# 设计案卷 — 游戏内 Overlay 与控制台（输入/输出通道代际策略）

日期：2026-11-13 ｜ 状态：**调研结论落盘，细则待多版本实勘修订**（按版本索引逐代复核后更新，见 §7）
前置：[phase0](../research/phase0-agent-poc.md)、[phase1](../research/phase1-first-brick.md)、[命令通道](agent-command-channel.md)

> 纪律声明：本卷所有"可读树"结论均已对照原始 jar 字节码复核的关键项会标注 [javap 实锤]；
> 未标注者为可读树证据，待 §7 实勘清单覆盖。**可读树是线索，原字节码才是证据。**

## 1. 输出侧：Overlay 三路线判决

| 路线 | 判决 | 理由 |
|---|---|---|
| A. OS 级透明 overlay 窗 | **否** | 额外进程+窗口跟随/穿透管理脆弱；数据本经通道出壳，多壳只为显示不值 |
| D. 窗口标题（`Display.setTitle` 追加 XYZ） | 过渡方案 | 零注入纯反射；LWJGL 类名不混淆；0327 游戏仅启动写一次标题（Minecraft.java L228），LoadingScreen 加载期会覆盖（L834）→ agent 每 N tick 夺回 |
| B. GuiIngame 尾部注入 `drawOverlay()` | **正解** | 首个渲染侧织入试点；方法末尾 GL 状态 = 游戏自绘 chat 字符串所用状态（实证照抄位零新风险）；tick 线程绝不碰 GL，绘制只发生在渲染线程钩内 |

**格式决策**：自绘 XYZ 采用 Alpha v1.2.3 原生格式（`x: <posX>` / `y:` / `z:` 三行，GuiIngame L167-169），**不另拟一套**。

**XYZ 需求窗口**：Classic → Alpha ≤ v1.2.2。≥ v1.2.3 原生自带：反射翻 `gameSettings.showFPS` 即得坐标 HUD（1.2.3 树核对：调试行含 `func_6241_m/o()` 系列+坐标三行；v1.2.0 树核对：无坐标行——下界成立）。此差异入版本兼容登记表"能力差异"栏。

## 2. 暂停模型（0327，字节码实锤）

- `isGamePaused = currentScreen != null && currentScreen.doesGuiPauseGame()`（Minecraft.java L372）——暂停**按屏幕类**决定。
- `GuiScreen.doesGuiPauseGame() = true`（基类）→ Esc 菜单冻世界；`GuiContainer = false`（L253）→ **背包开着世界照走**——"释放鼠标+文本输入+世界不冻"是引擎原生模式，控制台方案合法性来源。
- applet 侧 `MinecraftApplet.stop()` 直接置 `isGamePaused=true`（L57-59）——**待实跑**：Attic 容器失焦/Alt+Tab 是否触发 stop()（决定外部壳过渡方案可用性）。

## 3. Chat 考古（断代化石）

**0327 残桩 [javap 实锤]**：GuiIngame 真身 `net.minecraft.client.c.m` 的 `a(F)V`（renderGameOverlay）chat 段原字节码为
`getfield b:List → List.get(i) → pop → aconst_null → draw…(色值 14737632)`。
即：**取 ChatLine 当场丢弃、绘制入参硬编码 null**——非反编译伪证，是 Notch 摘除 `chatline.message` 的手术痕迹。
推论：①`drawString(null)` 执行必 NPE，整代 Infdev 没炸 ⇒ 列表恒空 ⇒ **0327 无任何隐藏写路径**（运行时反证，比"无 GuiChat 类"更硬）；②搭车 chat 输出 = 给雷管引信，**禁写 chatMessageList**。

**三态断代指纹**（可作版本代际判别特征，入登记表）：
1. Classic：渲染完整（0.0.17a Minecraft.java L988-990 真绘 `ChatLine.message`）；
2. Indev/Infdev：`get→pop→aconst_null` 自废桩；
3. Alpha ≥1.0.10：复活（1.0.14-2 树：GuiChat/T 键 L864/`EntityPlayerSP.sendChatMessage` L106/`instanceof GuiChat` 分支）。

**0327 混淆实名表（B 路线指纹半齐）**：GuiIngame=`c.m`、renderGameOverlay=`a(F)V`、updateTick=`a()V`、chatMessageList=`b`、Minecraft=`net.minecraft.client.d`、Gui=`c.l`、FontRenderer=`c.k`。

**半废通道处理判决：彻底绕开（选项 3）**。修复残桩=中段字节码手术（否）；自唤起复用 ChatScreen 仅限 Classic 代。0327 输出全走 B 路线尾钩 `drawOverlay()`——**一个注入点两用**（坐标行+命令结果行共用），chat 桩永不触碰。核心纪律：**不替 Notch 收尾拆迁**。

## 4. 输入侧：代际策略

| 代 | SP 唤起 | SP 发信 | 注入配方 |
|---|---|---|---|
| Classic 0.0.16a_02 | T 原生可开屏（无闸门） | Enter 触雷 NPE | **只装发信阀** |
| Classic 0.0.17a~0.0.23 | **闸门**：`KEY_T && connectionManager!=null && isConnected()`（L655），T 死键 | 同左（发送端原样带雷） | 自唤起 + 发信阀 |
| Indev/Infdev（0327） | 无 ChatScreen 类 | 无 | ①runTick 尾钩死键唤起 ②**运行时 ASM 生成 ConsoleScreen**（extends GuiScreen + doesGuiPauseGame=false + GuiTextField）——首个类生成，风险面大一档，排最后 |
| Alpha ≥v1.0.10 / Beta | 原生全通 | 原生（集成服广播回显） | **数据层一个钩**：`EntityPlayerSP.sendChatMessage`，`!` 前缀吞、其余透传——UI 零动，最干净 |

**0.0.17a 绕闸配方**（不碰闸门判定，零注入点唤起）：
1. 唤起：现有 runTick 尾钩 drain 内 `Keyboard.isKeyDown(KEY_T)` **上升沿检测**（状态轮询不抢事件队列；单人下游戏闸门自行不放行，无冲突）。条件 `screen==null && 单人` → 反射 `player.releaseAllKeys()`（照抄 L656 防带键穿墙）→ `mc.setScreen(ChatScreen.newInstance())`（public 无参构造 [树]；白名单串含 `!` [树]，前缀可输入）。
2. 发信阀：钩 `ChatScreen.keyPressed` **入口守卫**：`INVOKESTATIC guard(this)` → 非零 `RETURN`（早退吞掉）。SP：读 typedMsg → `!` 前缀入命令队列 → 关屏；其余透传。**首个入口守卫钩**——无新栈操作、无新帧，老验证器友好，比中段改条件浅一个数量级。
3. 输出：反射 `addChatMessage`（L1182），结果落 chat 行。

## 5. 战略结论

桌面壳拆两层：**输入壳**（游戏内控制台，上表代际配方）与**显示壳**（B 路线 HUD / ≥1.2.3 原生 showFPS）。游戏内控制台优先于外部壳——用户不出游戏窗口即可发令，鼠标问题被 `doesGuiPauseGame()=false` 原生解决。命令执行体仍走反射+tick 边界（不违 [命令通道](agent-command-channel.md) 戒律 2——本卷新增注入面仅：B 尾钩×1、Classic 入口守卫×1、Infdev 类生成×1，全部走指纹表+两遍核验）。

## 6. 排期

命令通道（文件队列细则待拍板）→ B 路线 HUD 尾注入试点（0327）→ Classic 0.0.17a 绕闸配方（自唤起+发信阀）→ Infdev ConsoleScreen 类生成 → Alpha/Beta `sendChatMessage` 数据层钩推广。

## 7. 待实勘修订清单（按版本索引逐代过）

- [ ] 0327 applet 失焦/Alt+Tab 是否触发 `stop()`→暂停（§2）
- [ ] `isKeyDown` 上升沿在按住 T 移动时的误触率（§4 配方 1）
- [ ] ChatScreen/setScreen/releaseAllKeys/addChatMessage 混淆实名逐版本 javap（§4，教训 7）
- [ ] 0.0.16a_02 无闸门形态原字节码复核（§4 表首行，现为树证）
- [ ] Classic 各代 ChatScreen 白名单/长度上限逐版本核对（`!` 可输入性，§4）
- [ ] Alpha v1.0.10 边界树缺失（本地索引最近为 1.0.1_01/1.0.4~1.0.6/1.0.14-2），1.0.6→1.0.14 间发信恢复时点待补 jar 复核
- [ ] ≥1.2.3 `showFPS` 键位与 GameSettings 混淆名 javap（§1 白嫖路线）
- [ ] Beta/Release 代 `EntityPlayerSP.sendChatMessage` 实名与命令前缀冲突（原版 `/` 指令出现后的前缀设计）
- [ ] ConsoleScreen 运行时类生成的验证器兼容性试点（§4 表第三行，最大风险项）
