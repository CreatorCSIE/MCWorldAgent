// [b1.5_01 转正批 2026-11-28] 仿 InjectSmoke19pre2Test（活体批前置无头验收）
package com.mcworldagent.agent;

import java.io.File;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

/**
 * b1.5_01 四项钩实形注入冒烟（TARGETS_1501 全中，活体批转正 2026-11-28）：
 * - 实形 et（a2out1501/et.class 真字节）：save-world **a(ZLwr;)V 双参实体**
 *   0x0001 ENTRY 命中——⚠️ 本版无 1 参包装（173/181/pre2 包装配方不适用），
 *   注入位 ILOAD 1 直读布尔参（Transformers (Z 分支），indirect 观察位=实参；
 *   WORLD_CTOR 本版省略（et 构造族=(wt,String,J) 系，dir 面走 cmd=，
 *   capturedSaveDir 恒 null=降级面自证）；
 * - 实形 Minecraft（ASM 合成）：load-world a(Let;String;Lgh;)V 3 参实体
 *   （判决 B 同 173：2 参壳 aconst_null 委托=超集恰中 1 证）+ run-tick
 *   **j()V**（⚠️173/181/pre2=k 断代——j→k 翻面下界=b1.6，本组必须 j）；
 * - 实形 tl（a2out1501/tl.class）：hud-render a(FZII)V 0x0001 EXIT 命中；
 *   ≥a1.2.3 边界线=XYZ 注入免做（hudXyzPaint=false），onHud 观测面单点计数。
 * parent-last Loader 隔离假件闭包（et 返回体/参数体 wr/dy/rj/gh 等须同 loader
 * ——本批全量灌入 a2out1501 裸类再覆写 woven，防漏件落 parent）。
 * 跨组隔离：1501 激活期 173/181/pre2 字母（fd/rv/ti/uq/abj/adm）零钩；
 * 173/181/pre2 激活期 et/tl/gh 零钩（配方不串代）。
 */
