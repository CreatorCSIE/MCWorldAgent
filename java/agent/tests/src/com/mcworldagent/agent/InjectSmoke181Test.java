// [B5-ARCHIVE 2026-11-22] headless test archived from %TEMP%\mcaut (source batch: B b1.8.1); run via java/agent/tests/run.ps1
package com.mcworldagent.agent;

import java.io.File;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

/**
 * b1.8.1 四项钩实形注入冒烟（TARGETS_181 全中）：
 * - 实形 rv（a2out181/rv.class 真字节）：save-world a(Lny;)V 0x0001 ENTRY 命中；
 *   WORLD_CTOR 本版省略（rv 构造族=(acq,String,dx) 系）→ capturedSaveDir 恒 null=降级面自证；
 * - 实形 Minecraft（ASM 合成）：load-world a(Lrv;String;sz)V 3 参实体（判决 B：2026-11-18
 *   维度跟随修复，钩自 2 参委托壳改挂 3 参实体；2 参壳同形合成，委托路径恰中 1 证超集）
 *   + run-tick k()V 双中（MC dump 与 b1.8 字节级全同——k 形保留）；
 * - 实形 abj（a2out181/abj.class）：hud-render a(FZII)V 0x0001 EXIT 命中；
 *   onHud 链本版未钉 → HUD_HOOK_DISABLED 优雅降级=钩已触发证据。
 * parent-last Loader 隔离假件闭包（rv 返回体 rl/nu/zf/ij/ny/kj 须同 loader 解析）。
 * 跨组隔离含撞车位件：181 激活期 sn/jc（173 身份）零钩；173 激活期 rv/abj/rl/kj/um 零钩。
 */
public class InjectSmoke181Test {

    static int checks = 0;

    static void ok(boolean c, String msg) {
        checks++;
        if (!c) {
            throw new AssertionError(msg);
        }
    }

    static class Loader extends ClassLoader {
        private final Map<String, byte[]> bytes = new HashMap<String, byte[]>();
        private final Map<String, Class<?>> done = new HashMap<String, Class<?>>();

        Loader(ClassLoader parent) {
            super(parent);
        }

        void put(String name, byte[] b) {
            bytes.put(name, b);
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            Class<?> c = findLoadedClass(name);
            if (c == null && done.containsKey(name)) {
                c = (Class<?>) done.get(name);
            }
            if (c == null) {
                byte[] b = (byte[]) bytes.get(name);
                if (b != null) {
                    c = defineClass(name, b, 0, b.length);
                    done.put(name, c);
                } else {
                    return super.loadClass(name, resolve);
                }
            }
            if (resolve) {
                resolveClass(c);
            }
            return c;
        }
    }

    static HookTargets.Target find(HookTargets.Target[] ts, String hookId) {
        for (int i = 0; i < ts.length; i++) {
            if (hookId.equals(ts[i].hookId)) {
                return ts[i];
            }
        }
        return null;
    }

    static byte[] readTmp(String rel) throws Exception {
        return java.nio.file.Files.readAllBytes(
                new File(System.getProperty("java.io.tmpdir"), "mcaut\\tests-out\\" + rel).toPath());
    }

    static int countSub(String s, String sub) {
        int n = 0, i = 0;
        while ((i = s.indexOf(sub, i)) >= 0) {
            n++;
            i += sub.length();
        }
        return n;
    }

