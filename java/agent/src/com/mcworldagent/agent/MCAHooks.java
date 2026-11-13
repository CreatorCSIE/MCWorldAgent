package com.mcworldagent.agent;

import java.io.File;
import java.io.PrintWriter;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 运行时捕获面：由织入代码回调（只收 Object/基本类型，不引用游戏类，
 * 避免 agent 侧对混淆类的编译期耦合）。
 *
 * 写纪律（案卷 §8.4）：任何游戏状态修改必须经 submit() 排队，
 * 由 onTick() 在游戏线程 drain —— 单人客户端即权威，无并发写。
 */
public final class MCAHooks {

    private MCAHooks() {
    }

    static PrintWriter out;

    private static final long T0 = System.currentTimeMillis();

    /** 把手①：活体 World + 存档目录。 */
    public static volatile Object capturedWorld;
    public static volatile File capturedSaveDir;

    private static final AtomicLong TICKS = new AtomicLong();

    /** 当前 tick 计数（命令通道 outbox/日志用）。 */
    public static long tickCount() {
        return TICKS.get();
    }
    private static final ConcurrentLinkedQueue<Runnable> QUEUE =
            new ConcurrentLinkedQueue<Runnable>();

    private static final AtomicInteger CHUNK_ENTER = new AtomicInteger();
    private static final AtomicInteger CHUNK_HIT = new AtomicInteger();
    private static final AtomicInteger CHUNK_MISS = new AtomicInteger();
    private static final AtomicInteger LIGHT_RECALC = new AtomicInteger();
    private static final AtomicInteger SAVE_CALLS = new AtomicInteger();

    /** 把手①前半回调：World(File,String) 构造器入口只记存档目录（此阶段 this 不可引用）。 */
    public static void onWorldDir(File dir) {
        capturedSaveDir = dir;
        log("WORLD_DIR dir=" + dir.getAbsolutePath()
                + " level.dat=" + new File(dir, "level.dat").exists());
    }

    /** tick 汇线：Minecraft.runTick() 入口。 */
    public static void onTick() {
        long n = TICKS.incrementAndGet();
        int drained = 0;
        Runnable r;
        while ((r = QUEUE.poll()) != null) {
            drained++;
            try {
                r.run();
            } catch (Throwable t) {
                log("TICK_TASK_ERROR " + t);
            }
        }
        if (n <= 3 || n % 20 == 0 || drained > 0) {
            log("TICK " + n + (drained > 0 ? " drained=" + drained : ""));
        }
    }

    /** 写纪律入口：任务只在游戏线程 tick 边界执行。 */
    public static void submit(final Runnable task) {
        QUEUE.add(task);
        log("SUBMIT queued (runs on game thread at next tick)");
    }

    /** G1 + 把手①后半：World.saveWorld(boolean) 入口（this 已初始化，可安全引用）。 */
    public static void onSaveWorld(Object world, boolean indirect) {
        int n = SAVE_CALLS.incrementAndGet();
        boolean first = capturedWorld == null;
        if (first) {
            log("WORLD_CREATED world=" + world.getClass().getName()
                    + "@" + Integer.toHexString(System.identityHashCode(world))
                    + " dir=" + (capturedSaveDir == null ? "?" : capturedSaveDir.getAbsolutePath()));
        } else if (capturedWorld != world) {
            // 同进程重进世界：新 World 实例（从盘读回）——把手必须跟进最新活体，
            // 否则命令通道会读到退出时的旧对象，持久化验证失真（cmdchannel-1 实跑发现）。
            log("WORLD_SWITCHED old=@" + Integer.toHexString(System.identityHashCode(capturedWorld))
                    + " new=@" + Integer.toHexString(System.identityHashCode(world)));
        }
        capturedWorld = world;
        if (first) {
            // Phase 1 写侧：第一块砖（此处仍在 saveWorld 栈帧内，必须 submit 排队到 tick 边界）
            final Object w = world;
            submit(new Runnable() {
                public void run() {
                    FirstBrick.run(w);
                }
            });
        }
        log("SAVE_WORLD #" + n
                + " id=" + Integer.toHexString(System.identityHashCode(world))
                + " indirect=" + indirect);
    }

    /** G2：loadChunk 进入。 */
    public static void onLoadChunkEnter(int x, int z) {
        int n = CHUNK_ENTER.incrementAndGet();
        if (n <= 10) {
            log("CHUNK_LOAD_ENTER x=" + x + " z=" + z);
        }
    }

    /** G2：loadChunk 返回（null=磁盘无件=新 chunk；非 null=存档载入）。 */
    public static void onLoadChunkResult(Object chunk) {
        if (chunk != null) {
            CHUNK_HIT.incrementAndGet();
        } else {
            CHUNK_MISS.incrementAndGet();
        }
    }

    /** G2：Chunk.generateHeightMap()（light/height 回退重算）。 */
    public static void onGenerateHeightMap() {
        int n = LIGHT_RECALC.incrementAndGet();
        if (n <= 10) {
            log("LIGHT_RECALC #" + n);
        }
    }

    /** JVM 退出前汇总（G1/G2 计数的最终证据行）。 */
    public static void logSummary() {
        log("SUMMARY ticks=" + TICKS.get()
                + " saveWorld=" + SAVE_CALLS.get()
                + " chunkEnter=" + CHUNK_ENTER.get()
                + " chunkHit=" + CHUNK_HIT.get()
                + " chunkMiss=" + CHUNK_MISS.get()
                + " lightRecalc=" + LIGHT_RECALC.get());
    }

    static void log(String s) {
        PrintWriter w = out;
        if (w == null) {
            return;
        }
        synchronized (w) {
            w.println("[" + (System.currentTimeMillis() - T0) + "ms] " + s);
            w.flush();
        }
    }
}
