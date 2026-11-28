// b1.6 A2 表对榫（JSON-only 版：无内建表，无头窗 V-7 批 2026-11-27）。
// 锚库 b1.6.json 为 mca-fingerprint-v1 档案 schema（>4096B=FingerprintLoader 静默跳过
// =草稿锚纪律），本测试自带测试侧转写注入 injectTable()（与 1501/1401 批同配方）。
// 重点断代腿（vs 1401/1501，JSON note 钉值）：setBlockM=f（1401/1501=e）；getLightM=n
// （=l）；setMetaSilentM=e(IIII)Z 返回 boolean（1501 V 断代回摆+同字母共存翻面）；
// entityJoinedWorld/setEntityDead/entityList=b/e/o 三件回摆 1501 字母；invGetM=d_（同
// 1501，1401 c_ 断代带止于 1401）；pos aL..aS（+1/版规律 16 站——sister tiny 错位一位
// 勿借）；healthF=Y（173 同字母）；睡眠三槽 u protected/b/a（1501 x/b/a 形近字母异）；
// isPlayerSleeping=N()Z/isEntityAlive=W()Z；rainStrF=j/thunderStrF=l+eg 天气键（判决①
// 翻面=capWeather OK）；biome a/a/n 三锚（判决②翻面=capGetbiome OK）；giant=np 已到世
// （判决③翻面）；hudDebugGateF=B；RUN_TICK=k()V（j→k 翻面下界=b1.6）；主类六字段全洗牌。
package com.mcworldagent.agent;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Map;

public class A2Table16Test {
    static int checks = 0;
    static void ok(boolean c, String n) {
        checks++;
        if (!c) { System.out.println("FAIL: " + n); throw new RuntimeException("check failed: " + n); }
    }

