# 案卷 — 0327 World 接口覆盖审计（2026-11-14 开工，进行中）

状态：**主体毕**（2026-11-14，判决表见 §4；残项 §5 不阻塞拍板）｜ 触发：用户判决"javaagent 推进=先审清 0327 覆盖度，版本扩展（跨阶段试点）次优先"｜ 基础设施：`%TEMP%\mcaut\audit327\{inventory.txt(287类方法面), all-c.txt(全量反汇编 4.2MB), classes.txt, dump-m.ps1}`

## 1. tick 面（钉毕，含一次自我翻案）

**jar/树 方法对应（全部字节码实锤）**：

| jar（Block 基类=`a.a.b.x`） | 可读树名 | 驱动点（jar） | 实现者 |
|---|---|---|---|
| `a(f,IIILjava/util/Random;)V` | **updateTick** | ①`World.h()` 前半：nextTickList 队列（`a.a.b` 类 a/b/c=xyz，`i4>0` 门，=树 L1142）②`World.h()` 后半：**每 tick 32000 次 LCG 散布**（`updateLCG*3+MAGIC`，玩家脚部 ±128 格、y∈[0,127]，门控 `x.d:[Z`=Block.tickOnLoad 静态布尔数组，=树 L1149-1158） | **12 类**：ab/ac/ad/af/ah(火)/ai/b(岩浆流)/f/m/o/p(液体基)/z(树苗) |
| `b(f,IIILjava/util/Random;)V` | **randomDisplayTick** | `World.randomDisplayUpdates`（树 L1162-1173，纯粒子） | 4 类：ac/ah/p/s（"smoke" 字符串=液体飞溅/火焰粒子，吻合） |
| `a(I,Random)I` | idDropped | （dig 掉落，见 §2 未完） | — |

**翻案记录**：初扫曾误判"jar 双 tick=物理/生长拆分、x.b=散布"——回溯行号实锤 h() 两调用点**同为 x.a**（51071 上下文 `x.d:[Z baload` 门控即树 L1155 tickOnLoad），x.b 是 randomDisplayTick。教训=调用者归属必须看调用行本身，不能只看行序距离。

**grow op 语义面成立**：
- sapling（x.z）updateTick=光照≥9 + nextInt(5)==0 → meta+1（<15），满 15 重挂自身（阶段推进，**不长树**=meta-1 翻案维持）；
- 12 实现者含草蔓延/作物/花/农田/树叶 decay/火把/火/液体——**0327 生长/蔓延面比 meta-1 判决时预想的宽**；
- 配方候选：grow op 直调 `blocksList[id].a(World,II,Random)V`（单次=概率语义，或循环至状态变化=确定性催熟——落码批再拍）；
- 红石线**无** updateTick 实现 → 0327 红石纯邻接更新（onNeighborBlockChange），**setmeta 已覆盖其状态面**，红石无独立 op 必要（疑点销案）。

## 2. 疑点销案（javap 实锤，2026-11-14 续）

- **biome：不存在**。World 类零 `[B/[S` 群系字段、零 Biome 类（jar 无）——0327 地形纯噪声无群系概念（Alpha 引入）。getbiome 类 op **不可行**，销案。
- **TE 全集：base+2**。`a/a/b/a/{a,b,c}`=TileEntity 基类+chest+furnace，**无第三个**（假类建 2 正确）；getchest/getfurnace 已全覆盖 TE 面，销案。
- **结构生成：无独立可触发接口**。生成器包 `a.a.c` 7 类=WorldGenerator 基类+Minable+Trees(=散布器，meta-1 翻案) + noise 家族；**mineshaft/dungeon 无独立类**（0327 内联于 chunk 主生成器，不可单独触发）→ 结构 op **不做**（复刻 ChunkProviderGenerate=超预算），销案。
- **chunk 级操作：无缺口**。ChunkProviderLoadOrGenerate 面=chunkExists/loadOrCreate/磁盘读/saveAll——无 delete/regenerate 语义；chunk 读写已被 fill/getblocks/place/dig 覆盖 → 无独立 op，销案。
- **红石：无周期 tick**。红石线类不在 updateTick 12 实现者 → 邻接更新驱动，setmeta 已覆盖状态面（§1 已记）。

## 3. 掉落面（harvest 输入，半钉）

idDropped（`x.a(I,Random)I`）实现者 **9 类**：aa/ab/af/aj/c/e/f/o/r（树对照候选=Grass/LeavesBase/Gravel/Ore/Sand/Mushroom/Torch/Crops 等——身份细对留落码批）。**harvest 判决预评**：掉落换算面真实存在且非平凡（草→泥土、树叶→树苗/苹果概率），但 give 可代劳终态——harvest 增量=概率掉落由原版规则代劳。**待拍：做（digAndCollect=World.removeBlockWithNotify+捕获 EntityItem 转 give）或不做**。

## 4. 三分判决表（审计主体，2026-11-14 钉）

