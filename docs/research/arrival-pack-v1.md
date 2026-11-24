# 到场包 v1（2026-11-25 自主窗口攒账——需要用户到场/活体游戏才能判决的项目清单）

> 用法：用户在场时按批次勾选执行；每项标注所需版本与前置。无头侧能钉的已全部钉完
> （守卫批 cffbf95 / D8 批 61a14e4 / 扩版批 5858e7a / 挖掘批 89273eb / D7 批 ac9afac /
> D10 批 ad67e1c setxp+giveeffect+removeeffect / D11 批 2d521af sethunger），本包=纯活体账。

## A. D8 weather 活体语义判决（b1.7.3 优先，10 分钟档）

- [ ] **浮值写是否被 tick 斜坡覆写**：`weather true true` 后观察雨是否真的起来并**持续**。
  无头证据（§6.1 追记）：World 浮值每 tick 按 WorldInfo 旗标 ±0.01 斜坡驱动，单写浮值
  理论必衰减。若判决成立→实装升级配方=写 WorldInfo 旗标+时间键（b1.7.3 ei 字母在档：
  l/m/n/o + getWorldInfo 取用器；b1.9-pre2 起有 public setter 更干净）。
- [ ] `setdifficulty 3` 活体语义：World 字段面写入后，客户端 GameSettings 门（怪Spawn/
  伤害系数）是否跟随——meta 轮判决"写无效"需活体复核一次。

## B. 扩版首波运行时启用（每版一 boot，b1.9-pre2 优先=新 op 富矿）

- [ ] **b1.9-pre2**：VersionRegistry.build19pre2（锚库件 b1.9-pre2.json 字母全量在册）
  + fake 断言批 + 活体钩四连验证（SAVE=ti.a(Lpd;)V 包装 / LOAD=Minecraft.a(Lti;…Luo;)V /
  TICK=k / HUD=adm，门=gk.E）。顺带 A 项的 setter 路径对照。
- [ ] **b1.5_01**：build1501 + 活体钩验证（⚠️ SAVE_WORLD=双参实体 a(ZLwr;)V 无包装=
  与 173 不同注入点形状，钩触发必须实机看）+ RUN_TICK=j；weather 门开（capWeather=TRUE，
  浮值 B/C/D/E 交叉斜坡观察=§6.1 判决复核）；存档 level.dat 里 NBT 键**小写**
  raining/rainTime 回读对账（D7 离线工具可代劳）。
- [ ] **b1.3_01**：build1301 + 钩验证（包装存在，173 配方适用）；weather/getbiome 门控
  响亮面复核（getbiome=MobSpawnerBase 系 hv.m 应可读出 "Plains" 类名）。
- [ ] 三版 `listops` 回声对榫：壳 44-op 镜像（getbiome 补洞+D10 三 op+D11 sethunger 后）vs agent LISTOPS 串。

## C. D10 新 op 管线前置取证（活体+无头混合）

- [ ] **xp**（b1.8+，D10 ad67e1c 已落无头 setxp）：活体=`setxp 10 5` 后 XP 条是否现
  10 级/进度 5、HUD 直读 aX 生效；写后 `save` 离线读 level.dat 玩家 NBT 三键
  **Xp/XpLevel/XpTotal** 对账（b1.8.1 aX/aY/aZ 直写面）；xpCooldown 写 0 后立即可再拾验证。
- [ ] **effect**（b1.8+，D10 ad67e1c 已落无头 giveeffect/removeeffect）：活体=
  `giveeffect 1 200 0`（速度）HUD 气泡现效、`removeeffect 1` 气泡消失；b1.8.1 无清空面
  （clearEffectsM null 判决）逐 id 移除即足——effect id 越界 [1,31] 响亮 REJECTED 复核。
- [ ] **hunger**（b1.8+，D11 2d521af 已落无头 sethunger）：活体=`sethunger 20 5` 后饱食
  条满/饱和生效，`sethunger 0` 观察饿死链（L37 food<=0 tickTimer 累加→attackEntityFrom）；
  food>=18 自然回血触发（L31）复核；写后 save 读玩家 NBT foodLevel/foodSaturationLevel 对账。
- [ ] **gethunger/getxp 只读扩展**（后续批候选）：现仅写面，读面=getplayer 扩字段
  （sz.aX/aY/aZ/ay + kc.a/b via at()）——活体确认读值与 HUD 一致后并入。
- [ ] **sleep/wake**（b1.5+）：旗标直写（uo.sleeping=aM / 1501 gh.x）能否在无 GUI 交互下
  触发黎明跳过（Minecraft tick 检 sleeping 的链路活体实锤）——成立则=time skip op 候选。
- [ ] **gamemode**（b1.8+，**未落无头——判决=非干净单字段面**）：b1.8.1 EntityPlayer 无
  public creativeMode 布尔（field_35216_aw 系计时器非旗标），需 WorldSettings.gameType=ek.b
  + sx.p 双层写序活体实钉再实装（capGamemode 槽仍"先行"，勿在无头硬凑）。
- [ ] **spawn kind=mob（19pre2）**：EntityCreature=vp abstract → 运行时须响亮 UNSUPPORTED
  （启用 build19pre2 时 mobClasses.mob=null 路径实机走一遍）。

## D. D7 存储写侧活体对账

- [ ] agent `save` 后离线读 level.dat：WorldInfo 键值回写对账（time/spawn/seed；
  weather 键大小写断代 b1.5 小写→b1.7.3 大写=双版各验一次）。
- [ ] mcr 直读工具（RegionFile 锚双版在册：1301 mo / 19pre2 jz）对 agent 写后区块的
  往返一致性——无头部分可先行，活体部分=游戏内改动 vs 离线读出的字节级对账。

## E. 杂项

- [ ] InjectSmoke 全绿≠活体全绿：三新版 fake 批之后仍需每版一次真实 boot 冒烟
  （钩命中数 expectedHits=1 实机回声）。
- [ ] HookSnapshot 基线（111 行）在扩版后未漂移=无头证据；活体首 boot 时快照对账一次。
