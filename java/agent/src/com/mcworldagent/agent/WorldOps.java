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

    // 0327 写侧实名（与 FirstBrick 一致；多版本化时移入登记表）
    private static final String M_SET_BLOCK = "c";   // setBlockWithNotify (IIII)Z public final
    private static final String M_GET_BLOCK = "a";   // getBlockId (III)I public final
    private static final String M_SAVE_WORLD = "a";  // saveWorld (Z)V private

    // 0327 读侧实名（Query 取证：saveWorld NBT 键绑定 + h() 每 tick b+1 且 b%200 触发 autosave）
    // 写侧同源（settime/setspawn 取证，全量 javap 303 类）：
    //   b 外部读点仅 Chunk 存盘 LastUpdate；渲染每帧 client.a.b.a(lerp)→f.b(F)F→glRotatef 实时算，零缓存
    //   g/h/i 唯一读点 = EntityPlayer 构造器 setPositionAndRotation——下次进世界生效，无通知面
    private static final String F_WORLD_TIME = "b";  // public long worldTime（NBT "Time"）
    private static final String F_RAND_SEED = "x";   // private long randSeed（NBT "RandomSeed"）
    private static final String F_SPAWN_X = "g";     // public int spawnX（NBT "SpawnX"）
    private static final String F_SPAWN_Y = "h";     // public int spawnY（NBT "SpawnY"）
    private static final String F_SPAWN_Z = "i";     // public int spawnZ（NBT "SpawnZ"）

    // tp 侧实名（javap 取证，Entity 基类 net.minecraft.a.c.b；全量反汇编 303 类）：
    //   World.c public Entity 字段 = 活体玩家（EntityPlayer 构造器自设 world.c=this，零新钩子）
    //   d,e,f=lastTick / g,h,i=pos / j,k,l=motion / m,n=yaw,pitch（public double/float）
    //   b(DDDFF)V=setPositionAndRotation：d=g=x; e=h=y+u(眼偏移1.62); f=i=z; 末尾 a(DDD) 重建 AABB
    //   = 游戏 NBT 载入（Entity.d←Player.Pos 后调 b）的同一配方；mover 每 tick 自愈重算 pos.y
    private static final String F_WORLD_PLAYER = "c"; // public Entity player（World 字段）
    private static final String M_SET_POS_ROT = "b";  // setPositionAndRotation (DDDFF)V public final
    private static final String E_POS_X = "g";
    private static final String E_POS_Y = "h";
    private static final String E_POS_Z = "i";
    private static final String E_MOT_X = "j";
    private static final String E_MOT_Y = "k";
    private static final String E_MOT_Z = "l";
    private static final String E_YAW = "m";
    private static final String E_PITCH = "n";

    // getplayer 面（javap 取证，EntityLiving=a.c.e）：W=Health（public int，NBT "Health" 双向绑定实锤，默认 10）。
    // W 在子类，World.c 声明类型=Entity 基类——execute 期按实例类惰性解析（失败→UnsupportedOp，不静默）。
    private static final String E_HEALTH = "W";

    // 物品栏面（give/inv/sethealth 取证，javap 全量反汇编定向检索）：
    //   EntityHuman=a.c.e.a public 字段 b=InventoryPlayer(a.c.e.b)：a:[ItemStack]=36 主槽、
    //   b:[ItemStack]=4 盔甲槽、c:I=当前快捷槽(0..9)；a(ItemStack)Z=addStack（同 id 且 dmg==0
    //   才合并、单栈上限 64、入包后设 k.b=5 拾取延迟；false=装不下）；EntityPlayer=client.g.a
    //   不 shadow 本批任何字段。
    //   ItemStack=net.minecraft.a.b.k：a:I=Count、b:I=拾取延迟、c:I=id、d:I=Damage；
    //   构造器 (II)V=(id,count)、(III)V=(id,count,damage)；a()Li;=i.b[c] 直索引
    //   （id 越界/空洞→后续 NPE，故 give 必须注册表校验）。
    //   Item 注册表=net.minecraft.a.b.i：public static i[] b 按 id 索引（部分槽位 null）。
    //   持久化：EntityPlayer.a(NBT) 写 "Inventory"（主槽 Slot 0..35、盔甲 100..103）→save 即落盘。
    //   sethealth 直写 W 绕过 b(I)V 治疗钳制（20 上限；死亡中不治疗）→op 侧自校验 0..20；
    //   HUD client.a_d 每帧直读 W→即时生效；置 0=deathTicks 流程接管（玩家 k() 覆写空操作、
    //   死亡屏 client.d 自动弹出、重生=原版按钮）→agent 不代打 respawn。
    //   全惰性解析（eHealth 同模式）：execute 期按实例类解析，NoSuch*→UnsupportedOp 不静默；
    //   resolve(Class) 主段零改动（World 类签名不变）。
    private static final String F_INV = "b";        // EntityHuman.b（InventoryPlayer）
    private static final String INV_MAIN = "a";     // ItemStack[36] 主槽
    private static final String INV_ARMOR = "b";    // ItemStack[4] 盔甲槽
    private static final String INV_CUR = "c";      // 当前快捷槽
    private static final String IS_COUNT = "a";     // ItemStack.Count
    private static final String IS_ID = "c";        // ItemStack id
    private static final String IS_DMG = "d";       // ItemStack Damage
    private static final String M_ADDSTACK = "a";   // addStack (ItemStack)Z
    private static final String G_ITEM_CLASS = "net.minecraft.a.b.i";
    private static final String ITEMS = "b";        // public static i[] 按 id 索引

    // 实体面（spawn-explode-drop 批取证，反编译源码树语义 + javap 混淆指纹双路交叉）：
    //   World a(Lnet/minecraft/a/c/b;DDDF)V public final=createExplosion(Entity source,x,y,z,float size)
    //   （Explosion 类=空标记，逻辑全在此：16³ 壳射线采样+方块阻力→HashSet 破坏集→区域实体伤害击退
    //   →playSoundEffect("random.explode")）；source=null 零 NPE（区域取实体遍历 `entity5!=entity1`
    //   判等实锤 L1180）——但**不豁免玩家**，实跑必须远距离。
    //   World a(Lnet/minecraft/a/c/b;)V public final=spawnEntityInWorld(Entity)=loadedEntityList.add——
    //   主循环每 tick 遍历该表，加入即活零新钩；Entity(World) 构造仅 set worldObj+pos(0,0,0) 零副作用
    //   （源码 L68-71），配方=构造→b(DDDFF)V 定位（tp 轮已钉）→spawn。
    //   **0327 saveWorld 不写实体**（Infdev 特性）——spawn/drop 恒临时态，存退即失；explode 破坏方块持久。
    //   混淆实体类表（字符串常量判决：贴图路径/实体名/getEntityName）：
    //   pig=a.c.b.c、sheep=a.c.b.b、creeper=a.c.a.d、skeleton=a.c.a.c、zombie=a.c.a.f、spider=a.c.a.b
    //   （六 mob 构造器全 (World)V）；tnt=a.c.c.a（(World,FFF)V，fuse=80 内置 bipush 实锤）；
    //   item=a.c.c.b（(World,DDD,ItemStack)V）；giant=a.c.a.a 无 (World)V 构造器→排除；
    //   arrow/painting 面留后续取证（shooter/朝向+EnumArt），不进 v1 白名单。
    private static final String M_EXPLODE = "a";       // createExplosion (Entity,DDDF)V
    private static final String M_SPAWN_ENTITY = "a";  // spawnEntityInWorld (Entity)V
    private static final String G_ITEM_ENTITY = "net.minecraft.a.c.c.b"; // EntityItem

    // invulnerable 面（sed-1 批前用户判决"玩家不死先"，源码+javap 双路实锤）：
    //   EntityPlayer.attackEntityFrom L188 `heartsLife > heartsHalvesLife/2(=10) → return false`
    //   =**全伤害源拒绝**——伤害入口全搜实锤：怪物近战/箭(EntityArrow L127)/火焰(EntityLiving L95)/
    //   坠落(L300)/虚空(Entity L150/157/346)/爆炸全部走 attackEntityFrom(Entity,int) 单入口。
    //   heartsLife=Entity(a.c.b) public int K（原版 i-frame 计时器，EntityLiving L112 每 tick>0 自减 1，
    //   正常受击 L233 重置 20=1 秒无敌帧）；写 K≥11=免疫窗口，写 0=取消；HUD 副作用=心闪烁
    //   （GuiIngame L47 读同字段，仅视觉噪音）。瞬态：不进 NBT，save 无作用。
    //   难度判决（sed-1 实跑前置）：EntityMonster.onUpdate L27 `difficultySetting==0→setEntityDead()`
    //   =和平抹杀敌对（spawn 四怪首 tick 即死）；World.d 每帧被 gameSettings.difficulty 覆写（Minecraft
    //   L763）写 d 无效——切换=启动前 options.txt difficulty:1（Easy，GameSettings L142 读入实锤）。
    //   Giant 更正：源码树有 EntityGiant(World) 构造器但**运行时 jar a.c.a.a 仅默认无参构造**
    //   （javap 全量复核，worldObj=null 首 tick NPE）——排除维持；教训=源码树与 jar 可能不同 build，
    //   构造器面以运行时 jar 为准。
    private static final String E_IFRAMES = "K";       // Entity.heartsLife（public int）

    // tileentity 面（tileentity-batch 批取证，源码树+javap 双路）：
    //   TE 基类=a.a.b.a.a（a=World、b/c/d=x/y/z）；Chest=a.a.b.a.c（27 槽，构造覆盖声明面 36）、
    //   Furnace=a.a.b.a.b（3 槽：0 料/1 燃/2 产；f=BurnTime、h=CookTime NBT 对榫实锤、
    //   g=currentItemBurnTime 不进 NBT；点燃=updateEntity 自动吞 slot1（wood 300t/煤 1600t），
    //   cook 满 200 冶炼，点燃态翻转 61↔62 换块重挂 TE）。
    //   IInventory=a.b：a()I=size、a(I)Lk;=getStack、a(ILk;)V=setStack（**直替换无合并**）——
    //   三方法直接从**实例类** getMethod（不取接口对象：假 IInventory 与注册表包目录 a/b/
    //   冲突=javac 结构限制，mob 基类同案；无头/活体同构）。
    //   Furnace getInvName 返回 "Chest"=原版笔误——类型判一律字段/槽数面，不用名字。
    //   **TE 创建/删除=游戏自动**：Chunk.setTile L194 调 onBlockAdded→BlockContainer 建 TE、
    //   onBlockRemoval 删——place(54/61)/dig 零手动。
    //   **TE 持久化随 chunk**（Chunk.save "TileEntities"+Items+BurnTime/CookTime）——
    //   与实体不落盘形成对照（te-1 断言点）。World h(III)=getBlockTileEntity（chunk 缺失→null）。
    private static final String M_TE_GET = "h";         // getBlockTileEntity (III)La.a.b.a.a;
    private static final String M_INV_SIZE = "a";       // IInventory.getSize ()I
    private static final String M_INV_GET = "a";        // getStackInSlot (I)Lk;
    private static final String M_INV_SET = "a";        // setInventorySlotContents (I,Lk;)V
    private static final String F_TE_BURN = "f";        // TileEntityFurnace.furnaceBurnTime
    private static final String F_TE_COOK = "h";        // TileEntityFurnace.furnaceCookTime
    private static final Map<String, String> MOB_CLASSES = new HashMap<String, String>();

    static {
        MOB_CLASSES.put("pig", "net.minecraft.a.c.b.c");
        MOB_CLASSES.put("sheep", "net.minecraft.a.c.b.b");
        MOB_CLASSES.put("creeper", "net.minecraft.a.c.a.d");
        MOB_CLASSES.put("skeleton", "net.minecraft.a.c.a.c");
        MOB_CLASSES.put("zombie", "net.minecraft.a.c.a.f");
        MOB_CLASSES.put("spider", "net.minecraft.a.c.a.b");
        MOB_CLASSES.put("tnt", "net.minecraft.a.c.c.a");
        // giant=a.c.a.a：运行时 jar 无 <init>（混淆管线剥死代码构造器）——由 NEW_CTOR
        // 受限构造器补全复活（HookTargets GIANT_CTOR，transform 日志 CTOR-INJECTED 对账）；
        // 复活后=RenderGiantZombie scale 6.0 的 10 倍巨僵尸（EntityMonster 子类，和平即死面同）。
        MOB_CLASSES.put("giant", "net.minecraft.a.c.a.a");
        // mob=a.c.c=EntityCreature 基类（用户判决"非正常生物但可生成"）：构造器健在免注入；
        // 渲染走 getEntityRenderObject 一级父类回退→EntityLiving=ModelBiped 人形（无 NPE）。
        MOB_CLASSES.put("mob", "net.minecraft.a.c.c");
    }

    // meta/genscatter 面（javap 取证，World L588/L637/L863 + a.a.c.b.b 散布器）：
    //   d(III)I=getBlockMetadata（Chunk.b(III)I 读 nibble g，索引 x<<11|z<<7|y）；
    //   b(IIII)V=静默 meta 写（h(IIII)Z→Chunk.b(IIII)V，仅 nibble+dirty，零通知——
    //   sapling 生长即此面；可读树名 "setBlockMetadataWithNotify" 系 deobf 误猜）；
    //   e(IIII)V=光照 6 邻（逐邻 i()→Block.b(f,IIII)=updateLighting，第 4 参 id 被 i 忽略）；
    //   u=private List 观察者（渲染标记 a(III)V）；
    //   a.a.c.b.b(int)=椭球石头散布器（meta-1 实跑翻案：原版矿石团生成器，c_a 以 id 14/15/16/aj
    //   调用；命中格仅当现为 stone(x.i=r(1,1) id=1) 时静默 a(IIII)Z 写入，恒返 true）；
    //   World.d:I=difficultySetting（主循环每帧=GameSettings.difficulty，客户端全局不进 level.dat）。
    private static final String M_GET_META = "d";
    private static final String M_SET_META_SILENT = "b";
    private static final String M_LIGHT6 = "e";
    // getlight 面（meta 轮翻案钉死）：World.f(III)I=getBrightness——Chunk.c(IIII)I 读
    // h=SkyLight 减环境值 World.q 后与 i=BlockLight 取 max，纯读返回 0-15（旧"气温"笔记作废）。
    private static final String M_GET_LIGHT = "f";
    private static final String F_OBSERVERS = "u";
    private static final String F_DIFFICULTY = "d";
    private static final String G_SCATTER_CLASS = "net.minecraft.a.a.c.b.b";

    // 区域批量写读上限（fill/getblocks 共用）：16³=4096。
    // 取证：v1 fill=循环 c(IIII)Z（place/dig 同路径）；Chunk.setTile 内部已含 heightmap 重算
    // （g→f→world.d 观察者区间通知）+tile 移除+onPlace+isModified 脏位，快路径（a(IIII)Z）省不出数量级。
    // 副作用实锤：provider b(II) 对未加载坐标就地生成整 chunk——cap 即 tick 预算限幅，fill-1 计时后议调。
    public static final int MAX_REGION_VOLUME = 4096;

    private static volatile Method setBlock;
    private static volatile Method getBlock;
    private static volatile Method saveWorld;
    private static volatile Field randSeed; // 仅缓存 setAccessible 结果
    private static volatile Field worldTime;
    private static volatile Field spawnX;
    private static volatile Field spawnY;
    private static volatile Field spawnZ;
    private static volatile Field player;      // World.c（类型=Entity 基类，活体玩家）
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
    private static volatile Method getMeta;
    private static volatile Method setMetaSilent;
    private static volatile Method light6;
    private static volatile Method getLight;
    private static volatile Field observers;  // World.u private List
    private static volatile Field difficulty; // World.d public int

    /** 签名探测（可在任意线程调用；NoSuchMethod/NoSuchField → UnsupportedOp）。 */
    static synchronized void resolve(Class<?> wc) throws UnsupportedOp {
        if (setBlock != null) {
            return;
        }
        try {
            Method sb = wc.getMethod(M_SET_BLOCK, int.class, int.class, int.class, int.class);
            Method gb = wc.getMethod(M_GET_BLOCK, int.class, int.class, int.class);
            Method sv = wc.getDeclaredMethod(M_SAVE_WORLD, boolean.class);
            sv.setAccessible(true);
            Field t = wc.getField(F_WORLD_TIME);
            Field sd = wc.getDeclaredField(F_RAND_SEED);
            sd.setAccessible(true);
            Field sx = wc.getField(F_SPAWN_X);
            Field sy = wc.getField(F_SPAWN_Y);
            Field sz = wc.getField(F_SPAWN_Z);
            // tp 面：World.c 的类型即 Entity 基类，公开成员 getField/getMethod 直取
            Field pl = wc.getField(F_WORLD_PLAYER);
            Class<?> ec = pl.getType();
            Method spr = ec.getMethod(M_SET_POS_ROT,
                    double.class, double.class, double.class, float.class, float.class);
            Field pgx = ec.getField(E_POS_X);
            Field pgy = ec.getField(E_POS_Y);
            Field pgz = ec.getField(E_POS_Z);
            Field pjx = ec.getField(E_MOT_X);
            Field pjy = ec.getField(E_MOT_Y);
            Field pjz = ec.getField(E_MOT_Z);
            Field pYaw = ec.getField(E_YAW);
            Field pPitch = ec.getField(E_PITCH);
            // meta/genscatter/difficulty 面
            Method gm = wc.getMethod(M_GET_META, int.class, int.class, int.class);
            Method sm = wc.getMethod(M_SET_META_SILENT, int.class, int.class, int.class, int.class);
            Method l6 = wc.getMethod(M_LIGHT6, int.class, int.class, int.class, int.class);
            Method gl = wc.getMethod(M_GET_LIGHT, int.class, int.class, int.class);
            Field ob = wc.getDeclaredField(F_OBSERVERS);
            ob.setAccessible(true);
            Field df = wc.getField(F_DIFFICULTY);
            setBlock = sb;
            getBlock = gb;
            saveWorld = sv;
            randSeed = sd;
            worldTime = t;
            spawnX = sx;
            spawnY = sy;
            spawnZ = sz;
            player = pl;
            setPosRot = spr;
            ePosX = pgx;
            ePosY = pgy;
            ePosZ = pgz;
            eMotX = pjx;
            eMotY = pjy;
            eMotZ = pjz;
            eYaw = pYaw;
            ePitch = pPitch;
            getMeta = gm;
            setMetaSilent = sm;
            light6 = l6;
            getLight = gl;
            observers = ob;
            difficulty = df;
        } catch (NoSuchMethodException e) {
            throw new UnsupportedOp("world signatures: " + e);
        } catch (NoSuchFieldException e) {
            throw new UnsupportedOp("world fields: " + e);
        }
    }

    /** 世界信息快照（op=getinfo，只读）：tick 边界执行=一致读。 */
    public static String getInfo(Object world) throws Exception {
        long time = worldTime.getLong(world);
        long seed = randSeed.getLong(world);
        int sx = spawnX.getInt(world);
        int sy = spawnY.getInt(world);
        int sz = spawnZ.getInt(world);
        int diff = difficulty.getInt(world);
        return "time=" + time + " spawnX=" + sx + " spawnY=" + sy + " spawnZ=" + sz
                + " seed=" + seed + " difficulty=" + diff;
    }

    /** 设置 worldTime（op=settime）。渲染每帧经 b(F) 实时读，无通知面；返回旧值供回滚。 */
    public static long setTime(Object world, long time) throws Exception {
        long old = worldTime.getLong(world);
        worldTime.setLong(world, time);
        return old;
    }

    /** 设置出生点（op=setspawn）。唯一消费方=EntityPlayer 构造器，下次进世界生效。 */
    public static void setSpawn(Object world, int x, int y, int z) throws Exception {
        spawnX.setInt(world, x);
        spawnY.setInt(world, y);
        spawnZ.setInt(world, z);
    }

    /**
     * 传送玩家（op=tp）。复刻游戏 NBT 载入配方（Entity.d←Pos 后调 b(DDDFF)）：
     * 直写 pos=块中心(x+0.5,y,z+0.5)、清 motion（防残留速度拉扯），
     * 再经 setPositionAndRotation 同步 lastTick(d,e,f) 与重建 AABB——与"重进世界恢复位置"同路径。
     * 注意 b() 内部 h=y+u（u=眼偏移 1.62）：脚落 y 平面、眼在 y+1.62，载入配方即如此。
     * 返回旧位供回滚记录。
     */
    public static String tp(Object world, int x, int y, int z) throws Exception {
        Object p = player.get(world);
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
        setPosRot.invoke(p, Double.valueOf(nx), Double.valueOf(ny), Double.valueOf(nz),
                Float.valueOf(yaw), Float.valueOf(pitch));
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

    /** 玩家快照（op=getplayer，只读）：pos/motion/rot（tp 轮已 resolve 字段）+ Health（惰性解析 W）。 */
    public static String getPlayer(Object world) throws Exception {
        Object p = player.get(world);
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
        Object p = player.get(world);
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
        Object p = player.get(world);
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

    /** 物品栏面惰性解析（eHealth 同模式）：ItemStack 类取自主槽数组分量类型，零硬编码类名。 */
    private static void resolveInventory(Object p) throws UnsupportedOp {
        if (addStack != null) {
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
            Method add = invType.getMethod(M_ADDSTACK, stackType);
            Field cnt = stackType.getField(IS_COUNT);
            Field id = stackType.getField(IS_ID);
            Field dmg = stackType.getField(IS_DMG);
            Constructor<?> c2 = stackType.getConstructor(int.class, int.class);
            Constructor<?> c3 = stackType.getConstructor(int.class, int.class, int.class);
            Class<?> itemClass = Class.forName(G_ITEM_CLASS, false, p.getClass().getClassLoader());
            Field reg = itemClass.getField(ITEMS);
            invField = inv;
            invMain = main;
            invArmor = armor;
            invCur = cur;
            addStack = add;
            isCount = cnt;
            isId = id;
            isDmg = dmg;
            isCtor2 = c2;
            isCtor3 = c3;
            itemRegistry = reg;
        } catch (NoSuchFieldException e) {
            throw new UnsupportedOp("inventory fields: " + e);
        } catch (NoSuchMethodException e) {
            throw new UnsupportedOp("inventory signatures: " + e);
        } catch (ClassNotFoundException e) {
            throw new UnsupportedOp("item registry class: " + e);
        }
    }

    /**
     * 给予物品（op=give）。注册表 i.b[] 直索引校验（越界/空洞→游戏侧 a() 即 NPE，必须拒
     * bad-itemid）+ count∈[1,64] + damage∈[0,32767]；damage==0 走 (II)V 否则 (III)V；
     * addStack false=装不下→Rejected inventory-full（原栈未消耗，可 inv 对账）。
     * save 后经 EntityPlayer "Inventory" NBT 落盘（取证：主槽 Slot 0..35、盔甲 100..103）。
     */
    public static String give(Object world, int itemId, int count, int damage) throws Exception {
        Object p = player.get(world);
        if (p == null) {
            throw new Rejected("no-player");
        }
        if (count < 1 || count > 64) {
            throw new Rejected("count-out-of-range [1,64]: " + count);
        }
        if (damage < 0 || damage > 32767) {
            throw new Rejected("dmg-out-of-range [0,32767]: " + damage);
        }
        resolveInventory(p);
        Object[] items = (Object[]) itemRegistry.get(null);
        if (itemId < 0 || itemId >= items.length || items[itemId] == null) {
            throw new Rejected("bad-itemid: " + itemId);
        }
        Object stack = (damage == 0)
                ? isCtor2.newInstance(Integer.valueOf(itemId), Integer.valueOf(count))
                : isCtor3.newInstance(Integer.valueOf(itemId), Integer.valueOf(count),
                        Integer.valueOf(damage));
        boolean fitted = ((Boolean) addStack.invoke(invField.get(p), stack)).booleanValue();
        if (!fitted) {
            throw new Rejected("inventory-full");
        }
        return "gave " + itemId + " x" + count + (damage == 0 ? "" : " dmg=" + damage);
    }

    /** 物品栏快照（op=inv，纯读）：主槽 0..35、盔甲 A0..A3，槽=数量xID[ d伤害]，cur=快捷槽；全空→empty。 */
    public static String inv(Object world) throws Exception {
        Object p = player.get(world);
        if (p == null) {
            throw new Rejected("no-player");
        }
        resolveInventory(p);
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
        Object p = player.get(world);
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
            Class<?> ec = player.getType();
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
                ctor = tnt
                        ? mc.getConstructor(wc, float.class, float.class, float.class)
                        : mc.getConstructor(wc);
            } catch (ClassNotFoundException e) {
                throw new UnsupportedOp("mob class: " + e);
            } catch (NoSuchMethodException e) {
                throw new UnsupportedOp("mob ctor: " + e);
            }
            MOB_CTORS.put(kind, ctor);
        }
        Object ent = tnt
                ? ctor.newInstance(world, Float.valueOf(x + 0.5f), Float.valueOf(y), Float.valueOf(z + 0.5f))
                : ctor.newInstance(world);
        setPosRot.invoke(ent, Double.valueOf(x + 0.5), Double.valueOf(y), Double.valueOf(z + 0.5),
                Float.valueOf(0f), Float.valueOf(0f));
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
        Object p = player.get(world);
        if (p == null) {
            throw new Rejected("no-player");
        }
        if (count < 1 || count > 64) {
            throw new Rejected("count-out-of-range [1,64]: " + count);
        }
        if (damage < 0 || damage > 32767) {
            throw new Rejected("dmg-out-of-range [0,32767]: " + damage);
        }
        resolveInventory(p);
        Object[] items = (Object[]) itemRegistry.get(null);
        if (itemId < 0 || itemId >= items.length || items[itemId] == null) {
            throw new Rejected("bad-itemid: " + itemId);
        }
        Object stack = (damage == 0)
                ? isCtor2.newInstance(Integer.valueOf(itemId), Integer.valueOf(count))
                : isCtor3.newInstance(Integer.valueOf(itemId), Integer.valueOf(count),
                        Integer.valueOf(damage));
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
        Object p = player.get(world);
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

    /** 注册表惰性解析独立件（TE 面零 player 依赖；与 resolveInventory 同 classloader 同字段）。 */
    private static void ensureRegistry(Object world) throws UnsupportedOp {
        if (itemRegistry != null) {
            return;
        }
        try {
            Class<?> itemClass = Class.forName(G_ITEM_CLASS, false, world.getClass().getClassLoader());
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
            ensureRegistry(world);
            Object[] items = (Object[]) itemRegistry.get(null);
            if (itemId < 0 || itemId >= items.length || items[itemId] == null) {
                throw new Rejected("bad-itemid: " + itemId);
            }
            stack = (damage == 0)
                    ? isCtor2.newInstance(Integer.valueOf(itemId), Integer.valueOf(count))
                    : isCtor3.newInstance(Integer.valueOf(itemId), Integer.valueOf(count),
                            Integer.valueOf(damage));
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
        saveWorld.invoke(world, Boolean.FALSE);
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
