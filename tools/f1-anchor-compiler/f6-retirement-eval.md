# F6 详表退役评估（评估批，2026-11-22）

- 性质：只评估、只产出本报告；docs/ 与 java/ 全部未改动，git 未触碰。
- 输入：docs/compatibility/verscan/ 11 份详表全文；java/agent/registry/fingerprints/ 11 份锚库 JSON；tools/f1-anchor-compiler/report.md（F5 矩阵 627/627）；F1 编译器 113 面规则。
- 分类口径：**取代**=已被锚库/F1 规则/F5 矩阵机械化覆盖，可加 superseded 注记后退役；**保留**=唯一权威（活体判决链、案底、运行时/存储断代细节），不可退役；**合并**=有价值但应移入他处（指明去处）。

## 0. 详表家族盘点（含任务预警的"不止 11 份"项）

1. verscan 目录实有 11 份：c0.30-c / in-20100223 / inf-20100618 / inf-20100629 / a1.1.2_01 / a1.2.0_02 / a1.2.6 / b1.2_02 / b1.7.3 / b1.8 / b1.8.1。
2. **inf-20100327 无详表**：0327 基线判词在 java/agent/src 的 WorldOps.java/HookTargets.java 常量注释 + docs/research/phase0-1 集成卷（fingerprints/README.md 明示"0327 基线表=WorldOps.java 常量+HookTargets.java，不在本目录重复"）。家族=11 份 verscan + 0327 基线另在。
3. **b1.8 有详表无锚库**：旁证批未接入（b1.8.md §5.4"本批不改主代码"），其详表是该 jar 的唯一记录。
4. **锚库 schema 分叉**：9 版用 targets/provenance 旧 schema；b1.7.3、b1.8.1 用 hooks/ops/capabilities 新 schema（详表钉值以 ops 字母表+provenance.note 长文承载）。退役评估须按各自 schema 对照。

## 1. 逐份逐节盘点

### 1.1 c0.30-c.md（163 行，可读名世代）
| 节 | 判定 | 依据 |
|---|---|---|
| §0 major=49 | **保留** | pre-51 帧格式=agent 注入约束，锚库无此面 |
| §1 Level 成员表 | **取代**（锚 37 targets 覆盖 getTile/setTile/netSetTile/setTileNoUpdate/updateNeighborsAt/spawnXYZ/setSpawnPos/findEntities/addEntity）；networkMode 门（setTile 联机返 false）=**保留**（运行时行为语义） | |
| §2 主类字段图 | **取代**（minecraftClass/thePlayer/theWorld/ingameGuiField 已锚）；屏幕机制 e.o+setter=**合并**→WorldOps adapter 注记 | |
| §3 Inventory 平行数组 | **取代**（invMain/invCount/invCur/addStack 已锚；"无 ItemStack/无 Damage"=capabilities 已录） | |
| §4 Entity float 坐标 | **取代**（pos/mot/yaw/pitch F 已锚）；**z=垂直轴**语义=**保留**（坐标换算唯一权威） | |
| §5 Tile 注册表自注册 | **取代**（blockClass/blockRegistry 已锚；判据已入 F1 规则） | |
| §6 HUD+字体 | guiIngame=**取代**；fontRendererClass(e.l)+drawString/测宽签名=**详表有锚库无**（见 §3 清单）→**合并** | |
| §7 TileEntity 不存在 | **取代**（ABSENT 判决进 F5 N/A 口径） | |
| §8 存储格式 | **保留**：magic=656127880、format=2、level.a 存读四方法、level/c readClassDescriptor 重映射、`.mine` 辨析（扫描陷阱 `\.mine(?!craft)`）——F1 已据此翻案 c0.30 saveWorld 判据，但判词本体唯一在此 | |
| §9 33 op 能力表 | **取代**（capabilities 段） | |
| §10 未命中声明 | **取代** | |
- **终判：部分保留**（保留 §0/§8 + networkMode/z 轴两条语义；§6 字体面合并后再议 superseded）。

