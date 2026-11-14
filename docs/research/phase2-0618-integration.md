# 案卷 — Phase 2 版本扩展 0618 接入批（A3 终审 + 四批钉修 + FirstBrick 判决）

日期：2026-11-14 ｜ 设计上游：[version-expansion-pilot.md](./version-expansion-pilot.md)（§5 结论 1 逐版全表制、§7 工业化方案；本地 spec version-registry-a2）｜ 详表：[verscan/inf-20100618.md](../compatibility/verscan/inf-20100618.md) ｜ 实跑日志：`integrations/attic/out/{a3-0618-1,hud-*,et327-1,el0618-1,el0618-2,el0618-3}/` ｜ 提交：`f45617c`（四批合一）、`75adde2`（方案 A）

> 本卷=试点案卷"逐版全表制"预判的第二版实弹检验：0618 与 0327 同属 G2 包路径混淆族（字母漂移、拓扑同构），A2 三档建模（缺失/签名差异/语义差异）从纸面机制变为活体现实。0618 收账=**19 OK / 16 fingerprint-unconfirmed**（本线起点 16 OK）。

## 1. 交付物（按批）

| 批 | 内容 | 活体锚 |
|---|---|---|
| A3 实跑终审 | 0618 已钉面全量实跑：OK 面 16/16 + 持久化（place/save/退重进读回）+ 世代换捕（WORLD_SWITCHED）全过 | a3-0618-1 |
| ′ 翻案+LOAD_WORLD 钩 | 全 jar 扫描实锤 **0618 无游戏内周期 autosave**（仅换世界/停机调 saveWorld）→ 新增 LOAD_WORLD 钩：`Minecraft.a(World,String)` 入口 slot1=活体 World（InjectMV ALOAD1→onWorldLoaded）——持久化验证面的世界捕获通路补全 | a3-0618-1 |
| ′ HUD 0618 补钉 | GuiIngame=`client.c.m`、注入方法 `a(F)V` public final、操作码 0x0011 与 0327 同形；onHud 反射链字母两版全同，唯一差异 Entity pos 字母走 `VersionRegistry.current()` 表化 | hud-* |
| entityType 改钉 | **0327 旧钉 `q` 系贴图路径陷阱**（活体铁证 kind=/char.png、/mob/pig.png）→改钉 `a()String`（基类 protected abstract、消费侧须 getDeclaredMethod 链）；0618 `o()`=return 字段 Y=`ldc "/char.png"` 同族陷阱→**撤钉 null**、锚 JSON rejected 留档 | et327-1 / el0618-1 |
| EntityList 双轨 | VersionTable +`entityListClassG`/`entityListLookupM` 两槽；命名三通路=**EntityList 静态查表 `a(Entity)String` 主面**（描述符特征筛 static+String+单参，零类名硬编码，惰性解析缓存）→per-instance fallback→类简名保底。0618 钉 `j()List`（返字段 p=loadedEntityList：构造器 ArrayList/spawn p.add/tick 遍历移除/"All:" 调试串三重实锤）+`a.c.e`=EntityList（⚠️ 本版 a.c.e 非 0327 EntityLiving=同字母异义）+查表 `a`（static{} 注册 15 件，未注册返 null 走保底）；0327 查表槽留 null=维持现状零行为变化 | el0618-2：N=53=Item@×51+Sheep@+a@（玩家类简名，/char.png 已消） |
| healthF 补钉 | 0618=`Z`（javap `a.c.f` 三重实锤：写点 `a(c)V` ldc "Health"←putfield Z(i2s)、读点 `b(c)V` "Health"→getfield Z 缺 tag 保底 bipush 10、构造器默认 10；public int 本类声明）；capabilities getplayer/sethealth 转 OK；A2TableTest 转正断言+nullSlots 摘除 | el0618-3：getplayer health 9→sethealth 15→回读 15；entitylist 玩家带 `[h=15]` 而 51 个 Item 无 h 段（per-instance 探测如设计） |
| FirstBrick 判决（方案 A） | 五段验证目标已被通道面全量覆盖→**不再适配**；但其在 0618 每会话自动触发必发假警报（el0618-3 L44：0327 硬编码 spawn 字段 `g` 在 0618 为对象字段→IllegalArgumentException，失败语义兜住零损伤）→`noFirstBrick` 默认反转 true+`firstBrick=true` 显式回开键（0327 历史演示面保留） | 活体核验点=下次 0618 会话现 FIRST_BRICK_SKIPPED 不再现 UNSUPPORTED |

