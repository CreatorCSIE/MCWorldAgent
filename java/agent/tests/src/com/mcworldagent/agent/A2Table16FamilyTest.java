// [全覆盖批产试点转正批 2026-11-28] b1.6 家族六版内建表对榫（仿 A2Table16Test 转正对榫段体例）
package com.mcworldagent.agent;

import java.io.File;
import java.util.Map;

/**
 * b1.6.1-1.6.6 六版转正对榫（derive16 派生表 vs SlotVerify 草稿锚）：
 * 1. BY_SHA1 反查+BY_ID 同表+activate 三态（戒律 14 严格指纹，sha 对榫草稿
 *    JSON 实算值）；
 * 2. 76 ops 槽逐槽 vs registry/fingerprints/b1.6.{1..6}.json（含 null 槽）+
 *    mobClasses 9 键——派生转写零漂移实证；
 * 3. 断代槽钉值：1.6.1-1.6.4=X/aR/aS+pos aL-aN/mot aO-aQ（零断代继承）；
 *    1.6.5/1.6.6=Y/aS/aT+pos aM-aO / mot aP-aR（si aD 起整段右移——
 *    b1.6.5 活体翻案批 2026-11-28 补 double 组位移，断代起点=b1.6.5）；
 * 4. 单源继承面：family=G3+hudXyzPaint=false+capWeather/capSleep+pos aL..aQ
 *    double 组不动（vs 1.6.5 断代判决自洽）；
 * 5. TARGETS_16 复用：六版 activate 后 forClass 三组命中=同数组对象
 *    （GROUPS 硬编码命中非 fromTable——SlotVerify 已验钩四项六元组六版全等）；
 * 6. 注入冒烟腿：实形 fb/uk 真字节+ASM 合成 Minecraft 逐版 transform 增长
 *    （SAVE_WORLD 双参实体+LOAD/RUN_TICK+HUD 四钩织入面六版同形）；
 * 7. b1.6 主表回归：派生不改基（base 三槽仍 X/aR/aS，独立实例）。
 */
public class A2Table16FamilyTest {
    static int checks = 0;

    static void ok(boolean c, String n) {
        checks++;
        if (!c) {
            System.out.println("FAIL: " + n);
            throw new RuntimeException("check failed: " + n);
        }
    }

    static final String FP = "E:/4k/github-projects/MCWorldAgent/java/agent/registry/fingerprints/";

    /** {versionId, jarSha1, healthF, yawF, pitchF, posX, motX}——sha 对榫草稿
     *  JSON 实算值；posX/motX=double 组位移钉值（b1.6.5 活体翻案批：1.6.5/6.6
     *  aM/aP，1.6.1-4 继承 aL/aO——si aD 起整段右移，pos aM-aO/mot aP-aR）。 */
    static final String[][] SIX = {
        {"b1.6.1", "63a66d6d145696296bdaaeaba0a42f738b87a362", "X", "aR", "aS", "aL", "aO"},
        {"b1.6.2", "e8aa50949b077b672be2e651ea3f7b1bbd9020e1", "X", "aR", "aS", "aL", "aO"},
        {"b1.6.3", "924e36dbb7c64abb30a95fe35f5affb5176f6cbc", "X", "aR", "aS", "aL", "aO"},
        {"b1.6.4", "b5d3bdb8a7b12d163651f4787ac6ca14689aab9e", "X", "aR", "aS", "aL", "aO"},
        {"b1.6.5", "90ed9854b43c4d031ed07381ea3ae3071a8bba6f", "Y", "aS", "aT", "aM", "aP"},
        {"b1.6.6", "f95fe05711d09553ca2a9089f981741c13d6b8c4", "Y", "aS", "aT", "aM", "aP"},
    };

    static byte[] readTmp(String rel) throws Exception {
        return java.nio.file.Files.readAllBytes(
                new File(System.getProperty("java.io.tmpdir"), "mcaut\\tests-out\\" + rel).toPath());
    }

