# level.dat 写侧门与备份纪律设计稿（D7 level.dat 腿，§7.4 配套；本窗只做设计+读侧工具，无活写）

> 状态：**设计稿**（无人值守窗 W-5 产物，2026-11-27）。写侧实施须待活体验收窗；本文所有"已证"均有案卷/工件指针，"待钉"不得当作已证消费。

## 1. 读侧（已实现，两面）
- **Java 生产面**：[NbtReader.java](file:///e:/4k/github-projects/MCWorldAgent/java/storage/src/com/mcworldagent/storage/NbtReader.java) 树模型（Compound=LinkedHashMap，全消费校验=解析错位必 IOException，绝不静默出半棵树）；写侧对称件 NbtWriter 已随 D7 mcr 腿落地（StorageTest D7 段含真盘 level.dat 往返纪律+响亮拒绝 60/60）。
- **PS 取证面**：`%TEMP%\mcaut\nbt_walk.ps1`（W-1 原型→W-5 泛化）——全档 pos 精确递归（大/深结构类型化 Skip 保位点、BADTAG 响亮脱轨告警、END pos=size 自证）+ `-KeyPath 'Data/rainTime,...'` 类型化查询（Type 列=NBT TAG 权威宽度，装箱类型不作数）。三世界终验：W173 571/571、W181 1050/1050、W19P2 1063/1063 全消费。
- **教训固化（W-1 翻案）**：check 脚本字节名扫描把 tag 号读反（3=Int/4=Long 误记 TAG_Long）——**任何宽度断言必须出自类型化解析**，扫描法只配做存在性检查。b1.7.3.md §12 已按此翻案。
- **盘上宽度基线（W-1 读侧 + W-3 写侧 javap 互证）**：四版 beta（1501 dy/173 ei/181 rl/pre2 sx）`rainTime=m:I`/`thunderTime=o:I` 全 TAG_Int；`Time`/`LastPlayed`=TAG_Long；`raining`/`thundering`=TAG_Byte。GameType/MapFeatures=b1.8 起新增键（W173 盘无、W19P2 盘有，实扫吻合 b1.8.1.md §rl 判决）。

## 2. 写侧门（G 系，全部满足才放行）
- **G1 活锁门（§7.4 L0/L1 分档）**：目标世界 `session.lock` 存在=实例在持世界 → 离线工具**拒绝写入**（L1 档：只读查询可放行，写一律拒；不偷锁、不等待重试）。agent 持锁期间 level.dat 写面归 agent 的 saveWorld 权威路（工具侧并发写=竞态毁档）。
- **G2 版本门**：仅当该版本 WorldInfo 键集已在 verscan 钉死才可写（现役=0327..b1.7.3 系 World 内嵌键时代 + b1.5_01/b1.7.3/b1.8.1/b1.9-pre2 四版 WorldInfo 字母表）；草稿锚（b1.8.json 类，运行时不消费）拒绝。1501 特注：其 level.dat 写面依赖 §3-Q1 存储链补钉，未钉前 G2 不放行。
- **G3 树形保真**：写=读全树→改目标键→NbtWriter 全树回写；**禁字节级 patch**（未知键必须原样透传，树模型天然保证）。往返自证=写后重读 parse 全消费+目标键值对账。
- **G4 宽度保真**：键写宽=读时解码的 TAG 类型（Int 保持 Int），禁隐式加宽/收窄；对账脚本按 §1 基线断言。
- **G5 禁区**：session.lock 永不写；level.dat_old 由游戏自转（盘验四世界俱在），工具**不代管不删除**；region/chunk 文件与 level.dat 的跨档一致性（如 spawn 越界）不在本工具职责——只写请求的键。

## 3. 备份纪律（B 系）+ 待钉
- **B1 写前快照**：`level.dat → level.dat.bak-<unixms>`，滚动保留 5 份；快照失败=写取消。
- **B2 事后校验**：G3 自证不符 → 自动回滚最新快照 + 响亮报告（宁回滚不带半改档）。
- **B3 事故命名先例沿用**：整世界隔离=`<world>.<reason>bak`（World4.corruptbak / World5.quarantine 在盘先例）；单档回滚=直接覆盖+保留致损件为 `.corrupt-<unixms>`。
- **B4 杀竞态戒律沿用**（phase1-first-brick 案卷）：实例在档期间禁强杀进程；离线写只在确认实例已退出后执行。
- **待钉清单**：Q1 b1.5_01 存储链类字母/region 语义（b1.5_01.md §6 全待钉，level.dat 在 1501 的 Data 键集差异未终核）；Q2 pre2 天气键大小写矛盾（b1.9-pre2.md ⚠️：note③ 大写 vs ⑬ 小写 vs 173 盘小写——W19P2 实扫=小写命中，倾向小写终判，待正式回写）；Q3 beta level.dat 是否存在尾部校验和字段（1.2a+ 才有 MATEC 系印象，本窗未取证——G3 全消费校验已天然兜底：若存在未知尾随结构，pos≠size 即拒写）。

## 4. 接入位置（实施批预留）
- 工具侧新类 `LevelStore`（java/storage，与 RegionStore 同层）：`read(File)→Map`（NbtReader 复用）、`writeKeys(File, Map<String,Object> patch)`（G1-G5+B1-B3 编排）；**agent 不新增 op**（戒律：agent 不写世界文件，写面走 saveWorld 权威路；离线工具在测试/工具侧）。
- 活体验收判据预登记：写前快照在盘、写后重读对账、游戏重入读回三缺 1=FAIL；G1 门用"实例在跑时拒写"负例验收。
