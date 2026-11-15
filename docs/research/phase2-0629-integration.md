# 案卷 — Phase 2 版本扩展 0629 接入批（G3 扁平散件首弹 + 双活体翻案）

日期：2026-11-15 ｜ 设计上游：[version-expansion-pilot.md](./version-expansion-pilot.md)（§5 逐版全表制、§5.1 边界线账本）｜ 详表：[verscan/inf-20100629.md](../compatibility/verscan/inf-20100629.md) ｜ 实跑日志：`integrations/attic/out/{el0629-1,el0629-2,el0629-3}/` ｜ 提交：`8560675`

> 本卷=0627/0629 形态分界（G2 包路径族→G3 扁平散件族起点）的第一次实弹：类名全部退化为根类短字母名（ez/at/gf/ht…）、字段可双字母（ab）、实体注册表反转为静态 Map 查表类。0629 收账=**26 OK / 9 fingerprint-unconfirmed**（entityType 维持 rejected：`em.r()`=基类恒 null 死桩）。

## 1. 交付物（按批）

| 批 | 内容 | 活体锚 |
|---|---|---|
| 离线取证 | 解包+SHA1 对榫（jarSha1=eb50bce3…）；逐面 javap：World=ez/Minecraft=at/GuiIngame=gf 全根类；**翻案①**：详表旧钉 `saveWorld=a(Z)Z` 系错——方法体首串 "TickNextTick list out of synch"=world tick 本体；真链=`public final a(Liw;)V` 包装→`private a(boolean,iw)`（level.dat_new/old 轮转+Saving chunks） | — |
| 表化 | `VersionRegistry.build0629()` 三档全表：档位一 null 槽 9 件（含翻案注）；档位二 `saveWorldDesc="(Liw;)V"`+`setPosRotDesc`；mobClasses 9 件（pig=hx…giant=aa，无 EntityList 内联注册差异）；`T_0629` 入 tables | — |
| 钩组 | `TARGETS_0629` 四钩（save 包装/loadWorld `at.a(ez,String)`/runTick `at.m()V`/HUD `gf.a(F)V`）+ `GROUPS` 三版查表激活；`Transformers` SAVE_WORLD 注入分支泛化 `startsWith("(Z")`（else 覆盖 ()V 与 (Liw;)V，ICONST_0 观察位）；WORLD_CTOR 有意省略=通道走 `cmd=` 覆盖（0618 同族判决） | el0629-1 |
| 锚 JSON | inf-20100629.json 全量重写：~30 targets、capabilities 26 OK/9 unconfirmed、saveWorldBody/progressClass 翻案留档 | — |
| 无头 | 假类 20 件（默认包 root0629）+ A2Table0629Test（JSON↔表逐槽对账）+ FakeOps0629Test（26 OK 面反射驱动+9 UNSUPPORTED 带槽名）+ InjectSmoke0629Test（钩组结构+织入冒烟）；run-headless 八类清单 | — |
| 活体 el0629-1 | 四钩织入+版本决议正确+**FIRST_BRICK_SKIPPED 首验**（方案 A 反转后不再现 FirstBrick 假警报）+SAVE_WORLD 直调/indirect=false；踩坑：启动漏传 `cmd=` → 通道静默（capturedSaveDir=null 无 CHANNEL_DIR） | el0629-1 |
| 活体 el0629-2 | 38 op 冒烟暴露**翻案②**：全部写面 ERROR "iw is not an interface"——真身 `iw`=**具体类**（`public class iw`，构造器 `iw(at)`；Minecraft 字段 `C:Liw`），Proxy no-op 方案作废；javap 复核：本体 null 守卫齐全（`ifnull@13/25`）且游戏构造器尾 490 处**自身裸传 null**、UI 进度调用 `a(this.C)` 走包装——ops 传 null 直调包装=游戏自身用法，语义正当 | el0629-2 |
| 翻案修正批 | WorldOps saveWorld 三分支（0 参/boolean/null）、noopProgress 删除；钩回挂 public 包装 0x0011；假类字节级复刻（iw=class、ez 包装委托+体内 null 守卫）；附带收获：**gt(EntityItem) 在 ht 注册面显名 "Item"**（静态扫描漏计第 12 件）、玩家类 da 未注册→类简名保底实锤 | 无头八类全绿 |
| 活体 el0629-3 | 38 op 全绿终账：12 写面 `saved=true`+显式 save×2；spawn/drop→`Item@`/entitylist N=277 注册名全显/clear 24 件/explode/playsound；give/inv/clearinv/setarmor；sethealth 18→20+getplayer 读回（`ab` 活体生效）；六件 UNSUPPORTED 带槽名（setmeta/grow/getinfo/invulnerable/getlight/genscatter）；getchest=REJECTED no-tileentity（预期——invSize 守卫需真实 TE 才触达，非版本缺陷）。协议参名补正 3 件重发全过 | el0629-3 |

