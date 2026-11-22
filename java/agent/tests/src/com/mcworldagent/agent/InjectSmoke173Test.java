// [B5-ARCHIVE 2026-11-22] headless test archived from %TEMP%\mcaut (source batch: B b1.7.3); run via java/agent/tests/run.ps1
package com.mcworldagent.agent;

import java.io.File;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

/**
 * b1.7.3 四项钩实形注入冒烟（TARGETS_173 全中）：
 * - 实形 fd（a2out173/fd.class 真字节）：save-world a(Lyb;)V 0x0001 ENTRY 命中；
 *   WORLD_CTOR 本版省略（fd(File,String) 构造器断代）→ capturedSaveDir 恒 null=降级面自证；
 * - 实形 Minecraft（ASM 合成）：load-world a(Lfd;String)V + run-tick k()V 双中
 *   （⚠️RUN_TICK=i→k 字母断代，1202 的 i()V 形不复现）；
 * - 实形 uq（a2out173/uq.class）：hud-render a(FZII)V 0x0001 EXIT 命中；
 *   onHud 链本版未钉（thePlayer=h 非 f→链在 fontRenderer "m" 位必断）
 *   → HUD_HOOK_DISABLED 优雅降级=钩已触发证据（只验注入不验 HUD 文本）。
 * parent-last Loader 隔离假件闭包（fd 返回体 ei/xv/qw/ow/yb/sn 须同 loader 解析）。
 */
public class InjectSmoke173Test {

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

