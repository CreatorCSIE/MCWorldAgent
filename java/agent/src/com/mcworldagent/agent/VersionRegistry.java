package com.mcworldagent.agent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 版本登记表（A1 迁移批 + A2 双表）。表=inf-20100327（值与迁移前 WorldOps 常量
 * 逐字段等值，无头等值断言=验收）+ inf-20100618（锚库
 * java/agent/registry/fingerprints/inf-20100618.json 数据化，三档建模见 spec
 * .trae/specs/version-registry-a2/spec.md）。
 * A2 激活模型：premain 期 VersionResolver 选表 → activate(versionId)；
 * WorldOps 类初始化在其后，static final 表值别名即取激活表（现网时序天然保证，
 * 0327 现行为零变化）。
 * 取证出处速查（全量证据在案卷，此处只留指针）：
 *   World 写读/时间/出生点  = docs/architecture/agent-command-channel + phase1-first-brick
 *   tp/实体定位             = tp 批 javap 全量反汇编 303 类
 *   物品栏 give/inv         = give 批案卷
 *   spawn/explode/drop      = spawn-explode-drop 批（源码树+javap 双路）
 *   grow/clear              = .trae/specs/grow-clear/spec.md（骨粉=官方强熟判决）
 *   invulnerable            = sed-1 批（attackEntityFrom 单入口实锤）
 *   tileentity              = tileentity-batch 案卷
 *   meta/getlight/genscatter = meta 轮（含翻案记录）
 * 后续批：A2=0618 表接入（含方法缺失/签名差异/语义差异三档建模）；
 * B5/F6=JSON 装载替换本硬编码工厂（锚库见 java/agent/registry/fingerprints/；
 * B5 已落地=JSON 并存装载见 static 块尾注，硬编码优先，8 表行为零变化）。
 */
public final class VersionRegistry {

    private VersionRegistry() {
    }

    private static final VersionTable T_0327 = build0327();
    private static final VersionTable T_0618 = build0618();
    private static final VersionTable T_0629 = build0629();
    private static final VersionTable T_1121 = build1121();
    private static final VersionTable T_120 = build120();
    private static final VersionTable T_1202 = build1202();
    private static final VersionTable T_173 = build173();
    private static final VersionTable T_181 = build181();
    private static final VersionTable T_19pre2 = build19pre2();

    private static final Map<String, VersionTable> BY_ID = new HashMap<String, VersionTable>();
    private static final Map<String, VersionTable> BY_SHA1 = new HashMap<String, VersionTable>();

    static {
        VersionTable[] tables = { T_0327, T_0618, T_0629, T_1121, T_120, T_1202, T_173, T_181, T_19pre2 };
        for (VersionTable t : tables) {
            BY_ID.put(t.versionId, t);
            BY_SHA1.put(t.jarSha1, t);
        }
        // a1.2.0 duplicate 陷阱（详表 a1.2.0_02.md §0）：原始 2057 候选件（betacraft
        // 43ffc4de，内嵌 stamp=v1.2.0）与 manifest 件（8632ea71=_02）7 class 签名级
        // diff 零——BY_SHA1 别名让两件自动同表；活体旁证批 el1257-* 验证映射无偏移。
        BY_SHA1.put("43ffc4de410512c3ca10dd5cd9812ea5df5dc0ed", T_120);
        // b1.2_02 两态同名版（详表 b1.2_02.md §0）：纯独立构建件 551be326（内嵌
        // stamp=Minecraft Beta 1.2_02，主表 jarSha1）与官方 manifest LW 重打包件
        // 093f371e（mainClass=Launch，1136276B，sidecar 实拉件）——BY_SHA1 别名两件
        // 同表；游戏类表同形已由借 b1.2 tiny 15/15 classlist 核验+方法级对榫实锤，
        // 活体冒烟 el1202-* 复核。
        BY_SHA1.put("093f371e1a05d89664cfb8068d607953687d5d94", T_1202);
        // B5：JSON 运行时装载（agent 资源 fingerprints/*.json，schema=mca-runtime-v1，
        // 见 FingerprintLoader）。硬编码优先——已有硬编码表的 versionId/SHA1 不采纳
        // JSON 表（防回归）；未知版行为不变（无表=activate false→回退默认）。
        List<VersionTable> json = FingerprintLoader.loadAll();
        for (int i = 0; i < json.size(); i++) {
            VersionTable t = json.get(i);
            if (BY_ID.containsKey(t.versionId) || BY_SHA1.containsKey(t.jarSha1)) {
                MCWorldAgent.logTransform("VERSION-JSON-SKIP id=" + t.versionId
                        + " hardcoded-wins");
                continue;
            }
            BY_ID.put(t.versionId, t);
            BY_SHA1.put(t.jarSha1, t);
            MCWorldAgent.logTransform("VERSION-JSON-LOADED id=" + t.versionId
                    + " sha=" + t.jarSha1);
        }
    }

    /** 激活表（premain 前默认 0327=现行为零变化；VersionResolver 命中后切换）。 */
    private static volatile VersionTable active = T_0327;

    public static VersionTable current() {
        return active;
    }

    public static String currentVersionId() {
        return active.versionId;
    }

    /** 按版本 id 激活；未知 id=false 不动激活态（调用方回退默认表）。 */
    public static boolean activate(String versionId) {
        VersionTable t = BY_ID.get(versionId);
        if (t == null) {
            return false;
        }
        active = t;
        return true;
    }

    /** 按原始 jar SHA1 查表（戒律 14 严格指纹；无匹配=null）。 */
    public static VersionTable byJarSha1(String sha1LowerHex) {
        return sha1LowerHex == null ? null : BY_SHA1.get(sha1LowerHex);
    }

    /** B5：按版本 id 查表（HookTargets JSON 钩组构造消费；无表=null）。 */
    public static VersionTable byVersionId(String versionId) {
        return versionId == null ? null : BY_ID.get(versionId);
    }

    private static VersionTable build0327() {
        VersionTable t = new VersionTable();
        t.versionId = "inf-20100327";
        t.family = "G2"; // 包路径混淆 net.minecraft.a.*
        t.jarSha1 = "47b1b32430a211520993552ba0a5e00c1af44724"; // HookTargets 头注同源

        // World 写读面（FirstBrick T1 javap 实锤；worldTime=NBT "Time"，randSeed=NBT "RandomSeed"）
        t.setBlockM = "c";   // setBlockWithNotify (IIII)Z
        t.getBlockM = "a";   // getBlockId (III)I
        t.saveWorldM = "a";  // saveWorld (Z)V private
        t.saveWorldDesc = "(Z)V";
        t.worldTimeF = "b";
        t.randSeedF = "x";
        t.spawnXF = "g";     // NBT "SpawnX"（g/h/i 唯一读点=EntityPlayer 构造器）
        t.spawnYF = "h";
        t.spawnZF = "i";

        // 玩家/实体定位面（Entity 基类 net.minecraft.a.c.b；World.c=活体玩家自设）
        t.playerF = "c";
        t.setPosRotM = "b";  // setPositionAndRotation (DDDFF)V=NBT 载入同一配方
        t.setPosRotDesc = "(DDDFF)V";
        t.posXF = "g";
        t.posYF = "h";
        t.posZF = "i";
        t.motXF = "j";
        t.motYF = "k";
        t.motZF = "l";
        t.yawF = "m";
        t.pitchF = "n";
        t.healthF = "W";     // EntityLiving.W（NBT "Health" 双向绑定，默认 10）
        t.iframesF = "K";    // Entity.heartsLife（写 >=11=免疫窗口；瞬态不进 NBT）

        // 物品栏面（InventoryPlayer a[36]/b[4]/c 当前槽；ItemStack a=Count,c=id,d=Damage；
        // Item 注册表 i.b[] 按 id 索引部分槽 null——give 必须注册表校验防 NPE）
        t.invF = "b";
        t.invMainF = "a";
        t.invArmorF = "b";
        t.invCurF = "c";
        t.isCountF = "a";
        t.isIdF = "c";
        t.isDmgF = "d";
        t.addStackM = "a";   // addStack (ItemStack)Z（同 id 且 dmg==0 才合并，栈上限 64）
        t.itemClassG = "net.minecraft.a.b.i";
        t.itemsF = "b";
        t.itemEntityG = "net.minecraft.a.c.c.b"; // EntityItem (World,DDD,ItemStack)V

        // 实体/世界事件面（createExplosion=逻辑全在 World，Explosion 类空标记；
        // spawnEntityInWorld=loadedEntityList.add 加入即活；0327 saveWorld 不写实体=恒 transient）
        t.explodeM = "a";        // (Entity,DDDF)V，source=null 零 NPE 但不豁免玩家
        t.spawnEntityM = "a";    // (Entity)V
        t.setEntityDeadM = "b";  // (Entity)V **static**，仅置 isDead，真移除在 tick
        t.entityListM = "i";     // getLoadedEntityList ()List
        t.entityTypeM = "a";     // getEntityType ()String——A3 javap 改钉（2026-11-14）：
        // 旧钉 q 系错钉=q 实为贴图路径（obs-1/grow-1 活体铁证 kind=/char.png、/mob/pig.png）。
        // 真身 a()String：基类 **protected abstract**，EntityPig public final 覆写返 "Pig"、
        // TNT protected final 覆写返 "PrimedTnt"→消费侧 getMethod 必败，须 getDeclaredMethod 链。
        // EntityList 查表面（entityListClassG/entityListLookupM）0327 留 null=维持现状：
        // per-instance a() 已活体全绿（et327-1：Pig/PrimedTnt/LocalPlayer/Item），零行为变化。
        t.playSoundM = "a";      // (DDD,String,F,F)V，不存在音效=静默 no-op

        // 方块 meta/光面（可读树名 setBlockMetadataWithNotify 系 deobf 误猜——b(IIII)V 实为静默写；
        // getBrightness=SkyLight 减环境值与 BlockLight 取 max 纯读；散布器=椭球矿石团生成器，
        // 命中格仅现为 stone 时写入，恒返 true）
        t.getMetaM = "d";        // (III)I
        t.setMetaSilentM = "b";  // (IIII)V 仅 nibble+dirty 零通知
        t.light6M = "e";         // (IIII)V 光照 6 邻
        t.getLightM = "f";       // (III)I
        t.observersF = "u";      // private List 渲染观察者
        t.difficultyF = "d";     // 每帧被 GameSettings.difficulty 覆写——写无效
        t.scatterClassG = "net.minecraft.a.a.c.b.b";

        // tileentity 面（TE 基类 a.a.b.a.a；Chest=a.a.b.a.c 27 槽；Furnace=a.a.b.a.b 3 槽，
        // getInvName 返回 "Chest"=原版笔误——类型判用字段/槽数面；TE 持久化随 chunk，
        // 创建/删除=Chunk.setTile 回调自动；IInventory 三方法从实例类 getMethod）
        t.teGetM = "h";          // getBlockTileEntity (III)ITE
        t.invSizeM = "a";        // ()I
        t.invGetM = "a";         // (I)Lk;
        t.invSetM = "a";         // (I,Lk;)V 直替换无合并
        t.teBurnF = "f";         // furnaceBurnTime（NBT 对榫）
        t.teCookF = "h";         // furnaceCookTime

        // 游戏 id（crops=59 texture 88；sapling=6；终态 meta=7=骨粉语义一步满熟；
        // growTree 首现 Beta 1.2——0327 树苗 UNSUPPORTED 响亮）
        t.idCrops = 59;
        t.idSapling = 6;
        t.cropsRipeMeta = 7;

        // spawn 白名单（六 mob 构造器全 (World)V；tnt=(World,FFF)V fuse=80 内置；
        // giant 运行时 jar 构造器被死代码剥离→transform NEW_CTOR 补全复活；
        // mob=EntityCreature 基类 a.c.c 构造器健在，渲染回退 ModelBiped 人形）
        t.mobClasses.put("pig", "net.minecraft.a.c.b.c");
        t.mobClasses.put("sheep", "net.minecraft.a.c.b.b");
        t.mobClasses.put("creeper", "net.minecraft.a.c.a.d");
        t.mobClasses.put("skeleton", "net.minecraft.a.c.a.c");
        t.mobClasses.put("zombie", "net.minecraft.a.c.a.f");
        t.mobClasses.put("spider", "net.minecraft.a.c.a.b");
        t.mobClasses.put("tnt", "net.minecraft.a.c.c.a");
        t.mobClasses.put("giant", "net.minecraft.a.c.a.a");
        t.mobClasses.put("mob", "net.minecraft.a.c.c");

        return t;
    }

