package com.mcworldagent.agent;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 命令操作面的反射执行体（案卷 agent-command-channel §6）。
 * 混淆实名与 FirstBrick 同源（0327 指纹表，phase1-first-brick §T1 javap 实锤）。
 * 仅在两类线程调用：轮询线程只做 resolve（签名探测）；游戏线程执行 invoke。
 * 签名不符一律抛 UnsupportedOp（映射为 UNSUPPORTED，宁可响亮失败不静默）。
 */
public final class WorldOps {

    private WorldOps() {
    }

    /** 版本签名不符（混淆名/描述符对不上）。 */
    public static final class UnsupportedOp extends Exception {
        public UnsupportedOp(String m) {
            super(m);
        }
    }

    /** 参数/前置不满足（映射 REJECTED）。 */
    public static final class Rejected extends Exception {
        public Rejected(String m) {
            super(m);
        }
    }

    /** 批量写零变更（映射 NO_CHANGE，协议 v2 终态；唯一生产者=fill）。message=完整 detail。 */
    public static final class NoChange extends Exception {
        public NoChange(String m) {
            super(m);
        }
    }

    // 版本契约表（A1 迁移批 + A2 双表）：全部混淆实名/游戏 id 已迁入
    // VersionRegistry.build0327()/build0618()（取证注释随值迁移，案卷指针在该文件头）。
    // 以下常量=表值别名，引用点零改动；零行为变化验收=无头逐字段等值断言。
    // A2 激活时序：VersionResolver 在 premain 跑，本类首触在 op 执行期（激活之后），
    // static final 捕获的即激活表（0327 现行为零变化）。
    // 档位一（spec §3）：null 槽=该面未钉——resolve 逐槽跳 null，op 入口 require 响亮。
    private static final VersionTable T = VersionRegistry.current();

    private static final String M_SET_BLOCK = T.setBlockM;    // setBlockWithNotify (IIII)Z
    private static final String M_GET_BLOCK = T.getBlockM;    // getBlockId (III)I
    private static final String M_SAVE_WORLD = T.saveWorldM;  // saveWorld (Z)V private
    private static final String F_WORLD_TIME = T.worldTimeF;  // NBT "Time"
    private static final String F_RAND_SEED = T.randSeedF;    // NBT "RandomSeed"
    private static final String F_SPAWN_X = T.spawnXF;
    private static final String F_SPAWN_Y = T.spawnYF;
    private static final String F_SPAWN_Z = T.spawnZF;
    // b1.7.3 断代（详表 §2 结构断代①）：World 不再持 time/seed/spawn 字段——
    // WorldInfo 取用器名（fd.x()Lei）；null=字段在 World 本体（0327..b1.2_02 现形态）
    private static final String M_GET_WORLD_INFO = T.getWorldInfoM;

    // tp/getplayer 面（表值；取证见 VersionRegistry。注意 W 在子类——execute 期按实例类
    // 惰性解析，失败→UnsupportedOp 不静默）
    private static final String F_WORLD_PLAYER = T.playerF;
    private static final String F_WORLD_PLAYER_LIST = T.playerListF;   // 档位三：无玩家直字段版本的列表面
    private static final String G_ENTITY_CLASS = T.entityClassG;       // 仅 F_WORLD_PLAYER==null 时消费
    private static final String M_SET_POS_ROT = T.setPosRotM;
    private static final String E_POS_X = T.posXF;
    private static final String E_POS_Y = T.posYF;
    private static final String E_POS_Z = T.posZF;
    private static final String E_MOT_X = T.motXF;
    private static final String E_MOT_Y = T.motYF;
    private static final String E_MOT_Z = T.motZF;
    private static final String E_YAW = T.yawF;
    private static final String E_PITCH = T.pitchF;
    private static final String E_HEALTH = T.healthF;

    // 物品栏面（表值；give 必须注册表校验防 NPE、sethealth 直写 W 绕过治疗钳制等
    // 取证细节见 VersionRegistry 注释与 give/sethealth 案卷批）
    private static final String F_INV = T.invF;
    private static final String INV_MAIN = T.invMainF;
    private static final String INV_ARMOR = T.invArmorF;
    private static final String INV_CUR = T.invCurF;
    private static final String IS_COUNT = T.isCountF;
    private static final String IS_ID = T.isIdF;
    private static final String IS_DMG = T.isDmgF;
    private static final String M_ADDSTACK = T.addStackM;
    private static final String G_ITEM_CLASS = T.itemClassG;
    private static final String ITEMS = T.itemsF;

    // 实体面（表值；spawn/drop 恒 transient（0327 saveWorld 不写实体）、explode 不豁免玩家、
    // 六 mob 构造器配方等取证见 VersionRegistry 与 spawn-explode-drop 案卷批）
    private static final String M_EXPLODE = T.explodeM;
    private static final String M_SPAWN_ENTITY = T.spawnEntityM;
    private static final String G_ITEM_ENTITY = T.itemEntityG;

    // grow/clear 面（表值；骨粉=官方强熟判决见 grow-clear spec §1，setEntityDead static
    // 仅置 isDead 真移除在 tick、EntityItem isInstance 缓存配方同见）
    private static final String M_SET_ENTITY_DEAD = T.setEntityDeadM;
    // A2 档位一：Integer（null=未钉，require 守卫后解箱）
    private static final Integer ID_CROPS = T.idCrops;
    private static final Integer ID_SAPLING = T.idSapling;
    private static final Integer CROPS_RIPE_META = T.cropsRipeMeta;

    // invulnerable 面（表值；attackEntityFrom 单入口全伤害源拒绝、K>=11 免疫窗口、
    // 和平难度抹杀敌对、giant 构造器复活更正等取证见 sed-1 案卷批）
    private static final String E_IFRAMES = T.iframesF;

    // tileentity 面（表值；TE 创建/删除=游戏自动、持久化随 chunk、IInventory 三方法从
    // 实例类 getMethod、Furnace getInvName 笔误等取证见 tileentity-batch 案卷批）
    private static final String M_TE_GET = T.teGetM;
    private static final String M_INV_SIZE = T.invSizeM;
    private static final String M_INV_GET = T.invGetM;
    private static final String M_INV_SET = T.invSetM;
    private static final String F_TE_BURN = T.teBurnF;
    private static final String F_TE_COOK = T.teCookF;

    // 零取证五 op 批（表值；entitylist per-instance 探测不可全局缓存 Field、playsound
    // miss 静默 no-op、setarmor 不校验 ItemArmor、clearinv 不可逆等取证见案卷批）
    private static final String M_ENTITY_LIST = T.entityListM;
    private static final String M_ENTITY_TYPE = T.entityTypeM;
    private static final String G_ENTITY_LIST_CLASS = T.entityListClassG;
    private static final String M_ENTITY_LIST_LOOKUP = T.entityListLookupM;
    private static final String M_PLAY_SOUND = T.playSoundM;
    private static final Map<String, String> MOB_CLASSES = T.mobClasses;

    // meta/genscatter/getlight 面（表值；静默写非 notify、散布器=椭球矿石团生成器（meta-1
    // 实跑翻案）、getBrightness 纯读（旧"气温"笔记作废）、difficulty 写无效等取证见 meta 轮案卷）
    private static final String M_GET_META = T.getMetaM;
    private static final String M_SET_META_SILENT = T.setMetaSilentM;
    private static final String M_LIGHT6 = T.light6M;
    private static final String M_GET_LIGHT = T.getLightM;
    private static final String F_OBSERVERS = T.observersF;
    private static final String F_DIFFICULTY = T.difficultyF;
    private static final String G_SCATTER_CLASS = T.scatterClassG;

    // biome 面（a1.2.0 首版；读回链三锚 verscan/a1.2.0_02.md §8：World.a()Lpb;→pb.a(II)Lfy;
    // →fy.m:String 全 public；WCM 大方法纯读（噪声器现算 temp/hum→4096 查找表），零副作用
    // 零 chunk 加载依赖——未加载坐标亦返回。beta 前版本槽 null=require 响亮 UNSUPPORTED）
    private static final String M_BIOME_WCM = T.biomeWcmM;
    private static final String M_BIOME_AT = T.biomeAtM;
    private static final String F_BIOME_NAME = T.biomeNameF;

    // 区域批量写读上限（fill/getblocks 共用）：16³=4096。
    // 取证：v1 fill=循环 c(IIII)Z（place/dig 同路径）；Chunk.setTile 内部已含 heightmap 重算
    // （g→f→world.d 观察者区间通知）+tile 移除+onPlace+isModified 脏位，快路径（a(IIII)Z）省不出数量级。
    // 副作用实锤：provider b(II) 对未加载坐标就地生成整 chunk——cap 即 tick 预算限幅，fill-1 计时后议调。
    public static final int MAX_REGION_VOLUME = 4096;