    /** ASM 合成 b1.7.3 Minecraft 真形：f:Lfd;（theWorld）+ a(Lfd;String)V putfield f + k()V。
     *  h/v/z 系字段必须省略（同 1202 g/u 教训：链接期实例尺寸计算解析全部非静态字段
     *  描述符类型，gs/uq/kv 未预置入 map 会落 parent→woven 版永不可达）；
     *  2026-11-18 判决：173 ≥a1.2.3 边界线=XYZ 注入免做（hudXyzPaint=false），
     *  onHud 不触绘制链，链字母 h/q/z/B 仅锚数据（A2Table173Test 钉表覆盖）。 */
    static byte[] asmMinecraft173() {
        org.objectweb.asm.ClassWriter cw =
                new org.objectweb.asm.ClassWriter(org.objectweb.asm.ClassWriter.COMPUTE_MAXS);
        cw.visit(org.objectweb.asm.Opcodes.V1_8, org.objectweb.asm.Opcodes.ACC_PUBLIC,
                "net/minecraft/client/Minecraft", null, "java/lang/Object", null);
        cw.visitField(org.objectweb.asm.Opcodes.ACC_PUBLIC, "f", "Lfd;", null, null).visitEnd();
        org.objectweb.asm.MethodVisitor mv = cw.visitMethod(org.objectweb.asm.Opcodes.ACC_PUBLIC,
                "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(org.objectweb.asm.Opcodes.ALOAD, 0);
        mv.visitMethodInsn(org.objectweb.asm.Opcodes.INVOKESPECIAL,
                "java/lang/Object", "<init>", "()V", false);
        mv.visitInsn(org.objectweb.asm.Opcodes.RETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();
        // 3 参实体（钩目标，判决 B 移植批 2026-11-19 同 b1.8.1）：putfield f=theWorld
        mv = cw.visitMethod(org.objectweb.asm.Opcodes.ACC_PUBLIC,
                "a", "(Lfd;Ljava/lang/String;Lgs;)V", null, null);
        mv.visitCode();
        mv.visitVarInsn(org.objectweb.asm.Opcodes.ALOAD, 0);
        mv.visitVarInsn(org.objectweb.asm.Opcodes.ALOAD, 1);
        mv.visitFieldInsn(org.objectweb.asm.Opcodes.PUTFIELD,
                "net/minecraft/client/Minecraft", "f", "Lfd;");
        mv.visitInsn(org.objectweb.asm.Opcodes.RETURN);
        mv.visitMaxs(2, 4);
        mv.visitEnd();
        // 2 参委托壳（不钩）：a(world, msg) → a(world, msg, null)，实形对榫
        mv = cw.visitMethod(org.objectweb.asm.Opcodes.ACC_PUBLIC,
                "a", "(Lfd;Ljava/lang/String;)V", null, null);
        mv.visitCode();
        mv.visitVarInsn(org.objectweb.asm.Opcodes.ALOAD, 0);
        mv.visitVarInsn(org.objectweb.asm.Opcodes.ALOAD, 1);
        mv.visitVarInsn(org.objectweb.asm.Opcodes.ALOAD, 2);
        mv.visitInsn(org.objectweb.asm.Opcodes.ACONST_NULL);
        mv.visitMethodInsn(org.objectweb.asm.Opcodes.INVOKEVIRTUAL,
                "net/minecraft/client/Minecraft", "a", "(Lfd;Ljava/lang/String;Lgs;)V", false);
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
        // ---- 1. TARGETS_173 结构：四钩全 0x0001 public ----
        VersionRegistry.activate("b1.7.3");
        HookTargets.activate("b1.7.3");
        HookTargets.Target[] w73 = HookTargets.forClass("fd");
        ok(w73.length == 1, "173 World 1 target（SAVE_WORLD；WORLD_CTOR 构造断代省略）got " + w73.length);
        HookTargets.Target sw = find(w73, HookTargets.SAVE_WORLD);
        ok(sw != null && "a".equals(sw.methodName) && "(Lyb;)V".equals(sw.desc)
                && sw.access == 0x0001 && sw.expectedHits == 1 && sw.injectMode == HookTargets.ENTRY,
                "save-world a(Lyb;)V 0x0001 ENTRY");
        ok(find(w73, HookTargets.WORLD_CTOR) == null, "no world-ctor target");
        HookTargets.Target[] m73 = HookTargets.forClass("net/minecraft/client/Minecraft");
        ok(m73.length == 2, "173 Minecraft 2 targets");
        HookTargets.Target lw = find(m73, HookTargets.LOAD_WORLD);
        HookTargets.Target rt = find(m73, HookTargets.RUN_TICK);
        ok(lw != null && "a".equals(lw.methodName) && "(Lfd;Ljava/lang/String;Lgs;)V".equals(lw.desc)
                && lw.access == 0x0001 && lw.expectedHits == 1 && lw.injectMode == HookTargets.EXIT,
                "load-world a(Lfd;String;gs)V 0x0001 EXIT（判决 B 移植批：2 参委托壳→3 参实体改挂）");
        ok(rt != null && "k".equals(rt.methodName) && "()V".equals(rt.desc)
                && rt.access == 0x0001 && rt.injectMode == HookTargets.ENTRY,
                "run-tick k()V 0x0001 ENTRY（⚠️非 1202 i）");
        HookTargets.Target[] hud73 = HookTargets.forClass("uq");
        ok(hud73.length == 1 && HookTargets.HUD_RENDER.equals(hud73[0].hookId)
                && "a".equals(hud73[0].methodName) && "(FZII)V".equals(hud73[0].desc)
                && hud73[0].access == 0x0001 && hud73[0].injectMode == HookTargets.EXIT,
                "173 HUD uq.a(FZII)V 0x0001 EXIT");
        // 跨组隔离：173 激活期旧组类名零钩；反向（其余版本激活）173 类名零钩
        ok(HookTargets.forClass("dn").length == 0 && HookTargets.forClass("pe").length == 0
                && HookTargets.forClass("ru").length == 0 && HookTargets.forClass("cu").length == 0
                && HookTargets.forClass("mz").length == 0 && HookTargets.forClass("ph").length == 0
                && HookTargets.forClass("aof").length == 0, "旧组类名在 173 组零钩");
        HookTargets.activate("b1.2_02");
        ok(HookTargets.forClass("fd").length == 0 && HookTargets.forClass("uq").length == 0
                && HookTargets.forClass("ei").length == 0, "173 类名在 1202 组零钩");
        HookTargets.activate("a1.2.0");
        ok(HookTargets.forClass("fd").length == 0 && HookTargets.forClass("uq").length == 0,
                "173 类名在 120 组零钩");
        HookTargets.activate("inf-20100629");
        ok(HookTargets.forClass("fd").length == 0 && HookTargets.forClass("uq").length == 0,
                "173 类名在 0629 组零钩");
        HookTargets.activate("b1.7.3");

        // ---- 2. 织入 fd：save-world ENTRY(ICONST_0) 单钩恰中（无 world-ctor）----
        byte[] rawFd = readTmp("a2out173\\fd.class");
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
        byte[] wovenFd = new Transformers().transform(null, "fd", null, null, rawFd);
        ok(wovenFd != null, "fd transformed");
        ok(wovenFd.length > rawFd.length, "fd bytes grew");

        Loader l = new Loader(InjectSmoke173Test.class.getClassLoader());
        // fd 闭包件同 loader 定义（返回体/参数体类型须与 woven fd 同一 ClassLoader）
        l.put("sn", readTmp("a2out173\\sn.class"));
        l.put("ei", readTmp("a2out173\\ei.class"));
        l.put("yb", readTmp("a2out173\\yb.class"));
        l.put("ow", readTmp("a2out173\\ow.class"));
        l.put("qx", readTmp("a2out173\\qx.class"));
        l.put("xv", readTmp("a2out173\\xv.class"));
        l.put("kd", readTmp("a2out173\\kd.class"));
        l.put("fd", wovenFd);
        Class<?> c = l.loadClass("fd");
        Object world = c.getConstructor().newInstance();
        ok(MCAHooks.capturedSaveDir == null, "无 world-ctor 钩：dir 不捕获（档位二降级面自证）");
        Class<?> ybC = l.loadClass("yb");
        Method save = c.getMethod("a", ybC);
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
        ok(ret == null, "(Lyb;)V void 形（入口注入不动返回值）");

        // ---- 3. fd.a(ZLyb)V 本体 null 守卫语义（yb 方法面未钉=零回调，不 NPE 即对）----
        Method body = c.getMethod("a", boolean.class, ybC);
        body.invoke(world, Boolean.FALSE, null);
        ok(((Integer) c.getField("saveCalls").get(world)) == 2, "null 守卫：直调本体不 NPE，saveCalls=2");
        ok(((Integer) c.getField("levelSaves").get(world)) == 2, "null 守卫：r()V 仍执行 levelSaves=2");
        Object prog = Proxy.newProxyInstance(l, new Class[] { ybC }, new InvocationHandler() {
            public Object invoke(Object p, Method m, Object[] args) {
                return null;
            }
        });
        body.invoke(world, Boolean.TRUE, prog);
        ok(((Integer) c.getField("saveCalls").get(world)) == 3, "非空 yb（空接口零回调）：saveCalls=3");

        // ---- 4. 织入 Minecraft（ASM 实形）：LOAD_WORLD + RUN_TICK(k 形) 双中 ----
        byte[] rawMc = asmMinecraft173();
        byte[] wovenMc = new Transformers().transform(null, "net/minecraft/client/Minecraft", null, null, rawMc);
        ok(wovenMc != null, "Minecraft transformed");
        ok(wovenMc.length > rawMc.length, "Minecraft bytes grew");
        l.put("net.minecraft.client.Minecraft", wovenMc);
        Class<?> mcC = l.loadClass("net.minecraft.client.Minecraft");
        Object mc = mcC.getConstructor().newInstance();
        long t0 = MCAHooks.tickCount();
        // 3 参实体=钩目标（EXIT）：直调（传送门 m() 即此径，绕过委托壳）恰中
        Method loadWorld3 = null;
        Method[] ms = mcC.getMethods();
        for (int i = 0; i < ms.length; i++) {
            if ("a".equals(ms[i].getName()) && ms[i].getParameterTypes().length == 3) {
                loadWorld3 = ms[i];
            }
        }
        ok(loadWorld3 != null, "3 参 changeWorld a(Lfd;String;gs)V 实形存在（钩目标）");
        loadWorld3.invoke(mc, world, "world", null);
        MCAHooks.out.flush();
        String logged2 = sw1.toString();
        ok(logged2.contains("WORLD_LOADED"), "LOAD_WORLD 3 参 EXIT 回调，log=" + logged2.trim());
        ok(MCAHooks.capturedWorld == world, "LOAD_WORLD 捕获同一 world（无换 world）");
        ok(mcC.getField("f").get(mc) == world, "changeWorld 本体 putfield f 对榫（theWorld=f）");
        // 2 参委托壳：aconst_null→3 参，恰中 1（无叠钩——委托壳不钩自证）
        int wlBefore = countSub(logged2, "WORLD_LOADED");
        Method loadWorld2 = mcC.getMethod("a", c, String.class);
        loadWorld2.invoke(mc, world, "world");
        MCAHooks.out.flush();
        logged2 = sw1.toString();
        ok(countSub(logged2, "WORLD_LOADED") == wlBefore + 1,
                "2 参委托→3 参恰中 1（无叠钩），WORLD_LOADED 计数=" + countSub(logged2, "WORLD_LOADED"));
        // null 清场：a(null,...) 走 null 守卫不 NPE、捕获保留（句柄①）
        loadWorld3.invoke(mc, null, "", null);
        MCAHooks.out.flush();
        logged2 = sw1.toString();
        ok(logged2.contains("WORLD_LOADED null"), "a(null,...) 清场走 null 守卫不 NPE");
        ok(MCAHooks.capturedWorld == world, "null 清场不清捕获（句柄①保留）");
        Method runTick = mcC.getMethod("k");
        runTick.invoke(mc);
        runTick.invoke(mc);
        ok(MCAHooks.tickCount() == t0 + 2, "RUN_TICK k()V 回调计数 +2");

        // ---- 5. 织入 uq：HUD_RENDER EXIT；≥a1.2.3 边界线=XYZ 注入免做（仅观测） ----
        byte[] rawUq = readTmp("a2out173\\uq.class");
        byte[] wovenUq = new Transformers().transform(null, "uq", null, null, rawUq);
        ok(wovenUq != null, "uq transformed");
        ok(wovenUq.length > rawUq.length, "uq bytes grew");
        l.put("uq", wovenUq);
        Class<?> uqC = l.loadClass("uq");
        Object gui = uqC.getConstructor(mcC).newInstance(mc);
        Method hud = uqC.getMethod("a", float.class, boolean.class, int.class, int.class);
        hud.invoke(gui, Float.valueOf(0f), Boolean.FALSE, Integer.valueOf(0), Integer.valueOf(0));
        hud.invoke(gui, Float.valueOf(0f), Boolean.FALSE, Integer.valueOf(0), Integer.valueOf(0));
        MCAHooks.out.flush();
        String logged3 = sw1.toString();
        MCAHooks.out = null;
        ok(logged3.contains("HUD_RENDER #1") && logged3.contains("injection-exempt")
                && !logged3.contains("HUD_PAINT") && !logged3.contains("HUD_HOOK_DISABLED"),
                "注入免做：HUD_RENDER 观测日志在、零绘制零降级（链 h/q/z 不触），log=" + logged3.trim());
        ok(((Integer) uqC.getField("hud").get(gui)) == 2, "HUD 渲染本体两次跑过（hud=2）");

        System.out.println("InjectSmoke173Test PASS (" + checks + " checks)");
    }
}