    /**
     * 0618 表（A2）。值源=锚库 java/agent/registry/fingerprints/inf-20100618.json
     * （逐字段对榫=无头验收之一）；取证=docs/compatibility/verscan/inf-20100618.md。
     * 三档建模（spec §3）：null 槽=档位一缺失/未钉（op 入口响亮 UnsupportedOp）；
     * saveWorldDesc/setPosRotDesc=档位二签名差异显式建模；档位三语义差异（a(DDD)V
     * 直写 h/i/j、AABB 重建未钉，依赖 mover 每 tick 自愈）=A3 实跑终审。
     * candidate 槽（saveWorld/setPosRot/invArmor）照常入表消费——严格签名匹配即保底。
     */
    private static VersionTable build0618() {
        VersionTable t = new VersionTable();
        t.versionId = "inf-20100618";
        t.family = "G2";
        t.jarSha1 = "89eab2c1a353707cc00f074dffba9cb7a4f5e304"; // 主卷 §0 对榫表

        // World 写读面（verscan §1；⚠️ 本版 a.a.g=World，a.a.f=光照队列条目——同字母异义陷阱）
        t.setBlockM = "c";        // (IIII)Z=setBlockWithNotify 等价（a(IIII)Z+h(IIII)V 通知）
        t.getBlockM = "a";        // (III)I
        t.getMetaM = "d";         // (III)I getBlockData
        t.saveWorldM = "b";       // private b(Z)V core——对齐 F1 裁决 2026-11-21（0618 与 0327
        t.saveWorldDesc = "(Z)V"; //  saveWorld 调用图同型，统一 core (Z)V 从 0327 先例；javap
        //  实锤 g.b(boolean) 唯一 (Z)V 重载，体写 level.dat/RandomSeed；构造器尾 468 处
        //  iconst_0 直调=0327 把手①同配方。0618 无在册活体线，phase2-0618 历史集成件为
        //  旧包装 f()V 形态。ops 传 false=游戏自身构造尾同参，语义无破坏）
        t.worldTimeF = "b";       // long=time
        t.randSeedF = "m";        // long=seed
        t.spawnXF = "i";          // i/j/k=spawn（⚠️ 与 teGetM=i 方法同名合法——Java 字段/方法分命名空间）
        t.spawnYF = "j";
        t.spawnZF = "k";
        t.playerF = "e";          // 声明 Entity=a.c.b，运行时 client.g.a
        t.teGetM = "i";           // (III)ITE（a.a.b.a.a 基类）

        // 实体定位面（verscan §3；pos=h/i/j ⚠️ 非 0327 g/h/i，独立钉）
        t.setPosRotM = "b";       // setPositionAndRotation b(DDDFF)V public final——对齐 F1 扩面
        t.setPosRotDesc = "(DDDFF)V"; // 翻案 2026-11-20（原钉 a(DDD)V 纯 setPosition，与 a1.2.6
                                      // 同型重载错位；javap 实锤 b(DDDFF)V 唯一 5 参重载）
        t.posXF = "h";
        t.posYF = "i";
        t.posZF = "j";
        t.motXF = "k";
        t.motYF = "l";
        t.motZF = "m";
        t.yawF = "n";
        t.pitchF = "o";
        t.healthF = "Z";     // EntityLiving(a.c.f).Z——healthF 补钉批 2026-11-14 javap 三重实锤：
        // 写点 a(c)V: ldc "Health"←getfield Z:I(i2s)；读点 b(c)V: "Health"→putfield Z，
        // 缺 tag 保底 bipush 10；构造器默认 bipush 10→Z（与 0327 W 同语义）。
        // public int 声明于 a.c.f 本类——getPlayer/setHealth 的 per-instance getField 可中。

        // 物品栏面（verscan §4；inv/clearinv=OK。⚠️ 本版 ItemStack=a.b.o 非 0327 k；
        // a.b.k=Item 基类=高危陷阱。invArmor=candidate b——盔甲尺寸未钉但数组面存在）
        t.invF = "b";             // EntityHuman(a.c.e.a).b=InventoryPlayer(a.c.e.b)
        t.invMainF = "a";         // o[] 主库存
        t.invArmorF = "b";        // o[]（candidate）
        t.invCurF = "c";          // int 槽位
        t.isCountF = "a";         // ItemStack a=Count（NBT 实证）
        t.isIdF = "c";
        t.isDmgF = "d";

        // 实体命名面（EntityList 双轨批 2026-11-14，javap 解包复核）：
        // j()List 返字段 p=loadedEntityList 三重实锤——构造器 new ArrayList 赋 p、
        // a(Entity)V spawn 处 p.add、tick 遍历 p 移除死实体、"All:"+p.size() 调试串。
        t.entityListM = "j";
        // EntityList 查表主面：a(Entity)String=Map b.get(getClass())，static{} 注册 15 件
        // （Arrow/Item/Painting/Mob/Monster/Creeper/Skeleton/Spider/Giant/Zombie/Pig/
        // Sheep/PrimedTnt/FallingSand/Minecart）。⚠️ 本版 a.c.e=EntityList（非 0327 EntityLiving）。
        t.entityListClassG = "net.minecraft.a.c.e";
        t.entityListLookupM = "a";
        // per-instance fallback 面**不钉**（el0618-1 活体翻案 2026-11-14）：javap 实锤
        // EntityLiving.o()=return 字段 Y，Y 构造器默认 ldc "/char.png"（皮肤路径）——
        // 活体玩家 kind=/char.png 铁证，与 0327 旧 q 贴图陷阱同族。0618 实体族无注册名
        // per-instance 面；正典命名面=EntityList 查表（游戏 NBT 存档 Entity.c(Tag) 同路）。
        // 查表 miss（玩家等未注册件）→类简名保底。

        // 档位一 null 槽（锚库 capabilities 16 件 unconfirmed 对榫，spec §3；
        // healthF 已于补钉批钉 Z=getplayer/sethealth 恢复，entitylist 带 [h=] 段）：
        // iframesF（invulnerable）、
        // addStackM/itemClassG/itemsF（give/setarmor/drop 注册表面——itemClass candidate
        // 不消费：addStackM null 连坐 give 面，宁缺毋滥）、
        // explodeM/spawnEntityM（explode/spawn/drop）、setEntityDeadM
        // （clear；entitylist 已于双轨批钉 j+o+查表）、playSoundM、setMetaSilentM/light6M/getLightM/observersF
        // （setmeta/getlight/grow 通知段）、difficultyF（getinfo）、scatterClassG（genscatter）、
        // invSizeM/invGetM/invSetM（getchest/setchest/getfurnace 在 resolveTe 响亮）、
        // teBurnF/teCookF（getfurnace）、itemEntityG（EntityItem 构造器未钉）、
        // idCrops/idSapling/cropsRipeMeta（grow）。mobClasses 空=spawn 版本面无。
        return t;
    }

    /**
     * 0629 表（第三版，G3 扁平散件族首例——0627/0629 形态分界实弹，pilot §5.1）。
     * 值源=本会话 javap 原始 jar 复核（解包 %TEMP%\mca0629-scan\x，SHA1 eb50bce3… 对榫
     * 主卷 §0）+ 详表 verscan/inf-20100629.md；**两处详表翻案**：
     * ① saveWorld 详表钉 a(Z)Z=错——方法体首串 "TickNextTick list out of synch"=world
     *   tick 本体；真链=public final a(Liw;)V 包装（iconst_1→invokespecial 私有
     *   a(boolean,iw)：level.dat_new/old 轮转+Saving chunks 循环）。档位二新形态：
     *   单引用型参（IProgressUpdate），WorldOps 按 desc 解析参数类名，调用传 null。
     * ② setPosRot 详表标"pos 双组归属需重钉"——NBT 双向对榫：Pos↔j/k/l（写点 d(ac)V
     *   三连 putfield，读点 e(ac)V getfield j 三复制 g/E）、Motion↔m/n/o、Rotation↔p/q
     *   （r/s=prev 复制位）；g/h/i=lastTickPos（mover 每 tick 从 j/k/l 复制）。
     *   setPosRot=b(DDDFF)V public final 与 0327 同形同字母（档位二零触发）。
     * 类名为根类（"ez"/"em"/"ht"…无包路径）——G3 扁平混淆形态，HookTargets 同组。
     */
    private static VersionTable build0629() {
        VersionTable t = new VersionTable();
        t.versionId = "inf-20100629";
        t.family = "G3"; // 扁平散件混淆（根类单/双字母）
        t.jarSha1 = "eb50bce3cb542488b3039aa0f4c3c0ec7595ab24"; // 主卷 §0 对榫表

        // World 写读面（ez；getBlockId a(III)I / setBlock c(IIII)Z 同 0327 形）
        t.setBlockM = "c";
        t.getBlockM = "a";
        t.getMetaM = "d";        // (III)I：边界检查→jr.c(III)I→hw nibble 访问器（meta 字节码级实锤）
        t.saveWorldM = "a";      // public final a(Liw;)V 包装（翻案①；el0629-2 活体：iw=具体类，
        // 本体 null 守卫齐全——ops 传 null 直调包装，Proxy 作废见 HookTargets 注释）
        t.saveWorldDesc = "(Liw;)V";
        t.worldTimeF = "b";      // long，NBT "Time" 写读配对
        t.randSeedF = "m";       // long，NBT "RandomSeed" 写读配对
        t.spawnXF = "i";         // NBT "SpawnX/Y/Z" 写读配对（i/j/k 与 0618 同字母——巧合非规则，逐版取证）
        t.spawnYF = "j";
        t.spawnZF = "k";

        // 玩家/实体定位面（Entity=em；⚠️ pos=j/k/l 非 0327 g/h/i 亦非 0618 h/i/j）
        t.playerF = "f";         // public em f（声明基类 em，运行时 gc）
        t.setPosRotM = "b";      // b(DDDFF)V public final 同 0327；双写 g..i+j..l（tp 主入口）
        t.setPosRotDesc = "(DDDFF)V"; // 档位二显式（与 0327 同形；resolve 分支判据）
        t.posXF = "j";
        t.posYF = "k";
        t.posZF = "l";
        t.motXF = "m";
        t.motYF = "n";
        t.motZF = "o";
        t.yawF = "p";            // Rotation↔p/q NBT 配对（r/s=prev 位）
        t.pitchF = "q";
        t.healthF = "ab";        // EntityLiving(jv) int ab：写点 "Health"←i2s、读点 containsKey→ab
        // 保底 10、构造器 bipush 10→ab 三重实锤（healthF=Z 模板复用）
        t.iframesF = null;       // 档位一：invulnerable 面未钉

        // 物品栏面（EntityPlayer=gc.b→ho；ItemStack=hr a/c/d NBT 钉；注册表 fz.b）
        t.invF = "b";
        t.invMainF = "a";
        t.invArmorF = "b";
        t.invCurF = "c";
        t.isCountF = "a";
        t.isIdF = "c";
        t.isDmgF = "d";
        t.addStackM = "a";       // ho.a(hr)Z
        t.itemClassG = "fz";     // 根类（[Lfz; 全 jar 唯一注册表锚）
        t.itemsF = "b";
        t.itemEntityG = "gt";    // EntityItem 构造器 (ez,DDD,hr)V 全 jar 扫描唯一命中

        // 实体/世界事件面（explode/addEntity/setEntityDead/playSound 全同 0327 形）
        t.explodeM = "a";        // a(Lem;DDDF)V public final
        t.spawnEntityM = "a";    // addEntity a(Lem)V（"Failed to add entity"+p.add 双实锤）
        t.setEntityDeadM = "b";  // public static b(em)V（0327 同 static 形）
        t.entityListM = "h";     // h()List 返字段 p：构造器 new ArrayList→p、addEntity p.add 两重实锤
        t.entityTypeM = null;    // per-instance 面 em.r()String=基类 aconst_null 死桩（0618 判决同族，不钉）
        t.entityListClassG = "ht"; // 扁平世代 EntityList=ht：static{} 注册串 "PrimedTnt" 全 jar 唯一命中
        t.entityListLookupM = "a"; // public static a(em)String=Map 查表（注册 11 件：Mob=jv Monster=gl
        // Giant=aa Zombie=gr Spider=cf Skeleton=fj Creeper=ft Pig=hx Sheep=dk PrimedTnt=cy FallingSand=ie；
        // 无 Item/Arrow/Painting/Minecart——比 0618 少 4 件，未注册件走类简名保底）
        t.playSoundM = "a";      // a(DDD,String,F,F)V

        // 档位一 null 槽（对齐 0618 欠账形态）：setMetaSilentM/light6M/getLightM/observersF/
        // difficultyF/scatterClassG（setmeta/getlight/genscatter）；invSizeM/invGetM/invSetM/
        // teBurnF/teCookF（chest/furnace 内部面——teGetM 已钉，resolveTe 可通）；
        // idCrops/idSapling/cropsRipeMeta（Infdev 无农业=grow 响亮 UNSUPPORTED）。
        t.teGetM = "b";          // (III)Lbf;（TE 基类=bf）

        // spawn 白名单（构造器 (ez)V 逐个 javap 实锤；tnt=(ez,FFF)V 同 0327 配方；
        // giant=aa 构造健在——0327 NEW_CTOR 复活面在 G3 无需要，HookTargets 0629 组不挂 GIANT_CTOR；
        // ⚠️ mob=gl(EntityMonster)：0629 无 EntityCreature 层（0327 mob=EntityCreature），
        // 语义=敌对基类——档位三语义漂移，活体冒烟观察渲染/行为后定案卷）
        t.mobClasses.put("pig", "hx");
        t.mobClasses.put("sheep", "dk");
        t.mobClasses.put("creeper", "ft");
        t.mobClasses.put("skeleton", "fj");
        t.mobClasses.put("zombie", "gr");
        t.mobClasses.put("spider", "cf");
        t.mobClasses.put("tnt", "cy");
        t.mobClasses.put("giant", "aa");
        t.mobClasses.put("mob", "gl");

        return t;
    }