    private static volatile Method setBlock;
    private static volatile Method getBlock;
    private static volatile Method saveWorld;
    private static volatile Method worldInfoM; // b1.7.3：fd.x()Lei（time/seed/spawn 承载体取用器）
    private static volatile Field randSeed; // 仅缓存 setAccessible 结果
    private static volatile Field worldTime;
    private static volatile Field spawnX;
    private static volatile Field spawnY;
    private static volatile Field spawnZ;
    private static volatile Field player;      // World.c（类型=Entity 基类，活体玩家）
    private static volatile Field playerList;  // 档位三回退：World.playerEntities（a1.1.2_01.k）
    private static volatile Class<?> entityClass; // Entity 基类（playerF 型或 entityClassG 装载）
    private static volatile Method setPosRot;  // Entity.b(DDDFF)V
    private static volatile Field ePosX, ePosY, ePosZ;
    private static volatile Field eMotX, eMotY, eMotZ;
    private static volatile Field eYaw, ePitch;
    private static volatile Field eHealth; // 惰性：EntityLiving 子类字段，按实例类解析
    // 物品栏面惰性缓存（give/inv 首用解析；ItemStack 类=主槽数组分量类型，零硬编码类名）
    private static volatile Field invField;   // EntityHuman.b（InventoryPlayer）
    private static volatile Field invMain;    // InventoryPlayer.a ItemStack[36]
    private static volatile Field invArmor;   // InventoryPlayer.b ItemStack[4]
    private static volatile Field invCur;     // InventoryPlayer.c int
    private static volatile Method addStack;  // InventoryPlayer.a(ItemStack)Z
    private static volatile Field isCount, isId, isDmg;
    private static volatile Constructor<?> isCtor2, isCtor3;
    private static volatile Field itemRegistry; // i.b public static（按 id 索引）
    // 实体面惰性缓存（spawn-explode-drop 批；仅游戏线程访问，无需并发容器）
    private static volatile Method explode;      // World a(Entity,DDDF)V
    private static volatile Method spawnEntity;  // World a(Entity)V
    private static volatile Constructor<?> itemCtor; // EntityItem (World,DDD,ItemStack)V
    private static volatile Field eIframes;      // Entity.K=heartsLife（惰性，eHealth 同模式）
    private static final Map<String, Constructor<?>> MOB_CTORS = new HashMap<String, Constructor<?>>();
    // tileentity 面惰性缓存（getchest/setchest/getfurnace 首用解析；仅游戏线程访问）
    private static volatile Method teGet;        // World h(III)->TE
    // IInventory 三方法**按 TE 类名缓存**（chest/furnace 异类 Method 对象不通用——无头首跑实锤）：
    // 值={size()I, get(I)Lk;, set(I,Lk;)V}
    private static final Map<String, Method[]> TE_METHODS = new HashMap<String, Method[]>();
    // 零取证五 op 批惰性缓存（entitylist 成员 per-instance 解析不入缓存，见方法注释）
    private static volatile Method entityListM;   // World i()/j()List
    private static volatile Method entityListLookup; // EntityList 静态 a(Entity)String（null=槽未钉）
    private static volatile Method playSoundM;    // World a(DDD,String,F,F)V
    private static volatile Method setEntityDeadM;    // World b(Entity)V static（clear 惰性）
    private static volatile Class<?> entityItemClass; // EntityItem instanceof 缓存（clear 惰性）
    private static volatile Method getMeta;
    private static volatile Method setMetaSilent;
    private static volatile Method light6;
    private static volatile Method getLight;
    private static volatile Field observers;  // World.u private List
    private static volatile Field difficulty; // World.d public int
    private static volatile Method biomeWcm;  // World.a()Lpb;（resolve 期钉，槽 null=跳过）
    /** A2 档位一：resolve 逐槽跳 null 后，缓存判据不能再用单槽非空（用完成标志）。 */
    private static volatile boolean resolved;

    /** 签名探测（可在任意线程调用；NoSuchMethod/NoSuchField → UnsupportedOp）。
     *  A2 档位一（spec §3）：null 槽=该面未钉，逐槽跳过不再整块 eager——
     *  0618 缺失面（setmeta/light/getinfo 等）不连坐已钉面（place/dig/tp/save）。 */
    static synchronized void resolve(Class<?> wc) throws UnsupportedOp {
        if (resolved) {
            return;
        }
        try {
            if (M_SET_BLOCK != null) {
                setBlock = wc.getMethod(M_SET_BLOCK, int.class, int.class, int.class, int.class);
            }
            if (M_GET_BLOCK != null) {
                getBlock = wc.getMethod(M_GET_BLOCK, int.class, int.class, int.class);
            }
            if (M_SAVE_WORLD != null) {
                // 档位二：按表描述符分支（0327 (Z)V / 0618 ()V / 0629 (Liw;)V /
                // a1.1.2_01 (Lnu;)V——el1121-1 翻案后同 0629 单引用型参形）。
                // 参数类名从 desc 解析（"(Liw;)V"→"iw"、"(Lnu;)V"→"nu"），零表外新槽。
                String sd = T.saveWorldDesc;
                saveWorld = ("(Z)V".equals(sd) || "(Z)Z".equals(sd))
                        ? wc.getDeclaredMethod(M_SAVE_WORLD, boolean.class)
                        : sd != null && sd.startsWith("(L") && sd.endsWith(";)V")
                        ? wc.getDeclaredMethod(M_SAVE_WORLD, Class.forName(sd.substring(2, sd.length() - 3)))
                        : wc.getDeclaredMethod(M_SAVE_WORLD);
                saveWorld.setAccessible(true);
            }
            // time/seed/spawn 五槽（b1.7.3 断代分支）：getWorldInfoM 非 null=字段在
            // WorldInfo 返回类型上（ei 字段面全 private javap 实锤——五槽一律
            // getDeclaredField+setAccessible）；null=World 本体现形态（零行为变化）。
            if (M_GET_WORLD_INFO != null) {
                worldInfoM = wc.getMethod(M_GET_WORLD_INFO);
                Class<?> wi = worldInfoM.getReturnType();
                if (F_WORLD_TIME != null) {
                    worldTime = wi.getDeclaredField(F_WORLD_TIME);
                    worldTime.setAccessible(true);
                }
                if (F_RAND_SEED != null) {
                    randSeed = wi.getDeclaredField(F_RAND_SEED);
                    randSeed.setAccessible(true);
                }
                if (F_SPAWN_X != null) {
                    spawnX = wi.getDeclaredField(F_SPAWN_X);
                    spawnX.setAccessible(true);
                }
                if (F_SPAWN_Y != null) {
                    spawnY = wi.getDeclaredField(F_SPAWN_Y);
                    spawnY.setAccessible(true);
                }
                if (F_SPAWN_Z != null) {
                    spawnZ = wi.getDeclaredField(F_SPAWN_Z);
                    spawnZ.setAccessible(true);
                }
            } else {
                if (F_WORLD_TIME != null) {
                    worldTime = wc.getField(F_WORLD_TIME);
                }
                if (F_RAND_SEED != null) {
                    randSeed = wc.getDeclaredField(F_RAND_SEED);
                    randSeed.setAccessible(true);
                }
                if (F_SPAWN_X != null) {
                    spawnX = wc.getField(F_SPAWN_X);
                }
                if (F_SPAWN_Y != null) {
                    spawnY = wc.getField(F_SPAWN_Y);
                }
                if (F_SPAWN_Z != null) {
                    spawnZ = wc.getField(F_SPAWN_Z);
                }
            }
            // tp 面：World.c 的类型即 Entity 基类，公开成员 getField/getMethod 直取。
            // 档位三（a1.1.2_01）：World 无玩家直字段——playerEntities 列表回退，
            // Entity 基类经表槽 entityClassG 装载（javap 实锤 World 无 dm 字段，verscan §7）。
            Field pl = null;
            if (F_WORLD_PLAYER != null) {
                pl = wc.getField(F_WORLD_PLAYER);
                player = pl;
                entityClass = pl.getType();
            } else if (F_WORLD_PLAYER_LIST != null) {
                playerList = wc.getField(F_WORLD_PLAYER_LIST);
                if (G_ENTITY_CLASS != null) {
                    entityClass = Class.forName(G_ENTITY_CLASS, false, wc.getClassLoader());
                }
            }
            if (M_SET_POS_ROT != null && entityClass != null) {
                Class<?> ec = entityClass;
                // 档位二：0327 (DDDFF)V 五参 / 0618 (DDD)V 三参。
                // a3-0618-3 活体翻案：0618 a(DDD)V 是 **protected final**——getMethod 只认
                // public 报 NoSuchMethod 并连坐整块 resolve（listops 全堵）。改 getDeclaredMethod：
                // 两版方法均声明于 Entity 基类（=pl.getType()，非实例子类），命中即等价；
                // setAccessible 对 0327 public 版零影响。
                setPosRot = "(DDD)V".equals(T.setPosRotDesc)
                        ? ec.getDeclaredMethod(M_SET_POS_ROT, double.class, double.class, double.class)
                        : ec.getDeclaredMethod(M_SET_POS_ROT, double.class, double.class, double.class,
                                float.class, float.class);
                setPosRot.setAccessible(true);
                ePosX = ec.getField(E_POS_X);
                ePosY = ec.getField(E_POS_Y);
                ePosZ = ec.getField(E_POS_Z);
                eMotX = ec.getField(E_MOT_X);
                eMotY = ec.getField(E_MOT_Y);
                eMotZ = ec.getField(E_MOT_Z);
                eYaw = ec.getField(E_YAW);
                ePitch = ec.getField(E_PITCH);
            }
            // meta/genscatter/difficulty 面
            if (M_GET_META != null) {
                getMeta = wc.getMethod(M_GET_META, int.class, int.class, int.class);
            }
            if (M_SET_META_SILENT != null) {
                setMetaSilent = wc.getMethod(M_SET_META_SILENT, int.class, int.class, int.class, int.class);
            }
            if (M_LIGHT6 != null) {
                light6 = wc.getMethod(M_LIGHT6, int.class, int.class, int.class, int.class);
            }
            if (M_GET_LIGHT != null) {
                getLight = wc.getMethod(M_GET_LIGHT, int.class, int.class, int.class);
            }
            if (F_OBSERVERS != null) {
                observers = wc.getDeclaredField(F_OBSERVERS);
                observers.setAccessible(true);
            }
            if (F_DIFFICULTY != null) {
                difficulty = wc.getField(F_DIFFICULTY);
            }
            if (M_BIOME_WCM != null) {
                biomeWcm = wc.getMethod(M_BIOME_WCM);
            }
            resolved = true;
        } catch (NoSuchMethodException e) {
            throw new UnsupportedOp("world signatures: " + e);
        } catch (NoSuchFieldException e) {
            throw new UnsupportedOp("world fields: " + e);
        } catch (ClassNotFoundException e) {
            // 0629 档位二：saveWorld 参数类名从 desc 解析（"iw"）——类不存在=表钉错，响亮
            throw new UnsupportedOp("saveWorld param class: " + e);
        }
    }

    /** 档位一守卫（spec §3）：表槽 null=该版本面无此能力→响亮 UnsupportedOp 带槽名。 */
    private static void require(String slot, Object pinned) throws UnsupportedOp {
        if (pinned == null) {
            throw new UnsupportedOp("slot " + slot + " not pinned for " + T.versionId);
        }
    }

    /** 玩家获取（档位三建模）：playerF 已钉=直读字段；否则 playerEntities.get(0)
     *  （a1.1.2_01 javap 实锤 World 无玩家字段；单人客户端列表首件=活体玩家，
     *  verscan §7 判决；空表=未载入→与 no-player 同语义 Rejected）。 */
    private static Object worldPlayer(Object world) throws Exception {
        Field pl = player;
        if (pl != null) {
            return pl.get(world);
        }
        if (playerList == null) {
            throw new UnsupportedOp("player face: neither playerF nor playerListF pinned for " + T.versionId);
        }
        List<?> l = (List<?>) playerList.get(world);
        return (l == null || l.isEmpty()) ? null : l.get(0);
    }

