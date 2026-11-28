// [b1.6 转正批 2026-11-28] 仿 InjectSmoke1401Test（活体批前置无头验收）
package com.mcworldagent.agent;

import java.io.File;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

/**
 * b1.6 四项钩实形注入冒烟（TARGETS_16 全中，转正批 2026-11-28）：
 * - 实形 fb（a2out16/fb.class 真字节）：save-world **a(ZLxs;)V 双参实体**
 *   0x0001 ENTRY 命中——断代带 b1.4→b1.6 延续（无 1 参包装；xs null 守卫
 *   @14/@30 与 1401 逐位同形；ILOAD 1 直读布尔参，indirect 观察位=实参）；
 *   WORLD_CTOR 省略（锚库未直钉 fb 构造族形，dir 面走 cmd=，capturedSaveDir
 *   恒 null=降级面自证）；
 * - 实形 Minecraft（ASM 合成）：load-world a(Lfb;Ljava/lang/String;Lgq;)V
 *   3 参实体（判决 B 同 173/1501/1401：2 参壳 aconst_null 委托超集恰中 1 证；
 *   theWorld=**f**——⚠️1401/1501=e 断代，JSON 主类字段面钉）+ run-tick
 *   **k()V**（⚠️1401/1501=j 断代——j→k 翻面下界提前至 b1.6 实锤，
 *   173/181/pre2=k 同字母）；
 * - 实形 uk（a2out16/uk.class）：hud-render a(FZII)V 0x0001 EXIT 命中
 *   （1501 tl→1401 qv→16 uk 断代；≥a1.2.3 边界线=XYZ 注入免做，onHud 观测单点）。
 * parent-last Loader 隔离假件闭包（全量灌入 a2out16 裸类再覆写 woven；
 * loadClass 用点分名——1401 批陷阱实录）。
 * 跨组隔离：b1.6 激活期 1401/1501/173/181/pre2 字母（eb/qv/fm/tw/et/tl/gh/
 * fd/uq/rv/ti/abj/adm）零钩；1401/1501/173 激活期 fb/uk/xs/gq 零钩（配方
 * 不串代；⚠️ 撞车位件 hj/iz/np/gq/dj/fp/cl/sf 不参与钩面，无串扰源）。
 */
public class InjectSmoke16Test {

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

