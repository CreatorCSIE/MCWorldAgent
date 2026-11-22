// [B5-ARCHIVE 2026-11-22] headless test archived from %TEMP%\mcaut (source batch: B5 fingerprint loader); run via java/agent/tests/run.ps1
package com.mcworldagent.agent;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * B5 headless acceptance: fingerprints/*.json -> VersionTable loader.
 * Run cp: b5out;java/agent/out/classes;asm (out/classes must contain the
 * copied fingerprints resources). args[0]=agent jar (optional, jar-resource
 * assertions run only when the file exists).
 */
public final class B5LoaderTest {

    private static int checks = 0;
    private static int fails = 0;

    private static void eq(String what, Object expected, Object actual) {
        checks++;
        boolean ok = expected == null ? actual == null : expected.equals(actual);
        if (!ok) {
            fails++;
            System.out.println("FAIL " + what + " expected=" + expected + " actual=" + actual);
        }
    }

    public static void main(String[] args) throws Exception {
        // 1. loadAll finds exactly the two runtime tables (11 anchor files skipped)
        List<VersionTable> tables = FingerprintLoader.loadAll();
        eq("loadAll size", Integer.valueOf(2), Integer.valueOf(tables.size()));
        VersionTable t24 = null;
        VersionTable t03 = null;
        Set<String> ids = new HashSet<String>();
        for (int i = 0; i < tables.size(); i++) {
            VersionTable t = tables.get(i);
            ids.add(t.versionId);
            if ("inf-20100624".equals(t.versionId)) {
                t24 = t;
            }
            if ("a1.0.3".equals(t.versionId)) {
                t03 = t;
            }
        }
        eq("ids", "[a1.0.3, inf-20100624]", String.valueOf(new java.util.TreeSet<String>(ids)));
        if (t24 == null || t03 == null) {
            System.out.println("B5LoaderTest ABORT: runtime tables missing");
            System.exit(1);
        }

        // 2. header identity
        eq("0624 family", "G2", t24.family);
        eq("0624 sha", "644c050e846035e06a6637bffa2afee1e5769c8c", t24.jarSha1);
        eq("103 family", "G3", t03.family);
        eq("103 sha", "31e9736457ef3e0bfea69c720137a1bd8ba7caae", t03.jarSha1);

        // 3. inf-20100624 slot sampling vs draft/javap (46 slots incl. javap supplements)
        eq("0624 setBlockM", "c", t24.setBlockM);
        eq("0624 getBlockM", "a", t24.getBlockM);
        eq("0624 saveWorldM", "a", t24.saveWorldM);
        eq("0624 saveWorldDesc dots", "(La.b;)V", t24.saveWorldDesc);
        eq("0624 worldTimeF", "b", t24.worldTimeF);
        eq("0624 randSeedF", "m", t24.randSeedF);
        eq("0624 spawnXF", "i", t24.spawnXF);
        eq("0624 spawnYF", "j", t24.spawnYF);
        eq("0624 spawnZF", "k", t24.spawnZF);
        eq("0624 getMetaM", "d", t24.getMetaM);
        eq("0624 setMetaSilentM", "b", t24.setMetaSilentM);
        eq("0624 getLightM", "i", t24.getLightM);
        eq("0624 entityClassG", "net.minecraft.a.c.b", t24.entityClassG);
        eq("0624 setPosRotM", "b", t24.setPosRotM);
        eq("0624 setPosRotDesc", "(DDDFF)V", t24.setPosRotDesc);
        eq("0624 posXF", "j", t24.posXF);
        eq("0624 posZF", "l", t24.posZF);
        eq("0624 motXF", "m", t24.motXF);
        eq("0624 motZF", "o", t24.motZF);
        eq("0624 yawF", "p", t24.yawF);
        eq("0624 pitchF", "q", t24.pitchF);
        eq("0624 iframesF", "P", t24.iframesF);
        eq("0624 entityTypeM", "r", t24.entityTypeM);
        eq("0624 entityListM", "h", t24.entityListM);
        eq("0624 spawnEntityM", "a", t24.spawnEntityM);
        eq("0624 setEntityDeadM", "b", t24.setEntityDeadM);
        eq("0624 playSoundM", "a", t24.playSoundM);
        eq("0624 explodeM", "a", t24.explodeM);
        eq("0624 isIdF", "c", t24.isIdF);
        eq("0624 invF", "b", t24.invF);
        eq("0624 invMainF", "a", t24.invMainF);
        eq("0624 addStackM", "a", t24.addStackM);
        eq("0624 itemClassG", "net.minecraft.a.b.k", t24.itemClassG);
        eq("0624 itemsF", "b", t24.itemsF);
        eq("0624 itemEntityG", "net.minecraft.a.c.c.b", t24.itemEntityG);
        eq("0624 entityListClassG", "net.minecraft.a.c.e", t24.entityListClassG);
        eq("0624 entityListLookupM", "a", t24.entityListLookupM);
        eq("0624 playerF", "f", t24.playerF);
        eq("0624 healthF", "ab", t24.healthF);
        eq("0624 observersF", "y", t24.observersF);
        eq("0624 light6M", "e", t24.light6M);
        eq("0624 teGetM", "b", t24.teGetM);
        eq("0624 scatterClassG", "net.minecraft.a.a.d.b.a", t24.scatterClassG);
        eq("0624 difficultyF javap", "g", t24.difficultyF);
        eq("0624 hud chain javap", "f/m/u/d", t24.hudPlayerF + "/" + t24.hudFontF
                + "/" + t24.hudSettingsF + "/" + t24.hudDebugGateF);
        eq("0624 playerListF tier1 null", null, t24.playerListF);
        eq("0624 biome null", null, t24.biomeWcmM);
        eq("0624 idCrops null", null, t24.idCrops);
        eq("0624 hudXyzPaint null", null, t24.hudXyzPaint);
        eq("0624 mobs size", Integer.valueOf(9), Integer.valueOf(t24.mobClasses.size()));
        eq("0624 mob pig", "net.minecraft.a.c.b.c", t24.mobClasses.get("pig"));
        eq("0624 mob giant", "net.minecraft.a.c.a.a", t24.mobClasses.get("giant"));
        eq("0624 mob tnt", "net.minecraft.a.c.c.a", t24.mobClasses.get("tnt"));
        eq("0624 mob mob", "net.minecraft.a.c.c", t24.mobClasses.get("mob"));

        // 4. a1.0.3 slot sampling (40 slots incl. javap supplements)
        eq("103 setBlockM", "d", t03.setBlockM);
        eq("103 getBlockM", "a", t03.getBlockM);
        eq("103 saveWorldM", "a", t03.saveWorldM);
        eq("103 saveWorldDesc", "(Ljm;)V", t03.saveWorldDesc);
        eq("103 worldTimeF", "b", t03.worldTimeF);
        eq("103 randSeedF", "p", t03.randSeedF);
        eq("103 spawnXF", "l", t03.spawnXF);
        eq("103 spawnZF", "n", t03.spawnZF);
        eq("103 getMetaM", "e", t03.getMetaM);
        eq("103 setMetaSilentM", "b", t03.setMetaSilentM);
        eq("103 getLightM", "i", t03.getLightM);
        eq("103 entityClassG", "eu", t03.entityClassG);
        eq("103 setPosRotM", "b", t03.setPosRotM);
        eq("103 posXF", "j", t03.posXF);
        eq("103 motXF", "m", t03.motXF);
        eq("103 yawF", "p", t03.yawF);
        eq("103 iframesF", "U", t03.iframesF);
        eq("103 entityTypeM", "v", t03.entityTypeM);
        eq("103 entityListM", "i", t03.entityListM);
        eq("103 spawnEntityM", "a", t03.spawnEntityM);
        eq("103 setEntityDeadM", "b", t03.setEntityDeadM);
        eq("103 playSoundM", "a", t03.playSoundM);
        eq("103 explodeM", "a", t03.explodeM);
        eq("103 isDmgF", "d", t03.isDmgF);
        eq("103 invF", "b", t03.invF);
        eq("103 addStackM", "a", t03.addStackM);
        eq("103 itemClassG", "gi", t03.itemClassG);
        eq("103 itemsF", "c", t03.itemsF);
        eq("103 itemEntityG", "hc", t03.itemEntityG);
        eq("103 entityListClassG", "if", t03.entityListClassG);
        eq("103 playerF tier1 null", null, t03.playerF);
        eq("103 playerListF", "h", t03.playerListF);
        eq("103 healthF", "au", t03.healthF);
        eq("103 observersF tier1 null", null, t03.observersF);
        eq("103 light6M", "f", t03.light6M);
        eq("103 teGetM", "b", t03.teGetM);
        eq("103 scatterClassG", "bg", t03.scatterClassG);
        eq("103 difficultyF javap", "i", t03.difficultyF);
        eq("103 hud chain javap", "g/o/y/d", t03.hudPlayerF + "/" + t03.hudFontF
                + "/" + t03.hudSettingsF + "/" + t03.hudDebugGateF);
        eq("103 mobs size", Integer.valueOf(9), Integer.valueOf(t03.mobClasses.size()));
        eq("103 mob pig", "ii", t03.mobClasses.get("pig"));
        eq("103 mob giant", "ac", t03.mobClasses.get("giant"));
        eq("103 mob tnt", "dd", t03.mobClasses.get("tnt"));
        eq("103 mob mob", "ht", t03.mobClasses.get("mob"));

        // 5. registry integration: JSON ids activate, SHA1 strict hit, hardcoded wins
        eq("activate 0624", Boolean.TRUE, Boolean.valueOf(VersionRegistry.activate("inf-20100624")));
        eq("current 0624", "inf-20100624", VersionRegistry.currentVersionId());
        eq("activate 103", Boolean.TRUE, Boolean.valueOf(VersionRegistry.activate("a1.0.3")));
        eq("current 103", "a1.0.3", VersionRegistry.currentVersionId());
        eq("sha hit 0624", "inf-20100624",
                VersionRegistry.byJarSha1("644c050e846035e06a6637bffa2afee1e5769c8c").versionId);
        eq("sha hit 103", "a1.0.3",
                VersionRegistry.byJarSha1("31e9736457ef3e0bfea69c720137a1bd8ba7caae").versionId);
        eq("sha miss", null, VersionRegistry.byJarSha1("0000000000000000000000000000000000000000"));
        eq("sha miss upper", null, VersionRegistry.byJarSha1("644C050E846035E06A6637BFFA2AFEE1E5769C8C"));
        eq("activate unknown", Boolean.FALSE, Boolean.valueOf(VersionRegistry.activate("inf-99999999")));
        eq("current unchanged", "a1.0.3", VersionRegistry.currentVersionId());
        // hardcoded priority: existing 8 tables unchanged (spot values)
        eq("activate 0327", Boolean.TRUE, Boolean.valueOf(VersionRegistry.activate("inf-20100327")));
        eq("0327 unchanged", "c", VersionRegistry.current().setBlockM);
        eq("activate b1.7.3", Boolean.TRUE, Boolean.valueOf(VersionRegistry.activate("b1.7.3")));
        eq("b1.7.3 unchanged", "f", VersionRegistry.current().setBlockM);
        eq("b1.7.3 hud unchanged", "h", VersionRegistry.current().hudPlayerF);
        // resolver path: sha of a runtime jar would select via BY_SHA1 (id round-trip)
        VersionRegistry.activate("inf-20100327");

        // 6. loader strictness: unknown slot / null literal / name mismatch = loud skip
        String good = "{\"schema\":\"mca-runtime-v1\",\"versionId\":\"vx\",\"family\":\"G3\","
                + "\"jarSha1\":\"0000000000000000000000000000000000000000\",\"ops\":{\"setBlockM\":\"a\"}}";
        eq("buildTable good", "vx", FingerprintLoader.buildTable("fingerprints/vx.json", good).versionId);
        String badSlot = good.replace("setBlockM", "setBlockX");
        eq("buildTable unknown slot", null, FingerprintLoader.buildTable("fingerprints/vx.json", badSlot));
        String badNull = good.replace("\"a\"", "null");
        eq("buildTable null literal", null, FingerprintLoader.buildTable("fingerprints/vx.json", badNull));
        String badName = good;
        eq("buildTable name mismatch", null, FingerprintLoader.buildTable("fingerprints/other.json", badName));
        String badSha = good.replace("0000000000000000000000000000000000000000", "XYZ");
        eq("buildTable bad sha", null, FingerprintLoader.buildTable("fingerprints/vx.json", badSha));
        eq("buildTable anchor schema", null,
                FingerprintLoader.buildTable("fingerprints/b1.7.3.json",
                        "{\"schema\":\"mca-anchor-v1\",\"versionId\":\"b1.7.3\"}"));

        // 6b. B5-d: JSON hookSpecs -> HookTargets consumption (data-driven weaving)
        eq("0624 hookSpecs size", Integer.valueOf(5), Integer.valueOf(t24.hookSpecs.size()));
        eq("103 hookSpecs size", Integer.valueOf(5), Integer.valueOf(t03.hookSpecs.size()));
        HookTargets.activate("inf-20100624");
        specEq(HookTargets.forClass("net/minecraft/a/a/h"), HookTargets.SAVE_WORLD,
                t24.hookSpecs.get("SAVE_WORLD"), "0624 h save");
        specEq(HookTargets.forClass("net/minecraft/a/a/h"), HookTargets.WORLD_CTOR,
                t24.hookSpecs.get("WORLD_CTOR"), "0624 h ctor");
        eq("0624 h targets", Integer.valueOf(2), Integer.valueOf(HookTargets.forClass("net/minecraft/a/a/h").length));
        specEq(HookTargets.forClass("net/minecraft/client/f"), HookTargets.LOAD_WORLD,
                t24.hookSpecs.get("LOAD_WORLD"), "0624 mc load");
        specEq(HookTargets.forClass("net/minecraft/client/f"), HookTargets.RUN_TICK,
                t24.hookSpecs.get("RUN_TICK"), "0624 mc tick");
        eq("0624 mc targets", Integer.valueOf(2), Integer.valueOf(HookTargets.forClass("net/minecraft/client/f").length));
        specEq(HookTargets.forClass("net/minecraft/client/c/m"), HookTargets.HUD_RENDER,
                t24.hookSpecs.get("HUD_RENDER"), "0624 hud");
        eq("0624 hud targets", Integer.valueOf(1), Integer.valueOf(HookTargets.forClass("net/minecraft/client/c/m").length));
        eq("0624 no a103 class leak", Integer.valueOf(0), Integer.valueOf(HookTargets.forClass("fi").length));
        HookTargets.activate("a1.0.3");
        specEq(HookTargets.forClass("fi"), HookTargets.SAVE_WORLD,
                t03.hookSpecs.get("SAVE_WORLD"), "103 fi save");
        specEq(HookTargets.forClass("fi"), HookTargets.WORLD_CTOR,
                t03.hookSpecs.get("WORLD_CTOR"), "103 fi ctor");
        eq("103 fi targets", Integer.valueOf(2), Integer.valueOf(HookTargets.forClass("fi").length));
        // a1.0.3 obfuscated main class av: data-driven, no hardcoded special case
        specEq(HookTargets.forClass("av"), HookTargets.LOAD_WORLD,
                t03.hookSpecs.get("LOAD_WORLD"), "103 av load");
        specEq(HookTargets.forClass("av"), HookTargets.RUN_TICK,
                t03.hookSpecs.get("RUN_TICK"), "103 av tick");
        eq("103 av targets", Integer.valueOf(2), Integer.valueOf(HookTargets.forClass("av").length));
        specEq(HookTargets.forClass("gq"), HookTargets.HUD_RENDER,
                t03.hookSpecs.get("HUD_RENDER"), "103 gq hud");
        eq("103 gq targets", Integer.valueOf(1), Integer.valueOf(HookTargets.forClass("gq").length));
        eq("103 no 0624 class leak", Integer.valueOf(0),
                Integer.valueOf(HookTargets.forClass("net/minecraft/a/a/h").length));
        // hardcoded priority: b1.7.3 group wins (hardcoded label, not json:)
        HookTargets.activate("b1.7.3");
        HookTargets.Target[] fd = HookTargets.forClass("fd");
        eq("b173 fd 1 target", Integer.valueOf(1), Integer.valueOf(fd.length));
        eq("b173 fd hardcoded label", "World.saveWorld(progress) public wrapper", fd[0].label);
        // unknown version: fallback 0327 group unchanged (2 World targets on a.a.f)
        HookTargets.activate("inf-99999999");
        eq("unknown fallback 0327", Integer.valueOf(2),
                Integer.valueOf(HookTargets.forClass("net/minecraft/a/a/f").length));
        // fromTable rejects NEW_CTOR (6-field spec has no superOwner face)
        VersionTable badHook = new VersionTable();
        badHook.versionId = "vx";
        badHook.hookSpecs.put("GIANT_CTOR", "x|<init>|()V|1|1|NEW_CTOR");
        eq("NEW_CTOR rejected", null, HookTargets.fromTable(badHook));
        badHook.hookSpecs.clear();
        badHook.hookSpecs.put("NOT_A_HOOK", "x|a|()V|1|1|ENTRY");
        eq("unknown hook key rejected", null, HookTargets.fromTable(badHook));
        HookTargets.activate("inf-20100327");

        // 7. jar resource readability (only when built jar passed / exists)
        String jarPath = args.length > 0 ? args[0] : null;
        if (jarPath != null && new java.io.File(jarPath).isFile()) {
            JarFile jar = new JarFile(jarPath);
            try {
                String text = read(jar, "fingerprints/inf-20100624.json");
                Map m = MiniJson.parse(text);
                eq("jar res 0624 schema", "mca-runtime-v1", m.get("schema"));
                eq("jar res 0624 sha", "644c050e846035e06a6637bffa2afee1e5769c8c", m.get("jarSha1"));
                text = read(jar, "fingerprints/a1.0.3.json");
                m = MiniJson.parse(text);
                eq("jar res 103 schema", "mca-runtime-v1", m.get("schema"));
                eq("jar res 103 sha", "31e9736457ef3e0bfea69c720137a1bd8ba7caae", m.get("jarSha1"));
                // hooks anchor data: 5 well-formed entries per version
                Map hooks = (Map) MiniJson.parse(read(jar, "fingerprints/inf-20100624.json")).get("hooks");
                eq("0624 hooks count", Integer.valueOf(5), Integer.valueOf(hooks.size()));
                String sw = (String) hooks.get("SAVE_WORLD");
                eq("0624 SAVE_WORLD hook", "net/minecraft/a/a/h|a|(La/b;)V|17|1|ENTRY", sw);
                hooks = (Map) MiniJson.parse(read(jar, "fingerprints/a1.0.3.json")).get("hooks");
                eq("103 hooks count", Integer.valueOf(5), Integer.valueOf(hooks.size()));
                eq("103 RUN_TICK hook", "av|i|()V|1|1|ENTRY", hooks.get("RUN_TICK"));
            } finally {
                jar.close();
            }
        } else {
            System.out.println("NOTE jar assertions skipped (no jar arg)");
        }

        System.out.println("B5LoaderTest checks=" + checks + " fails=" + fails);
        if (fails > 0) {
            System.exit(1);
        }
        System.out.println("B5LoaderTest GREEN");
    }

