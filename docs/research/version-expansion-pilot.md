# 案卷 — 版本扩展跨阶段试点调研（version-expansion-pilot，2026-11-14）

状态：**调研毕**（9 代表版本 × 接口+指纹全钉，详表=`docs/compatibility/verscan/*.md` 8 份；实跑零——纯离线 javap 调研，实跑属后续逐版接入批）｜ 触发：用户判决"版本扩展=跨阶段代表版本，0227/0313 次优先"并指定选版：Classic 0.30-c / Indev 0223 / Infdev 0618+0629 / Alpha 1.1.2_01+1.2.6 / Beta 1.7.3+1.8.1（0327=既有基线）

## 0. 弹药与身份（jar 闸门）

9 版 jar 全本地在位，索引行三态=**九件全部"有对应jar，为原始版本"**（version-index 逐行核对，无高仿件混入——本卷断代证据等级=原始件）。**SHA1 对榫表**（戒律 14 合规项，实算于本地件）：

| jar | SHA1 |
|---|---|
| c0.30-c-1900.jar | `51bc951530207b538596941a6f353f87dfc24233` |
| in-20100223.jar | `801ce486bb7fd1b43a56bc5d226dfb1370c08678` |
| inf-20100327.jar（基线） | `47b1b32430a211520993552ba0a5e00c1af44724`（与 phase0 案卷吻合） |
| inf-20100618.jar | `89eab2c1a353707cc00f074dffba9cb7a4f5e304` |
| inf-20100629.jar | `eb50bce3cb542488b3039aa0f4c3c0ec7595ab24` |
| a1.1.2_01.jar | `daa4b9f192d2c260837d3b98c39432324da28e86` |
| a1.2.6.jar | `a68c817afd6c05c253ba5462287c2c19bbb57935` |
| b1.7.3.jar | `43db9b498cb67058d2e12d394e6507722e71bb45` |
| b1.8.1.jar | `6b562463ccc2c7ff12ff350a2b04a67b3adcd37b` |

跨目录 SHA1 对榫（.betacraft ↔ .minecraft 七版全同件；`.attic` 仅存 Indev/Infdev 早期+0327）。正式名修正：Classic 目标=`c0.30-c-1900.jar`（`c0.30-c.jar` 不存在；索引发布时刻 18:20 UTC，文件名 `-1900` 后缀系 jar 源命名，无冲突——索引不钉文件名）。全部 **class major=49**（Java 5 old verifier 世代统一——织入纪律全代适用，无 verifier 断代）。

## 1. 混淆拓扑 = 四代形态（指纹表机制的根本分型）

| 世代 | 版本 | 形态 | 钉法含义 |
|---|---|---|---|
| G1 Classic | c0.30-c-1900 | `com/mojang/minecraft` **可读+短名混合**（Entity/level/Level/Mob 可读，主循环=l） | 半可读：锚名直接给，但字段/方法短名需钉 |
| G2 包路径混淆 | in-20100223、inf-20100327、inf-20100618 | `net.minecraft.a.a.f` 式**包路径+短名** | 0327 指纹表机制原生世代；**同字母跨版异义**（0618 `a.b.k`=Item 基类≠0327 ItemStack） |
| G3 扁平混淆 | inf-20100629、a1.1.2_01、a1.2.6、b1.7.3、b1.8.1（**及整个正式版时代**） | 功能类移根目录**单/双字母无包**（`ez`/`cn`/`fd`/`rv`…）；0629 连 Minecraft 主类都是 `at`（a1.1.2 起主类回归可读 `net.minecraft.client.Minecraft`）。**延续性（用户指正+索引规则 7）**：自 c0.0.2a 起所有后续版本均经 ProGuard 混淆、中间无不混淆阶段——扁平散件形态一路延续进正式版（索引表覆盖至 1.0.0-rc2）；唯一例外=`b1.2_02-20110517` 内部开发者构建（788 条目类名全可读，可直接用可读名表） | 每 jar 一张独立字母表，**禁止跨版本复用**；锚定链=可读入口→字段图→字符串常量→注册表数组 `[L..;` 全 jar 唯一性。**0629 转折语境（索引备注）**："开始测试离线客户端，并引入启动器"——G3 形态与启动器化同源；接入批启动通路 mainClass 须按启动器分发形态实钉 |
| — | （0227/0313 次优先未开工） | G2 同族 | 用户判决留后 |

