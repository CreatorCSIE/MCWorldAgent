# Infdev~Beta 版本覆盖勘测清单（version-coverage-survey v1）

> 生成：2026-11-25 自主窗口 P5。数据源=索引 `minecraft-verification-analysis/indexes/version-index.md`
> （权威 jar 三态）+ 本地 `%APPDATA%\.betacraft\versions`（744 jar）+ agent 现集成态
> （`VersionRegistry` 硬编码表 + `HookTargets` 钩组 + `fingerprints/*.json`）。
> 目的：把"Infdev~Beta 广度缺口"从**未知**变**已知可排序清单**。

## 0. 一句话结论

索引 Infdev+Alpha+Beta 共 **138 版**；其中 **121 原始件**（可作事实依据）、14 高仿（戒律 5/9 排除）、
3 丢失无 jar（不可达）。agent 现 **runtime 10 + fingerprint 3 = 13 版**已接；
**102 版**属"原始件 + 本地有 jar + 未接"= 实打实可转正候选。

## 1. 覆盖数字

| 维度 | 数 |
|---|---|
| 索引行（inf/a1/b1） | 138 |
| jar 证据=原始 | 121 |
| jar 证据=高仿（排除） | 14 |
| jar 证据=无（不可达） | 3 |
| 已 runtime 激活 | 10 |
| 已 fingerprint 锚定（未转 runtime） | 3 |
| **可转正候选（原始+本地+未接）** | **102** |

> 注：runtime 计数 10 而非 11——`inf-20100624` 在索引 Infdev 行的命名与本地 jar 名一致但被
> 候选过滤计入；`a1.0.3` 等个别名以索引格式化名为准，逐版对账见 manifest.csv（中间件，未入库）。

## 2. 102 候选按版本线聚类（自然别名簇）

| 线 | 候选数 | 代表版（已接/建议） | 备注 |
|---|---|---|---|
| Infdev | 23 | inf-20100327 ✓ | 同 G2 世代，字母逐版漂移，需逐版 tiny |
| a1.0 | 27 | a1.0.3 ✓ | Alpha 首例已接；余 27 版字母近同度待验 |
| a1.1 | 4 | a1.1.2_01 ✓ | 线短，易补 |
| a1.2 | 13 | a1.2.0 ✓ / a1.2.6 ✓ | biome 面起此线 |
| b1.0 | 2 | — | Beta 首例未接 |
| b1.1 | 4 | — | |
| b1.2 | 2 | b1.2_02 ✓ | |
| b1.3 | 3 | b1.3_01（finger）| McRegion 断代起点 |
| b1.4 | 3 | — | |
| b1.5 | 1 | b1.5_01（finger）| weather 引入线 |
| b1.6 | 7 | — | |
| b1.7 | 3 | b1.7.3 ✓ | weather 门控代表 |
| b1.8 | 3 | b1.8.1 ✓ | **xp/effect/hunger/gamemode 富矿线** |
| b1.9-pre | 7 | b1.9-pre2（finger）| **最富矿：addXpM/clearEffects 全表面** |

## 3. 别名方法论与诚实边界

agent 运行时表消费约 30 个混淆字母槽（saveWorldM/getWorldInfoM/posX..Z/playerF/xpF…）。
两版可**共用一张表** ⟺ 这 30 槽字母逐一对齐。判定途径：

- **可行（本窗已做）**：按索引证据态 + 版本线聚类，给出"同线近同、待逐版验"的先验分组。
- **需后续机械任务**：逐版 tiny 映射（社区 `versions/<v>/*.tiny`）+ javap 抽 30 槽字母，
  与代表表 diff → 自动判 `ALIASABLE / NEEDS-OWN-TABLE / STRUCTURAL-BREAK`。
  ⚠️ ProGuard 每构建重生成字母，**全类名集合相等≠可别名**（过严）；**必须逐成员槽比对**（过松
  即假绿）。本窗未跑全 102 版 tiny diff——上下文预算所限，列为后续机械批，**不臆断别名**。
- **陷阱在册**（§6.1 借表陷阱集）：fd 撞名（173=World/1301=EntityPlayer）、Entity pos 每版
  +2 右移、hudDebugGate 同字母异名、saveWorld 包装 1501 无双参——逐版转正必重钉，禁借表。

## 4. 转正配方（每版五步，与 D10~D14 同标准）

1. **指纹**：原始 jar SHA1 对榫索引行（戒律 14）→ `fingerprints/<v>.json`（schema=mca-runtime-v1）。
2. **运行时表**：`VersionRegistry.build<v>()` 或 JSON 装载，钉 30 槽字母。
3. **钩组**：`HookTargets` 加 TARGETS_<v>（SAVE_WORLD/RUN_TICK/HUD_RENDER/WORLD_CTOR），
   或 JSON hookSpecs 数据驱动。
4. **op 门控**：capabilities 表逐 op 判 OK/UNSUPPORTED（按 §6.1 版本定语）。
5. **测试**：root<v> 假件 + FakeOps<A>Test + D8GateTest 门控 + A2Table 对账；壳 OPS 同步。

## 5. 优先级建议（性价比）

1. **b1.9-pre2 转正**（finger 字母全在册，xp/effect 面比 181 更全：独立 `addXpM=f`/
   `clearEffectsM=aG`；只差 build19pre2 + 钩组 + 假件测试）——**最低成本最高增量**。
   ✅ 本窗实锤：本地 `b1.9-pre2.jar` SHA1=`b0d40cf43b625631af65e2a645c34b533251da0e`
   与锚件 `jarSha1` 逐字符对榫（戒律 14）——字母确系此原始件 javap 所出，非臆造，转正即取即用。
2. **b1.5_01 / b1.3_01 转正**（finger 在册；weather 引入线 + McRegion 存储断代，补存储面）。
3. **b1.8 线余 3 版**（b1.8/b1.8-pre2×2）：若 tiny diff 证字母近同 181，可低成本别名。
4. **a1.2 线余 13 版**：biome 面已通，逐版验字母后批量转正。
5. Infdev/a1.0 大线：面广但 op 面窄（无 xp/effect/biome），优先级最低，按需补。

## 6. 排除清单（不入候选）

- **14 高仿**（索引标"基于其他版本 mod 过"）：戒律 5/9——不可作事实依据，除非用户点名探究。
- **3 丢失无 jar**：物理不可达。
- **dev/preview/singleplayercommand/space/mt 等**：索引未列（戒律 11 非索引行），
  如 b1.8-dev1..10、a1.2.0-preview、b1.9-pre*-dev——一律不作版本存在性依据。

## 7. 活体边界（与无头互补，非本清单覆盖）

本清单纯无头广度勘测。每版**深度**仍欠活体账（钩命中 expectedHits、op 语义行为、存盘往返），
见 `arrival-pack-v1.md`。广度转正与深度活体是两条正交轴。