    /** ASM 合成 b1.8.1 Minecraft 真形：f:Lrv;（theWorld）+ a(Lrv;String;sz)V 3 参实体
     *  （putfield f）+ a(Lrv;String)V 2 参委托壳（ACONST_NULL→invokevirtual 3 参，实形对榫）
     *  + k()V。h/v/z 系字段必须省略（同 173 教训：链接期实例尺寸计算解析全部非静态字段描述符类型，
     *  sz/abj/fv 未预置入 map 会落 parent→woven 版永不可达）；字段面由 A2Table181Test 钉表覆盖。
     *  2026-11-18 判决 B：LOAD_WORLD 钩目标=3 参实体（三径全汇超集），2 参壳不钩。
     *  2026-11-18 判决：181 ≥a1.2.3 边界线=XYZ 注入免做（hudXyzPaint=false），
     *  onHud 不触绘制链，链字母 h/q/z/E 仅锚数据。 */
    static byte[] asmMinecraft181() {
        org.objectweb.asm.ClassWriter cw =
                new org.objectweb.asm.ClassWriter(org.objectweb.asm.ClassWriter.COMPUTE_MAXS);
        cw.visit(org.objectweb.asm.Opcodes.V1_8, org.objectweb.asm.Opcodes.ACC_PUBLIC,
                "net/minecraft/client/Minecraft", null, "java/lang/Object", null);
        cw.visitField(org.objectweb.asm.Opcodes.ACC_PUBLIC, "f", "Lrv;", null, null).visitEnd();
        org.objectweb.asm.MethodVisitor mv = cw.visitMethod(org.objectweb.asm.Opcodes.ACC_PUBLIC,
                "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(org.objectweb.asm.Opcodes.ALOAD, 0);
        mv.visitMethodInsn(org.objectweb.asm.Opcodes.INVOKESPECIAL,
                "java/lang/Object", "<init>", "()V", false);
        mv.visitInsn(org.objectweb.asm.Opcodes.RETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();
        // 3 参实体（钩目标）：putfield f=theWorld（真形首行 f.a(s) 存旧世界与 sz 面与本冒烟无关，省略）
        mv = cw.visitMethod(org.objectweb.asm.Opcodes.ACC_PUBLIC,
                "a", "(Lrv;Ljava/lang/String;Lsz;)V", null, null);
        mv.visitCode();
        mv.visitVarInsn(org.objectweb.asm.Opcodes.ALOAD, 0);
        mv.visitVarInsn(org.objectweb.asm.Opcodes.ALOAD, 1);
        mv.visitFieldInsn(org.objectweb.asm.Opcodes.PUTFIELD,
                "net/minecraft/client/Minecraft", "f", "Lrv;");
        mv.visitInsn(org.objectweb.asm.Opcodes.RETURN);
        mv.visitMaxs(2, 4);
        mv.visitEnd();
        // 2 参委托壳（不钩）：a(world, msg) → a(world, msg, null)，实形对榫 @3411 aconst_null 委托
        mv = cw.visitMethod(org.objectweb.asm.Opcodes.ACC_PUBLIC,
                "a", "(Lrv;Ljava/lang/String;)V", null, null);
        mv.visitCode();
        mv.visitVarInsn(org.objectweb.asm.Opcodes.ALOAD, 0);
        mv.visitVarInsn(org.objectweb.asm.Opcodes.ALOAD, 1);
        mv.visitVarInsn(org.objectweb.asm.Opcodes.ALOAD, 2);
        mv.visitInsn(org.objectweb.asm.Opcodes.ACONST_NULL);
        mv.visitMethodInsn(org.objectweb.asm.Opcodes.INVOKEVIRTUAL,
                "net/minecraft/client/Minecraft", "a", "(Lrv;Ljava/lang/String;Lsz;)V", false);
        mv.visitInsn(org.objectweb.asm.Opcodes.RETURN);
        mv.visitMaxs(4, 3);
        mv.visitEnd();
        mv = cw.visitMethod(org.objectweb.asm.Opcodes.ACC_PUBLIC, "k", "()V", null, null);
        mv.visitCode();
        mv.visitInsn(org.objectweb.asm.Opcodes.RETURN);
        mv.visitMaxs(0, 1);
        mv.visitEnd();
        cw.visitEnd();
        return cw.toByteArray();
    }

    public static void main(String[] a) throws Exception {
        // ---- 1. TARGETS_181 结构：四钩全 0x0001 public ----
        VersionRegistry.activate("b1.8.1");
        HookTargets.activate("b1.8.1");
        HookTargets.Target[] w81 = HookTargets.forClass("rv");
        ok(w81.length == 1, "181 World 1 target（SAVE_WORLD；WORLD_CTOR dx 构造断代省略）got " + w81.length);
        HookTargets.Target sw = find(w81, HookTargets.SAVE_WORLD);
        ok(sw != null && "a".equals(sw.methodName) && "(Lny;)V".equals(sw.desc)
                && sw.access == 0x0001 && sw.expectedHits == 1 && sw.injectMode == HookTargets.ENTRY,
                "save-world a(Lny;)V 0x0001 ENTRY");
        ok(find(w81, HookTargets.WORLD_CTOR) == null, "no world-ctor target");
        HookTargets.Target[] m81 = HookTargets.forClass("net/minecraft/client/Minecraft");
        ok(m81.length == 2, "181 Minecraft 2 targets");
        HookTargets.Target lw = find(m81, HookTargets.LOAD_WORLD);
        HookTargets.Target rt = find(m81, HookTargets.RUN_TICK);
        ok(lw != null && "a".equals(lw.methodName) && "(Lrv;Ljava/lang/String;Lsz;)V".equals(lw.desc)
                && lw.access == 0x0001 && lw.expectedHits == 1 && lw.injectMode == HookTargets.EXIT,
                "load-world a(Lrv;String;sz)V 0x0001 EXIT（判决 B+el181-4 修订：存界后捕获）");
        ok(rt != null && "k".equals(rt.methodName) && "()V".equals(rt.desc)
                && rt.access == 0x0001 && rt.injectMode == HookTargets.ENTRY,
                "run-tick k()V 0x0001 ENTRY（MC dump 与 b1.8 全同）");
        HookTargets.Target[] hud81 = HookTargets.forClass("abj");
        ok(hud81.length == 1 && HookTargets.HUD_RENDER.equals(hud81[0].hookId)
                && "a".equals(hud81[0].methodName) && "(FZII)V".equals(hud81[0].desc)
                && hud81[0].access == 0x0001 && hud81[0].injectMode == HookTargets.EXIT,
                "181 HUD abj.a(FZII)V 0x0001 EXIT");
        // 跨组隔离：181 激活期——173 字母+撞车位件（sn/jc）+更早世代零钩
        ok(HookTargets.forClass("fd").length == 0 && HookTargets.forClass("uq").length == 0
                && HookTargets.forClass("ei").length == 0 && HookTargets.forClass("yb").length == 0
                && HookTargets.forClass("qj").length == 0 && HookTargets.forClass("sn").length == 0
                && HookTargets.forClass("jc").length == 0 && HookTargets.forClass("dn").length == 0
                && HookTargets.forClass("pe").length == 0 && HookTargets.forClass("mz").length == 0,
                "181 组：173 字母+撞车位 sn/jc 零钩");
        HookTargets.activate("b1.7.3");
        ok(HookTargets.forClass("rv").length == 0 && HookTargets.forClass("abj").length == 0
                && HookTargets.forClass("rl").length == 0 && HookTargets.forClass("kj").length == 0
                && HookTargets.forClass("um").length == 0, "173 组：181 字母零钩");
        HookTargets.activate("b1.2_02");
        ok(HookTargets.forClass("rv").length == 0 && HookTargets.forClass("abj").length == 0,
                "1202 组：181 字母零钩");
        HookTargets.activate("b1.8.1");

        // ---- 2. 织入 rv：save-world ENTRY(ICONST_0) 单钩恰中（无 world-ctor）----
        byte[] rawRv = readTmp("a2out181\\rv.class");
        MCAHooks.noFirstBrick = true;
        MCAHooks.capturedWorld = null;
        MCAHooks.capturedSaveDir = null;
        java.io.StringWriter sw1 = new java.io.StringWriter();
        MCAHooks.out = new java.io.PrintWriter(sw1);
        java.lang.reflect.Field scf = MCAHooks.class.getDeclaredField("SAVE_CALLS");
        scf.setAccessible(true);
        java.util.concurrent.atomic.AtomicInteger saveCalls =
                (java.util.concurrent.atomic.AtomicInteger) scf.get(null);
        int hooksBefore = saveCalls.get();
        byte[] wovenRv = new Transformers().transform(null, "rv", null, null, rawRv);
        ok(wovenRv != null, "rv transformed");
        ok(wovenRv.length > rawRv.length, "rv bytes grew");

        Loader l = new Loader(InjectSmoke181Test.class.getClassLoader());
        // rv 闭包件同 loader 定义（返回体/参数体类型须与 woven rv 同一 ClassLoader）
        l.put("kj", readTmp("a2out181\\kj.class"));
        l.put("rl", readTmp("a2out181\\rl.class"));
        l.put("ny", readTmp("a2out181\\ny.class"));
        l.put("ij", readTmp("a2out181\\ij.class"));
        l.put("zf", readTmp("a2out181\\zf.class"));
        l.put("nu", readTmp("a2out181\\nu.class"));
        l.put("vh", readTmp("a2out181\\vh.class"));
        l.put("rv", wovenRv);
        Class<?> c = l.loadClass("rv");
        Object world = c.getConstructor().newInstance();
        ok(MCAHooks.capturedSaveDir == null, "无 world-ctor 钩：dir 不捕获（dx 构造断代降级面自证）");
        Class<?> nyC = l.loadClass("ny");
        Method save = c.getMethod("a", nyC);
        ok(saveCalls.get() == hooksBefore, "no calls yet");
        Object ret = save.invoke(world, (Object) null);
        MCAHooks.out.flush();
        String logged = sw1.toString();
        ok(MCAHooks.capturedWorld == world, "onSavedWorld captured world");
        ok(logged.contains("SAVE_WORLD") && logged.contains("indirect=false"),
                "ICONST_0 观察位=false，log=" + logged.trim());
        ok(saveCalls.get() == hooksBefore + 1, "SAVE_CALLS++（单钩恰中 1）");
        ok(((Integer) c.getField("saveCalls").get(world)) == 1, "wrapper->body 委托 saveCalls=1");
        ok(((Integer) c.getField("levelSaves").get(world)) == 1, "body 调 saveLevel r()V levelSaves=1");
        ok(ret == null, "(Lny;)V void 形（入口注入不动返回值）");

        // ---- 3. rv.a(ZLny)V 本体 null 守卫语义（ny 方法面未钉=零回调，不 NPE 即对）----
        Method body = c.getMethod("a", boolean.class, nyC);
        body.invoke(world, Boolean.FALSE, null);
        ok(((Integer) c.getField("saveCalls").get(world)) == 2, "null 守卫：直调本体不 NPE，saveCalls=2");
        ok(((Integer) c.getField("levelSaves").get(world)) == 2, "null 守卫：r()V 仍执行 levelSaves=2");
        Object prog = Proxy.newProxyInstance(l, new Class[] { nyC }, new InvocationHandler() {
            public Object invoke(Object p, Method m, Object[] args) {
                return null;
            }
        });
        body.invoke(world, Boolean.TRUE, prog);
        ok(((Integer) c.getField("saveCalls").get(world)) == 3, "非空 ny（空接口零回调）：saveCalls=3");

        // ---- 4. 织入 Minecraft（ASM 实形）：LOAD_WORLD + RUN_TICK(k 形) 双中 ----
        byte[] rawMc = asmMinecraft181();
        byte[] wovenMc = new Transformers().transform(null, "net/minecraft/client/Minecraft", null, null, rawMc);
        ok(wovenMc != null, "Minecraft transformed");
        ok(wovenMc.length > rawMc.length, "Minecraft bytes grew");
        l.put("net.minecraft.client.Minecraft", wovenMc);
        Class<?> mcC = l.loadClass("net.minecraft.client.Minecraft");
        Object mc = mcC.getConstructor().newInstance();
        long t0 = MCAHooks.tickCount();
        Method loadWorld3 = null;
        Method[] ms = mcC.getMethods();
        for (int i = 0; i < ms.length; i++) {
            if ("a".equals(ms[i].getName()) && ms[i].getParameterTypes().length == 3) {
                loadWorld3 = ms[i];
            }
        }
        ok(loadWorld3 != null, "3 参 changeWorld a(Lrv;String;sz)V 实形在（钩目标）");
        loadWorld3.invoke(mc, world, "W181", null);
        MCAHooks.out.flush();
        String logged2 = sw1.toString();
        ok(logged2.contains("WORLD_LOADED"), "LOAD_WORLD 3 参钩回调，log=" + logged2.trim());
        ok(MCAHooks.capturedWorld == world, "LOAD_WORLD 捕获同一 world（无换 world）");
        ok(mcC.getField("f").get(mc) == world, "changeWorld 本体 putfield f 对榫（theWorld=f）");
        // 委托壳路径：2 参→3 参恰中 1（证超集：2 参壳未被双钩）
        int wlBefore = countSub(logged2, "WORLD_LOADED");
        Method loadWorld2 = mcC.getMethod("a", c, String.class);
        loadWorld2.invoke(mc, world, "W181");
        MCAHooks.out.flush();
        logged2 = sw1.toString();
        ok(countSub(logged2, "WORLD_LOADED") == wlBefore + 1,
                "2 参委托→3 参恰中 1（无叠钩，超集自证）");
        // 清场路径 a(null,...)：null 守卫记日志、不清捕获
        loadWorld3.invoke(mc, null, "", null);
        MCAHooks.out.flush();
        logged2 = sw1.toString();
        ok(logged2.contains("WORLD_LOADED null"), "a(null,...) 清场走 null 守卫");
        ok(MCAHooks.capturedWorld == world, "null 清场不清捕获（把手①保持）");
        Method runTick = mcC.getMethod("k");
        runTick.invoke(mc);
        runTick.invoke(mc);
        ok(MCAHooks.tickCount() == t0 + 2, "RUN_TICK k()V 回调计数 +2");

        // ---- 5. 织入 abj：HUD_RENDER EXIT；≥a1.2.3 边界线=XYZ 注入免做（仅观测） ----
        byte[] rawAbj = readTmp("a2out181\\abj.class");
        byte[] wovenAbj = new Transformers().transform(null, "abj", null, null, rawAbj);
        ok(wovenAbj != null, "abj transformed");
        ok(wovenAbj.length > rawAbj.length, "abj bytes grew");
        l.put("abj", wovenAbj);
        Class<?> abjC = l.loadClass("abj");
        Object gui = abjC.getConstructor(mcC).newInstance(mc);
        Method hud = abjC.getMethod("a", float.class, boolean.class, int.class, int.class);
        hud.invoke(gui, Float.valueOf(0f), Boolean.FALSE, Integer.valueOf(0), Integer.valueOf(0));
        hud.invoke(gui, Float.valueOf(0f), Boolean.FALSE, Integer.valueOf(0), Integer.valueOf(0));
        MCAHooks.out.flush();
        String logged3 = sw1.toString();
        MCAHooks.out = null;
        ok(logged3.contains("HUD_RENDER #1") && logged3.contains("injection-exempt")
                && !logged3.contains("HUD_PAINT") && !logged3.contains("HUD_HOOK_DISABLED"),
                "注入免做：HUD_RENDER 观测日志在、零绘制零降级（链 h/q/z 不触），log=" + logged3.trim());
        ok(((Integer) abjC.getField("hud").get(gui)) == 2, "HUD 渲染本体两次跑过（hud=2）");

        System.out.println("InjectSmoke181Test PASS (" + checks + " checks)");
    }
}