World 全方法面 67 员 × 33 op 对照毕（Entity/Player/Inventory/TE 面历批已钉，残差如下）：

**① 已覆盖**：方块读写（place/dig/getblock/getblocks/fill/replace）、meta/light（setmeta/getmeta/getlight）、属性（getinfo/settime/setspawn/save）、玩家（getplayer/tp/setlook/setmotion/sethealth/invulnerable）、物品栏（give/inv/setarmor/clearinv）、实体（spawn/drop/explode/entitylist）、TE（getchest/setchest/getfurnace）、音（playsound）、自省（listops）。

**② 缺口→值得 op（候选批）**：
- **grow**：直调 `Block.blocksList[id].a(World,II,Random)V`（updateTick，§1 钉毕 12 实现者）；辅助面 `World.k(III)`=scheduleUpdate 可入 nextTickList 队列。拍板项=单次概率语义 vs 循环确定催熟。
- **clear**（实体清除）：`World.b(Entity)`=**static setEntityDead 入口实锤**（树 L535-537）——entitylist 有枚举无删除，obs-1 实跑"用户手清实体"=真实痛点；配方=entitylist 索引→b(Entity)。
- **groundLevel**（低优先）：`a(II)I` 找地面高（spawn 搜索件）——getblocks 扫描可代劳，便利性非能力面。

**③ 缺口→不做（有因）**：biome（0327 不存在）；结构生成（无独立可触发接口）；chunk 操作（已被方块面覆盖）；rayTrace `a(Vec3D,Vec3D)`（T2 UI picking 面，非 op）；lightmap float 系 `a..e(F)`+`g(III)F`（渲染内部件，`t:[F` 查表实锤）；level.dat 静态工具 `a(File,String)`/`b(File,String)`（getinfo 已覆盖读）；setname（低价值）；实体 NBT 注入（高风险低回报）；光照手动通知（setmeta 批已复刻）。

**harvest 待拍**：idDropped 9 实现者真实非平凡（草→泥土/树叶概率掉落），但 give 代劳终态——做=「digAndCollect」或不做，用户拍板。

## 5. 未完面（残项）

- [ ] idDropped 9 类身份逐一对照（树语义）——grow/clear 批不依赖，harvest 才需
- [ ] tickOnLoad 启用面（x.g(Z) 调用者扫描落空，x.g 语义待复核）——grow 适用范围输入
- [ ] `a(II)I` 方法体复核（groundLevel 若拍板做）

**审计状态：主体毕**（tick/掉落计数/biome/TE/结构/chunk/World 67 员对照全钉；②三候选+harvest 待拍板进批）。

## 6. plan.md 交叉复核（2026-11-14 续）

- **plan.md §4.5 remaining 清单过时已修正**（metadata/entity/tp/batch 全历批验收；真剩余=getChunkFromChunkCoords 深钩+命令台）。
- **硬约束碰撞**：plan.md §3.4/§4.3"检查不得隐式生成地形"× 本案卷 getblocks 就地生成实锤 → **chunk 导出批第一设计约束**=只读导出必须 chunkExists 门控/直读盘，禁走 loadOrCreate。深钩（Phase 0 教训 5 的 1729/639 缺口）与导出批同题合拍。
- **T2 定性修正（workbench-model 已同步）**：plan.md §12 垂直切片 9 步中 1/5/6/7/8/9 已由 agent 全链完成，**唯缺步 2/3/4（读世界数据+3D 显示）**——chunk 导出+Three.js 不是"壳打磨前置"而是**里程碑收口件**。
- **Registry 合流**：本案卷三分判决表=plan.md §2.1 Version Compatibility Registry 的 0327 首条原料；listops v2 五态=其 agent 侧机器可读形态（workbench-model §6 判决）。
- **plan.md 有、本案卷未覆盖的新缺口候选（登记待拍）**：①实体生命周期事件跟踪（§4.7——entitylist=快照非事件流，无 add/remove 钩）；②undo/rollback（§7.3——从未进队，通道现单命令同步）。
- **分析面（§4.8 slime/monolith/pyramid）**=离线算法集成（seed 已由 getinfo 出账），不占 op 队列，Phase 5 归位。

## 7. chunk-export 批完成记录（2026-11-14，审计→落码当日闭环）

审计 §6 硬约束直接催生本批：**java/storage 纯离线件**（NbtReader 11 型 tag+全消费校验、ChunkExport CLI b36 路径→JSON RLE），静态存档实跑 `chunk-export-1` **10 项断言全绿**（Furnace TE/items=te-1 账面逐项、cobble=51≥49=obs-1 石台账、heightMap 合理域、present:false、2813 文件哈希零写入）。索引公式翻案入档：**Blocks idx=x<<11|z<<7|y（y 最内）**——spec 初版假设 x|z<<4|y<<8 被 Chunk.a(III)I 字节码推翻（"字节码=证据"纪律再立功）。无头 38/38。Three.js 真渲染（§12 步 4）数据源自此就绪=T2 开工无 agent 侧阻塞。