    /**
     * a1.1.2_01 表（Alpha 1.1.2_01，G3-alpha 首例——扁平混淆但主类未混淆
     * net.minecraft.client.Minecraft 自带 main）。值源=本会话 javap 原始 jar 复核
     * （解包 %TEMP%\mcaut\a1121\，SHA1 daa4b9f1… 对索引 L317）+ 详表
     * docs/compatibility/verscan/a1.1.2_01.md（九面钉死+档位表 §9）。
     * 三档建模照 0629 惯例：档位二=saveWorld a(Z)**Z**（返回值差）、runTick public、
     * HUD 钩换形 a(FZII)V、spawnEntity 返 Z、setEntityDead **实例** d(kh)V；
     * 档位三=玩家面（World 无 dm 字段！playerListF=k + entityClassG=kh 回退，
     * WorldOps.worldPlayer 消费）；档位一=未钉面 null。
     * 字母漂移族规（详表 §10）：pos=ak..am、mot=an..ap、yaw/pitch=aq/ar、health=E、
     * iframes=aW、invCur=d、Item 注册表=c——inventory b + ItemStack a/c/d 全同形。
     * 冬天模式：World.d=snowCovered（level.dat 键集冒烟判据，a1.2.0 移除=与生物群系同版）。
     */
    private static VersionTable build1121() {
        VersionTable t = new VersionTable();
        t.versionId = "a1.1.2_01";
        t.family = "G3"; // 扁平散件+未混淆主类（G3 家族 alpha 变体）
        t.jarSha1 = "daa4b9f192d2c260837d3b98c39432324da28e86"; // 索引 L317 对榫（897164B）

        // World 写读面（cn；详表 §2，javap 访问位全 public 实锤）
        t.setBlockM = "d";        // d(IIII)Z 候选=setBlockWithNotify（⚠️a(IIII)Z=tiny 裸 setBlock——活体 diff 定夺）
        t.getBlockM = "a";        // a(III)I 同 0327 形
        t.getMetaM = "e";         // e(III)I getBlockData（tiny 对榫）
        t.saveWorldM = "a";       // a(Lnu)V public 包装（el1121-1 活体翻案，0629 同型：a(Z)Z 体首串
        // "TickNextTick list out of synch"=tick 本体非 saveWorld，g() 每 tick 直调=钩错则刷屏；
        // 真链=a(Lnu)V→a(true,nu)→a(ZLnu)V 本体（H 守卫+nu null 守卫+m() 写 level.dat+aw.a 存 chunk）；
        // changeWorld 两处调用传实参 q:Lgr，本体 null 守卫齐全→ops 传 null 语义正当）
        t.saveWorldDesc = "(Lnu;)V";
        t.worldTimeF = "c";       // public long c（NBT "Time" 写读配对）
        t.randSeedF = "u";        // public long u（NBT "RandomSeed"）
        t.spawnXF = "o";          // o/p/q=SpawnX/Y/Z（o=spawnX 候选——tiny 只钉 p/q）
        t.spawnYF = "p";
        t.spawnZF = "q";
        t.teGetM = "b";           // b(III)Lic;（TE 基类=ic）

        // 玩家/实体定位面（Entity=kh；档位三：World 无玩家直字段）
        t.playerF = null;         // javap 实锤 cn 无 dm 字段（详表 §7）
        t.playerListF = "k";      // public List k=playerEntities（worldPlayer get(0) 回退）
        t.entityClassG = "kh";
        t.setPosRotM = "b";       // b(DDDFF)V public 同 0327 字母同形（直写 ak..am+aq/ar=NBT 载入配方）
        t.setPosRotDesc = "(DDDFF)V";
        t.posXF = "ak";           // ⚠️非 0327 g/h/i、非 0618 h/i/j、非 0629 j/k/l——逐版取证铁律
        t.posYF = "al";
        t.posZF = "am";
        t.motXF = "an";
        t.motYF = "ao";
        t.motZF = "ap";
        t.yawF = "aq";
        t.pitchF = "ar";
        t.healthF = "E";          // EntityLiving(ge).E（NBT "Health" 双向+保底 10，同 0327 W/0618 Z/0629 ab 语义）
        t.iframesF = "aW";        // Entity.aW=heartsLife（0327 K 同语义，瞬态不进 NBT）

        // 物品栏面（EntityPlayer=dm.b→eu；ItemStack=ev a/c/d 全同形）
        t.invF = "b";             // dm.b=InventoryPlayer（0327/0618/0629 同字母！）
        t.invMainF = "a";         // ev[37]（⚠️readFromNBT 重建 ev[36]——give 循环按 arraylength 自适应无险）
        t.invArmorF = "b";        // ev[4]
        t.invCurF = "d";          // ⚠️非 0327 c
        t.isCountF = "a";
        t.isIdF = "c";
        t.isDmgF = "d";
        t.addStackM = "a";        // eu.a(ev)Z addItemStackToInventory（give 主入口）
        t.itemClassG = "di";
        t.itemsF = "c";           // ⚠️public static di[] c（0327=b 字母漂移）
        t.itemEntityG = "dx";     // EntityItem (cn,DDD,ev)V ✓同 0327 配方

        // 实体/世界事件面（详表 §2/§9；档位二两处=spawnEntity 返 Z、setEntityDead 实例）
        t.explodeM = "a";         // a(Lkh;DDDF)V 同 0327 形
        t.spawnEntityM = "a";     // a(Lkh;)Z（⚠️0327/0629 void——getMethod 返回值无关，档位二显式）
        t.setEntityDeadM = "d";   // d(Lkh;)V **实例** public（⚠️0327/0629 static——clear 收 world 调用，档位二）
        t.entityListM = "i";      // i()List getLoadedEntityList
        t.entityTypeM = null;     // per-instance 命名面未钉（0629 判决同族），主面=EntityList 查表
        t.entityListClassG = "ew";
        t.entityListLookupM = "b"; // public static b(kh)String（Map 查表；注册面详表 §4 含 Giant=hl 53）
        t.playSoundM = "a";       // a(DDD,String,F,F)V 同 0327 形

        // getinfo 面（difficultyF 已钉——0618/0629 欠账本版有）
        t.difficultyF = "l";      // public int l=difficultySetting

        // 档位一 null 槽（未钉面，op 入口 require 响亮）：setMetaSilentM 已钉 b(IIII)V ✓tiny，
        // 但 notify 段 light6M/observersF 未钉⇒setmeta/grow 仍 UNSUPPORTED（require 连坐）；
        // getLightM=j ✓tiny 已钉（getlight OK）；scatterClassG（genscatter）、
        // invSizeM/invGetM/invSetM/teBurnF/teCookF（chest/furnace 内部面）、
        // idCrops/idSapling/cropsRipeMeta（农业 id 本批未取证——宁缺毋滥）留 null。
        t.setMetaSilentM = "b";   // (IIII)V ✓tiny setBlockMetadataWithNotify 静默写
        t.getLightM = "j";        // (III)I ✓tiny getBrightness

        // spawn 白名单（mob 构造器 (cn)V 注册表在案=候选，无头逐个验；
        // tnt=jd(cn,FFF)V fuse 内置同 0327 配方；giant=hl 构造健在候选——0327 死桩复活面
        // 在 alpha 无需要（详表 §3：hl 仍在生物树）；mob=ek=EntityCreature（0327 同语义层，
        // 非 0629 EntityMonster 漂移）；cow=am/chicken=mz 在注册表但 op 白名单键沿用既有面）
        t.mobClasses.put("pig", "mv");
        t.mobClasses.put("sheep", "bo");
        t.mobClasses.put("creeper", "dd");
        t.mobClasses.put("skeleton", "cw");
        t.mobClasses.put("zombie", "mb");
        t.mobClasses.put("spider", "ax");
        t.mobClasses.put("tnt", "jd");
        t.mobClasses.put("giant", "hl");
        t.mobClasses.put("mob", "ek");

        return t;
    }

