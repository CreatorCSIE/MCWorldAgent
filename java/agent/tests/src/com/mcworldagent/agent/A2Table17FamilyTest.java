// [全覆盖批产 b1.7 带批 2026-11-28] b1.7 带三版内建表对榫（仿 A2Table16FamilyTest 体例——
// 本带=75 槽零断代，钉值全继承 b1.7.3 锚）
package com.mcworldagent.agent;

import java.io.File;
import java.util.Map;

/**
 * b1.7/b1.7_01/b1.7.2 三版转正对榫（derive17 派生表 vs SlotVerify v2.1 草稿锚）：
 * 1. BY_SHA1 反查+BY_ID 同表+activate 三态（戒律 14 严格指纹，sha 对榫草稿
 *    JSON 实算值=在库 jar Get-FileHash 双路一致）；
 * 2. 75 ops 槽逐槽 vs registry/fingerprints/b1.7{,_01,.2}.json（含 null 槽）+
 *    mobClasses 9 键——derive17 转写零漂移实证；
 * 3. 零断代钉值：三版全等基准锚 b1.7.3——healthF=Y/yawF=aS/pitchF=aT+
 *    pos aM-aO/mot aP-aR+setMetaSilentM=d（v2.1 假红判决槽：真件 (IIII)V，
 *    WorldOps getMethod 不校验返回型，钉值继承）；
 * 4. 单源继承面：family=G3+hudXyzPaint=false+capWeather/capSleep；
 * 5. TARGETS_173 复用：三版 activate 后 forClass 三组命中=同数组对象
 *    （GROUPS 硬编码命中非 fromTable——SlotVerify hooks 腿已验六元组三版全等）；
 * 6. 注入冒烟腿：实形 fd/uq 真字节（root173 合成件=带内三版同形）+ASM 合成
 *    Minecraft 逐版 transform 增长（四钩织入面三版同形）；
 * 7. b1.7.3 主表回归：派生不改基（base 钉值面未动，独立实例）。
 */
public class A2Table17FamilyTest {
    static int checks = 0;

    static void ok(boolean c, String n) {
        checks++;
        if (!c) {
            System.out.println("FAIL: " + n);
            throw new RuntimeException("check failed: " + n);
        }
    }

    static final String FP = "E:/4k/github-projects/MCWorldAgent/java/agent/registry/fingerprints/";

    /** {versionId, jarSha1}——sha 对榫草稿 JSON 实算值（version-index L370-372
     *  原始 jar；b1.7.1 不存在、b1.7.10 假件不收）。 */
    static final String[][] THREE = {
        {"b1.7", "ad7960853437bcab86bd72c4a1b95f6fe19f4258"},
        {"b1.7_01", "b0f67f3105e32b1a905d934f1e56117ef45d7148"},
        {"b1.7.2", "7dc50cc5e2ff204a7283f0c7d38cd0370b49875b"},
    };

    static byte[] readTmp(String rel) throws Exception {
        return java.nio.file.Files.readAllBytes(
                new File(System.getProperty("java.io.tmpdir"), "mcaut\\tests-out\\" + rel).toPath());
    }

