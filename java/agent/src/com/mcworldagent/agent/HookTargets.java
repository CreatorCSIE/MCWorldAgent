package com.mcworldagent.agent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Hook 指纹表 — infdev 20100327 原始 jar（SHA1 47b1b32430a211520993552ba0a5e00c1af44724）
 * javap 全量扫描钉死的混淆实名。
 * 可读名对榫：E:\4k\github-projects\minecraft-verification-analysis\src-minecraft\
 * src-minecraft-infdev-20100327（World.java L93/L131、Minecraft.java L578、
 * ChunkProviderLoadOrGenerate.java L88、Chunk.java L276）。
 * 严格匹配纪律：类名 + 方法名 + 描述符 + access flags + 期望命中数，
 * 五项全中才织入；任一不符 = UNSUPPORTED 记录并跳过整类（绝不静默错钩）。
 */
public final class HookTargets {

    /** 注入方式。 */
    public static final int ENTRY = 1;            // 方法入口注入（对称压弹，帧形不变）
    public static final int ENTRY_AND_RESULT = 2; // 入口注入 + 返回值 DUP 观察

    /** 钩 id（注入 dispatch 用）。 */
    public static final String WORLD_CTOR = "world-ctor";
    public static final String SAVE_WORLD = "save-world";
    public static final String RUN_TICK = "run-tick";
    public static final String LOAD_CHUNK = "load-chunk";
    public static final String GEN_HEIGHT = "gen-height";

    public static final class Target {
        public final String className;  // internal name
        public final String methodName;
        public final String desc;
        public final int access;        // 期望精确 access flags
        public final int expectedHits;  // (name,desc) 期望命中数
        public final int injectMode;
        public final String hookId;
        public final String label;

        Target(String className, String methodName, String desc, int access,
               int expectedHits, int injectMode, String hookId, String label) {
            this.className = className;
            this.methodName = methodName;
            this.desc = desc;
            this.access = access;
            this.expectedHits = expectedHits;
            this.injectMode = injectMode;
            this.hookId = hookId;
            this.label = label;
        }
    }

    public static final Target[] TARGETS = {
        // G1 autosave + 把手①前半：World.saveWorld(boolean) 入口。
        // 构造器尾部 this 尚未通过 old_verifier 的 initialized 状态机（ALOAD 0 作
        // INVOKESTATIC 实参触发 VerifyError "Incompatible argument to function"），
        // 故活体 World 引用改由本钩捕获（0327 构造器尾必调 saveWorld(false)，
        // 树 World.java L120；saveWorld 为普通方法，this 已初始化）。
        new Target("net/minecraft/a/a/f", "a", "(Z)V",
                0x0002, 1, ENTRY, SAVE_WORLD, "World.saveWorld(boolean)"),
        // 把手①后半：构造器 (File,String) 入口只记存档目录（ILOAD 1 = 引用槽，
        // 不经 initialized 状态机，old verifier 合法；构造器尾 saveWorld 补上 this）。
        new Target("net/minecraft/a/a/f", "<init>", "(Ljava/io/File;Ljava/lang/String;)V",
                0x0001, 1, ENTRY, WORLD_CTOR, "World.<init>(File,String)"),
        // tick 汇线：Minecraft.runTick()
        new Target("net/minecraft/client/d", "h", "()V",
                0x0002, 1, ENTRY, RUN_TICK, "Minecraft.runTick()"),
        // G2：ChunkProviderLoadOrGenerate.loadChunk(x,z)（磁盘命中/未命中计数）
        new Target("net/minecraft/a/a/e/c", "d", "(II)Lnet/minecraft/a/a/e/d;",
                0x0002, 1, ENTRY_AND_RESULT, LOAD_CHUNK, "ChunkProviderLoadOrGenerate.loadChunk(int,int)"),
        // G2：Chunk.generateHeightMap()（light/height 回退重算事件计数）
        new Target("net/minecraft/a/a/e/d", "a", "()V",
                0x0011, 1, ENTRY, GEN_HEIGHT, "Chunk.generateHeightMap()"),
    };

    private static final Map<String, List<Target>> BY_CLASS = new HashMap<String, List<Target>>();

    static {
        for (Target t : TARGETS) {
            List<Target> list = BY_CLASS.get(t.className);
            if (list == null) {
                list = new ArrayList<Target>();
                BY_CLASS.put(t.className, list);
            }
            list.add(t);
        }
    }

    public static Target[] forClass(String internalName) {
        List<Target> list = BY_CLASS.get(internalName);
        return list == null ? new Target[0] : list.toArray(new Target[0]);
    }

    private HookTargets() {
    }
}
