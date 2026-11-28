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
    private static final VersionTable T_19pre3 = build19pre3();
    private static final VersionTable T_1501 = build1501();
    private static final VersionTable T_1401 = build1401();
    private static final VersionTable T_16 = build16();
    // 全覆盖批产试点转正批（2026-11-28，L-12）：b1.6 家族六版=SlotVerify v1 自动
    // 继承校验草稿锚（registry/fingerprints/b1.6.{1..6}.json）转写转正。
    // 1.6.1-1.6.4=零断代（69/69 槽全绿，与 build16 全同形）；
    // 1.6.5/1.6.6=断代（si aD 起整段右移一位+lo int 组右移一位；断代起点=b1.6.5）：
    // healthF X→Y / yawF aR→aS / pitchF aS→aT / pos aL-aN→**aM-aO** / mot
    // aO-aQ→**aP-aR**（pos/mot 位移=b1.6.5 活体翻案腿 2026-11-28——SlotVerify
    // 原判「double 组不动」系字母存在性假绿，活体 getplayer 循环错位实锤后
    // javap 三版对拍钉死；工具已补 pos DLOAD 语义腿+mot 布局腿）。语义锚=字节码
    // 证据（LDC "Health"→GETFIELD I；(DDDFF)V 全体方法 DLOAD 1/3/5→PUTFIELD D、
    // FLOAD 7/8→PUTFIELD F），log=%TEMP%\mcaut\exp\fullcov\b1.6.*.log。
    // 单源=derive16 继承 build16()（76 槽+9 mob+mobs 全量），
    // 对榫=A2Table16FamilyTest 六版逐槽 vs 草稿 JSON+BY_SHA1 反查+TARGETS_16
    // 复用（钩四项 class/method/desc/access 六版全等，SlotVerify 已验）。
    private static final VersionTable T_161 = derive16("b1.6.1", "63a66d6d145696296bdaaeaba0a42f738b87a362", "X", "aR", "aS", false);
    private static final VersionTable T_162 = derive16("b1.6.2", "e8aa50949b077b672be2e651ea3f7b1bbd9020e1", "X", "aR", "aS", false);
    private static final VersionTable T_163 = derive16("b1.6.3", "924e36dbb7c64abb30a95fe35f5affb5176f6cbc", "X", "aR", "aS", false);
    private static final VersionTable T_164 = derive16("b1.6.4", "b5d3bdb8a7b12d163651f4787ac6ca14689aab9e", "X", "aR", "aS", false);
    private static final VersionTable T_165 = derive16("b1.6.5", "90ed9854b43c4d031ed07381ea3ae3071a8bba6f", "Y", "aS", "aT", true);
    private static final VersionTable T_166 = derive16("b1.6.6", "f95fe05711d09553ca2a9089f981741c13d6b8c4", "Y", "aS", "aT", true);
    // 全覆盖批产 b1.7 带批转正（2026-11-28，L-13）：b1.7/b1.7_01/b1.7.2 三版=
    // SlotVerify v2.1 自动继承校验草稿锚（registry/fingerprints/b1.7*.json）
    // 转写转正。三版 **75/75 零断代**（vs 基准锚 b1.7.3=T_173 转正件+h10 活体
    // 判决在案）：hooks 四项六元组全等（TARGETS_173 复用）+派生链全同形
    // （World=fd/EP=gs/Entity=sn/EntityLiving=ls/WI=ei…）。唯一首轮 MISS=
    // setMetaSilentM=工具假红（(IIII)Z 硬码 vs 真件 void——tiny fd.d=
    // setBlockMetadataWithNotify (IIII)V+b1.7↔1.7.3 方法体逐帧同+WorldOps
    // getMethod 不校验返回型，v2.1 放宽 Z|V）。钉版=version-index L370-372
    // （b1.7.1 不存在、b1.7.10 假件不收）。单源=derive17 继承 build173()
    // 覆写 id/sha 两键，零槽位改动；对榫=A2Table17FamilyTest。
    private static final VersionTable T_17 = derive17("b1.7", "ad7960853437bcab86bd72c4a1b95f6fe19f4258");
    private static final VersionTable T_17_01 = derive17("b1.7_01", "b0f67f3105e32b1a905d934f1e56117ef45d7148");
    private static final VersionTable T_172 = derive17("b1.7.2", "7dc50cc5e2ff204a7283f0c7d38cd0370b49875b");

    private static final Map<String, VersionTable> BY_ID = new HashMap<String, VersionTable>();
    private static final Map<String, VersionTable> BY_SHA1 = new HashMap<String, VersionTable>();

    static {
        VersionTable[] tables = { T_0327, T_0618, T_0629, T_1121, T_120, T_1202, T_173, T_181, T_19pre2, T_19pre3, T_1501, T_1401, T_16,
                T_161, T_162, T_163, T_164, T_165, T_166, T_17, T_17_01, T_172 };
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
        t.blockClassG = "net.minecraft.a.a.b.x"; // Block 类（static x[] 字段=c，54 实例字段，取证批）
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
        t.blockClassG = "net.minecraft.a.a.b.aa"; // Block 类（static aa[] 字段=b，57 实例字段；0618 无 itemClassG 锚，靠 final 性+256/1024 长度闭合）
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
        t.blockClassG = "gk"; // Block 类（static gk[] 字段=b，45 实例字段=老 infdev 注册数；⚠️ 撞车：gk 在 b1.6=Item，禁跨代复用）
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
        t.blockClassG = "ly"; // Block 类（static ly[] 字段=n，78 实例字段，姊妹仓交叉对榫）
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
        t.blockClassG = "ne"; // Block 类（static ne[] 字段=n，83 实例字段，姊妹仓交叉对榫）
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
        t.blockClassG = "pj"; // Block 类（static pj[] 字段=m，84 实例字段；姊妹仓 dev 无 Block.java，jar 侧三判据+锚 eo 排除闭合）
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
        t.blockClassG = "uu"; // Block 类（static uu[] 字段=m，84 实例字段，姊妹仓交叉对榫）
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
        // World rainingStrength=j:F/thunderingStrength=l:F 浮值面）；
        // h6-B 旗标双写转正（真 jar javap NBT 地面真值：ei.b(Z)→l:Z=raining/
        // ei.a(Z)→n:Z=thundering 全 public，T1 机制直接适用）
        t.capWeather = Boolean.TRUE;
        t.rainStrF = "j";         // verscan b1.7.3 §0-§11 浮值面
        t.thunderStrF = "l";
        t.wiSetRainM = "b";
        t.wiSetThunderM = "a";

        // h8 批 TE 容器面转正（b1.7.3 真 jar tiny+javap 实锤：lw=IInventory
        // a()I/f_(I)Liz/a(ILiz)V；sk=TileEntityFurnace public int a=furnaceBurnTime/
        // c=furnaceCookTime（b=currentItemBurnTime 非写面）+private iz[] i；
        // js=TileEntityChest private iz[] a=chestContents 27 槽——a 撞数组字段形，
        // getFurnace int 门命中）。
        t.invSizeM = "a";
        t.invGetM = "f_";
        t.invSetM = "a";
        t.teBurnF = "a";
        t.teCookF = "c";

        // h10 批 sleep 面（活体 173 崩溃翻案 T2 裸字段配方——tick 睡眠段 gs.am()
        // 裸解引用 v:Lbr; bedChunkCoordinates，裸写 sleeping 必 NPE）。写面改游戏
        // API：gs.b(III)Lcw;=sleepInBedAt（成功尾 u=true/a=0/v=new br/清动量/fd.y()，
        // 返 cw.a）、gs.a(ZZZ)V=wakeUpPlayer（v==null ifnull 跳床段=null-safe）——
        // 字节码 %TEMP%/mcaut/b173/gs_disasm.txt L1579/L2066/L1909 全量实锤。
        t.capSleep = Boolean.TRUE;
        t.sleepInBedM = "b";
        t.wakeUpM = "a";
        t.sleepOkF = "a";

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
        t.blockClassG = "lr"; // Block 类（static lr[] 字段=m，97 实例字段，姊妹仓交叉对榫）
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
        // 覆写（斜坡陷阱在案）；h6-B 旗标双写转正（真 jar javap：rl.o()Z→l:Z=isRaining/
        // rl.m()Z→n:Z=isThundering getter 字母洗牌，setter rl.b(Z)→l/rl.a(Z)→n 全 public，
        // NBT raining/thundering 地面真值对榫——完整态=T1 双写，到场包判决销案）。
        t.capWeather = Boolean.TRUE;
        t.rainStrF = "o";         // rv.o=rainingStrength protected float
        t.thunderStrF = "q";      // rv.q=thunderingStrength protected float
        t.wiSetRainM = "b";
        t.wiSetThunderM = "a";

        // h7 批 TE 容器面转正（b1.8.1 真 jar tiny+javap 实锤：gl=IInventory
        // a()I=getSizeInventory/d_(I)Lul=getStackInSlot/a(ILul;)V=setInventorySlotContents；
        // aaa=TileEntityFurnace public int a=furnaceBurnTime/c=furnaceCookTime
        // （b=currentItemBurnTime 非写面）；fh=TileEntityChest public boolean a=
        // numPlayersUsing 撞 teBurnF 同 pre2 fv 形，int 门防线在位）。
        t.invSizeM = "a";
        t.invGetM = "d_";
        t.invSetM = "a";
        t.teBurnF = "a";
        t.teCookF = "c";

        // h10 批 sleep 面（同 173 翻案配方）：sz.d(III)Lbm;=sleepInBedAt（尾 aN=true/
        // a=0/aO=new ps/k=rv·I 门/rv.y()V/返 bm.a，disasm L2299 区段实锤）、
        // sz.a(ZZZ)V=wakeUpPlayer（L2514 区段：aN=false/iload_2→rv.y()/iload_1→a=0|100）。
        // ⚠️ ul.d=itemDamage **private**（javap 实锤）——setchest getDeclaredField
        // 修复对 181 同样必要。
        t.capSleep = Boolean.TRUE;
        t.sleepInBedM = "d";
        t.wakeUpM = "a";
        t.sleepOkF = "a";

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
        t.blockClassG = "mq"; // Block 类（static mq[] 字段=m，102 实例字段，姊妹仓交叉对榫）
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
        // T7 批（无头窗 2026-11-26）：TE 容器面转正——tiny+dump 双源实锤：
        // IInventory=hc（getSizeInventory=a()I / getStackInSlot=**e_**(I)Lwc; 尾下划线
        // 撞字母规避面 / setInventorySlotContents=a(ILwc;)V 全 public）；
        // TileEntityFurnace=acb extends je implements hc（public int a=furnaceBurnTime/
        // b=currentItemBurnTime/c=furnaceCookTime——**public 非 private**，0327 f/h 字母
        // 与访问位双断代，严禁互抄再证；items d=private wc[]）；
        // TileEntityChest=fv（items p=private wc[]，27 槽）。
        t.invSizeM = "a";         // hc.getSizeInventory ()I
        t.invGetM = "e_";         // hc.getStackInSlot (I)Lwc;（⚠️ e_ 非 e）
        t.invSetM = "a";          // hc.setInventorySlotContents (I,Lwc;)V
        t.teBurnF = "a";          // acb.furnaceBurnTime public int（0327=f 断代）
        t.teCookF = "c";          // acb.furnaceCookTime public int（0327=h 断代）
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
        // ⚠️ 活体批1/2 三层撞名在案（同字母三形态）：sd(EntityPlayerSP).a=mn public
        // 非 int 遮蔽 + uo(EntityPlayer).a=sleepTimer private int 劫持玩家链——
        // livingClass 探针 v2 走 mobClasses 链（批2），批3 闭环 [h=]/sethealth/
        // level.dat Health=15 全实锤。跨版本抄 a 者必读本注。
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

        // 药水活体落地批（2026-11-28，potiontest 消费；取证=potcmp all-p2.txt javap 全量
        // 逐类对榫，源签名↔混淆签名——判决注释详 VersionTable 药水链段头注）。
        // PotionHelper=go（static HashMap a/b+String[] c+解释器 a(String,III)I 全形同构；
        // dd_d=d(I)Ljava/util/List; 由 dn.a_ 体 invokestatic go.d 直接对榫；静态块 a 收
        // 20 put/b 收 7 put=potionRequirements/potionAmplifiers 声明序对榫）。
        // ItemPotion=dn（extends uk；onFoodEaten=**b** 非 a——b 体 stackSize--/ti.I 门/
        // new afk(Lafk;)V 拷贝构造/uo.a(Lafk;)V 挂载/uk.bt glassBottle 五锚全中；
        // a(wc,ti,uo) 同形=onItemRightClick 陷阱，体仅 uo.c(Lwc;I)V setItemInUse）。
        // PotionEffect=afk 三 getter a/b/c（dn.getRecord 体对榫：a()=vt.a[] 索引、
        // b()=duration>20、c()=potion.potency 拼接）。cauldron 存在性（不入槽，留档）：
        // TileEntityCauldron=yf（extends je，双 private int+clickedOnCauldron a(wc)Z+
        // uk.e itemsList 直引）/BlockCauldron=to（extends abv BlockContainer，d()I=24
        // 渲染型+o_()Lje; new yf+blockActivated 状态机——**无 <init>**=从未实例化死代码，
        // 权威文档 §6"无方块 ID"实锤）；"Cauldron" 字符串全 jar 仅 je@78 一处（TileEntity
        // addMapping 注册面）。
        t.capPotionTest = Boolean.TRUE;
        t.potHelperClassG = "go";
        t.potDddM = "d";
        t.potDddDesc = "(I)Ljava/util/List;";
        t.potReqMapF = "a";        // go.a=potionRequirements（size=20 判决指纹）
        t.itemPotionClassG = "dn";
        t.itemPotionOnEatM = "b";
        t.itemStackClassG = "wc";
        t.potionItemF = "bs";      // uk.bs=Item.potion（dn.getRecord getstatic 对榫）
        t.peIdM = "a";             // afk 三 getter 同 pre3 agp 字母（getRecord 体逐锚）
        t.peDurM = "b";
        t.peAmpM = "c";

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
        // T1 批（无头窗 2026-11-26）：旗标 setter 面首钉——sx.setIsRaining=b(Z)V/
        // setIsThundering=a(Z)V public（dump L8871-8887 在册；活体批3 实证浮值面
        // 不落 level.dat raining/thundering 键=斜坡覆写，完整语义走旗标）。
        t.wiSetRainM = "b";
        t.wiSetThunderM = "a";

        // h10 批 sleep 面（活体 173 崩溃翻案 T2 裸字段配方，同法重钉）：
        // uo.d(III)Lbr;=sleepInBedAt（门 ap()Z/H()Z→br.e、ti.k()Z 白天→br.c、
        // 距→br.d；尾 aM=true/a=0/aN=new rc/s-t-r 清零/ti.z()V/返 br.a，
        // disasm L2323 区段）、uo.a(ZZZ)V=wakeUpPlayer（L2538 区段同构）。
        // ⚠️ 本版 EnumStatus=br（173 的 br=PairCoordinate 字母表独立——严禁按
        // 173 印象推）；ItemStack wc **无 (II)V**、isDmgF=e private（d→e 断代在案）。
        // b1.3_01 无床维持 UNSUPPORTED 判决。
        t.capSleep = Boolean.TRUE;
        t.sleepInBedM = "d";
        t.wakeUpM = "a";
        t.sleepOkF = "a";

        // D13 gamemode（sx.p=gameType private int——tiny L8128 实锤，NBT "GameType"；
        // sx=rl 字段字母全同判决兑现。写面=WorldInfo 持久层（重进世界生效），
        // mode∈[0,1]（adventure=2 系 b1.9 正式版后，pre2 越界 Rejected 同 181 判决）。
        t.capGamemode = Boolean.TRUE;
        t.gameTypeF = "p";        // sx.p=gameType private int

        return t;
    }

    /** b1.9-pre3-1402 内建表——G3 家族第 4 例（药水活体落地批 2026-11-28 **最小转正**，
     *  非全 76 槽：仅钉 potiontest 依赖面（药水链 12 槽+效果回读面+worldPlayer 取玩家链）
     *  与身份三元组；其余槽 null=对应 op 响亮 UNSUPPORTED，后续批按取证补钉。
     *  取证=potcmp all-p3.txt javap 全量逐类对榫（%APPDATA%\.attic\versions\
     *  b1.9-pre3-1402.jar sha1=5b7fe76a… 本窗 Get-FileHash 实算对榫）；
     *  mainClass=net.minecraft.client.Minecraft 不混淆（TARGETS_19pre3 类名即证，
     *  VersionTable 无 mainClass 槽——launcher 侧由 versionId 解析）。 */
    private static VersionTable build19pre3() {
        VersionTable t = new VersionTable();
        t.versionId = "b1.9-pre3-1402";   // launcher id 对齐（attic 文件名同源）
        t.blockClassG = "nd"; // Block 类（static nd[] 字段=m，107 实例字段；nd@4045 锅注册面同族件）
        t.family = "G3";
        t.jarSha1 = "5b7fe76a602b7511c97740e36dc25040ccb6e76b"; // Get-FileHash 实算

        // ===== 药水链全钉（T1 取证，判决注释详 VersionTable 药水链段）=====
        // PotionHelper=gv：static String×9+potionRequirements/potionAmplifiers 双 HashMap
        // (j/k)+String[] l+解释器 a(String,III)I 全形同构；dd_d 重构为 aaaa_Aaa_(int,
        // boolean)→混淆 **a(IZ)Ljava/util/List;**（do.b_ 体 invokestatic gv.a:(IZ) 直接
        // 对榫，iconst_0=非坏效果门）。静态块 j 收 8 put/k 收 7 put——**reqTableSize=8
        // 判决指纹**（vs pre2=20，配方表 one-hot 重构的表尺寸证据）。
        t.capPotionTest = Boolean.TRUE;
        t.potHelperClassG = "gv";
        t.potDddM = "a";
        t.potDddDesc = "(IZ)Ljava/util/List;";
        t.potReqMapF = "j";       // gv.j=potionRequirements（声明序：9 常量串后首位）
        // ItemPotion=do（extends vf=Item）；onFoodEaten=**b(wy,uc,vj)Lwy;**——b 体
        // wy.a isub(stackSize--)/uc.I(multiplayerWorld)/new agp(Lagp;)V 拷贝构造/
        // vj.a(Lagp;)V 挂载/vf.bt glassBottle 五锚与 pre2 dn.b 逐指令同构；
        // a(wy,uc,vj) 同形=onItemRightClick 陷阱（体仅 b(wy)I+vj.c(Lwy;I)V）。
        t.itemPotionClassG = "do";
        t.itemPotionOnEatM = "b";
        t.itemStackClassG = "wy";
        t.itemClassG = "vf";      // Item 类名（vf[]=e itemsList 注册表+bs/bt 字段面与
                                  // pre2 uk 逐字母同构——do extends vf 声明即证）
        t.potionItemF = "bs";     // vf.bs=Item.potion **Ldo;** 收窄声明=类字双证
        t.peIdM = "a";            // agp 三 getter 与 pre2 afk 撞同字母（do.getRecord 体
        t.peDurM = "b";           // a()=wo.a[] 索引/b()=duration>20/c()=potency 逐锚对榫）
        t.peAmpM = "c";

        // ===== 效果回读面（potiontest readback=activePotionsMap 遍历依赖）=====
        // EntityLiving=yv（与 pre2 xx 字段面逐字母同构：bP=activePotionsMap **撞同字母**
        // protected HashMap；addPotionEffect=a(Lagp;)V；removePotionEffect=i(I)V；
        // isPotionActive=a(Lwo;)Z；getPotionEffect=b(Lwo)Lagp;——yv 全类声明比对实锤）。
        // Potion=wo（wo[] a=potionTypes 32 槽+protected wo(IZI)V+static a(Lagp;)String——
        // 与 pre2 vt 同构，pre3 新增 L/M/N 三字段=effectiveness 面）。
        t.capEffect = Boolean.TRUE;
        t.addEffectM = "a";
        t.addEffectDesc = "(Lagp;)V";
        t.removeEffectM = "i";
        t.effectsMapF = "bP";
        t.potionClassG = "wo";
        t.potionEffectClassG = "agp";
        t.potionEffectCtorDesc = "(III)V";   // agp(int,int,int) 声明序首构对榫

        // ===== worldPlayer 取玩家链（potiontest applied 段依赖）=====
        t.playerListF = "i";     // uc.i=playerEntities——uc.a(Llu;)Z 实体入列体内
                                 // instanceof vj→checkcast→getfield i→List.add 与 pre2
                                 // ti.a(Llh;)Z 逐指令同构（本窗 javap -c 对榫，非猜字母）
        t.entityClassG = "lu";   // Entity 抽象基类（yv extends lu 声明即证）

        // 缺陷二修复显式声明（2026-11-28 save 腿 NPE 实锤批）：op 面 saveWorldM 未钉
        // （最小转正），且 pr 档 null 守卫未取证——即便未来补钉也先响亮拒绝，
        // javap a(ZLpr)V 守卫核实后方可撤 FALSE（pre2 pd 档先例=守卫齐全可直传）
        t.saveNullArgOk = Boolean.FALSE;

        // ===== 钩面所需（TARGETS_19pre3 对榫，HookTargets 消费非本表槽，此处留档注释）：
        // SAVE_WORLD=uc.a(Lpr;)V public（体 iconst_1→a(ZLpr;)V 核心=pre2 ti.a(Lpd;)V
        //   同包装配方；pr=IProgressUpdate）
        // LOAD_WORLD=Minecraft.a(Luc;Ljava/lang/String;Lvj;)V public 3 参实体
        // RUN_TICK=Minecraft.k()V public（体首串 z()V→kg.a→ago.d→aeo.a()V→kd.a(F)V→
        //   uc.w()Lbi; 与 pre2 k() 的 z/afj.d/adm.a/jt.a/ti.w 调用序列同构；i/j 为
        //   初始化小体排除）
        // HUD_RENDER=aeo.a(FZII)V public（aeo extends mr+ctor aeo(Minecraft)=pre2 adm
        //   同形；k() 体调用 a(FZII)V 实锤）
        // cauldron 存在性（不入槽，留档）：TileEntityCauldron **已删**——jm 子类无
        // 双 int+clickedOnCauldron 同形类，且 TileEntity.java L128 addMapping
        // (TileEntityBrewingStand.class,"Cauldron")（映射名保留换指向类）；
        // BlockCauldron=ui（extends nd 非 abv！pre3 降级为元数据方块：ui(int)→
        // nd(int,yr.f=iron)+154 纹理、d()I=24、a(II)I=138/155/154、b(uc,iii,vj)Z
        // 简化 bucketWater/glassBottle 元数据状态机、0.3125f/0.125f 包围盒）；
        // 补注册面实锤：nd@4045 ldc "cauldron"（Block.java L141 setBlockName）+
        // vf@3348 ldc "cauldron"（Item.java L133 Item.cauldron=ItemReed(118)）。

        // 其余槽（物品栏/方块表/世界目录/实体列表/经验饥饿面等）本批未取证——
        // 保持 null，对应 op 经 require()/resolve() 响亮 UNSUPPORTED。
        return t;
    }

    /**
     * b1.5_01 表（Beta 1.5_01，天气/群系首例版——活体批转正 2026-11-28，仿
     * build19pre2 体例）。值源=锚库 java/agent/registry/fingerprints/b1.5_01.json
     * （mca-fingerprint-v1 草稿锚，A2Table1501Test 逐槽对榫在案）；取证=
     * docs/compatibility/verscan/b1.5_01.md + %TEMP%\mcaut\exp\b1501\
     * （javap 全量 9052 行+et_c/mc_c/tl_c 定向 -c+client.tiny 直钉；件 SHA1
     * e2a692e5… 对榫版本索引行 L362）。三大边界判决：①WorldInfo 架构确立版
     * （et 不再持 time/seed/spawn——dy 承载全 private，字母与 173 ei 全同）；
     * ②天气引入首例（et 四浮值 B/C/D/E+dy 持久化四键 l/m/n/o 小写 NBT；
     * ⑰批 javap -c 裁决 C=rainStrength/E=thunderStrength——getter 错位自食已销案）；
     * ③saveWorld 无 1 参包装世代（仅 a(ZLwr;)V 双参实体，wr null 守卫 @14/@30
     * 实锤——173/181/pre2 包装配方不适用，WorldOps (false,null) 双参分支）。
     * vs 173 字母断代（详表 note）：World fd→et、Entity sn→rj、EntityLiving
     * ls→kw、EntityPlayer gs→gh、InventoryPlayer ix→ih、ItemStack iz→ii、
     * Item gm→gb、EntityList jc→ik、GuiIngame uq→tl、WorldInfo ei→dy、
     * IProgressUpdate yb→wr。pos 面整体左移两位（aK..aR vs 173 aM..aT——
     * 严禁借表；+1/版规律 1301=aI..→1401=aJ..→1501=aK..→16=aL..→173=aM..）。
     * iframes=null（kw 无 heartsLife；⚠️rj.by=skinUrl 撞车位陷阱）→invulnerable
     * UNSUPPORTED。RUN_TICK=j（⚠️173/181/pre2=k 断代——j→k 翻面下界=b1.7，
     * V-5 批 b1.6 复验后修正为 b1.6=k/1501=j）。
     */
    private static VersionTable build1501() {
        VersionTable t = new VersionTable();
        t.versionId = "b1.5_01";
        t.blockClassG = "to"; // Block 类（static to[] 字段=m，87 实例字段，姊妹仓交叉对榫；⚠️ 撞车：to 在 pre2=BlockCauldron 死代码件，跨 JVM 无碍禁复用）
        t.family = "G3"; // 扁平散件+未混淆主类（G3-beta，173 同族）
        t.jarSha1 = "e2a692e5e8160c84b29c834ecbf398618db9749c"; // 对榫版本索引 L362

        // World 写读面（et；详表 §2 javap 实锤）
        t.setBlockM = "e";        // e(IIII)Z setBlockWithNotify（⚠️173=f/1401=e/16=f）
        t.getBlockM = "a";        // a(III)I ✓历代同字母
        t.getMetaM = "e";         // e(III)I getBlockMetadata（同字母异描述符共存）
        t.setMetaSilentM = "d";   // d(IIII)V setBlockMetadata（⚠️16=e(IIII)Z 断代）
        t.getLightM = "l";        // l(III)I（⚠️173=n/16=n 断代）
        t.teGetM = "b";           // b(III)Lnx;（TileEntity=nx）
        t.saveWorldM = "a";       // a(ZLwr;)V 双参实体——⚠️ 无 1 参 public 包装
        t.saveWorldDesc = "(ZLwr;)V"; // （173/181/pre2 包装配方不适用；wr null 守卫
                                  // @14/@30 实锤；a(Z)Z=TickUpdates 禁钩同形预避）
        // WorldInfo 架构确立版（判决①）：字段在 dy 非 et——
        t.getWorldInfoM = "x";    // et.x()Ldy; getWorldInfo（⚠️1401=w 断代/16=x 同）
        t.worldTimeF = "e";       // dy.e:J=worldTime（NBT "Time"）
        t.randSeedF = "a";        // dy.a:J=randomSeed（NBT "RandomSeed"）
        t.spawnXF = "b";          // dy.b/c/d:I=SpawnX/Y/Z（五版同形链）
        t.spawnYF = "c";
        t.spawnZF = "d";

        // 玩家/实体定位面（Entity=rj；档位三：World 无玩家直字段）
        t.playerF = null;
        t.playerListF = "d";      // et public List d=playerEntities（1401/16 同字母）
        t.entityClassG = "rj";
        t.setPosRotM = "b";       // b(DDDFF)V ✓历代同字母同形
        t.setPosRotDesc = "(DDDFF)V";
        t.posXF = "aK";           // ⚠️ 左移两位 vs 173 aM..（NBT "Pos" 配对）
        t.posYF = "aL";
        t.posZF = "aM";
        t.motXF = "aN";           // NBT "Motion" 配对
        t.motYF = "aO";
        t.motZF = "aP";
        t.yawF = "aQ";            // NBT "Rotation" 配对
        t.pitchF = "aR";
        t.healthF = "W";          // kw.W public int（⚠️173=Y/1401=V/16=Y 洗牌链）
        t.iframesF = null;        // kw 无 heartsLife（⚠️rj.by=skinUrl 撞车位勿混）
                                  // →invulnerable UNSUPPORTED

        // HUD 面（GuiIngame=tl；门读面两点 @117 jz.A=thirdPersonView/@1142
        // jz.B=showDebugInfo——偏移与 173 逐位同形；主类 h/p/y 字母同 173）
        t.hudPlayerF = "h";
        t.hudFontF = "p";         // ⚠️173=q 断代（fontRenderer=p(Lrf)）
        t.hudSettingsF = "y";     // gameSettings=y(Ljz)
        t.hudDebugGateF = "B";    // jz.B=showDebugInfo（非 173 kv.B=showFPS 语义——
                                  // 同字母异名陷阱在案）
        t.hudXyzPaint = Boolean.FALSE; // a1.2.3 边界线（§5.1）：F3 自带坐标=注入免做

        // 物品栏面（EntityPlayer=gh.f→ih；⚠️ invF=f 同 1401/173=c 断代）
        t.invF = "f";
        t.invMainF = "a";         // ih.a=mainInventory[ii]
        t.invArmorF = "b";
        t.invCurF = "c";          // ⚠️ ih.d=player 回指非 cur（1401/16 同形陷阱）
        t.isCountF = "a";         // ii a=stackSize/c=itemID public、d=itemDamage private
        t.isIdF = "c";
        t.isDmgF = "d";
        t.addStackM = "a";        // ih.a(ii)Z addItemStackToInventory ✓
        t.itemClassG = "gb";      // ⚠️ 撞车位：1401 gb=EntityItem↔1501 gb=Item
        t.itemsF = "c";           // public static gb[] c ✓
        t.itemEntityG = "gz";     // EntityItem (et,DDD,ii)V（ii 无 (II)V——newStack (III)V）

        // 实体/世界事件面（⚠️ entityJoinedWorld/setEntityDead/entityList 三件
        // 与 173 同字母——1401 a/d/n 断代带止于 1501，16 亦 b/e/o 同形）
        t.explodeM = "a";         // a(Lrj;DDDF)Lpx;（返回值不消费）
        t.spawnEntityM = "b";     // b(Lrj;)Z entityJoinedWorld
        t.setEntityDeadM = "e";   // e(Lrj;)V
        t.entityListM = "o";      // o()List getLoadedEntityList
        t.entityTypeM = null;     // per-instance 命名面未钉（同族判决）
        t.entityListClassG = "ik"; // ⚠️ 撞车位：1401 ik=…↔1501 ik=EntityList
        t.entityListLookupM = "b"; // ik.b(rj)String static ✓同字母
        t.playSoundM = "a";       // a(DDD,String,F,F)V ✓

        // getinfo 面
        t.difficultyF = "l";      // et.l=difficultySetting（⚠️1401=j/16=q/173=q 洗牌）

        // D8 天气面（判决②首例版：et 四浮值 protected——B/D=prev 拷贝槽、
        // C=rainStrength/E=thunderStrength（⑰批 javap -c 裁决，getter 错位自食销案）；
        // dy 旗标 setter b(Z)V→l:Z=raining/a(Z)V→n:Z=thundering 全 public
        // （四版 dy/ei/rl/sx 同形）——T1 双写机制直接适用）
        t.capWeather = Boolean.TRUE;
        t.rainStrF = "C";         // et.C=rainingStrength protected float
        t.thunderStrF = "E";      // et.E=thunderingStrength protected float
        t.wiSetRainM = "b";       // dy.b(Z)V setIsRaining
        t.wiSetThunderM = "a";    // dy.a(Z)V setIsThundering

        // 未钉面（同族判决：light6/observers 三件套未钉→setmeta 静默面走
        // setMetaSilentM；genscatter 散布器未钉）
        t.light6M = null;
        t.observersF = null;
        t.scatterClassG = null;

        // TE 容器面（⑲批真 jar javap 对榫：la=IInventory a()I/d_(I)Lii/a(ILii)V；
        // rg=TileEntityFurnace public a/c+private ii[] h；iy=TileEntityChest
        // private ii[] a 27 槽——a 撞数组字段形，getFurnace int 门天然区分）
        t.invSizeM = "a";
        t.invGetM = "d_";         // ⚠️1401=c_ 断代（尾下划线链 1301/1401 c_→1501/16 d_）
        t.invSetM = "a";
        t.teBurnF = "a";          // rg.a=furnaceBurnTime public int
        t.teCookF = "c";          // rg.c=furnaceCookTime public int

        // h10 批 sleep 面（⑲批钉面：gh.b(III)Lco;=sleepInBedAt 四门 e→b→c→d
        // （gh_disasm 实锤：O()Z||!W()Z→co.e/et.o:Lvr;.c:Z→co.b/et.f()Z→co.c/
        // 距 aK/aL/aM 3/2/3→co.d；OK 尾 x=true(Sleeping protected)/b=0/
        // a:Lbk;=new bk/动量 aN-aP 清零/!et.v→et.y()V/返 co.a）、
        // gh.a(ZZZ)V=wakeUpPlayer（a:Lbk; ifnull @24 null-safe）。
        // ⚠️ tick NPE 红线：gh.am()Z 裸解引用 a:Lbk;——T2 裸字段配方退役，
        // 写面恒走 b(III) API（173 崩溃活体实锤同构，FakeOps1501Test 假件复刻）。
        // updateAllPlayersSleepingFlag=et.y()V（1401=x 断代；173/16=y 同）。
        t.capSleep = Boolean.TRUE;
        t.sleepInBedM = "b";
        t.wakeUpM = "a";
        t.sleepOkF = "a";         // co.a=EnumStatus OK（1401 cb/16 cu 同字母 a）

        // grow 面（骨粉 id 未钉——同 173 惯例 UNSUPPORTED）
        t.idCrops = null;
        t.idSapling = null;
        t.cropsRipeMeta = null;

        // biome 面（判决群系首例版：三锚链 et.a()Lwl→wl.a(II)Ljj→jj.m:String
        // 全 public；⚠️ biomeNameF=m——173=n 断代，16=n 亦漂移）
        t.biomeWcmM = "a";        // et.a()Lwl; getWorldChunkManager
        t.biomeAtM = "a";         // wl.a(II)Ljj; getBiomeGenAt ✓
        t.biomeNameF = "m";       // jj.m=biomeName public String

        // spawn 白名单（EntityList=ik 注册表+tiny 直钉，ctor (et)V 全中；
        // tnt=pw (et,DDD)V DDD 型同 173 fallback；giant=mx 构造健在；
        // mob=ht=EntityCreature public 非 abstract（181 abstract 断代未到））
        t.mobClasses.put("pig", "va");
        t.mobClasses.put("sheep", "dd");
        t.mobClasses.put("creeper", "fq");
        t.mobClasses.put("skeleton", "fg");
        t.mobClasses.put("zombie", "ts");
        t.mobClasses.put("spider", "cg");
        t.mobClasses.put("tnt", "pw");
        t.mobClasses.put("giant", "mx");
        t.mobClasses.put("mob", "ht");

        return t;
    }

    /**
     * b1.4_01 表（Beta 1.4_01，睡眠面在位但天气/群系双 UNSUPPORTED 的下界锚版；
     * 活体批转正 2026-11-28，仿 build1501 体例）。值源=锚库
     * java/agent/registry/fingerprints/b1.4_01.json（mca-fingerprint-v1 草稿锚，
     * A2Table1401Test 逐槽对榫在案）；取证=docs/compatibility/verscan/b1.4_01.md
     * + %TEMP%\mcaut\exp\b1401\（javap 全量 9903 行+eb_c/mc_c/qv_c/ob_c 定向
     * -c+client.tiny 三级直钉；件 SHA1 6f157f26… 对榫版本索引行 L360）。
     * 四大边界判决入表：①天气下界自证（eb 无 B/C/D/E 浮值+dj 无天气键）
     * →capWeather=FALSE 四 null 槽；②群系下界=b1.5_01（源树零 BiomeGen*，
     * tq=前群系气候件非群系链）→biome 三 null 槽；③GiantZombie 未到世
     * →mobClasses.giant=null；④worldTime=dj e:J 五版同形（W-3 复验补记）。
     * vs 1501 断代重点（严禁借表）：World=eb/Entity=pb/EntityLiving=jm/
     * EntityPlayer=fm/WorldInfo=dj（取用器 w——同形陷阱勿抄 1501 x）；
     * pos 面左移一位 aJ..aQ（+1/版规律 1301=aI..→1401=aJ..→1501=aK..）；
     * entityJoinedWorld=a/setEntityDead=d/entityList=n（1501 b/e/o 断代带——
     * 16 回摆 1501 字母，1401 独异）；getWorldInfoM=w（1501=x/16=x）；
     * invGetM=c_（1301/1401 c_→1501/16 d_ 尾下划线链）；hudDebugGateF=A
     * （is.A=showDebugInfo——1501 jz.B 断代，W-7 同字母同义链在 1401 翻面）；
     * health=jm V（1501=W/173=Y）；difficulty=eb j（1501=l/16=q）。
     * 睡眠面（边界批 W-9+tiny 终核）：fm 三槽 a:Z/c:I/b:Lay; private
     * （与 1501 x/b/a 全不同）；四门序同 1501 形 e→b→c→d；EnumStatus cb
     * a=OK；isPlayerSleeping=M()Z（⚠️边界批 §2 勘误：N()Z=isPlayerFullyAsleep，
     * isEntityAlive=pb.U()Z）。saveWorld 断代带延续：a(ZLtw;)V 双参实体无包装
     * （tw null 守卫 @14/@30 同 1501 位形；a(Z)Z=TickUpdates 禁钩预避）；
     * saveLevel=A()V（1501=D 断代）。RUN_TICK=j（173=k 断代同 1501 字母，
     * eb.l:()V 调用点 @1209 实锤）。撞车位实录入注：rj/gb/fg/iy/rg/jj 六件
     * 跨代异义——严禁跨表借字母。
     */
    private static VersionTable build1401() {
        VersionTable t = new VersionTable();
        t.versionId = "b1.4_01";
        t.blockClassG = "ra"; // Block 类（static ra[] 字段=m，87 实例字段，姊妹仓交叉对榫）
        t.family = "G3"; // 扁平散件+未混淆主类（G3-beta，1501/173 同族）
        t.jarSha1 = "6f157f26955c35006c1afa8b0479e0ce785fb864"; // 对榫版本索引 L360

        // World 写读面（eb；b1.4_01.md §2 javap+tiny 实锤）
        t.setBlockM = "e";        // e(IIII)Z setBlockWithNotify（1501 同字母；16=f 断代）
        t.getBlockM = "a";        // a(III)I ✓历代同字母
        t.getMetaM = "e";         // e(III)I getBlockMetadata（同字母异描述符共存）
        t.setMetaSilentM = "d";   // d(IIII)Z setBlockMetadata（⚠️本代返回 boolean=
                                  // 源 L391；1501 d(IIII)V 断代——WorldOps getMethod
                                  // 名+四 int 形解析不依赖返回型，setmeta OK 判决成立）
        t.getLightM = "l";        // l(III)I getBlockLightValue（1501 同字母；16=n 断代）
        t.teGetM = "b";           // b(III)Lma;（TileEntity=ma）
        t.saveWorldM = "a";       // a(ZLtw;)V 双参实体——无 1 参 public 包装
        t.saveWorldDesc = "(ZLtw;)V"; // （断代带与 1501 同形：tw null 守卫 @14/@30
                                  // 两处齐全；a(Z)Z=TickUpdates 禁钩同形预避；
                                  // saveLevel=A()V 1501=D 断代经 sr 落 level.dat）
        // WorldInfo 架构（dj 承载五 private 槽；⚠️ 取用器 w——同形陷阱勿抄 1501 x）
        t.getWorldInfoM = "w";    // eb.w()Ldj; getWorldInfo（1501=x/16=x 断代）
        t.worldTimeF = "e";       // dj.e:J=worldTime（NBT "Time"，判决④五版同形）
        t.randSeedF = "a";        // dj.a:J=randomSeed（NBT "RandomSeed"）
        t.spawnXF = "b";          // dj.b/c/d:I=SpawnX/Y/Z（tiny 命名全中）
        t.spawnYF = "c";
        t.spawnZF = "d";

        // 玩家/实体定位面（Entity=pb；档位三：World 无玩家直字段）
        t.playerF = null;
        t.playerListF = "d";      // eb public List d=playerEntities（1501/16 同字母）
        t.entityClassG = "pb";
        t.setPosRotM = "b";       // b(DDDFF)V ✓历代同字母同形
        t.setPosRotDesc = "(DDDFF)V";
        t.posXF = "aJ";           // ⚠️ 左移一位 vs 1501 aK..（+1/版规律 1301=aI..
        t.posYF = "aK";           //   →1401=aJ..→1501=aK..→16=aL..→173=aM.. 严格成立）
        t.posZF = "aL";
        t.motXF = "aM";
        t.motYF = "aN";
        t.motZF = "aO";
        t.yawF = "aP";
        t.pitchF = "aQ";
        t.healthF = "V";          // jm.V public int（1501=W/173=Y 洗牌链）
        t.iframesF = null;        // jm 无 heartsLife（同 1501 惯例）→invulnerable UNSUPPORTED

        // HUD 面（GuiIngame=qv；门读面两点 @117 is.z=thirdPersonView/@1142
        // is.A=showDebugInfo——偏移与 1501 tl 逐位同形；主类 h/p/y 字母同 1501）
        t.hudPlayerF = "h";
        t.hudFontF = "p";         // fontRenderer=p(ox) ✓同 1501
        t.hudSettingsF = "y";     // gameSettings=y(is) ✓
        t.hudDebugGateF = "A";    // ⚠️ is.A=showDebugInfo——1501 jz.B 断代
                                  // （W-7 同字母同义链在 1401 翻面，勿抄 1501 B）
        t.hudXyzPaint = Boolean.FALSE; // a1.2.3 边界线：F3 自带坐标=注入免做

        // 物品栏面（EntityPlayer=fm.f→hh；invF=f 同 1501/173）
        t.invF = "f";
        t.invMainF = "a";         // hh.a=mainInventory[hi]（四字母全同 1501）
        t.invArmorF = "b";
        t.invCurF = "c";          // ⚠️ hh.d=player 回指非 cur（同 1501 陷阱）
        t.isCountF = "a";         // hi a=stackSize/c=itemID public、d=itemDamage private
        t.isIdF = "c";
        t.isDmgF = "d";
        t.addStackM = "a";        // hh.a(hi)Z addItemStackToInventory ✓
        t.itemClassG = "fg";      // ⚠️ 撞车位：1401 fg=Item↔1501 fg=Skeleton
        t.itemsF = "c";           // public static fg[] c ✓
        t.itemEntityG = "gb";     // ⚠️ 撞车位：1401 gb=EntityItem↔1501 gb=Item
                                  // EntityItem (eb,DDD,hi)V（hi 仅 (III)V 无 (II)V 同形）

        // 实体/世界事件面（⚠️ 三件独异断代带：entityJoinedWorld=a/
        // setEntityDead=d/entityList=n——1501=b/e/o、16 回摆 1501 字母，1401 全不同）
        t.explodeM = "a";         // a(Lpb;DDDF)Lnq;（返回值不消费）
        t.spawnEntityM = "a";     // a(Lpb;)Z entityJoinedWorld（1501=b 断代）
        t.setEntityDeadM = "d";   // d(Lpb;)V（1501=e 断代）
        t.entityListM = "n";      // n()List getLoadedEntityList（1501=o 断代）
        t.entityTypeM = null;     // per-instance 命名面未钉（同族判决）
        t.entityListClassG = "hj"; // ⚠️ 撞车位：1401 hj=EntityList↔1501 hj=Render 系
        t.entityListLookupM = "b"; // hj.b(pb)String static ✓同字母
        t.playSoundM = "a";       // a(DDD,String,F,F)V ✓

        // getinfo 面
        t.difficultyF = "j";      // eb.j=difficultySetting（1501=l/16=q 洗牌链）

        // D8 天气面（判决①下界自证 UNSUPPORTED）：eb 无 B/C/D/E 浮值字段+
        // dj 无 raining/rainTime/thundering/thunderTime 键（b1.5_01.md 首例判决
        // 负半轴实锤）——capWeather=FALSE，四槽 null 响亮。
        t.capWeather = Boolean.FALSE;
        t.rainStrF = null;
        t.thunderStrF = null;
        t.wiSetRainM = null;
        t.wiSetThunderM = null;

        // 未钉面（同族判决：light6/observers 三件套未钉→setmeta 静默面走
        // setMetaSilentM；genscatter 散布器未钉）
        t.light6M = null;
        t.observersF = null;
        t.scatterClassG = null;

        // TE 容器面（真 jar javap+tiny 对榫：jp=IInventory a()I/c_(I)Lhi/a(ILhi)V；
        // ⚠️ 取件字母 c_（1301/1401 c_→1501/16 d_ 尾下划线链，严禁按 1501 抄 d_）；
        // oy=TileEntityFurnace public a/c+private hi[] h（与 1501 rg 全同字母）；
        // hx=TileEntityChest private hi[] a 27 槽——hx 无 public int a→
        // getFurnace int 类型门天然区分同形）
        t.invSizeM = "a";
        t.invGetM = "c_";
        t.invSetM = "a";
        t.teBurnF = "a";          // oy.a=furnaceBurnTime public int
        t.teCookF = "c";          // oy.c=furnaceCookTime public int

        // h10 批 sleep 面（边界批 W-9+tiny 终核：fm.b(III)Lcb;=sleepInBedAt
        // 四门 e→b→c→d（门1 !M()Z&&U()Z 源 L513/门2 sy.c:Z provider 旗/
        // 门3 eb.f()Z 白天/门4 距离 aJ/aK/aL 3/2/3；OK 尾 a=true(Sleeping
        // private)/c=0/b=new ay(x,y,z)/动量 aM-aO 清零/!eb.t→eb.x()V/返 cb.a）。
        // fm 三槽 a:Z/c:I/b:Lay; 全 private（与 1501 x/b/a 三字母全不同）。
        // ⚠️ 边界批 §2 勘误：fm.N()Z=isPlayerFullyAsleep（非 isEntityAlive——
        // isEntityAlive=pb.U()Z，1501=rj.W()Z）；isPlayerSleeping=M()Z。
        // wakeUpPlayer=a(ZZZ)V（b:Lay; ifnull null-safe 同 1501 @24 位形）。
        // tick NPE 红线：eb.x()V 黎明段裸解引用 b:Lay;——写面恒走 b(III) API
        // （FakeOps1401Test 假件复刻在案）。updateAllPlayersSleepingFlag=
        // eb.x()V（1501=y 断代）。
        t.capSleep = Boolean.TRUE;
        t.sleepInBedM = "b";
        t.wakeUpM = "a";
        t.sleepOkF = "a";         // cb.a=EnumStatus OK（1501 co/16 cu 同字母 a）

        // grow 面（骨粉 id 未钉——同 1501 惯例 UNSUPPORTED）
        t.idCrops = null;
        t.idSapling = null;
        t.cropsRipeMeta = null;

        // biome 面（判决②群系下界未到世 UNSUPPORTED：源树零 BiomeGen* 文件，
        // tq=WorldChunkManager 为前群系气温/湿度气候件 a(II)Lig;=MobSpawnerBase
        // 返回型系 retromcp 桩名错配，非群系链——三槽 null 响亮）
        t.biomeWcmM = null;
        t.biomeAtM = null;
        t.biomeNameF = null;

        // spawn 白名单（EntityList=hj 注册表+tiny 直钉，ctor (eb)V 全中；
        // tnt=np (eb,DDD)V 同 1501 型；⚠️ 判决③ giant=null 未到世（tiny 零命中）；
        // mob=gv=EntityCreature public 非 abstract（181 abstract 断代未到））
        t.mobClasses.put("pig", "si");
        t.mobClasses.put("sheep", "cq");
        t.mobClasses.put("creeper", "ex");
        t.mobClasses.put("skeleton", "eo");
        t.mobClasses.put("zombie", "re");
        t.mobClasses.put("spider", "bt");
        t.mobClasses.put("tnt", "np");
        t.mobClasses.put("giant", null); // 判决③：GiantZombie 未到世——require 响亮
        t.mobClasses.put("mob", "gv");

        return t;
    }

    /**
     * b1.6 表（Beta 1.6，天气/群系/giant 三判决全翻面在位+RUN_TICK j→k 翻面
     * 下界版；转正批 2026-11-28，仿 build1401 体例）。值源=锚库
     * java/agent/registry/fingerprints/b1.6.json（mca-fingerprint-v1 草稿锚，
     * A2Table16Test 逐槽对榫在案）；取证=%TEMP%\mcaut\exp\b16\（jar SHA1
     * ecc0288d… 对榫版本索引行 L363；javap 全量 9465 行+gq_c/fb_c/mc_c/uk_c
     * 定向 -c+sister 仓 client.tiny 类/方法级直钉）。⚠️⚠️ 头号警示：sister 仓
     * b1.6 client.tiny 字段面 ≠ .betacraft 真 jar——Entity 系错位一位（tiny
     * worldObj=aI/posX=aM ↔ 真 jar aH/aL，gq_c 门4 @50-84 |aL-x|>3 实锤+si 块
     * javap 复验）；本表全部字段字母以真 jar javap/反汇编为准，类字母面 tiny
     * 与真 jar 一致。类树：World=fb/Entity=si/EntityLiving=lo/EntityPlayer=gq/
     * EntityPlayerSP=da/WorldInfo=eg/GuiIngame=uk/IProgressUpdate=xs/
     * IInventory=ls/TileEntity=os。vs 1401/1501 断代重点（严禁借表）：pos
     * aL..aS（+1/版规律 1301=aI..→1401=aJ..→1501=aK..→16=aL..→173=aM..
     * 严格成立）；entityJoinedWorld=b/setEntityDead=e/entityList=o 三件全回摆
     * 1501 字母（1401 a/d/n 断代带止于 1501）；getWorldInfoM=x（1401=w 断代）；
     * invGetM=d_（1401 c_ 断代带止于 1401，严禁借表）；hudDebugGateF=B
     * （kr.B=showDebugInfo @1142——1401=A 翻面）；healthF=X（lo public int——
     * 活体勘误件，173=Y 非同字母；1401=V/1501=W 洗牌链 +1/版严格成立）；difficultyF=q（1401=j/1501=l 再洗牌）；
     * setBlockM=f/getLightM=n（1401/1501=e/l 断代）；setMetaSilentM=e(IIII)Z
     * （1501 d(IIII)V 断代回摆 boolean，与 getMetaM=e(III)I 同字母异描述符共存；
     * WorldOps 名+形解析不依赖返回型，setmeta OK 判决成立）。主类六字段全洗牌
     * （vs 1401/1501 弱断代在 16 翻脸）：theWorld=f（1401/1501=e）/thePlayer=
     * h:da（声明类型=EntityPlayerSP——1401/1501 声明 EntityLiving 断代）/
     * ingameGUI=v/gameSettings=z/fontRenderer=q/mcDataDir=Z。saveWorld 断代带
     * 延续：a(ZLxs;)V 双参实体无 1 参包装（xs null 守卫 @14/@30 与 1401 逐位
     * 同形；a(Z)Z=TickUpdates 禁钩同形预避；saveLevel=D()V private——1501
     * public D 访问位翻面注）。三边界判决全翻面（vs 1401）：①capWeather=TRUE
     * ——fb protected float j/l（fb.E()V 反汇编语义钉：eg.o()Z=getRaining→j/
     * eg.m()Z=getThundering→l，prev=i/k @242-245 lerp）+eg 天气键 l/n 在位+
     * setter b(Z)V/a(Z)V（1501 dy 同字母）；②biome 三锚 a/a/n 在位（fb.a()
     * Lxm;→xm.a(II)Ljz;→jz.n=biomeName——1501 wl/jj/m 字母漂移，判决②翻面）；
     * ③mobClasses.giant=np 已到世（EntityGiantZombie extends gx=EntityMob
     * concrete——1401 null/1501=mx 断代链，判决③翻面）。睡眠面（gq_c @0-329
     * 反汇编全套）：三槽 u protected:Z/b private:I/a private:Lbp;（⚠️tiny
     * u/a/v 错位件警示——真 jar 为准）；四门序 e→b→c→d 三版同形；OK 尾
     * u=true/b=0/a=new bp(x,y,z)/动量 aO-aQ 清零/!fb.B→fb.y()V/返 cu.a；
     * isPlayerSleeping=N()Z（1501=O 断代）/isEntityAlive=si.W()Z（1501 同
     * 字母）；wakeUpPlayer=gq.a(ZZZ)V（@24 a:Lbp; ifnull null-safe）；tick
     * NPE 红线 am()Z 裸解引用 a:Lbp;（写面恒走 b(III) API，FakeOps16Test 假件
     * 复刻在案）；remote 旁路=fb.B:Z public（链 1401 t→1501 v→16 B→173 B，
     * 门位三版同构=仅 OK 尾/wake/tick 黎明段）。RUN_TICK=k()V（⚠️1401/1501=
     * j 断代——j→k 翻面下界提前至 b1.6 实锤，173=k 同字母；fb.l()V 调用点
     * mc_c @1260）。撞车位实录（严禁跨表借字母）：16 hj=EntityItem↔1501
     * hj=EntityList；16 iz=EntityList↔1401 iz=Render 系；16 np=EntityGiant
     * Zombie↔1401 np=TNTPrimed；16 gq=EntityPlayer↔1501 gq=Item；16 dj=
     * EntitySheep↔1401 dj=WorldInfo；16 fp=EntitySkeleton↔1501 fp=ItemStack；
     * 16 cl=EntitySpider↔181 cl=ItemStack；16 sf=TileEntityFurnace↔1401
     * sf=Hell。
     */
    private static VersionTable build16() {
        VersionTable t = new VersionTable();
        t.versionId = "b1.6";
        t.blockClassG = "un"; // Block 类（static un[] 字段=m，86 实例字段；⚠️ 撞车：gk 在 0629=Block 在本代=Item，逐代钉死禁复用）
        t.family = "G3"; // 扁平散件+未混淆主类（G3-beta，1501/1401/173 同族）
        t.jarSha1 = "ecc0288d218fd7479027a17c150cbf283fa950a1"; // 对榫版本索引 L363

        // World 写读面（fb；b16 javap+tiny 实锤）
        t.setBlockM = "f";        // f(IIII)Z setBlockWithNotify（⚠️1401/1501=e 断代）
        t.getBlockM = "a";        // a(III)I ✓历代同字母
        t.getMetaM = "e";         // e(III)I getBlockMetadata（同字母异描述符共存）
        t.setMetaSilentM = "e";   // e(IIII)Z setBlockMetadata（⚠️1501 d(IIII)V 断代回摆
                                  // boolean 同 1401 形；d(IIII)V=setBlockMetadataWithNotify
                                  // 不消费；WorldOps 名+形解析不依赖返回型）
        t.getLightM = "n";        // n(III)I（⚠️1401/1501=l 断代）
        t.teGetM = "b";           // b(III)Los;（TileEntity=os——1401 ma/1501 nx 断代）
        t.saveWorldM = "a";       // a(ZLxs;)V 双参实体——⚠️ 无 1 参 public 包装
        t.saveWorldDesc = "(ZLxs;)V"; // （断代带 b1.4→b1.6 延续：xs null 守卫 @14/@30
                                  // 与 1401 逐位同形；前置 v:Lcj; 旗；a(Z)Z=
                                  // TickUpdates 禁钩同形预避；saveLevel=D()V private）
        // WorldInfo 架构（eg 承载五 private 槽；取用器 x 同 1501——1401=w 断代）
        t.getWorldInfoM = "x";    // fb.x()Leg; getWorldInfo（1401=w/1501=x）
        t.worldTimeF = "e";       // eg.e:J=worldTime（NBT "Time"，五版同形链）
        t.randSeedF = "a";        // eg.a:J=randomSeed（NBT "RandomSeed"）
        t.spawnXF = "b";          // eg.b/c/d:I=SpawnX/Y/Z（五版同形链）
        t.spawnYF = "c";
        t.spawnZF = "d";

        // 玩家/实体定位面（Entity=si；档位三：World 无玩家直字段）
        t.playerF = null;         // fb 无玩家直字段（同族判决）
        t.playerListF = "d";      // fb public List d=playerEntities（三版稳定）
        t.entityClassG = "si";
        t.setPosRotM = "b";       // b(DDDFF)V ✓历代同字母同形
        t.setPosRotDesc = "(DDDFF)V";
        t.posXF = "aL";           // ⚠️真 jar 实锤 aL（sister tiny 错位一位 aM 勿借——
        t.posYF = "aM";           //   gq_c 门4 |aL-x|>3 实锤；+1/版规律 1301=aI..→
        t.posZF = "aN";           //   1401=aJ..→1501=aK..→16=aL..→173=aM.. 严格成立）
        t.motXF = "aO";
        t.motYF = "aP";
        t.motZF = "aQ";
        t.yawF = "aR";
        t.pitchF = "aS";
        t.healthF = "X";          // lo.X public int（⚠️活体勘误批 2026-11-28：原钉 Y 系
        // 抄 173 字母违 +1/版规律——1401=V/1501=W/16=X/173=Y。三重实锤：
        // NBT 写读双向 b(nq)/a(nq) ldc "Health"↔getfield/putfield X:I +
        // 构造器 bipush 10→X 保底 + heal clamp 20；活体反证=sethealth 写 Y
        // 心形 HUD 不动。Y=hurt 快照备份（attackEntityFrom @90 getfield X→
        // putfield Y），非 health 本尊）
        t.iframesF = null;        // 全 tiny 零 heartsLife 命中→invulnerable UNSUPPORTED
                                  // （同 1401/1501 惯例）

        // HUD 面（GuiIngame=uk；门读面两点 @117 kr.A=thirdPersonView/@1142
        // kr.B=showDebugInfo——偏移与 1401/1501 逐位同形；主类字段全洗牌见头注）
        t.hudPlayerF = "h";       // thePlayer=h:da（声明类型 EntityPlayerSP 断代）
        t.hudFontF = "q";         // fontRenderer=q(se)（1401/1501=p 断代）
        t.hudSettingsF = "z";     // gameSettings=z(kr)（1401/1501=y 断代）
        t.hudDebugGateF = "B";    // kr.B=showDebugInfo（1401 is.A 断代；1501 同侧 B）
        t.hudXyzPaint = Boolean.FALSE; // a1.2.3 边界线：F3 自带坐标=注入免做

        // 物品栏面（EntityPlayer=gq.c→iu；⚠️ invF=c——1401/1501=f 断代回摆 173 形）
        t.invF = "c";
        t.invMainF = "a";         // iu.a=mainInventory[36]（四字母三版全稳定）
        t.invArmorF = "b";
        t.invCurF = "c";          // ⚠️ iu.d=player 回指非 cur（1401/1501 同形陷阱）
        t.isCountF = "a";         // iw a=stackSize/c=itemID public、d=itemDamage private
        t.isIdF = "c";
        t.isDmgF = "d";
        t.addStackM = "a";        // iu.a(iw)Z addItemStackToInventory public ✓
        t.itemClassG = "gk";      // Item=gk（⚠️撞车位链，头注实录）
        t.itemsF = "c";           // public static gk[] c ✓
        t.itemEntityG = "hj";     // ⚠️ 撞车位：16 hj=EntityItem↔1501 hj=EntityList；
                                  // EntityItem (fb,DDD,iw)V（iw 仅 (III)V 无 (II)V——
                                  // javap 实锤，newStack dmg=0 等价配方前提）

        // 实体/世界事件面（⚠️ 三件全回摆 1501 字母：entityJoinedWorld=b/
        // setEntityDead=e/entityList=o——1401 a/d/n 断代带止于 1501）
        t.explodeM = "a";         // a(Lsi;DDDF)Lqs;（返回值不消费）
        t.spawnEntityM = "b";     // b(Lsi;)Z entityJoinedWorld
        t.setEntityDeadM = "e";   // e(Lsi;)V
        t.entityListM = "o";      // o()List getLoadedEntityList
        t.entityTypeM = null;     // per-instance 命名面未钉（同族判决）
        t.entityListClassG = "iz"; // ⚠️ 撞车位：16 iz=EntityList↔1401 iz=Render 系
        t.entityListLookupM = "b"; // iz.b(si)String static ✓字母 b 三版稳定
        t.playSoundM = "a";       // a(DDD,String,F,F)V ✓

        // getinfo 面
        t.difficultyF = "q";      // fb.q=difficultySetting（⚠️1401=j/1501=l 再洗牌）

        // D8 天气面（判决①翻面 OK 世代：fb protected float j/l——fb.E()V 反汇编
        // 语义钉死 eg.o()Z→j/eg.m()Z→l，prev=i/k；eg 天气键 l/n+setter b/a 在位，
        // 1501 dy 同字母——T1 双写机制直接适用）
        t.capWeather = Boolean.TRUE;
        t.rainStrF = "j";         // fb.j=rainingStrength protected float
        t.thunderStrF = "l";      // fb.l=thunderingStrength protected float
        t.wiSetRainM = "b";       // eg.b(Z)V setIsRaining
        t.wiSetThunderM = "a";    // eg.a(Z)V setIsThundering

        // 未钉面（同族判决：light6/observers 三件套未钉→setmeta 静默面走
        // setMetaSilentM；genscatter 散布器未钉）
        t.light6M = null;
        t.observersF = null;
        t.scatterClassG = null;

        // TE 容器面（真 jar javap+tiny 对榫：ls=IInventory a()I/d_(I)Liw;/a(ILiw;)V；
        // ⚠️ 取件字母 d_ 同 1501——1401 c_ 断代带止于 1401，严禁按 1401 抄 c_；
        // sf=TileEntityFurnace public a/c/b+private iw[] h 3 槽（三版同字母）；
        // jo=TileEntityChest private iw[] a 27 槽——无 public int a→getFurnace
        // int 类型门天然区分同形）
        t.invSizeM = "a";
        t.invGetM = "d_";
        t.invSetM = "a";
        t.teBurnF = "a";          // sf.a=furnaceBurnTime public int
        t.teCookF = "c";          // sf.c=furnaceCookTime public int

        // sleep 面（gq_c @0-329 反汇编全套：gq.b(III)Lcu;=sleepInBedAt 四门
        // e→b→c→d（门1 @1 N()Z||!W()Z→cu.e——isPlayerSleeping=N 1501=O 断代、
        // isEntityAlive=si.W()Z 1501 同字母；门2 @22 aH:fb→fb.t:wr.c:Z→cu.b；
        // 门3 @39 fb.f()Z→cu.c；门4 @50/67/84 |aL-x|>3|||aM-y|>2|||aN-z|>3→cu.d；
        // OK 尾 @272 u=true/@277 b=0/@291 a=new bp(x,y,z)/@298-306 动量清零/
        // @313 !fb.B:Z→fb.y()V/返 cu.a——门序三版同形判决复验通过）。
        // 三槽 u protected:Z/b private:I/a private:Lbp;（⚠️tiny u/a/v 错位件——
        // 真 jar javap 为准）。wakeUpPlayer=gq.a(ZZZ)V（@24 a:Lbp; ifnull
        // null-safe/@204 b=p1?0:100 偏移与 1401 逐位同形）。⚠️ tick NPE 红线：
        // w_() @33 invokespecial am()Z 裸解引用 a:Lbp;——写面恒走 b(III) API
        // （FakeOps16Test 假件复刻在案）。updateAllPlayersSleepingFlag=fb.y()V
        // （1401=x 断代、1501 同字母）。
        t.capSleep = Boolean.TRUE;
        t.sleepInBedM = "b";
        t.wakeUpM = "a";
        t.sleepOkF = "a";         // cu.a=EnumStatus OK（1401 cb/1501 co 同字母 a）

        // grow 面（骨粉 id 未钉——同 1501 惯例 UNSUPPORTED）
        t.idCrops = null;
        t.idSapling = null;
        t.cropsRipeMeta = null;

        // biome 面（判决②翻面 OK 世代：三锚链 fb.a()Lxm;→xm.a(II)Ljz;→jz.n:
        // String 全在位；⚠️ biomeNameF=n——1501 jj.m 字母漂移；BiomeGenHell=s）
        t.biomeWcmM = "a";        // fb.a()Lxm; getWorldChunkManager（与 a(III)I 共存）
        t.biomeAtM = "a";         // xm.a(II)Ljz; getBiomeGenAt（1501 wl.a 同形）
        t.biomeNameF = "n";       // jz.n=biomeName public String（javap 复验）

        // spawn 白名单（EntityList=iz 注册表+tiny 类级直钉，ctor (Lfb;)V 全中；
        // tnt=qr（(fb)V 在位实锤，(fb,DDD)V 面待活体复验）；⚠️ 判决③翻面
        // giant=np 已到世（EntityGiantZombie extends gx=EntityMob concrete——
        // 1401 null/1501=mx 断代链）；mob=if=EntityCreature public 非 abstract
        // （extends lo；181 abstract 断代未到，同 1501 ht 语义））
        t.mobClasses.put("pig", "vz");
        t.mobClasses.put("sheep", "dj"); // ⚠️ 撞车位：16 dj=EntitySheep↔1401 dj=WorldInfo
        t.mobClasses.put("creeper", "fz");
        t.mobClasses.put("skeleton", "fp"); // ⚠️ 16 fp=EntitySkeleton↔1501 fp=ItemStack
        t.mobClasses.put("zombie", "ur");
        t.mobClasses.put("spider", "cl");    // ⚠️ 16 cl=EntitySpider↔181 cl=ItemStack
        t.mobClasses.put("tnt", "qr");       // ⚠️ 16 np=EntityGiant↔1401 np=TNT 对偶撞位
        t.mobClasses.put("giant", "np");     // 判决③翻面：GiantZombie 已到世
        t.mobClasses.put("mob", "if");

        return t;
    }

    /**
     * b1.6 家族派生表（全覆盖批产试点转正批 2026-11-28）：单源=build16() 全 76 槽
     * +9 mob+钩能力面继承，覆写 versionId/jarSha1+语义锚槽；dblShift=true 时
     * si double 组同步右移（pos aM-aO/mot aP-aR）。1.6.1-1.6.4 传
     * X/aR/aS+false=零断代继承（SlotVerify 69/69 全绿）；1.6.5/1.6.6 传
     * Y/aS/aT+**true**=断代采纳（lo int 组 D→E/S→T/X→Y/Z→aa/aa→ab/ac→ad 与
     * si aD 起整段右移：double aD,aI-aQ→aE,aJ-aR、float aR-aU→aS-aV）。
     * 证据=SlotVerify v1 语义锚（java/agent/tools/SlotVerify.java，防移位假绿
     * 两案：pitchF=aS 字母假绿唯语义腿抓到=首案；「double 组不动」字母假绿被
     * b1.6.5 活体 getplayer 循环错位戳破=二案，工具已补 pos DLOAD 1/3/5→
     * PUTFIELD D 语义腿+mot 声明序布局腿）；活体复验腿（b1.6.5 已跑）=
     * sethealth 写 Y 回读+setlook aS/aT 转向+tp 错位对榫。
     */
    private static VersionTable derive16(String versionId, String jarSha1,
            String healthF, String yawF, String pitchF, boolean dblShift) {
        VersionTable t = build16();
        t.versionId = versionId;
        t.jarSha1 = jarSha1;
        t.healthF = healthF;
        t.yawF = yawF;
        t.pitchF = pitchF;
        if (dblShift) {
            // 活体翻案腿（b1.6.5 W1.6.5 批 2026-11-28）：si aD 起整段右移 +1——
            // javap 三版对拍 double aD,aI-aQ→aE,aJ-aR（float aR-aU→aS-aV 同批
            // 采纳）；SlotVerify 原判「double 组 aL-aQ 不动」=字母存在性假绿
            // （pos/mot 缺语义腿）。活体铁证=getplayer 回读 (x,y,z) 循环错位
            // (z,x,y)+motX≡posZ；tp(-100,68,103) 后回读 (103.5,-99.5,68.0) 逐位
            // 对榫。修正=pos aM/aN/aO、mot aP/aQ/aR（posZ 后三连 double 布局腿）。
            t.posXF = "aM";
            t.posYF = "aN";
            t.posZF = "aO";
            t.motXF = "aP";
            t.motYF = "aQ";
            t.motZF = "aR";
        }
        return t;
    }

    /** b1.7 带单源派生（全覆盖批产 L-13 批 2026-11-28）：b1.7/b1.7_01/b1.7.2
     *  三版 vs 基准锚 b1.7.3=75 槽零断代（SlotVerify v2.1 全绿+派生链同形
     *  +hooks 六元组全等），仅覆写 id/sha 两键——同 b1.6.1-4 零断代案。 */
    private static VersionTable derive17(String versionId, String jarSha1) {
        VersionTable t = build173();
        t.versionId = versionId;
        t.jarSha1 = jarSha1;
        return t;
    }
}
