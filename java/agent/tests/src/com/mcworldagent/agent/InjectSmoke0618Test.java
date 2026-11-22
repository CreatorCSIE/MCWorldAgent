// [B5-ARCHIVE 2026-11-22] headless test archived from %TEMP%\mcaut (source batch: A2 (0618/0629/1121)); run via java/agent/tests/run.ps1
package com.mcworldagent.agent;

import java.io.File;
import java.lang.reflect.Method;

/**
 * A2 验收 6：HookTargets 0618 组结构断言 + InjectMV ()V 注入冒烟——
 * ASM 织入假 net/minecraft/a/a/g（public final void f()），触发回调
 * onSaveWorld 收到 world+false（ICONST_0 观察位）。
 */
public class InjectSmoke0618Test {

    static int checks = 0;

    static void ok(boolean c, String msg) {
        checks++;
        if (!c) {
            throw new AssertionError(msg);
        }
    }

    /** parent-last 装载器：目标类用织入字节定义，其余委托 app loader（解析 MCAHooks）。 */
    static class Loader extends ClassLoader {
        private final byte[] bytes;

        Loader(ClassLoader parent, byte[] bytes) {
            super(parent);
            this.bytes = bytes;
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            if (name.equals("net.minecraft.a.a.g")) {
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
        // ---- 0618 组结构 ----
        HookTargets.activate("inf-20100618");
        HookTargets.Target[] g18 = HookTargets.forClass("net/minecraft/a/a/g");
        ok(g18.length == 1, "0618 World 1 target");
        ok(HookTargets.SAVE_WORLD.equals(g18[0].hookId), "save-world");
        // F1 裁决 2026-11-21 对齐：包装 f()V→core b(Z)V private（0327 先例同型）
        ok("b".equals(g18[0].methodName) && "(Z)V".equals(g18[0].desc), "b(Z)V");
        ok(g18[0].access == 0x0002 && g18[0].expectedHits == 1, "access/hits");
        ok(g18[0].injectMode == HookTargets.ENTRY, "ENTRY");
        HookTargets.Target[] m18 = HookTargets.forClass("net/minecraft/client/d");
        // a3-0618-2/3 终审后 0618 组 d 类双钩：LOAD_WORLD（装载即捕获）+ RUN_TICK
        ok(m18.length == 2, "0618 client.d 2 targets");
        HookTargets.Target lw = null, rt = null;
        for (HookTargets.Target t : m18) {
            if (HookTargets.LOAD_WORLD.equals(t.hookId)) lw = t;
            if (HookTargets.RUN_TICK.equals(t.hookId)) rt = t;
        }
        ok(lw != null && "a".equals(lw.methodName)
                && "(Lnet/minecraft/a/a/g;Ljava/lang/String;)V".equals(lw.desc)
                && lw.access == 0x0002 && lw.expectedHits == 1, "load-world a(g,String)V private");
        ok(rt != null && "l".equals(rt.methodName) && "()V".equals(rt.desc)
                && rt.access == 0x0002, "l()V private");
        ok(HookTargets.forClass("net/minecraft/a/a/f").length == 0, "0327 World 类在 0618 组零钩（同字母异义陷阱）");
        // HUD 补钉（A3 实跑派生修复）：0618 GuiIngame 与 0327 同形同 access
        HookTargets.Target[] hud18 = HookTargets.forClass("net/minecraft/client/c/m");
        ok(hud18.length == 1 && HookTargets.HUD_RENDER.equals(hud18[0].hookId)
                && "a".equals(hud18[0].methodName) && "(F)V".equals(hud18[0].desc)
                && hud18[0].access == 0x0011 && hud18[0].expectedHits == 1
                && hud18[0].injectMode == HookTargets.EXIT, "0618 HUD 钩补钉 EXIT c.m.a(F)V 0x0011");

        // ---- 0327 组回归结构 ----
        HookTargets.activate("inf-20100327");
        ok(HookTargets.forClass("net/minecraft/a/a/f").length == 2, "0327 World 2 targets");
        HookTargets.activate("inf-20100618");

        // ---- InjectMV ()V 织入冒烟 ----
        File cls = new File(System.getProperty("java.io.tmpdir"),
                "mcaut\\tests-out\\inj0618\\net\\minecraft\\a\\a\\g.class");
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
        byte[] out = new Transformers().transform(null, "net/minecraft/a/a/g", null, null, raw);
        ok(out != null, "transformed");
        ok(out.length > raw.length, "bytes grew");

        Loader l = new Loader(InjectSmoke0618Test.class.getClassLoader(), out);
        Class<?> c = l.loadClass("net.minecraft.a.a.g");
        Object world = c.newInstance();
        Method f = c.getDeclaredMethod("b", boolean.class);
        f.setAccessible(true);
        f.invoke(world, Boolean.FALSE);
        MCAHooks.out.flush();
        String logged = sw.toString();
        MCAHooks.out = null;

        ok(MCAHooks.capturedWorld == world, "onSaveWorld captured world");
        ok(logged.contains("SAVE_WORLD") && logged.contains("indirect=false"),
                "ICONST_0 observe slot, log=" + logged.trim());
        ok(saveCalls.get() == hooksBefore + 1, "SAVE_CALLS++");
        ok(((Integer) c.getField("calls").get(world)) == 1, "original body intact");

        System.out.println("InjectSmoke0618Test PASS (" + checks + " checks)");
    }
}
