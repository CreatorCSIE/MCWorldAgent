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

    /**
     * 把手①世代（P1，案卷 §5）：世界实例切换计数。
     * 初值 1 = 本进程捕获的第一个世界实例；每次 WORLD_SWITCHED 自增。
     * 命令信封带 targetGeneration 时，执行时（tick 边界）比对，不符 REJECTED；
     * 不带 = "执行时绑定当前世界"语义，两者不混同。
     */
    public static volatile int capturedWorldGeneration = 1;

    /** P0 fixture 开关：true=首次捕获世界不再自动提交 FirstBrick（防测试锚点污染，教训 9）。 */
    public static volatile boolean noFirstBrick;

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
                    + " dir=" + (capturedSaveDir == null ? "?" : capturedSaveDir.getAbsolutePath())
                    + " generation=" + capturedWorldGeneration);
        } else if (capturedWorld != world) {
            // 同进程重进世界：新 World 实例（从盘读回）——把手必须跟进最新活体，
            // 否则命令通道会读到退出时的旧对象，持久化验证失真（cmdchannel-1 实跑发现）。
            capturedWorldGeneration++;
            log("WORLD_SWITCHED old=@" + Integer.toHexString(System.identityHashCode(capturedWorld))
                    + " new=@" + Integer.toHexString(System.identityHashCode(world))
                    + " generation=" + capturedWorldGeneration);
        }
        capturedWorld = world;
        if (first) {
            // Phase 1 写侧：第一块砖（此处仍在 saveWorld 栈帧内，必须 submit 排队到 tick 边界）
            if (noFirstBrick) {
                log("FIRST_BRICK_SKIPPED noFirstBrick=true（PoC fixture 已禁用，世界零自动写入）");
            } else {
                final Object w = world;
                submit(new Runnable() {
                    public void run() {
                        FirstBrick.run(w);
                    }
                });
            }
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

    // ===== HUD 调试面（hud-overlay 批，GuiIngame EXIT 钩）=====
    // 反射链（javap 全实名，spec hud-overlay §1，hud-1 修正）：gui(client.c.m)→
    // 唯一构造参数类型=Minecraft(client.d)→按类型扫字段得 mc（实名 c.m.d）
    // →mc.f=thePlayer(client.g.a，tp 轮钉)、mc.m=fontRenderer(client.c.k)、
    // c.k.a(String,III)V=drawStringWithShadow；player pos=Entity g/h/i。
    // 崩溃隔离：任何异常=永久停用本钩（HUD 是附加观察面，绝不带崩渲染循环）。
    private static volatile java.lang.reflect.Field hudMcF, hudPlayerF, hudFontF;
    private static volatile java.lang.reflect.Field hudPosX, hudPosY, hudPosZ;
    private static volatile java.lang.reflect.Field hudSettingsF, hudShowFPSF;
    private static volatile java.lang.reflect.Method hudDrawM;
    private static volatile boolean hudBroken;
    private static long hudPaints; // 诊断计数（渲染线程单线程访问）

    /** javaagent 列表（纯 JDK，JVM 无 agent 注册表 API——启动参数即真相）。 */
    static java.util.List<String> agentArgs() {
        java.util.List<String> out = new java.util.ArrayList<String>();
        for (String a : java.lang.management.ManagementFactory.getRuntimeMXBean()
                .getInputArguments()) {
            if (a.startsWith("-javaagent:")) {
                out.add(a.length() > 90 ? a.substring(0, 90) + "..." : a);
            }
        }
        if (out.isEmpty()) {
            out.add("-javaagent: none");
        }
        return out;
    }

    /** HUD 显示压缩（用户判决：全路径挡屏）：剥 '=参数' 尾、取 jar 文件名；none 态留描述。 */
    static String shortAgent(String a) {
        if (a.indexOf('=') < 0 && !a.contains(".jar")) {
            return a.startsWith("-javaagent: ") ? a.substring(12) : a;
        }
        int eq = a.indexOf('=');
        String jar = eq >= 0 ? a.substring(0, eq) : a;
        int sep = Math.max(jar.lastIndexOf('/'), jar.lastIndexOf('\\'));
        return sep >= 0 ? jar.substring(sep + 1) : jar;
    }

    private static java.lang.reflect.Field findField(Class<?> c, String name) {
        for (Class<?> k = c; k != null; k = k.getSuperclass()) {
            try {
                java.lang.reflect.Field f = k.getDeclaredField(name);
                f.setAccessible(true);
                return f;
            } catch (NoSuchFieldException ignored) {
                // 继续上溯
            }
        }
        return null;
    }

    /** 按声明类型上溯扫描非静态字段（hud-1 教训：字段名不可假设，类型推导才可靠）。 */
    private static java.lang.reflect.Field findFieldByType(Class<?> c, Class<?> type) {
        for (Class<?> k = c; k != null; k = k.getSuperclass()) {
            for (java.lang.reflect.Field f : k.getDeclaredFields()) {
                if (!java.lang.reflect.Modifier.isStatic(f.getModifiers())
                        && type.isAssignableFrom(f.getType())) {
                    f.setAccessible(true);
                    return f;
                }
            }
        }
        return null;
    }

    /** GuiIngame.renderGameOverlay EXIT 钩（Transformers 注入，ALOAD 0=gui）。 */
    public static void onHud(Object gui) {
        if (hudBroken) {
            return;
        }
        try {
            java.lang.reflect.Field mcF = hudMcF;
            if (mcF == null) { // 首帧一次性解析；链=构造器参数类型+Field.getType() 推导，零硬编码 FQCN
                // GuiIngame 唯一构造 (Minecraft)V → 参数类型即 Minecraft 类；
                // 再按该类型扫描字段（hud-1 教训：mc 字段实名是 c.m.d，名字不可假设）。
                java.lang.reflect.Constructor<?>[] ctors = gui.getClass().getDeclaredConstructors();
                if (ctors.length != 1 || ctors[0].getParameterTypes().length != 1) {
                    throw new NoSuchMethodException("gui ctor shape " + ctors.length);
                }
                mcF = findFieldByType(gui.getClass(), ctors[0].getParameterTypes()[0]);
                if (mcF == null) {
                    throw new NoSuchFieldException("gui mc field");
                }
                java.lang.reflect.Field pf = mcF.getType().getDeclaredField("f"); // thePlayer
                pf.setAccessible(true);
                java.lang.reflect.Field ff = mcF.getType().getDeclaredField("m"); // fontRenderer
                ff.setAccessible(true);
                java.lang.reflect.Method dm = ff.getType().getMethod("a", String.class,
                        int.class, int.class, int.class); // drawStringWithShadow
                // showFPS 门（用户判决：与原版调试信息同开关、同列排版）：
                // mc.u=gameSettings(client.o)、o.d=showFPS（javap 实锤：
                // renderGameOverlay 字节码唯一 GameSettings 布尔读取点）。
                java.lang.reflect.Field gf = mcF.getType().getDeclaredField("u");
                gf.setAccessible(true);
                java.lang.reflect.Field sf = gf.getType().getDeclaredField("d");
                sf.setAccessible(true);
                Class<?> pc = pf.getType();
                java.lang.reflect.Field px = findField(pc, "g");
                java.lang.reflect.Field py = findField(pc, "h");
                java.lang.reflect.Field pz = findField(pc, "i");
                if (px == null || py == null || pz == null) {
                    throw new NoSuchFieldException("entity pos");
                }
                hudMcF = mcF;
                hudPlayerF = pf;
                hudFontF = ff;
                hudDrawM = dm;
                hudSettingsF = gf;
                hudShowFPSF = sf;
                hudPosX = px;
                hudPosY = py;
                hudPosZ = pz;
            }
            Object mc = mcF.get(gui);
            if (!hudShowFPSF.getBoolean(hudSettingsF.get(mc))) {
                return; // 用户判决：仅原版调试信息开启时显示（同开关同列）
            }
            Object player = hudPlayerF.get(mc);
            if (player == null) {
                return; // 加载窗（GuiIngame 已建、thePlayer 未就位）
            }
            double x = hudPosX.getDouble(player);
            double y = hudPosY.getDouble(player);
            double z = hudPosZ.getDouble(player);
            Object fr = hudFontF.get(mc);
            // 零 GL 状态改动（hud-1 实锤教训）：EXIT 点状态=原版调试行绘制态
            // （BLEND 关、透明像素靠全局 ALPHA_TEST 丢弃）。曾在此 glEnable(GL_BLEND)
            // 反被准星段残留的反色 blendFunc 污染出伪色——原版怎么做就怎么画。
            // 排版钉原版调试列：x=2；原版尾行 "P: ... T: ..." 在 y=32，
            // XYZ 接 y=42；agent 列表=y=52 头行 "javaagents:" + y=62 起 "- jar" 行
            // （用户判决 2026-11-14：头行+列表结构，行距 10）。
            hudDrawM.invoke(fr, String.format("XYZ %.2f / %.2f / %.2f", x, y, z),
                    2, 42, 0xFFFFFF);
            java.util.List<String> agents = agentArgs();
            hudDrawM.invoke(fr, "javaagents:", 2, 52, 0xAAAAAA);
            for (int i = 0; i < agents.size(); i++) {
                hudDrawM.invoke(fr, "- " + shortAgent(agents.get(i)), 2, 62 + i * 10, 0xAAAAAA);
            }
            long paints = ++hudPaints;
            if (paints == 1 || paints == 120) {
                log("HUD_PAINT #" + paints + " pos=" + x + "," + y + "," + z
                        + " agents=" + agents.size());
            }
        } catch (Throwable t) {
            hudBroken = true;
            log("HUD_HOOK_DISABLED " + t);
        }
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