### 1.2 in-20100223.md（58 行）
| 节 | 判定 | 依据 |
|---|---|---|
| 头部形态（481 类/全混淆/主类 client.d） | **保留**（G2 家族判据样本） | |
| §1 World=a.a.g 钉表 | **取代**（getBlockId a/setBlock b/setBlockSilent a/getMeta e/setMetaSilent f/getBlockIdByte d/randSeed h/spawn i-k/saveWorld 序列化器 a.a.a 全已锚） | |
| §2 主类 client.d | **取代**（theWorld d/thePlayer f 已锚） | |
| §3 实体链（a.c.b=Entity、a.c.e=EntityLiving、a.c.e.a=Player、a.b.k=ItemStack、a.c.e.b=Inventory） | **取代**；**EntityItem=a.c.a 原值已悬案**（见 §2 案 6/7） | |
| §4 Block=a.a.b.x+registry c | **取代** | |
| §5 HUD c.l.a(F)V | **取代** | |
| §6 TileEntity 系（a.a.b.a.a/b/c） | **取代**（teGet/tileEntityClass/chestClass/furnaceClass 已锚） | |
| §8 与 0327 差异表（a.c.b 同名同语义、g 同形） | **保留**（跨版字母稳定论证=案底，F4 聚类佐证材料） | |
| §9 存储=level.dat GZIP+序列化、无 NBT | **保留**（断代判词） | |
| §10 能力面 | **取代** | |
- **终判：部分保留**（保留 §8/§9 + 头部形态）。

### 1.3 inf-20100618.md（59 行）
| 节 | 判定 | 依据 |
|---|---|---|
| 头部（492 类，与 0223 同世代） | **保留** | |
| §1 World=a.a.g 钉表（含 difficultySetting `d:I`、entityList j、randSeed m、spawn i-k） | **取代**（锚 54 keys 覆盖；difficulty 0618 未入锚=F1 BLOCKED 口径，见 §2 附注） | |
| §2 主类 client.d（runTick l、theWorld d、thePlayer f、guiIngame c.m、font c.k） | **取代**（fontRendererClass 已锚） | |
| §3 实体链（a.c.b=Entity、a.c.f=EntityLiving、a.c.e.a=Player、a.b.o=ItemStack、a.c.e.b=Inventory） | **取代**；**EntityItem=a.c.a 悬案**（案 7）；health=f.Z 锚有详表无=反向（F1/0327 系补钉） | |
| §4 Block=a.a.b.aa+registry b | **取代** | |
| §5 TileEntity 系+sign d | **取代**（signClass 已锚） | |
| §6 无 biome/无下界判决 | **保留**（断代案底：biome=b1.0、下界=a1.2.0） | |
| §7 与 0327 差异表 | **保留**（跨版稳定论证） | |
| §9 存储：save 入口 `World.f()V`→私有 `b(true)`；load 入口 a.a.a(File)/(InputStream) | **取代+口径注**：锚钉 b(Z)V=本体，详表钉 f()V=入口——**非冲突但两文档口径不一**，退役前在 superseded 注记写明"入口/本体"映射 | |
| §10 能力面 | **取代** | |
- **终判：部分保留**（保留 §6/§7 断代与稳定论证；§9 加口径注）。

### 1.4 inf-20100629.md（91 行）
| 节 | 判定 | 依据 |
|---|---|---|
| 头部（617 类、**G3 首例**、主类=at 短名回漂） | **保留**（世代断代判词） | |
| §1 World=ez 钉表+**翻案案底**（任务书猜 a(Z)V 错；真链 a(Z)Z→a(ZLiw;)V→a(Liw;)V 包装；el1129-1 活体 3964 次实锤） | **保留**（案底+活体判决链；现值 a(Liw;)V 已入锚=取代） |
| §2 主类 at（runTick m、loadWorld at.a(ez,String)V、theWorld d、inventory c） | **取代**（loadWorld/theWorld 已锚） | |
| §3 实体链（em=Entity pos j-l、jv=EntityLiving health ab、gc=Player、hr=ItemStack、ho=Inventory、gt=EntityItem） | **取代**（70 keys 全覆盖） | |
| §4 mob 表（cy tnt/ft creeper/fj skeleton/gr zombie/cf spider/hx pig/dk sheep/aa giant） | **取代**（mob* 已锚；**mobBase 0629 未锚**=F1 待办，非详表悬案） | |
| §5 Block=gk/registry b | **取代** | |
| §6 GUI（gf a(F)V）/字体 ef | **取代** | |
| §7 TileEntity=bf+teGet b/teSet a | **取代** | |
| §8 存储（iw=IProgressUpdate、level.dat+NBT、无 region） | **取代**（progressClass 已锚）；无 region 判决=**保留**（断代案底，b1.3 首例） | |
| §9 33 op | **取代** | |
| §10 未命中声明 | **取代** | |
- **终判：部分保留**（保留 §1 翻案案底、§8 无 region 判决、头部断代）。

