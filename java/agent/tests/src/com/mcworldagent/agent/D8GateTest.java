// [D8-GATE 2026-11-25] headless gate test; run via java/agent/tests/run.ps1
package com.mcworldagent.agent;

/**
 * D8 门控响亮面（案卷 §6.1）：weather 在 capWeather 未真版本（L0 线
 * 0624/a1.0.3=JSON 运行时表，无该槽）必须 UNSUPPORTED，不静默不猜测。
 * 门检查先于字段解析——dummy world 对象即可触达判决面（零 fake 依赖）。
 */
public final class D8GateTest {

    static int checks = 0;

    static void ok(boolean c, String msg) {
        checks++;
        if (!c) {
            throw new AssertionError(msg);
        }
    }

    public static void main(String[] a) throws Exception {
        String vid = a.length > 0 ? a[0] : "inf-20100624";
        ok(VersionRegistry.activate(vid), "activate " + vid);
        VersionTable t = VersionRegistry.byVersionId(vid);
        ok(t != null, "table present");
        ok(!Boolean.TRUE.equals(t.capWeather), "capWeather unset/false on L0 line");
        // 门=判决第一站：world 对象根本不触碰（未 resolve 也走不到字段面）
        boolean unsupported = false;
        String why = "";
        try {
            WorldOps.weather(new Object(), true, false);
        } catch (WorldOps.UnsupportedOp u) {
            unsupported = true;
            why = String.valueOf(u.getMessage());
        }
        ok(unsupported, "weather on " + vid + " must UNSUPPORTED, got pass");
        ok(why.contains("capWeather") && why.contains("b1.5"), "gate reason cites slot+era: " + why);

        // D10 门控（案卷 §6.1）：setxp/giveeffect/removeeffect 在 capXp/capEffect 未真版本
        // 同样响亮 UNSUPPORTED——门=判决第一站，dummy 对象即触达（不碰字段面）。
        ok(!Boolean.TRUE.equals(t.capXp), "capXp unset/false on L0 line");
        ok(!Boolean.TRUE.equals(t.capEffect), "capEffect unset/false on L0 line");
        gateUnsupported(new RunnableEx() {
            public void run() throws Exception { WorldOps.setXp(new Object(), 1, 1); }
        }, "setxp", "capXp");
        gateUnsupported(new RunnableEx() {
            public void run() throws Exception { WorldOps.giveEffect(new Object(), 1, 100, 0); }
        }, "giveeffect", "capEffect");
        gateUnsupported(new RunnableEx() {
            public void run() throws Exception { WorldOps.removeEffect(new Object(), 1); }
        }, "removeeffect", "capEffect");
        ok(!Boolean.TRUE.equals(t.capHunger), "capHunger unset/false on L0 line");
        gateUnsupported(new RunnableEx() {
            public void run() throws Exception { WorldOps.setHunger(new Object(), 10, -1); }
        }, "sethunger", "capHunger");
        ok(!Boolean.TRUE.equals(t.capGamemode), "capGamemode unset/false on L0 line");
        gateUnsupported(new RunnableEx() {
            public void run() throws Exception { WorldOps.setGameMode(new Object(), 1); }
        }, "setgamemode", "capGamemode");
        // D14 读面同样门控（getxp/gethunger 复用 capXp/capHunger）
        gateUnsupported(new RunnableEx() {
            public void run() throws Exception { WorldOps.getXp(new Object()); }
        }, "getxp", "capXp");
        gateUnsupported(new RunnableEx() {
            public void run() throws Exception { WorldOps.getHunger(new Object()); }
        }, "gethunger", "capHunger");

        System.out.println("D8GateTest(" + vid + ") PASS (" + checks + " checks)");
    }

    interface RunnableEx {
        void run() throws Exception;
    }

    /** 门控响亮断言：op 在未放行版本必 UnsupportedOp 且理由引用能力槽名。 */
    private static void gateUnsupported(RunnableEx r, String op, String slot) {
        checks++;
        try {
            r.run();
            throw new AssertionError(op + " on L0 line must UNSUPPORTED, got pass");
        } catch (WorldOps.UnsupportedOp u) {
            String m = String.valueOf(u.getMessage());
            if (!m.contains(slot)) {
                throw new AssertionError(op + " gate reason must cite " + slot + ": " + m);
            }
        } catch (Exception e) {
            throw new AssertionError(op + " wrong exception: " + e);
        }
    }

    private D8GateTest() {
    }
}
