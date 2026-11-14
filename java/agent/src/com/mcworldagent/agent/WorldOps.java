package com.mcworldagent.agent;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
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
    private static volatile Method getMeta;
    private static volatile Method setMetaSilent;
    private static volatile Method light6;
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
        Field h = eHealth;
        if (h == null) {
            try {
                h = p.getClass().getField(E_HEALTH);
            } catch (NoSuchFieldException e) {
                throw new UnsupportedOp("entity health field: " + e);
            }
            eHealth = h;
        }
        return "x=" + ePosX.getDouble(p) + " y=" + ePosY.getDouble(p)
                + " z=" + ePosZ.getDouble(p)
                + " motX=" + eMotX.getDouble(p) + " motY=" + eMotY.getDouble(p)
                + " motZ=" + eMotZ.getDouble(p)
                + " yaw=" + eYaw.getFloat(p) + " pitch=" + ePitch.getFloat(p)
                + " health=" + h.getInt(p);
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