## 2. 验收账

- **无头五件套终值**：A2TableTest 1594 / FakeOps0618Test 36 / InjectSmoke0618Test 17 / VersionResolverA2Test 11 / REGISTRY-EQUALS 66+57（跑法=%TEMP%\mcaut\run-headless.ps1，测试件不入库）
- **锚 JSON**：inf-20100618.json 与 build0618() 逐字段等值断言在 REGISTRY-EQUALS 内；entityType rejected 留档、health confirmed
- **活体账**：a3-0618-1（16/16）、et327-1（0327 命名四件全绿）、el0618-1（翻案）、el0618-2（N=53）、el0618-3（health 闭环）——证据目录 `integrations/attic/out/<会话名>/`（仓外）
- **戒律执行**：全程不 push；commit 逐批待用户指令；jar 重建前确认无 java 进程占锁

## 3. 教训（编号续 phase1-command-channel §3）

15. **命名/展示类字段以 NBT 键配对为唯一硬证据**：构造器 `ldc "/char.png"` 型字符串常量=贴图路径强信号。0327-q 与 0618-Y 两版先后栽在同一族陷阱——签名 `(III)I→String` 相符、字母相邻、甚至活体曾"背书"（旧 q 时代 entitylist 输出过稳定字符串）都不构成充分性；写点+读点+默认值三点配对（healthF=Z 的三重实锤模板）才是钉字段标准。
16. **实体命名优先静态注册表面而非 per-instance 面**：EntityList 查表=单锚点、覆盖面可由 static{} 注册清单直接枚举验证（15 件）；per-instance 面逐类覆写形态不一（abstract/final/返贴图），且玩家类根本不在场。保底通路（类简名）必须保留——未注册实体是常态不是异常。
17. **档位一跳 null 的活体形态=输出面降级而非连坐**：healthF 未钉期 entitylist 自动省略 `[h=]` 段、getplayer 响亮 UNSUPPORTED、其余面照常全绿——"宁可响亮失败不静默"在 op 粒度上成立，这是 A2 三档建模的验收形态基准。
18. **bootstrap 期演示件要"判决关单"**：验证目标被新基础设施全量覆盖后，遗留件继续自动触发=每会话放血（假警报+白探测）。正确落点=默认关断+显式回开键（代码层根治），而非启动参数层约定（漏传即复发）；案卷保留回开路径，不删件。
19. **"无周期 autosave"是版本事实不是游戏事实**：0618 全 jar 扫描仅换世界/停机两处 saveWorld 调用点——持久化验证不能假设游戏会替你存盘，显式 save + 载入/换世界钩（LOAD_WORLD）是唯一可靠捕获通路；legacyfix 改 5 分钟存盘间隔的接管纪律（project_memory）跨版本同样成立。

## 4. 状态与下一步

- 0618=第二版全钉闭环收账；**16 件 fingerprint-unconfirmed**（give/spawn/explode/chest/light 等外围面）按"版本迭代优先级规则"不整批先补——待后续版本取证回流跨版配方后补钉成本更低。
- 下一批=**0629 接入**（a1.2.3/0627-0629/生物群系带/b1.8 四线账见 pilot §5.1）：0627/0629 形态分界（G2→G3 扁平散件起点）的第一次实弹——解包+SHA1 对榫→逐面 javap→表化→无头→活体冒烟（同流水线）。
- 能力边界版本（a1.1.2_01~a1.2.0 带）接入时，冒烟须加数据面读回（getblocks+id 表对拍）——本卷教训 15 的语义面延伸。