## 2. 验收账

- **无头八类终值**：A2TableTest 1594 / A2Table0629Test 215 / FakeOps0618Test 36 / FakeOps0629Test 46 / InjectSmoke0618Test 17 / InjectSmoke0629Test 17 / VersionResolverA2Test 14 / REGISTRY-EQUALS 66+57（跑法=%TEMP%\mcaut\run-headless.ps1，测试件不入库）
- **活体账**：el0629-1（钩组+FIRST_BRICK 核验）、el0629-2（iw 翻案暴露）、el0629-3（38 op 终全绿）——inbox/outbox 全留存 `integrations/attic/out/<会话名>/mca/`
- **启动命令模板**（cmd= 必传）：`node dist/cli.cjs launch --version inf-20100629 --java <JDK8> --jvm-arg -Xbootclasspath/a:<asm> --jvm-arg "-javaagent:<jar>=log=…;hooks=…;cmd=…\mca"`
- **戒律执行**：全程不 push；commit 待用户指令（8560675）；committer=trae-solo-agent；jar 重建前确认无 java 进程

## 3. 教训（编号续 phase2-0618-integration §3）

20. **假类必须字节级复刻真身，否则无头假绿**：root0629 假 `iw` 写成 interface，Proxy 方案无头全绿、活体第一步即炸（"iw is not an interface"）。假类标准=对每个消费面类 javap 头三行对榫：access flag（class/interface）、构造器签名、被消费方法的可空性。无头绿≠活体绿，档位三终审不可绕过。
21. **依赖启动参数传信的机制=复发风险，须案卷固化命令模板**：0618 批曾判决"cmd= 覆盖代替 WORLD_CTOR 钩"（教训 18 的落点），本批 el0629-1 即因手工启动漏传 `cmd=` 通道静默半日。判决本身成立（0629 无 ctor 可钉成本更高），但启动命令模板必须随案卷入库、复跑照抄不凭记忆。
22. **静态注册面以活体输出对账为准**：ht 注册表 static{} 枚举"11 件"漏计 Item=gt——活体 entitylist 显名 "Item@" 才揭穿。注册面/命名面结论的最终校准时钟=活体冒烟输出，静态清单只作预测用。

## 4. 状态与下一步

- 0629=第三版全钉闭环收账（26 OK/9 unconfirmed）；9 件 unconfirmed（setmeta/grow/getinfo/light/scatter/invulnerable/chest 系）按版本迭代优先级规则待后续版本配方回流补钉。
- entityType 双版 rejected 同因（0618-Y/0629-em.r()=死桩）——若未来 op 需要类型面，改走 entitylist 注册名反查而非实体方法。
- 下一批候选：①a1.1.2_01~a1.2.0 生物群系带代表版（**能力边界版**——冒烟须加数据面读回 getblocks+id 表对拍，pilot §5.1）②B5/F6 锚 JSON 装载（消除硬表/JSON 双账，历史挂账）。
