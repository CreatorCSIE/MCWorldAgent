// [全覆盖批产 b1.8 带批 2026-11-28] b1.8 带两版内建表对榫（仿 A2Table17FamilyTest
// 体例——本带=74 槽零断代，钉值全继承 b1.8.1 锚；pre1 两件=独立映射带留案不收）
package com.mcworldagent.agent;

import java.io.File;
import java.util.Map;

/**
 * b1.8/b1.8-pre2 两版转正对榫（derive18 派生表 vs SlotVerify v2.1 草稿锚）：
 * 1. BY_SHA1 反查+BY_ID 同表+activate 三态（戒律 14 严格指纹，sha 对榫草稿
 *    JSON 实算值=在库 jar Get-FileHash 双路一致）；
 * 2. 75 ops 槽逐槽 vs registry/fingerprints/b1.8{,-pre2}.json（含 null 槽）+
 *    mobClasses 8 键（el181-1 mob 槽摘除面）——derive18 转写零漂移实证；
 * 3. 零断代钉值：两版全等基准锚 b1.8.1——healthF=bz/iframesF=aa/yawF=u/pitchF=v+
 *    pos o/p/q/mot r/s/t+setMetaSilentM=f+itemClassG=sv/itemsF=f+
 *    entityListClassG=um+difficultyF=v+biomeNameF=l+invGetM=d_
 *    （旧 F6 draft 漂移面 gb/hy/q/p/y 终裁不成立）；
 * 4. 单源继承面：family=G3+hudXyzPaint=false+capWeather/capSleep+
 *    b1.8+ 新面 capXp/capHunger/capEffect/capGamemode（181 首版实装随带继承）；
 * 5. TARGETS_181 复用：两版 activate 后 forClass 三组命中=同数组对象
 *    （GROUPS 硬编码命中非 fromTable——SlotVerify hooks 腿已验六元组两版全等）；
 * 6. 注入冒烟腿：实形 rv/abj 真字节（a2out181 合成件=带内两版同形）+ASM 合成
 *    Minecraft 逐版 transform 增长（四钩织入面两版同形）；
 * 7. b1.8.1 主表回归：派生不改基（base 钉值面未动，独立实例）。
 */
public class A2Table18FamilyTest {
    static int checks = 0;

    static void ok(boolean c, String n) {
        checks++;
        if (!c) {
            System.out.println("FAIL: " + n);
            throw new RuntimeException("check failed: " + n);
        }
    }

    static final String FP = "E:/4k/github-projects/MCWorldAgent/java/agent/registry/fingerprints/";

    /** {versionId, jarSha1}——sha 对榫草稿 JSON 实算值（version-index L377-378
     *  原始 jar；121559 构建不在库、pre1 两件独立映射带不收）。 */
    static final String[][] TWO = {
        {"b1.8", "3139e9c29b2c74f59ea04de760ac2af5bc21b410"},
        {"b1.8-pre2", "44191f2895bf1e064269c9279778f2e3e9c3c9c7"},
    };

    static byte[] readTmp(String rel) throws Exception {
        return java.nio.file.Files.readAllBytes(
                new File(System.getProperty("java.io.tmpdir"), "mcaut\\tests-out\\" + rel).toPath());
    }