    /**
     * a1.2.0 表（G3-alpha 第二版，生物群系带接入批；verscan/a1.2.0_02.md 全量取证）。
     * 原生 tiny 直钉（不再借最近版本映射）——字母大漂移于 a1.1.2_01：
     * World cn→cu、Entity kh→lk、EntityLiving ge→gx（health **E→J**，NBT "Health"
     * 写读配对翻案 E 猜测）、EntityPlayer dm→dv（invF **b→e** 漂移⚠️）、
     * InventoryPlayer eu→fh、ItemStack ev→fi、Item di→dr、EntityList ew→fj、
     * TE ic→iz、NBT hm→ih、IProgressUpdate nu→ph、GuiIngame lu→mz。
     * 同形面（描述符骨架与字母全同）：getBlock a(III)I、setBlockWithNotify d(IIII)Z、
     * getMeta e(III)I、setMetaSilent b(IIII)V、getLight j(III)I、setPosRot b(DDDFF)V、
     * ItemStack a/c/d、inv a/b/d、addStack a、Item 注册表 c[]、explode/spawnEntity/
     * setEntityDead/playSound、theWorld=e、thePlayer=g、ingameGUI=u、runTick=i、
     * iframes=aW（notch=field_9311_be 跨版稳定）。
     * 冬天模式：snowCovered 字段移除实锤（§0 串面 diff+§2 javap 无字段）=null；
     * 本批键集冒烟判据（World5 Data 无 SnowCovered vs a1.1.2_01 World4 有=1）。
     * biome 面（本批核心增量）：cu.a()Lpb;→pb.a(II)Lfy;→fy.m:String 三锚全 public、
     * tiny 直钉+字节码体对榫（§8）——12 群系无数字 id（beta 序号表未生），读回=名串。
     * 档位一 null 槽：light6M/observersF（setmeta 连坐）、scatterClassG（genscatter）、
     * invSizeM/invGetM/invSetM/teBurnF/teCookF（chest/furnace 内部面）、
     * idCrops/idSapling/cropsRipeMeta（农业 id 未取证）——a1.1.2_01 同档。
     */
    private static VersionTable build120() {
        VersionTable t = new VersionTable();
        t.versionId = "a1.2.0";
        t.family = "G3"; // 扁平散件+未混淆主类（G3-alpha，a1.1.2_01 同族）
        t.jarSha1 = "8632ea716fd083c2975f16d612306fd80bee46db"; // 官方 manifest a1.2.0 分发件=内嵌 stamp v1.2.0_02（duplicate 陷阱，详表 §0；原始 2057 候选件签名级零 diff，本表两件通用）

        // World 写读面（cu；详表 §2，字母与 a1.1.2_01 全同、类字母漂移）
        t.setBlockM = "d";        // d(IIII)Z=setBlockWithNotify（tiny 直钉；裸 setBlock a(IIII)Z 留档）
        t.getBlockM = "a";        // a(III)I
        t.getMetaM = "e";         // e(III)I getBlockData
        t.saveWorldM = "a";       // a(Lph)V public 包装（javap -c：包装→iconst_1→a(ZLph)V 本体
        t.saveWorldDesc = "(Lph;)V"; //  "Saving level"→o()V+"Saving chunks"→bb.a；null 守卫齐全=0629 配方）
        t.worldTimeF = "e";       // public long e=worldTime
        t.randSeedF = "v";        // public long v=randomSeed
        t.spawnXF = "n";          // n/o/p=SpawnX/Y/Z（⚠️a1.1.2_01=o/p/q 整体左移一位）
        t.spawnYF = "o";
        t.spawnZF = "p";
        t.teGetM = "b";           // b(III)Liz;（TE 基类 ic→iz 漂移）

        // 玩家/实体定位面（Entity=lk；档位三同 a1.1.2_01：World 无玩家直字段）
        t.playerF = null;
        t.playerListF = "d";      // public List d=playerEntities（⚠️a1.1.2_01=k）
        t.entityClassG = "lk";
        t.setPosRotM = "b";       // b(DDDFF)V 同字母同形
        t.setPosRotDesc = "(DDDFF)V";
        t.posXF = "ap";           // ⚠️a1.1.2_01=ak..am 整体右移（NBT "Pos" 写读配对）
        t.posYF = "aq";
        t.posZF = "ar";
        t.motXF = "as";           // NBT "Motion" 配对
        t.motYF = "at";
        t.motZF = "au";
        t.yawF = "av";            // NBT "Rotation" 配对
        t.pitchF = "aw";
        t.healthF = "J";          // EntityLiving(gx).J（NBT "Health" 写读配对+保底 10——**E 猜测翻案**）
        t.iframesF = "aW";        // Entity.aW（notch=field_9311_be 与 a1.1.2_01 稳定，同字母同语义）

        // 物品栏面（EntityPlayer=dv.e→fh；⚠️invF b→e 字母漂移）
        t.invF = "e";
        t.invMainF = "a";         // fh.a=mainInventory
        t.invArmorF = "b";
        t.invCurF = "d";          // ✓同 a1.1.2_01
        t.isCountF = "a";         // fi a/c/d ✓全同形
        t.isIdF = "c";
        t.isDmgF = "d";
        t.addStackM = "a";        // fh.a(fi)Z addItemStackToInventory
        t.itemClassG = "dr";
        t.itemsF = "c";           // public static dr[] c ✓同字母
        t.itemEntityG = "ei";     // EntityItem (cu,DDD,fi)V ✓javap 实锤同配方

        // 实体/世界事件面（详表 §2/§3/§4）
        t.explodeM = "a";         // a(Llk;DDDF)V
        t.spawnEntityM = "a";     // a(Llk;)Z
        t.setEntityDeadM = "d";   // d(Llk;)V 实例 public
        t.entityListM = "k";      // k()List getLoadedEntityList（⚠️a1.1.2_01=i）
        t.entityTypeM = null;     // per-instance 命名面未钉（同族判决）
        t.entityListClassG = "fj";
        t.entityListLookupM = "b"; // b(lk)String ✓同字母（static{} 全表详表 §4：Giant=ig:53 存活）
        t.playSoundM = "a";       // a(DDD,String,F,F)V

        // getinfo 面
        t.difficultyF = "k";      // public int k=difficultySetting（⚠️a1.1.2_01=l）

        // 未钉面（档位一 null，op 入口 require 响亮）——a1.1.2_01 同档
        t.setMetaSilentM = "b";   // (IIII)V ✓tiny；notify 段 light6M/observersF 未钉⇒setmeta 仍 UNSUPPORTED
        t.getLightM = "j";        // (III)I ✓tiny getBrightness

        // spawn 白名单（fj 注册表+javap 构造器 (cu)V 逐个实锤；tnt=kg(cn,FFF)V fuse 内置；
        // giant=ig=EntityZombieSimple 存活（注册 53，"疑移除"翻案）；mob=ex=EntityCreature）
        t.mobClasses.put("pig", "oc");
        t.mobClasses.put("sheep", "bu");
        t.mobClasses.put("creeper", "dl");
        t.mobClasses.put("skeleton", "dd");
        t.mobClasses.put("zombie", "nh");
        t.mobClasses.put("spider", "bc");
        t.mobClasses.put("tnt", "kg");
        t.mobClasses.put("giant", "ig");
        t.mobClasses.put("mob", "ex");

        // biome 面（本批核心增量，详表 §8 三锚链全 public 直钉）
        t.biomeWcmM = "a";        // cu.a()Lpb; getWorldChunkManager（cu 唯一无参 a——getMethod 安全）
        t.biomeAtM = "a";         // pb.a(II)Lfy; func_4073_a（pb 的 a 重载中 (II) 唯一）
        t.biomeNameF = "m";       // fy.m=biomeName public String（子类 de/lz/i 继承 getField 可达）

        return t;
    }

    /**
     * b1.2_02 表（Beta 首例，G3-beta）。值源=锚库
     * java/agent/registry/fingerprints/b1.2_02.json；取证=
     * docs/compatibility/verscan/b1.2_02.md（借 retromcp b1.2 client.tiny 直钉——
     * 姊妹仓无 b1.2_02 tiny，15/15 关键类 classlist 同形核验+World 方法级对榫全过）。
     * 本批=字母断代批（详表 §9 对榫表）：World 方法字母大面积漂移——
     * setBlockWithNotify d→e（⚠️ d(IIII)Z 现为 setBlockMetadata=签名同形语义移位
     * 陷阱，严禁按签名猜名）、setMetaSilent b→c、getLight j→l、saveLevel o→r、
     * entityList k→l、randSeed v→u、spawn n/o/p→m/n/o、Entity pos ap..ar→aF..aH、
     * mot as..au→aI..aK、yaw/pitch av/aw→aL/aM、health J→S、inv e→f、invCur d→c、
     * setEntityDead I()V→F()V——描述符骨架全同形，五元组锚定风险大降。
     * 两态同名版（详表 §0）：纯独立构建件 551be326（主表）与官方 manifest LW 重打包
     * 件 093f371e（sidecar 实拉）=BY_SHA1 双件别名同表。
     * 档位一 null 槽：iframesF（⚠️ nl.aW=isDead 撞车，heartsLife 未钉——
     * invulnerable UNSUPPORTED，历代 aW 稳定惯例本版破裂）；light6M/observersF
     * （setmeta 连坐）、scatterClassG（genscatter）、invSizeM/invGetM/invSetM/
     * teBurnF/teCookF（chest/furnace 内部面）、idCrops/idSapling/cropsRipeMeta
     * （农业 id 未取证）——a1.2.0 同档。
     */
    private static VersionTable build1202() {
        VersionTable t = new VersionTable();
        t.versionId = "b1.2_02";
        t.family = "G3"; // 扁平散件+未混淆主类（G3-beta，a1.2.0 同族）
        t.jarSha1 = "551be32655e81a410d81e76259502dde83ebe525"; // 纯独立构建件（内嵌 stamp=Minecraft Beta 1.2_02；LW manifest 件 093f371e=BY_SHA1 别名，详表 §0）

        // World 写读面（dn；详表 §2，借 b1.2 tiny 直钉+javap 逐验）
        t.setBlockM = "e";        // e(IIII)Z=setBlockWithNotify 主写面（⚠️ d(IIII)Z=setBlockMetadata 陷阱；裸 setBlock=b(IIII)Z）
        t.getBlockM = "a";        // a(III)I ✓同字母
        t.getMetaM = "e";         // e(III)I getBlockData ✓
        t.saveWorldM = "a";       // a(Lru)V public 包装（IProgressUpdate ph→ru 断代；
        t.saveWorldDesc = "(Lru;)V"; // 本体 a(ZLru)V null 守卫齐全=0629/a1.2.0 配方；a(Z)Z=TickUpdates 禁钩）
        t.worldTimeF = "e";       // public long e=worldTime ✓同字母
        t.randSeedF = "u";        // randomSeed v→u（⚠️a1.2.0=v）
        t.spawnXF = "m";          // m/n/o=SpawnX/Y/Z（⚠️a1.2.0=n/o/p 整体左移一位）
        t.spawnYF = "n";
        t.spawnZF = "o";
        t.teGetM = "b";           // b(III)Lkp;（TE 基类 iz→kp 漂移）

        // 玩家/实体定位面（Entity=nl；档位三同族：World 无玩家直字段）
        t.playerF = null;
        t.playerListF = "d";      // playerEntities=d:List ✓同字母
        t.entityClassG = "nl";
        t.setPosRotM = "b";       // b(DDDFF)V ✓同字母同形
        t.setPosRotDesc = "(DDDFF)V";
        t.posXF = "aF";           // ⚠️a1.2.0=ap/aq/ar 整体断代（NBT "Pos" 写读配对）
        t.posYF = "aG";
        t.posZF = "aH";
        t.motXF = "aI";           // NBT "Motion" 配对
        t.motYF = "aJ";
        t.motZF = "aK";
        t.yawF = "aL";            // NBT "Rotation" 配对
        t.pitchF = "aM";
        t.healthF = "S";          // EntityLiving(ig).S（NBT "Health" 写读配对+保底 10；⚠️a1.2.0=J）
        t.iframesF = null;        // 档位一：nl.aW=isDead 撞车，heartsLife 未钉——invulnerable UNSUPPORTED

        // 物品栏面（EntityPlayer=eu.f；⚠️inv e→f、invCur d→c 双漂移）
        t.invF = "f";
        t.invMainF = "a";         // gl.a=mainInventory
        t.invArmorF = "b";
        t.invCurF = "c";          // ⚠️a1.2.0=d 左移一位
        t.isCountF = "a";         // gm a/c/d ✓全同字母（isDmg d=private——WorldOps setAccessible fallback）
        t.isIdF = "c";
        t.isDmgF = "d";
        t.addStackM = "a";        // gl.a(gm)Z addItemStackToInventory ✓
        t.itemClassG = "eo";
        t.itemsF = "c";           // public static eo[] c=itemsList ✓同字母
        t.itemEntityG = "fh";     // EntityItem (dn,DDD,gm)V ✓（gm 无 (II)V 构造——newStack 走 (III)V）

        // 实体/世界事件面（详表 §2/§4）
        t.explodeM = "a";         // a(Lnl;DDDF)→Explosion（c11b777 翻案 V→Lmc;，返回值不消费）✓tiny
        t.spawnEntityM = "a";     // a(Lnl;)Z entityJoinedWorld ✓
        t.setEntityDeadM = "d";   // d(Lnl;)V 实例 ✓
        t.entityListM = "l";      // l()List getLoadedEntityList（⚠️a1.2.0=k）
        t.entityTypeM = null;     // per-instance 命名面未钉（同族判决）
        t.entityListClassG = "gn";
        t.entityListLookupM = "b"; // b(nl)String ✓同字母（static{} 23 项详表 §4：Giant=jv:53 存活；Cow92/Chicken93/Squid94=a1.2.0 三动物 id=91 怪癖修复）
        t.playSoundM = "a";       // a(DDD,String,F,F)V ✓tiny playSoundEffect

        // getinfo 面
        t.difficultyF = "k";      // difficultySetting=k:I ✓同字母

        // meta/光面（详表 §2；notify 段 light6M/observersF 未钉⇒setmeta UNSUPPORTED 连坐）
        t.setMetaSilentM = "c";   // c(IIII)V setBlockMetadataWithNotify（⚠️a1.2.0=b）
        t.getLightM = "l";        // l(III)I getBrightness（⚠️a1.2.0=j）

        // spawn 白名单（gn 注册表+javap 构造器 (dn)V 逐个实锤；tnt=mb
        // ⚠️(dn,DDD)V 历代 FFF 断代——WorldOps fallback；giant=jv=EntityZombieSimple
        // 存活（注册 53）；mob=fz=EntityCreature）
        t.mobClasses.put("pig", "qk");
        t.mobClasses.put("sheep", "ch");
        t.mobClasses.put("creeper", "eh");
        t.mobClasses.put("skeleton", "dy");
        t.mobClasses.put("zombie", "pn");
        t.mobClasses.put("spider", "bn");
        t.mobClasses.put("tnt", "mb");
        t.mobClasses.put("giant", "jv");
        t.mobClasses.put("mob", "fz");

        // biome 面（详表 §8 三锚链与 a1.2.0 全同形——读回链冒烟判据复用）
        t.biomeWcmM = "a";        // dn.a()Lro; getWorldChunkManager（dn 唯一无参 a ✓）
        t.biomeAtM = "a";         // ro.a(II)Lhf; func_4073_a ✓（ro 的 a 重载中 (II) 唯一）
        t.biomeNameF = "m";       // hf.m=biomeName public String ✓（12 系 a..l 旧表保留）

        return t;
    }