    /** Assert the target for hookId in ts equals the JSON spec, field by field. */
    private static void specEq(HookTargets.Target[] ts, String hookId, String spec, String what) {
        HookTargets.Target found = null;
        for (int i = 0; i < ts.length; i++) {
            if (hookId.equals(ts[i].hookId)) {
                found = ts[i];
            }
        }
        eq(what + " present", Boolean.TRUE, Boolean.valueOf(found != null));
        if (found == null) {
            return;
        }
        String[] p = spec.split("\\|", -1);
        eq(what + " class", p[0], found.className);
        eq(what + " name", p[1], found.methodName);
        eq(what + " desc", p[2], found.desc);
        eq(what + " access", Integer.valueOf(Integer.parseInt(p[3])), Integer.valueOf(found.access));
        eq(what + " hits", Integer.valueOf(Integer.parseInt(p[4])), Integer.valueOf(found.expectedHits));
        eq(what + " mode", Integer.valueOf(HookTargets.modeOf(p[5])), Integer.valueOf(found.injectMode));
    }

    private static String read(JarFile jar, String name) throws Exception {
        JarEntry e = jar.getJarEntry(name);
        if (e == null) {
            throw new IllegalStateException("resource missing in jar: " + name);
        }
        InputStream in = jar.getInputStream(e);
        try {
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) > 0) {
                b.write(buf, 0, n);
            }
            return new String(b.toByteArray(), "UTF-8");
        } finally {
            in.close();
        }
    }
}
