package com.mcworldagent.agent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Hook 指纹表（A2 起按版本分组：TARGETS_0327/TARGETS_0618，activate(versionId) 切换，
 * premain 前默认 0327 组=现行为零变化）。0327 组=infdev 20100327 原始 jar
 * （SHA1 47b1b32430a211520993552ba0a5e00c1af44724）javap 全量扫描钉死的混淆实名。
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
    public static final String LOAD_WORLD = "load-world";
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

    /** 0327 钩组（原始 jar SHA1 47b1b324…，全量 confirmed）。 */
    public static final Target[] TARGETS_0327 = {
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

    /**
     * 0618 钩组（A2，锚库 java/agent/registry/fingerprints/inf-20100618.json）。
     * A3 实跑终审（run a3-0618-1，2026-11-14）发现：**0618 无游戏内周期 autosave**——
     * 全 jar 仅一个 saveWorld 调用点（Minecraft.a(World,String) 换世界流程 + b() 停机），
     * 游玩期间 SAVE_WORLD 钩永不触发 → 把手只靠它会通道永久 not-ready。
     * 故补 LOAD_WORLD 装载即捕获钩（指纹=实跑字节码实锤：a(String) 内
     * new g(File,String) 后 invokespecial a(g,String)，private，slot1=已构造世界实参，
     * 非构造期 this 无 old-verifier 风险）；构造器描述符缺口同场钉死
     * =(Ljava/io/File;Ljava/lang/String;)V 与 0327 同，但 WORLD_CTOR 仍不启用
     * （0327 该钩仅记 dir，0618 通道走 cmd= 覆盖，dir 面非必需）。
     * 省略（未钉）：LOAD_CHUNK/GEN_HEIGHT（类未钉）、GIANT_CTOR（类未钉）。
     * HUD_RENDER 曾列"access 未钉"，A3 后续补钉（见下）：javap 全量反汇编实锤
     * GuiIngame=client.c.m a(F)V public final 0x0011 与 0327 同形，且调用点
     * 441: invokevirtual c.m.a:(F)V 在渲染路径（非 hud-1 误钩的计时类）。
     * SAVE_WORLD 对齐 F1 裁决 2026-11-21（锚库 inf-20100618.json provenance
     * "F1 批层级统一"）：包装 f()V→core b(Z)V（javap 实锤：g.b(boolean) 唯一
     * (Z)V 重载 private，体写 level.dat/RandomSeed NBT；构造器尾 468 iconst_0
     * 直调+周期检查 i()V 124 处 iconst_0 直调+f()V 壳 iconst_1——core 钩三径
     * 全汇，0327 把手①同配方；0618 无在册活体线，phase2-0618 历史集成件为
     * 旧 f()V 形态）。
     */
    public static final Target[] TARGETS_0618 = {
        // World.saveWorld(boolean) private core（F1 裁决统一 core (Z)V，0327 先例同型）。
        new Target("net/minecraft/a/a/g", "b", "(Z)V",
                0x0002, 1, ENTRY, SAVE_WORLD, "World.saveWorld(boolean) core"),
        // Minecraft 换世界流程 a(World,String) 入口：slot1=活体 World（a3-0618-1 字节码实锤）。
        new Target("net/minecraft/client/d", "a", "(Lnet/minecraft/a/a/g;Ljava/lang/String;)V",
                0x0002, 1, ENTRY, LOAD_WORLD, "Minecraft.loadWorld(World,String)"),
        // tick 汇线：Minecraft.runTick()（verscan §2 private void l()V，run() 内 invokespecial 钉）
        new Target("net/minecraft/client/d", "l", "()V",
                0x0002, 1, ENTRY, RUN_TICK, "Minecraft.runTick()"),
        // HUD 调试面补钉（A3 实跑派生修复，用户判决 2026-11-14）：与 0327 同形同字母。
        // onHud 反射链字母两版全同（thePlayer=f、font=m、settings=u、showFPS=d），
        // 唯一差异=Entity pos 字母（0327 g/h/i vs 0618 h/i/j）——已由
        // MCAHooks.onHud 走 VersionRegistry.current().posXF/Y/Z 表化兜住。
        new Target("net/minecraft/client/c/m", "a", "(F)V",
                0x0011, 1, EXIT, HUD_RENDER, "GuiIngame.renderGameOverlay(float)"),
    };

    /**
     * 0629 钩组（第三版，G3 扁平散件族首例——类名为根类无包路径）。
     * SAVE_WORLD 活体二次翻案（el0629-2 实跑）：iw=**具体类**（public class iw，构造器 iw(at)）
     * 非接口——Proxy no-op 方案作废（无头假类 iw 误写成 interface=假绿，教训 20）；
     * 本体 a(boolean,iw) 有完整 null 守卫（ifnull@13/25）且游戏构造器尾 490 处裸传 null——
     * ops 直调 public 包装 a(null) 即可。钩回挂 public 包装 a(Liw;)V 0x0011
     * （InjectMV else 分支 ICONST_0：包装无布尔参，indirect 观察位=常量 0，
     * 包装恒直调本体）。游戏内部 UI 进度调用亦经包装→钩会记录之（观察面更全，无害）。
     * LOAD_WORLD：Minecraft(at).a(ez,String) private（与 0618 同形态，slot1=活体 World）。
     * RUN_TICK：at.m()V private（详表 §2，run() 内 invokespecial）。
     * HUD_RENDER：gf.a(F)V public final 0x0011（详表 §6 与 0327/0618 全同形）。
     * 省略：WORLD_CTOR（cmd= 覆盖，同 0618 判决）、LOAD_CHUNK/GEN_HEIGHT（未钉）、
     * GIANT_CTOR（aa 构造健在，G3 无复活需求）。
     */
    public static final Target[] TARGETS_0629 = {
        new Target("ez", "a", "(Liw;)V",
                0x0011, 1, ENTRY, SAVE_WORLD, "World.saveWorld(progress) public wrapper"),
        new Target("at", "a", "(Lez;Ljava/lang/String;)V",
                0x0002, 1, ENTRY, LOAD_WORLD, "Minecraft.loadWorld(World,String)"),
        new Target("at", "m", "()V",
                0x0002, 1, ENTRY, RUN_TICK, "Minecraft.runTick()"),
        new Target("gf", "a", "(F)V",
                0x0011, 1, EXIT, HUD_RENDER, "GuiIngame.renderGameOverlay(float)"),
    };

    /**
     * a1.1.2_01 钩组（Alpha 首例，G3-alpha：扁平散件 World=cn/Gui=lu，但主类未混淆
     * =net/minecraft/client/Minecraft 自带 main）。详表 verscan/a1.1.2_01.md §9。
     * SAVE_WORLD：cn.a(Lnu)V public 包装（el1121-1 活体翻案：初钉 a(Z)Z 系 0629 翻案①同型误判——
     * 体首串 "TickNextTick list out of synch"=tick 本体，g() 每 tick 直调致 SAVE_WORLD 刷屏
     * （820 次/75s）且 ops save 无磁盘效果；真链=a(Lnu)V→a(true,nu)→a(ZLnu)V 本体，
     * 本体 nu null 守卫齐全（ifnull@13）→ops 传 null 直调包装，0629 配方复用）。
     * WORLD_CTOR：cn(File,String) public——本版启用（详表 §2：Minecraft 换世界流程唯一
     * new 点，dir 面入把手，同 0327 双钩保底）。
     * LOAD_WORLD：Minecraft.a(cn,String)——详表 §7 标 private，javap 复核=**public**
     * （0x0001 按实钉；slot1=活体 World，0618 同形态保底）。
     * RUN_TICK：Minecraft.i()V **public**（档位二：0327/0629 private——五项全中按实访问位）。
     * HUD_RENDER：lu.a(FZII)V public EXIT（档位二换形：0327/0618/0629 a(F)V 形失效——
     * iq L1110 渲染路径 invokevirtual 实锤，hud-1 教训规避）。onHud 反射链字母 f/m/u/d
     * 在本版必断（thePlayer=g、font=o）→HUD_HOOK_DISABLED 优雅降级=钩已触发证据，
     * 0629 同族先例（el0629-1/2/3 cz.a 断链判决）。
     */
    public static final Target[] TARGETS_1121 = {
        new Target("cn", "a", "(Lnu;)V",
                0x0001, 1, ENTRY, SAVE_WORLD, "World.saveWorld(progress) public wrapper"),
        new Target("cn", "<init>", "(Ljava/io/File;Ljava/lang/String;)V",
                0x0001, 1, ENTRY, WORLD_CTOR, "World.<init>(File,String)"),
        new Target("net/minecraft/client/Minecraft", "a", "(Lcn;Ljava/lang/String;)V",
                0x0001, 1, ENTRY, LOAD_WORLD, "Minecraft.loadWorld(World,String)"),
        new Target("net/minecraft/client/Minecraft", "i", "()V",
                0x0001, 1, ENTRY, RUN_TICK, "Minecraft.runTick()"),
        new Target("lu", "a", "(FZII)V",
                0x0001, 1, EXIT, HUD_RENDER, "GuiIngame.renderGameOverlay(float,boolean,int,int)"),
    };

    /**
     * a1.2.0 钩组（G3-alpha 第二版，生物群系带接入批）。详表 verscan/a1.2.0_02.md §1-§9。
     * 五钩字母全漂移于 a1.1.2_01（cn→cu/lu→mz/nu→ph/bi→bo），钩形逐面同构：
     * SAVE_WORLD：cu.a(Lph)V public 包装（javap -c 对榫=与 a1.1.2_01 翻案后同链：
     * 包装→iconst_1→a(ZLph)V 本体，本体 L980 起 "Saving level"→o()V 写 level.dat
     * +"Saving chunks"→bb.a(ZLph)Z；本体 ph null 守卫 ifnull 齐全→ops 传 null 直调包装，
     * 0629/a1.1.2_01 配方三连用。⚠️cu.a(Z)Z=TickUpdates 禁钩（el1121-1 同款陷阱）。
     * WORLD_CTOR：cu(File,String) public（构造族四重载之基础形，同 0327/a1.1.2_01 判决）。
     * LOAD_WORLD：Minecraft.a(cu,String,dv) public 0x0001——2026-11-19 维度跟随移植批
     * （判决 B 同 b1.8.1 e3375b7）由 2 参委托壳改挂 3 参实体：javap 实锤 2 参壳
     * aconst_null 委托 3 参+传送门 k() 两处直调 3 参=三径全汇超集；注入位=EXIT
     * （体首段存旧世界 cu.a(Lph;)V 经 onSaveWorld 切回旧界，EXIT 在存界+addEntity 后
     * 捕获=双向终态；单一尾 RETURN @341），slot1=World 形不变，a(null,...) 走 null 守卫。
     * RUN_TICK：Minecraft.i()V public（run() tick 循环 invokevirtual i()+try/catch 置 e=null
     * 调 a(Lcu;)V 对榫——a1.1.2_01 同字母同形）。
     * HUD_RENDER：mz.a(FZII)V public EXIT（tiny 直钉 renderGameOverlay+jq.b(F)V L1225
     * invokevirtual 调用点双实锤）。onHud 反射链字母本版未钉→HUD_HOOK_DISABLED 优雅降级
     * =钩已触发证据（a1.1.2_01/0629 同族判决）。
     */
    public static final Target[] TARGETS_120 = {
        new Target("cu", "a", "(Lph;)V",
                0x0001, 1, ENTRY, SAVE_WORLD, "World.saveWorld(progress) public wrapper"),
        new Target("cu", "<init>", "(Ljava/io/File;Ljava/lang/String;)V",
                0x0001, 1, ENTRY, WORLD_CTOR, "World.<init>(File,String)"),
        new Target("net/minecraft/client/Minecraft", "a", "(Lcu;Ljava/lang/String;Ldv;)V",
                0x0001, 1, EXIT, LOAD_WORLD, "Minecraft.changeWorld(World,String,Entity)"),
        new Target("net/minecraft/client/Minecraft", "i", "()V",
                0x0001, 1, ENTRY, RUN_TICK, "Minecraft.runTick()"),
        new Target("mz", "a", "(FZII)V",
                0x0001, 1, EXIT, HUD_RENDER, "GuiIngame.renderGameOverlay(float,boolean,int,int)"),
    };

    /**
     * b1.2_02 钩组（Beta 首例，G3-beta：扁平散件 World=dn/GuiIngame=pe，主类未混淆）。
     * 详表 verscan/b1.2_02.md §2/§6/§7（借 b1.2 tiny 直钉+javap 逐验）。
     * SAVE_WORLD：dn.a(Lru)V public 包装（IProgressUpdate ph→ru 档位二断代；本体
     * a(ZLru)V null 守卫齐全=0629/a1.2.0 配方三连用；a(Z)Z=TickUpdates 禁钩同字母预避）。
     * WORLD_CTOR：dn(File,String) public（启用，同 a1.2.0 双钩保底）。
     * LOAD_WORLD：Minecraft.a(dn,String,eu) public 0x0001——2026-11-19 维度跟随移植批
     * （判决 B 同 b1.8.1 e3375b7）由 2 参委托壳改挂 3 参实体：javap 实锤 2 参壳
     * aconst_null 委托 3 参+传送门 k() 两处直调 3 参=三径全汇超集；注入位=EXIT
     * （单一尾 RETURN @395，形制同 120）。
     * RUN_TICK：Minecraft.i()V public ✓同字母同形。
     * HUD_RENDER：pe.a(FZII)V public EXIT（钩形与 a1.2.0 mz 完全同构——ll
     * (EntityRenderer) 字节码 L281 invokevirtual pe.a:(FZII)V 调用点实锤）。
     * onHud 反射链字母本版未钉→HUD_HOOK_DISABLED 优雅降级=钩已触发证据（同族判决）。
     * 省略：LOAD_CHUNK/GEN_HEIGHT/GIANT_CTOR（同 a1.2.0 判决）。
     */
    public static final Target[] TARGETS_1202 = {
        new Target("dn", "a", "(Lru;)V",
                0x0001, 1, ENTRY, SAVE_WORLD, "World.saveWorld(progress) public wrapper"),
        new Target("dn", "<init>", "(Ljava/io/File;Ljava/lang/String;)V",
                0x0001, 1, ENTRY, WORLD_CTOR, "World.<init>(File,String)"),
        new Target("net/minecraft/client/Minecraft", "a", "(Ldn;Ljava/lang/String;Leu;)V",
                0x0001, 1, EXIT, LOAD_WORLD, "Minecraft.changeWorld(World,String,Entity)"),
        new Target("net/minecraft/client/Minecraft", "i", "()V",
                0x0001, 1, ENTRY, RUN_TICK, "Minecraft.runTick()"),
        new Target("pe", "a", "(FZII)V",
                0x0001, 1, EXIT, HUD_RENDER, "GuiIngame.renderGameOverlay(float,boolean,int,int)"),
    };

    /**
     * b1.7.3 钩组（Beta 1.7.3，McRegion 世代首例——存储断代批）。
     * 详表 verscan/b1.7.3.md §9（javap 全量实锤，件 SHA1 43db9b49… 纯独立件 stamp 自证）。
     * SAVE_WORLD：fd.a(Lyb;)V public 包装（IProgressUpdate ru→yb 断代；本体 a(ZLyb)V
     * null 守卫=0629/a1.2.0/b1.2_02 配方四连用；a(Z)Z=TickUpdates 禁钩同字母预避）。
     * LOAD_WORLD：Minecraft.a(fd,String,gs) public 0x0001——2026-11-19 维度跟随移植批
     * （判决 B 同 b1.8.1 e3375b7）由 2 参委托壳改挂 3 参实体：javap 实锤 2 参壳
     * aconst_null 委托 3 参+传送门 m() 两处直调 3 参=三径全汇超集；注入位=EXIT
     * （单一尾 RETURN @366，形制同 120）。
     * RUN_TICK：Minecraft.k()V **public**（⚠️ 字母断代 i→k——b1.2_02=i()V 惯例破裂，
     * 详表 §7 run() 调用点 invokespecial k()V 实锤；五项全中按实访问位）。
     * HUD_RENDER：uq.a(FZII)V public EXIT（钩形与 a1.1.2_01..b1.2_02 全同构）。
     * onHud 反射链字母本版未钉（theWorld=f/thePlayer=h/ingameGUI=v/gameSettings=z=kv）
     * →HUD_HOOK_DISABLED 优雅降级=钩已触发证据（同族判决）。
     * 省略：WORLD_CTOR（fd(File,String) 已消失——构造器族=fd(wt,String,J)/
     * (wt,String,xa,J)/(fd,xa)，dir 面走 cmd=，0618/0629 先例）、
     * LOAD_CHUNK/GEN_HEIGHT/GIANT_CTOR（同族判决，nt 构造健在）。
     */
    public static final Target[] TARGETS_173 = {
        new Target("fd", "a", "(Lyb;)V",
                0x0001, 1, ENTRY, SAVE_WORLD, "World.saveWorld(progress) public wrapper"),
        new Target("net/minecraft/client/Minecraft", "a", "(Lfd;Ljava/lang/String;Lgs;)V",
                0x0001, 1, EXIT, LOAD_WORLD, "Minecraft.changeWorld(World,String,Entity)"),
        new Target("net/minecraft/client/Minecraft", "k", "()V",
                0x0001, 1, ENTRY, RUN_TICK, "Minecraft.runTick()"),
        new Target("uq", "a", "(FZII)V",
                0x0001, 1, EXIT, HUD_RENDER, "GuiIngame.renderGameOverlay(float,boolean,int,int)"),
    };

    /**
     * b1.8.1 钩组（Beta 1.8.1，9 代表版线线尾件——旁证直转批）。
     * 详表 verscan/b1.8.1.md §1（借 retromcp b1.8 原生 tiny+实件逐锚复核；件
     * SHA1 6b562463… 对榫主卷 §0，stamp=Minecraft Beta 1.8.1 自证）。
     * **锚面零漂移判决**：117 类 javap dump 逐类 hash 对照 b1.8=116/117 字节级
     * 全同，唯一漂移 cf(Container) a(Lul;IIZ) 返回值 V→boolean（不在 ops 消费链）。
     * SAVE_WORLD：rv.a(Lny;)V public 包装（fd→rv、yb→ny 断代；本体 a(ZLny)V；
     * a(Z)Z=TickUpdates 禁钩同字母预避）。
     * LOAD_WORLD：Minecraft.a(rv,String,sz) public 0x0001——**2026-11-18 维度跟随修复
     * （el181-3 判决 B）由 2 参委托壳改挂 3 参实体**：loadWorld 启动、handleRespawn
     * （1→2→3 参）、传送门 m()（直调 3 参绕过 2 参）三径全汇 3 参=通用超集（a1.1.2_01
     * 已证 (cu,String,dv) 形态 alpha 起存在）；**注入位=EXIT**（el181-4 活体实测修订：ENTRY
     * 捕获被方法体首行存旧世界经 onSaveWorld 切回旧界，EXIT 在存界+addEntity 后捕获=双向终态
     * 正确），slot1=World 形不变，a(null,...) 清场仍走 onWorldLoaded null 守卫。
     * RUN_TICK：Minecraft.k()V public（MC dump 与 b1.8 字节级全同——b1.7.3 断代值 k 保留）。
     * HUD_RENDER：abj.a(FZII)V public EXIT（uq→abj 仅类字母；onHud 链未钉=
     * HUD_HOOK_DISABLED 优雅降级）。
     * 省略：WORLD_CTOR（rv 构造族=(acq,String,dx)/(acq,String,dx,acz)/(rv,acz)——
     * WorldSettings dx 参=b1.8 系新面，dir 面走 cmd=）、LOAD_CHUNK/GEN_HEIGHT/GIANT_CTOR。
     */
    public static final Target[] TARGETS_181 = {
        new Target("rv", "a", "(Lny;)V",
                0x0001, 1, ENTRY, SAVE_WORLD, "World.saveWorld(progress) public wrapper"),
        new Target("net/minecraft/client/Minecraft", "a", "(Lrv;Ljava/lang/String;Lsz;)V",
                0x0001, 1, EXIT, LOAD_WORLD, "Minecraft.changeWorld(World,String,Entity)"),
        new Target("net/minecraft/client/Minecraft", "k", "()V",
                0x0001, 1, ENTRY, RUN_TICK, "Minecraft.runTick()"),
        new Target("abj", "a", "(FZII)V",
                0x0001, 1, EXIT, HUD_RENDER, "GuiIngame.renderGameOverlay(float,boolean,int,int)"),
    };

    /**
     * b1.9-pre2 钩组（Beta 1.9 pre-release 2，活体测试转正批 2026-11-25）。
     * 锚库 java/agent/registry/fingerprints/b1.9-pre2.json hooks 段四节对榫
     * （A2Table19pre2 五项全中纪律断言）；取证=%TEMP%\mcaut\exp\b19pre2\。
     * SAVE_WORLD：ti.a(Lpd;)V public 包装（IProgressUpdate yb→ny→**pd** 断代；
     * saveWorldIndirectly→本体 a(ZLpd)V iconst_1 委托实锤 world_c@3，173 包装配方适用；
     * a(Z)Z=TickUpdates 禁钩同字母预避）。
     * LOAD_WORLD：Minecraft.a(Lti;Ljava/lang/String;Luo;)V public 0x0001——3 参实体
     * （判决 B 三径全汇超集同 181；2 参壳 aconst_null 委托同形实锤）；注入位=EXIT
     * （el181-4 修订沿用：存界+addEntity 后捕获=双向终态），slot1=活体 World。
     * RUN_TICK：Minecraft.k()V public（173/181 同字母 k 保留——ti.c:()V=World.tick
     * 调用点实锤）。
     * HUD_RENDER：adm.a(FZII)V public EXIT（GuiIngame uq→abj→**adm** 断代；
     * ⚠️ 撞车位：173 uq=GuiIngame↔pre2 uq=EntityMob）。gk.E=showDebugInfo 门读面
     * 单点对榫（173=kv.B/181=fv.E）。≥a1.2.3 边界线=XYZ 注入免做（hudXyzPaint=false）。
     * 省略：WORLD_CTOR（ti 构造族=(aew,String,ek)/(aew,String,ek,aff)/(ti,aff) 系——
     * WorldSettings/SaveHandler 参，dir 面走 cmd=，173/181 先例）、
     * LOAD_CHUNK/GEN_HEIGHT/GIANT_CTOR（同族判决，ip 构造健在）。
     */
    public static final Target[] TARGETS_19pre2 = {
        new Target("ti", "a", "(Lpd;)V",
                0x0001, 1, ENTRY, SAVE_WORLD, "World.saveWorld(progress) public wrapper"),
        new Target("net/minecraft/client/Minecraft", "a", "(Lti;Ljava/lang/String;Luo;)V",
                0x0001, 1, EXIT, LOAD_WORLD, "Minecraft.changeWorld(World,String,Entity)"),
        new Target("net/minecraft/client/Minecraft", "k", "()V",
                0x0001, 1, ENTRY, RUN_TICK, "Minecraft.runTick()"),
        new Target("adm", "a", "(FZII)V",
                0x0001, 1, EXIT, HUD_RENDER, "GuiIngame.renderGameOverlay(float,boolean,int,int)"),
    };

    /**
     * b1.9-pre3-1402 钩组（Beta 1.9 pre-release 3，药水活体落地批 2026-11-28 最小转正）。
     * 取证=%TEMP%\mcaut\exp\potcmp\all-p3.txt javap 全量，按 pre2 ti/pd/uo/adm 同族
     * 签名匹配逐字对榫（非字母规律猜）：
     * SAVE_WORLD：uc.a(Lpr;)V public 包装（体 iconst_1→a(ZLpr;)V 核心委托=pre2
     * ti.a(Lpd;)V 同包装配方实锤；uc=World 特征四参构造 uc(agr,String,agk,em)+
     * 切片构造 uc(uc,agk)；IProgressUpdate pd→**pr** 断代）。
     * LOAD_WORLD：Minecraft.a(Luc;Ljava/lang/String;Lvj;)V public 0x0001——3 参实体
     * （2 参壳 a(Luc;Ljava/lang/String;)V 同在，pre2 陷阱教训复用=按实体钉）；
     * 注入位=EXIT，slot1=活体 World。
     * RUN_TICK：Minecraft.k()V public（判决法=体首调用序列与 pre2 k() 对榫：
     * z()V→ago.d()V→aeo.a()V→kd.a(F)V→uc.w()Lbi; 五连同构 pre2 的
     * z/afj.d/adm.a/jt.a/ti.w；i()V/j()V 为初始化小体排除——本窗 javap -c 实锤）。
     * HUD_RENDER：aeo.a(FZII)V public EXIT（aeo extends mr+唯一构造 aeo(Minecraft)
     * =pre2 adm 同形；Minecraft.k() 体 invokevirtual aeo.a:(FZII)V 调用点实锤；
     * GuiIngame adm→**aeo** 断代）。
     * 省略：WORLD_CTOR（uc 构造族=(agr,String,agk,em) 系，dir 面走 cmd=，
     * 173/181/pre2 先例）、LOAD_CHUNK/GEN_HEIGHT/GIANT_CTOR（同族判决免钉）。
     */
    public static final Target[] TARGETS_19pre3 = {
        new Target("uc", "a", "(Lpr;)V",
                0x0001, 1, ENTRY, SAVE_WORLD, "World.saveWorld(progress) public wrapper"),
        new Target("net/minecraft/client/Minecraft", "a", "(Luc;Ljava/lang/String;Lvj;)V",
                0x0001, 1, EXIT, LOAD_WORLD, "Minecraft.changeWorld(World,String,EntityPlayer)"),
        new Target("net/minecraft/client/Minecraft", "k", "()V",
                0x0001, 1, ENTRY, RUN_TICK, "Minecraft.runTick()"),
        new Target("aeo", "a", "(FZII)V",
                0x0001, 1, EXIT, HUD_RENDER, "GuiIngame.renderGameOverlay(float,boolean,int,int)"),
    };

    /**
     * b1.5_01 钩组（Beta 1.5_01，活体批转正 2026-11-28）。
     * 锚库 java/agent/registry/fingerprints/b1.5_01.json hooks 段四节对榫
     * （A2Table1501Test 六元组断言在案）；取证=%TEMP%\mcaut\exp\b1501\。
     * SAVE_WORLD：et.a(ZLwr;)V public **双参实体**——⚠️ 本版无 1 参包装
     * （173/181/pre2 包装配方不适用；wr null 守卫 @14/@30 实锤；a(Z)Z=
     * TickUpdates 禁钩同形预避）。WorldOps save (false,null) 双参分支直触本钩。
     * LOAD_WORLD：Minecraft.a(Let;Ljava/lang/String;Lgh;)V public 0x0001——
     * 3 参实体（判决 B 同 173：2 参壳 aconst_null 委托 3 参实锤 mc_c@3398，
     * 三径全汇超集）；注入位=EXIT，slot1=活体 World。
     * RUN_TICK：Minecraft.j()V public（⚠️ 173/181/pre2=k 断代——j→k 翻面
     * 下界=b1.6，本版仍 j；et.l:()V=World.tick 调用点 @1239 实锤）。
     * HUD_RENDER：tl.a(FZII)V public EXIT（GuiIngame uq→tl 断代；onHud 链
     * h/p/y/B 已钉=观测+降级双路；≥a1.2.3 边界线 XYZ 注入免做）。
     * 省略：WORLD_CTOR（et 构造族=(wt,String,J) 系，dir 面走 cmd=，173 先例）、
     * LOAD_CHUNK/GEN_HEIGHT/GIANT_CTOR（同族判决，mx 构造健在）。
     */
    public static final Target[] TARGETS_1501 = {
        new Target("et", "a", "(ZLwr;)V",
                0x0001, 1, ENTRY, SAVE_WORLD, "World.saveWorld(boolean,IProgressUpdate) two-arg entity"),
        new Target("net/minecraft/client/Minecraft", "a", "(Let;Ljava/lang/String;Lgh;)V",
                0x0001, 1, EXIT, LOAD_WORLD, "Minecraft.changeWorld(World,String,Entity)"),
        new Target("net/minecraft/client/Minecraft", "j", "()V",
                0x0001, 1, ENTRY, RUN_TICK, "Minecraft.runTick()"),
        new Target("tl", "a", "(FZII)V",
                0x0001, 1, EXIT, HUD_RENDER, "GuiIngame.renderGameOverlay(float,boolean,int,int)"),
    };

    /**
     * b1.4_01 钩组（Beta 1.4_01，活体批转正 2026-11-28）。
     * 锚库 java/agent/registry/fingerprints/b1.4_01.json hooks 段四节对榫
     * （A2Table1401Test 六元组断言在案）；取证=%TEMP%\mcaut\exp\b1401\。
     * SAVE_WORLD：eb.a(ZLtw;)V public **双参实体**（断代带与 1501 同形：
     * tw null 守卫 @14/@30；a(Z)Z=TickUpdates 禁钩同形预避）。
     * LOAD_WORLD：Minecraft.a(Leb;Ljava/lang/String;Lfm;)V public EXIT
     * （2 参壳 aconst_null 委托 3 参实锤 mc_c@3 同 173/1501 判决 B；
     * 3 参头 putfield h aconst_null=thePlayer 复位）。
     * RUN_TICK：Minecraft.j()V public（eb.l:()V 调用点 @1209 实锤；
     * ⚠️173=k 断代同 1501 字母——j→k 翻面下界=b1.6）。
     * HUD_RENDER：qv.a(FZII)V public EXIT（GuiIngame.renderGameOverlay；
     * is.A=showDebugInfo 门字母 A 非 1501 B——hudDebugGateF 断代）。
     * 省略：WORLD_CTOR（eb 构造族=(dj,String,Z) 系，dir 面走 cmd=，173 先例）、
     * LOAD_CHUNK/GEN_HEIGHT/GIANT_CTOR（判决③ giant 未到世）。
     */
    public static final Target[] TARGETS_1401 = {
        new Target("eb", "a", "(ZLtw;)V",
                0x0001, 1, ENTRY, SAVE_WORLD, "World.saveWorld(boolean,IProgressUpdate) two-arg entity"),
        new Target("net/minecraft/client/Minecraft", "a", "(Leb;Ljava/lang/String;Lfm;)V",
                0x0001, 1, EXIT, LOAD_WORLD, "Minecraft.changeWorld(World,String,Entity)"),
        new Target("net/minecraft/client/Minecraft", "j", "()V",
                0x0001, 1, ENTRY, RUN_TICK, "Minecraft.runTick()"),
        new Target("qv", "a", "(FZII)V",
                0x0001, 1, EXIT, HUD_RENDER, "GuiIngame.renderGameOverlay(float,boolean,int,int)"),
    };

    /**
     * b1.6 钩组（Beta 1.6，转正批 2026-11-28）。
     * 锚库 java/agent/registry/fingerprints/b1.6.json hooks 段四节对榫
     * （A2Table16Test 六元组断言在案）；取证=%TEMP%\mcaut\exp\b16\。
     * SAVE_WORLD：fb.a(ZLxs;)V public **双参实体**（断代带 b1.4→b1.6 延续：
     * xs null 守卫 @14/@30 与 1401 逐位同形；前置 v:Lcj; 旗；a(Z)Z=
     * TickUpdates 禁钩同形预避）。WorldOps save (false,null) 双参分支直触本钩。
     * LOAD_WORLD：Minecraft.a(Lfb;Ljava/lang/String;Lgq;)V public 0x0001——
     * 3 参实体（判决 B：2 参壳 @3 aconst_null 委托 3 参实锤 mc_c，三径全汇
     * 超集）；注入位=EXIT，3 参头 @16 i:Llo;=null renderViewEntity+@105
     * h:Lda;=null thePlayer 复位，slot1=活体 World。
     * RUN_TICK：Minecraft.**k()V** public（⚠️ 1401/1501=j 断代——**j→k 翻面
     * 下界提前至 b1.6 实锤**，173/181/pre2=k 同字母；fb.l()V=World.tick 调用点
     * mc_c @1260 实锤）。
     * HUD_RENDER：uk.a(FZII)V public EXIT（GuiIngame 1501 tl→1401 qv→16 uk
     * 断代；kr.B=showDebugInfo @1142/kr.A=thirdPersonView @117——偏移三版逐位
     * 同形；≥a1.2.3 边界线=XYZ 注入免做，onHud 观测单点）。
     * 省略：WORLD_CTOR（锚库未直钉 fb 构造族形，dir 面走 cmd=，173/1501/1401
     * 先例）、LOAD_CHUNK/GEN_HEIGHT/GIANT_CTOR（同族判决，np=EntityGiant 构造
     * 健在（tiny 类级直钉+ctor (Lfb;)V 全中）无复活需求）。
     */
    public static final Target[] TARGETS_16 = {
        new Target("fb", "a", "(ZLxs;)V",
                0x0001, 1, ENTRY, SAVE_WORLD, "World.saveWorld(boolean,IProgressUpdate) two-arg entity"),
        new Target("net/minecraft/client/Minecraft", "a", "(Lfb;Ljava/lang/String;Lgq;)V",
                0x0001, 1, EXIT, LOAD_WORLD, "Minecraft.changeWorld(World,String,Entity)"),
        new Target("net/minecraft/client/Minecraft", "k", "()V",
                0x0001, 1, ENTRY, RUN_TICK, "Minecraft.runTick()"),
        new Target("uk", "a", "(FZII)V",
                0x0001, 1, EXIT, HUD_RENDER, "GuiIngame.renderGameOverlay(float,boolean,int,int)"),
    };

    private static final Map<String, Target[]> GROUPS = new HashMap<String, Target[]>();

    static {
        GROUPS.put("inf-20100327", TARGETS_0327);
        GROUPS.put("inf-20100618", TARGETS_0618);
        GROUPS.put("inf-20100629", TARGETS_0629);
        GROUPS.put("a1.1.2_01", TARGETS_1121);
        GROUPS.put("a1.2.0", TARGETS_120);
        GROUPS.put("b1.2_02", TARGETS_1202);
        GROUPS.put("b1.7.3", TARGETS_173);
        GROUPS.put("b1.8.1", TARGETS_181);
        GROUPS.put("b1.9-pre2", TARGETS_19pre2);
        GROUPS.put("b1.9-pre3-1402", TARGETS_19pre3);
        GROUPS.put("b1.5_01", TARGETS_1501);
        GROUPS.put("b1.4_01", TARGETS_1401);
        GROUPS.put("b1.6", TARGETS_16);
    }

    private static volatile Map<String, List<Target>> BY_CLASS = index(TARGETS_0327);

    private static Map<String, List<Target>> index(Target[] group) {
        Map<String, List<Target>> m = new HashMap<String, List<Target>>();
        for (Target t : group) {
            List<Target> list = m.get(t.className);
            if (list == null) {
                list = new ArrayList<Target>();
                m.put(t.className, list);
            }
            list.add(t);
        }
        return m;
    }

    /** premain 期版本激活（未知 id=保持 0327 组=现行为零变化；A2 后按 GROUPS 表扩展。
     *  B5：GROUPS 未收录且激活表带 JSON hookSpecs=数据驱动组（0624/a1.0.3 起）；
     *  硬编码 8 组命中路径零改动。 */
    public static synchronized void activate(String versionId) {
        Target[] group = GROUPS.get(versionId);
        if (group == null) {
            group = fromTable(VersionRegistry.byVersionId(versionId));
        }
        BY_CLASS = index(group == null ? TARGETS_0327 : group);
    }

    /** JSON hookSpecs→钩组（class|name|desc|access|hits|mode 六元组逐条解析，
     *  key 升序=确定性）。NEW_CTOR 拒收（六元组无 superOwner 面）；任何违例=
     *  响亮跳过整组返回 null（回退 0327=改前行为）。 */
    static Target[] fromTable(VersionTable t) {
        if (t == null || t.hookSpecs.isEmpty()) {
            return null;
        }
        String[] keys = t.hookSpecs.keySet().toArray(new String[0]);
        java.util.Arrays.sort(keys);
        List<Target> out = new ArrayList<Target>();
        for (int i = 0; i < keys.length; i++) {
            String key = keys[i];
            String spec = t.hookSpecs.get(key);
            String[] p = spec.split("\\|", -1);
            String hookId = hookIdOf(key);
            int mode;
            int access;
            int hits;
            try {
                if (hookId == null || p.length != 6) {
                    throw new IllegalArgumentException("bad key/arity");
                }
                mode = modeOf(p[5]);
                if (mode < 0 || mode == NEW_CTOR) {
                    throw new IllegalArgumentException("mode '" + p[5] + "' unsupported from JSON");
                }
                access = Integer.parseInt(p[3]);
                hits = Integer.parseInt(p[4]);
            } catch (RuntimeException bad) {
                MCWorldAgent.logTransform("HOOK-JSON-SKIP " + t.versionId + " " + key
                        + " " + bad + " -> fallback group");
                return null;
            }
            out.add(new Target(p[0], p[1], p[2], access, hits, mode, hookId,
                    "json:" + t.versionId + ":" + key));
        }
        return out.toArray(new Target[0]);
    }

    /** 钩名 key→hookId 常量白名单（与注入 dispatch 面同名，零特判版本）。 */
    static String hookIdOf(String key) {
        if ("SAVE_WORLD".equals(key)) return SAVE_WORLD;
        if ("WORLD_CTOR".equals(key)) return WORLD_CTOR;
        if ("LOAD_WORLD".equals(key)) return LOAD_WORLD;
        if ("RUN_TICK".equals(key)) return RUN_TICK;
        if ("LOAD_CHUNK".equals(key)) return LOAD_CHUNK;
        if ("GEN_HEIGHT".equals(key)) return GEN_HEIGHT;
        if ("GIANT_CTOR".equals(key)) return GIANT_CTOR;
        if ("HUD_RENDER".equals(key)) return HUD_RENDER;
        return null;
    }

    static boolean isKnownHookKey(String key) {
        return hookIdOf(key) != null;
    }

    /** 注入位名→常量；未知=-1（装载校验拒收）。 */
    static int modeOf(String mode) {
        if ("ENTRY".equals(mode)) return ENTRY;
        if ("ENTRY_AND_RESULT".equals(mode)) return ENTRY_AND_RESULT;
        if ("NEW_CTOR".equals(mode)) return NEW_CTOR;
        if ("EXIT".equals(mode)) return EXIT;
        return -1;
    }

    public static Target[] forClass(String internalName) {
        List<Target> list = BY_CLASS.get(internalName);
        return list == null ? new Target[0] : list.toArray(new Target[0]);
    }

    private HookTargets() {
    }
}
