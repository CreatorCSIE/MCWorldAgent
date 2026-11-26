// b1.5_01 A2 表对榫（JSON-only 版：无内建表，无头窗批 2026-11-27）。
// 锚库 b1.5_01.json 为 mca-fingerprint-v1 档案 schema（null 字面量/三层嵌套/超
// MiniJson 4096 限=FingerprintLoader 静默跳过，详注见本文件末尾 note 存档），
// 故本测试自带测试侧转写注入 injectTable()：ops→VersionTable 槽、嵌套 hooks→
// class|name|desc|access|hits|mode 六元组、ops.mobClasses→t.mobClasses、
// BY_ID/BY_SHA1/active 反射注入（与 FingerprintLoader.buildOrThrow 同配方）。
// ⚠️ 数据补钉一处：ops 原缺 capWeather 槽——JSON note「②天气引入首例版」+
// capabilities weather→OK+wiSetRainM/wiSetThunderM 已钉=门旗自证，本批补钉
// "capWeather": true（ops 77 键含 mobClasses，76 槽对榫）。
package com.mcworldagent.agent;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Map;

public class A2Table1501Test {
    static int checks = 0;
    static void ok(boolean c, String n) {
        checks++;
        if (!c) { System.out.println("FAIL: " + n); throw new RuntimeException("check failed: " + n); }
    }

