package com.mcworldagent.agent;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 外部命令通道 v1（案卷 agent-command-channel v2，载体 A 文件队列）。
 *
 * 线程模型（§5）：premain 期创建的 daemon 轮询线程负责全部文件 I/O 与 JSON 解析；
 * 只有经过校验的命令以 Runnable 经 MCAHooks.submit()（ConcurrentLinkedQueue，线程安全已核）
 * 进入队列，由游戏线程在 tick 边界 drain 执行。磁盘慢不卡 tick。
 *
 * 生命周期（§3）：tmp→rename→inbox；读取成功先移 processed/（=ACCEPTED，写 outbox 临时态）
 * 再 submit；执行后 outbox 原子替换终态（OK/REJECTED/UNSUPPORTED/ERROR）。
 * ACCEPTED 滞留 = agent 在接收后执行前崩溃的诊断信号。
 *
 * 限额（§4-3）：单文件 ≤4KB、单次轮询 ≤8 条、积压 >64 拒绝（queue-full）。
 * 幂等（§3）：会话内 id 去重（processed/ 文件名 + 内存集合）。
 */
public final class CommandChannel {

    private CommandChannel() {
    }

    private static final int PROTO = 1;
    private static final int MAX_FILE = 4096;
    private static final int MAX_PER_POLL = 8;
    private static final int MAX_BACKLOG = 64;
    private static final long POLL_MS = 250; // 20TPS 下约每 5 tick

    private static final Set<String> KNOWN_OPS = new HashSet<String>(
            Arrays.asList("place", "dig", "getblock", "getinfo", "settime", "setspawn", "tp",
                    "save", "fill", "getblocks", "getplayer",
                    "getmeta", "setmeta", "genscatter", "listops",
                    "getlight", "setlook", "setmotion",
                    "give", "inv", "sethealth",
                    "explode", "spawn", "drop", "invulnerable",
                    "getchest", "setchest", "getfurnace",
                    "entitylist", "playsound", "setarmor", "clearinv", "replace",
                    "grow", "clear", "getbiome"));

    /** op 自表面（op=listops，agent 侧零游戏调用）：与 runOp 分支同步维护。 */
    private static final String LISTOPS =
            "ops=place(x,y,z,blockId)w;dig(x,y,z)w;getblock(x,y,z)r;getinfo r;settime(time)w;"
          + "setspawn(x,y,z)w;tp(x,y,z)w[save-default-false];save w[explicit];"
          + "fill(x1,y1,z1,x2,y2,z2,blockId)w[cap4096];getblocks(x1,y1,z1,x2,y2,z2)r[cap4096];"
          + "getplayer r;getmeta(x,y,z)r;setmeta(x,y,z,meta0..15)w;"
          + "genscatter(x,y,z,blockId[,seed])w;listops r;"
          + "getlight(x,y,z)r[0..15];setlook(yaw0..359,pitch-90..90 int-deg)w;"
          + "setmotion(vx,vy,vz int-thousandths +-10000)w;"
          + "give(itemId1..255,count1..64[,dmg0..32767])w;inv r;sethealth(0..20)w;"
          + "explode(x,y,z,size0..10)w;"
          + "spawn(kind pig|sheep|creeper|skeleton|zombie|spider|tnt|giant|mob,x,y,z)t;"
          + "drop(x,y,z,itemId1..255,count1..64[,dmg])t;"
          + "invulnerable(ticks0..32767 >=11-immune,0-cancel)t;"
          + "getchest(x,y,z)r[IInventory 通用:chest/furnace];"
          + "setchest(x,y,z,slot,itemId1..255,count1..64[,dmg]|clear=true)w[直替换无合并];"
          + "getfurnace(x,y,z)r;"
          + "entitylist r[N=kind@x,y,z[h=H]];"
          + "playsound(x,y,z,name,vol0..10,pitch0..20 十分位)t;"
          + "setarmor(slot0..3,itemId1..255,count1..64[,dmg])w[不校验ItemArmor];"
          + "clearinv w[不可逆];"
          + "replace(x1,y1,z1,x2,y2,z2,oldId,newId)w[cap4096];"
          + "grow(x,y,z)w[crops59-only;sapling=UNSUPPORTED-Beta1.2];"
          + "clear([x,y,z,r0..64])t[EntityItem-only];"
          + "getbiome(x,z)r[a1.2.0+;beta 前版本 UNSUPPORTED]";