    @SuppressWarnings("unchecked")
    static VersionTable injectTable() throws Exception {
        byte[] buf = java.nio.file.Files.readAllBytes(new java.io.File(
                "E:/4k/github-projects/MCWorldAgent/java/agent/registry/fingerprints/b1.6.json").toPath());
        Map j = A2TableTest.obj(A2TableTest.parseJson(new String(buf, "UTF-8")));
        VersionTable t = new VersionTable();
        t.versionId = A2TableTest.str(j.get("versionId"));
        t.family = A2TableTest.str(j.get("family"));
        t.jarSha1 = A2TableTest.str(j.get("jarSha1"));
        Map ops = A2TableTest.obj(j.get("ops"));
        for (Object ko : ops.keySet()) {
            String key = (String) ko;
            if ("mobClasses".equals(key)) {
                Map mobs = A2TableTest.obj(ops.get(key));
                for (Object mk : mobs.keySet()) {
                    t.mobClasses.put((String) mk, A2TableTest.str(mobs.get(mk)));
                }
                continue;
            }
            Object v = ops.get(key);
            if (v == null) { continue; }
            Field f = VersionTable.class.getDeclaredField(key);
            f.setAccessible(true);
            if (v instanceof String || v instanceof Boolean) {
                f.set(t, v);
            } else {
                throw new RuntimeException("ops " + key + " unsupported type " + v.getClass());
            }
        }
        Map hooks = A2TableTest.obj(j.get("hooks"));
        for (Object hk : hooks.keySet()) {
            String key = (String) hk;
            Map h = A2TableTest.obj(hooks.get(key));
            t.hookSpecs.put(key, A2TableTest.str(h.get("class")) + "|" + A2TableTest.str(h.get("method"))
                    + "|" + A2TableTest.str(h.get("desc")) + "|"
                    + Integer.parseInt(A2TableTest.str(h.get("access")).substring(2), 16) + "|"
                    + ((Double) h.get("expectedHits")).intValue() + "|" + A2TableTest.str(h.get("injectMode")));
        }
        Field bid = VersionRegistry.class.getDeclaredField("BY_ID");
        bid.setAccessible(true);
        ((Map<String, VersionTable>) bid.get(null)).put(t.versionId, t);
        Field bsha = VersionRegistry.class.getDeclaredField("BY_SHA1");
        bsha.setAccessible(true);
        ((Map<String, VersionTable>) bsha.get(null)).put(t.jarSha1, t);
        Field act = VersionRegistry.class.getDeclaredField("active");
        act.setAccessible(true);
        act.set(null, t);
        return t;
    }

    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {
        // ---- 转正对榫段（2026-11-28 转正批，注入前取内建表）----
        VersionTable builtin = VersionRegistry.byJarSha1("ecc0288d218fd7479027a17c150cbf283fa950a1");
        ok(builtin != null && "b1.6".equals(builtin.versionId), "builtin registered");
        Map opsJ = A2TableTest.obj(A2TableTest.obj(A2TableTest.parseJson(new String(
                java.nio.file.Files.readAllBytes(new java.io.File(
                "E:/4k/github-projects/MCWorldAgent/java/agent/registry/fingerprints/b1.6.json").toPath()),
                "UTF-8"))).get("ops"));
        int bc = 0;
        for (Object ko : opsJ.keySet()) {
            String key = (String) ko;
            if ("mobClasses".equals(key)) { continue; }
            Object jv = opsJ.get(key);
            ok(jv == null ? A2TableTest.slot(builtin, key) == null
                    : jv.equals(A2TableTest.slot(builtin, key)), "builtin ops " + key);
            bc++;
        }
        ok(bc == 76, "builtin 76 slots got " + bc);
        Map mobJ = A2TableTest.obj(opsJ.get("mobClasses"));
        for (Object mk : mobJ.keySet()) {
            String jv = A2TableTest.str(mobJ.get(mk));
            Object bv = builtin.mobClasses.get(mk);
            ok(jv == null ? bv == null : jv.equals(bv), "builtin mob " + mk);
        }
        ok(builtin.mobClasses.size() == 9, "builtin mobClasses 9");
        ok(Boolean.FALSE.equals(builtin.hudXyzPaint), "builtin hudXyzPaint=false（≥a1.2.3 边界线）");
        ok(Boolean.TRUE.equals(builtin.capWeather), "builtin capWeather=true（判决①翻面=天气键在位世代）");
        ok(Boolean.TRUE.equals(builtin.capSleep), "builtin capSleep=true（睡眠面三版同形）");
        ok(builtin.biomeWcmM != null && builtin.biomeNameF != null, "builtin biome 三锚在位（判决②翻面）");
        ok("np".equals(builtin.mobClasses.get("giant")), "builtin giant=np 已到世（判决③翻面）");
        // hooks 六元组 vs TARGETS_16 硬编码组（激活=GROUPS 命中，非 hookSpecs）
        HookTargets.activate("b1.6");
        Map hooksJ = A2TableTest.obj(A2TableTest.obj(A2TableTest.parseJson(new String(
                java.nio.file.Files.readAllBytes(new java.io.File(
                "E:/4k/github-projects/MCWorldAgent/java/agent/registry/fingerprints/b1.6.json").toPath()),
                "UTF-8"))).get("hooks"));
        String[][] ids = {
            {"SAVE_WORLD", HookTargets.SAVE_WORLD, "fb"},
            {"LOAD_WORLD", HookTargets.LOAD_WORLD, "net/minecraft/client/Minecraft"},
            {"RUN_TICK", HookTargets.RUN_TICK, "net/minecraft/client/Minecraft"},
            {"HUD_RENDER", HookTargets.HUD_RENDER, "uk"},
        };
        for (int i = 0; i < ids.length; i++) {
            Map h = A2TableTest.obj(hooksJ.get(ids[i][0]));
            HookTargets.Target tg = null;
            HookTargets.Target[] all = HookTargets.forClass(ids[i][2]);
            for (int k = 0; k < all.length; k++) {
                if (ids[i][1].equals(all[k].hookId)) { tg = all[k]; }
            }
            ok(tg != null, "TARGETS_16 has " + ids[i][0]);
            ok(A2TableTest.str(h.get("class")).equals(tg.className), "builtin hook " + ids[i][0] + " class");
            ok(A2TableTest.str(h.get("method")).equals(tg.methodName), "builtin hook " + ids[i][0] + " method");
            ok(A2TableTest.str(h.get("desc")).equals(tg.desc), "builtin hook " + ids[i][0] + " desc");
            ok(Integer.parseInt(A2TableTest.str(h.get("access")).substring(2), 16) == tg.access,
                    "builtin hook " + ids[i][0] + " access");
            ok(((Double) h.get("expectedHits")).intValue() == tg.expectedHits,
                    "builtin hook " + ids[i][0] + " hits");
            String mode = A2TableTest.str(h.get("injectMode"));
            ok("ENTRY".equals(mode) ? tg.injectMode == HookTargets.ENTRY
                    : "EXIT".equals(mode) && tg.injectMode == HookTargets.EXIT,
                    "builtin hook " + ids[i][0] + " mode");
        }
        ok(HookTargets.forClass("fb").length == 1, "TARGETS_16 fb 单钩");
        // ---- 原 JSON-only 对榫段（injectTable 转写注入=锚库回归面，后置执行）----
        VersionTable t = injectTable();
        // ---- 身份三锚 ----
        VersionTable bySha = VersionRegistry.byJarSha1("ecc0288d218fd7479027a17c150cbf283fa950a1");
        ok(bySha != null && "b1.6".equals(bySha.versionId), "byJarSha1 main");
        ok(t == bySha, "inject==bySha same table");
        ok(VersionRegistry.activate("b1.6"), "activate");
        ok(VersionRegistry.current() == t, "current==injected");
        ok("G3".equals(t.family), "family G3");

        // ---- ops 全表逐槽对榫（77 键含 mobClasses；capSleep/capWeather 为 Boolean）----
        Map ops = A2TableTest.obj(A2TableTest.obj(A2TableTest.parseJson(new String(
                java.nio.file.Files.readAllBytes(new java.io.File(
                "E:/4k/github-projects/MCWorldAgent/java/agent/registry/fingerprints/b1.6.json").toPath()),
                "UTF-8"))).get("ops"));
        ok(ops.size() == 77, "ops 77 keys got " + ops.size());
        int compared = 0;
        for (Object ko : ops.keySet()) {
            String key = (String) ko;
            if ("mobClasses".equals(key)) { continue; }
            ok(ops.get(key) == null
                            ? A2TableTest.slot(t, key) == null
                            : ops.get(key).equals(A2TableTest.slot(t, key)),
                    "ops " + key);
            compared++;
        }
        ok(compared == 76, "76 slots compared got " + compared);

        // ---- 断代面重点复核（vs 1401/1501/173，JSON note 钉值）----
        ok("x".equals(t.getWorldInfoM), "getWorldInfoM x（1401=w 断代、1501 同字母）");
        ok("e".equals(t.worldTimeF) && "a".equals(t.randSeedF), "worldTimeF e/randSeedF a（eg 五版同形）");
        ok("b".equals(t.spawnXF) && "c".equals(t.spawnYF) && "d".equals(t.spawnZF), "spawnXYZ b/c/d");
        ok("aL".equals(t.posXF) && "aM".equals(t.posYF) && "aN".equals(t.posZF),
                "pos aL/aM/aN（+1/版规律 16 站——⚠️sister tiny 错位一位 aM.. 勿借）");
        ok("aO".equals(t.motXF) && "aP".equals(t.motYF) && "aQ".equals(t.motZF), "mot aO/aP/aQ");
        ok("aR".equals(t.yawF) && "aS".equals(t.pitchF), "look aR/aS");
        ok("Y".equals(t.healthF), "healthF Y（lo public int——173 同字母；1401=V/1501=W 链）");
        ok("si".equals(t.entityClassG) && "d".equals(t.playerListF), "entityClassG si/playerListF d 三版稳定");
        ok("c".equals(t.invF) && "a".equals(t.invMainF) && "b".equals(t.invArmorF) && "c".equals(t.invCurF),
                "inv 面 c/a/b/c（iu=InventoryPlayer——⚠️1401/1501=f 断代，iu.d 回指）");
        ok("a".equals(t.isCountF) && "c".equals(t.isIdF) && "d".equals(t.isDmgF), "isCount a/isId c/isDmg d 四版链");
        ok("b".equals(t.teGetM), "teGetM b（TileEntity=os——1401 ma/1501 nx 断代）");
        ok("a".equals(t.invSizeM) && "d_".equals(t.invGetM) && "a".equals(t.invSetM),
                "ls 三槽 a/d_/a（⚠️取件 d_ 同 1501——1401 c_ 断代带止于 1401，严禁借表）");
        ok("a".equals(t.teBurnF) && "c".equals(t.teCookF), "teBurnF a/teCookF c（sf public int，三版同字母）");
        ok("gk".equals(t.itemClassG) && "c".equals(t.itemsF) && "hj".equals(t.itemEntityG),
                "gk.c 注册表/hj=EntityItem（⚠️16 hj=EntityItem↔1501 hj=EntityList 撞车位）");
        ok("iz".equals(t.entityListClassG) && "b".equals(t.entityListLookupM), "iz.b（查表字母 b 三版稳定）");
        ok("o".equals(t.entityListM) && "b".equals(t.spawnEntityM) && "e".equals(t.setEntityDeadM),
                "fb o/b/e（三件全回摆 1501 字母——1401 a/d/n 断代带止于 1501）");
        ok("f".equals(t.setBlockM) && "a".equals(t.getBlockM) && "e".equals(t.getMetaM)
                && "e".equals(t.setMetaSilentM) && "n".equals(t.getLightM) && "q".equals(t.difficultyF),
                "fb 方块面 f/a/e/e/n/q（⚠️setBlockM=f 非 1401/1501 e；setMetaSilent=e(IIII)Z 与 getMetaM=e(III)I 同字母共存；getLightM=n 非 l）");
        ok("a".equals(t.explodeM) && "a".equals(t.playSoundM) && "a".equals(t.addStackM), "a 三撞名槽留档");
        ok("a".equals(t.saveWorldM) && "(ZLxs;)V".equals(t.saveWorldDesc),
                "saveWorld a(ZLxs;)V 双参实体（断代带 b1.4→b1.6 延续；null 守卫 @14/@30 逐位同形 1401）");
        ok("b".equals(t.setPosRotM) && "(DDDFF)V".equals(t.setPosRotDesc), "setPosRot b(DDDFF)V 三版同字母");
        ok("b".equals(t.sleepInBedM) && "a".equals(t.wakeUpM) && "a".equals(t.sleepOkF),
                "sleep 三槽 b/a/a（gq.b(III)Lcu;/gq.a(ZZZ)V/cu.a——字母同 1401/1501 形）");
        ok(Boolean.TRUE.equals(t.capSleep) && Boolean.TRUE.equals(t.capWeather),
                "capSleep/capWeather true（判决①翻面=天气键在位世代）");
        ok("j".equals(t.rainStrF) && "l".equals(t.thunderStrF),
                "rainStrF j/thunderStrF l（fb.E()V 反汇编语义钉死：eg.o()Z→j/eg.m()Z→l）");
        ok("b".equals(t.wiSetRainM) && "a".equals(t.wiSetThunderM), "wiSet b/a（eg public setter，同 1501）");
        ok("a".equals(t.biomeWcmM) && "a".equals(t.biomeAtM) && "n".equals(t.biomeNameF),
                "biome 三锚 a/a/n（判决②翻面=群系链在位；⚠️biomeNameF=n 非 1501 m）");
        ok("h".equals(t.hudPlayerF) && "q".equals(t.hudFontF) && "z".equals(t.hudSettingsF)
                && "B".equals(t.hudDebugGateF), "HUD 链 h/q/z/B（kr.B=showDebugInfo @1142；1401 A 断代）");

        // ---- hooks 四节对榫（HookTargets.activate 经 hookSpecs 六元组）----
        Map hooks = A2TableTest.obj(A2TableTest.obj(A2TableTest.parseJson(new String(
                java.nio.file.Files.readAllBytes(new java.io.File(
                "E:/4k/github-projects/MCWorldAgent/java/agent/registry/fingerprints/b1.6.json").toPath()),
                "UTF-8"))).get("hooks"));
        ok(hooks.size() == 4, "hooks 4 got " + hooks.size());
        HookTargets.activate("b1.6");
        String[][] hookTriples = {
            {"SAVE_WORLD", HookTargets.SAVE_WORLD, "fb"},
            {"LOAD_WORLD", HookTargets.LOAD_WORLD, "net/minecraft/client/Minecraft"},
            {"RUN_TICK", HookTargets.RUN_TICK, "net/minecraft/client/Minecraft"},
            {"HUD_RENDER", HookTargets.HUD_RENDER, "uk"},
        };
        for (int i = 0; i < hookTriples.length; i++) {
            Map h = A2TableTest.obj(hooks.get(hookTriples[i][0]));
            ok(h != null, "anchor hook " + hookTriples[i][0]);
            HookTargets.Target tg = null;
            HookTargets.Target[] all = HookTargets.forClass(hookTriples[i][2]);
            for (int k = 0; k < all.length; k++) {
                if (hookTriples[i][1].equals(all[k].hookId)) { tg = all[k]; }
            }
            ok(tg != null, "hookSpecs has " + hookTriples[i][1]);
            ok(A2TableTest.str(h.get("class")).equals(tg.className), tg.hookId + " class");
            ok(A2TableTest.str(h.get("method")).equals(tg.methodName), tg.hookId + " method");
            ok(A2TableTest.str(h.get("desc")).equals(tg.desc), tg.hookId + " desc");
            ok(Integer.parseInt(A2TableTest.str(h.get("access")).substring(2), 16) == tg.access,
                    tg.hookId + " access 0x0001");
            ok(((Double) h.get("expectedHits")).intValue() == tg.expectedHits, tg.hookId + " hits");
            String mode = A2TableTest.str(h.get("injectMode"));
            ok("ENTRY".equals(mode) ? tg.injectMode == HookTargets.ENTRY
                            : "EXIT".equals(mode) ? tg.injectMode == HookTargets.EXIT
                            : false,
                    tg.hookId + " mode " + mode);
        }
        ok("k".equals(A2TableTest.str(A2TableTest.obj(hooks.get("RUN_TICK")).get("method"))),
                "RUN_TICK 字母 k（1401/1501=j 断代——j→k 翻面下界=b1.6）");
        ok(hooks.get("WORLD_CTOR") == null, "no WORLD_CTOR hook");
        ok(HookTargets.forClass("fb").length == 1, "fb 单钩（仅 SAVE_WORLD）");

        // ---- mob 白名单 9 项对榫（giant=np 已到世——判决③翻面）----
        Map mobJson = A2TableTest.obj(ops.get("mobClasses"));
        ok(t.mobClasses.size() == 9 && mobJson.size() == 9, "mobClasses 9");
        String[][] mobs = {
            {"pig", "vz"}, {"sheep", "dj"}, {"creeper", "fz"}, {"skeleton", "fp"},
            {"zombie", "ur"}, {"spider", "cl"}, {"tnt", "qr"}, {"giant", "np"}, {"mob", "if"},
        };
        for (int i = 0; i < mobs.length; i++) {
            ok(mobs[i][1].equals(t.mobClasses.get(mobs[i][0])), "mob " + mobs[i][0]);
            ok(mobs[i][1].equals(A2TableTest.str(mobJson.get(mobs[i][0]))), "mob json " + mobs[i][0]);
        }
        ok(t.mobClasses.get("giant") != null, "giant=np 已到世（1401 null/1501=mx 断代链）");
        ok("if".equals(t.mobClasses.get("mob")) && "qr".equals(t.mobClasses.get("tnt")),
                "if=EntityCreature public 非 abstract/qr=TNT（16 np↔1401 np=TNT 撞车位）");

        // ---- capabilities：39 OK / 3 UNSUPPORTED（grow/genscatter/invulnerable——同 1501）----
        Map caps = A2TableTest.obj(A2TableTest.obj(A2TableTest.parseJson(new String(
                java.nio.file.Files.readAllBytes(new java.io.File(
                "E:/4k/github-projects/MCWorldAgent/java/agent/registry/fingerprints/b1.6.json").toPath()),
                "UTF-8"))).get("capabilities"));
        int okN = 0;
        int unN = 0;
        for (Object v : caps.values()) {
            String s = A2TableTest.str(v);
            if ("OK".equals(s)) { okN++; }
            else if ("UNSUPPORTED".equals(s)) { unN++; }
            else { throw new RuntimeException("bad cap value " + s); }
        }
        ok(okN == 39, "caps 39 OK got " + okN);
        ok(unN == 3, "caps 3 UNSUPPORTED got " + unN);
        String[][] unPairs = {
            {"grow", "idCrops"}, {"genscatter", "scatterClassG"}, {"invulnerable", "iframesF"},
        };
        for (int i = 0; i < unPairs.length; i++) {
            ok("UNSUPPORTED".equals(A2TableTest.str(caps.get(unPairs[i][0]))), "un " + unPairs[i][0]);
            ok(A2TableTest.slot(t, unPairs[i][1]) == null, unPairs[i][0] + " slot null");
        }
        String[][] okPairs = {
            {"place", "setBlockM"}, {"dig", "setBlockM"}, {"getblock", "getBlockM"},
            {"getblocks", "getBlockM"}, {"fill", "setBlockM"}, {"replace", "getBlockM"},
            {"save", "saveWorldM"}, {"give", "addStackM"}, {"inv", "invF"},
            {"setarmor", "addStackM"}, {"clearinv", "invMainF"}, {"entitylist", "entityListM"},
            {"playsound", "playSoundM"}, {"getmeta", "getMetaM"}, {"setmeta", "setMetaSilentM"},
            {"getlight", "getLightM"}, {"getinfo", "difficultyF"},
            {"settime", "worldTimeF"}, {"setspawn", "spawnXF"},
            {"tp", "setPosRotM"}, {"setlook", "yawF"}, {"setmotion", "motXF"},
            {"getplayer", "healthF"}, {"sethealth", "healthF"},
            {"spawn", "spawnEntityM"}, {"drop", "spawnEntityM"}, {"explode", "explodeM"},
            {"clear", "setEntityDeadM"}, {"kill", "setEntityDeadM"}, {"clone", "setBlockM"},
            {"setdifficulty", "difficultyF"}, {"sleep", "sleepInBedM"},
            {"getchest", "invSizeM"}, {"setchest", "invSizeM"},
            {"getfurnace", "teBurnF"}, {"setfurnace", "teBurnF"},
            {"weather", "rainStrF"}, {"getbiome", "biomeWcmM"},
        };
        ok(okPairs.length == 38, "38 slot-backed OK pairs");
        for (int i = 0; i < okPairs.length; i++) {
            ok("OK".equals(A2TableTest.str(caps.get(okPairs[i][0]))), "OK " + okPairs[i][0]);
            ok(A2TableTest.slot(t, okPairs[i][1]) != null, okPairs[i][0] + " slot non-null");
        }
        ok("OK".equals(A2TableTest.str(caps.get("listops"))), "listops OK");

        // ---- null 槽断言（9 件，JSON 显式 null）----
        String[] nullSlots = {
            "light6M", "observersF", "scatterClassG", "entityTypeM", "idCrops",
            "idSapling", "cropsRipeMeta", "playerF", "iframesF",
        };
        for (int i = 0; i < nullSlots.length; i++) {
            ok(A2TableTest.slot(t, nullSlots[i]) == null, nullSlots[i] + " null");
        }

        // ---- 假件实形访问位复核（钉表纪律：真 jar 为准，防 sister tiny 借表假绿）----
        // 1) iw.d=itemDamage private + int 参构造仅 (III)V 无 (II)V
        ok(Modifier.isPrivate(Class.forName("iw").getDeclaredField("d").getModifiers()), "iw.d private");
        boolean noCtor2 = false;
        try { Class.forName("iw").getConstructor(int.class, int.class); }
        catch (NoSuchMethodException e) { noCtor2 = true; }
        ok(noCtor2, "iw no (II)V");
        Class.forName("iw").getConstructor(int.class, int.class, int.class);
        checks++;
        // 2) eg 五槽全 private + 天气键 l/n boolean private + setter b/a public (Z)V（capWeather OK 面）
        String[] egPrivs = {"a", "b", "c", "d", "e"};
        for (int i = 0; i < egPrivs.length; i++) {
            ok(Modifier.isPrivate(Class.forName("eg").getDeclaredField(egPrivs[i]).getModifiers()),
                    "eg." + egPrivs[i] + " private");
        }
        ok(Modifier.isPrivate(Class.forName("eg").getDeclaredField("l").getModifiers())
                && Class.forName("eg").getDeclaredField("l").getType() == boolean.class,
                "eg.l private boolean raining");
        ok(Modifier.isPrivate(Class.forName("eg").getDeclaredField("n").getModifiers())
                && Class.forName("eg").getDeclaredField("n").getType() == boolean.class,
                "eg.n private boolean thundering");
        ok(Modifier.isPublic(Class.forName("eg").getMethod("b", boolean.class).getModifiers()),
                "eg.b(Z)V public setRaining");
        ok(Modifier.isPublic(Class.forName("eg").getMethod("a", boolean.class).getModifiers()),
                "eg.a(Z)V public setThundering");
        // 3) gq 睡眠三槽真 jar 访问位：u PROTECTED boolean / b private int / a private bp
        //    （⚠️sister tiny u/a/v 错位件——假绿防线：a 必须是 bp 型且 private）
        Field gqu = Class.forName("gq").getDeclaredField("u");
        ok(Modifier.isProtected(gqu.getModifiers()) && gqu.getType() == boolean.class,
                "gq.u protected boolean sleeping（真 jar 访问位）");
        Field gqb = Class.forName("gq").getDeclaredField("b");
        ok(Modifier.isPrivate(gqb.getModifiers()) && gqb.getType() == int.class,
                "gq.b private int sleepTimer（tiny 错位件 a 勿借）");
        Field gqa = Class.forName("gq").getDeclaredField("a");
        ok(Modifier.isPrivate(gqa.getModifiers()) && gqa.getType() == Class.forName("bp"),
                "gq.a private bp bedChunkCoordinates（tiny 错位件 v 勿借）");
        // 4) si 真 jar 字段面：aH=fb worldObj、aL=posX double（tiny aI/aM 错位防线）
        ok(Class.forName("si").getField("aH").getType() == Class.forName("fb"),
                "si.aH public fb worldObj（⚠️非 tiny aI）");
        ok(Class.forName("si").getField("aL").getType() == double.class,
                "si.aL public double posX（⚠️非 tiny aM）");
        ok(Class.forName("si").getDeclaredMethod("W").getReturnType() == boolean.class,
                "si.W()Z isEntityAlive（1501 同字母）");
        // 5) fb 天气浮值=protected float j/l（判决①翻面实体；prev=i/k 同 protected）
        Field fbf = Class.forName("fb").getDeclaredField("l");
        ok(Modifier.isProtected(fbf.getModifiers()) && fbf.getType() == float.class,
                "fb.l protected float thunderStrength");
        ok(Class.forName("fb").getDeclaredField("j").getType() == float.class,
                "fb.j protected float rainStrength（fb.E()V 语义件）");
        // 6) fb 同字母异描述符共存：f(IIII)Z setBlock；e(III)I+e(IIII)Z 共存；a(III)I+a()Lxm 共存
        ok(Class.forName("fb").getMethod("f", int.class, int.class, int.class, int.class)
                .getReturnType() == boolean.class, "fb.f(IIII)Z setBlockWithNotify");
        ok(Class.forName("fb").getMethod("e", int.class, int.class, int.class)
                .getReturnType() == int.class, "fb.e(III)I getMeta");
        ok(Class.forName("fb").getMethod("e", int.class, int.class, int.class, int.class)
                .getReturnType() == boolean.class, "fb.e(IIII)Z setMetaSilent 返回 boolean（1501 V 断代回摆）");
        ok(Class.forName("fb").getMethod("a").getReturnType() == Class.forName("xm"),
                "fb.a()Lxm; WCM 零参与 a(III)I 共存");
        ok(Class.forName("fb").getMethod("n", int.class, int.class, int.class)
                .getReturnType() == int.class, "fb.n(III)I getLight");
        ok(Class.forName("fb").getMethod("x").getReturnType() == Class.forName("eg"), "fb.x()Leg;");
        ok(Class.forName("fb").getMethod("y").getReturnType() == void.class, "fb.y()V 旗标刷新");
        ok(Class.forName("fb").getMethod("f").getReturnType() == boolean.class, "fb.f()Z isDaytime");
        ok(Class.forName("fb").getField("B").getType() == boolean.class, "fb.B public boolean multiplayerWorld");
        ok(Class.forName("fb").getField("q").getType() == int.class, "fb.q public int difficulty");
        Field fbT = Class.forName("fb").getField("t");
        ok(Modifier.isFinal(fbT.getModifiers()) && fbT.getType() == Class.forName("wr"), "fb.t public final wr");
        ok(Class.forName("wr").getField("c").getType() == boolean.class, "wr.c public boolean disableSleep");
        ok(Class.forName("fb").getDeclaredMethod("a", boolean.class, Class.forName("xs"))
                .getReturnType() == void.class, "fb.a(ZLxs;)V declared");
        ok(Class.forName("fb").getMethod("b", int.class, int.class, int.class)
                .getReturnType() == Class.forName("os"), "fb.b(III)Los; teGet");
        ok(Class.forName("fb").getMethod("b", Class.forName("si")).getReturnType() == boolean.class,
                "fb.b(Lsi;)Z entityJoinedWorld（回摆 1501 字母）");
        ok(Class.forName("fb").getMethod("e", Class.forName("si")).getReturnType() == void.class,
                "fb.e(Lsi;)V setEntityDead");
        ok(Class.forName("fb").getMethod("o").getReturnType() != void.class, "fb.o()List getLoadedEntityList");
        ok(Class.forName("fb").getMethod("l").getReturnType() == void.class, "fb.l()V tick 三版同字母");
        // 7) cu 真 enum + a public static cu
        ok(Class.forName("cu").isEnum(), "cu isEnum");
        Field cuA = Class.forName("cu").getField("a");
        ok(Modifier.isStatic(cuA.getModifiers()) && cuA.getType() == Class.forName("cu"), "cu.a public static cu");
        // 8) bp (III)V + a public int；lo Y public int；gq c public iu
        Class.forName("bp").getConstructor(int.class, int.class, int.class);
        checks++;
        ok(Class.forName("bp").getField("a").getType() == int.class, "bp.a public int");
        ok(Class.forName("lo").getField("Y").getType() == int.class, "lo.Y public int health");
        ok(Class.forName("gq").getField("c").getType() == Class.forName("iu"), "gq.c public iu inventory");
        // 9) 睡眠方法形：gq.b(III)Lcu;（isEnum 门）+ gq.a(ZZZ)V + N()Z + O()Z
        ok(Class.forName("gq").getDeclaredMethod("b", int.class, int.class, int.class)
                .getReturnType() == Class.forName("cu"), "gq.b(III)Lcu;");
        ok(Class.forName("gq").getDeclaredMethod("a", boolean.class, boolean.class, boolean.class)
                .getReturnType() == void.class, "gq.a(ZZZ)V");
        ok(Class.forName("gq").getDeclaredMethod("N").getReturnType() == boolean.class, "gq.N()Z");
        ok(Class.forName("gq").getDeclaredMethod("O").getReturnType() == boolean.class, "gq.O()Z fullyAsleep");
        // 10) ls 接口三方法形（d_ 取件面）+ iu implements ls
        ok(Class.forName("ls").isInterface(), "ls interface");
        ok(Class.forName("ls").getMethod("d_", int.class).getReturnType() == Class.forName("iw"), "ls.d_(I)Liw;");
        ok(Class.forName("ls").isAssignableFrom(Class.forName("iu")), "iu implements ls");
        // 11) gk.c public static gk[] + protected ctor；iz.b static String(si)
        Field gkc = Class.forName("gk").getField("c");
        ok(Modifier.isStatic(gkc.getModifiers()) && gkc.getType().isArray()
                && gkc.getType().getComponentType() == Class.forName("gk"), "gk.c public static gk[]");
        Class.forName("gk").getDeclaredConstructor(int.class);
        checks++;
        java.lang.reflect.Method izB = Class.forName("iz").getDeclaredMethod("b", Class.forName("si"));
        ok(Modifier.isStatic(izB.getModifiers()) && izB.getReturnType() == String.class,
                "iz.b static String(si)（查表字母 b 三版稳定）");
        // 12) 群系链件在位（判决②翻面树侧自证——与 1401 不建 tq 对照）
        ok(Class.forName("xm").getMethod("a", int.class, int.class)
                .getReturnType() == Class.forName("jz"), "xm.a(II)Ljz; getBiomeGenAt");
        ok(Class.forName("jz").getField("n").getType() == String.class
                && Modifier.isPublic(Class.forName("jz").getField("n").getModifiers()),
                "jz.n public String biomeName（biomeAt getField 消费面）");
        // 13) TE 面：jo.a private iw[]；sf public a/c + h private + implements ls
        Field joA = Class.forName("jo").getDeclaredField("a");
        ok(Modifier.isPrivate(joA.getModifiers()) && joA.getType().isArray()
                && joA.getType().getComponentType() == Class.forName("iw"), "jo.a private iw[]");
        ok(Class.forName("jo").getSuperclass() == Class.forName("os"), "jo extends os");
        ok(Class.forName("ls").isAssignableFrom(Class.forName("sf")), "sf implements ls");
        ok(Class.forName("sf").getField("a").getType() == int.class, "sf.a public int burn");
        ok(Class.forName("sf").getField("c").getType() == int.class, "sf.c public int cook");
        ok(Modifier.isPrivate(Class.forName("sf").getDeclaredField("h").getModifiers()), "sf.h private iw[]");
        // 14) 继承链身份（si←lo←gq；hj extends si；sf/jo extends os）
        ok(Class.forName("gq").getSuperclass() == Class.forName("lo"), "gq extends lo");
        ok(Class.forName("lo").getSuperclass() == Class.forName("si"), "lo extends si");
        ok(Class.forName("hj").getSuperclass() == Class.forName("si"), "hj=EntityItem extends si");
        ok(Class.forName("iw").getField("c").getType() == int.class, "iw.c public int itemId");

        System.out.println("A2Table16Test PASS (" + checks + " checks)");
    }
}
