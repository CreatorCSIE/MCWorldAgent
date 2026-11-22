// [B5-ARCHIVE 2026-11-22] headless test archived from %TEMP%\mcaut (source batch: A2 (0618/0629/1121)); run via java/agent/tests/run.ps1
package com.mcworldagent.agent;

import java.io.File;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * a1.1.2_01 接入批验收：HookTargets 1121 组五钩结构断言（cn 双钩 + Minecraft 双钩 + lu HUD，
 * 访问位全 0x0001 public=档位二差异面：0327/0629 runTick private、HUD a(F)V 0x0011）
 * + 注入冒烟：ASM 织入假根类 cn（SAVE_WORLD a(Lnu)V ENTRY——0629 同形 else 分支 ICONST_0
 * 观察位，包装→本体委托验证；WORLD_CTOR <init>(File,String)V ENTRY=onWorldDir 记 dir，本批启用=0327 双钩保底）
 * 与假 lu（HUD_RENDER a(FZII)V EXIT——onHud 反射链 f/m 字母在本版必断=HUD_HOOK_DISABLED
 * 优雅降级证据，HookTargets 注释判决）。Minecraft 双钩=结构断言（命名包假类无法引用默认包
 * cn 类型，0629 at/gf 同处理）。织入输入=FakeOps 同套 root1121 编译产物（a2out\cn.class 等，
 * 未织入版）。
 */
public class InjectSmoke1121Test {

    static int checks = 0;

    static void ok(boolean c, String msg) {
        checks++;
        if (!c) {
            throw new AssertionError(msg);
        }
    }

    /** parent-last 装载器：织入类用给定字节定义，其余委托 app loader。 */
    static class Loader extends ClassLoader {
        private final Map<String, byte[]> bytes = new HashMap<String, byte[]>();

        Loader(ClassLoader parent) {
            super(parent);
        }

        void put(String name, byte[] b) {
            bytes.put(name, b);
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            byte[] b = bytes.get(name);
            if (b != null) {
                Class<?> c = defineClass(name, b, 0, b.length);
                if (resolve) {
                    resolveClass(c);
                }
                return c;
            }
            return super.loadClass(name, resolve);
        }
    }

    static HookTargets.Target find(HookTargets.Target[] ts, String hookId) {
        for (HookTargets.Target t : ts) {
            if (hookId.equals(t.hookId)) {
                return t;
            }
        }
        return null;
    }

    static byte[] readTmp(String rel) throws Exception {
        return java.nio.file.Files.readAllBytes(
                new File(System.getProperty("java.io.tmpdir"), "mcaut\\tests-out\\" + rel).toPath());
    }