### 1.5 a1.1.2_01.md（163 行）
| 节 | 判定 | 依据 |
|---|---|---|
| §0 major=49 | **保留** | |
| §1 World=cn 全成员表 | **取代**（61 keys 覆盖）；**spawnXYZ=o/p/q 与运行时旧钉 n/o/p 的断代案底**（el1121-1 活体 setspawn 读回）=**保留**（案底） | |
| §2 主类 Minecraft（字段图+**a(Z)Z=tick 本体禁钩陷阱案**） | 字段图=**取代**；a(Z)Z 陷阱+loadWorld 三重载判词=**保留**（运行时钩子细节唯一权威） | |
| §3 实体链（kh 全字段表 aW=heartsLife/aR=age 辨析、ge=EntityLiving E=health、dm=Player b=inventory、dx=EntityItem、ev=ItemStack） | **取代**（iframes=aW/health=E/invField=b 已锚且 F5 全绿；aW/aR 辨析论证=**保留**案底） | |
| §4 物品/方块/TE/注册表（di.c=[Ldi;、TileEntity 五工厂串） | **取代**（F1 规则已机械化该判据） | |
| §5 mob 表（15 mob+Giant=hl） | **取代** | |
| §6 GUI/字体（lu/mz） | **取代** | |
| §7 存储（nu=IProgressUpdate、a(Lnu;)V 包装、无 region） | **取代**（saveWorld 已锚）；无 region 判决=**保留** | |
| §8 33 op | **取代** | |
| §9 钩子映射表（四钩 access/injectMode/expectedHits） | **保留**（运行时钩子细节；锚 JSON 仅 hooks 段 b1.7.3/b1.8.1 有，旧 schema 9 版无） | |
| §10 活体判决 el1121-1 | **保留**（判决链唯一记录） | |
| §11 未命中声明 | **取代** | |
- **终判：部分保留**（保留 §0/§2 陷阱案/§3 辨析案底/§9/§10；其余 superseded）。

### 1.6 a1.2.0_02.md（134 行）
| 节 | 判定 | 依据 |
|---|---|---|
| §0 总体（502 类、Minecraft 可读名、**下界休眠态**） | 休眠态判词=**保留**（后被 a1.2.0_02 §1A 终审，案底链） | |
| §1A 下界维度机制全活终审（getProviderForDimension 全量 javap） | **保留**（断代终审唯一权威，影响 a1.2.x 全线） | |
| §2 World=cu 钉表（spawnXYZ 左移 n/o/p、difficulty=k、health 案） | **取代**（锚覆盖）；左移案底=**保留**（跨版移位警示） | |
| §3 实体链（lk Entity 字段表、gx health=J 翻案案底、dv inventory=e 翻案） | **取代**（iframes=aW 经 F1 tiny 规则机械化=**取代**；E→J、b→e 翻案案底=**保留**） | |
| §4-§9（物品/方块/TE/mob/GUI/存储/33 op） | **取代** | |
| §10 活体判决（el120-3 iframes aW 287→300 等） | **保留**（判决链） | |
- **终判：部分保留**（保留 §1A/§10/移位与翻案案底；iframes 判据本轮已由 F1 tiny 规则接管，是"取代"扩面的最新实例）。

### 1.7 a1.2.6.md（42 行）——悬案最集中
| 节 | 判定 | 依据 |
|---|---|---|
| §0 总体（527 类、major=49） | **保留** | |
| §1 主类（runTick i、loadWorld 三重载 a(cy,String,eb)V、字段图） | 字段图=**取代**；**loadWorld 三重载详表有、锚库 a1.2.6 无 loadWorld 键**=**合并**→F1 续扩面批补钉或归钩子映射文档 | |
| §2 World=cy 钉表 | **5 项悬案**（见 §2 案 1-5）：setBlock c→d、saveWorld a(Z)Z→a(Lpu;)V、addEntity (Llw;)V→(Llw;)Z、explode a(ch,IIII)V→a(Llw;DDDF)Lks;、getClosestPlayer 详表有锚无（**合并**→F1 补钉）；维度面/rayTrace=**保留**（超面框架） | |
| §3 实体链 | EntityItem=pp 悬案（案 5 系）；ghast/pigzombie/slime/chicken 归类修正案底=**保留**；minecart/painting/GuiGameOver=**详表有锚库无**=**合并** | |
| §4 注册表（fp/nq/dx/hb/ji/fo/ha） | **取代** | |
| §5 GUI/字体 | **取代**（fontRendererClass=ls 已锚） | |
| §6 存储（mu 散件、**无 region=证伪"Alpha=region"假设**、Dimension NBT） | **保留**（断代案底：region=Beta 1.3 首例） | |
| §7/§8 能力面+钉法可行性 | **取代** | |
- **终判：部分保留 + 优先修订**：本表 5 处旧钉值未随 F1 翻案更新，是 11 份中唯一"正文即错值"的表；superseded 注记必须逐条写明"以锚 JSON 为准"，翻案案底**合并→version-expansion-pilot.md §7 追记**。

### 1.8 b1.2_02.md（108 行）
| 节 | 判定 | 依据 |
|---|---|---|
| §0 总体（719 类、三位字母未现） | **保留** | |
| §1 主类（runTick i、loadWorld 3 参、thePlayer g:bz） | **取代**（74 keys 覆盖） | |
| §2 World=dn 钉表（setBlock e、difficulty k、spawn m-o） | **取代** | |
| §3 实体链（nl pos aF-aH、fh=EntityItem、gm=ItemStack、ig health=S、eu f=inventory） | **取代**；**iframes 未钉（档位一欠账）**=详表锚库一致空，b1.7.3 复活案底归 §1.9 | |
| §4-§8（注册表/TE/mob 全表含 squid=rm/Giant=jv） | **取代** | |
| §9-§10 GUI/存储（ru、无 region 沿用） | **取代**；无 region=**保留** | |
| §11 活体判决+UNSUPPORTED 表 | **保留**（el 判决链；iframes UNSUPPORTED 判词） | |
- **终判：部分保留**（保留 §0/§11/无 region 案底）。

### 1.9 b1.7.3.md（74 行）
| 节 | 判定 | 依据 |
|---|---|---|
| §0 总体（747 类、**三位字母首例**、stamp 自证） | **保留** | |
| §1 主类 | **取代**（ops 覆盖） | |
| §2 World=fd+**WorldInfo=ei 断代**（time/seed/spawn 迁出 World；ei 字段面 a/e/b-d；difficulty k→q；getMeta 活体翻案 g→e） | **保留**：WorldInfo 断代=超 113 面框架的存储侧结构断代，锚 ops 仅字母表、判词唯一在此；getMeta 翻案案底=**保留** | |
| §3 实体链（sn pos aM-aO、**iframesF=by 复活**=heartsLife public 实锤非 be=isDead 撞车位） | 字母=**取代**（ops）；复活案底+撞车位警示=**保留** | |
| §4-§6（jc 注册表 26 项、Block uu、TE ow） | **取代**（mobClasses 入 ops） | |
| §7-§8（GUI uq、字体） | **取代** | |
| §9 存储断代（**region/r.<x>>5>.<z>>5>.mcr**、qj=RegionFile 定位表语义、el173-1 真盘判决、下界回归、天气键 l/m/n/o 复活、钩组四项+LOAD_WORLD 判决 B 改挂 3 参+EXIT） | **保留**（全部为超面框架断代+运行时钩子细节；JSON provenance.note 是摘要非判词本体） | |
| §10 字母断代陷阱群对榫表 | **保留**（跨版警示） | |
| §11 活体判决 el173-* | **保留** | |
- **终判：暂不退役**。阻塞项=WorldInfo/region/维度/天气断代判词无机械化归宿；解阻条件=存储断代内容结构化迁入 RegionStore/WorldOps 文档或新 schema JSON 后再 superseded。

### 1.10 b1.8.md（39 行）
- 全表性质=**旁证批**（无锚 JSON，唯一记录）。§1 类名定榫（tiny 直钉全量表）、§2 漂移判决表（b1.7.3→b1.8 逐项）、§3 region 链同形确认、§4 新面（饥饿 EntityFood 四键/GameType int 键/WorldSettings 构造参）、§5 判决（**b1.7.3 表严禁套用**+撞车位 jc/wd）。
- **终判：暂不退役**。阻塞项=b1.8 无锚库 JSON；若未来接入=独立全量批，本表是其唯一起点。

### 1.11 b1.8.1.md（42 行）
| 节 | 判定 | 依据 |
|---|---|---|
| §0 总体（809 根类=b1.8 同集、借 b1.8 tiny+实件逐锚复核） | **保留**（配方案底） | |
| §1 旁证直转判决（116/117 dump 全同、cf 唯一漂移 V→boolean 不在 ops 链） | **保留**（判决链） | |
| §2 锚表（类身份断代+ops 漂移/同字母清单） | **取代**（b1.8.1.json ops 已承载）；**mob=tz abstract 摘槽判决**=**保留**（el181-1 活体+类树断代） | |
| §3 存储面（rl=WorldInfo 键集=ei 复刻+GameType/MapFeatures、新建世界红线） | **保留** | |
| §4 新面（饥饿 kc/GameType/ItemFood） | **保留**（超面框架） | |
| §5 注册表 um 观察位 | **取代**（ops entityListClassG=um） | |
| §6 活体判决 el181-1..5（钩四项全中、维度跟随单向机制 javap 终钉、修复判决 B、**el181-5 推翻两条旧判**、HUD 免做开关终验） | **保留**（完整判决链，JSON note 仅摘要） | |
- **终判：部分保留**（§2 锚表节可 superseded，其余保留）。

## 2. 悬案清单（详表钉值 vs 现行锚库 JSON 不一致）

全部集中在 a1.2.6（5 案）+ 0223/0618（itemEntityClass 各 1 案），均已在锚 JSON provenance 记明翻案，唯详表正文未回写：

| # | 版本 | 面 | 详表原值 | 现锚值 | 翻案记录 |
|---|---|---|---|---|---|
| 1 | a1.2.6 | setBlock | c(IIII)Z | cy.d(IIII)Z | F1 批 2026-11-20：c 实为 chunk 元数据写 |
| 2 | a1.2.6 | saveWorld | a(Z)Z | cy.a(Lpu;)V | F1 批 2026-11-20：a(Z)Z=tick 本体（0629/a1.1.2 同型陷阱第三次重演） |
| 3 | a1.2.6 | spawnEntity | a(Llw;)V | cy.a(Llw;)Z | F1 扩面翻案 2026-11-20：V 形态在 jar 不存在 |
| 4 | a1.2.6 | explode | a(ch,IIII)V | cy.a(Llw;DDDF)Lks; | F1 扩面翻案：ch 非 Explosion 真面 |
| 5 | a1.2.6 | itemEntityClass | pp | eo | F1 续扩面 2026-11-21 人工裁决："构造器未钉"疑点自白→eo 具全形态 |
| 6 | in-20100223 | itemEntityClass | a.c.a | a.c.c.b | 同上（a.c.a 无 ItemStack 面） |
| 7 | inf-20100618 | itemEntityClass | a.c.a | a.c.c.b | 同上（与 0327 同构） |

口径差异（非冲突，注记即可）：0618 saveWorld 详表钉入口 f()V、锚钉本体 b(Z)V。
BLOCKED 旁注：difficulty 面 F1 判 b1.7.3 ei.q / b1.8.1 rl.v 不存在（运行时 registry 把 difficultyF 映射到 WorldInfo 的口径错位），与详表 §2/§10 的 q/v=World/WorldInfo 字段实锤并存——待 oracle 修正，不属详表错。

## 3. 详表有、锚库无的面（退役前必须找归宿）

| 版本 | 面 | 详表出处 | 归宿建议 |
|---|---|---|---|
| c0.30-c | loadWorld（level.a.a(File)V/a(InputStream)LLevel;） | §8 | F1 续扩面补钉（saveWorldBody 同型配方） |
| c0.30-c | fontRendererClass（e.l：a(String;III)V/a(String)I） | §6 | F1 补钉（a1.2.6/b1.7.3 已有该面先例） |
| c0.30-c | getClosestPlayer（Level.getPlayer()LEntity;） | §1 | F1 补钉（c0.28 草稿已证可编译） |
| c0.30-c | 屏幕机制 e.o+setter、addToTickNextTick、setNetworkMode、findSpawn/getHighestTile/copyBlocks | §1/§2 | 合并→WorldOps adapter 注记（非锚面） |
| a1.2.6 | loadWorld 三重载（含 a(cy,String,eb)V 维度入口） | §1 | F1 补钉 loadWorld（a1.1.2/a1.2.0/b1.2_02 已锚，此版漏） |
| a1.2.6 | getClosestPlayer（eb a(Llw;D)Leb;/a(DDDD)Leb;） | §2 | F1 补钉 |
| a1.2.6 | rayTrace、维度面 a(eb,III)Z、EntityMinecart=qd、EntityPainting=gb、GuiGameOver=bb、ghast=am/pigzombie=pt/slime=ns/chicken=ou、Chunk 存盘器=mu | §2/§3/§6 | 超 113 面框架：合并→version-expansion-pilot.md 或 mob 扩展批 |
| 0618 | health=f.Z（锚有详表无，反向缺记） | — | 仅注记，无需动作 |
| b1.7.3/b1.8/b1.8.1 | WorldInfo 字段面（ei/rl：a/e/b-d/i/l-m-n-o）、region 定位表语义、天气四键、GameType/MapFeatures、饥饿 kc=EntityFood 五字段、ItemFood aea.l()I、WorldSettings=dx、Teleporter/WorldProviderHell、mob 类树 abstract 断代（tb/aq/pe） | b1.7.3 §2/§9、b1.8 §3/§4、b1.8.1 §3/§4 | **超面框架核心缺口**：唯一归宿=详表本身（或未来"存储/维度断代专卷"）——这是 b1.7.3/b1.8 暂不退役的直接原因 |
| 全线 | 钩组 access/injectMode/expectedHits 明细（旧 schema 9 版锚 JSON 无 hooks 段） | 各表钩子映射节 | 合并→统一 hooks 段 schema 化后 superseded |

## 4. 汇总判决

**终判分布：全退役 0 / 部分保留 9 / 暂不退役 2（b1.7.3、b1.8）。**

- 部分保留 9 份：c0.30-c、in-20100223、inf-20100618、inf-20100629、a1.1.2_01、a1.2.0_02、a1.2.6、b1.2_02、b1.8.1。共同保留段=活体判决链（el*）、翻案案底、世代断代判词、钩子注入细节；共同退役段=成员钉表（被锚库+F5 矩阵 627/627 取代）。
- 无一"全退役"的原因：每份都至少含一段机械化未覆盖的运行时/断代/案底内容。
- 悬案 7 案全部是"F1/人工已翻案、详表正文未回写"型——退役动作（文件头 superseded 注记+指向锚 JSON）即自动消解，无需改 docs 正文（本评估批未改）。
- 维护面节省测算：11 份详表合计钉值行 ≈560 行手工锚面，其中 ~85%（475 行级）已被锚 JSON+F1 规则+F5 对账机械化；保留段合计 ≈85 行（判决链/断代/案底），即手工维护面从"每版全量详表"收敛到"每版仅断代判决+活体链"两段。

**流程建议（新版本取证还需要详表吗）**：常规同世代新版本=F1 草稿（--draft）+活体判决即可，全量成员钉表形态退役；**反例如实报告**=跨存储/结构断代版（region 首例 b1.7.3 型、类树 abstract 断代型、WorldInfo 迁移型）仍需详表级专卷，但可从"全量钉表"降级为"断代判决+活体链"轻卷；另需先补 hooks 段 schema 统一（旧 schema 9 版）与 §3 缺口面的 F1 续扩面，否则断代版取证无处落码。
