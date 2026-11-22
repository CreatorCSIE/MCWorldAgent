// [B5-ARCHIVE 2026-11-22] headless test archived from %TEMP%\mcaut (source batch: A2 a1.2.0); run via java/agent/tests/run.ps1
package com.mcworldagent.agent;

import java.io.File;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

public class InjectSmoke120Test {

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

    /** ASM 合成 a1.2.0 Minecraft 真形：e:Lcu;（theWorld）+ a(Lcu;String;dv)V 3 参实体
     *  （putfield e，钩目标——判决 B 移植批 2026-11-19 同 b1.8.1）+ a(Lcu;String)V
     *  2 参委托壳（ACONST_NULL→invokevirtual 3 参，实形对榫 @4 aconst_null 委托，不钩）+ i()V。
     *  仅声明 e:Lcu;：g/u 字段必须省略——Minecraft 链接期实例尺寸计算会解析全部非静态
     *  字段描述符类型，递归经本 Loader 加载 bo/mz；此时 map 尚无 mz（woven 版在后面），
     *  会落到 parent=AppClassLoader，而 findLoadedClass 对 parent 已加载类同样命中
     *  →后续 woven mz 永远进不来。 */
    static byte[] asmMinecraft120() {
        org.objectweb.asm.ClassWriter cw =
                new org.objectweb.asm.ClassWriter(org.objectweb.asm.ClassWriter.COMPUTE_MAXS);
        cw.visit(org.objectweb.asm.Opcodes.V1_8, org.objectweb.asm.Opcodes.ACC_PUBLIC,
                "net/minecraft/client/Minecraft", null, "java/lang/Object", null);
        cw.visitField(org.objectweb.asm.Opcodes.ACC_PUBLIC, "e", "Lcu;", null, null).visitEnd();
        org.objectweb.asm.MethodVisitor mv = cw.visitMethod(org.objectweb.asm.Opcodes.ACC_PUBLIC,
                "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(org.objectweb.asm.Opcodes.ALOAD, 0);
        mv.visitMethodInsn(org.objectweb.asm.Opcodes.INVOKESPECIAL,
                "java/lang/Object", "<init>", "()V", false);
        mv.visitInsn(org.objectweb.asm.Opcodes.RETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();
        // 3 参实体（钩目标）：putfield e=theWorld（真形体首段存旧世界与 bo 面与本冒烟无关，省略）
        mv = cw.visitMethod(org.objectweb.asm.Opcodes.ACC_PUBLIC,
                "a", "(Lcu;Ljava/lang/String;Ldv;)V", null, null);
        mv.visitCode();
        mv.visitVarInsn(org.objectweb.asm.Opcodes.ALOAD, 0);
        mv.visitVarInsn(org.objectweb.asm.Opcodes.ALOAD, 1);
        mv.visitFieldInsn(org.objectweb.asm.Opcodes.PUTFIELD,
                "net/minecraft/client/Minecraft", "e", "Lcu;");
        mv.visitInsn(org.objectweb.asm.Opcodes.RETURN);
        mv.visitMaxs(2, 4);
        mv.visitEnd();
        // 2 参委托壳（不钩）：a(world, msg) → a(world, msg, null)，实形对榫
        mv = cw.visitMethod(org.objectweb.asm.Opcodes.ACC_PUBLIC,
                "a", "(Lcu;Ljava/lang/String;)V", null, null);
        mv.visitCode();
        mv.visitVarInsn(org.objectweb.asm.Opcodes.ALOAD, 0);
        mv.visitVarInsn(org.objectweb.asm.Opcodes.ALOAD, 1);
        mv.visitVarInsn(org.objectweb.asm.Opcodes.ALOAD, 2);
        mv.visitInsn(org.objectweb.asm.Opcodes.ACONST_NULL);
        mv.visitMethodInsn(org.objectweb.asm.Opcodes.INVOKEVIRTUAL,
                "net/minecraft/client/Minecraft", "a", "(Lcu;Ljava/lang/String;Ldv;)V", false);
        mv.visitInsn(org.objectweb.asm.Opcodes.RETURN);
        mv.visitMaxs(4, 3);
        mv.visitEnd();
        mv = cw.visitMethod(org.objectweb.asm.Opcodes.ACC_PUBLIC, "i", "()V", null, null);
        mv.visitCode();
        mv.visitInsn(org.objectweb.asm.Opcodes.RETURN);
        mv.visitMaxs(0, 1);
        mv.visitEnd();
        cw.visitEnd();
        return cw.toByteArray();
    }

    public static void main(String[] a) throws Exception {
        // ---- 1. TARGETS_120 结构：五钩全 0x0001 public ----
        VersionRegistry.activate("a1.2.0");
        HookTargets.activate("a1.2.0");
        HookTargets.Target[] w20 = HookTargets.forClass("cu");
        ok(w20.length == 2, "120 World 2 targets（SAVE_WORLD+WORLD_CTOR）");
        HookTargets.Target sw = find(w20, HookTargets.SAVE_WORLD);
        HookTargets.Target wc = find(w20, HookTargets.WORLD_CTOR);
        ok(sw != null && "a".equals(sw.methodName) && "(Lph;)V".equals(sw.desc)
                && sw.access == 0x0001 && sw.expectedHits == 1 && sw.injectMode == HookTargets.ENTRY,
                "save-world a(Lph;)V 0x0001 ENTRY");
        ok(wc != null && "<init>".equals(wc.methodName)
                && "(Ljava/io/File;Ljava/lang/String;)V".equals(wc.desc)
                && wc.access == 0x0001 && wc.injectMode == HookTargets.ENTRY,
                "world-ctor <init>(File,String)V 0x0001 ENTRY");
        HookTargets.Target[] m20 = HookTargets.forClass("net/minecraft/client/Minecraft");
        ok(m20.length == 2, "120 Minecraft 2 targets");
        HookTargets.Target lw = find(m20, HookTargets.LOAD_WORLD);
        HookTargets.Target rt = find(m20, HookTargets.RUN_TICK);
        ok(lw != null && "a".equals(lw.methodName) && "(Lcu;Ljava/lang/String;Ldv;)V".equals(lw.desc)
                && lw.access == 0x0001 && lw.expectedHits == 1 && lw.injectMode == HookTargets.EXIT,
                "load-world a(Lcu;String;dv)V 0x0001 EXIT（判决 B 移植批：2 参委托壳→3 参实体改挂）");
        ok(rt != null && "i".equals(rt.methodName) && "()V".equals(rt.desc)
                && rt.access == 0x0001 && rt.injectMode == HookTargets.ENTRY,
                "runTick i()V 0x0001 ENTRY");
        HookTargets.Target[] hud20 = HookTargets.forClass("mz");
        ok(hud20.length == 1 && HookTargets.HUD_RENDER.equals(hud20[0].hookId)
                && "a".equals(hud20[0].methodName) && "(FZII)V".equals(hud20[0].desc)
                && hud20[0].access == 0x0001 && hud20[0].injectMode == HookTargets.EXIT,
                "120 HUD mz.a(FZII)V 0x0001 EXIT");
        // 跨组隔离：120 激活期 1121/0629 类名零钩；反向亦然
        ok(HookTargets.forClass("cn").length == 0 && HookTargets.forClass("lu").length == 0
                && HookTargets.forClass("nu").length == 0, "1121 类在 120 组零钩");
        HookTargets.activate("a1.1.2_01");
        ok(HookTargets.forClass("cu").length == 0 && HookTargets.forClass("mz").length == 0
                && HookTargets.forClass("ph").length == 0, "120 类在 1121 组零钩");
        HookTargets.activate("inf-20100629");
        ok(HookTargets.forClass("cu").length == 0 && HookTargets.forClass("mz").length == 0,
                "120 类在 0629 组零钩");
        HookTargets.activate("a1.2.0");

        // ---- 2. 织入 cu：SAVE_WORLD ENTRY(ICONST_0) + WORLD_CTOR ENTRY ----
        byte[] rawCu = readTmp("a2out120\\cu.class");
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
        byte[] wovenCu = new Transformers().transform(null, "cu", null, null, rawCu);
        ok(wovenCu != null, "cu transformed");
        ok(wovenCu.length > rawCu.length, "cu bytes grew");

        Loader l = new Loader(InjectSmoke120Test.class.getClassLoader());
        // cu 闭包件同 loader 定义（避免双 loader 下 "cu" 类型身份分裂）
        l.put("lk", readTmp("a2out120\\lk.class"));
        l.put("ph", readTmp("a2out120\\ph.class"));
        l.put("pb", readTmp("a2out120\\pb.class"));
        l.put("fy", readTmp("a2out120\\fy.class"));
        l.put("om", readTmp("a2out120\\om.class"));
        l.put("iz", readTmp("a2out120\\iz.class"));
        l.put("cu", wovenCu);
        Class<?> c = l.loadClass("cu");
        File dir = new File(System.getProperty("java.io.tmpdir"), "mcaut\\tests-out\\smokedir120");
        Object world = c.getConstructor(File.class, String.class).newInstance(dir, "world");
        ok(MCAHooks.capturedSaveDir == dir, "WORLD_CTOR hook captured dir");
        Class<?> phC = l.loadClass("ph");
        Method save = c.getMethod("a", phC);
        Object ret = save.invoke(world, (Object) null);
        MCAHooks.out.flush();
        String logged = sw1.toString();
        ok(MCAHooks.capturedWorld == world, "onSavedWorld captured world");
        ok(logged.contains("SAVE_WORLD") && logged.contains("indirect=false"),
                "ICONST_0 观察位=false，log=" + logged.trim());
        ok(saveCalls.get() == hooksBefore + 1, "SAVE_CALLS++");
        ok(((Integer) c.getField("saveCalls").get(world)) == 1, "wrapper->body 委托 saveCalls=1");
        ok(((Integer) c.getField("levelSaves").get(world)) == 1, "body 调 saveLevel o()V levelSaves=1");
        ok(ret == null, "(Lph)V void 形（入口注入不动返回值）");

        // ---- 3. cu.a(ZLph;)V 本体 null 进度守卫语义 ----
        Method body = c.getMethod("a", boolean.class, phC);
        body.invoke(world, Boolean.FALSE, null);
        ok(((Integer) c.getField("saveCalls").get(world)) == 2, "null 守卫：直调本体不 NPE，saveCalls=2");
        ok(((Integer) c.getField("levelSaves").get(world)) == 2, "null 守卫：o()V 仍执行 levelSaves=2");
        final java.util.List progress = new java.util.ArrayList();
        Object prog = Proxy.newProxyInstance(l, new Class[] { phC }, new InvocationHandler() {
            public Object invoke(Object p, Method m, Object[] args) {
                progress.add(m.getName() + ":" + (args == null || args.length == 0 ? "" : args[0]));
                return null;
            }
        });
        body.invoke(world, Boolean.TRUE, prog);
        ok(((Integer) c.getField("saveCalls").get(world)) == 3, "非空 ph：saveCalls=3");
        ok(progress.contains("b:Saving level") && progress.contains("d:Saving chunks"),
                "非空 ph：b/d 进度回调对榫 " + progress);

        // ---- 4. 织入 Minecraft（ASM 实形）：LOAD_WORLD + RUN_TICK 回调计数 ----
        byte[] rawMc = asmMinecraft120();
        byte[] wovenMc = new Transformers().transform(null, "net/minecraft/client/Minecraft", null, null, rawMc);
        ok(wovenMc != null, "Minecraft transformed");
        ok(wovenMc.length > rawMc.length, "Minecraft bytes grew");
        l.put("net.minecraft.client.Minecraft", wovenMc);
        Class<?> mcC = l.loadClass("net.minecraft.client.Minecraft");
        Object mc = mcC.getConstructor().newInstance();
        long t0 = MCAHooks.tickCount();
        // 3 参实体=钩目标（EXIT）：直调（传送门 k() 即此径，绕过委托壳）恰中
        Method loadWorld3 = null;
        Method[] ms = mcC.getMethods();
        for (int i = 0; i < ms.length; i++) {
            if ("a".equals(ms[i].getName()) && ms[i].getParameterTypes().length == 3) {
                loadWorld3 = ms[i];
            }
        }
        ok(loadWorld3 != null, "3 参 changeWorld a(Lcu;String;dv)V 实形存在（钩目标）");
        loadWorld3.invoke(mc, world, "world", null);
        MCAHooks.out.flush();
        String logged2 = sw1.toString();
        ok(logged2.contains("WORLD_LOADED"), "LOAD_WORLD 3 参 EXIT 回调，log=" + logged2.trim());
        ok(MCAHooks.capturedWorld == world, "LOAD_WORLD 捕获同一 world（无换 world）");
        ok(mcC.getField("e").get(mc) == world, "changeWorld 本体 putfield e 对榫");
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
        Method runTick = mcC.getMethod("i");
        runTick.invoke(mc);
        runTick.invoke(mc);
        ok(MCAHooks.tickCount() == t0 + 2, "RUN_TICK 回调计数 +2");

        // ---- 5. 织入 mz：HUD_RENDER EXIT；onHud 链 f 字母必断=HUD_HOOK_DISABLED ----
        byte[] rawMz = readTmp("a2out120\\mz.class");
        byte[] wovenMz = new Transformers().transform(null, "mz", null, null, rawMz);
        ok(wovenMz != null, "mz transformed");
        ok(wovenMz.length > rawMz.length, "mz bytes grew");
        l.put("mz", wovenMz);
        Class<?> mzC = l.loadClass("mz");
        Object gui = mzC.getConstructor(mcC).newInstance(mc);
        Method hud = mzC.getMethod("a", float.class, boolean.class, int.class, int.class);
        hud.invoke(gui, Float.valueOf(0f), Boolean.FALSE, Integer.valueOf(0), Integer.valueOf(0));
        MCAHooks.out.flush();
        String logged3 = sw1.toString();
        MCAHooks.out = null;
        ok(logged3.contains("HUD_HOOK_DISABLED"),
                "EXIT 注入触发 onHud，链断（120 thePlayer=g 非 f）=优雅降级，log=" + logged3.trim());
        ok(((Integer) mzC.getField("hud").get(gui)) == 1, "HUD 渲染本体跑过（hud=1）");

        System.out.println("InjectSmoke120Test PASS (" + checks + " checks)");
    }
}
