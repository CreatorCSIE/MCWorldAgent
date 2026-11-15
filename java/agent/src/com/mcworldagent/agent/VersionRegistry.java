package com.mcworldagent.agent;

import java.util.HashMap;
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
 * B5/F6=JSON 装载替换本硬编码工厂（锚库见 java/agent/registry/fingerprints/）。
 */
public final class VersionRegistry {

    private VersionRegistry() {
    }

    private static final VersionTable T_0327 = build0327();
    private static final VersionTable T_0618 = build0618();
    private static final VersionTable T_0629 = build0629();

    private static final Map<String, VersionTable> BY_ID = new HashMap<String, VersionTable>();
    private static final Map<String, VersionTable> BY_SHA1 = new HashMap<String, VersionTable>();

    static {
        VersionTable[] tables = { T_0327, T_0618, T_0629 };
        for (VersionTable t : tables) {
            BY_ID.put(t.versionId, t);
            BY_SHA1.put(t.jarSha1, t);
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
        t.saveWorldM = "f";       // ()V public final（candidate；与 0327 a(Z)V 不同型=档位二）
        t.saveWorldDesc = "()V";
        t.worldTimeF = "b";       // long=time
        t.randSeedF = "m";        // long=seed
        t.spawnXF = "i";          // i/j/k=spawn（⚠️ 与 teGetM=i 方法同名合法——Java 字段/方法分命名空间）
        t.spawnYF = "j";
        t.spawnZF = "k";
        t.playerF = "e";          // 声明 Entity=a.c.b，运行时 client.g.a
        t.teGetM = "i";           // (III)ITE（a.a.b.a.a 基类）

        // 实体定位面（verscan §3；pos=h/i/j ⚠️ 非 0327 g/h/i，独立钉）
        t.setPosRotM = "a";       // setPosition (DDD)V（candidate）直写 h/i/j——
                                  // setPositionAndRotation 面未钉，槽位取 tp 主入口（档位二+三）
        t.setPosRotDesc = "(DDD)V";
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
}
