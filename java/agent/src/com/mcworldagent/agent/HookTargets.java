package com.mcworldagent.agent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Hook 指纹表 — infdev 20100327 原始 jar（SHA1 47b1b32430a211520993552ba0a5e00c1af44724）
 * javap 全量扫描钉死的混淆实名。
 * 可读名对榫：姊妹仓 minecraft-verification-analysis 的
 * src-minecraft/src-minecraft-infdev-20100327 可读树（本地位置随环境；
 * World.java L93/L131、Minecraft.java L578、
 * ChunkProviderLoadOrGenerate.java L88、Chunk.java L276）。
 * 严格匹配纪律：类名 + 方法名 + 描述符 + access flags + 期望命中数，
 * 五项全中才织入；任一不符 = UNSUPPORTED 记录并跳过整类（绝不静默错钩）。
 */
public final class HookTargets {

    /** 注入方式。 */
    public static final int ENTRY = 1;            // 方法入口注入（对称压弹，帧形不变）
    public static final int ENTRY_AND_RESULT = 2; // 入口注入 + 返回值 DUP 观察
    /**
     * 受限构造器补全（戒律 §1"命令执行面零注入"的唯一豁免，spec spawn-explode-drop §6）：
     * 触发=指纹表 expectedHits:0（类在、<init> 被混淆管线剥掉的死代码实体类）；
     * 注入体=标准四指令构造器（ALOAD 0/ALOAD 1/INVOKESPECIAL super<init>/RETURN——
     * 构造器先 super 是 verifier 合法序列，非"构造期 this 作 INVOKESTATIC 实参"禁区）；
     * 父链缺构造→类校验 VerifyError→Class.forName 失败→spawn UNSUPPORTED=优雅降级。
     * 泛化背景=用户启示：0413/0414 版本全生物构造器丢失，本模式是跨版本复活能力非一次性补丁。
     */
    public static final int NEW_CTOR = 3;
    /**
     * RETURN 前注入（void 观察钩专用，hud-overlay 批）：同 ENTRY 对称压弹纪律；
     * 方法有多个 RETURN 时逐个注入=HUD 重复绘制同文本（幂等，视觉无差）。
     */
    public static final int EXIT = 4;

    /** 钩 id（注入 dispatch 用）。 */
    public static final String WORLD_CTOR = "world-ctor";
    public static final String SAVE_WORLD = "save-world";
    public static final String RUN_TICK = "run-tick";
    public static final String LOAD_CHUNK = "load-chunk";
    public static final String GEN_HEIGHT = "gen-height";
    public static final String GIANT_CTOR = "giant-ctor";
    public static final String HUD_RENDER = "hud-render";

    public static final class Target {
        public final String className;  // internal name
        public final String methodName;
        public final String desc;
        public final int access;        // 期望精确 access flags
        public final int expectedHits;  // (name,desc) 期望命中数
        public final int injectMode;
        public final String hookId;
        public final String label;
        public final String superOwner; // 仅 NEW_CTOR：INVOKESPECIAL 目标父类 internal name

        Target(String className, String methodName, String desc, int access,
               int expectedHits, int injectMode, String hookId, String label) {
            this(className, methodName, desc, access, expectedHits, injectMode, hookId, label, null);
        }

        Target(String className, String methodName, String desc, int access,
               int expectedHits, int injectMode, String hookId, String label, String superOwner) {
            this.className = className;
            this.methodName = methodName;
            this.desc = desc;
            this.access = access;
            this.expectedHits = expectedHits;
            this.injectMode = injectMode;
            this.hookId = hookId;
            this.label = label;
            this.superOwner = superOwner;
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
        // 受限构造器补全（NEW_CTOR 首例）：EntityGiant=a.c.a.a，混淆管线剥掉 <init>
        // （javap 全量实锤，类仅存 a(III)F+a()String 两方法）；父=EntityMonster=a.c.a.e
        // 其 (World)V 构造健在（javap 实锤）；渲染面 RenderGiantZombie(scale 6.0) 已注册
        // （RenderManager L53）——复活后=可生成的 10 倍巨僵尸。
        new Target("net/minecraft/a/c/a/a", "<init>", "(Lnet/minecraft/a/a/f;)V",
                0x0001, 0, NEW_CTOR, GIANT_CTOR, "EntityGiant.<init>(World) resurrect",
                "net/minecraft/a/c/a/e"),
        // HUD 调试面（hud-overlay 批，EXIT 首例）：GuiIngame=client.c.m（hud-1 实跑
        // 教训：d.b 是无关计时类——a(F)V 仅 49 字节写 mc.e.a 浮点，钩上后绘制调用
        // 全成功但画面无字=挂在 tick 路径非渲染路径；c.m 才是真身：extends c.l(Gui)、
        // 唯一构造 c.m(client.d)、a(F)V 30KB 字节码 18×GL11+13×FontRenderer+1×RETURN、
        // 含 "Minecraft Infdev" 字符串常量——javap 实锤）。
        // renderGameOverlay(F)V public final——EXIT 注入 onHud(gui)：投影就绪+全 HUD 顶层。
        new Target("net/minecraft/client/c/m", "a", "(F)V",
                0x0011, 1, EXIT, HUD_RENDER, "GuiIngame.renderGameOverlay(float)"),
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
