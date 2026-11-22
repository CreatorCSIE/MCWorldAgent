// [B5-ARCHIVE 2026-11-22] headless test archived from %TEMP%\mcaut (source batch: A2 (0618/0629/1121)); run via java/agent/tests/run.ps1
package com.mcworldagent.agent;

import java.io.File;
import java.lang.reflect.Method;

/**
 * 0629 接入批验收：HookTargets 0629 组结构断言 + SAVE_WORLD (Liw;)V 注入冒烟——
 * ASM 织入假根类 ez（public final 包装 a(iw)，el0629-2 翻案：钩挂包装非私有本体），
 * 触发包装传 null → 钩回调 onSaveWorld 收到 world + ICONST_0 观察位（else 分支），
 * 体内 null 守卫→委托 a(true,null)→saveCalls++（复刻真身 null 路径）。
 * 织入输入=FakeOps 同套 root0629 编译产物（a2out\ez.class，未织入版）。
 */
public class InjectSmoke0629Test {

    static int checks = 0;

    static void ok(boolean c, String msg) {
        checks++;
        if (!c) {
            throw new AssertionError(msg);
        }
    }

    /** parent-last 装载器：ez 用织入字节定义，iw/em 等其余委托 app loader。 */
    static class Loader extends ClassLoader {
        private final byte[] bytes;

        Loader(ClassLoader parent, byte[] bytes) {
            super(parent);
            this.bytes = bytes;
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            if (name.equals("ez")) {
                Class<?> c = defineClass(name, bytes, 0, bytes.length);
                if (resolve) {
                    resolveClass(c);
                }
                return c;
            }
            return super.loadClass(name, resolve);
        }
    }

    public static void main(String[] a) throws Exception {
        // ---- 0629 组结构（扁平根类名=无斜杠 internal name） ----
        HookTargets.activate("inf-20100629");
        HookTargets.Target[] w29 = HookTargets.forClass("ez");
        ok(w29.length == 1, "0629 World 1 target");
        ok(HookTargets.SAVE_WORLD.equals(w29[0].hookId), "save-world");
        ok("a".equals(w29[0].methodName) && "(Liw;)V".equals(w29[0].desc), "a(Liw;)V 包装（el0629-2 翻案）");
        ok(w29[0].access == 0x0011 && w29[0].expectedHits == 1, "public final/hits");
        ok(w29[0].injectMode == HookTargets.ENTRY, "ENTRY");
        HookTargets.Target[] m29 = HookTargets.forClass("at");
        ok(m29.length == 2, "0629 Minecraft 2 targets");
        HookTargets.Target lw = null, rt = null;
        for (HookTargets.Target t : m29) {
            if (HookTargets.LOAD_WORLD.equals(t.hookId)) lw = t;
            if (HookTargets.RUN_TICK.equals(t.hookId)) rt = t;
        }
        ok(lw != null && "a".equals(lw.methodName)
                && "(Lez;Ljava/lang/String;)V".equals(lw.desc)
                && lw.access == 0x0002 && lw.expectedHits == 1, "load-world a(ez,String)V private");
        ok(rt != null && "m".equals(rt.methodName) && "()V".equals(rt.desc)
                && rt.access == 0x0002, "runTick m()V private");
        HookTargets.Target[] hud29 = HookTargets.forClass("gf");
        ok(hud29.length == 1 && HookTargets.HUD_RENDER.equals(hud29[0].hookId)
                && "a".equals(hud29[0].methodName) && "(F)V".equals(hud29[0].desc)
                && hud29[0].access == 0x0011 && hud29[0].injectMode == HookTargets.EXIT,
                "0629 HUD gf.a(F)V 0x0011 EXIT（与 0327/0618 全同形）");
        // 跨组隔离：0629 激活期 0618 类名零钩
        ok(HookTargets.forClass("net/minecraft/a/a/g").length == 0, "0618 World 在 0629 组零钩");
        HookTargets.activate("inf-20100327");
        ok(HookTargets.forClass("ez").length == 0, "0629 类在 0327 组零钩");
        HookTargets.activate("inf-20100629");

        // ---- InjectMV (Liw;)V 织入冒烟（else 分支 ICONST_0；el0629-2 翻案=钩挂 public 包装） ----
        File cls = new File(System.getProperty("java.io.tmpdir"),
                "mcaut\\tests-out\\a2out\\ez.class");
        byte[] raw = java.nio.file.Files.readAllBytes(cls.toPath());
        MCAHooks.noFirstBrick = true;
        MCAHooks.capturedWorld = null;
        java.io.StringWriter sw = new java.io.StringWriter();
        MCAHooks.out = new java.io.PrintWriter(sw);
        java.lang.reflect.Field scf = MCAHooks.class.getDeclaredField("SAVE_CALLS");
        scf.setAccessible(true);
        java.util.concurrent.atomic.AtomicInteger saveCalls =
                (java.util.concurrent.atomic.AtomicInteger) scf.get(null);
        int hooksBefore = saveCalls.get();
        byte[] out = new Transformers().transform(null, "ez", null, null, raw);
        ok(out != null, "transformed");
        ok(out.length > raw.length, "bytes grew");

        Loader l = new Loader(InjectSmoke0629Test.class.getClassLoader(), out);
        Class<?> c = l.loadClass("ez");
        Object world = c.getConstructor().newInstance();
        Method saveWrap = c.getMethod("a", Class.forName("iw"));
        saveWrap.invoke(world, (Object) null); // 活体同形：传 null（本体 null 守卫）
        MCAHooks.out.flush();
        String logged = sw.toString();
        MCAHooks.out = null;

        ok(MCAHooks.capturedWorld == world, "onSavedWorld captured world");
        ok(logged.contains("SAVE_WORLD") && logged.contains("indirect=false"),
                "ICONST_0 observe slot, log=" + logged.trim());
        ok(saveCalls.get() == hooksBefore + 1, "SAVE_CALLS++");
        ok(((Integer) c.getField("saveCalls").get(world)) == 1, "original wrapper->body intact");

        System.out.println("InjectSmoke0629Test PASS (" + checks + " checks)");
    }
}