    private static String cmdDirArg;
    private static File baseDir; // 解析后的 mca/ 根
    private static final Set<String> seenIds = new HashSet<String>(); // 仅轮询线程访问

    /** premain 调用。cmdDirArg=null 时默认 <gameDir>\mca（gameDir 由捕获的世界目录回溯）。 */
    public static void start(String dirArg) {
        cmdDirArg = dirArg;
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                pollLoop();
            }
        }, "MCWorldAgent-CommandChannel");
        t.setDaemon(true);
        t.start();
        MCAHooks.log("CHANNEL_START dirArg=" + (dirArg == null ? "<default>" : dirArg));
    }

    private static void pollLoop() {
        while (true) {
            try {
                Thread.sleep(POLL_MS);
                File dir = resolveDir();
                if (dir == null) {
                    continue; // 默认目录需等世界捕获；无 cmd= 时通道静默等待
                }
                pollOnce(dir);
            } catch (InterruptedException ie) {
                return;
            } catch (Throwable t) {
                // 通道故障只记日志，永不外溢（崩溃隔离）
                MCAHooks.log("CHANNEL_ERROR " + t);
            }
        }
    }

    private static File resolveDir() {
        if (baseDir != null) {
            return baseDir;
        }
        String path = cmdDirArg;
        if (path == null) {
            File saveDir = MCAHooks.capturedSaveDir; // <gameDir>\saves\<World>
            if (saveDir == null) {
                return null;
            }
            File gameDir = saveDir.getParentFile() == null ? null
                    : saveDir.getParentFile().getParentFile();
            if (gameDir == null) {
                MCAHooks.log("CHANNEL_DIR_FAIL cannot derive gameDir from " + saveDir);
                return null;
            }
            path = new File(gameDir, "mca").getAbsolutePath();
        }
        File dir = new File(path).getAbsoluteFile();
        // §4-4 归属校验：拒绝指向 saves\ 内（规范化后禁止 …\saves\… 前缀）
        String np = normalize(dir.getAbsolutePath()).toLowerCase();
        if (np.contains("\\saves\\") || np.endsWith("\\saves")) {
            MCAHooks.log("CHANNEL_DIR_FAIL refuses saves path: " + np);
            return null;
        }
        if (!dir.isDirectory() && !dir.mkdirs() && !dir.isDirectory()) {
            MCAHooks.log("CHANNEL_DIR_FAIL cannot create " + dir);
            return null;
        }
        mkdirs(new File(dir, "inbox"));
        mkdirs(new File(dir, "processed"));
        mkdirs(new File(dir, "outbox"));
        baseDir = dir;
        MCAHooks.log("CHANNEL_DIR " + dir);
        return dir;
    }

    private static void pollOnce(File dir) {
        File inbox = new File(dir, "inbox");
        File[] all = inbox.listFiles();
        List<File> files = new ArrayList<File>();
        if (all != null) {
            for (File f : all) {
                if (f.isFile() && f.getName().toLowerCase().endsWith(".json")) {
                    files.add(f);
                }
            }
        }
        Collections.sort(files, NAME_ORDER);
        boolean backlogFull = files.size() > MAX_BACKLOG;
        int n = Math.min(files.size(), MAX_PER_POLL);
        for (int i = 0; i < n; i++) {
            consume(files.get(i), dir, backlogFull);
        }
    }

    private static final Comparator<File> NAME_ORDER = new Comparator<File>() {
        @Override
        public int compare(File a, File b) {
            return a.getName().compareTo(b.getName());
        }
    };

    /** 单条命令独立校验独立处理，错误不阻塞队列（§4-3）。 */
    private static void consume(File f, File dir, boolean backlogFull) {
        String id = null;
        try {
            if (f.length() > MAX_FILE) {
                moveToProcessed(f);
                writeStatus(dir, safeId(f), "REJECTED", "file too large", 0);
                return;
            }
            String text = read(f);
            Map<String, Object> env;
            try {
                env = MiniJson.parse(text);
            } catch (IllegalArgumentException je) {
                moveToProcessed(f);
                writeStatus(dir, safeId(f), "REJECTED", "bad-json: " + je.getMessage(), 0);
                return;
            }
            String vErr = validate(env);
            if (vErr != null) {
                moveToProcessed(f);
                writeStatus(dir, safeId(f), "REJECTED", vErr, 0);
                return;
            }
            id = (String) env.get("id");
            if (backlogFull) {
                moveToProcessed(f);
                writeStatus(dir, id, "REJECTED", "queue-full", 0);
                return;
            }
            if (seenIds.contains(id)) {
                moveToProcessed(f);
                writeStatus(dir, id, "REJECTED", "duplicate-id", 0);
                return;
            }
            seenIds.add(id);

            // 先移 processed/（=ACCEPTED）再 submit（§4-1）
            if (!moveToProcessed(f)) {
                seenIds.remove(id);
                return; // 移动失败，下轮重试
            }
            writeStatus(dir, id, "ACCEPTED", null, MCAHooks.tickCount());

            final Command c = Command.from(id, env);
            MCAHooks.submit(new Runnable() {
                @Override
                public void run() {
                    execute(c);
                }
            });
            MCAHooks.log("CHANNEL_ACCEPT id=" + id + " op=" + c.op);
        } catch (Throwable t) {
            MCAHooks.log("CHANNEL_CONSUME_ERROR " + f.getName() + " " + t);
        }
    }

    /** 游戏线程执行（tick 边界 drain）。 */
    private static void execute(Command c) {
        File dir = baseDir;
        String status;
        String detail;
        try {
            Object world = MCAHooks.capturedWorld;
            if (world == null) {
                status = "REJECTED";
                detail = "not-ready";
            } else {
                // P1 世代定向：带 targetGeneration 的会话命令执行时须命中当前世界会话，
                // 防"旧世界提交、新世界激活后误执行"（案卷 §5）。
                // 无该字段 = 执行时绑定当前世界（CLI 普通命令语义），两者不混同。
                Object tg = c.env.get("targetGeneration");
                if (tg != null) {
                    long want = ((Long) tg).longValue();
                    int actual = MCAHooks.capturedWorldGeneration;
                    if (want != actual) {
                        finish(dir, c, "REJECTED",
                                "generation-mismatch expected=" + want + " actual=" + actual);
                        return;
                    }
                }
                try {
                    WorldOps.resolve(world.getClass());
                } catch (WorldOps.UnsupportedOp u) {
                    status = "UNSUPPORTED";
                    detail = u.getMessage();
                    finish(dir, c, status, detail);
                    return;
                }
                try {
                    detail = runOp(world, c);
                    status = "OK";
                } catch (WorldOps.Rejected r) {
                    status = "REJECTED";
                    detail = r.getMessage();
                } catch (WorldOps.NoChange nc) {
                    // 协议 v2 终态：批量写零变更（唯一生产者=fill），save 已跳过
                    status = "NO_CHANGE";
                    detail = nc.getMessage();
                } catch (WorldOps.UnsupportedOp u) {
                    // runOp 期惰性签名解析（getplayer 的 W）——与 resolve 段同映射，不静默
                    status = "UNSUPPORTED";
                    detail = u.getMessage();
                } catch (java.lang.reflect.InvocationTargetException ite) {
                    status = "ERROR";
                    detail = WorldOps.unwrap(ite);
                }
            }
        } catch (Throwable t) {
            status = "ERROR";
            detail = String.valueOf(t);
        }
        finish(dir, c, status, detail);
    }

    @SuppressWarnings("unchecked")
    private static String runOp(Object world, Command c) throws Exception {
        Map<String, Object> a = (Map<String, Object>) c.env.get("args");
        if ("save".equals(c.op)) {
            WorldOps.saveWorld(world);
            return "saveWorld(false) called";
        }
        if ("getinfo".equals(c.op)) {
            return WorldOps.getInfo(world); // Query 只读快照，无坐标校验无存盘
        }
        // 写侧非坐标 op：不走 place/dig 的坐标管线
        if ("settime".equals(c.op)) {
            long t = longArg(a, "time");
            WorldOps.validateTime(t);
            long old = WorldOps.setTime(world, t);
            return "time " + old + " -> " + t + saveSuffix(world, c);
        }
        if ("setspawn".equals(c.op)) {
            int sx = intArg(a, "x");
            int sy = intArg(a, "y");
            int sz = intArg(a, "z");
            WorldOps.validateCoords(sx, sy, sz);
            WorldOps.setSpawn(world, sx, sy, sz);
            return "spawn set x=" + sx + " y=" + sy + " z=" + sz + saveSuffix(world, c);
        }
        if ("tp".equals(c.op)) {
            int tx = intArg(a, "x");
            int ty = intArg(a, "y");
            int tz = intArg(a, "z");
            WorldOps.validateCoords(tx, ty, tz);
            String r = WorldOps.tp(world, tx, ty, tz);
            // tp 临时态语义（§3）：save 默认 false，显式 save=true 才落 level.dat
            if (Boolean.TRUE.equals(c.env.get("save"))) {
                WorldOps.saveWorld(world);
                r += " saved=true";
            }
            return r;
        }
        // 区域批量 op（fill/getblocks）：cap=MAX_REGION_VOLUME，tick 边界计时入日志（tick 预算取证）
        if ("fill".equals(c.op)) {
            int x1 = intArg(a, "x1");
            int y1 = intArg(a, "y1");
            int z1 = intArg(a, "z1");
            int x2 = intArg(a, "x2");
            int y2 = intArg(a, "y2");
            int z2 = intArg(a, "z2");
            int id = intArg(a, "blockId");
            long t0 = System.nanoTime();
            try {
                return WorldOps.fill(world, x1, y1, z1, x2, y2, z2, id) + saveSuffix(world, c);
            } finally {
                MCAHooks.log("FILL_MS id=" + c.id + " ms="
                        + ((System.nanoTime() - t0) / 1000000L));
            }
        }
        if ("getblocks".equals(c.op)) {
            int x1 = intArg(a, "x1");
            int y1 = intArg(a, "y1");
            int z1 = intArg(a, "z1");
            int x2 = intArg(a, "x2");
            int y2 = intArg(a, "y2");
            int z2 = intArg(a, "z2");
            long t0 = System.nanoTime();
            try {
                return WorldOps.getblocks(world, x1, y1, z1, x2, y2, z2); // 读侧忽略 save
            } finally {
                MCAHooks.log("GETBLOCKS_MS id=" + c.id + " ms="
                        + ((System.nanoTime() - t0) / 1000000L));
            }
        }
        if ("getplayer".equals(c.op)) {
            return WorldOps.getPlayer(world); // 读侧快照；W 惰性解析失败→UnsupportedOp→UNSUPPORTED
        }
        if ("getlight".equals(c.op)) {
            int x = intArg(a, "x");
            int y = intArg(a, "y");
            int z = intArg(a, "z");
            int light = WorldOps.getLightValue(world, x, y, z);
            return "x=" + x + " y=" + y + " z=" + z + " light=" + light;
        }
        if ("setlook".equals(c.op)) {
            int yaw = intArg(a, "yaw");
            int pitch = intArg(a, "pitch");
            // Player NBT "Rotation" 随存盘持久（setw-1/tp-1 已证 Player compound 独立持久化）
            return WorldOps.setLook(world, yaw, pitch) + saveSuffix(world, c);
        }
        if ("setmotion".equals(c.op)) {
            int vx = intArg(a, "vx");
            int vy = intArg(a, "vy");
            int vz = intArg(a, "vz");
            // Player NBT "Motion" 随存盘持久，同上
            return WorldOps.setMotion(world, vx, vy, vz) + saveSuffix(world, c);
        }
        // 物品栏三 op（give-inv-health 批）：give/sethealth 写侧 saveSuffix（"Inventory" NBT 随 save 落盘）；
        // inv 纯读忽略 save；give 的 dmg 可选（缺省 0，"缺键=默认"面同 genscatter.seed）。
        if ("give".equals(c.op)) {
            int itemId = intArg(a, "itemId");
            int count = intArg(a, "count");
            int dmg = optionalIntArg(a, "dmg", 0);
            return WorldOps.give(world, itemId, count, dmg) + saveSuffix(world, c);
        }
        if ("inv".equals(c.op)) {
            return WorldOps.inv(world); // 读侧忽略 save
        }
        if ("sethealth".equals(c.op)) {
            int health = intArg(a, "health");
            return WorldOps.setHealth(world, health) + saveSuffix(world, c);
        }
        // 实体面三 op（spawn-explode-drop 批）：explode 写侧 saveSuffix（破坏方块进方块表）；
        // spawn/drop **忽略 save**（0327 saveWorld 不写实体=恒临时态，spec §1 实锤——
        // detail 不拼 saved= 防误导，标记 t=transient 见 listops）。
        if ("explode".equals(c.op)) {
            int x = intArg(a, "x");
            int y = intArg(a, "y");
            int z = intArg(a, "z");
            int size = intArg(a, "size");
            return WorldOps.explode(world, x, y, z, size) + saveSuffix(world, c);
        }
        if ("spawn".equals(c.op)) {
            String kind = stringArg(a, "kind");
            int x = intArg(a, "x");
            int y = intArg(a, "y");
            int z = intArg(a, "z");
            return WorldOps.spawn(world, kind, x, y, z); // 实体不落盘，save 无作用
        }
        if ("drop".equals(c.op)) {
            int x = intArg(a, "x");
            int y = intArg(a, "y");
            int z = intArg(a, "z");
            int itemId = intArg(a, "itemId");
            int count = intArg(a, "count");
            int dmg = optionalIntArg(a, "dmg", 0);
            return WorldOps.drop(world, itemId, count, dmg, x, y, z); // 同上
        }
        if ("invulnerable".equals(c.op)) {
            int ticks = intArg(a, "ticks");
            return WorldOps.invulnerable(world, ticks); // i-frame 计时器不进 NBT，save 无作用
        }
        // tileentity 三 op：getchest/getfurnace 纯读；setchest 写侧 saveSuffix
        // （TE 随 chunk "TileEntities" 持久=与实体不落盘对照面，spec §1）。
        if ("getchest".equals(c.op)) {
            int x = intArg(a, "x");
            int y = intArg(a, "y");
            int z = intArg(a, "z");
            return WorldOps.getInv(world, x, y, z); // IInventory 通用面（chest/furnace 皆可）
        }
        if ("setchest".equals(c.op)) {
            int x = intArg(a, "x");
            int y = intArg(a, "y");
            int z = intArg(a, "z");
            int slot = intArg(a, "slot");
            boolean clear = Boolean.TRUE.equals(a == null ? null : a.get("clear"));
            // clear 与 itemId/count 互斥（clear 优先，二者缺省允许）
            int itemId = clear ? 0 : intArg(a, "itemId");
            int count = clear ? 0 : intArg(a, "count");
            int dmg = optionalIntArg(a, "dmg", 0);
            return WorldOps.setInv(world, x, y, z, slot, itemId, count, dmg, clear)
                    + saveSuffix(world, c);
        }
        if ("getfurnace".equals(c.op)) {
            int x = intArg(a, "x");
            int y = intArg(a, "y");
            int z = intArg(a, "z");
            return WorldOps.getFurnace(world, x, y, z);
        }
        // 零取证五 op 批（全部落已钉面，spec zero-evidence-five）：
        // entitylist/playsound 忽略 save（清单只读；音效瞬态不进 NBT）；
        // setarmor/clearinv/replace 写侧 saveSuffix。
        if ("entitylist".equals(c.op)) {
            return WorldOps.entityList(world); // 纯读=spawn/drop 账面对账面
        }
        if ("playsound".equals(c.op)) {
            int x = intArg(a, "x");
            int y = intArg(a, "y");
            int z = intArg(a, "z");
            String name = stringArg(a, "name");
            int vol = intArg(a, "vol");
            int pitch = intArg(a, "pitch");
            return WorldOps.playSound(world, x, y, z, name, vol, pitch); // 瞬态
        }
        if ("setarmor".equals(c.op)) {
            int slot = intArg(a, "slot");
            int itemId = intArg(a, "itemId");
            int count = intArg(a, "count");
            int dmg = optionalIntArg(a, "dmg", 0);
            return WorldOps.setArmor(world, slot, itemId, count, dmg) + saveSuffix(world, c);
        }
        if ("clearinv".equals(c.op)) {
            return WorldOps.clearInv(world) + saveSuffix(world, c); // 不可逆
        }
        if ("replace".equals(c.op)) {
            int x1 = intArg(a, "x1");
            int y1 = intArg(a, "y1");
            int z1 = intArg(a, "z1");
            int x2 = intArg(a, "x2");
            int y2 = intArg(a, "y2");
            int z2 = intArg(a, "z2");
            int oldId = intArg(a, "oldId");
            int newId = intArg(a, "newId");
            return WorldOps.replace(world, x1, y1, z1, x2, y2, z2, oldId, newId)
                    + saveSuffix(world, c);
        }
        // grow/clear 批（grow-clear spec §2）：grow 走坐标管线+saveSuffix（meta 进方块表持久）；
        // clear **忽略 save**（0327 saveWorld 不写实体=恒 transient，spawn/drop 同型标注）。
        if ("grow".equals(c.op)) {
            int x = intArg(a, "x");
            int y = intArg(a, "y");
            int z = intArg(a, "z");
            WorldOps.validateCoords(x, y, z);
            return WorldOps.growCrops(world, x, y, z) + saveSuffix(world, c);
        }
        if ("clear".equals(c.op)) {
            boolean hasCenter = a != null && a.get("x") != null;
            if (!hasCenter) {
                return WorldOps.clearGroundItems(world, null, null, null, null);
            }
            int x = intArg(a, "x");
            int y = intArg(a, "y");
            int z = intArg(a, "z");
            int r = intArg(a, "r");
            return WorldOps.clearGroundItems(world, Integer.valueOf(x), Integer.valueOf(y),
                    Integer.valueOf(z), Integer.valueOf(r));
        }
        if ("listops".equals(c.op)) {
            return LISTOPS; // agent 侧自省，零游戏调用（世界未捕获仍 not-ready——v1 从简）
        }
        if ("getmeta".equals(c.op)) {
            int x = intArg(a, "x");
            int y = intArg(a, "y");
            int z = intArg(a, "z");
            int m = WorldOps.getMeta(world, x, y, z);
            return "x=" + x + " y=" + y + " z=" + z + " meta=" + m;
        }
        if ("setmeta".equals(c.op)) {
            int x = intArg(a, "x");
            int y = intArg(a, "y");
            int z = intArg(a, "z");
            int meta = intArg(a, "meta");
            return WorldOps.setMeta(world, x, y, z, meta) + saveSuffix(world, c);
        }
        if ("getbiome".equals(c.op)) {
            // biome=列面函数（x,z），y 无意义不取——validateCoords 不适用；
            // WCM 纯数学现算不触 chunk（WorldOps.biomeAt 注释），beta 前版本槽 null=require 响亮
            int x = intArg(a, "x");
            int z = intArg(a, "z");
            return "x=" + x + " z=" + z + " biome=" + WorldOps.biomeAt(world, x, z);
        }
        if ("genscatter".equals(c.op)) {
            int x = intArg(a, "x");
            int y = intArg(a, "y");
            int z = intArg(a, "z");
            int id = intArg(a, "blockId");
            Object sd = a == null ? null : a.get("seed");
            Long seed = null;
            if (sd != null) {
                if (!(sd instanceof Long)) {
                    throw new WorldOps.Rejected("arg 'seed' must be integer");
                }
                seed = (Long) sd;
            }
            return WorldOps.genScatter(world, id, x, y, z, seed) + saveSuffix(world, c);
        }
        int x = intArg(a, "x");
        int y = intArg(a, "y");
        int z = intArg(a, "z");
        WorldOps.validateCoords(x, y, z);
        if ("getblock".equals(c.op)) {
            int got = WorldOps.getBlockId(world, x, y, z);
            return "x=" + x + " y=" + y + " z=" + z + " id=" + got;
        }
        int blockId = "dig".equals(c.op) ? 0 : intArg(a, "blockId");
        WorldOps.validateBlockId(blockId);
        boolean ok = WorldOps.setBlock(world, x, y, z, blockId);
        if (!ok) {
            throw new WorldOps.Rejected("setBlockWithNotify returned false");
        }
        String r = (blockId == 0 ? "dug" : "placed") + " x=" + x + " y=" + y + " z=" + z + " id=" + blockId;
        boolean save = !Boolean.FALSE.equals(c.env.get("save")); // 写操作默认 true（§3）
        if (save) {
            WorldOps.saveWorld(world);
            r += " saved=true";
        }
        return r;
    }

    private static int intArg(Map<String, Object> a, String k) throws WorldOps.Rejected {
        Object v = a == null ? null : a.get(k);
        if (!(v instanceof Long)) {
            throw new WorldOps.Rejected("arg '" + k + "' must be integer");
        }
        long l = ((Long) v).longValue();
        if (l < Integer.MIN_VALUE || l > Integer.MAX_VALUE) {
            throw new WorldOps.Rejected("arg '" + k + "' out of int range");
        }
        return (int) l;
    }

    /** long 域参数（settime.time）：MiniJson 整数一律解析为 Long。 */
    private static long longArg(Map<String, Object> a, String k) throws WorldOps.Rejected {
        Object v = a == null ? null : a.get(k);
        if (!(v instanceof Long)) {
            throw new WorldOps.Rejected("arg '" + k + "' must be integer");
        }
        return ((Long) v).longValue();
    }

    /** 可选整数参数（give.dmg）：缺键=默认值（"缺键=默认"面同 genscatter.seed）。 */
    private static int optionalIntArg(Map<String, Object> a, String k, int def)
            throws WorldOps.Rejected {
        Object v = a == null ? null : a.get(k);
        if (v == null) {
            return def;
        }
        if (!(v instanceof Long)) {
            throw new WorldOps.Rejected("arg '" + k + "' must be integer");
        }
        long l = ((Long) v).longValue();
        if (l < Integer.MIN_VALUE || l > Integer.MAX_VALUE) {
            throw new WorldOps.Rejected("arg '" + k + "' out of int range");
        }
        return (int) l;
    }

    /** 字符串参数（spawn.kind）：类型面已由 validate 保证（String ≤256），此处只做取用与判空。 */
    private static String stringArg(Map<String, Object> a, String k) throws WorldOps.Rejected {
        Object v = a == null ? null : a.get(k);
        if (!(v instanceof String)) {
            throw new WorldOps.Rejected("arg '" + k + "' must be string");
        }
        return (String) v;
    }

    /** 写操作默认存盘（§3：save=false 显式关闭），返回 detail 后缀。 */
    private static String saveSuffix(Object world, Command c) throws Exception {
        if (!Boolean.FALSE.equals(c.env.get("save"))) {
            WorldOps.saveWorld(world);
            return " saved=true";
        }
        return "";
    }

    /** 信封校验：protocolVersion==1、id 形态、op 已知、targetGeneration（可选）为整数。返回 null=通过，否则=拒绝原因。 */
    private static String validate(Map<String, Object> env) {
        Object pv = env.get("protocolVersion");
        if (!(pv instanceof Long)) {
            return "protocolVersion must be integer";
        }
        if (((Long) pv).longValue() != PROTO) {
            return "unsupported protocolVersion " + pv + " (agent speaks " + PROTO + ")";
        }
        Object id = env.get("id");
        if (!(id instanceof String)) {
            return "id must be string";
        }
        if (!((String) id).matches("[A-Za-z0-9_-]{1,64}")) {
            return "bad id (charset/length)";
        }
        Object op = env.get("op");
        if (!(op instanceof String)) {
            return "op must be string";
        }
        if (!KNOWN_OPS.contains(op)) {
            return "unknown op '" + op + "'";
        }
        Object args = env.get("args");
        if (args != null && !(args instanceof Map)) {
            return "args must be object";
        }
        // P1 世代定向（案卷 §5）：信封可选字段，仅形态校验；语义比对在执行时（tick 边界）做
        Object tg = env.get("targetGeneration");
        if (tg != null && !(tg instanceof Long)) {
            return "targetGeneration must be integer";
        }
        return null;
    }

    private static void finish(File dir, Command c, String status, String detail) {
        writeStatus(dir, c.id, status, detail, MCAHooks.tickCount());
        MCAHooks.log("CHANNEL_DONE id=" + c.id + " op=" + c.op + " status=" + status
                + " tick=" + MCAHooks.tickCount() + " detail=" + detail);
    }

    // ---------- 文件工具（全部仅轮询/游戏线程的原子 rename） ----------

    private static boolean moveToProcessed(File inboxFile) {
        File p = new File(inboxFile.getParentFile().getParentFile(), "processed");
        return inboxFile.renameTo(new File(p, inboxFile.getName()));
    }

    /** outbox 原子写：~<id>.json → <id>.json（同目录 rename）。 */
    private static void writeStatus(File dir, String id, String status, String detail, long tick) {
        try {
            File outbox = new File(dir, "outbox");
            mkdirs(outbox);
            File tmp = new File(outbox, "~" + id + ".json");
            File fin = new File(outbox, id + ".json");
            StringBuilder b = new StringBuilder();
            b.append("{\"protocolVersion\":").append(PROTO)
                    .append(",\"id\":\"").append(esc(id))
                    .append("\",\"status\":\"").append(status).append('"');
            if (detail != null) {
                b.append(",\"detail\":\"").append(esc(detail)).append('"');
            }
            b.append(",\"tick\":").append(tick).append("}\n");
            FileOutputStream fo = new FileOutputStream(tmp);
            try {
                fo.write(b.toString().getBytes("UTF-8"));
            } finally {
                fo.close();
            }
            if (!tmp.renameTo(fin)) {
                //noinspection ResultOfMethodCallIgnored
                fin.delete();
                //noinspection ResultOfMethodCallIgnored
                tmp.renameTo(fin);
            }
        } catch (Throwable t) {
            MCAHooks.log("CHANNEL_OUTBOX_ERROR id=" + id + " " + t);
        }
    }

    private static String safeId(File f) {
        String n = f.getName().replaceAll("\\.json$", "");
        return n.matches("[A-Za-z0-9_-]{1,64}") ? n : "unknown";
    }

    private static String esc(String s) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"' || c == '\\') {
                b.append('\\').append(c);
            } else if (c < 0x20 || c > 0x7e) {
                b.append(String.format("\\u%04x", Integer.valueOf(c)));
            } else {
                b.append(c);
            }
        }
        return b.toString();
    }

    private static String read(File f) throws IOException {
        InputStream in = new FileInputStream(f);
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            int total = 0;
            while ((n = in.read(buf)) > 0) {
                total += n;
                if (total > MAX_FILE) {
                    throw new IOException("file too large");
                }
                bos.write(buf, 0, n);
            }
            return new String(bos.toByteArray(), "UTF-8");
        } finally {
            in.close();
        }
    }

    private static String normalize(String p) {
        try {
            return new File(p).getCanonicalPath();
        } catch (IOException e) {
            return new File(p).getAbsolutePath();
        }
    }

    private static void mkdirs(File d) {
        if (!d.isDirectory()) {
            //noinspection ResultOfMethodCallIgnored
            d.mkdirs();
        }
    }

    /** 已校验命令的不可变记录。 */
    private static final class Command {
        final String id;
        final String op;
        final Map<String, Object> env;

        Command(String id, String op, Map<String, Object> env) {
            this.id = id;
            this.op = op;
            this.env = env;
        }

        @SuppressWarnings("unchecked")
        static Command from(String id, Map<String, Object> env) {
            return new Command(id, (String) env.get("op"), env);
        }
    }
}
