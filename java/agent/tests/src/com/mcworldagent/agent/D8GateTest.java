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
        System.out.println("D8GateTest(" + vid + ") PASS (" + checks + " checks)");
    }

    private D8GateTest() {
    }
}