    @SuppressWarnings("unchecked")
    public static void main(String[] a) throws Exception {
        VersionTable base = VersionRegistry.byVersionId("b1.6");
        ok(base != null, "b1.6 基表在位");
        byte[] rawFb = readTmp("a2out16\\fb.class");
        byte[] rawUk = readTmp("a2out16\\uk.class");
        byte[] rawMc = InjectSmoke16Test.asmMinecraft16();
        for (int i = 0; i < SIX.length; i++) {
            String id = SIX[i][0];
            String sha = SIX[i][1];
            // ---- 1. 注册三态 ----
            VersionTable t = VersionRegistry.byJarSha1(sha);
            ok(t != null && id.equals(t.versionId), id + " byJarSha1 反查");
            ok(t == VersionRegistry.byVersionId(id), id + " byVersionId 同表");
            ok(VersionRegistry.activate(id), id + " activate");
            ok(t != base, id + " 独立实例（不占 b1.6 主表）");
            // ---- 2. 单源继承面 ----
            ok("G3".equals(t.family), id + " family=G3");
            ok(Boolean.FALSE.equals(t.hudXyzPaint), id + " hudXyzPaint=false 继承");
            ok(Boolean.TRUE.equals(t.capWeather) && Boolean.TRUE.equals(t.capSleep),
                    id + " capWeather/capSleep 继承");
            // ---- 3. 断代槽钉值（三语义槽+double 组位移锚——b1.6.5 活体翻案批：
            // 1.6.5/6.6 pos aM-aO/mot aP-aR，1.6.1-4 继承 aL-aN/aO-aQ） ----
            ok(SIX[i][2].equals(t.healthF) && SIX[i][3].equals(t.yawF)
                    && SIX[i][4].equals(t.pitchF),
                    id + " 三槽 healthF/yawF/pitchF=" + SIX[i][2] + "/" + SIX[i][3] + "/" + SIX[i][4]);
            boolean shift = "aM".equals(SIX[i][5]);
            ok(SIX[i][5].equals(t.posXF) && SIX[i][6].equals(t.motXF)
                    && (shift ? "aO" : "aN").equals(t.posZF)
                    && (shift ? "aR" : "aQ").equals(t.motZF),
                    id + " double 组位移锚 pos " + SIX[i][5] + ".." + (shift ? "aO" : "aN")
                    + "/mot " + SIX[i][6] + ".." + (shift ? "aR" : "aQ"));
            // ---- 4. 76 ops 槽 vs 草稿 JSON ----
            Map j = A2TableTest.obj(A2TableTest.parseJson(new String(
                    java.nio.file.Files.readAllBytes(new File(FP + id + ".json").toPath()),
                    "UTF-8")));
            ok(id.equals(A2TableTest.str(j.get("versionId"))), id + " JSON versionId 对榫");
            ok(sha.equals(A2TableTest.str(j.get("jarSha1"))), id + " JSON jarSha1 对榫");
            Map ops = A2TableTest.obj(j.get("ops"));
            int bc = 0;
            for (Object ko : ops.keySet()) {
                String key = (String) ko;
                if ("mobClasses".equals(key)) {
                    continue;
                }
                Object jv = ops.get(key);
                ok(jv == null ? A2TableTest.slot(t, key) == null
                        : jv.equals(A2TableTest.slot(t, key)), id + " ops " + key);
                bc++;
            }
            ok(bc == 76, id + " 76 槽 got " + bc);
            Map mobs = A2TableTest.obj(ops.get("mobClasses"));
            int mn = 0;
            for (Object mk : mobs.keySet()) {
                ok(A2TableTest.str(mobs.get(mk)).equals(t.mobClasses.get(mk)), id + " mob " + mk);
                mn++;
            }
            ok(mn == 9 && t.mobClasses.size() == 9, id + " mobClasses 9");
            // ---- 5. TARGETS_16 复用（GROUPS 命中=同对象） ----
            HookTargets.activate(id);
            HookTargets.Target[] fbt = HookTargets.forClass("fb");
            ok(fbt.length == 1 && fbt[0] == HookTargets.TARGETS_16[0],
                    id + " SAVE_WORLD=TARGETS_16[0] 同对象");
            HookTargets.Target[] mct = HookTargets.forClass("net/minecraft/client/Minecraft");
            ok(mct.length == 2 && mct[0] == HookTargets.TARGETS_16[1]
                    && mct[1] == HookTargets.TARGETS_16[2], id + " LOAD_WORLD/RUN_TICK 同对象");
            HookTargets.Target[] hudt = HookTargets.forClass("uk");
            ok(hudt.length == 1 && hudt[0] == HookTargets.TARGETS_16[3], id + " HUD_RENDER 同对象");
            // ---- 6. 注入冒烟腿（六版同形） ----
            byte[] w1 = new Transformers().transform(null, "fb", null, null, rawFb);
            ok(w1 != null && w1.length > rawFb.length, id + " fb SAVE_WORLD 织入增长");
            byte[] w2 = new Transformers().transform(null, "uk", null, null, rawUk);
            ok(w2 != null && w2.length > rawUk.length, id + " uk HUD 织入增长");
            byte[] w3 = new Transformers().transform(null, "net/minecraft/client/Minecraft",
                    null, null, rawMc);
            ok(w3 != null && w3.length > rawMc.length, id + " Minecraft LOAD/RUN_TICK 织入增长");
        }
        // ---- 7. b1.6 主表回归（派生不改基） ----
        ok("X".equals(base.healthF) && "aR".equals(base.yawF) && "aS".equals(base.pitchF),
                "b1.6 主表三槽未动（derive16 新实例）");
        System.out.println("A2Table16FamilyTest PASS (" + checks + " checks)");
    }
}