    /**
     * ASM 合成 16 主类真形：f:Lfb;（theWorld=f——⚠️1401/1501=e 断代，JSON
     * 主类字段面钉）+ a(Lfb;Ljava/lang/String;Lgq;)V 3 参实体（putfield f；
     * gq=EntityPlayer）+ a(Lfb;Ljava/lang/String;)V 2 参委托壳（ACONST_NULL→
     * INVOKEVIRTUAL 3 参，实形 mc_c@3 对榫）+ **k()V** runTick（⚠️ 非 1401/
     * 1501 的 j——j→k 翻面下界=b1.6，173 同字母）。
     * h/v/z/q/Z 系字段必须省略（同 173/1501/1401 教训：链接期实例尺寸计算
     * 解析全部非静态字段描述符类型，未预置入 map 的类落 parent——da=
     * EntityPlayerSP 假件树未建）；字段面由 A2Table16Test 钉表覆盖。
     * XYZ 注入免做（hudXyzPaint=false），链字母 h/q/z/B 仅锚数据。
     */
    static byte[] asmMinecraft16() {
        org.objectweb.asm.ClassWriter cw =
                new org.objectweb.asm.ClassWriter(org.objectweb.asm.ClassWriter.COMPUTE_MAXS);
        cw.visit(org.objectweb.asm.Opcodes.V1_8, org.objectweb.asm.Opcodes.ACC_PUBLIC,
                "net/minecraft/client/Minecraft", null, "java/lang/Object", null);
        cw.visitField(org.objectweb.asm.Opcodes.ACC_PUBLIC, "f", "Lfb;", null, null).visitEnd();
        org.objectweb.asm.MethodVisitor mv = cw.visitMethod(org.objectweb.asm.Opcodes.ACC_PUBLIC,
                "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(org.objectweb.asm.Opcodes.ALOAD, 0);
        mv.visitMethodInsn(org.objectweb.asm.Opcodes.INVOKESPECIAL,
                "java/lang/Object", "<init>", "()V", false);
        mv.visitInsn(org.objectweb.asm.Opcodes.RETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();
        // 3 参实体（钩目标）：putfield f=theWorld
        mv = cw.visitMethod(org.objectweb.asm.Opcodes.ACC_PUBLIC,
                "a", "(Lfb;Ljava/lang/String;Lgq;)V", null, null);
        mv.visitCode();
        mv.visitVarInsn(org.objectweb.asm.Opcodes.ALOAD, 0);
        mv.visitVarInsn(org.objectweb.asm.Opcodes.ALOAD, 1);
        mv.visitFieldInsn(org.objectweb.asm.Opcodes.PUTFIELD,
                "net/minecraft/client/Minecraft", "f", "Lfb;");
        mv.visitInsn(org.objectweb.asm.Opcodes.RETURN);
        mv.visitMaxs(2, 4);
        mv.visitEnd();
        // 2 参委托壳（不钩）：a(world, msg) → a(world, msg, null)
        mv = cw.visitMethod(org.objectweb.asm.Opcodes.ACC_PUBLIC,
                "a", "(Lfb;Ljava/lang/String;)V", null, null);
        mv.visitCode();
        mv.visitVarInsn(org.objectweb.asm.Opcodes.ALOAD, 0);
        mv.visitVarInsn(org.objectweb.asm.Opcodes.ALOAD, 1);
        mv.visitVarInsn(org.objectweb.asm.Opcodes.ALOAD, 2);
        mv.visitInsn(org.objectweb.asm.Opcodes.ACONST_NULL);
        mv.visitMethodInsn(org.objectweb.asm.Opcodes.INVOKEVIRTUAL,
                "net/minecraft/client/Minecraft", "a", "(Lfb;Ljava/lang/String;Lgq;)V", false);
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
        // ---- 1. TARGETS_16 结构：四钩全 0x0001 public ----
        VersionRegistry.activate("b1.6");
        HookTargets.activate("b1.6");
        HookTargets.Target[] w16 = HookTargets.forClass("fb");
        ok(w16.length == 1, "16 World 1 target（SAVE_WORLD；WORLD_CTOR 构造族未钉省略）got " + w16.length);
        HookTargets.Target sw = find(w16, HookTargets.SAVE_WORLD);
        ok(sw != null && "a".equals(sw.methodName) && "(ZLxs;)V".equals(sw.desc)
                && sw.access == 0x0001 && sw.expectedHits == 1 && sw.injectMode == HookTargets.ENTRY,
                "save-world a(ZLxs;)V 0x0001 ENTRY（双参实体=断代带 b1.4→b1.6 延续）");
        ok(find(w16, HookTargets.WORLD_CTOR) == null, "no world-ctor target");
        HookTargets.Target[] m16 = HookTargets.forClass("net/minecraft/client/Minecraft");
        ok(m16.length == 2, "16 Minecraft 2 targets");
        HookTargets.Target lw = find(m16, HookTargets.LOAD_WORLD);
        HookTargets.Target rt = find(m16, HookTargets.RUN_TICK);
        ok(lw != null && "a".equals(lw.methodName) && "(Lfb;Ljava/lang/String;Lgq;)V".equals(lw.desc)
                && lw.access == 0x0001 && lw.expectedHits == 1 && lw.injectMode == HookTargets.EXIT,
                "load-world a(Lfb;String;gq)V 0x0001 EXIT（判决 B：2 参壳委托超集）");
        ok(rt != null && "k".equals(rt.methodName) && "()V".equals(rt.desc)
                && rt.access == 0x0001 && rt.injectMode == HookTargets.ENTRY,
                "run-tick k()V 0x0001 ENTRY（⚠️1401/1501=j 断代——j→k 翻面下界=b1.6，本组必须 k）");
        HookTargets.Target[] hud16 = HookTargets.forClass("uk");
        ok(hud16.length == 1 && HookTargets.HUD_RENDER.equals(hud16[0].hookId)
                && "a".equals(hud16[0].methodName) && "(FZII)V".equals(hud16[0].desc)
                && hud16[0].access == 0x0001 && hud16[0].injectMode == HookTargets.EXIT,
                "16 HUD uk.a(FZII)V 0x0001 EXIT（1501 tl→1401 qv→16 uk 断代）");
        // 跨组隔离：b1.6 激活期——1401/1501/173/181/pre2 字母零钩
        ok(HookTargets.forClass("eb").length == 0 && HookTargets.forClass("qv").length == 0
                && HookTargets.forClass("fm").length == 0 && HookTargets.forClass("tw").length == 0
                && HookTargets.forClass("et").length == 0 && HookTargets.forClass("tl").length == 0
                && HookTargets.forClass("gh").length == 0
                && HookTargets.forClass("fd").length == 0 && HookTargets.forClass("uq").length == 0
                && HookTargets.forClass("rv").length == 0
                && HookTargets.forClass("ti").length == 0 && HookTargets.forClass("abj").length == 0
                && HookTargets.forClass("adm").length == 0,
                "16 组：1401/1501/173/181/pre2 字母零钩");
        HookTargets.activate("b1.4_01");
        ok(HookTargets.forClass("fb").length == 0 && HookTargets.forClass("uk").length == 0
                && HookTargets.forClass("xs").length == 0 && HookTargets.forClass("gq").length == 0,
                "1401 组：16 字母零钩");
        HookTargets.activate("b1.5_01");
        ok(HookTargets.forClass("fb").length == 0 && HookTargets.forClass("uk").length == 0,
                "1501 组：16 字母零钩");
        HookTargets.activate("b1.7.3");
        ok(HookTargets.forClass("fb").length == 0 && HookTargets.forClass("uk").length == 0
                && HookTargets.forClass("xs").length == 0 && HookTargets.forClass("gq").length == 0,
                "173 组：16 字母零钩（fb 非 173 fd/xs 非 yb 字母，配方不串代）");
        VersionRegistry.activate("b1.6");
        HookTargets.activate("b1.6");

        // ---- 2. 织入 fb：save-world ENTRY ILOAD 1 双参实体钩恰中 ----
        byte[] rawFb = readTmp("a2out16\\fb.class");
        MCAHooks.noFirstBrick = true;
        MCAHooks.capturedWorld = null;
        MCAHooks.capturedSaveDir = null;
        java.io.StringWriter swr1 = new java.io.StringWriter();
        MCAHooks.out = new java.io.PrintWriter(swr1);
        java.lang.reflect.Field scf = MCAHooks.class.getDeclaredField("SAVE_CALLS");
        scf.setAccessible(true);
        java.util.concurrent.atomic.AtomicInteger saveCalls =
                (java.util.concurrent.atomic.AtomicInteger) scf.get(null);
        int hooksBefore = saveCalls.get();
        byte[] wovenFb = new Transformers().transform(null, "fb", null, null, rawFb);
        ok(wovenFb != null, "fb transformed");
        ok(wovenFb.length > rawFb.length, "fb bytes grew");

        Loader l = new Loader(InjectSmoke16Test.class.getClassLoader());
        File[] cls = new File(System.getProperty("java.io.tmpdir"),
                "mcaut\\tests-out\\a2out16").listFiles();
        for (int i = 0; i < cls.length; i++) {
            String n = cls[i].getName();
            if (n.endsWith(".class")) {
                l.put(n.substring(0, n.length() - 6),
                        java.nio.file.Files.readAllBytes(cls[i].toPath()));
            }
        }
        l.put("fb", wovenFb);
        Class<?> c = l.loadClass("fb");
        Object world = c.getConstructor().newInstance();
        ok(MCAHooks.capturedSaveDir == null, "无 world-ctor 钩：dir 不捕获（构造族未钉降级面自证）");
        Class<?> xsC = l.loadClass("xs");
        Method save = c.getMethod("a", boolean.class, xsC);
        ok(saveCalls.get() == hooksBefore, "no calls yet");
        Object ret = save.invoke(world, Boolean.FALSE, (Object) null);
        MCAHooks.out.flush();
        String logged = swr1.toString();
        ok(MCAHooks.capturedWorld == world, "onSaveWorld captured world");
        ok(logged.contains("SAVE_WORLD") && logged.contains("indirect=false"),
                "ILOAD 1 直读布尔参=false（双参实体形态），log=" + logged.trim());
        ok(saveCalls.get() == hooksBefore + 1, "SAVE_CALLS++（单钩恰中 1）");
        ok(((Integer) c.getField("saveCalls").get(world)) == 1, "实体本体跑过 saveCalls=1");
        ok(ret == null, "(ZLxs;)V void 形（入口注入不动返回值）");
        Object prog = Proxy.newProxyInstance(l, new Class[] { xsC }, new InvocationHandler() {
            public Object invoke(Object p, Method m, Object[] args) {
                return null;
            }
        });
        save.invoke(world, Boolean.TRUE, prog);
        MCAHooks.out.flush();
        ok(((Integer) c.getField("saveCalls").get(world)) == 2, "非空 xs：saveCalls=2");
        ok(saveCalls.get() == hooksBefore + 2, "双参实体钩每次调用恒触发 SAVE_CALLS+2");
        ok(swr1.toString().contains("indirect=true"), "indirect=true 观察位对榫");

        // ---- 3. 织入 Minecraft（ASM 实形）：LOAD_WORLD + RUN_TICK(k 形) 双中 ----
        byte[] rawMc = asmMinecraft16();
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
        ok(loadWorld3 != null, "3 参 changeWorld a(Lfb;String;gq)V 实形在（钩目标）");
        loadWorld3.invoke(mc, world, "W16", null);
        MCAHooks.out.flush();
        String logged2 = swr1.toString();
        ok(logged2.contains("WORLD_LOADED"), "LOAD_WORLD 3 参钩回调，log=" + logged2.trim());
        ok(MCAHooks.capturedWorld == world, "LOAD_WORLD 捕获同一 world（无换 world）");
        ok(mcC.getField("f").get(mc) == world, "changeWorld 本体 putfield f 对榫（theWorld=f——1401/1501=e 断代）");
        int wlBefore = countSub(logged2, "WORLD_LOADED");
        Method loadWorld2 = mcC.getMethod("a", c, String.class);
        loadWorld2.invoke(mc, world, "W16");
        MCAHooks.out.flush();
        logged2 = swr1.toString();
        ok(countSub(logged2, "WORLD_LOADED") == wlBefore + 1,
                "2 参委托→3 参恰中 1（无叠钩，超集自证）");
        loadWorld3.invoke(mc, null, "", null);
        MCAHooks.out.flush();
        logged2 = swr1.toString();
        ok(logged2.contains("WORLD_LOADED null"), "a(null,...) 清场走 null 守卫");
        ok(MCAHooks.capturedWorld == world, "null 清场不清捕获（把手①保持）");
        Method runTick = mcC.getMethod("k");
        runTick.invoke(mc);
        runTick.invoke(mc);
        ok(MCAHooks.tickCount() == t0 + 2, "RUN_TICK k()V 回调计数 +2（本组 k——j→k 翻面下界=b1.6）");

        // ---- 4. 织入 uk：HUD_RENDER EXIT；≥a1.2.3 边界线=XYZ 注入免做（仅观测） ----
        byte[] rawUk = readTmp("a2out16\\uk.class");
        byte[] wovenUk = new Transformers().transform(null, "uk", null, null, rawUk);
        ok(wovenUk != null, "uk transformed");
        ok(wovenUk.length > rawUk.length, "uk bytes grew");
        l.put("uk", wovenUk);
        Class<?> ukC = l.loadClass("uk");
        Object gui = ukC.getConstructor(mcC).newInstance(mc);
        Method hud = ukC.getMethod("a", float.class, boolean.class, int.class, int.class);
        hud.invoke(gui, Float.valueOf(0f), Boolean.FALSE, Integer.valueOf(0), Integer.valueOf(0));
        hud.invoke(gui, Float.valueOf(0f), Boolean.FALSE, Integer.valueOf(0), Integer.valueOf(0));
        MCAHooks.out.flush();
        String logged3 = swr1.toString();
        MCAHooks.out = null;
        ok(logged3.contains("HUD_RENDER #1") && logged3.contains("injection-exempt")
                && !logged3.contains("HUD_PAINT") && !logged3.contains("HUD_HOOK_DISABLED"),
                "注入免做：HUD_RENDER 观测日志在、零绘制零降级（链 h/q/z/B 不触），log=" + logged3.trim());
        ok(((Integer) ukC.getField("hud").get(gui)) == 2, "HUD 渲染本体两次跑过（hud=2）");

        System.out.println("InjectSmoke16Test PASS (" + checks + " checks)");
    }
}
