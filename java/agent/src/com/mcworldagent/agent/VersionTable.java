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

    // HUD 次级链面（MCAHooks.onHud 反射链字母表化，2026-11-17 清 173/181 欠账；
    // null=0327 形态 f/m/u/d——0327/0618 门=showFPS P 键调试列；Beta 独立调试列
    // 已并入 F3（姊妹仓 b1.7.3/b1.8.1 树 GuiIngame 实钉），门=showDebugInfo：
    // javap 实锤 173 MC h/q/z+kv.B、181 MC h/q/z+fv.E；drawStringWithShadow
    // a(String,III)V 全代同字母不入表）
    public String hudPlayerF;       // Minecraft.thePlayer
    public String hudFontF;         // Minecraft.fontRenderer
    public String hudSettingsF;     // Minecraft.gameSettings
    public String hudDebugGateF;    // GameSettings 调试显示布尔门
    public Boolean hudXyzPaint;     // null=绘制（<a1.2.3：0327/0618 原版无坐标面）；
                                    // false=注入免做仅观测（§5.1 a1.2.3 边界线：原版自带
                                    // 坐标显示，Beta 面=F3；链字母仍钉=锚数据非消费面）

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

    // D8 门控能力槽（案卷 §6.1 逐版权益候选表；Boolean null=未宣告=op 响亮
    // UNSUPPORTED，TRUE=已取证放行；JSON ops 段 Boolean 分支 B5 schema 原生支持）
    public Boolean capWeather;      // b1.5+（verscan b1.7.3：World 浮值面+WorldInfo 布尔键两层）
    public Boolean capGamemode;     // b1.8+（creative 旗标）——D13 实装 setgamemode，build181 放行
    public Boolean capXp;           // b1.8+（经验字段，setxp 消费）——D10 实装，build181 放行
    public Boolean capEffect;       // b1.8+（PotionEffect 挂 EntityLiving，giveeffect/removeeffect
                                    // 消费）——D10 实装，build181 放行（b1.9-pre2 同族，锚件在档）
    public String rainStrF;         // World.rainingStrength float（b1.7.3=j，verscan 在档）
    public String thunderStrF;      // World.thunderingStrength float（b1.7.3=l）
    // weather 完整语义槽（无头窗 T1 批 2026-11-26）：WorldInfo 旗标 setter 名
    // （desc=(Z)V，timeHolder 取实例 invoke）。null=旧线维持浮值面单写（181 判决
    // 不变）；pre2 首钉=sx.b(Z)V setIsRaining/sx.a(Z)V setIsThundering（public，
    // JSON note 钉；写旗标免 updateWeather 每 tick 斜坡覆写，level.dat 持久化面）。
    public String wiSetRainM;
    public String wiSetThunderM;

    // T2 sleep 面（无头窗 2026-11-26，D10 sleep/wake 条目）：b1.5+ 门控。
    // pre2 首钉（tiny 反查全链）：uo.aM=sleeping:Z / uo.a=sleepTimer:I（⚠️活体批2
    // 撞名面本尊——本槽按声明类精确解析，与 healthField 的 mob 链探针互不干扰）/
    // ti.z()V=updateAllPlayersSleepingFlag。旧线 null=op 响亮 UNSUPPORTED。
    public Boolean capSleep;
    // h10 批（活体 173 崩溃翻案）：sleep 写面=游戏 API 双方法，T2 裸字段三槽退役。
    public String sleepInBedM;      // EntityPlayer.sleepInBedAt (III)EnumStatus（173=b/181=d）
    public String wakeUpM;          // EntityPlayer.wakeUpPlayer (ZZZ)V（四版=a）
    public String sleepOkF;         // EnumStatus OK 常量字段（四版=a，成功路径 areturn 实钉）

    // D13 游戏模式面（b1.8+；setgamemode 消费。取证=src-minecraft-beta-1.8.1 树
    // WorldInfo.java 直读：field_35920_p=gameType private int，NBT "GameType" 读写配对
    // （L26 getInteger/L104 setInteger 实锤）。经 getWorldInfoM（rv.x()Lrl）取 WorldInfo
    // 实例再 getDeclaredField+setAccessible 写 private p（timeHolder 同路径）。
    // ⚠️ 语义=level.dat 持久层（重进世界生效）；会话内即时飞行/破坏联动需玩家能力面
    // （b1.8 无独立 capabilities 对象，1.3+ 才有）=到场包活体判决，勿当即时 op。
    public String gameTypeF;        // WorldInfo gameType int（b1.8.1=rl.p private）

    // D10 XP 面（b1.8+；键名与锚库 b1.9-pre2.json ops 段逐一对榫——b1.8.1 实值见 build181，
    // 取证=src-minecraft-beta-1.8.1 树 EntityPlayer.java 直读：field_35211_aX:NBT"Xp"/
    // field_35210_aY:"XpLevel"/field_35209_aZ:"XpTotal"/field_35214_aG:"XpCooldown"/ay=score，
    // 全 public int（L464-487 NBT 写读配对实锤）。⚠️ 跨版本字母漂移严禁互抄：
    // b1.9-pre2 xpF=aW/xpLevelF=aY/playerLevelF=aX（XpTotal 载体，MCP 命名混乱位）/
    // xpCooldownF=aF/scoreF=ax，b1.8.1 全不同（aX/aY/aZ/aG/ay）。
    public String xpF;              // EntityPlayer "Xp" int（当前等级内进度经验）
    public String xpLevelF;         // EntityPlayer "XpLevel" int（等级）
    public String playerLevelF;     // EntityPlayer "XpTotal" int（累计总经验——键名沿用
                                    // MCP 字段名混淆位，语义=NBT XpTotal，b1.9-pre2 锚件同键）
    public String scoreF;           // EntityPlayer score int（NBT "Score"，只读面入 detail）
    public String xpCooldownF;      // EntityPlayer xpCooldown int（拾取冷却 tick，写 0 立即可再拾）
    public String addXpM;           // addExperience(I)V（b1.9-pre2=f；b1.8.1 无独立方法=null——
                                    // 加经验循环体内联 EntityPlayer L1050，setxp 走直写字段面）
    public String increaseLevelM;   // increaseLevel()V（19pre2=aL / b1.8.1=ab func_35206——留档）

    // D10 效果面（b1.8+；取证=src-minecraft-beta-1.8.1 树 EntityLiving.java 直读：
    // func_35165_a(Lade;)V=addPotionEffect（L1007 bR.put/合并双分支）、damageEntity(I)V
    // =MCP 误名实为 removePotionEffect（L1019 bR.remove(i1)——javap 字母 f）、
    // func_35160_a(Lud;)Z=isPotionActive、func_35167_b(Lud;)Lade;=getPotionEffect、
    // field_35170_bR:HashMap=activePotionsMap（protected）、ade 构造 (III)V（L8）。
    // ⚠️ b1.8.1 无 clearActivePotions 面（全树 bR.clear() 零命中）=clearEffectsM null；
    // b1.9-pre2 锚件字母全不同（afk/vt/bP/i/aG）——严禁互抄）。
    public String addEffectM;       // EntityLiving addPotionEffect 名（b1.8.1=a / 19pre2=a）
    public String addEffectDesc;    // 参类名从 desc 解（b1.8.1=(Lade;)V / 19pre2=(Lafk;)V）
    public String removeEffectM;    // EntityLiving removePotionEffect(I)V（b1.8.1=f / 19pre2=i）
    public String clearEffectsM;    // clearActivePotions()V（b1.8.1=null 无此面 / 19pre2=aG）
    public String isActiveEffectM;  // isPotionActive(L<Potion>;)Z（查 Potion 对象非 id——留档）
    public String getActiveEffectM; // getPotionEffect(L<Potion>;)L<PotionEffect>;（留档）
    public String effectsMapF;      // EntityLiving activePotionsMap HashMap（b1.8.1=bR / 19pre2=bP）
    public String potionClassG;     // Potion 类名（b1.8.1=ud / 19pre2=vt——isActive/get 参类）
    public String potionEffectClassG; // PotionEffect 类名（b1.8.1=ade / 19pre2=afk）
    public String potionEffectCtorDesc; // 构造描述符 (III)V（id,durationTicks,amplifier 历代同形）
    public String xpOrbClassG;      // EntityXPOrb 类名（19pre2=bi——spawn 面后续，本批不消费）

    // D11 饥饿面（b1.8+；sethunger 消费。取证=src-minecraft-beta-1.8.1 树 FoodStats.java
    // 直读：field_35776_a=foodLevel(int 0..20,NBT"foodLevel")/field_35774_b=saturation
    // (float,NBT"foodSaturationLevel")/field_35775_c=exhaustion(float,NBT"foodExhaustionLevel")/
    // field_35772_d=tickTimer(int,NBT"foodTickTimer")——**全 private**（getDeclaredField+
    // setAccessible，teBurnF/isDmg 同配方）。EntityPlayer.field_35217_av=protected FoodStats，
    // 取用器 func_35191_at()Lkc;（public 字母 at）——走访问器零 private 玩家字段。
    // ⚠️ b1.9-pre2 饥饿面同族存在（capabilities 记 OK），但其混淆字母未钉=本批不建运行表。
    public Boolean capHunger;       // b1.8+（FoodStats 挂 EntityPlayer）
    public String foodStatsM;       // EntityPlayer 取用器名（b1.8.1=at ()Lkc;）
    public String foodStatsClassG;  // FoodStats 类名（b1.8.1=kc）
    public String foodLevelF;       // FoodStats foodLevel int（b1.8.1=a）
    public String foodSatF;         // FoodStats saturation float（b1.8.1=b）
    public String foodExhaustF;     // FoodStats exhaustion float（b1.8.1=c，留档）
    public String foodTimerF;       // FoodStats tickTimer int（b1.8.1=d，留档）

    /** spawn 白名单：kind→实体类全名（含 tnt/giant/mob 特例）。 */
    public final Map<String, String> mobClasses = new HashMap<String, String>();

    /** B5 织入锚数据（JSON hooks 段原样入表，key=钩名 SAVE_WORLD 等，
     *  value=class|name|desc|access|expectedHits|injectMode 平铺六元组）；
     *  HookTargets.activate 对无硬编码钩组的 JSON 版消费本表（硬编码优先）。 */
    public final Map<String, String> hookSpecs = new HashMap<String, String>();
}
