package com.mcworldagent.agent;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

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
        return "time=" + time + " spawnX=" + sx + " spawnY=" + sy + " spawnZ=" + sz
                + " seed=" + seed;
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