**入口类包路径世系（用户指正，与功能类扁平化正交）**：Classic=`com.mojang.*`；**Indev 起=`net.minecraft.*` 此后不再变**——0629 功能类虽扁平化，入口锚 `net.minecraft.client.MinecraftApplet` 仍在原位（a1.1.2 起主类也回归该包）。索引规则 7 实证：0.31-20100104 反编译中 SoundManager=`net.minecraft.client.d.c`。锚定链第一环（可读入口）因此跨 G2→G3→正式版稳定可用。

## 2. 存储断代（数据侧 adapter 分型依据）

| 版本 | 格式 | 关键证据 |
|---|---|---|
| c0.30-c | **本地单文件 level.dat=ObjectOutputStream 序列化**（魔数 656127880，无 NBT！），float 坐标 **z 垂直轴**；`.mine`=0.30 **在线上传**形态（e/k "Save level" 屏，premium-only），非本地存档。**c/s 选型判据（索引备注）**：c0.30-c=默认创造模式、c0.30-s=默认生存，两者均"第一次引入防盗版机制"——选件能力矩阵差异仅在默认游戏模式闸 | level.c extends ObjectInputStream；l.class 常量池 `level.dat` 存读双点；e/k `.mine`×2+「save your levels online」；c/s 判据=索引备注（维护者总结） |
| in-20100223 | **`.mclevel` 单文件**（GZIP+NBT root "MinecraftLevel"），chunk 内联 byte[]，**getBlockId 返回 byte**；**mclevel 断代在 0122**：索引备注（维护者总结）钉 in-20100122-2251"引入 mclevel，仅离线保存；0.31 在线存档自首个构建已失效"；0122 前件扫描零 mclevel 串佐证 | 序列化器 a.a.a.a(Level,OutputStream)V；扫描佐证件三态：in-20100114/0122=**高仿重建**（结论与索引备注吻合），in-20100125=原始件旁证；in-20100101 索引未列=按不存在处理 |
| inf-20100227–0325（0327 前全区间） | **无存档链路（mclevel 已废）**：`level.dat`/`MinecraftLevel`/`tmp_chunk`/`SpawnX` 四探针全零（SpawnX 零=World 无 NBT 存盘键，结构面实锤）；仅 `client/c/e`(加载 GUI)+`c/q`(FilenameFilter) 带 `.mclevel` **残留过滤串**（读写皆死、UI 未删净——按串钉存档面会误判） | 索引件全量扫描（version-index §4 为准枚举）：原始件 1433/0313/0316/0320/0321/0325-1640 同相+高仿 0325-1545 同相；0227-1414（高仿）本地无件。存盘定装=0327（索引备注：引入 Alpha 世界格式） |
| 0327/0618/0629 | level.dat + **chunk 散件 `c.<x>.<z>.dat`**（+tmp_chunk.dat）——Infdev 存盘复活代。**注意 Infdev 存储并非铁板**：0624 曾换 zone 格式（32×32 chunk 巨型文件零压缩，唯一载体），0625-1 即回滚散件、0625-2 删残骸（姊妹仓 zone 案卷源码闭环）——若覆盖扩至 0624，存储 adapter 另立一型 | 0327 已实跑钉死；0618 包 a.a.e；zone 案=姊妹仓 `minecraft-infdev-20100624-zone-format-analysis.md` |
| a1.1.2/a1.2.6 | **仍散件**——"Alpha=region" 假设**证伪**；a1.2.6 level.dat 含 "Dimension"（下界） | 全 jar 无 region/.mcr 字符串 |
| b1.0–b1.2_02 | **仍散件（Alpha 同形）**——Beta 世代存储断代在 **1.3**，非 Beta 起点（用户指正+补验） | b1.2_02 jar 全量扫描零 `.mcr`/`region` 串（仅 `tmp_chunk`@oj） |
| b1.3–b1.8.1 | **region `.mcr`**（b1.3_01=`nl` 类常量池 "region"+".mcr"；b1.7.3/b1.8.1 region 类同形）。mcregion 出自社区作者 Scaevolus（源码自述），**非 zone 续嗣**（并行发明）。**导出批纪律警示**：b1.3 自带 `SaveConverterMcRegion` 离线转换器——游戏打开散件旧档会**自动迁移落盘**，违反"只读禁隐式生成"，旧档只读作业必须副本先行 | b1.3_01 jar 扫描实锤；`.mca`(Anvil)=12w07a→1.3.1 才出现，Beta 全代无；converter=姊妹仓 zone 案卷 §8 |