    /**
     * b1.7.3 表（Beta 1.7.3，McRegion 世代首例——存储断代批）。值源=锚库
     * java/agent/registry/fingerprints/b1.7.3.json；取证=
     * docs/compatibility/verscan/b1.7.3.md（借 retromcp b1.7 client.tiny 直钉+
     * 原始 jar javap 全量逐验，件 SHA1 43db9b49… 纯独立件 stamp 自证）。
     * 本批三大结构断代（详表 §2/§9/§10）：
     * ① World 不再持 time/seed/spawn 字段——WorldInfo=ei 承载（fd.x()Lei 取用器，
     *    ei 字段面全 private javap 实锤：a=randomSeed/e=worldTime/b-d=spawnXYZ；
     *    WorldOps resolve WorldInfo 分支消费，getDeclaredField+setAccessible）；
     * ② 存储=McRegion .mcr（region/r.<x>>5>.<z>>5>.mcr，下界 DIM-1/region）——
     *    数据侧 RegionStore 只读 adapter（java/storage），活体散件时代判据作废；
     * ③ 下界回归（传送门面+维度构造+DIM-1 目录），天气键持久化复活（ei l-o）。
     * 字母断代陷阱预避（详表 §9 对榫表）：runTick i→**k**、spawnEntity a→**b**、
     * setEntityDead d→**e**、entityList l→**o**、setBlockWithNotify e→**f**、
     * getMeta **e 不变**（el173-1 活体翻案：g(III)Z=notifyBlockChange 非元数据读，
     * e(III)I=bounds→c(II)→lm.b(III)I nibble 读，与 b1.2_02 同字母）、setMetaSilent c→**d**、getLight l→**n**、biomeName m→**n**；
     * iframesF=**by** 复活（b1.2_02 档位一欠账清偿——sn.by=heartsLife public 实锤，
     * 非 aW=isDead 撞车位）。
     * 档位一 null 槽：light6M/observersF（setmeta 连坐）、scatterClassG（genscatter）、
     * invSizeM/invGetM/invSetM/teBurnF/teCookF（chest/furnace 内部面）、
     * idCrops/idSapling/cropsRipeMeta（农业 id 未取证）——b1.2_02 同档。
     */
    private static VersionTable build173() {
        VersionTable t = new VersionTable();
        t.versionId = "b1.7.3";
        t.family = "G3"; // 扁平散件+未混淆主类（G3-beta，b1.2_02 同族）
        t.jarSha1 = "43db9b498cb67058d2e12d394e6507722e71bb45"; // 纯独立件（内嵌 stamp=Minecraft Beta 1.7.3，详表 §0）

        // World 写读面（fd；详表 §2 javap 全量实锤）
        t.setBlockM = "f";        // f(IIII)Z=setBlockWithNotify（⚠️ 裸 setBlock=c(IIII)Z、setBlockAndMetadata=a(IIIII)Z 同形陷阱群）
        t.getBlockM = "a";        // a(III)I ✓历代同字母
        t.getMetaM = "e";         // e(III)I getBlockMetadata（el173-1 活体翻案：bounds→c(II)Llm;→lm.b(III)I nibble 读；g(III)Z 是别物非元数据，详表 §2 原判作废）
        t.setMetaSilentM = "d";   // d(IIII)V setBlockMetadataWithNotify（⚠️b1.2_02=c）
        t.getLightM = "n";        // n(III)I getBrightness（⚠️b1.2_02=l）
        t.saveWorldM = "a";       // a(Lyb;)V public 包装（IProgressUpdate ru→yb 断代；
        t.saveWorldDesc = "(Lyb;)V"; // 本体 a(ZLyb)V null 守卫=0629/a1.2.0/b1.2_02 配方四连用；a(Z)Z=TickUpdates 禁钩）
        t.teGetM = "b";           // b(III)Low;（TE 基类 kp→ow 漂移）
        // WorldInfo 断代五槽（详表 §2 结构断代①）：字段在 ei 非 fd——
        // getWorldInfoM 非 null 时 WorldOps 在 x() 返回类型上 getDeclaredField+setAccessible
        t.getWorldInfoM = "x";    // fd.x()Lei public 零参（getWorldInfo）
        t.worldTimeF = "e";       // ei.e:J=worldTime（NBT "Time" 构造器读写配对）
        t.randSeedF = "a";        // ei.a:J=randomSeed（NBT "RandomSeed"）
        t.spawnXF = "b";          // ei.b/c/d:I=SpawnX/Y/Z（NBT 键构造器配对）
        t.spawnYF = "c";
        t.spawnZF = "d";

        // 玩家/实体定位面（Entity=sn；档位三同族：World 无玩家直字段）
        t.playerF = null;
        t.playerListF = "d";      // fd public List d=playerEntities（⚠️ 五 List 字段 b/c/d/e 中 javap 调用点定 d）
        t.entityClassG = "sn";
        t.setPosRotM = "b";       // b(DDDFF)V ✓历代同字母同形
        t.setPosRotDesc = "(DDDFF)V";
        t.posXF = "aM";           // ⚠️b1.2_02=aF..aH 整体断代（NBT "Pos" 写读配对）
        t.posYF = "aN";
        t.posZF = "aO";
        t.motXF = "aP";           // NBT "Motion" 配对
        t.motYF = "aQ";
        t.motZF = "aR";
        t.yawF = "aS";            // NBT "Rotation" 配对
        t.pitchF = "aT";
        t.healthF = "Y";          // EntityLiving(ls).Y public int（NBT "Health" 写读配对+保底 10；⚠️b1.2_02=S）
        t.iframesF = "by";        // sn.by public int=heartsLife（b1.2_02 档位一欠账清偿——invulnerable 复活；⚠️ be=isDead 撞车位勿混）

        // HUD 次级链（本批清账：主钩 uq.a(FZII)V 注入成功但链字母未钉→优雅降级。
        // javap 实锤 MC thePlayer=h/fontRenderer=q(Lsj)/gameSettings=z(Lkv)；
        // 门=kv.B=showDebugInfo（uq 字节码两布尔读点：A@117=pumpkin 门 thirdPersonView、
        // B@1142=F3 块 glTranslate 32F+版本串，姊妹仓 b1.7.3 树 L169 对榫；P 键独立调试列
        // b1.5 起并入 F3——0327 showFPS 语义的 Beta 后继））
        t.hudPlayerF = "h";
        t.hudFontF = "q";
        t.hudSettingsF = "z";
        t.hudDebugGateF = "B";
        t.hudXyzPaint = Boolean.FALSE; // a1.2.3 边界线（§5.1）：F3 自带坐标=注入免做，链字母留作锚数据

        // 物品栏面（EntityPlayer=gs.c→ix；⚠️ invF f→c 回漂、invCur c ✓同 b1.2_02）
        t.invF = "c";
        t.invMainF = "a";         // ix.a=mainInventory[iz]
        t.invArmorF = "b";
        t.invCurF = "c";          // ⚠️ ix.d=player/ix.e=inventoryChanged 互换陷阱——c 才是 currentItem
        t.isCountF = "a";         // iz a=stackSize/c=itemID/d=itemDamage（NBT 配对）
        t.isIdF = "c";
        t.isDmgF = "d";
        t.addStackM = "a";        // ix.a(iz)Z addItemStackToInventory ✓
        t.itemClassG = "gm";
        t.itemsF = "c";           // public static gm[] c ✓同字母
        t.itemEntityG = "hl";     // EntityItem (fd,DDD,iz)V ✓（iz 无 (II)V 构造——newStack 走 (III)V）

        // 实体/世界事件面（详表 §2/§4；⚠️ spawnEntity a→b、setEntityDead d→e、entityList l→o 断代）
        t.explodeM = "a";         // a(Lsn;DDDF)Lqx;（返回值 V→Explosion 断代留档——getMethod 返回值无关，invoke 面不消费返回值）
        t.spawnEntityM = "b";     // b(Lsn;)Z entityJoinedWorld（⚠️历代 a）
        t.setEntityDeadM = "e";   // e(Lsn;)V（⚠️历代 d）
        t.entityListM = "o";      // o()List getLoadedEntityList（⚠️b1.2_02=l）
        t.entityTypeM = null;     // per-instance 命名面未钉（同族判决）
        t.entityListClassG = "jc";
        t.entityListLookupM = "b"; // b(sn)String ✓同字母（static{} 26 项详表 §4：Giant=nt:53 存活、XPOrb 未生）
        t.playSoundM = "a";       // a(DDD,String,F,F)V ✓

        // getinfo 面
        t.difficultyF = "q";      // fd public int q=difficultySetting（⚠️b1.2_02=k）

        // D8 门控首发（案卷 §6.1：weather=b1.5+，b1.7.3 verscan 取证在档——
        // World rainingStrength=j:F/thunderingStrength=l:F 浮值面；WorldInfo
        // 布尔键由游戏存盘侧写，op 只动运行时浮值）
        t.capWeather = Boolean.TRUE;
        t.rainStrF = "j";         // verscan b1.7.3 §0-§11 浮值面
        t.thunderStrF = "l";

        // spawn 白名单（jc 注册表+javap 构造器 (fd)V 逐个实锤；tnt=qw
        // (fd,DDD)V DDD 断代延续 b1.2_02——WorldOps fallback；giant=nt 构造健在；
        // mob=ii=EntityCreature（⚠️b1.2_02=fz））
        t.mobClasses.put("pig", "wh");
        t.mobClasses.put("sheep", "dl");
        t.mobClasses.put("creeper", "gb");
        t.mobClasses.put("skeleton", "fr");
        t.mobClasses.put("zombie", "uz");
        t.mobClasses.put("spider", "cn");
        t.mobClasses.put("tnt", "qw");
        t.mobClasses.put("giant", "nt");
        t.mobClasses.put("mob", "ii");

        // biome 面（详表 §8；⚠️ biomeName m→n 断代——kd 16 系新表含 hell 复活）
        t.biomeWcmM = "a";        // fd.a()Lxv; getWorldChunkManager（fd 唯一无参 a ✓）
        t.biomeAtM = "a";         // xv.a(II)Lkd; getBiomeGenAt ✓
        t.biomeNameF = "n";       // kd.n=biomeName public String（⚠️b1.2_02=m 字母漂移）

        return t;
    }

