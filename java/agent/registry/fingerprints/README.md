# 指纹锚库（fingerprints）

机器可读的版本目标锚库：把 verscan 手工调研批钉好的逐版字节码指纹（`<repo>/docs/compatibility/verscan/*.md`）与 0327 基线表（`<repo>/java/agent/src/com/mcworldagent/agent/WorldOps.java` 常量区 + `HookTargets.java`）转成 JSON，供 B1 对账与 B2（tiny 对账 / F1 锚库回归夹具）消费。本批=纯数据转换，零新算法、不改任何现有代码。

## 文件清单（9 件，每版一个 `<versionId>.json`）

c0.30-c ｜ in-20100223 ｜ inf-20100327（基线） ｜ inf-20100618 ｜ inf-20100629 ｜ a1.1.2_01 ｜ a1.2.6 ｜ b1.7.3 ｜ b1.8.1

## Schema

```
versionId      版本 id（与 verscan 文件名一致）
jarSha1        jar SHA1（主卷 §0 对榫表实算钉死；查不到=null 并在 provenance 注明）
jarState       三态：original | high-fidelity | none（本批 9 件全部 original，主卷 §0 逐行核对）
family         混淆世代：G1=Classic 半可读 com.mojang.* ｜ G2=包路径混淆 net.minecraft.* ｜ G3=扁平散件（0629 起）
targets        { 语义槽位名: { class, name, descriptor } }
capabilities   { op名: OK | fingerprint-unconfirmed | capability-absent | not-implemented }
provenance     { 槽位名: 判据来源一句话（javap/verscan§/WorldOps 行号/案卷§） }
confidence     { 槽位名: confirmed | candidate }
tinyDiff       预留（B2 tiny 对账用，本批一律 null）
```

## 槽位（targets key）命名约定

- 用 WorldOps 语义名：`worldClass / setBlock / getBlockId / saveWorld / worldTime / randSeed / spawnX|Y|Z / playerField / entityList / itemEntityClass / setEntityDead / playSound / teGet / invField / invMain / invArmor / invCur / addStack / isCount / isId / isDmg / itemClass / itemRegistry / setPosRot / posX|Y|Z / motX|Y|Z / yaw / pitch / health / iframes / entityType / getMeta / setMetaSilent / light6 / getLight / observers / difficulty / scatterClass / teBurn / teCook / invSize / invGet / invSet / explode / spawnEntity / worldCtor / runTick / guiIngame / chunkGenHeight / mobPig…mobBase` 等。
- `class` 一律点分 FQCN（内部名把 `.` 换 `/` 即得）；G3 扁平类无包时直接写字母类名（如 `ez`）。
- 字段槽的 `descriptor` = 字段描述符（`I/J/F/D/[L…;/L…;`）；方法槽 = 方法描述符；纯类槽 `name=null、descriptor=null`。
- **0327 件与 WorldOps.java 常量逐字段一致**（B1 自洽验收基准）：方法/字段槽 `name` = 对应常量值，类槽 `class` = 对应 G_* 常量值。注：A1 迁移批后 WorldOps 常量=`VersionRegistry.build0327()` 表值的别名（逐字段等值），权威值以该表为准，两处均已对榫通过。
- verscan 详表未钉出 name+descriptor 的槽位**不收录**（宁缺毋滥，收录=已钉）。

## capabilities 判据（35 op 清单 = CommandChannel.java KNOWN_OPS）

- `inf-20100327`：35 op 全 `OK`（实跑全绿 35 op，phase1 案卷）。
- 其余 8 版：详表明确钉出入口指纹面 → `OK`；详表明确断代/确认不存在（如 Classic 无 TileEntity/无 meta/无 NBT）→ `capability-absent`；详表标 ⚠️ 待细验/❓/仅注册表级证据 → `fingerprint-unconfirmed`；详表未覆盖的 op 面一律 `fingerprint-unconfirmed`。**不许猜**。
- `listops` 例外：agent 自省面零游戏调用，不依赖指纹，九版一律 `OK`。
- `not-implemented` 本批不启用，预留（agent 侧未实现的 op）。

## confidence

`confirmed` = 详表/实跑钉死且无歧义；`candidate` = 详表钉出但标注签名漂移/语义待复核（如 0629 saveWorld `a(Z)Z`、Alpha HUD `a(FZII)V`、0223 库存 b 数组含义未钉）。

## 脱敏纪律

文档与 JSON 内不写本机绝对路径；仓库内引用一律 `<repo>/…`，jar 位置沿用 verscan 的 `%APPDATA%` 相对写法或 `<workspace>` 占位。