public class InjectSmoke1501Test {

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
     * ASM 合成 1501 主类真形：e:L et;（theWorld ✓1401/1501=e，16=f 断代）+
     * a(Let;Ljava/lang/String;Lgh;)V 3 参实体（putfield e；gh=EntityPlayer 断代
     * ——173=Lgs）+ a(Let;Ljava/lang/String;)V 2 参委托壳（ACONST_NULL→
     * INVOKEVIRTUAL 3 参，实形 mc_c@3398 对榫）+ j()V runTick（⚠️ 非 173 的 k）。
     * h/u/y/p/W 系字段必须省略（同 173/181 教训：链接期实例尺寸计算解析全部
     * 非静态字段描述符类型，未预置入 map 的类落 parent→woven 版永不可达）；
     * 字段面由 A2Table1501Test 钉表覆盖。LOAD_WORLD 钩目标=3 参实体（判决 B），
     * 2 参壳不钩；XYZ 注入免做（hudXyzPaint=false），链字母 h/p/y/B 仅锚数据。
     */
    static byte[] asmMinecraft1501() {
        org.objectweb.asm.ClassWriter cw =
                new org.objectweb.asm.ClassWriter(org.objectweb.asm.ClassWriter.COMPUTE_MAXS);
        cw.visit(org.objectweb.asm.Opcodes.V1_8, org.objectweb.asm.Opcodes.ACC_PUBLIC,
                "net/minecraft/client/Minecraft", null, "java/lang/Object", null);
        cw.visitField(org.objectweb.asm.Opcodes.ACC_PUBLIC, "e", "Let;", null, null).visitEnd();
        org.objectweb.asm.MethodVisitor mv = cw.visitMethod(org.objectweb.asm.Opcodes.ACC_PUBLIC,
                "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(org.objectweb.asm.Opcodes.ALOAD, 0);
        mv.visitMethodInsn(org.objectweb.asm.Opcodes.INVOKESPECIAL,
                "java/lang/Object", "<init>", "()V", false);
        mv.visitInsn(org.objectweb.asm.Opcodes.RETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();
        // 3 参实体（钩目标）：putfield e=theWorld
        mv = cw.visitMethod(org.objectweb.asm.Opcodes.ACC_PUBLIC,
                "a", "(Let;Ljava/lang/String;Lgh;)V", null, null);
        mv.visitCode();
        mv.visitVarInsn(org.objectweb.asm.Opcodes.ALOAD, 0);
        mv.visitVarInsn(org.objectweb.asm.Opcodes.ALOAD, 1);
        mv.visitFieldInsn(org.objectweb.asm.Opcodes.PUTFIELD,
                "net/minecraft/client/Minecraft", "e", "Let;");
        mv.visitInsn(org.objectweb.asm.Opcodes.RETURN);
        mv.visitMaxs(2, 4);
        mv.visitEnd();
        // 2 参委托壳（不钩）：a(world, msg) → a(world, msg, null)
        mv = cw.visitMethod(org.objectweb.asm.Opcodes.ACC_PUBLIC,
                "a", "(Let;Ljava/lang/String;)V", null, null);
        mv.visitCode();
        mv.visitVarInsn(org.objectweb.asm.Opcodes.ALOAD, 0);
        mv.visitVarInsn(org.objectweb.asm.Opcodes.ALOAD, 1);
        mv.visitVarInsn(org.objectweb.asm.Opcodes.ALOAD, 2);
        mv.visitInsn(org.objectweb.asm.Opcodes.ACONST_NULL);
        mv.visitMethodInsn(org.objectweb.asm.Opcodes.INVOKEVIRTUAL,
                "net/minecraft/client/Minecraft", "a", "(Let;Ljava/lang/String;Lgh;)V", false);
        mv.visitInsn(org.objectweb.asm.Opcodes.RETURN);
        mv.visitMaxs(4, 3);
        mv.visitEnd();
        mv = cw.visitMethod(org.objectweb.asm.Opcodes.ACC_PUBLIC, "j", "()V", null, null);
        mv.visitCode();
        mv.visitInsn(org.objectweb.asm.Opcodes.RETURN);
        mv.visitMaxs(0, 1);
        mv.visitEnd();
        cw.visitEnd();
        return cw.toByteArray();
    }

    public static void main(String[] a) throws Exception {
        // ---- 1. TARGETS_1501 结构：四钩全 0x0001 public ----
        VersionRegistry.activate("b1.5_01");
        HookTargets.activate("b1.5_01");
        HookTargets.Target[] w01 = HookTargets.forClass("et");
        ok(w01.length == 1, "1501 World 1 target（SAVE_WORLD；WORLD_CTOR (wt,String,J) 构造断代省略）got " + w01.length);
        HookTargets.Target sw = find(w01, HookTargets.SAVE_WORLD);
        ok(sw != null && "a".equals(sw.methodName) && "(ZLwr;)V".equals(sw.desc)
                && sw.access == 0x0001 && sw.expectedHits == 1 && sw.injectMode == HookTargets.ENTRY,
                "save-world a(ZLwr;)V 0x0001 ENTRY（双参实体=断代，无包装）");
        ok(find(w01, HookTargets.WORLD_CTOR) == null, "no world-ctor target");
        HookTargets.Target[] m01 = HookTargets.forClass("net/minecraft/client/Minecraft");
        ok(m01.length == 2, "1501 Minecraft 2 targets");
        HookTargets.Target lw = find(m01, HookTargets.LOAD_WORLD);
        HookTargets.Target rt = find(m01, HookTargets.RUN_TICK);
        ok(lw != null && "a".equals(lw.methodName) && "(Let;Ljava/lang/String;Lgh;)V".equals(lw.desc)
                && lw.access == 0x0001 && lw.expectedHits == 1 && lw.injectMode == HookTargets.EXIT,
                "load-world a(Let;String;gh)V 0x0001 EXIT（判决 B：2 参壳委托超集）");
        ok(rt != null && "j".equals(rt.methodName) && "()V".equals(rt.desc)
                && rt.access == 0x0001 && rt.injectMode == HookTargets.ENTRY,
                "run-tick j()V 0x0001 ENTRY（⚠️173/181/pre2=k 断代——本组必须 j）");
        HookTargets.Target[] hud01 = HookTargets.forClass("tl");
        ok(hud01.length == 1 && HookTargets.HUD_RENDER.equals(hud01[0].hookId)
                && "a".equals(hud01[0].methodName) && "(FZII)V".equals(hud01[0].desc)
                && hud01[0].access == 0x0001 && hud01[0].injectMode == HookTargets.EXIT,
                "1501 HUD tl.a(FZII)V 0x0001 EXIT（uq→tl 断代）");
        // 跨组隔离：1501 激活期——173/181/pre2 字母零钩
        ok(HookTargets.forClass("fd").length == 0 && HookTargets.forClass("rv").length == 0
                && HookTargets.forClass("ti").length == 0 && HookTargets.forClass("uq").length == 0
                && HookTargets.forClass("abj").length == 0 && HookTargets.forClass("adm").length == 0
                && HookTargets.forClass("gs").length == 0 && HookTargets.forClass("sz").length == 0
                && HookTargets.forClass("yb").length == 0 && HookTargets.forClass("ny").length == 0
                && HookTargets.forClass("pd").length == 0 && HookTargets.forClass("ls").length == 0,
                "1501 组：173/181/pre2 字母零钩");
        HookTargets.activate("b1.7.3");
        ok(HookTargets.forClass("et").length == 0 && HookTargets.forClass("tl").length == 0
                && HookTargets.forClass("gh").length == 0 && HookTargets.forClass("wr").length == 0,
                "173 组：1501 字母零钩");
        HookTargets.activate("b1.8.1");
        ok(HookTargets.forClass("et").length == 0 && HookTargets.forClass("tl").length == 0,
                "181 组：1501 字母零钩");
        HookTargets.activate("b1.9-pre2");
        ok(HookTargets.forClass("et").length == 0 && HookTargets.forClass("tl").length == 0,
                "pre2 组：1501 字母零钩（ti/adm 配方不串代）");
        VersionRegistry.activate("b1.5_01");
        HookTargets.activate("b1.5_01");

        // ---- 2. 织入 et：save-world ENTRY ILOAD 1 双参实体钩恰中 ----
        byte[] rawEt = readTmp("a2out1501\\et.class");
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
        byte[] wovenEt = new Transformers().transform(null, "et", null, null, rawEt);
        ok(wovenEt != null, "et transformed");
        ok(wovenEt.length > rawEt.length, "et bytes grew");

        Loader l = new Loader(InjectSmoke1501Test.class.getClassLoader());
        // 全量灌入 a2out1501 裸类（闭包件同 loader 定义，防漏件落 parent）
        File[] cls = new File(System.getProperty("java.io.tmpdir"),
                "mcaut\\tests-out\\a2out1501").listFiles();
        for (int i = 0; i < cls.length; i++) {
            String n = cls[i].getName();
            if (n.endsWith(".class")) {
                l.put(n.substring(0, n.length() - 6),
                        java.nio.file.Files.readAllBytes(cls[i].toPath()));
            }
        }
        l.put("et", wovenEt);
        Class<?> c = l.loadClass("et");
        Object world = c.getConstructor().newInstance();
        ok(MCAHooks.capturedSaveDir == null, "无 world-ctor 钩：dir 不捕获（(wt,String,J) 构造断代降级面自证）");
        Class<?> wrC = l.loadClass("wr");
        Method save = c.getMethod("a", boolean.class, wrC);
        ok(saveCalls.get() == hooksBefore, "no calls yet");
        Object ret = save.invoke(world, Boolean.FALSE, (Object) null);
        MCAHooks.out.flush();
        String logged = swr1.toString();
        ok(MCAHooks.capturedWorld == world, "onSaveWorld captured world");
        ok(logged.contains("SAVE_WORLD") && logged.contains("indirect=false"),
                "ILOAD 1 直读布尔参=false（双参实体形态），log=" + logged.trim());
        ok(saveCalls.get() == hooksBefore + 1, "SAVE_CALLS++（单钩恰中 1）");
        ok(((Integer) c.getField("saveCalls").get(world)) == 1, "实体本体跑过 saveCalls=1");
        ok(ret == null, "(ZLwr;)V void 形（入口注入不动返回值）");
        // 非空 wr（接口 Proxy 零回调）+ true：再恰中 1（实体钩无包装/本体之分——
        // 每次调用都触发钩=173 包装形态的镜像差异，断代自证）
        Object prog = Proxy.newProxyInstance(l, new Class[] { wrC }, new InvocationHandler() {
            public Object invoke(Object p, Method m, Object[] args) {
                return null;
            }
        });
        save.invoke(world, Boolean.TRUE, prog);
        MCAHooks.out.flush();
        ok(((Integer) c.getField("saveCalls").get(world)) == 2, "非空 wr：saveCalls=2");
        ok(saveCalls.get() == hooksBefore + 2, "双参实体钩每次调用恒触发 SAVE_CALLS+2");
        ok(swr1.toString().contains("indirect=true"), "indirect=true 观察位对榫");

        // ---- 3. 织入 Minecraft（ASM 实形）：LOAD_WORLD + RUN_TICK(j 形) 双中 ----
        byte[] rawMc = asmMinecraft1501();
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
        ok(loadWorld3 != null, "3 参 changeWorld a(Let;String;gh)V 实形在（钩目标）");
        loadWorld3.invoke(mc, world, "W1501", null);
        MCAHooks.out.flush();
        String logged2 = swr1.toString();
        ok(logged2.contains("WORLD_LOADED"), "LOAD_WORLD 3 参钩回调，log=" + logged2.trim());
        ok(MCAHooks.capturedWorld == world, "LOAD_WORLD 捕获同一 world（无换 world）");
        ok(mcC.getField("e").get(mc) == world, "changeWorld 本体 putfield e 对榫（theWorld=e）");
        // 委托壳路径：2 参→3 参恰中 1（证超集：2 参壳未被双钩）
        int wlBefore = countSub(logged2, "WORLD_LOADED");
        Method loadWorld2 = mcC.getMethod("a", c, String.class);
        loadWorld2.invoke(mc, world, "W1501");
        MCAHooks.out.flush();
        logged2 = swr1.toString();
        ok(countSub(logged2, "WORLD_LOADED") == wlBefore + 1,
                "2 参委托→3 参恰中 1（无叠钩，超集自证）");
        // 清场路径 a(null,...)：null 守卫记日志、不清捕获
        loadWorld3.invoke(mc, null, "", null);
        MCAHooks.out.flush();
        logged2 = swr1.toString();
        ok(logged2.contains("WORLD_LOADED null"), "a(null,...) 清场走 null 守卫");
        ok(MCAHooks.capturedWorld == world, "null 清场不清捕获（把手①保持）");
        Method runTick = mcC.getMethod("j");
        runTick.invoke(mc);
        runTick.invoke(mc);
        ok(MCAHooks.tickCount() == t0 + 2, "RUN_TICK j()V 回调计数 +2（本组 j 非 173 k）");

        // ---- 4. 织入 tl：HUD_RENDER EXIT；≥a1.2.3 边界线=XYZ 注入免做（仅观测） ----
        byte[] rawTl = readTmp("a2out1501\\tl.class");
        byte[] wovenTl = new Transformers().transform(null, "tl", null, null, rawTl);
        ok(wovenTl != null, "tl transformed");
        ok(wovenTl.length > rawTl.length, "tl bytes grew");
        l.put("tl", wovenTl);
        Class<?> tlC = l.loadClass("tl");
        Object gui = tlC.getConstructor(mcC).newInstance(mc);
        Method hud = tlC.getMethod("a", float.class, boolean.class, int.class, int.class);
        hud.invoke(gui, Float.valueOf(0f), Boolean.FALSE, Integer.valueOf(0), Integer.valueOf(0));
        hud.invoke(gui, Float.valueOf(0f), Boolean.FALSE, Integer.valueOf(0), Integer.valueOf(0));
        MCAHooks.out.flush();
        String logged3 = swr1.toString();
        MCAHooks.out = null;
        ok(logged3.contains("HUD_RENDER #1") && logged3.contains("injection-exempt")
                && !logged3.contains("HUD_PAINT") && !logged3.contains("HUD_HOOK_DISABLED"),
                "注入免做：HUD_RENDER 观测日志在、零绘制零降级（链 h/p/y/B 不触），log=" + logged3.trim());
        ok(((Integer) tlC.getField("hud").get(gui)) == 2, "HUD 渲染本体两次跑过（hud=2）");

        System.out.println("InjectSmoke1501Test PASS (" + checks + " checks)");
    }
}