## 3. 关键指纹速览（详表在 verscan/）

| 面 | c0.30-c | in-20100223 | inf-20100618 | inf-20100629 | a1.1.2_01 | a1.2.6 | b1.7.3 | b1.8.1 |
|---|---|---|---|---|---|---|---|---|
| World/Level | level/Level `getTile(III)I`/`setTile(IIII)Z` | `a.a.g` `d(III)B`! | `a.a.g` `a(III)I`/`c(IIII)Z` | `ez` `a(III)I`/`c(IIII)Z` | `cn` 同形 | `cy` 同形+维度 | `fd` `a(III)I` | `rv` 同 fd 形 |
| 存盘 | (无 NBT 序列化) | 序列化器 a.a.a.a | `f()V`→level.dat | `a(Z)Z` | `a(Lnu)V` 包装（活体翻案≠0629） | 同 | SaveHandler | 同+GameType |
| Minecraft | `l`（thePlayer=`f:Player`） | `client.d` thePlayer=`f` | `client.d` thePlayer=`f` | `at` | 可读 Minecraft | 可读 | 可读 | 可读 |
| Entity pos | **float x/y/z**（z-up） | **float h/i/j** | **double h/i/j** | `em` j/k/l | `kh` ak..am | `lw` aw..ay | `sn`+`a(DDD)V` | `kj` 同 |
| ItemStack | **无**（平行 int[] slots/count） | `a.b.k` a=Count,c=id,d=Damage | `a.b.o` 同布局 | `hr` 同布局 | `ev` | `fp` | `iz`(stackSize/damage) | `ul` |
| Block 注册表 | `tile.a` 自注册 `b[]` | `a.a.b.x` 数组 `c` | `a.a.b.aa` 数组 `b` | `gk` `[Lgk;` | `ly` | `nq` | `uu[] m` | `lr[] m` |
| TileEntity | **无** | Chest+Furnace | +Sign | Furnace/Chest | +4 子类 | +Sign/MobSpawner | **8 子类** | 8 子类同名集 |
| GuiIngame | `e/m` 立即模式 | `c.l` | `c.m` | `gf.a(F)V` **0x0011=0327 同形** | `lu.a(FZII)V` 断代 | `nl.a(FZII)V` | `dv` | `ce` |
| FontRenderer | `e/l a(String,III)V` | `c.j b(String,III)V` | `c.k a(String,III)V` | `ef` a/b(String,III)V | `kd` | `ls` | `sj` | `kh` |

## 4. 33 op 能力矩阵（九版×能力组，✅存在/⚠️形态差异/❌不存在/❓未验）

| op 组 | c0.30 | 0223 | 0327 | 0618 | 0629 | a1.1.2 | a1.2.6 | b1.7.3 | b1.8.1 |
|---|---|---|---|---|---|---|---|---|---|
| 方块读写（place/dig/getblock/getblocks） | ⚠️ float z-up+byte id | ⚠️ byte id+有限界 | ✅ | ✅ | ✅ | ✅ | ✅+维度 | ✅ | ✅ |
| 区域批量（fill/replace） | ⚠️ 循环 setTile 可行 | ⚠️ 同 | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| meta/light（setmeta/getmeta/getlight/genscatter） | ❌ 无 meta 面（byte id 无 state） | ⚠️ Data 面待细验 | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| 属性/时间/出生点（getinfo/settime/setspawn） | ⚠️ setSpawnPos 有、time 形态异 | ⚠️ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| 玩家六 op（getplayer/tp/setlook/setmotion/sethealth/invulnerable） | ⚠️ float pos+health 在 Mob | ⚠️ float pos | ✅ | ⚠️ pos=h/i/j 漂移 | ✅ 重钉 | ✅ 重钉 | ✅ 重钉 | ✅ a(DDD)V | ✅ |
| 物品栏四 op（give/inv/setarmor/clearinv） | ❌ 无 ItemStack（平行数组） | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| 实体四 op（spawn/drop/explode/entitylist）+invulnerable | ⚠️ addEntity/findEntities 有、TNT❓ | ⚠️ TNT 有❓爆炸面待验 | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅+XPOrb |
| TE 三 op（getchest/setchest/getfurnace） | ❌ 无 TE | ✅ 两类 | ✅ | ✅ 三类 | ✅ | ✅ | ✅ | ✅ 八类 | ✅ |
| playsound | ⚠️ 音效面形态异（无 World.playSoundEffect?） | ❓ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| HUD（EXIT 钩） | ⚠️ 立即模式 e/m | ⚠️ c.l | ✅ c.m | ⚠️ c.m 同 0327 | ✅ gf 同形 | ⚠️ **a(FZII)V 断代** | ⚠️ | ⚠️ | ⚠️ |
| save | ⚠️ 序列化器非 saveWorld | ⚠️ 同 | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |

## 5. 战略结论（agent 架构影响，供拍板）

1. **逐版全表制确立**：G2 世代的"同族字母"是**陷阱**（0618 `a.b.k`=Item 基类≠0327 ItemStack；0223/0618 pos=h/i/j≠0327 g/h/i；跨世代也撞：b1.3_01 `nl`=region 类≠a1.2.6 `nl`=GuiIngame）——HookTargets 必须**一版一张表**，plan.md §4.1 Version Adapter 架构预判被字节码实证。指纹表机制（五元组严格匹配+不符即 UNSUPPORTED）本身跨代通用，G3 扁平世代锚定链已验证可行（可读入口→字段图→字符串→注册表唯一数组）。
2. **断代硬边界**（非混淆，是真能力差异）：Classic 无 NBT/无 ItemStack/无 TE/z-up float 坐标；Indev byte id+有限世界；Alpha HUD 签名 a(FZII)V 断代；**Beta 存储断代在世代内部 1.3**（≤1.2_02=Alpha 散件同形，1.3+=`.mcr` region）；1.8.1 饥饿/经验/创造模式。这些进 Version Registry 能力差异栏（plan.md §2.1）。**断代维度不随混淆世代对齐**：G3 扁平混淆从 0629 一路延续进正式版（索引规则 7；入口包路径 `net.minecraft.*` 自 Indev 起不变，与扁平化正交），但存储形态在 Alpha→Beta 边界（0629 散件起）与 Beta 内部 1.3 各断一次——adapter 分型必须按"混淆世代 × 存储形态"二维钉，不能按发行世代一刀切。存储断代比发行世代更细的三例：**Indev 内部 0122**（以前无存档路径，agent 无盘可钩）；**Infdev 内部 0327**（0227–0313 mclevel 已废且无 level.dat = 无存档链路，仅 GUI 残留 `.mclevel` 过滤串；agent 落盘面从 0327 起才有）；**Classic 内部 0.30**（`.mine` 仅在线上传形态，本地链路全代统一 level.dat 序列化——**取舍判决：Classic adapter 走 level.dat 链路**，`.mine` 不碰）。
3. **接入次序建议**（实跑批，待拍）：G2 内近亲优先=**0618→0629**（与 0327 语义等价面最大、saveWorld/hook 形态最接近）→ a1.1.2（HUD 断代首验）→ b1.7.3（region 首验）→ 其余。Indev 0223/Classic c0.30=形态差异最大，留后。若后续覆盖 Beta 早期（≤1.2_02），数据侧**复用 Alpha 散件 adapter**，agent 侧仍按 G3 逐版表钉。
4. **grow/clear 等 op 语义批不受影响**（仍 0327 面）。
5. 0227/0313（用户判决次优先）=G2 同族，但**无存档链路**（§2）——agent 落盘/持久化验证面为空，接入批价值仅剩观测面（tick/HUD/实体/内存态 op），持久化类 op 应标 UNSUPPORTED 而非勉强适配；待接入批顺路钉时按此定性。
6. **覆盖面判决（用户重申）**：工具覆盖=**原版件+高仿重建件**——高仿件不再是"仅探究"旁路，而是正式覆盖目标（丢失版本的启动器重建件因此可作业）。配套纪律：兼容登记表逐件标三态（原始/高仿）；高仿件行为不保证与原始一致，**指纹表逐件独立钉**（高仿件系"基于其他版本 mod 出"，混淆拓扑可能继承母本而非本版本名义），断代结论仍以原始件+索引备注为准。
7. **高仿来源（实物出处）**：姊妹仓 README §1 资源表钉死=GitHub **`nasko222/the-minecraft-recreation-project`**（重建丢失/内部客户端构建的 media preservation 项目，仅客户端，服务器不在范围）。结构：还原 jar 按**阶段分支**存放（pre-classic/classic/indev/infdev/alpha/beta/release），main 不放 jar 仅 `hashes.info`（全库 283 条 MD5 对表）；`testing`=正式发布前社区测试区（跨阶段混存）；`sound`=b1.5-dev 高仿用旧版天气音效四件套。分支根目录 jar 命名与 version-index 对表。接入批取高仿件（含本地缺件的 0227-1414 等）按此路径取回→MD5/SHA1 对榫→标三态入登记表。