    public static void main(String[] a) throws Exception {
        // ---- 1121 组结构（五钩全 0x0001 public；cn 根类、Minecraft 未混淆命名类、lu 根类） ----
        HookTargets.activate("a1.1.2_01");
        HookTargets.Target[] w21 = HookTargets.forClass("cn");
        ok(w21.length == 2, "1121 World 2 targets（SAVE_WORLD+WORLD_CTOR 双钩保底）");
        HookTargets.Target sw = find(w21, HookTargets.SAVE_WORLD);
        HookTargets.Target wc = find(w21, HookTargets.WORLD_CTOR);
        ok(sw != null && "a".equals(sw.methodName) && "(Lnu;)V".equals(sw.desc), "save-world a(Lnu;)V");
        ok(sw != null && sw.access == 0x0001 && sw.expectedHits == 1 && sw.injectMode == HookTargets.ENTRY,
                "save-world public 0x0001 ENTRY（档位二=0327 0x0002 private）");
        ok(wc != null && "<init>".equals(wc.methodName)
                && "(Ljava/io/File;Ljava/lang/String;)V".equals(wc.desc)
                && wc.access == 0x0001 && wc.injectMode == HookTargets.ENTRY,
                "world-ctor <init>(File,String)V ENTRY（本批启用，同 0327 把手配方）");
        HookTargets.Target[] m21 = HookTargets.forClass("net/minecraft/client/Minecraft");
        ok(m21.length == 2, "1121 Minecraft 2 targets");
        HookTargets.Target lw = find(m21, HookTargets.LOAD_WORLD);
        HookTargets.Target rt = find(m21, HookTargets.RUN_TICK);
        ok(lw != null && "a".equals(lw.methodName) && "(Lcn;Ljava/lang/String;)V".equals(lw.desc)
                && lw.access == 0x0001 && lw.expectedHits == 1,
                "load-world a(cn,String)V public（详表标 private，javap 复核按实钉）");
        ok(rt != null && "i".equals(rt.methodName) && "()V".equals(rt.desc) && rt.access == 0x0001,
                "runTick i()V public（档位二：0327 h/0629 m 均 private）");
        HookTargets.Target[] hud21 = HookTargets.forClass("lu");
        ok(hud21.length == 1 && HookTargets.HUD_RENDER.equals(hud21[0].hookId)
                && "a".equals(hud21[0].methodName) && "(FZII)V".equals(hud21[0].desc)
                && hud21[0].access == 0x0001 && hud21[0].injectMode == HookTargets.EXIT,
                "1121 HUD lu.a(FZII)V 0x0001 EXIT（档位二换形：a(F)V 形失效）");
        // 跨组隔离：1121 激活期 0629/0327 类名零钩；反向亦然
        ok(HookTargets.forClass("ez").length == 0, "0629 World 在 1121 组零钩");
        HookTargets.activate("inf-20100629");
        ok(HookTargets.forClass("cn").length == 0 && HookTargets.forClass("lu").length == 0,
                "1121 类在 0629 组零钩");
        HookTargets.activate("inf-20100327");
        ok(HookTargets.forClass("cn").length == 0
                && HookTargets.forClass("net/minecraft/client/Minecraft").length == 0,
                "1121 类在 0327 组零钩");
        HookTargets.activate("a1.1.2_01");

        // ---- 织入冒烟 1：cn（SAVE_WORLD a(Lnu)V ENTRY ICONST_0 + WORLD_CTOR ENTRY onWorldDir） ----
        byte[] raw = readTmp("a2out\\cn.class");
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
        byte[] woven = new Transformers().transform(null, "cn", null, null, raw);
        ok(woven != null, "cn transformed");
        ok(woven.length > raw.length, "cn bytes grew");

        Loader l = new Loader(InjectSmoke1121Test.class.getClassLoader());
        l.put("cn", woven);
        Class<?> c = l.loadClass("cn");
        File dir = new File(System.getProperty("java.io.tmpdir"), "mcaut\\tests-out\\smokedir1121");
        Object world = c.getConstructor(File.class, String.class).newInstance(dir, "world");
        ok(MCAHooks.capturedSaveDir == dir, "WORLD_CTOR hook captured dir");
        Method save = c.getMethod("a", Class.forName("nu"));
        Object ret = save.invoke(world, (Object) null);
        MCAHooks.out.flush();
        String logged = sw1.toString();
        MCAHooks.out = null;

        ok(MCAHooks.capturedWorld == world, "onSavedWorld captured world");
        ok(logged.contains("SAVE_WORLD") && logged.contains("indirect=false"),
                "ICONST_0 观察位=false（0629 同形 else 分支），log=" + logged.trim());
        ok(saveCalls.get() == hooksBefore + 1, "SAVE_CALLS++");
        ok(((Integer) c.getField("saveCalls").get(world)) == 1, "wrapper->body 委托 saveCalls=1");
        ok(ret == null, "(Lnu)V void 形（入口注入不动返回值）");

        // ---- 织入冒烟 2：lu（HUD_RENDER a(FZII)V EXIT；onHud 链必断=HUD_HOOK_DISABLED） ----
        byte[] rawLu = readTmp("a2out\\lu.class");
        java.io.StringWriter sw2 = new java.io.StringWriter();
        MCAHooks.out = new java.io.PrintWriter(sw2);
        byte[] wovenLu = new Transformers().transform(null, "lu", null, null, rawLu);
        ok(wovenLu != null, "lu transformed");
        ok(wovenLu.length > rawLu.length, "lu bytes grew");
        l.put("lu", wovenLu);
        Class<?> luC = l.loadClass("lu");
        Class<?> mcC = Class.forName("net.minecraft.client.Minecraft");
        Object gui = luC.getConstructor(mcC).newInstance(mcC.getConstructor().newInstance());
        Method hud = luC.getMethod("a", float.class, boolean.class, int.class, int.class);
        hud.invoke(gui, Float.valueOf(0f), Boolean.FALSE, Integer.valueOf(0), Integer.valueOf(0));
        MCAHooks.out.flush();
        String logged2 = sw2.toString();
        MCAHooks.out = null;
        ok(logged2.contains("HUD_HOOK_DISABLED"),
                "EXIT 注入触发 onHud，反射链 f/m 字母断链=优雅降级，log=" + logged2.trim());

        System.out.println("InjectSmoke1121Test PASS (" + checks + " checks)");
    }
}