    /** setPosRot 调用适配（档位二）：5 参版透传 yaw/pitch；3 参版（0618 a(DDD)V）
     *  直写面不含旋转——调用方已先直写字段，yaw/pitch 未动=等价（spec §3）。 */
    private static void invokeSetPosRot(Object target, double x, double y, double z,
                                        float yaw, float pitch) throws Exception {
        if (setPosRot.getParameterTypes().length == 5) {
            setPosRot.invoke(target, Double.valueOf(x), Double.valueOf(y), Double.valueOf(z),
                    Float.valueOf(yaw), Float.valueOf(pitch));
        } else {
            setPosRot.invoke(target, Double.valueOf(x), Double.valueOf(y), Double.valueOf(z));
        }
    }

    /** b1.7.3 断代：time/seed/spawn 五字段承载体=WorldInfo（fd.x()Lei）；
     *  worldInfoM null=World 本体（0327..b1.2_02 现形态，零行为变化）。 */
    private static Object timeHolder(Object world) throws Exception {
        return worldInfoM == null ? world : worldInfoM.invoke(world);
    }

    /** 世界信息快照（op=getinfo，只读）：tick 边界执行=一致读。 */
    public static String getInfo(Object world) throws Exception {
        require("difficultyF", F_DIFFICULTY); // 0618 未钉→UNSUPPORTED（spec §3）
        Object wi = timeHolder(world);
        long time = worldTime.getLong(wi);
        long seed = randSeed.getLong(wi);
        int sx = spawnX.getInt(wi);
        int sy = spawnY.getInt(wi);
        int sz = spawnZ.getInt(wi);
        int diff = difficulty.getInt(world);
        return "time=" + time + " spawnX=" + sx + " spawnY=" + sy + " spawnZ=" + sz
                + " seed=" + seed + " difficulty=" + diff;
    }

    /** 设置 worldTime（op=settime）。渲染每帧经 b(F) 实时读，无通知面；返回旧值供回滚。 */
    public static long setTime(Object world, long time) throws Exception {
        Object wi = timeHolder(world);
        long old = worldTime.getLong(wi);
        worldTime.setLong(wi, time);
        return old;
    }

    /** 设置出生点（op=setspawn）。唯一消费方=EntityPlayer 构造器，下次进世界生效。 */
    public static void setSpawn(Object world, int x, int y, int z) throws Exception {
        Object wi = timeHolder(world);
        spawnX.setInt(wi, x);
        spawnY.setInt(wi, y);
        spawnZ.setInt(wi, z);
    }

    /**
     * 读列生物群系名（op=getbiome，只读；a1.2.0 起，verscan §8 三锚链）：
     * World.a()Lpb;→pb.a(II)Lfy;→fy.m:String。WCM/biome 方法按**实例类**解析
     * （pb 的 (II) 重载唯一；biome 子类 de/lz/i 继承 public m——getField 达继承公共成员），
     * 零表外类名硬编码。纯数学现算（噪声器 temp/hum→4096 查找表），不触 chunk——
     * 未加载坐标亦返回，无 tick 时序依赖。
     */
    public static String biomeAt(Object world, int x, int z) throws Exception {
        require("biomeWcmM", M_BIOME_WCM);
        Object wcm = biomeWcm.invoke(world);
        Method at = wcm.getClass().getMethod(M_BIOME_AT, int.class, int.class);
        Object biome = at.invoke(wcm, Integer.valueOf(x), Integer.valueOf(z));
        return (String) biome.getClass().getField(F_BIOME_NAME).get(biome);
    }

    /**
     * 传送玩家（op=tp）。复刻游戏 NBT 载入配方（Entity.d←Pos 后调 b(DDDFF)）：
     * 直写 pos=块中心(x+0.5,y,z+0.5)、清 motion（防残留速度拉扯），
     * 再经 setPositionAndRotation 同步 lastTick(d,e,f) 与重建 AABB——与"重进世界恢复位置"同路径。
     * 注意 b() 内部 h=y+u（u=眼偏移 1.62）：脚落 y 平面、眼在 y+1.62，载入配方即如此。
     * 返回旧位供回滚记录。
     */
    public static String tp(Object world, int x, int y, int z) throws Exception {
        Object p = worldPlayer(world);
        if (p == null) {
            throw new Rejected("no-player");
        }
        double ox = ePosX.getDouble(p);
        double oy = ePosY.getDouble(p);
        double oz = ePosZ.getDouble(p);
        float yaw = eYaw.getFloat(p);
        float pitch = ePitch.getFloat(p);
        double nx = x + 0.5;
        double ny = y;
        double nz = z + 0.5;
        ePosX.setDouble(p, nx);
        ePosY.setDouble(p, ny);
        ePosZ.setDouble(p, nz);
        eMotX.setDouble(p, 0.0);
        eMotY.setDouble(p, 0.0);
        eMotZ.setDouble(p, 0.0);
        invokeSetPosRot(p, nx, ny, nz, yaw, pitch); // 档位二：5 参透传/3 参不含旋转
        return "teleported from x=" + ox + " y=" + oy + " z=" + oz
                + " -> x=" + x + " y=" + y + " z=" + z;
    }

    /** time 校验：0327 世界时间从 0 起算，负值无意义（REJECTED）。 */
    public static void validateTime(long time) throws Rejected {
        if (time < 0) {
            throw new Rejected("time must be >= 0: " + time);
        }
    }

    /**
     * 区域校验+归一化（fill/getblocks 共用）：两角各自 validateCoords，
     * min/max 归一，体积（含边界）≤ MAX_REGION_VOLUME（long 乘法防溢出）。
     * 返回 {minX,minY,minZ,maxX,maxY,maxZ}。
     */
    public static int[] normalizeRegion(int x1, int y1, int z1, int x2, int y2, int z2)
            throws Rejected {
        validateCoords(x1, y1, z1);
        validateCoords(x2, y2, z2);
        int mnx = Math.min(x1, x2);
        int mxx = Math.max(x1, x2);
        int mny = Math.min(y1, y2);
        int mxy = Math.max(y1, y2);
        int mnz = Math.min(z1, z2);
        int mxz = Math.max(z1, z2);
        long vol = (long) (mxx - mnx + 1) * (long) (mxy - mny + 1) * (long) (mxz - mnz + 1);
        if (vol > MAX_REGION_VOLUME) {
            throw new Rejected("volume-too-large: " + vol + " > " + MAX_REGION_VOLUME);
        }
        return new int[]{mnx, mny, mnz, mxx, mxy, mxz};
    }

    /**
     * 区域批量写（op=fill）。v1=循环 setBlockWithNotify（取证：快路径省不出数量级，正确性优先）。
     * 零变更（全区域已是同 id）→ NoChange(detail)（映射 NO_CHANGE，跳过 save——磁盘态未动）。
     */
    public static String fill(Object world, int x1, int y1, int z1, int x2, int y2, int z2,
                              int blockId) throws Exception {
        validateBlockId(blockId);
        int[] r = normalizeRegion(x1, y1, z1, x2, y2, z2);
        int changed = 0;
        for (int y = r[1]; y <= r[4]; y++) {
            for (int x = r[0]; x <= r[3]; x++) {
                for (int z = r[2]; z <= r[5]; z++) {
                    if (setBlock(world, x, y, z, blockId)) {
                        changed++;
                    }
                }
            }
        }
        String detail = "filled [" + r[0] + ".." + r[3] + "] [" + r[1] + ".." + r[4] + "] ["
                + r[2] + ".." + r[5] + "] id=" + blockId + " changed=" + changed;
        if (changed == 0) {
            throw new NoChange(detail);
        }
        return detail;
    }

    /**
     * 区域批量读（op=getblocks）。循环 getBlockId（游戏同语义；未加载坐标=provider 就地生成，cap 限幅）。
     * RLE 编码遍历序 y→x→z（y 外层），run 形如 id:count，| 分隔。
     */
    public static String getblocks(Object world, int x1, int y1, int z1, int x2, int y2, int z2)
            throws Exception {
        int[] r = normalizeRegion(x1, y1, z1, x2, y2, z2);
        StringBuilder rle = new StringBuilder("rle=");
        int runId = -1;
        int runLen = 0;
        for (int y = r[1]; y <= r[4]; y++) {
            for (int x = r[0]; x <= r[3]; x++) {
                for (int z = r[2]; z <= r[5]; z++) {
                    int id = getBlockId(world, x, y, z);
                    if (id == runId) {
                        runLen++;
                    } else {
                        if (runLen > 0) {
                            appendRun(rle, runId, runLen);
                        }
                        runId = id;
                        runLen = 1;
                    }
                }
            }
        }
        if (runLen > 0) {
            appendRun(rle, runId, runLen);
        }
        int dx = r[3] - r[0] + 1;
        int dy = r[4] - r[1] + 1;
        int dz = r[5] - r[2] + 1;
        return "getblocks " + dx + "x" + dy + "x" + dz + " " + rle;
    }

    private static void appendRun(StringBuilder rle, int id, int len) {
        if (rle.length() > 4) {
            rle.append('|');
        }
        rle.append(id).append(':').append(len);
    }

    /** 玩家快照（op=getplayer，只读）：pos/motion/rot（tp 轮已 resolve 字段）+ Health（惰性解析，0327=W/0618=Z）。 */
    public static String getPlayer(Object world) throws Exception {
        require("healthF", E_HEALTH); // 档位一守卫（双表已钉，spec §3）
        Object p = worldPlayer(world);
        if (p == null) {
            throw new Rejected("no-player");
        }
        int health = healthField(p).getInt(p);
        return "x=" + ePosX.getDouble(p) + " y=" + ePosY.getDouble(p)
                + " z=" + ePosZ.getDouble(p)
                + " motX=" + eMotX.getDouble(p) + " motY=" + eMotY.getDouble(p)
                + " motZ=" + eMotZ.getDouble(p)
                + " yaw=" + eYaw.getFloat(p) + " pitch=" + ePitch.getFloat(p)
                + " health=" + health;
    }