    @SuppressWarnings("unchecked")
    static VersionTable injectTable() throws Exception {
        byte[] buf = java.nio.file.Files.readAllBytes(new java.io.File(
                "E:/4k/github-projects/MCWorldAgent/java/agent/registry/fingerprints/b1.5_01.json").toPath());
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
        VersionTable t = injectTable();
        // ---- 身份三锚 ----
        VersionTable bySha = VersionRegistry.byJarSha1("e2a692e5e8160c84b29c834ecbf398618db9749c");
        ok(bySha != null && "b1.5_01".equals(bySha.versionId), "byJarSha1 main");
        ok(t == bySha, "inject==bySha same table");
        ok(VersionRegistry.activate("b1.5_01"), "activate");
        ok(VersionRegistry.current() == t, "current==injected");
        ok("G3".equals(t.family), "family G3");

        // ---- ops 全表逐槽对榫（77 键含 mobClasses；capSleep/capWeather 为 Boolean）----
        Map ops = A2TableTest.obj(A2TableTest.obj(A2TableTest.parseJson(new String(
                java.nio.file.Files.readAllBytes(new java.io.File(
                "E:/4k/github-projects/MCWorldAgent/java/agent/registry/fingerprints/b1.5_01.json").toPath()),
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

        // ---- 断代面重点复核（vs 173/181 漂移项，JSON note 钉值）----
        ok("x".equals(t.getWorldInfoM), "getWorldInfoM x（dy=WorldInfo 首例架构确立版）");
        ok("e".equals(t.worldTimeF) && "a".equals(t.randSeedF), "worldTimeF e/randSeedF a（dy 字母同 173 ei）");
        ok("b".equals(t.spawnXF) && "c".equals(t.spawnYF) && "d".equals(t.spawnZF), "spawnXYZ b/c/d");
        ok("C".equals(t.rainStrF) && "E".equals(t.thunderStrF), "rainStrF C/thunderStrF E（note⑰ 判决）");
        ok("b".equals(t.wiSetRainM) && "a".equals(t.wiSetThunderM), "wiSet b/a（dy public setter）");
        ok("b".equals(t.sleepInBedM) && "a".equals(t.wakeUpM) && "a".equals(t.sleepOkF),
                "sleep 三槽 b/a/a（gh.b(III)Lco;/gh.a(ZZZ)V/co.a）");
        ok(Boolean.TRUE.equals(t.capSleep) && Boolean.TRUE.equals(t.capWeather), "capSleep/capWeather true");
        ok("d".equals(t.playerListF) && "rj".equals(t.entityClassG), "playerListF d/entityClassG rj");
        ok("aK".equals(t.posXF) && "aL".equals(t.posYF) && "aM".equals(t.posZF),
                "pos aK/aL/aM（⚠️1501 左移两位 vs 173 aM..aT，严禁借表）");
        ok("aN".equals(t.motXF) && "aO".equals(t.motYF) && "aP".equals(t.motZF), "mot aN/aO/aP");
        ok("aQ".equals(t.yawF) && "aR".equals(t.pitchF), "look aQ/aR");
        ok("W".equals(t.healthF), "healthF W（kw public int）");
        ok("f".equals(t.invF) && "a".equals(t.invMainF) && "b".equals(t.invArmorF) && "c".equals(t.invCurF),
                "inv 面 f/a/b/c（ih=InventoryPlayer）");
        ok("a".equals(t.isCountF) && "c".equals(t.isIdF) && "d".equals(t.isDmgF), "isCount a/isId c/isDmg d");
        ok("b".equals(t.teGetM), "teGetM b（TileEntity nx——iy/rg 基类）");
        ok("a".equals(t.invSizeM) && "d_".equals(t.invGetM) && "a".equals(t.invSetM),
                "la 三槽 a/d_/a（IInventory=la）");
        ok("a".equals(t.teBurnF) && "c".equals(t.teCookF), "teBurnF a/teCookF c（rg public int）");
        ok("gb".equals(t.itemClassG) && "c".equals(t.itemsF) && "gz".equals(t.itemEntityG),
                "gb.c 注册表/gz=EntityItem");
        ok("ik".equals(t.entityListClassG) && "b".equals(t.entityListLookupM), "ik.b（EntityList 断代 jc→ik）");
        ok("o".equals(t.entityListM) && "b".equals(t.spawnEntityM) && "e".equals(t.setEntityDeadM),
                "et o/b/e（entityList/spawn/setEntityDead）");
        ok("e".equals(t.setBlockM) && "a".equals(t.getBlockM) && "e".equals(t.getMetaM)
                && "d".equals(t.setMetaSilentM) && "l".equals(t.getLightM) && "l".equals(t.difficultyF),
                "et 方块面 e/a/e/d/l/l（setMetaSilent=d 同 173 形）");
        ok("a".equals(t.explodeM) && "a".equals(t.playSoundM) && "a".equals(t.addStackM), "a 三撞名槽留档");
        ok("a".equals(t.saveWorldM) && "(ZLwr;)V".equals(t.saveWorldDesc),
                "saveWorld a(ZLwr;)V 双参实体（无 1 参包装=断代，WorldOps 零参分支命中 a()Lwl 同形）");
        ok("b".equals(t.setPosRotM) && "(DDDFF)V".equals(t.setPosRotDesc), "setPosRot b(DDDFF)V");
        ok("a".equals(t.biomeWcmM) && "a".equals(t.biomeAtM) && "m".equals(t.biomeNameF),
                "biome 三锚 a/a/m（wl.a(II)Ljj;/jj.m）");
        ok("h".equals(t.hudPlayerF) && "p".equals(t.hudFontF) && "y".equals(t.hudSettingsF)
                && "B".equals(t.hudDebugGateF), "HUD 链 h/p/y/B（jz.B=showDebugInfo 非 173 showFPS）");

        // ---- hooks 四节对榫（HookTargets.activate 经 hookSpecs 六元组）----
        Map hooks = A2TableTest.obj(A2TableTest.obj(A2TableTest.parseJson(new String(
                java.nio.file.Files.readAllBytes(new java.io.File(
                "E:/4k/github-projects/MCWorldAgent/java/agent/registry/fingerprints/b1.5_01.json").toPath()),
                "UTF-8"))).get("hooks"));
        ok(hooks.size() == 4, "hooks 4 got " + hooks.size());
        HookTargets.activate("b1.5_01");
        String[][] hookTriples = {
            {"SAVE_WORLD", HookTargets.SAVE_WORLD, "et"},
            {"LOAD_WORLD", HookTargets.LOAD_WORLD, "net/minecraft/client/Minecraft"},
            {"RUN_TICK", HookTargets.RUN_TICK, "net/minecraft/client/Minecraft"},
            {"HUD_RENDER", HookTargets.HUD_RENDER, "tl"},
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
        ok(hooks.get("WORLD_CTOR") == null, "no WORLD_CTOR hook");
        ok(HookTargets.forClass("et").length == 1, "et 单钩（仅 SAVE_WORLD）");

        // ---- mob 白名单 9 项对榫（JSON ops.mobClasses 嵌套）----
        Map mobJson = A2TableTest.obj(ops.get("mobClasses"));
        ok(t.mobClasses.size() == 9 && mobJson.size() == 9, "mobClasses 9");
        String[][] mobs = {
            {"pig", "va"}, {"sheep", "dd"}, {"creeper", "fq"}, {"skeleton", "fg"},
            {"zombie", "ts"}, {"spider", "cg"}, {"tnt", "pw"}, {"giant", "mx"}, {"mob", "ht"},
        };
        for (int i = 0; i < mobs.length; i++) {
            ok(mobs[i][1].equals(t.mobClasses.get(mobs[i][0])), "mob " + mobs[i][0]);
            ok(mobs[i][1].equals(A2TableTest.str(mobJson.get(mobs[i][0]))), "mob json " + mobs[i][0]);
        }

        // ---- capabilities：39 OK / 3 UNSUPPORTED（grow/genscatter/invulnerable）----
        Map caps = A2TableTest.obj(A2TableTest.obj(A2TableTest.parseJson(new String(
                java.nio.file.Files.readAllBytes(new java.io.File(
                "E:/4k/github-projects/MCWorldAgent/java/agent/registry/fingerprints/b1.5_01.json").toPath()),
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
            {"clear", "setEntityDeadM"}, {"getbiome", "biomeWcmM"},
            {"weather", "rainStrF"}, {"kill", "setEntityDeadM"}, {"clone", "setBlockM"},
            {"setdifficulty", "difficultyF"}, {"sleep", "sleepInBedM"},
            {"getchest", "invSizeM"}, {"setchest", "invSizeM"},
            {"getfurnace", "teBurnF"}, {"setfurnace", "teBurnF"},
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

        // ---- 假件实形访问位复核（钉表纪律：private 就 private，防假绿）----
        // 1) ii.d=itemDamage private（setchest getDeclaredField 修复面）+ 仅 (III)V 无 (II)V
        ok(Modifier.isPrivate(Class.forName("ii").getDeclaredField("d").getModifiers()), "ii.d private");
        boolean noCtor2 = false;
        try { Class.forName("ii").getConstructor(int.class, int.class); }
        catch (NoSuchMethodException e) { noCtor2 = true; }
        ok(noCtor2, "ii no (II)V");
        Class.forName("ii").getConstructor(int.class, int.class, int.class);
        checks++;
        // 2) dy 五槽全 private（WorldInfo 分支前提）+ 旗标 l/n private
        String[] dyPrivs = {"a", "b", "c", "d", "e", "l", "n"};
        for (int i = 0; i < dyPrivs.length; i++) {
            ok(Modifier.isPrivate(Class.forName("dy").getDeclaredField(dyPrivs[i]).getModifiers()),
                    "dy." + dyPrivs[i] + " private");
        }
        // 3) gh 睡眠三形：x protected / b private / a private（173 v public 断代）
        ok(Modifier.isProtected(Class.forName("gh").getDeclaredField("x").getModifiers()), "gh.x protected");
        ok(Modifier.isPrivate(Class.forName("gh").getDeclaredField("b").getModifiers()), "gh.b private");
        ok(Modifier.isPrivate(Class.forName("gh").getDeclaredField("a").getModifiers()), "gh.a private");
        ok(Class.forName("gh").getDeclaredField("a").getType() == Class.forName("bk"), "gh.a:Lbk;");
        // 4) iy.a private ii[]（27 槽）；rg public int a/c + implements la
        Field iya = Class.forName("iy").getDeclaredField("a");
        ok(Modifier.isPrivate(iya.getModifiers()) && iya.getType().isArray()
                && iya.getType().getComponentType() == Class.forName("ii"), "iy.a private ii[]");
        ok(Class.forName("iy").getSuperclass() == Class.forName("nx"), "iy extends nx");
        ok(Class.forName("la").isAssignableFrom(Class.forName("iy")), "iy implements la");
        ok(Class.forName("rg").getField("a").getType() == int.class, "rg.a public int");
        ok(Class.forName("rg").getField("c").getType() == int.class, "rg.c public int");
        ok(Class.forName("la").isAssignableFrom(Class.forName("rg")), "rg implements la");
        ok(Modifier.isPrivate(Class.forName("rg").getDeclaredField("h").getModifiers()), "rg.h private ii[]");
        // 5) et 同字母异描述符共存：e(IIII)Z + e(III)I；l 字段 int + l(III)I 方法
        ok(Class.forName("et").getMethod("e", int.class, int.class, int.class, int.class)
                .getReturnType() == boolean.class, "et.e(IIII)Z setBlock");
        ok(Class.forName("et").getMethod("e", int.class, int.class, int.class)
                .getReturnType() == int.class, "et.e(III)I getMeta 共存");
        ok(Class.forName("et").getField("l").getType() == int.class, "et.l public int difficulty");
        ok(Class.forName("et").getMethod("l", int.class, int.class, int.class)
                .getReturnType() == int.class, "et.l(III)I getLight 共存");
        // 6) et.x()Ldy 返回型 + et.y()V public + et.f()Z public + et.v public boolean
        ok(Class.forName("et").getMethod("x").getReturnType() == Class.forName("dy"), "et.x()Ldy");
        ok(Class.forName("et").getMethod("y").getReturnType() == void.class, "et.y()V public");
        ok(Class.forName("et").getMethod("f").getReturnType() == boolean.class, "et.f()Z public");
        ok(Class.forName("et").getField("v").getType() == boolean.class, "et.v public boolean");
        ok(Class.forName("et").getField("o").getType() == Class.forName("vr"), "et.o public vr");
        ok(Class.forName("vr").getField("c").getType() == boolean.class, "vr.c public boolean");
        // 7) et C/E private float（dump javap -p 实锤——非 public，WorldOps getDeclaredField 配方）
        ok(Modifier.isPrivate(Class.forName("et").getDeclaredField("C").getModifiers())
                && Class.forName("et").getDeclaredField("C").getType() == float.class, "et.C private float");
        ok(Modifier.isPrivate(Class.forName("et").getDeclaredField("E").getModifiers()), "et.E private float");
        // 8) co 真 enum + a public static co（sleepOkF 引用相等面）
        ok(Class.forName("co").isEnum(), "co isEnum");
        Field coA = Class.forName("co").getField("a");
        ok(Modifier.isStatic(coA.getModifiers()) && coA.getType() == Class.forName("co"), "co.a public static co");
        // 9) bk (III)V public + 三 int public；rj aK public double + b(DDDFF)V public
        Class.forName("bk").getConstructor(int.class, int.class, int.class);
        checks++;
        ok(Class.forName("bk").getField("a").getType() == int.class, "bk.a public int");
        ok(Class.forName("rj").getField("aK").getType() == double.class, "rj.aK public double");
        ok(Class.forName("rj").getDeclaredMethod("b", double.class, double.class, double.class,
                float.class, float.class) != null, "rj.b(DDDFF)V");
        ok(Class.forName("kw").getField("W").getType() == int.class, "kw.W public int");
        ok(Class.forName("gh").getField("f").getType() == Class.forName("ih"), "gh.f public ih");
        // 10) 睡眠方法形：gh.b(III)Lco;（isEnum 门）+ gh.a(ZZZ)V + gh.O()Z
        ok(Class.forName("gh").getDeclaredMethod("b", int.class, int.class, int.class)
                .getReturnType() == Class.forName("co"), "gh.b(III)Lco;");
        ok(Class.forName("gh").getDeclaredMethod("a", boolean.class, boolean.class, boolean.class)
                .getReturnType() == void.class, "gh.a(ZZZ)V");
        ok(Class.forName("gh").getDeclaredMethod("O").getReturnType() == boolean.class, "gh.O()Z");
        // 11) saveWorld 双参实体声明面 + teGet 返回型 nx + gb.c public static gb[]
        ok(Class.forName("et").getDeclaredMethod("a", boolean.class, Class.forName("wr"))
                .getReturnType() == void.class, "et.a(ZLwr;)V declared");
        ok(Class.forName("et").getMethod("b", int.class, int.class, int.class)
                .getReturnType() == Class.forName("nx"), "et.b(III)Lnx; teGet");
        Field gbc = Class.forName("gb").getField("c");
        ok(Modifier.isStatic(gbc.getModifiers()) && gbc.getType().isArray()
                && gbc.getType().getComponentType() == Class.forName("gb"), "gb.c public static gb[]");
        Class.forName("gb").getDeclaredConstructor(int.class);
        checks++;
        // 12) biome 链可解析：et.a()Lwl / wl.a(II)Ljj / jj.m String
        ok(Class.forName("et").getMethod("a").getReturnType() == Class.forName("wl"), "fake et.a()Lwl;");
        ok(Class.forName("wl").getMethod("a", int.class, int.class).getReturnType()
                == Class.forName("jj"), "fake wl.a(II)Ljj;");
        ok(Class.forName("jj").getField("m").getType() == String.class, "fake jj.m String");
        // 13) la 接口三方法形（invSizeM/invGetM/invSetM 消费面）
        ok(Class.forName("la").isInterface(), "la interface");
        ok(Class.forName("la").getMethod("d_", int.class).getReturnType() == Class.forName("ii"), "la.d_(I)Lii;");
        // 14) 继承链身份（rj←kw←gh；iy/rg←nx；ih implements la）
        ok(Class.forName("gh").getSuperclass() == Class.forName("kw"), "gh extends kw");
        ok(Class.forName("kw").getSuperclass() == Class.forName("rj"), "kw extends rj");
        ok(Class.forName("la").isAssignableFrom(Class.forName("ih")), "ih implements la");

        System.out.println("A2Table1501Test PASS (" + checks + " checks)");
    }
}

// ---- provenance.note 存档（b1.5_01.json 权威 forensic 记录，防转写批误删）----
// b1.5_01 独立件（SHA1 对榫版本索引）。三大边界判决：① WorldInfo 架构确立版——World(et)
// 不再持 time/seed/spawn——dy 承载（et.x()Ldy；dy 字段面 a=randomSeed/b-d=spawnXYZ/
// e=worldTime 全 private，字母与 b1.7.3 ei 全同）；② 天气引入首例=World 四浮值 B/C+D/E
// +WorldInfo 持久化四键 l/m/n/o（NBT 键小写 raining/thundering）；getter 字母洗牌
// dy.o()Z→l/dy.m()Z→n、setter dy.b(Z)V→l/dy.a(Z)V→n 全 public——rainStrF=C/thunderStrF=E
// 维持正确（B/D=每 tick 先覆 prev 拷贝槽）；③ 存档面 saveWorld 无 1 参 public 包装——仅
// a(ZLwr;)V 双参实体（wr null 守卫两处）。字母断代 vs b1.7.3：World fd→et、Entity sn→rj、
// EntityLiving ls→kw、EntityPlayer gs→gh、InventoryPlayer ix→ih、ItemStack iz→ii、Item
// gm→gb、EntityList jc→ik、GuiIngame uq→tl、WorldInfo ei→dy、IProgressUpdate yb→wr、
// EntityItem hl→gz。pos 整体左移两位 aK..aR（173=aM..aT）。iframes=null（1501 rj.by=
// skinUrl 撞车位陷阱；伤害面=kw.Y/Z）。getbiome 三锚 wl.a(II)Ljj;+jj.m。钩四项
// SAVE_WORLD=et.a(ZLwr;)V、LOAD_WORLD=Minecraft.a(Let;Ljava/lang/String;Lgh;)V EXIT、
// RUN_TICK=Minecraft.j()V、HUD_RENDER=tl.a(FZII)V EXIT。TE/sleep 钉面：IInventory=la
// a/d_/a；rg public a/c+private ii[] h；iy private ii[] a（非 181 p）；gh protected x=
// sleeping/private b=sleepTimer；et.y()V public=updateAllPlayersSleepingFlag。
