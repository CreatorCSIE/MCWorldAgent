package com.mcworldagent.agent;

/**
 * 版本登记表（A1 迁移批）。当前唯一表=inf-20100327，值与迁移前 WorldOps 常量
 * 逐字段等值（无头等值断言=验收）。
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

    /** A1 单表；A2 起按捕获世界的版本指纹选表。 */
    public static VersionTable current() {
        return T_0327;
    }

    public static String currentVersionId() {
        return T_0327.versionId;
    }

    private static VersionTable build0327() {
        VersionTable t = new VersionTable();
        t.versionId = "inf-20100327";
        t.family = "G2"; // 包路径混淆 net.minecraft.a.*

        // World 写读面（FirstBrick T1 javap 实锤；worldTime=NBT "Time"，randSeed=NBT "RandomSeed"）
        t.setBlockM = "c";   // setBlockWithNotify (IIII)Z
        t.getBlockM = "a";   // getBlockId (III)I
        t.saveWorldM = "a";  // saveWorld (Z)V private
        t.worldTimeF = "b";
        t.randSeedF = "x";
        t.spawnXF = "g";     // NBT "SpawnX"（g/h/i 唯一读点=EntityPlayer 构造器）
        t.spawnYF = "h";
        t.spawnZF = "i";

        // 玩家/实体定位面（Entity 基类 net.minecraft.a.c.b；World.c=活体玩家自设）
        t.playerF = "c";
        t.setPosRotM = "b";  // setPositionAndRotation (DDDFF)V=NBT 载入同一配方
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
        t.entityTypeM = "q";     // getEntityType ()String（与 AABB 字段 q 同名合法）
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
}