    /**
     * 朝向直写（op=setlook）。Entity m/n=yaw/pitch（public float，tp 轮 setFloat 活体实锤）；
     * 零通知面——渲染/瞄准直读字段（tp 轮 b(DDDFF) 设 m/n 即生效同证）。
     * 协议：整数角度（MiniJson 拒浮点），yaw∈[0,360)、pitch∈[-90,90]。返回旧值供回滚。
     */
    public static String setLook(Object world, int yawDeg, int pitchDeg) throws Exception {
        Object p = worldPlayer(world);
        if (p == null) {
            throw new Rejected("no-player");
        }
        if (yawDeg < 0 || yawDeg >= 360) {
            throw new Rejected("yaw-out-of-range [0,360): " + yawDeg);
        }
        if (pitchDeg < -90 || pitchDeg > 90) {
            throw new Rejected("pitch-out-of-range [-90,90]: " + pitchDeg);
        }
        float oy = eYaw.getFloat(p);
        float op = ePitch.getFloat(p);
        eYaw.setFloat(p, (float) yawDeg);
        ePitch.setFloat(p, (float) pitchDeg);
        return "look yaw " + oy + " -> " + yawDeg + " pitch " + op + " -> " + pitchDeg;
    }

    /**
     * 速度直写（op=setmotion）。Entity j/k/l=motion（public double，tp 轮清速度 setDouble 活体实锤；
     * spec 表"float"系笔误，以代码为准）。mover 每 tick 由 AABB 自愈重算 pos——写速度后物理自洽。
     * 协议：整数千分位（|v|≤10000=±10 格/tick），旧值按千分位回显供回滚。
     * 活体疑点（llm-1 定语义）：站方块上写 vy 是否被 mover 首 tick 碰撞解算清零。
     */
    public static String setMotion(Object world, int vx1000, int vy1000, int vz1000)
            throws Exception {
        Object p = worldPlayer(world);
        if (p == null) {
            throw new Rejected("no-player");
        }
        checkMotionThousandths("vx", vx1000);
        checkMotionThousandths("vy", vy1000);
        checkMotionThousandths("vz", vz1000);
        double ox = eMotX.getDouble(p);
        double oy = eMotY.getDouble(p);
        double oz = eMotZ.getDouble(p);
        eMotX.setDouble(p, vx1000 / 1000.0);
        eMotY.setDouble(p, vy1000 / 1000.0);
        eMotZ.setDouble(p, vz1000 / 1000.0);
        return "motion x=" + Math.round(ox * 1000.0) + " y=" + Math.round(oy * 1000.0)
                + " z=" + Math.round(oz * 1000.0)
                + " -> x=" + vx1000 + " y=" + vy1000 + " z=" + vz1000;
    }

    private static void checkMotionThousandths(String name, int v) throws Rejected {
        if (v < -10000 || v > 10000) {
            throw new Rejected(name + "-out-of-range [-10000,10000] thousandths: " + v);
        }
    }

    /** W 惰性解析（getPlayer/setHealth 共用）：EntityLiving 子类字段，按实例类解析。 */
    private static Field healthField(Object p) throws UnsupportedOp {
        Field h = eHealth;
        if (h == null) {
            try {
                h = p.getClass().getField(E_HEALTH);
            } catch (NoSuchFieldException e) {
                throw new UnsupportedOp("entity health field: " + e);
            }
            eHealth = h;
        }
        return h;
    }

    /** 物品栏面惰性解析（eHealth 同模式）：ItemStack 类取自主槽数组分量类型，零硬编码类名。
     *  A2 拆分（spec §5）：inv 面（读/清）与 give 面（addStack+注册表）解耦——
     *  0618 addStackM null 不连坐 inv/clearinv（锚库 capabilities 对榫：inv=OK）。 */
    private static void resolveInvFace(Object p) throws UnsupportedOp {
        if (invMain != null) {
            return;
        }
        try {
            Field inv = p.getClass().getField(F_INV);
            Class<?> invType = inv.getType();
            Field main = invType.getField(INV_MAIN);
            Class<?> stackType = main.getType().getComponentType();
            if (stackType == null) {
                throw new UnsupportedOp("inventory main not an array: " + main);
            }
            Field armor = invType.getField(INV_ARMOR);
            Field cur = invType.getField(INV_CUR);
            Field cnt = stackType.getField(IS_COUNT);
            Field id = stackType.getField(IS_ID);
            Field dmg;
            try {
                dmg = stackType.getField(IS_DMG);
            } catch (NoSuchFieldException e1) {
                // b1.2_02 断代（详表 §5）：ItemStack(gm).itemDamage=d 为 **private**——
                // getDeclaredField+setAccessible（teBurnF private 先例同配方）；
                // isDmg 读写（inv 快照/drop 对账）全经此 Field 对象。
                dmg = stackType.getDeclaredField(IS_DMG);
                dmg.setAccessible(true);
            }
            Constructor<?> c2;
            try {
                c2 = stackType.getConstructor(int.class, int.class);
            } catch (NoSuchMethodException e2) {
                // b1.2_02 断代（详表 §5）：gm 无 (II)V 构造——容忍 null，
                // 构造走 newStack 配方（(III)V dmg=0 等价，字节码实钉）。
                c2 = null;
            }
            Constructor<?> c3 = stackType.getConstructor(int.class, int.class, int.class);
            invField = inv;
            invMain = main;
            invArmor = armor;
            invCur = cur;
            isCount = cnt;
            isId = id;
            isDmg = dmg;
            isCtor2 = c2;
            isCtor3 = c3;
            if (M_ADDSTACK != null) {
                addStack = invType.getMethod(M_ADDSTACK, stackType);
            }
        } catch (NoSuchFieldException e) {
            throw new UnsupportedOp("inventory fields: " + e);
        } catch (NoSuchMethodException e) {
            throw new UnsupportedOp("inventory signatures: " + e);
        }
    }

    /** ItemStack 构造配方（b1.2_02 断代批，详表 §5）：damage==0 且二参构造健在
     *  （0327..a1.2.0）→(II)V 原配方零变化；二参缺失（isCtor2=null 容忍）或
     *  damage>0 →(III)V（gm 字节码实钉 arg1→c id、arg2→a count、arg3→d dmg，
     *  dmg=0 与 (II)V 等价）。 */
    private static Object newStack(int itemId, int count, int damage) throws Exception {
        if (damage == 0 && isCtor2 != null) {
            return isCtor2.newInstance(Integer.valueOf(itemId), Integer.valueOf(count));
        }
        return isCtor3.newInstance(Integer.valueOf(itemId), Integer.valueOf(count),
                Integer.valueOf(damage));
    }

    /** give 面（addStack+注册表，spec §5）：任一槽 null→UnsupportedOp（give/setarmor/drop 入口守卫）。 */
    private static void resolveGiveFace(Object p) throws UnsupportedOp {
        require("addStackM", M_ADDSTACK);
        require("itemClassG", G_ITEM_CLASS);
        require("itemsF", ITEMS);
        resolveInvFace(p);
        ensureRegistry(p.getClass().getClassLoader());
    }

    /**
     * 给予物品（op=give）。注册表 i.b[] 直索引校验（越界/空洞→游戏侧 a() 即 NPE，必须拒
     * bad-itemid）+ count∈[1,64] + damage∈[0,32767]；damage==0 走 (II)V 否则 (III)V；
     * addStack false=装不下→Rejected inventory-full（原栈未消耗，可 inv 对账）。
     * save 后经 EntityPlayer "Inventory" NBT 落盘（取证：主槽 Slot 0..35、盔甲 100..103）。
     */
    public static String give(Object world, int itemId, int count, int damage) throws Exception {
        Object p = worldPlayer(world);
        if (p == null) {
            throw new Rejected("no-player");
        }
        if (count < 1 || count > 64) {
            throw new Rejected("count-out-of-range [1,64]: " + count);
        }
        if (damage < 0 || damage > 32767) {
            throw new Rejected("dmg-out-of-range [0,32767]: " + damage);
        }
        resolveGiveFace(p);
        Object[] items = (Object[]) itemRegistry.get(null);
        if (itemId < 0 || itemId >= items.length || items[itemId] == null) {
            throw new Rejected("bad-itemid: " + itemId);
        }
        Object stack = newStack(itemId, count, damage);
        boolean fitted = ((Boolean) addStack.invoke(invField.get(p), stack)).booleanValue();
        if (!fitted) {
            throw new Rejected("inventory-full");
        }
        return "gave " + itemId + " x" + count + (damage == 0 ? "" : " dmg=" + damage);
    }