### 5.1 边界线账本与代表版二维判据（用户指定，2026-11-14 补）

**判据**：真代表版 = **形态边界 ∪ 能力边界**。F4 聚类只能收敛字母表（形态侧），能力边界相邻构建字母表可能近同、聚类会并表——**只能靠领域指定**，指定版（§0 九版）是拓扑候选格而非终集；聚类残差 + 本账自动升格新代表。

| 边界 | 类型 | 代价 | 工程含义 |
|---|---|---|---|
| **a1.2.3** | 钩子侧（GuiIngame 免注入 XYZ 分界，用户指定） | **小** | 此后原版自带坐标显示面，HUD 注入从"必须"降为"可选/免做"——单钩开关，不动表、无语义风险。选版对 a1.1.2_01（前）+a1.2.6（后）天然夹住此线，接入即活体验证 |
| **0627/0629** | 形态侧（G2→G3 扁平散件起点，用户确认） | 中 | 新表但方法学原样复用——0629 接入批=第一次实弹检验 |
| **a1.1.2_01~a1.2.0** | 语义侧（生物群系系统带，用户指定） | 大 | 字母可能连续而 id 表/NBT 结构/mob 集/TE 类型全变——**同表≠同能力**；接入此带版本冒烟必须加数据面读回（getblocks+id 表对拍），防"字母对、语义错" |
| **b1.7.3/b1.8** | 语义侧重做（"维度级别"分界，用户指定，展开从略） | 最大 | javap 逐面对榫可能全绿但语义漂移——静态取证失效，只能活体终审+数据对拍兜底；本卷 §5 结论 2 的"1.8 饥饿/经验"即其可静态观测子集 |
| **in-20100223** | 回扩起点·Indev 线（末版/最成熟，用户指定 2026-11-15） | 待评 | 回扩**按世代线各从终态切入**：Indev 线=in-20100223（.indev 存储成熟形态、实体/物品栏雏形最全，与 Infdev 线形态距离最近，现有管线复用最大化） |
| **0.30** | 回扩起点·Classic 线（末版/生存测试形态，用户指定） | 待评 | Classic 线不从 0.0.2a 首版而从 0.30 终态（Classic 世代的形态超集：生存玩法+最完整方块/实体面）；注意 F2 账 0.30 曾留置（c/s/01c 三变体歧义）——取证批先解变体归属再钉表 |
| **a1.0.4 冬天模式** | 能力边界（引入 a1.0.4，移除 a1.2.0，用户指定探究+已取证闭环 2026-11-15） | 小 | 实钉（考古仓 tree→jar 双核，§0 第 9 条路径）：`World.snowCovered:Z`（a1.0.4 混淆=fk.c；a1.0.1~a1.0.3=fi 字母邻位=G3 续族实证）+ **level.dat NBT `"SnowCovered"`** + 新建世界 `rand.nextInt(4)==0`（25% roll，字节码 offset 431-448 实锤）+ 三消费面（ChunkProviderGenerate 生成雪层/World 落雪 tick/EntityRenderer renderSnow）。**串面 9 枚存档原件全 HAS（a1.0.4→a1.1.2_01），a1.2.0 含 dev/preview 链全 none**=移除点与生物群系引入同版（a1.1.2_01~a1.2.0 带交接窗实证）。工程含义：①SnowCovered=数据面新 NBT 键（世界 flag 读写 op 候选）②生物群系带冒烟加机器锚点（level.dat 键消失+biome 类出现）③同带附带断言核验：a1.0.1_01 红石（wire=55/ore=73-74/torch=75-76，a1.0.1/1.0.2 原始丢失故锚 a1.0.1_01）、a1.0.3 洞穴音效（`ambient.cave.cave` 在 World，a1.0.2_02 零引用） |

