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

    // World 写读面
    public String setBlockM;        // setBlockWithNotify (IIII)Z
    public String getBlockM;        // getBlockId (III)I
    public String saveWorldM;       // saveWorld (Z)V
    public String worldTimeF;       // public long worldTime（NBT "Time"）
    public String randSeedF;        // private long randSeed（NBT "RandomSeed"）
    public String spawnXF;
    public String spawnYF;
    public String spawnZF;

    // 玩家/实体定位面（Entity 基类）
    public String playerF;          // World.c=活体玩家
    public String setPosRotM;       // setPositionAndRotation (DDDFF)V
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
    public String entityTypeM;      // getEntityType ()String
    public String playSoundM;       // playSoundEffect (DDD,String,F,F)V

    // 方块 meta/光面
    public String getMetaM;         // getBlockMetadata (III)I
    public String setMetaSilentM;   // setBlockMetadata 静默写 (IIII)V
    public String light6M;          // 光照 6 邻更新 (IIII)V
    public String getLightM;        // getBrightness (III)I
    public String observersF;       // World 渲染观察者 List
    public String difficultyF;      // difficultySetting int
    public String scatterClassG;    // 椭球矿石散布器类

    // tileentity 面
    public String teGetM;           // getBlockTileEntity (III)ITE
    public String invSizeM, invGetM, invSetM; // IInventory 三方法
    public String teBurnF, teCookF; // TileEntityFurnace Burn/Cook

    // 游戏 id 常量（版本相关：Classic 无 metadata 面即落此表达）
    public int idCrops;
    public int idSapling;
    public int cropsRipeMeta;

    /** spawn 白名单：kind→实体类全名（含 tnt/giant/mob 特例）。 */
    public final Map<String, String> mobClasses = new HashMap<String, String>();
}
