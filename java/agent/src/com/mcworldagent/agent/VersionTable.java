package com.mcworldagent.agent;

import java.util.HashMap;
import java.util.Map;

/**
 * 版本兼容契约表（案卷 version-expansion-pilot §8 + 审议 A1 判决）。
 * 一个具体 JAR 一张表：混淆实名/描述符面名/游戏 id 常量全部数据化于此，
 * WorldOps 只消费表值（A1 迁移批=零行为变化）。
 * 取证注释随值迁移在 VersionRegistry.build0327()。
 * 戒律 15：G1 Classic 是结构差异非换名差异——方法缺失/签名差异/语义差异三档
 * 建模待 A2 有第二版实测时引入（戒律 10：不做无第二用例的抽象），本类字段
 * 为 null 即"缺失"预留表达位，A1 不使用。
 */
public final class VersionTable {

    /** 版本身份（与锚库 JSON versionId 对榫）。 */
    public String versionId;
    /** 混淆世代：G1=Classic 半可读 / G2=包路径混淆 / G3=扁平散件。 */
    public String family;
    /** 原始 jar SHA1（戒律 14 索引圭臬；VersionResolver 对榫用，与锚库 JSON jarSha1 同源）。 */
    public String jarSha1;

    // World 写读面
    public String setBlockM;        // setBlockWithNotify (IIII)Z
    public String getBlockM;        // getBlockId (III)I
    public String saveWorldM;       // saveWorld（0327 (Z)V / 0618 ()V——签名漂移见 desc 字段）
    /** saveWorld 描述符（档位二·签名差异，A2；null=按 0327 (Z)V 处理）。 */
    public String saveWorldDesc;
    public String worldTimeF;       // public long worldTime（NBT "Time"）
    public String randSeedF;        // private long randSeed（NBT "RandomSeed"）
    public String spawnXF;
    public String spawnYF;
    public String spawnZF;
    /** b1.7.3 结构断代（verscan/b1.7.3.md §2）：World(fd) 不再持 time/seed/spawn 字段——
     *  本槽=WorldInfo 取用器方法名（fd.x()Lei public，零参）。非 null 时上五槽
     *  （worldTimeF/randSeedF/spawnXF/Y/Z）在 x() 返回类型上解析（ei 字段面全 private
     *  javap 实锤——getDeclaredField+setAccessible，WorldOps resolve WorldInfo 分支）；
     *  null=字段在 World 本体（0327..b1.2_02 现形态，零行为变化）。 */
    public String getWorldInfoM;

    // 玩家/实体定位面（Entity 基类）
    public String playerF;          // World.c=活体玩家
    /** 档位三（a1.1.2_01）：World 无玩家直字段——playerEntities 列表字段名（op 取 get(0) 回退）。 */
    public String playerListF;
    /** Entity 基类名（仅 playerF==null 时消费：tp/entity 面经此解析；null=走 playerF.getType() 推导）。 */
    public String entityClassG;
    public String setPosRotM;       // setPositionAndRotation（0327 (DDDFF)V / 0618 a(DDD)V——见 desc 字段）
    /** setPosRot 描述符（档位二·签名差异，A2；null=按 0327 (DDDFF)V 处理）。 */
    public String setPosRotDesc;
    public String posXF, posYF, posZF;
    public String motXF, motYF, motZF;
    public String yawF, pitchF;
    public String healthF;          // EntityLiving.W=Health
    public String iframesF;         // Entity.K=heartsLife

    // 物品栏面
    public String invF;             // EntityHuman.b（InventoryPlayer）
    public String invMainF;         // ItemStack[36]
    public String invArmorF;        // ItemStack[4]
    public String invCurF;          // 当前快捷槽
    public String isCountF, isIdF, isDmgF; // ItemStack 三字段
    public String addStackM;        // addStack (ItemStack)Z
    public String itemClassG;       // Item 注册表类（按 id 索引）
    public String itemsF;           // public static Item[] 字段
    public String itemEntityG;      // EntityItem 类名

    // 实体/世界事件面
    public String explodeM;         // createExplosion (Entity,DDDF)V
    public String spawnEntityM;     // spawnEntityInWorld (Entity)V
    public String setEntityDeadM;   // setEntityDead (Entity)V（0327 为 static）
    public String entityListM;      // getLoadedEntityList ()List
    public String entityTypeM;      // getEntityType ()String（per-instance，查表面 miss 时的 fallback）
    public String entityListClassG; // EntityList 静态查表类（实体→注册名，主命名面）
    public String entityListLookupM;// EntityList 静态 a(Entity)String（未注册实体返 null→entityTypeM fallback）
    public String playSoundM;       // playSoundEffect (DDD,String,F,F)V

    // 方块 meta/光面
    public String getMetaM;         // getBlockMetadata (III)I
    public String setMetaSilentM;   // setBlockMetadata 静默写 (IIII)V
    public String light6M;          // 光照 6 邻更新 (IIII)V
    public String getLightM;        // getBrightness (III)I
    public String observersF;       // World 渲染观察者 List
    public String difficultyF;      // difficultySetting int
    public String scatterClassG;    // 椭球矿石散布器类

    // 生物群系面（a1.2.0 首版，verscan/a1.2.0_02.md §8 读回链三锚：World.getWcm()LWcm;
    // →Wcm.biomeAt(II)LBiome;→Biome.name public String；beta 前版本（0327/0618/0629/
    // a1.1.2_01）=null——getbiome 入口 require 响亮 UNSUPPORTED：这些版本无 biome 面，
    // 非指纹欠账）
    public String biomeWcmM;        // World.getWorldChunkManager 名（无参唯一）
    public String biomeAtM;         // WCM 坐标→biome 名（(II) 双 int 参唯一重载）
    public String biomeNameF;       // Biome 名 public String 字段（子类继承可达）

    // tileentity 面
    public String teGetM;           // getBlockTileEntity (III)ITE
    public String invSizeM, invGetM, invSetM; // IInventory 三方法
    public String teBurnF, teCookF; // TileEntityFurnace Burn/Cook

    // 游戏 id 常量（版本相关：Classic 无 metadata 面即落此表达；
    // A2 档位一：Integer null=未钉，op 入口判 null→UnsupportedOp）
    public Integer idCrops;
    public Integer idSapling;
    public Integer cropsRipeMeta;

    /** spawn 白名单：kind→实体类全名（含 tnt/giant/mob 特例）。 */
    public final Map<String, String> mobClasses = new HashMap<String, String>();
}