**落码纪律**：VersionTable 不预建能力档字段（over-engineering 戒律）——待能力边界版本接入、实跑证据逼出时再显式建模（先例=A2 档位三）。

## 6. 证据索引

- 详表：[docs/compatibility/verscan/](../compatibility/verscan/)（8 版逐一 FQCN+签名+判据）
- 中间 dump：`%TEMP%\mcaut\verscan\`（javap 全量，不入库）
- jar 身份：本卷 §0（SHA1 前 12 位对榫表）
- Beta 存储断代补验（非试点件，仅边界取证）：`.minecraft\versions\b1.2_02`/`b1.3_01` jar 全类字符串常量扫描（`.mcr`/`region`/`tmp_chunk` 三探针，结果见 §2 表）
- **版本钉版权威（戒律级）**：姊妹仓 `minecraft-verification-analysis\indexes\version-index.md`（本卷全部断代结论已按索引行复核；未列版本=不存在；高仿件探究须标三态，覆盖面可扩至高仿=用户判决）
- Indev 存档断代补验（非试点件）：`in-20100101`/`in-20100114`/`in-20100122`/`in-20100125`（betacraft）四件对照扫描；c0.30 `.mine` 语义 javap（`e/k`+`l`）。**扫描教训**：`.mine` 子串会命中 `www.minecraft.net` URL 假阳性，必须用负向断言 `\.mine(?!craft)`；0114 首轮 `.mine` 命中即为此例，javap 复核证实为资源/皮肤下载器。三态重标（索引复核）：0114/0122=高仿重建、0125=原始、0101=索引未列按不存在处理；断代主证据=索引备注（维护者总结），扫描为佐证
- Infdev 无存档断代补验（0227–0325 全区间，索引 §4 为准枚举）：原始件 1433/0313/0316/0320/0321/0325-1640 + 高仿 0325-1545 逐件扫 `level.dat`/`.mclevel`/`MinecraftLevel`/`tmp_chunk`/`SpawnX` 五探针——全部同相（四探针零+GUI 残留 `.mclevel` 过滤串），存盘定装 0327；`saving-beta1/2` 经用户判决=第三方 mod 件（逆移植 Alpha 存档实验、不完善），不入断代证据链
- 方法论：0327 基线=[phase1-command-channel](./phase1-command-channel.md)；混淆表教训（调用点反推不可靠/类体级证据）=[phase1 §23](./phase1-command-channel.md)

## 7. 指纹工业化方案（议题已钉，未开工）

**问题**：逐版全表制 × 索引数百版本 = 找类+钉指纹无法手工覆盖。本方案把工作量从"每版本一次手工"降为"每语义面一次算法+每版本一次机器执行"。

**前提成立性**（本卷实证）：①锚定链本身是程序不是直觉——9 版详表记录的每一步判据（入口稳定包路径→字段图→字符串常量→注册表唯一数组→五元组）均可参数化；②入口锚跨代稳定（§1 世系：Indev 起 `net.minecraft.*` 不变）；③NBT 键是 15 年不变的语义载体（SpawnX/Blocks/TerrainPopulated）；④既有五元组严格匹配+UNSUPPORTED 机制保证**错表=覆盖率缺口而非错钩**——自动化容错前提成立。

**组件**：

| # | 件 | 职责 |
|---|---|---|
| F1 | 语义锚库 | 每 op 面一个搜索程序（getBlockId/setBlock/saveWorld/HUD/pos/ItemStack/TE…），输入 jar 输出候选五元组+判据链+置信度。手工调研的全部判据在此固化 |
| F2 | tiny 快路径（**已解剖+全量拉取，2026-11-14**） | 主源=Unofficial fork 的数据仓库 `Blizzardfur-Maxxx/MCPHackers.github.io`（代码实钉：Unofficial `VersionsParser.jsonURL` 指向此；姊妹仓 tiny 皆出自这里）。versions.json=170 条目、映射 zip=133 件（37 条目无映射=覆盖缺口清单）；zip 内含 `client.tiny`（tiny v2 三列：obf ID+**official=jar 实名**+named=可读类路径/成员 SRG 或真名）+`client.exc`+`patches_client` 反编译补丁+start 模板。**质检对账**：0327 件与金标准表逐条吻合（World/Minecraft/GuiIngame/Chunk 四类+saveWorld`(Z)V a`/restartTimeOfDay`h`/saveWorldIndirectly`f`），类覆盖 234/303=77%——质量参差实证，逐件覆盖率实测入档。附赠：versions.json `client_url`（betacraft/omniarchive vault）=本地缺件 jar 取回源（0227-1414 类高仿件经此）。**反哺已执行（2026-11-14）**：72 树目录写入 `client.tiny`（与源码树同目录统一命名；姊妹仓原有 23→现 95/110）；合法例外=0.0.11a（社区无 tiny）、b1.2_02-dev（未混淆无需）；留置=0.30（c/s/01c 歧义）、alpha-v1.1.2（无 zip）、pc-*（与 rd-* 不对榫）、*-server/server.tiny 另议。**named 风格分族**：扁平世代 RetroMCP 用 `net/minecraft/src/*` 路径（0629 件抽检）——F4 按 named 对齐时 named 空间本身按世代分族。**质检缺口**：133 件逐件覆盖率实测未做（P3 项）。 |
| F3 | 慢路径（锚库补洞） | tiny 覆盖缺口/低质件跑 F1 锚库；UNRESOLVED/低置信项出人工复核清单。**F2/F3 非两条独立路径，是 tiny 为主+锚库补洞** |
| F4 | 家族聚类 | 按类集哈希+字符串指纹聚类 jar：相邻构建字母表全同/近同→一族一表，数百版本收敛为数十有效表；高仿件母本归属由聚类直接暴露（§5 结论 6 从警告变算法输出）；SHA1 同件先去重。**SRG/named 列=跨版本语义 ID 层**（用户指出 SRG Name 字段保留）——聚类对齐可按 named 类路径而非字母，同字母异义陷阱在 named 空间自动消解 |
| F5 | 回归金标准 | **verscan/ 8 份详表+0327 基线表=编译器测试 oracle**——机器输出必须逐字段对上手工表才准出；详表从"调研产物"升格为"永久回归夹具" |
| F6 | 运行时消费 | premain 按 versionId 载 `fingerprint.json`（HookTargets 表数据化，代码零版本常量）；表缺失/置信不足→该面 UNSUPPORTED（响亮失败纪律不变） |

**数据侧分工**：存储 adapter 按型不按版（6 型封顶：序列化 level.dat/mclevel/散件/zone/mcr/mca），不吃指纹工作量。

**分期（开工时再拍，此处仅序）**：P1=F1 慢路径最小核（3 面：getBlockId/setBlock/saveWorld）+F5 对账 9 版；P2=扩全 op 面+F4 聚类跑索引全量；P3=tiny 全量 QC（133 件逐件覆盖率实测+金标准抽检）+F6 运行时装载。语料现状：`unoriginal/original/pages` 三克隆在 `%TEMP%\mcaut\retromcp\`（133 zip 已在位，临时区——转正式资产需另行安置，待拍）。

**边界与风险**：G1 Classic 半可读世代锚库需独立一族规则（可读名直给但字段短名仍需钉）；0624 zone 类单日异型件聚类应识别为独立型；b1.2_02-20110517 未混淆例外件=零成本直通；高仿件拓扑继承母本→聚类归属前禁并表。

**验收判据（开工时）**：9 版回归全绿；聚类收敛比实测入档；任一版本任一面错表时实跑表现为 UNSUPPORTED 而非错钩。

---

## 8. 版本适用指令集（Version × Op 能力矩阵——版本扩展北极星交付物）

> 来源：2026-11-14 用户判决（Classic 范围+结构差异提醒）+ ChatGPT 审议落档（四因可区分/登记表=JAR 契约/能力描述层进 Registry）。原料=§4 能力断代矩阵+§5 存储断代+verscan 9 版表；本方案把它从案卷判决表升格为三层结构。

**① 每版一个适用指令集（不是全 35 op 通吃假设）**
- `inf-20100327`=基线全 35；`inf-20100618`=同代换指纹；`b1.7.3`=35+growTree 转正（Beta 1.2 断代翻转，树苗 grow 由 UNSUPPORTED→OK）。
- **Classic 线最小集（用户判决）**：world 操控面（place/dig/fill/getblocks/getinfo）+ spawn 单一 mob（G 键等价物）≈6~7 op；其余 28 全落 `UNSUPPORTED(capability-absent)`。Classic 无 metadata/TileEntity/NBT ⇒ getmeta/setmeta/getchest/grow/clear 连载体都不存在。
- Indev 段=中间态（metadata 有、tileentity 半、inventory 形态异），逐 op 判待取证。

**② 五态模型（op × 版本单元，非布尔）**
`OK` / `UNSUPPORTED(capability-absent 版本真没有)` / `UNSUPPORTED(fingerprint-unconfirmed 有但未钉)` / `UNSUPPORTED(not-implemented agent 未写)` / `REJECTED(runtime-condition 此刻不行)`。
四因必须可诊断（审议判决：否则跨版后 UNSUPPORTED 成黑箱）——detail 文案分域，A5 落码进 runOp 前置查表。

**③ 落点**
- B1 锚库：每版 JSON 加 `capabilities` 节（与 `targets` 指纹节并列，分开管理）；结构预留 tiny 对账字段（B2 diff 用）。
- A1 Registry 抽象必须容**方法缺失/签名差异/语义差异**三档（戒律 15：Classic≠换名）。
- listops v2（C4）=本表机器可读出口，桌面按当前版本灰化不适用 op。
- **0.24~0.30 翻转区间**（戒律 16）：同版两模式能力面不同，spawn 挂 runtime-condition 维；逐件扫描钉翻转构建=A4-Classic 前置取证；G 键路径与 agent 直写路径不同构，分开验。

**取证缺口**：a) 0.24~0.30 生存翻转逐件扫；b) Classic spawn 面（G 键配方/Entity 构造/mob 存活旗标）新面取证；c) Indev 段逐 op 能力判（verscan 0223 只有指纹面无 op 判）。
**工作量口径**：Classic=新面（adapter 级），非新列（换表级）——A4 排期按此估。

---

## 9. a1.1.2_01 接入批活体记录（2026-11-15，el1121-1/2）

**链路**：verscan 详表（[a1.1.2_01.md](../compatibility/verscan/a1.1.2_01.md)，含 §11 活体判决）→ 锚 JSON `java/agent/registry/fingerprints/a1.1.2_01.json`（SHA1 daa4b9f1…）→ VersionRegistry.build1121+HookTargets.TARGETS_1121+WorldOps → 假类复刻无头三件全绿（A2Table1121 260/FakeOps1121 52/InjectSmoke1121 22）→ Attic 启动器活体 World4（-Xbootclasspath asm-9.7）。

**判决**（详表 §11 为准，此处索引）：
- 五钩 HOOKED 全中；HUD 次级链断链即禁（HUD_HOOK_DISABLED，无错钩）。
- **saveWorld 活体翻案**（0629 翻案①同型）：a(Z)Z=tick 本体（体首串 "TickNextTick list out of synch"，g() 每 tick 直调）→ 初钉致 SAVE_WORLD 刷屏 820 次；重钉 `a(Lnu)V` public 包装后 14 次全 ops 触发。
- **setBlock 主写面定夺=`d(IIII)Z`**：marker place(-34,70,169,id=1) 退出重进 getblock=id=1 持久化。
- **档位三玩家回退实锤**：World.k.get(0)=bi（entitylist N=327 含 bi@…[h=9]，未注册类简名留档=§4 预期）。
- **冬天模式冒烟命中**：level.dat 终验 World4 Data 含 **SnowCovered=1**、World3(0629) 无=移除与生物群系同版（§5.1 a1.0.4 行）；SpawnX/Y/Z=-34/70/169（setspawn 持久化+o/p/q 三轴）、Time=6501（c:J）。
- ops 面：38op 批 38/38 应答（26 OK/6 REJECTED 参数名错/其余 UNSUPPORTED 如设计）+s3 修正批 8/8；getinfo 实锤 o=spawnX+seed=u+difficulty=l；getlight=13 实锤 j；spawn sheep 跨存档存活+TNT 爆炸→Item@ 掉落簇持久化闭环。
- **§5.1 生物群系带冒烟判据兑现**：数据面读回（level.dat 键集 diff）=防"字母对、语义错"机制首次全链执行。

**证据**：`integrations/attic/out/el1121-1/`+`el1121-2/`（transform/hooks/game-stream 三 log+mca 全队列）。**repo 面**：M=VersionTable/VersionRegistry/HookTargets/WorldOps/锚 JSON/详表/本卷（未 commit，待指令）；测试件不入库（%TEMP%\mcaut）。