    /**
     * b1.8.1 表（Beta 1.8.1，9 代表版线线尾件——旁证直转批）。值源=锚库
     * java/agent/registry/fingerprints/b1.8.1.json；取证=
     * docs/compatibility/verscan/b1.8.1.md（借 retromcp b1.8 原生 tiny+实件逐锚
     * 复核；件 SHA1 6b562463… 对榫主卷 §0，stamp 自证）。
     * **锚面零漂移判决**（详表 §1）：117 类 javap dump 逐类 hash 对照 b1.8=
     * 116/117 字节级全同，唯一漂移 cf(Container) a(Lul;IIZ) 返回值 V→boolean
     * （b1.8.1 热修复点，不在 ops 消费链）——b1.8 旁证批（verscan/b1.8.md）表
     * 整体平移成立。
     * vs b1.7.3 类身份断代（详表 §2）：World fd→rv、Entity sn→kj、EntityLiving
     * ls→wd、EntityPlayer gs→sz、InventoryPlayer ix→ui、ItemStack iz→ul、Item
     * gm→sv、EntityList jc→um、GuiIngame uq→abj、WorldInfo ei→rl、IProgressUpdate
     * yb→ny、BiomeGenBase kd→vh。⚠️ 跨版本撞车位：sn=EntityCreeper（≠b1.7.3
     * Entity）、jc=RegionFile（≠b1.7.3 EntityList）、wd=EntityLiving（≠b1.7.3
     * WorldProviderHell）——严禁互抄表。
     * ops 漂移项：setBlockM f→g、setMetaSilentM d→f、playerListF d→i、pos
     * aM-aO→o/p/q、mot→r/s/t、look→u/v、healthF Y→bz、iframesF by→aa、invF
     * c→as、itemClassG gm→sv、itemsF c→f、itemEntityG hl→ee、spawnEntityM
     * b→a（回漂）、setEntityDeadM e→b（回漂）、entityListM o→p、difficultyF
     * q→v、biomeNameF n→l。同字母保留：getBlockM/getMetaM/getLightM/teGetM/
     * saveWorldM/getWorldInfoM/setPosRotM/addStackM/explodeM/playSoundM/
     * entityListLookupM/biomeWcmM/biomeAtM/WorldInfo 字段面全同（rl=ei 复刻）。
     * 存储=.mcr 世代（RegionStore 直接适用，el173-1 真盘判决语义）；
     * b1.8.1 新面=饥饿 kc=EntityFood（NBT 四键）+GameType int 键（rl，无枚举类）。
     */
    private static VersionTable build181() {
        VersionTable t = new VersionTable();
        t.versionId = "b1.8.1";
        t.family = "G3"; // 扁平散件+未混淆主类（G3-beta，b1.7.3 同族）
        t.jarSha1 = "6b562463ccc2c7ff12ff350a2b04a67b3adcd37b"; // 纯独立件（stamp=Minecraft Beta 1.8.1，详表 §0）

        // World 写读面（rv；详表 §1 实件终核）
        t.setBlockM = "g";        // g(IIII)Z=setBlockWithNotify（⚠️b1.7.3=f 漂移；裸 setBlock=d(IIII)Z、setBlockMetadata=e(IIII)Z 同形陷阱群）
        t.getBlockM = "a";        // a(III)I ✓历代同字母
        t.getMetaM = "e";         // e(III)I getBlockMetadata ✓同 b1.7.3（el173-1 翻案值沿用）
        t.setMetaSilentM = "f";   // f(IIII)V setBlockMetadataWithNotify（⚠️b1.7.3=d）
        t.getLightM = "n";        // n(III)I ✓同字母
        t.saveWorldM = "a";       // a(Lny;)V public 包装（IProgressUpdate yb→ny 断代；
        t.saveWorldDesc = "(Lny;)V"; // 本体 a(ZLny)V；a(Z)Z=TickUpdates 禁钩）
        t.teGetM = "b";           // b(III)Lij;（TileEntity ow→ij 漂移，字母 b ✓）
        // WorldInfo 面（rl=ei 字段表全同字母复刻——b1.8.1 批判决，详表 §2）
        t.getWorldInfoM = "x";    // rv.x()Lrl; getWorldInfo ✓同字母
        t.worldTimeF = "e";       // rl.e:J=worldTime ✓
        t.randSeedF = "a";        // rl.a:J=randomSeed ✓
        t.spawnXF = "b";          // rl.b/c/d:I=SpawnX/Y/Z ✓
        t.spawnYF = "c";
        t.spawnZF = "d";

        // 玩家/实体定位面（Entity=kj；档位三同族）
        t.playerF = null;
        t.playerListF = "i";      // rv public List i=playerEntities（⚠️b1.7.3=d）
        t.entityClassG = "kj";
        t.setPosRotM = "b";       // b(DDDFF)V ✓历代同字母同形
        t.setPosRotDesc = "(DDDFF)V";
        t.posXF = "o";            // kj.o/p/q=posX/Y/Z（⚠️b1.7.3=aM-aO 全面重写；NBT "Pos" 配对）
        t.posYF = "p";
        t.posZF = "q";
        t.motXF = "r";            // NBT "Motion" 配对
        t.motYF = "s";
        t.motZF = "t";
        t.yawF = "u";             // NBT "Rotation" 配对
        t.pitchF = "v";
        t.healthF = "bz";         // wd.bz public int=health（⚠️b1.7.3=Y；NBT "Health" 配对）
        t.iframesF = "aa";        // kj.aa public int=heartsLife（⚠️b1.7.3=by；invulnerable 沿用）

        // HUD 次级链（本批清账：MC dump 与 b1.8 字节级全同——h/q/z 三字段保留；
        // 门=fv.E=showDebugInfo（abj 字节码两布尔读点：D@117=thirdPersonView、
        // E@1656=F3 块，姊妹仓 b1.8.1 树 L259 对榫；⚠️173=B——kv→fv 类断代带字母漂移））
        t.hudPlayerF = "h";
        t.hudFontF = "q";
        t.hudSettingsF = "z";
        t.hudDebugGateF = "E";
        t.hudXyzPaint = Boolean.FALSE; // a1.2.3 边界线（§5.1）：F3 自带坐标=注入免做，链字母留作锚数据

        // 物品栏面（EntityPlayer=sz.as→ui；⚠️ invF as 断代）
        t.invF = "as";
        t.invMainF = "a";         // ui.a=mainInventory[ul] ✓
        t.invArmorF = "b";
        t.invCurF = "c";          // ⚠️ ui.d=player/e=inventoryChanged 互换陷阱延续——c 才是 currentItem
        t.isCountF = "a";         // ul a=stackSize/c=itemID/d=itemDamage ✓全同字母
        t.isIdF = "c";
        t.isDmgF = "d";
        t.addStackM = "a";        // ui.a(ul)Z addItemStackToInventory ✓
        t.itemClassG = "sv";
        t.itemsF = "f";           // public static sv[] f（⚠️b1.7.3=gm.c）
        t.itemEntityG = "ee";     // EntityItem ee（hl→ee 断代）

        // 实体/世界事件面（详表 §2；⚠️ spawnEntity b→a、setEntityDead e→b 双双回漂）
        t.explodeM = "a";         // a(Lkj;DDDF)Lzf;（V→Explosion 断代延续，返回值不消费）
        t.spawnEntityM = "a";     // a(Lkj;)Z entityJoinedWorld（⚠️b1.7.3=b 回漂）
        t.setEntityDeadM = "b";   // b(Lkj;)V（⚠️b1.7.3=e 回漂）
        t.entityListM = "p";      // p()List getLoadedEntityList（⚠️b1.7.3=o）
        t.entityTypeM = null;     // per-instance 命名面未钉（同族判决）
        t.entityListClassG = "um";
        t.entityListLookupM = "b"; // b(kj)String ✓同字母（um static 面详表 §5；注册表 26 项活体批复核）
        t.playSoundM = "a";       // a(DDD,String,F,F)V ✓

        // getinfo 面
        t.difficultyF = "v";      // rv public int v=difficultySetting（⚠️b1.7.3=q）

        // spawn 白名单（mobClasses 全表断代，详表 §2；giant=hv 构造健在预期——活体复验；
        // creeper=sn ⚠️ 撞车位=字母与 b1.7.3 Entity 相同，跨版本勿混）
        // ⚠️ mob 槽摘除（el181-1 活体判决）：tz=EntityCreature **abstract**（javap 实锤，
        // b1.8-pre1-2 起断代——b1.7.3 ii=concrete 可实例化=173 活体 mob OK 不矛盾）；
        // 中间层 tb=EntityMob/aq=EntityAnimal/pe=EntityWaterMob 全 abstract（tiny 对榫），
        // b1.8+ 线无泛用可实例化 mob 基类=能力断代非表错。旁证批 b1.8 表同修。
        t.mobClasses.put("pig", "mx");
        t.mobClasses.put("sheep", "bx");
        t.mobClasses.put("creeper", "sn");
        t.mobClasses.put("skeleton", "se");
        t.mobClasses.put("zombie", "abq");
        t.mobClasses.put("spider", "bh");
        t.mobClasses.put("tnt", "ze");
        t.mobClasses.put("giant", "hv");

        // biome 面（⚠️ BiomeGenBase kd→vh——b1.8.md 前记 kd 错已修=ModelSpider）
        t.biomeWcmM = "a";        // rv.a()Lnu; getWorldChunkManager（rv 唯一无参 a 返回 nu ✓）
        t.biomeAtM = "a";         // nu.a(II)Lvh; getBiomeGenAt ✓
        t.biomeNameF = "l";       // vh.l=biomeName public String（⚠️b1.7.3=kd.n）

        // D10 XP 面（b1.8.1 首版实装——setxp 消费；取证=src-minecraft-beta-1.8.1 树
        // EntityPlayer.java 直读，字段全 public int，L464-487 NBT 三键写读配对实锤：
        // aX="Xp"/aY="XpLevel"/aZ="XpTotal"/aG="XpCooldown"（L21 默认 0，L124 tick 递减守卫）、
        // ay=score。bb(field_35213_bb F) 系拾取动画系数非进度权威（全树唯一赋值=初始化
        // 0.02F，L315-318 仅读作 bw 平滑增量）——XP 条显示直读 aX/func_35193_as()，不写 bb。
        // addXpM null 判决：b1.8.1 无独立 addExperience 方法，加经验循环内联 L1050
        // （aX+=i1;aZ+=i1;while aX>=as(){aX-=as();ab()}）——setxp 走直写三字段面，
        // 不依赖该方法。increaseLevelM=ab（func_35206_ab L1065 ++aY）留档不入消费链。
        // ⚠️ 与 b1.9-pre2 锚件字母整体不同（19pre2 xpF=aW/xpLevelF=aY/playerLevelF=aX/
        // xpCooldownF=aF/scoreF=ax），互抄即错。
        t.capXp = Boolean.TRUE;
        t.xpF = "aX";             // NBT "Xp"（当前级内进度经验）
        t.xpLevelF = "aY";        // NBT "XpLevel"（等级）
        t.playerLevelF = "aZ";    // NBT "XpTotal"（累计总经验，键名沿用 MCP 混淆位）
        t.scoreF = "ay";          // NBT "Score"
        t.xpCooldownF = "aG";     // NBT "XpCooldown"（写 0=立即可再拾）
        t.addXpM = null;          // b1.8.1 无独立方法（内联 L1050，见上注）
        t.increaseLevelM = "ab";  // func_35206_ab()V（留档，setxp 直写字段不消费）

        // D10 效果面（b1.8.1 首版实装——giveeffect/removeeffect 消费；取证=
        // src-minecraft-beta-1.8.1 树 EntityLiving.java 直读：func_35165_a(Lade;)V
        // =addPotionEffect（L1007-1016 合并/新增双分支，bR.put+回调 func_35164_b）、
        // damageEntity(I)V=MCP 误名实为 removePotionEffect（L1019 bR.remove(i1)，
        // javap 字母 f）、func_35160_a(Lud;)Z=isPotionActive（L1000 bR.containsKey）、
        // func_35167_b(Lud;)Lade;=getPotionEffect（L1004 bR.get）、
        // field_35170_bR:HashMap=activePotionsMap（L51 protected，键=Potion.field_35670_H
        // 即效果 id）。ade 构造 (III)V（PotionEffect.java L8 field_35805_a=id）对榫
        // 19pre2 形态。⚠️ clearEffectsM null 判决：b1.8.1 全树无 bR.clear()/
        // clearActivePotions 面（dropFewItems L986 逐键 iterator.remove 非清空）——
        // removeeffect 逐 id 移除已足，clear 面待 19pre2 转正再启用。
        // isActive/getActive 参类=Potion(ud) 非 int id（与 19pre2 同形态，留档不消费）。
        t.capEffect = Boolean.TRUE;
        t.addEffectM = "a";       // wd.a(Lade;)V（⚠️ wd 段 a 重载群，getMethod 按 ade 参型消歧）
        t.addEffectDesc = "(Lade;)V";
        t.removeEffectM = "f";    // wd.f(I)V（damageEntity 误名，L1019 实锤）
        t.clearEffectsM = null;   // b1.8.1 无清空面（见上注）
        t.isActiveEffectM = "a";  // wd.a(Lud;)Z（留档）
        t.getActiveEffectM = "b"; // wd.b(Lud;)Lade;（留档）
        t.effectsMapF = "bR";     // wd.bR protected HashMap（留档，give/remove 走方法面）
        t.potionClassG = "ud";    // Potion（isActive/get 参类，留档）
        t.potionEffectClassG = "ade"; // PotionEffect 类名
        t.potionEffectCtorDesc = "(III)V"; // (id,durationTicks,amplifier)
        t.xpOrbClassG = null;     // EntityXPOrb 未钉（spawn 面后续批，本批不消费）

        // D11 饥饿面（b1.8.1 首版实装——sethunger 消费；取证=src-minecraft-beta-1.8.1 树
        // FoodStats.java 直读，四字段全 private（L4-7），NBT 四键写读配对实锤（L53-66）：
        // a=foodLevel(int 0..20)/b=saturation(float)/c=exhaustion(float)/d=tickTimer(int)。
        // 取用器 EntityPlayer.func_35191_at()Lkc;（public 字母 at，L1077）——走访问器
        // 返回 FoodStats 实例，再 getDeclaredField+setAccessible 写 private 字段（teBurnF 同配方）。
        // 语义：food∈[0,20]（原版饱食度上限）、saturation∈[0,food]（游戏钳 saturation≤foodLevel，
        // L12 Math.min）——本 op 直写不重算钳制（写后游戏 tick 自愈），saturation 越界仅 Rejected 负值。
        t.capHunger = Boolean.TRUE;
        t.foodStatsM = "at";      // sz.at()Lkc; getFoodStats
        t.foodStatsClassG = "kc"; // FoodStats 类名
        t.foodLevelF = "a";       // kc.a=foodLevel private int
        t.foodSatF = "b";         // kc.b=saturation private float
        t.foodExhaustF = "c";     // kc.c=exhaustion private float（留档）
        t.foodTimerF = "d";       // kc.d=tickTimer private int（留档）

        // D12 weather 缺口闭合（b1.8.1 浮值面——capWeather 此前仅在 build173 设，b1.8.1
        // 漏设致 weather op 在 181 响亮 UNSUPPORTED，属不一致 bug）。取证=tiny 映射实锤：
        // rv.rainingStrength=o(F protected) / rv.thunderingStrength=q(F protected)
        // （prevRaining=n/prevThundering=p 非写面）。⚠️ 与 b1.7.3 j/l 字母不同（世代漂移），
        // 且 WorldOps resolve 已改 getDeclaredField+setAccessible 兼容 protected。
        // 语义同 D8 判决：写浮值=临时态，updateWeather L1804 每 tick 由 WorldInfo.isRaining
        // 覆写（斜坡陷阱在档）——完整态需写 WorldInfo 旗标=到场包活体判决，本 op 维持浮值面。
        t.capWeather = Boolean.TRUE;
        t.rainStrF = "o";         // rv.o=rainingStrength protected float
        t.thunderStrF = "q";      // rv.q=thunderingStrength protected float

        // D13 gamemode（b1.8.1 首版实装——setgamemode 消费，完成 capGamemode 槽先行承诺）。
        // 取证=tiny 实锤 WorldInfo=rl，field_35920_p=gameType private int（NBT "GameType"
        // L26/L104 读写配对）。写面=WorldInfo 持久层（重进世界生效），经 getWorldInfoM=x 取
        // 实例 getDeclaredField+setAccessible 写 p。mode∈[0,1]（b1.8 仅 survival/creative，
        // adventure=2 系 b1.9-pre 后，越界 Rejected）。⚠️ 会话内即时行为联动=到场包活体判决。
        t.capGamemode = Boolean.TRUE;
        t.gameTypeF = "p";        // rl.p=gameType private int

        return t;
    }