    @SuppressWarnings("unchecked")
    public static void main(String[] a) throws Exception {
        VersionTable base = VersionRegistry.byVersionId("b1.7.3");
        ok(base != null, "b1.7.3 基表在位");
        byte[] rawFd = readTmp("a2out173\\fd.class");
        byte[] rawUq = readTmp("a2out173\\uq.class");
        byte[] rawMc = InjectSmoke173Test.asmMinecraft173();
        for (int i = 0; i < THREE.length; i++) {
            String id = THREE[i][0];
            String sha = THREE[i][1];
            // ---- 1. 注册三态 ----
            VersionTable t = VersionRegistry.byJarSha1(sha);
            ok(t != null && id.equals(t.versionId), id + " byJarSha1 反查");
            ok(t == VersionRegistry.byVersionId(id), id + " byVersionId 同表");
            ok(VersionRegistry.activate(id), id + " activate");
            ok(t != base, id + " 独立实例（不占 b1.7.3 主表）");
            // ---- 2. 单源继承面 ----
            ok("G3".equals(t.family), id + " family=G3");
            ok(Boolean.FALSE.equals(t.hudXyzPaint), id + " hudXyzPaint=false 继承");
            ok(Boolean.TRUE.equals(t.capWeather) && Boolean.TRUE.equals(t.capSleep),
                    id + " capWeather/capSleep 继承");
            // ---- 3. 零断代钉值（全等基准锚——SlotVerify v2.1 75/75 判决的表侧复写） ----
            ok("Y".equals(t.healthF) && "aS".equals(t.yawF) && "aT".equals(t.pitchF),
                    id + " 三槽 healthF/yawF/pitchF=Y/aS/aT");
            ok("aM".equals(t.posXF) && "aN".equals(t.posYF) && "aO".equals(t.posZF)
                    && "aP".equals(t.motXF) && "aQ".equals(t.motYF) && "aR".equals(t.motZF),
                    id + " double 组 pos aM-aO/mot aP-aR");
            ok("d".equals(t.setMetaSilentM), id + " setMetaSilentM=d（v2.1 假红槽=void 真件继承）");
            // ---- 4. 75 ops 槽 vs 草稿 JSON ----
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
            ok(bc == 75, id + " 75 槽 got " + bc);
            Map mobs = A2TableTest.obj(ops.get("mobClasses"));
            int mn = 0;
            for (Object mk : mobs.keySet()) {
                ok(A2TableTest.str(mobs.get(mk)).equals(t.mobClasses.get(mk)), id + " mob " + mk);
                mn++;
            }
            ok(mn == 9 && t.mobClasses.size() == 9, id + " mobClasses 9");
            // ---- 5. TARGETS_173 复用（GROUPS 命中=同对象） ----
            HookTargets.activate(id);
            HookTargets.Target[] fdT = HookTargets.forClass("fd");
            ok(fdT.length == 1 && fdT[0] == HookTargets.TARGETS_173[0],
                    id + " SAVE_WORLD=TARGETS_173[0] 同对象");
            HookTargets.Target[] mct = HookTargets.forClass("net/minecraft/client/Minecraft");
            ok(mct.length == 2 && mct[0] == HookTargets.TARGETS_173[1]
                    && mct[1] == HookTargets.TARGETS_173[2], id + " LOAD_WORLD/RUN_TICK 同对象");
            HookTargets.Target[] hudt = HookTargets.forClass("uq");
            ok(hudt.length == 1 && hudt[0] == HookTargets.TARGETS_173[3], id + " HUD_RENDER 同对象");
            // ---- 6. 注入冒烟腿（三版同形=带内零断代的织入面实证） ----
            byte[] w1 = new Transformers().transform(null, "fd", null, null, rawFd);
            ok(w1 != null && w1.length > rawFd.length, id + " fd SAVE_WORLD 织入增长");
            byte[] w2 = new Transformers().transform(null, "uq", null, null, rawUq);
            ok(w2 != null && w2.length > rawUq.length, id + " uq HUD 织入增长");
            byte[] w3 = new Transformers().transform(null, "net/minecraft/client/Minecraft",
                    null, null, rawMc);
            ok(w3 != null && w3.length > rawMc.length, id + " Minecraft LOAD/RUN_TICK 织入增长");
        }
        // ---- 7. b1.7.3 主表回归（派生不改基） ----
        ok("Y".equals(base.healthF) && "aS".equals(base.yawF) && "aT".equals(base.pitchF)
                && "d".equals(base.setMetaSilentM),
                "b1.7.3 主表钉值面未动（derive17 新实例）");
        System.out.println("A2Table17FamilyTest PASS (" + checks + " checks)");
    }
}