    @SuppressWarnings("unchecked")
    public static void main(String[] a) throws Exception {
        VersionTable base = VersionRegistry.byVersionId("b1.8.1");
        ok(base != null, "b1.8.1 基表在位");
        byte[] rawRv = readTmp("a2out181\\rv.class");
        byte[] rawAbj = readTmp("a2out181\\abj.class");
        byte[] rawMc = InjectSmoke181Test.asmMinecraft181();
        for (int i = 0; i < TWO.length; i++) {
            String id = TWO[i][0];
            String sha = TWO[i][1];
            // ---- 1. 注册三态 ----
            VersionTable t = VersionRegistry.byJarSha1(sha);
            ok(t != null && id.equals(t.versionId), id + " byJarSha1 反查");
            ok(t == VersionRegistry.byVersionId(id), id + " byVersionId 同表");
            ok(VersionRegistry.activate(id), id + " activate");
            ok(t != base, id + " 独立实例（不占 b1.8.1 主表）");
            // ---- 2. 单源继承面 ----
            ok("G3".equals(t.family), id + " family=G3");
            ok(Boolean.FALSE.equals(t.hudXyzPaint), id + " hudXyzPaint=false 继承");
            ok(Boolean.TRUE.equals(t.capWeather) && Boolean.TRUE.equals(t.capSleep),
                    id + " capWeather/capSleep 继承");
            ok(Boolean.TRUE.equals(t.capXp) && Boolean.TRUE.equals(t.capHunger)
                            && Boolean.TRUE.equals(t.capEffect) && Boolean.TRUE.equals(t.capGamemode),
                    id + " b1.8+ 新面 capXp/capHunger/capEffect/capGamemode 继承");
            // ---- 3. 零断代钉值（全等基准锚——SlotVerify v2.1 74/74 判决的表侧复写） ----
            ok("bz".equals(t.healthF) && "aa".equals(t.iframesF)
                    && "u".equals(t.yawF) && "v".equals(t.pitchF),
                    id + " 四槽 healthF/iframesF/yawF/pitchF=bz/aa/u/v");
            ok("o".equals(t.posXF) && "p".equals(t.posYF) && "q".equals(t.posZF)
                    && "r".equals(t.motXF) && "s".equals(t.motYF) && "t".equals(t.motZF),
                    id + " double 组 pos o-q/mot r-t");
            ok("f".equals(t.setMetaSilentM), id + " setMetaSilentM=f（b1.7.3=d 断代带值）");
            ok("sv".equals(t.itemClassG) && "f".equals(t.itemsF),
                    id + " itemClassG/itemsF=sv/f（旧 F6 draft gb/c 漂移终裁）");
            ok("um".equals(t.entityListClassG), id + " entityListClassG=um（旧 F6 draft hy 漂移终裁）");
            ok("v".equals(t.difficultyF) && "q".equals(t.hudFontF) && "z".equals(t.hudSettingsF),
                    id + " difficultyF/hudFontF/hudSettingsF=v/q/z（旧 F6 draft q/p/y 漂移终裁）");
            ok("l".equals(t.biomeNameF) && "d_".equals(t.invGetM), id + " biomeNameF/invGetM=l/d_");
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
            ok(mn == 8 && t.mobClasses.size() == 8, id + " mobClasses 8（mob 槽摘除面）");
            // ---- 5. TARGETS_181 复用（GROUPS 命中=同对象） ----
            HookTargets.activate(id);
            HookTargets.Target[] rvT = HookTargets.forClass("rv");
            ok(rvT.length == 1 && rvT[0] == HookTargets.TARGETS_181[0],
                    id + " SAVE_WORLD=TARGETS_181[0] 同对象");
            HookTargets.Target[] mct = HookTargets.forClass("net/minecraft/client/Minecraft");
            ok(mct.length == 2 && mct[0] == HookTargets.TARGETS_181[1]
                    && mct[1] == HookTargets.TARGETS_181[2], id + " LOAD_WORLD/RUN_TICK 同对象");
            HookTargets.Target[] hudt = HookTargets.forClass("abj");
            ok(hudt.length == 1 && hudt[0] == HookTargets.TARGETS_181[3], id + " HUD_RENDER 同对象");
            // ---- 6. 注入冒烟腿（两版同形=带内零断代的织入面实证） ----
            byte[] w1 = new Transformers().transform(null, "rv", null, null, rawRv);
            ok(w1 != null && w1.length > rawRv.length, id + " rv SAVE_WORLD 织入增长");
            byte[] w2 = new Transformers().transform(null, "abj", null, null, rawAbj);
            ok(w2 != null && w2.length > rawAbj.length, id + " abj HUD 织入增长");
            byte[] w3 = new Transformers().transform(null, "net/minecraft/client/Minecraft",
                    null, null, rawMc);
            ok(w3 != null && w3.length > rawMc.length, id + " Minecraft LOAD/RUN_TICK 织入增长");
        }
        // ---- 7. b1.8.1 主表回归（派生不改基） ----
        ok("bz".equals(base.healthF) && "u".equals(base.yawF) && "v".equals(base.pitchF)
                && "f".equals(base.setMetaSilentM) && "sv".equals(base.itemClassG),
                "b1.8.1 主表钉值面未动（derive18 新实例）");
        System.out.println("A2Table18FamilyTest PASS (" + checks + " checks)");
    }
}