    /**
     * b1.9-pre2 表（Beta 1.9 pre-release 2，G3-beta 扁平散件，9 代表版线外独立件——
     * 活体测试转正批 2026-11-25）。锚库 java/agent/registry/fingerprints/b1.9-pre2.json
     * （逐字段对榫=A2Table19pre2 验收）；取证=%TEMP%\mcaut\exp\b19pre2\（jar SHA1
     * b0d40cf4… 对榫索引行，javap 全量 10240 行+client.tiny 直钉）。
     * ⚠️ pre2 断代陷阱（禁借 173/181 表，跨版本字母严禁互抄）：
     *   ① EntityCreature=vp **abstract**（dump L10296）→spawn kind=mob UNSUPPORTED
     *     （181 el181-1 同判决延续）；
     *   ② Entity=lh **abstract**+EntityPlayer=uo **abstract**（dump L9905，tiny L9416）
     *     ——活体玩家真身=sd EntityPlayerSP（concrete，dump L8619）；181 sz 具体可直造
     *     的形态在本版破裂，假件布景走 sd；
     *   ③ healthF=a **private**（dump L11444，tiny L11013=health）——181 bz public 断代，
     *     WorldOps.healthField 本批扩 getDeclaredField 父链回退（b1.2_02 isDmgF 同配方）；
     *   ④ setPosRotM=c=setLocationAndAngles（tiny L5037）——181 主写=b setPositionAndRotation
     *     断代翻面；b(DDDFF)V 同形 decoy 在位（tiny L4991）；
     *   ⑤ setMetaSilentM=c(IIII)Z=setBlockMetadata 裸静默写（tiny L8293）——181 走
     *     f(IIII)V WithNotify 断代翻面；f/d/e(IIII) decoy 群在位；
     *   ⑥ isDmgF d→e（wc.e **private**，dump L10553——resolveInvFace 回退消费）；
     *     itemsF c→e（uk public static uk[] e，dump L9684）；
     *   ⑦ MobEffectList 名=Potion(vt)（1.0+ 才改名——挖掘侧命名陷阱）；
     *   ⑧ xp 双 level 字段并存怪癖：aW=xp/aY=xpLevel/aX=playerLevel（XpTotal 载体，
     *     tiny L9460/9497/9519）——1.0+ 合并；与 181 aX/aY/aZ/aG/ay 整体不同（表注钉死）；
     *   ⑨ heartsLife=Z 回归（tiny L5057 public int Z——1301 移除→pre2 与 hurtTime=bz 并存）；
     *     pos 面右移 o/p/q（173=aM..aT/1501=aK..aR/1301=aI..aP 每版+2 规律在案）。
     * getWorldInfoM=y（⚠️181=x）；WorldInfo=sx 字段字母与 rl 全同（a/e/b/c/d/p 对榫
     * dump L8871-8887）；weather 浮值面 o/q ✓同 181 字母（旗标 setter 完整语义=活体判决项）。
     */
    private static VersionTable build19pre2() {
        VersionTable t = new VersionTable();
        t.versionId = "b1.9-pre2";
        t.family = "G3"; // 扁平散件+未混淆主类（G3-beta，b1.7.3/b1.8.1 同族）
        t.jarSha1 = "b0d40cf43b625631af65e2a645c34b533251da0e"; // 索引对榫件（锚库主 sha）

        // World 写读面（ti；tiny L8257 块逐锚）
        t.setBlockM = "g";        // g(IIII)Z setBlockWithNotify ✓同 181 字母
        t.getBlockM = "a";        // a(III)I ✓历代同字母
        t.getMetaM = "e";         // e(III)I ✓同 181
        t.setMetaSilentM = "c";   // c(IIII)Z setBlockMetadata 裸静默写（⚠️181=f(IIII)V
        // 断代翻面；返回值不消费，f(IIII)V WithNotify/d(IIII)Z setBlock/e(IIII)Z doChunks 同形陷阱群）
        t.getLightM = "n";        // n(III)I ✓同字母（dump L9145）
        t.saveWorldM = "a";       // a(Lpd;)V public 包装 saveWorldIndirectly（dump L9204，
        t.saveWorldDesc = "(Lpd;)V"; // IProgressUpdate yb→ny→**pd** 断代；本体 a(ZLpd)V null 守卫）
        t.teGetM = "b";           // b(III)Lje; getBlockTileEntity（IBlockAccess afm L1529 直钉；
        // TileEntity ow→ij→**je** 断代；chest/furnace 面未钉=门先响，本槽不消费）
        // WorldInfo 面（sx=ei/rl 字段表全同字母复刻——a:J seed/e:J time/b/c/d:I spawn/p:I GameType）
        t.getWorldInfoM = "y";    // ti.y()Lsx; getWorldInfo（⚠️181=x——dump L9255 断代）
        t.worldTimeF = "e";       // sx.e:J=worldTime ✓
        t.randSeedF = "a";        // sx.a:J=randomSeed ✓
        t.spawnXF = "b";          // sx.b/c/d:I=SpawnX/Y/Z ✓
        t.spawnYF = "c";
        t.spawnZF = "d";

        // 玩家/实体定位面（Entity=lh abstract；档位三同族）
        t.playerF = null;
        t.playerListF = "i";      // ti public List i=playerEntities ✓同 181 字母
        t.entityClassG = "lh";    // ⚠️ 撞车位群：181 lh=EntityDamageSource↔pre2 lh=Entity
        t.setPosRotM = "c";       // c(DDDFF)V setLocationAndAngles（⚠️181=b——断代翻面，见上注④）
        t.setPosRotDesc = "(DDDFF)V";
        t.posXF = "o";            // lh.o/p/q=posX/Y/Z（173=aM..aT 右移规律在案）
        t.posYF = "p";
        t.posZF = "q";
        t.motXF = "r";            // NBT "Motion" 配对
        t.motYF = "s";
        t.motZF = "t";
        t.yawF = "u";             // NBT "Rotation" 配对
        t.pitchF = "v";
        t.healthF = "a";          // xx.a=health **private**（⚠️181=bz public——healthField
        // 本批 getDeclaredField 父链回退，见上注③）
        t.iframesF = "Z";         // lh.Z=heartsLife public int（⚠️181=aa——回归钉，见上注⑨）

        // HUD 次级链（主类字段面 JSON 钉：theWorld=f/thePlayer=i（声明 xx 断代）/
        // ingameGUI=v/gameSettings=z(gk)/fontRenderer=q(lf)/mcDataDir=E；
        // 门=gk.E=showDebugInfo（tiny L3627；173=kv.B/181=fv.E——类断代字母 E 保留））
        t.hudPlayerF = "i";
        t.hudFontF = "q";
        t.hudSettingsF = "z";
        t.hudDebugGateF = "E";
        t.hudXyzPaint = Boolean.FALSE; // a1.2.3 边界线（§5.1）：F3 自带坐标=注入免做

        // 物品栏面（EntityPlayer=uo.ar→vz；⚠️ invF ar（181=as 右移一位））
        t.invF = "ar";
        t.invMainF = "a";         // vz.a=mainInventory[wc] ✓
        t.invArmorF = "b";
        t.invCurF = "c";          // ⚠️ vz.d=player/e=inventoryChanged 互换陷阱延续
        t.isCountF = "a";         // wc a=stackSize/c=itemID public，e=itemDamage **private**
        t.isIdF = "c";            // （⚠️181 d public——断代访问位翻面，resolveInvFace 回退）
        t.isDmgF = "e";
        t.addStackM = "a";        // vz.a(wc)Z addItemStackToInventory ✓
        t.itemClassG = "uk";      // ⚠️ 撞车位：181 uk=…↔pre2 uk=Item
        t.itemsF = "e";           // public static uk[] e（⚠️181=f——dump L9684 断代）
        t.itemEntityG = "eq";     // EntityItem eq（hl→ee→eq 断代）

        // 实体/世界事件面
        t.explodeM = "a";         // a(Llh;DDDF)Labf（V→Explosion 断代延续，返回值不消费）
        t.spawnEntityM = "a";     // a(Llh;)Z entityJoinedWorld ✓同 181
        t.setEntityDeadM = "b";   // b(Llh;)V ✓同 181
        t.entityListM = "p";      // p()List getLoadedEntityList ✓同 181
        t.entityTypeM = null;     // per-instance 命名面未钉（同族判决）
        t.entityListClassG = "wd"; // ⚠️ 撞车位：181 wd=EntityLiving↔pre2 wd=EntityList
        t.entityListLookupM = "b"; // wd.b(lh)String ✓同字母（dump L10617）
        t.playSoundM = "a";       // a(DDD,String,F,F)V playSoundEffect ✓

        // getinfo 面
        t.difficultyF = "v";      // ti public int v=difficultySetting ✓同 181

        // spawn 白名单（mobClasses 全表断代；⚠️ mob 槽 null：vp=EntityCreature abstract
        // 同 181 el181-1 判决——b1.8-pre1-2 起无泛用可实例化 mob 基类）
        t.mobClasses.put("pig", "oa");
        t.mobClasses.put("sheep", "cd");
        t.mobClasses.put("creeper", "ub");
        t.mobClasses.put("skeleton", "ts");
        t.mobClasses.put("zombie", "adt");
        t.mobClasses.put("spider", "bk");
        t.mobClasses.put("tnt", "abg");   // (ti,DDD)V——FFF 断代延续（spawn fallback）
        t.mobClasses.put("giant", "ip");  // EntityGiantZombie extends uq(EntityMob) 构造健在

        // biome 面（三锚链 ti.a()Loz→oz.a(II)Lwz→wz.r=biomeName，173 同形字母断代）
        t.biomeWcmM = "a";        // ti.a()Loz; getWorldChunkManager（无参唯一）
        t.biomeAtM = "a";         // oz.a(II)Lwz; getBiomeGenAt ✓
        t.biomeNameF = "r";       // wz.r=biomeName（⚠️181=vh.l 断代）

        // D10 XP 面（19pre2 首版全方法面——键名与锚库 ops 段逐一对榫；
        // 取证=tiny L9460/9497/9519/9459/9472 实锤，字段全 public int：
        // aW=xp/aY=xpLevel/aX=playerLevel(XpTotal 载体)/ax=score/aF=xpCooldown。
        // 双 level 字段并存怪癖（1.0+ 合并）——setxp 写 aY/aW/aX/aF 四件，语义同 181 配方。
        // addXpM=f=increaseXP(I)V（dump L10030 public）/increaseLevelM=aL=increaseLevel()V
        // （dump L10032 **private**）——本批留档不消费，setxp 走直写面。
        // ⚠️ 与 181 aX/aY/aZ/aG/ay 整体不同，互抄即错（VersionTable D10 注同纪律）。
        t.capXp = Boolean.TRUE;
        t.xpF = "aW";             // NBT "Xp"（当前级内进度经验）
        t.xpLevelF = "aY";        // NBT "XpLevel"（等级）
        t.playerLevelF = "aX";    // NBT "XpTotal"（累计总经验载体）
        t.scoreF = "ax";          // NBT "Score"
        t.xpCooldownF = "aF";     // NBT "XpCooldown"（写 0=立即可再拾）
        t.addXpM = "f";           // increaseXP(I)V public（留档，直写面不消费）
        t.increaseLevelM = "aL";  // increaseLevel()V private（留档）

        // D10 效果面（19pre2 首次完整可钉——giveeffect/removeeffect 消费；
        // 取证=tiny L11084/11115/11028/10988/11013 块实锤：
        // xx.a(Lafk;)V=addPotionEffect / i(I)V=removePotionEffect / aG()V=clearActivePotions
        // （181 无清空面 null→本版 aG 启用候选，本批仍走逐 id 移除）/
        // a(Lvt;)Z=isPotionActive / b(Lvt;)Lafk;=getPotionEffect（参类=Potion(vt) 非 int——
        // MobEffectList 名=Potion 命名陷阱，1.0+ 才改名）/ bP=activePotionsMap protected
        // HashMap（键=Potion id）。afk 构造 (III)V（tiny L1478 id/duration/amplifier）
        // 三字段 a/b/c public（L1476/1482/1484）。xpOrbClassG=bi（EntityXPOrb 在册，
        // spawn 面后续批，本批不消费）。
        t.capEffect = Boolean.TRUE;
        t.addEffectM = "a";       // xx.a(Lafk;)V
        t.addEffectDesc = "(Lafk;)V";
        t.removeEffectM = "i";    // xx.i(I)V
        t.clearEffectsM = "aG";   // xx.aG()V（181=null 断代新增）
        t.isActiveEffectM = "a";  // xx.a(Lvt;)Z（留档）
        t.getActiveEffectM = "b"; // xx.b(Lvt;)Lafk;（留档）
        t.effectsMapF = "bP";     // xx.bP protected HashMap（留档，give/remove 走方法面）
        t.potionClassG = "vt";    // Potion（isActive/get 参类，留档）
        t.potionEffectClassG = "afk"; // PotionEffect 类名
        t.potionEffectCtorDesc = "(III)V"; // (id,durationTicks,amplifier)
        t.xpOrbClassG = "bi";     // EntityXPOrb（spawn 面后续批）

        // D11 饥饿面（19pre2 转正批补钉——锚件 ops 段原缺，tiny L4896 块实锤：
        // uo.av()Lla;=getFoodStats（tiny L9418；⚠️181=at()Lkc 断代）；
        // la 字段面 a=foodLevel:I(默认 20)/b=foodSaturationLevel:F/c=foodExhaustion:F/
        // d=tickTimer:I/e=prevFoodLevel:I（L4907/4902/4897/4900/4901）全 private
        // （dump L5723 块）——181 kc 同字母同形，getDeclaredField+setAccessible 同配方。
        t.capHunger = Boolean.TRUE;
        t.foodStatsM = "av";      // uo.av()Lla; getFoodStats
        t.foodStatsClassG = "la"; // FoodStats 类名
        t.foodLevelF = "a";       // la.a=foodLevel private int
        t.foodSatF = "b";         // la.b=saturation private float
        t.foodExhaustF = "c";     // la.c=exhaustion private float（留档）
        t.foodTimerF = "d";       // la.d=tickTimer private int（留档）

        // D12 weather（19pre2 浮值面 o/q ✓同 181 字母——ti protected float
        // rainingStrength=o/thunderingStrength=q（tiny L8698/8749）；prevRaining=n/
        // prevThundering=p 非写面。⚠️ 完整语义候选：sx 旗标 setter setIsRaining=b(Z)V/
        // setIsThundering=a(Z)V（JSON note 钉，写旗标免 updateWeather 斜坡覆写）=
        // 活体判决项，本 op 维持浮值面同 181 判决。
        t.capWeather = Boolean.TRUE;
        t.rainStrF = "o";         // ti.o=rainingStrength protected float
        t.thunderStrF = "q";      // ti.q=thunderingStrength protected float

        // D13 gamemode（sx.p=gameType private int——tiny L8128 实锤，NBT "GameType"；
        // sx=rl 字段字母全同判决兑现。写面=WorldInfo 持久层（重进世界生效），
        // mode∈[0,1]（adventure=2 系 b1.9 正式版后，pre2 越界 Rejected 同 181 判决）。
        t.capGamemode = Boolean.TRUE;
        t.gameTypeF = "p";        // sx.p=gameType private int

        return t;
    }
}