    /** 物品栏快照（op=inv，纯读）：主槽 0..35、盔甲 A0..A3，槽=数量xID[ d伤害]，cur=快捷槽；全空→empty。 */
    public static String inv(Object world) throws Exception {
        Object p = worldPlayer(world);
        if (p == null) {
            throw new Rejected("no-player");
        }
        resolveInvFace(p);
        Object inv = invField.get(p);
        Object[] main = (Object[]) invMain.get(inv);
        Object[] armor = (Object[]) invArmor.get(inv);
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < main.length; i++) {
            if (main[i] != null) {
                if (b.length() > 0) {
                    b.append(' ');
                }
                b.append(i).append('=').append(describeStack(main[i]));
            }
        }
        for (int i = 0; i < armor.length; i++) {
            if (armor[i] != null) {
                if (b.length() > 0) {
                    b.append(' ');
                }
                b.append('A').append(i).append('=').append(describeStack(armor[i]));
            }
        }
        if (b.length() == 0) {
            return "empty";
        }
        b.append(" cur=").append(invCur.getInt(inv));
        return b.toString();
    }

    private static String describeStack(Object s) throws Exception {
        String r = isCount.getInt(s) + "x" + isId.getInt(s);
        int dmg = isDmg.getInt(s);
        if (dmg != 0) {
            r += " d" + dmg;
        }
        return r;
    }

    /**
     * 血量直写（op=sethealth）。W 直写绕过 b(I)V 治疗钳制（20 上限；死亡中不治疗）
     * →op 侧自校验 0..20，越界拒绝而非截断；HUD 每帧直读 W→即时生效。
     * 置 0=原版死亡流程接管（deathTicks→死亡屏→按钮重生），agent 不代打 respawn；
     * 持久化 0 后重进=载入 Health=0 再次死亡屏（原版语义，不特殊处理）。
     */
    public static String setHealth(Object world, int health) throws Exception {
        require("healthF", E_HEALTH); // 档位一守卫（双表已钉，spec §3）
        Object p = worldPlayer(world);
        if (p == null) {
            throw new Rejected("no-player");
        }
        if (health < 0 || health > 20) {
            throw new Rejected("health-out-of-range [0,20]: " + health);
        }
        Field h = healthField(p);
        int old = h.getInt(p);
        h.setInt(p, health);
        return "health " + old + " -> " + health;
    }

    /** 实体面惰性解析（物品栏面同模式）：Entity 基类=player 字段声明类型，零硬编码 FQCN。 */
    private static void resolveEntity(Object world) throws UnsupportedOp {
        if (spawnEntity != null) {
            return;
        }
        try {
            Class<?> wc = world.getClass();
            Class<?> ec = entityClass;
            if (ec == null) {
                throw new UnsupportedOp("entity class not resolved for " + T.versionId);
            }
            explode = wc.getMethod(M_EXPLODE, ec,
                    double.class, double.class, double.class, float.class);
            spawnEntity = wc.getMethod(M_SPAWN_ENTITY, ec);
        } catch (NoSuchMethodException e) {
            throw new UnsupportedOp("entity surface: " + e);
        }
    }

    /**
     * 爆炸（op=explode）。createExplosion(null,x,y,z,(float)size)——source=null 零 NPE
     * （区域取实体判等实锤），但**伤害不豁免玩家**（原版语义，实跑远距离戒律见 spec §4）；
     * size∈[0,10] 整数入参（MiniJson 整数子集），越界拒绝。破坏方块进方块表→save 落盘。
     */
    public static String explode(Object world, int x, int y, int z, int size) throws Exception {
        require("explodeM", M_EXPLODE); // 0618 未钉→UNSUPPORTED（spec §3）
        if (size < 0 || size > 10) {
            throw new Rejected("size-out-of-range [0,10]: " + size);
        }
        resolveEntity(world);
        explode.invoke(world, null,
                Double.valueOf(x), Double.valueOf(y), Double.valueOf(z), Float.valueOf(size));
        return "exploded at " + x + "," + y + "," + z + " size=" + size;
    }

    /**
     * 生成实体（op=spawn）。白名单七 kind（混淆类表 spec §1）；配方=构造(World)→
     * setPosRot(x+0.5,y,z+0.5,0,0)（tnt float 构造已定位，同调用无害幂等）→spawnEntityInWorld
     * （加入即活，主循环每 tick 遍历）；**0327 实体不落盘=恒临时态**（存退即失，save 忽略）。
     */
    public static String spawn(Object world, String kind, int x, int y, int z) throws Exception {
        require("spawnEntityM", M_SPAWN_ENTITY); // 0618 未钉→UNSUPPORTED（spec §3）
        if (MOB_CLASSES.isEmpty()) {
            // 空表=版本面无实体表（档位一），非参数错——响亮报槽名不报 bad-kind
            throw new UnsupportedOp("slot mobClasses not pinned for " + T.versionId);
        }
        String fqcn = MOB_CLASSES.get(kind);
        if (fqcn == null) {
            throw new Rejected("bad-kind: " + kind);
        }
        resolveEntity(world);
        boolean tnt = "tnt".equals(kind);
        Constructor<?> ctor = MOB_CTORS.get(kind);
        if (ctor == null) {
            try {
                Class<?> wc = world.getClass();
                Class<?> mc = Class.forName(fqcn, false, wc.getClassLoader());
                if (tnt) {
                    try {
                        ctor = mc.getConstructor(wc, float.class, float.class, float.class);
                    } catch (NoSuchMethodException e1) {
                        // b1.2_02 断代（详表 §4）：EntityTNTPrimed(mb)=(dn,DDD)V 非历代
                        // FFF（0327..a1.2.0 fuse 内置配方）——fallback 三 double 参构造。
                        ctor = mc.getConstructor(wc, double.class, double.class, double.class);
                    }
                } else {
                    ctor = mc.getConstructor(wc);
                }
            } catch (ClassNotFoundException e) {
                throw new UnsupportedOp("mob class: " + e);
            } catch (NoSuchMethodException e) {
                throw new UnsupportedOp("mob ctor: " + e);
            }
            MOB_CTORS.put(kind, ctor);
        }
        Object ent;
        if (tnt) {
            if (ctor.getParameterTypes()[1] == double.class) {
                ent = ctor.newInstance(world, Double.valueOf(x + 0.5), Double.valueOf(y),
                        Double.valueOf(z + 0.5));
            } else {
                ent = ctor.newInstance(world, Float.valueOf(x + 0.5f), Float.valueOf(y),
                        Float.valueOf(z + 0.5f));
            }
        } else {
            ent = ctor.newInstance(world);
        }
        invokeSetPosRot(ent, x + 0.5, y, z + 0.5, 0f, 0f); // 档位二参长适配
        spawnEntity.invoke(world, ent);
        return "spawned " + kind + " at " + x + "," + y + "," + z;
    }

    /**
     * 掉落物实体（op=drop）。count/dmg/注册表校验复用 give 面（同序同串）；
     * EntityItem(World,DDD,ItemStack) 构造（位置=格中心）→spawnEntityInWorld；
     * 实体不落盘=save 忽略（同 spawn）；走近由原版拾取面收起（addStack 同规则）。
     */
    public static String drop(Object world, int itemId, int count, int damage, int x, int y, int z)
            throws Exception {
        require("spawnEntityM", M_SPAWN_ENTITY); // 0618 未钉→UNSUPPORTED（spec §3）
        Object p = worldPlayer(world);
        if (p == null) {
            throw new Rejected("no-player");
        }
        if (count < 1 || count > 64) {
            throw new Rejected("count-out-of-range [1,64]: " + count);
        }
        if (damage < 0 || damage > 32767) {
            throw new Rejected("dmg-out-of-range [0,32767]: " + damage);
        }
        resolveGiveFace(p);
        Object[] items = (Object[]) itemRegistry.get(null);
        if (itemId < 0 || itemId >= items.length || items[itemId] == null) {
            throw new Rejected("bad-itemid: " + itemId);
        }
        Object stack = newStack(itemId, count, damage);
        resolveEntity(world);
        if (itemCtor == null) {
            try {
                Class<?> wc = world.getClass();
                Class<?> ic = Class.forName(G_ITEM_ENTITY, false, wc.getClassLoader());
                itemCtor = ic.getConstructor(wc, double.class, double.class, double.class,
                        stack.getClass());
            } catch (ClassNotFoundException e) {
                throw new UnsupportedOp("item entity class: " + e);
            } catch (NoSuchMethodException e) {
                throw new UnsupportedOp("item entity ctor: " + e);
            }
        }
        Object ent = itemCtor.newInstance(world,
                Double.valueOf(x + 0.5), Double.valueOf(y), Double.valueOf(z + 0.5), stack);
        spawnEntity.invoke(world, ent);
        return "dropped " + itemId + " x" + count + (damage == 0 ? "" : " dmg=" + damage)
                + " at " + x + "," + y + "," + z;
    }

    /**
     * 免伤窗口（op=invulnerable）。写 Entity.K=heartsLife（原版 i-frame 计时器）——
     * EntityPlayer.attackEntityFrom 入口 `K>10 → return false` **拒绝全部伤害源**
     * （近战/箭/火焰/坠落/虚空/爆炸同走单入口，实锤见常量区注释）；每 tick 自减 1，
     * ticks≥11=免疫窗口、0=取消（32767≈27 分钟）；HUD 副作用=心闪烁（仅视觉）。
     * 瞬态：heartsLife 不进 NBT，save 无作用。
     */
    public static String invulnerable(Object world, int ticks) throws Exception {
        require("iframesF", E_IFRAMES); // 0618 未钉→UNSUPPORTED（spec §3）
        Object p = worldPlayer(world);
        if (p == null) {
            throw new Rejected("no-player");
        }
        if (ticks < 0 || ticks > 32767) {
            throw new Rejected("ticks-out-of-range [0,32767]: " + ticks);
        }
        Field f = eIframes;
        if (f == null) {
            try {
                f = p.getClass().getField(E_IFRAMES);
            } catch (NoSuchFieldException e) {
                throw new UnsupportedOp("entity iframes field: " + e);
            }
            eIframes = f;
        }
        int old = f.getInt(p);
        f.setInt(p, ticks);
        return "iframes " + old + " -> " + ticks;
    }

    /**
     * TE 句柄：World h(III)=getBlockTileEntity（惰性）。**0327 原版地雷（te-1 实跑+字节码双实锤）**：
     * Chunk.d(III) map miss 走重建路径 `Block.blocksList[getBlockId].d(...)`——TE 缺失且方块=空气
     * （id 0，blocksList[0]=null）时该处 **NPE**（游戏自身只在非空气容器块查询，从不踩）。
     * 判决：InvocationTargetException(cause=NPE) 映射 Rejected `no-tileentity`（语义=TE 不在，
     * 协议面不露 ERROR）；其余 ITE 原样上抛（真内部错误仍响亮失败）。
     */
    private static Object teHandle(Object world, int x, int y, int z) throws Exception {
        Method get = teGet;
        if (get == null) {
            try {
                get = world.getClass().getMethod(M_TE_GET, int.class, int.class, int.class);
            } catch (NoSuchMethodException e) {
                throw new UnsupportedOp("tileentity get: " + e);
            }
            teGet = get;
        }
        Object te;
        try {
            te = get.invoke(world, Integer.valueOf(x), Integer.valueOf(y), Integer.valueOf(z));
        } catch (java.lang.reflect.InvocationTargetException ite) {
            if (ite.getCause() instanceof NullPointerException) {
                throw new Rejected("no-tileentity");
            }
            throw ite;
        }
        if (te == null) {
            throw new Rejected("no-tileentity");
        }
        return te;
    }

    /**
     * IInventory 三方法惰性解析（实例类直取，不取接口对象——javac 结构限制案，spec §3.2）。
     * **按 TE 类名缓存**：chest/furnace 是不同类，Method 对象不可跨类 invoke（无头首跑实锤）。
     * 返回 {size()I, get(I)Lk;, set(I,Lk;)V}；ItemStack 字段/构造器全局共享（唯一类）。
     */
    private static Method[] resolveTe(Object te) throws UnsupportedOp {
        // 档位一（spec §3）：0618 teGetM 有值但 IInventory 三方法未钉——
        // getchest/setchest/getfurnace 在此响亮（锚库三件 unconfirmed 对榫）
        require("invSizeM", M_INV_SIZE);
        require("invGetM", M_INV_GET);
        require("invSetM", M_INV_SET);
        String key = te.getClass().getName();
        Method[] cached = TE_METHODS.get(key);
        if (cached != null) {
            return cached;
        }
        try {
            Class<?> tc = te.getClass();
            Method size = tc.getMethod(M_INV_SIZE);
            Method get = tc.getMethod(M_INV_GET, int.class);
            Class<?> stackType = get.getReturnType();
            Method set = tc.getMethod(M_INV_SET, int.class, stackType);
            if (isCount == null) {
                isCount = stackType.getField(IS_COUNT);
                isId = stackType.getField(IS_ID);
                isDmg = stackType.getField(IS_DMG);
                isCtor2 = stackType.getConstructor(int.class, int.class);
                isCtor3 = stackType.getConstructor(int.class, int.class, int.class);
            }
            Method[] m = new Method[] { size, get, set };
            TE_METHODS.put(key, m);
            return m;
        } catch (NoSuchMethodException e) {
            throw new UnsupportedOp("inventory interface: " + e);
        } catch (NoSuchFieldException e) {
            throw new UnsupportedOp("stack fields: " + e);
        }
    }

    /** 注册表惰性解析独立件（TE 面零 player 依赖；与 resolveInvFace 同 classloader 同字段）。 */
    private static void ensureRegistry(ClassLoader cl) throws UnsupportedOp {
        if (itemRegistry != null) {
            return;
        }
        require("itemClassG", G_ITEM_CLASS); // 0618 null→响亮（give 面，spec §5）
        require("itemsF", ITEMS);
        try {
            Class<?> itemClass = Class.forName(G_ITEM_CLASS, false, cl);
            itemRegistry = itemClass.getField(ITEMS);
        } catch (ClassNotFoundException e) {
            throw new UnsupportedOp("item registry class: " + e);
        } catch (NoSuchFieldException e) {
            throw new UnsupportedOp("item registry field: " + e);
        }
    }

    private static String describeInv(Object te, Method[] inv) throws Exception {
        int size = ((Integer) inv[0].invoke(te)).intValue();
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < size; i++) {
            Object s = inv[1].invoke(te, Integer.valueOf(i));
            if (s != null) {
                if (b.length() > 0) {
                    b.append(' ');
                }
                b.append(i).append('=').append(describeStack(s));
            }
        }
        return b.length() == 0 ? "empty" : b.toString();
    }

    /** 容器快照（op=getchest，IInventory 通用面=chest/furnace 皆可）：slots=N 槽=数量xID[ d伤害]；纯读。 */
    public static String getInv(Object world, int x, int y, int z) throws Exception {
        Object te = teHandle(world, x, y, z);
        Method[] inv = resolveTe(te);
        int size = ((Integer) inv[0].invoke(te)).intValue();
        return "slots=" + size + " " + describeInv(te, inv);
    }

    /**
     * 容器写槽（op=setchest）。slot∈[0,size)；clear=true 置空槽（与 itemId/count 互斥，clear 优先）；
     * 否则校验复用 give 面同序同串（count→dmg→bad-itemid，**TE 面零 player 依赖**=ensureRegistry）；
     * setInventorySlotContents **直替换无合并**（原版语义，spec §4）。写侧持久=TE 随 chunk TileEntities。
     */
    public static String setInv(Object world, int x, int y, int z, int slot,
            int itemId, int count, int damage, boolean clear) throws Exception {
        Object te = teHandle(world, x, y, z);
        Method[] inv = resolveTe(te);
        int size = ((Integer) inv[0].invoke(te)).intValue();
        if (slot < 0 || slot >= size) {
            throw new Rejected("slot-out-of-range [0," + size + "): " + slot);
        }
        Object stack = null;
        String what;
        if (clear) {
            what = "cleared";
        } else {
            if (count < 1 || count > 64) {
                throw new Rejected("count-out-of-range [1,64]: " + count);
            }
            if (damage < 0 || damage > 32767) {
                throw new Rejected("dmg-out-of-range [0,32767]: " + damage);
            }
            ensureRegistry(world.getClass().getClassLoader());
            Object[] items = (Object[]) itemRegistry.get(null);
            if (itemId < 0 || itemId >= items.length || items[itemId] == null) {
                throw new Rejected("bad-itemid: " + itemId);
            }
            stack = newStack(itemId, count, damage);
            what = "<- " + itemId + " x" + count + (damage == 0 ? "" : " dmg=" + damage);
        }
        inv[2].invoke(te, Integer.valueOf(slot), stack);
        return "setchest " + x + "," + y + "," + z + " slot=" + slot + " " + what;
    }

    /**
     * 熔炉态（op=getfurnace）。类型判=字段面（f/h 仅 Furnace 类存在且系 **private**——
     * getDeclaredField+setAccessible，randSeed 先例；getInvName 系原版笔误不可信）：
     * NoSuchField→Rejected not-furnace；输出 burn/cook/burning+3 槽（0 料/1 燃/2 产）。纯读。
     */
    public static String getFurnace(Object world, int x, int y, int z) throws Exception {
        require("teBurnF", F_TE_BURN); // 0618 未钉→UNSUPPORTED（spec §3）
        require("teCookF", F_TE_COOK);
        Object te = teHandle(world, x, y, z);
        Method[] inv = resolveTe(te);
        Field burn;
        Field cook;
        try {
            burn = te.getClass().getDeclaredField(F_TE_BURN);
            cook = te.getClass().getDeclaredField(F_TE_COOK);
        } catch (NoSuchFieldException e) {
            throw new Rejected("not-furnace");
        }
        burn.setAccessible(true);
        cook.setAccessible(true);
        int b = burn.getInt(te);
        int c = cook.getInt(te);
        return "furnace burn=" + b + " cook=" + c + " burning=" + (b > 0) + " " + describeInv(te, inv);
    }

    /**
     * 实体清单（op=entitylist，纯读=spawn/drop 的账面对账面）。World i()/j()List 遍历。
     * kind 双轨（EntityList 双轨批 2026-11-14）：主面=EntityList 静态查表 a(Entity)String
     * （注册名=存档 id 同一命名体系，public static 零访问权限麻烦）；miss（玩家等未注册件）
     * →per-instance entityTypeM fallback；再 miss→类简名保底。0327 查表槽 null=纯 fallback
     * 链=改钉批现行为零变化。pos/health 全 **per-instance** 解析——Entity 面缓存 Field/Method
     * 跨类复用是 TE 按类缓存同族坑（假类/真 Painting 非缓存声明类子类即抛
     * IllegalArgumentException），per-instance getField/findDeclaredMethod 走继承链。
     * healthF 双表已钉（0327=W/0618=Z 补钉批）；字段缺失（非 Living，如 Painting/Item）→省略
     * health。输出 `N=kind@x,y,z[h=H] ...`；
     * 空表→`none`。
     */
    public static String entityList(Object world) throws Exception {
        require("entityListM", M_ENTITY_LIST); // 两表已钉（双轨批）——未钉→UNSUPPORTED（spec §3）
        if (M_ENTITY_LIST_LOOKUP == null && M_ENTITY_TYPE == null) {
            require("entityTypeM", M_ENTITY_TYPE); // 命名面全无→响亮
        }
        Method lookup = entityLookup(world);
        Method listM = entityListM;
        if (listM == null) {
            try {
                listM = world.getClass().getMethod(M_ENTITY_LIST);
            } catch (NoSuchMethodException e) {
                throw new UnsupportedOp("entity list: " + e);
            }
            entityListM = listM;
        }
        List<?> es = (List<?>) listM.invoke(world);
        if (es.isEmpty()) {
            return "none";
        }
        StringBuilder b = new StringBuilder();
        for (Object e : es) {
            Class<?> ec = e.getClass();
            if (b.length() > 0) {
                b.append(' ');
            }
            b.append(resolveKind(lookup, ec, e)).append('@')
                    .append(fmt1(ec.getField(E_POS_X).getDouble(e))).append(',')
                    .append(fmt1(ec.getField(E_POS_Y).getDouble(e))).append(',')
                    .append(fmt1(ec.getField(E_POS_Z).getDouble(e)));
            if (E_HEALTH != null) {
                try {
                    int hp = ec.getField(E_HEALTH).getInt(e); // 先探测成功再写，防半截 "[h=" 残留
                    b.append("[h=").append(hp).append(']');
                } catch (NoSuchFieldException ignored) {
                    // 非 Living 实体（Painting 等）：省略 health
                }
            }
        }
        return "N=" + es.size() + "=" + b;
    }

    /** EntityList 静态查表惰性解析（描述符特征筛：static+String 返回+单参，零类名硬编码）。 */
    private static Method entityLookup(Object world) throws UnsupportedOp {
        if (G_ENTITY_LIST_CLASS == null) {
            return null; // 槽未钉（0327）——纯 fallback 链
        }
        Method m = entityListLookup;
        if (m != null) {
            return m;
        }
        Class<?> c;
        try {
            c = Class.forName(G_ENTITY_LIST_CLASS, false, world.getClass().getClassLoader());
        } catch (ClassNotFoundException e) {
            throw new UnsupportedOp("entity list lookup class: " + e);
        }
        for (Method cand : c.getMethods()) {
            if (cand.getName().equals(M_ENTITY_LIST_LOOKUP)
                    && java.lang.reflect.Modifier.isStatic(cand.getModifiers())
                    && cand.getReturnType() == String.class
                    && cand.getParameterTypes().length == 1) {
                m = cand;
                break;
            }
        }
        if (m == null) {
            throw new UnsupportedOp("entity list lookup: " + M_ENTITY_LIST_LOOKUP
                    + "(Entity)String on " + G_ENTITY_LIST_CLASS);
        }
        entityListLookup = m;
        return m;
    }

    /** kind 双轨：查表→per-instance→类简名。 */
    private static String resolveKind(Method lookup, Class<?> ec, Object e) throws Exception {
        if (lookup != null) {
            Object n = lookup.invoke(null, e);
            if (n != null) {
                return (String) n;
            }
        }
        if (M_ENTITY_TYPE != null) {
            Object n = findDeclaredMethod(ec, M_ENTITY_TYPE).invoke(e);
            if (n != null) {
                return (String) n;
            }
        }
        return ec.getSimpleName();
    }

    /**
     * 上溯 getDeclaredMethod 链（A3 改钉配套：0327 getEntityType=a()String 基类
     * protected abstract、TNT 覆写 protected final——getMethod 只认 public 必报
     * NoSuchMethod，与 setPosRot a3-0618-3 同模式；setAccessible 对 public 覆写零影响）。
     */
    private static Method findDeclaredMethod(Class<?> c, String name) throws NoSuchMethodException {
        for (Class<?> k = c; k != null; k = k.getSuperclass()) {
            try {
                Method m = k.getDeclaredMethod(name);
                m.setAccessible(true);
                return m;
            } catch (NoSuchMethodException ignored) {
                // 继续上溯
            }
        }
        throw new NoSuchMethodException(name + " on " + c.getName());
    }

    private static String fmt1(double v) {
        return String.valueOf(Math.round(v * 10.0) / 10.0);
    }

    /**
     * 播音效（op=playsound，瞬态——不进 NBT，save 忽略）。playSoundEffect(x,y,z,name,vol/10,pitch/10)
     * 客户端查表广播；不存在音效=静默 no-op（透传不建白名单，spec §1）。
     * vol∈[0,10]、pitch∈[0,20] 整数十分位（MiniJson 整数子集）。
     */
    public static String playSound(Object world, int x, int y, int z, String name,
                                   int vol, int pitch) throws Exception {
        require("playSoundM", M_PLAY_SOUND); // 0618 未钉→UNSUPPORTED（spec §3）
        if (name == null || name.isEmpty() || name.length() > 64) {
            throw new Rejected("bad-name");
        }
        if (vol < 0 || vol > 10) {
            throw new Rejected("vol-out-of-range [0,10]: " + vol);
        }
        if (pitch < 0 || pitch > 20) {
            throw new Rejected("pitch-out-of-range [0,20]: " + pitch);
        }
        Method m = playSoundM;
        if (m == null) {
            try {
                m = world.getClass().getMethod(M_PLAY_SOUND,
                        double.class, double.class, double.class,
                        String.class, float.class, float.class);
            } catch (NoSuchMethodException e) {
                throw new UnsupportedOp("playSoundEffect: " + e);
            }
            playSoundM = m;
        }
        m.invoke(world, Double.valueOf(x), Double.valueOf(y), Double.valueOf(z), name,
                Float.valueOf(vol / 10f), Float.valueOf(pitch / 10f));
        return "played " + name + " at " + x + "," + y + "," + z
                + " vol=" + vol + " pitch=" + pitch;
    }

    /**
     * 盔甲槽写（op=setarmor）。slot∈[0,4)；校验复用 give 面同序同串（count→dmg→bad-itemid）；
     * b[4] 数组元素**直写替换**；不校验 ItemArmor（注册表非 null 即放行=give 同戒律，
     * 非盔甲=无减伤不崩溃）。写侧随 "Inventory" NBT 持久（Slot 100..103）。
     */
    public static String setArmor(Object world, int slot, int itemId, int count, int damage)
            throws Exception {
        Object p = worldPlayer(world);
        if (p == null) {
            throw new Rejected("no-player");
        }
        if (slot < 0 || slot > 3) {
            throw new Rejected("slot-out-of-range [0,4): " + slot);
        }
        if (count < 1 || count > 64) {
            throw new Rejected("count-out-of-range [1,64]: " + count);
        }
        if (damage < 0 || damage > 32767) {
            throw new Rejected("dmg-out-of-range [0,32767]: " + damage);
        }
        resolveGiveFace(p); // 盔甲写需注册表校验=give 面（spec §5；0618 null→UNSUPPORTED）
        Object[] items = (Object[]) itemRegistry.get(null);
        if (itemId < 0 || itemId >= items.length || items[itemId] == null) {
            throw new Rejected("bad-itemid: " + itemId);
        }
        Object stack = newStack(itemId, count, damage);
        ((Object[]) invArmor.get(invField.get(p)))[slot] = stack;
        return "setarmor slot=" + slot + " <- " + itemId + " x" + count
                + (damage == 0 ? "" : " dmg=" + damage);
    }

    /** 背包清空（op=clearinv，**不可逆**——无 undo 面，spec §4）。主槽+盔甲全 null。写侧。 */
    public static String clearInv(Object world) throws Exception {
        Object p = worldPlayer(world);
        if (p == null) {
            throw new Rejected("no-player");
        }
        resolveInvFace(p); // 清空只用 main/armor 面（spec §5；0618 clearinv=OK 对榫）
        Object inv = invField.get(p);
        Object[] main = (Object[]) invMain.get(inv);
        for (int i = 0; i < main.length; i++) {
            main[i] = null;
        }
        Object[] armor = (Object[]) invArmor.get(inv);
        for (int i = 0; i < armor.length; i++) {
            armor[i] = null;
        }
        return "cleared " + main.length + "+" + armor.length;
    }

    /**
     * 作物强熟（op=grow，骨粉语义=官方强熟：一步满熟，绕过 updateTick 光门+概率逐阶）。
     * id=59：meta==7→NoChange（已熟）；否则 setMetaSilent+notifyBlock（setmeta 同配方——
     * 同 id 走 c() 被 Chunk 短路不通知）→OK 带旧 meta 供回滚。
     * id=6（sapling）：UnsupportedOp 响亮——0327 无树化能力（growTree 首现 Beta 1.2，spec §1）。
     * 其余 id：Rejected not-growable。
     */
    public static String growCrops(Object world, int x, int y, int z) throws Exception {
        // 档位一（spec §3）：id 三件+通知段任一未钉→UNSUPPORTED（0618 五槽全 null）
        require("idCrops", ID_CROPS);
        require("idSapling", ID_SAPLING);
        require("cropsRipeMeta", CROPS_RIPE_META);
        require("setMetaSilentM", M_SET_META_SILENT);
        require("observersF", F_OBSERVERS);
        require("light6M", M_LIGHT6);
        int id = getBlockId(world, x, y, z);
        if (id == ID_SAPLING.intValue()) {
            throw new UnsupportedOp(
                    "sapling->tree not a " + T.versionId + " capability (introduced Beta 1.2)");
        }
        if (id != ID_CROPS.intValue()) {
            throw new Rejected("not growable: id=" + id);
        }
        int old = ((Integer) getMeta.invoke(world, Integer.valueOf(x), Integer.valueOf(y),
                Integer.valueOf(z))).intValue();
        if (old == CROPS_RIPE_META.intValue()) {
            throw new NoChange("crops already ripe meta=" + CROPS_RIPE_META
                    + " at " + x + "," + y + "," + z);
        }
        setMetaSilent.invoke(world, Integer.valueOf(x), Integer.valueOf(y), Integer.valueOf(z),
                CROPS_RIPE_META);
        notifyBlock(world, x, y, z, id);
        return "grown " + ID_CROPS + " meta " + old + "->" + CROPS_RIPE_META;
    }

    /**
     * 清地面物（op=clear，EntityItem-only）。setEntityDead=World.b(Entity)V **static** 惰性解析
     * （签名不符→UnsupportedOp 响亮；仅置 isDead，真移除在 tick——i()List 活表遍历零 CME）；
     * 实体源=World i()List（entitylist 同通路）。无坐标=全表；cx/cy/cz/rad=仅水平距离
     * （格中心 cx+0.5/cz+0.5，同 drop 位置语义）≤rad 且垂直≤rad 的圆柱过滤。
     * 不动玩家、不动其他实体类（背包=clearinv 边界）。0327 saveWorld 不写实体⇒恒 transient。
     * N==0→NoChange("cleared 0")。
     */
    public static String clearGroundItems(Object world, Integer cx, Integer cy, Integer cz,
                                          Integer rad) throws Exception {
        require("setEntityDeadM", M_SET_ENTITY_DEAD); // 0618 未钉→UNSUPPORTED（spec §3）
        require("entityListM", M_ENTITY_LIST);
        Method kill = setEntityDeadM;
        if (kill == null) {
            try {
                kill = world.getClass().getMethod(M_SET_ENTITY_DEAD, entityClass);
            } catch (NoSuchMethodException e) {
                throw new UnsupportedOp("setEntityDead: " + e);
            }
            setEntityDeadM = kill;
        }
        Class<?> itemClass = entityItemClass;
        if (itemClass == null) {
            try {
                itemClass = Class.forName(G_ITEM_ENTITY, false, world.getClass().getClassLoader());
            } catch (ClassNotFoundException e) {
                throw new UnsupportedOp("item entity class: " + e);
            }
            entityItemClass = itemClass;
        }
        boolean radius = cx != null;
        if (radius) {
            if (cy == null || cz == null || rad == null) {
                throw new Rejected("x/y/z/r must come together");
            }
            validateCoords(cx.intValue(), cy.intValue(), cz.intValue());
            if (rad.intValue() < 0 || rad.intValue() > 64) {
                throw new Rejected("r-out-of-range [0,64]: " + rad.intValue());
            }
        }
        Method listM = entityListM;
        if (listM == null) {
            try {
                listM = world.getClass().getMethod(M_ENTITY_LIST);
            } catch (NoSuchMethodException e) {
                throw new UnsupportedOp("entity list: " + e);
            }
            entityListM = listM;
        }
        List<?> es = (List<?>) listM.invoke(world);
        int n = 0;
        for (Object e : es) {
            if (!itemClass.isInstance(e)) {
                continue;
            }
            if (radius) {
                double dx = ePosX.getDouble(e) - (cx.intValue() + 0.5);
                double dy = ePosY.getDouble(e) - cy.intValue();
                double dz = ePosZ.getDouble(e) - (cz.intValue() + 0.5);
                double horiz = Math.sqrt(dx * dx + dz * dz);
                if (horiz > rad.doubleValue() || Math.abs(dy) > rad.doubleValue()) {
                    continue;
                }
            }
            // 档位二（a1.1.2_01）：0327/0629=static b(kh)V 收 null；a1.1.2_01=**实例** d(kh)V 收 world
            kill.invoke(java.lang.reflect.Modifier.isStatic(kill.getModifiers()) ? null : world, e);
            n++;
        }
        if (n == 0) {
            throw new NoChange("cleared 0");
        }
        return "cleared " + n + " ground items"
                + (radius ? " within r=" + rad.intValue() + " of "
                        + cx.intValue() + "," + cy.intValue() + "," + cz.intValue() : "");
    }

    /**
     * 区域条件替换（op=replace）。fill 面复用：normalizeRegion（cap 4096）+validateBlockId；
     * oldId==newId→Rejected `same-id`；仅 getBlock==oldId 的格 c(IIII)Z 重写；
     * 零变更→NoChange（fill 同语义，NO_CHANGE 跳 save）。
     */
    public static String replace(Object world, int x1, int y1, int z1, int x2, int y2, int z2,
                                 int oldId, int newId) throws Exception {
        validateBlockId(oldId);
        validateBlockId(newId);
        if (oldId == newId) {
            throw new Rejected("same-id: " + oldId);
        }
        int[] r = normalizeRegion(x1, y1, z1, x2, y2, z2);
        int changed = 0;
        for (int y = r[1]; y <= r[4]; y++) {
            for (int x = r[0]; x <= r[3]; x++) {
                for (int z = r[2]; z <= r[5]; z++) {
                    Integer id = (Integer) getBlock.invoke(world,
                            Integer.valueOf(x), Integer.valueOf(y), Integer.valueOf(z));
                    if (id.intValue() == oldId) {
                        Boolean ok = (Boolean) setBlock.invoke(world, Integer.valueOf(x),
                                Integer.valueOf(y), Integer.valueOf(z), Integer.valueOf(newId));
                        if (ok.booleanValue()) {
                            changed++;
                        }
                    }
                }
            }
        }
        String detail = "replaced [" + r[0] + ".." + r[3] + "] [" + r[1] + ".." + r[4] + "] ["
                + r[2] + ".." + r[5] + "] " + oldId + "->" + newId + " changed=" + changed;
        if (changed == 0) {
            throw new NoChange(detail);
        }
        return detail;
    }

    /** 方块元数据读（op=getmeta）。World.d(III)I=getBlockMetadata（Chunk nibble g，纯读）。 */
    public static int getMeta(Object world, int x, int y, int z) throws Exception {
        validateCoords(x, y, z);
        return ((Integer) getMeta.invoke(world, x, y, z)).intValue();
    }

    /**
     * 方块元数据写（op=setmeta）。b(IIII)V 静默写（取证：游戏 sapling 同面）+ 手动复刻
     * c(IIII)Z 通知段——同 id 走 c() 被 Chunk 短路不通知，必须观察者 a(III)V + e(IIII)V 光照 6 邻。
     */
    public static String setMeta(Object world, int x, int y, int z, int meta) throws Exception {
        require("setMetaSilentM", M_SET_META_SILENT); // 0618 未钉→UNSUPPORTED（spec §3）
        require("observersF", F_OBSERVERS);
        require("light6M", M_LIGHT6);
        validateCoords(x, y, z);
        if (meta < 0 || meta > 15) {
            throw new Rejected("meta-out-of-range: " + meta);
        }
        int old = ((Integer) getMeta.invoke(world, x, y, z)).intValue();
        setMetaSilent.invoke(world, x, y, z, meta);
        notifyBlock(world, x, y, z, getBlockId(world, x, y, z));
        return "meta " + old + " -> " + meta;
    }

    /** 亮度读（op=getlight）。World.f(III)I=getBrightness（纯读，0-15；光照不进存盘对账——由方块表实时算）。 */
    public static int getLightValue(Object world, int x, int y, int z) throws Exception {
        require("getLightM", M_GET_LIGHT); // 0618 未钉→UNSUPPORTED（spec §3）
        validateCoords(x, y, z);
        return ((Integer) getLight.invoke(world, x, y, z)).intValue();
    }

    /** c(IIII)Z 通知段复刻：World.u 观察者 a(III)V（渲染标记）+ e(IIII)V 光照 6 邻。 */
    private static void notifyBlock(Object world, int x, int y, int z, int id) throws Exception {
        List<?> obs = (List<?>) observers.get(world);
        for (Object o : obs) {
            Method m;
            try {
                m = o.getClass().getMethod("a", int.class, int.class, int.class);
            } catch (NoSuchMethodException e) {
                throw new UnsupportedOp("observer notify: " + e);
            }
            m.invoke(o, x, y, z);
        }
        light6.invoke(world, x, y, z, id);
    }

    // genscatter 惰性解析（getplayer W 同模式）：生成器类不在 World 签名面，错版响亮 UnsupportedOp
    private static volatile Constructor<?> scatterCtor;
    private static volatile Method scatterGen;

    private static void resolveScatter(Class<?> wc) throws UnsupportedOp {
        if (scatterGen != null) {
            return;
        }
        try {
            Class<?> gc = Class.forName(G_SCATTER_CLASS, false, wc.getClassLoader());
            Constructor<?> ct = gc.getConstructor(int.class);
            Method gen = gc.getMethod("a", wc, Random.class, int.class, int.class, int.class);
            scatterCtor = ct;
            scatterGen = gen;
        } catch (ClassNotFoundException e) {
            throw new UnsupportedOp("scatter generator: " + e);
        } catch (NoSuchMethodException e) {
            throw new UnsupportedOp("scatter generator: " + e);
        }
    }

    /**
     * 石头基底散布（op=genscatter）。调真生成器 a.a.c.b.b(id).a(f,Random,III)Z——0327 唯一原生
     * "结构生成"（椭球曲线把 id 撒在 **stone** 上；meta-1 实跑翻案：该类实为原版矿石团生成器，
     * 基底检查 x.i=r(1,1) id=1 石头而非草地；名 WorldGenTrees 系误，sapling 在 0327 不长树）。
     * 游戏写=静默（地形期语义），运行时补通知：预扫影响盒取真实上界
     * [x+4..x+12]×[y..y+5]×[z+4..z+12]（486 格；中心线 [x+6..x+10]×[y+2..y+4]×[z+6..z+10]
     * 加椭球半径 (sin+1)*rnd+1∈[1,3) 半轴，(int) 截断向零）→复扫 diff→变更格逐个通知。
     * 零变更（盒内无石头）→NoChange（NO_CHANGE 第二生产者）。
     */
    public static String genScatter(Object world, int id, int x, int y, int z, Long seed)
            throws Exception {
        require("scatterClassG", G_SCATTER_CLASS); // 0618 未钉→UNSUPPORTED（spec §3）
        validateBlockId(id);
        validateCoords(x, y, z);
        resolveScatter(world.getClass());
        Object gen = scatterCtor.newInstance(new Integer(id));
        int x0 = x + 4;
        int x1 = x + 12;
        int y0 = y;
        int y1 = y + 5;
        int z0 = z + 4;
        int z1 = z + 12;
        int n = (x1 - x0 + 1) * (y1 - y0 + 1) * (z1 - z0 + 1);
        int[] before = new int[n];
        int idx = 0;
        for (int cy = y0; cy <= y1; cy++) {
            for (int cx = x0; cx <= x1; cx++) {
                for (int cz = z0; cz <= z1; cz++) {
                    before[idx++] = getBlockId(world, cx, cy, cz);
                }
            }
        }
        Random rnd = (seed == null) ? new Random() : new Random(seed.longValue());
        scatterGen.invoke(gen, world, rnd, x, y, z);
        int changed = 0;
        idx = 0;
        for (int cy = y0; cy <= y1; cy++) {
            for (int cx = x0; cx <= x1; cx++) {
                for (int cz = z0; cz <= z1; cz++) {
                    int now = getBlockId(world, cx, cy, cz);
                    if (now != before[idx]) {
                        changed++;
                        notifyBlock(world, cx, cy, cz, now);
                    }
                    idx++;
                }
            }
        }
        String detail = "scattered id=" + id + " changed=" + changed
                + " at x=" + x + " y=" + y + " z=" + z
                + (seed == null ? "" : " seed=" + seed.longValue());
        if (changed == 0) {
            throw new NoChange(detail);
        }
        return detail;
    }

    public static int getBlockId(Object world, int x, int y, int z) throws Exception {
        return ((Integer) getBlock.invoke(world, x, y, z)).intValue();
    }

    /** 放置/挖除（blockId=0 即挖除——与游戏自身删除路径同入口，PlayerController L27 实锤）。 */
    public static boolean setBlock(Object world, int x, int y, int z, int blockId) throws Exception {
        return ((Boolean) setBlock.invoke(world, x, y, z, blockId)).booleanValue();
    }

    public static void saveWorld(Object world) throws Exception {
        // 档位二（spec §3）：0327 a(Z)V 传 FALSE=现行为；0618 f()V 零参；
        // 0629 a(Liw;)V 传 null（el0629-2 活体翻案：iw=具体类非接口，Proxy 作废；
        // 本体 a(boolean,iw) 有完整 null 守卫 ifnull@13/25，游戏构造器尾 490 处自身裸传 null——
        // 包装恒直调本体，null=无进度 UI，语义正当）
        Class<?>[] pt = saveWorld.getParameterTypes();
        if (pt.length == 0) {
            saveWorld.invoke(world);
        } else if (pt[0] == boolean.class) {
            saveWorld.invoke(world, Boolean.FALSE);
        } else {
            saveWorld.invoke(world, (Object) null);
        }
    }

    /** 参数校验（案卷 §7 戒律：世界边界 y∈[1,127]，x/z ±32000000）。 */
    public static void validateCoords(int x, int y, int z) throws Rejected {
        if (y < 1 || y > 127) {
            throw new Rejected("y out of range [1,127]: " + y);
        }
        if (x < -32000000 || x > 32000000 || z < -32000000 || z > 32000000) {
            throw new Rejected("x/z out of world bounds");
        }
    }

    public static void validateBlockId(int id) throws Rejected {
        if (id < 0 || id > 255) {
            throw new Rejected("blockId out of range [0,255]: " + id);
        }
    }

    /** invoke 异常解包：游戏方法内抛出的异常映射 ERROR 文本。 */
    public static String unwrap(InvocationTargetException e) {
        Throwable c = e.getTargetException();
        return (c == null ? e : c).toString();
    }
}
