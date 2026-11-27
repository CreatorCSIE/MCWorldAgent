// b1.4_01 A2 表对榫（JSON-only 版：无内建表，无头窗 V-2 批 2026-11-27）。
// 锚库 b1.4_01.json 为 mca-fingerprint-v1 档案 schema（>4096B=FingerprintLoader
// 静默跳过，运行时不消费=草稿锚纪律），故本测试自带测试侧转写注入 injectTable()
// （与 A2Table1501Test 同配方：ops→VersionTable 槽、嵌套 hooks→六元组、
// ops.mobClasses→t.mobClasses、BY_ID/BY_SHA1/active 反射注入）。
// 重点断代腿（vs 1501，JSON note 钉值）：getWorldInfoM=w 非 x；pos aJ..aQ 整体右移
// 一位；healthF=V；睡眠三槽 a/c/b 全 private（1501 x protected/b/a）；isEntityAlive=
// pb.U()Z（边界批 N()Z 勘误件）；setMetaSilent d(IIII)Z 返回 boolean（1501 V 断代）；
// entityJoinedWorld/spawn=a、setEntityDead=d、entityList=n（1501 b/e/o 全洗牌）；
// invGetM=c_（1501 d_）；hudDebugGateF=A（1501 B）；四大边界：weather/getbiome/
// invulnerable/giant 五 UNSUPPORTED 槽位 null。
package com.mcworldagent.agent;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Map;

public class A2Table1401Test {
    static int checks = 0;
    static void ok(boolean c, String n) {
        checks++;
        if (!c) { System.out.println("FAIL: " + n); throw new RuntimeException("check failed: " + n); }
    }

    @SuppressWarnings("unchecked")
    static VersionTable injectTable() throws Exception {
        byte[] buf = java.nio.file.Files.readAllBytes(new java.io.File(
                "E:/4k/github-projects/MCWorldAgent/java/agent/registry/fingerprints/b1.4_01.json").toPath());
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
        VersionTable bySha = VersionRegistry.byJarSha1("6f157f26955c35006c1afa8b0479e0ce785fb864");
        ok(bySha != null && "b1.4_01".equals(bySha.versionId), "byJarSha1 main");
        ok(t == bySha, "inject==bySha same table");
        ok(VersionRegistry.activate("b1.4_01"), "activate");
        ok(VersionRegistry.current() == t, "current==injected");
        ok("G3".equals(t.family), "family G3");

        // ---- ops 全表逐槽对榫（77 键含 mobClasses；capSleep/capWeather 为 Boolean）----
        Map ops = A2TableTest.obj(A2TableTest.obj(A2TableTest.parseJson(new String(
                java.nio.file.Files.readAllBytes(new java.io.File(
                "E:/4k/github-projects/MCWorldAgent/java/agent/registry/fingerprints/b1.4_01.json").toPath()),
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

        // ---- 断代面重点复核（vs 1501/173 漂移项，JSON note 钉值）----
        ok("w".equals(t.getWorldInfoM), "getWorldInfoM w（dj=WorldInfo 同架构；⚠️1501=x 同形陷阱）");
        ok("e".equals(t.worldTimeF) && "a".equals(t.randSeedF), "worldTimeF e/randSeedF a（dj 字母同形）");
        ok("b".equals(t.spawnXF) && "c".equals(t.spawnYF) && "d".equals(t.spawnZF), "spawnXYZ b/c/d");
        ok("aJ".equals(t.posXF) && "aK".equals(t.posYF) && "aL".equals(t.posZF),
                "pos aJ/aK/aL（+1/版规律 1301=aI..→1401=aJ..→1501=aK.. 严格成立）");
        ok("aM".equals(t.motXF) && "aN".equals(t.motYF) && "aO".equals(t.motZF), "mot aM/aN/aO");
        ok("aP".equals(t.yawF) && "aQ".equals(t.pitchF), "look aP/aQ");
        ok("V".equals(t.healthF), "healthF V（jm public int——1501=W 断代）");
        ok("pb".equals(t.entityClassG) && "d".equals(t.playerListF), "entityClassG pb/playerListF d");
        ok("f".equals(t.invF) && "a".equals(t.invMainF) && "b".equals(t.invArmorF) && "c".equals(t.invCurF),
                "inv 面 f/a/b/c（hh=InventoryPlayer 四字母全同 1501）");
        ok("a".equals(t.isCountF) && "c".equals(t.isIdF) && "d".equals(t.isDmgF), "isCount a/isId c/isDmg d");
        ok("b".equals(t.teGetM), "teGetM b（TileEntity=ma——1501=nx 断代）");
        ok("a".equals(t.invSizeM) && "c_".equals(t.invGetM) && "a".equals(t.invSetM),
                "jp 三槽 a/c_/a（⚠️取件 c_ 非 1501 d_——尾下划线链在案）");
        ok("a".equals(t.teBurnF) && "c".equals(t.teCookF), "teBurnF a/teCookF c（oy public int，同 1501 rg）");
        ok("fg".equals(t.itemClassG) && "c".equals(t.itemsF) && "gb".equals(t.itemEntityG),
                "fg.c 注册表/gb=EntityItem（⚠️1401 gb=EntityItem↔1501 gb=Item 撞车位）");
        ok("hj".equals(t.entityListClassG) && "b".equals(t.entityListLookupM), "hj.b（EntityList 查表）");
        ok("n".equals(t.entityListM) && "a".equals(t.spawnEntityM) && "d".equals(t.setEntityDeadM),
                "eb n/a/d（entityList/spawn/setEntityDead——1501 o/b/e 全洗牌）");
        ok("e".equals(t.setBlockM) && "a".equals(t.getBlockM) && "e".equals(t.getMetaM)
                && "d".equals(t.setMetaSilentM) && "l".equals(t.getLightM) && "j".equals(t.difficultyF),
                "eb 方块面 e/a/e/d/l/j（⚠️本代 d(IIII)Z 返回 boolean——1501 V 断代；difficultyF j 非 1501 l）");
        ok("a".equals(t.explodeM) && "a".equals(t.playSoundM) && "a".equals(t.addStackM), "a 三撞名槽留档");
        ok("a".equals(t.saveWorldM) && "(ZLtw;)V".equals(t.saveWorldDesc),
                "saveWorld a(ZLtw;)V 双参实体（断代带延续：无 1 参包装；a(Z)Z=TickUpdates 禁钩预避）");
        ok("b".equals(t.setPosRotM) && "(DDDFF)V".equals(t.setPosRotDesc), "setPosRot b(DDDFF)V 同字母");
        ok("b".equals(t.sleepInBedM) && "a".equals(t.wakeUpM) && "a".equals(t.sleepOkF),
                "sleep 三槽 b/a/a（fm.b(III)Lcb;/fm.a(ZZZ)V/cb.a——字母同 1501 形，槽面全洗牌）");
        ok(Boolean.TRUE.equals(t.capSleep) && Boolean.FALSE.equals(t.capWeather),
                "capSleep true/capWeather false（判决①天气下界负半轴）");
        ok("h".equals(t.hudPlayerF) && "p".equals(t.hudFontF) && "y".equals(t.hudSettingsF)
                && "A".equals(t.hudDebugGateF), "HUD 链 h/p/y/A（qv/is.A=showDebugInfo——1501 tl/B 断代翻面）");

        // ---- hooks 四节对榫（HookTargets.activate 经 hookSpecs 六元组）----
        Map hooks = A2TableTest.obj(A2TableTest.obj(A2TableTest.parseJson(new String(
                java.nio.file.Files.readAllBytes(new java.io.File(
                "E:/4k/github-projects/MCWorldAgent/java/agent/registry/fingerprints/b1.4_01.json").toPath()),
                "UTF-8"))).get("hooks"));
        ok(hooks.size() == 4, "hooks 4 got " + hooks.size());
        HookTargets.activate("b1.4_01");
        String[][] hookTriples = {
            {"SAVE_WORLD", HookTargets.SAVE_WORLD, "eb"},
            {"LOAD_WORLD", HookTargets.LOAD_WORLD, "net/minecraft/client/Minecraft"},
            {"RUN_TICK", HookTargets.RUN_TICK, "net/minecraft/client/Minecraft"},
            {"HUD_RENDER", HookTargets.HUD_RENDER, "qv"},
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
        ok(HookTargets.forClass("eb").length == 1, "eb 单钩（仅 SAVE_WORLD）");

        // ---- mob 白名单 9 项对榫（JSON ops.mobClasses 嵌套；giant=null 判决③）----
        Map mobJson = A2TableTest.obj(ops.get("mobClasses"));
        ok(t.mobClasses.size() == 9 && mobJson.size() == 9, "mobClasses 9");
        String[][] mobs = {
            {"pig", "si"}, {"sheep", "cq"}, {"creeper", "ex"}, {"skeleton", "eo"},
            {"zombie", "re"}, {"spider", "bt"}, {"tnt", "np"}, {"mob", "gv"},
        };
        for (int i = 0; i < mobs.length; i++) {
            ok(mobs[i][1].equals(t.mobClasses.get(mobs[i][0])), "mob " + mobs[i][0]);
            ok(mobs[i][1].equals(A2TableTest.str(mobJson.get(mobs[i][0]))), "mob json " + mobs[i][0]);
        }
        ok(t.mobClasses.get("giant") == null && mobJson.get("giant") == null,
                "mob giant null（判决③：tiny 零 Giant 命中，未到世）");
        ok("gv".equals(t.mobClasses.get("mob")) && "np".equals(t.mobClasses.get("tnt")),
                "gv=EntityCreature public 非 abstract（181 abstract 断代未到）/np=TNT ctor 同 1501 型");

        // ---- capabilities：37 OK / 5 UNSUPPORTED（grow/genscatter/getbiome/invulnerable/weather）----
        Map caps = A2TableTest.obj(A2TableTest.obj(A2TableTest.parseJson(new String(
                java.nio.file.Files.readAllBytes(new java.io.File(
                "E:/4k/github-projects/MCWorldAgent/java/agent/registry/fingerprints/b1.4_01.json").toPath()),
                "UTF-8"))).get("capabilities"));
        int okN = 0;
        int unN = 0;
        for (Object v : caps.values()) {
            String s = A2TableTest.str(v);
            if ("OK".equals(s)) { okN++; }
            else if ("UNSUPPORTED".equals(s)) { unN++; }
            else { throw new RuntimeException("bad cap value " + s); }
        }
        ok(okN == 37, "caps 37 OK got " + okN);
        ok(unN == 5, "caps 5 UNSUPPORTED got " + unN);
        String[][] unPairs = {
            {"grow", "idCrops"}, {"genscatter", "scatterClassG"}, {"invulnerable", "iframesF"},
            {"weather", "rainStrF"}, {"getbiome", "biomeWcmM"},
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
        };
        ok(okPairs.length == 36, "36 slot-backed OK pairs");
        for (int i = 0; i < okPairs.length; i++) {
            ok("OK".equals(A2TableTest.str(caps.get(okPairs[i][0]))), "OK " + okPairs[i][0]);
            ok(A2TableTest.slot(t, okPairs[i][1]) != null, okPairs[i][0] + " slot non-null");
        }
        ok("OK".equals(A2TableTest.str(caps.get("listops"))), "listops OK");

        // ---- null 槽断言（16 件，JSON 显式 null）----
        String[] nullSlots = {
            "light6M", "observersF", "scatterClassG", "entityTypeM", "idCrops",
            "idSapling", "cropsRipeMeta", "playerF", "iframesF",
            "rainStrF", "thunderStrF", "wiSetRainM", "wiSetThunderM",
            "biomeWcmM", "biomeAtM", "biomeNameF",
        };
        for (int i = 0; i < nullSlots.length; i++) {
            ok(A2TableTest.slot(t, nullSlots[i]) == null, nullSlots[i] + " null");
        }

        // ---- 假件实形访问位复核（钉表纪律：private 就 private，防假绿）----
        // 1) hi.d=itemDamage private + int 参构造仅 (III)V 无 (II)V
        ok(Modifier.isPrivate(Class.forName("hi").getDeclaredField("d").getModifiers()), "hi.d private");
        boolean noCtor2 = false;
        try { Class.forName("hi").getConstructor(int.class, int.class); }
        catch (NoSuchMethodException e) { noCtor2 = true; }
        ok(noCtor2, "hi no (II)V");
        Class.forName("hi").getConstructor(int.class, int.class, int.class);
        checks++;
        // 2) dj 五槽全 private（WorldInfo 分支前提）+ 判决①：dj 无任何 boolean 字段（天气键零命中）
        String[] djPrivs = {"a", "b", "c", "d", "e"};
        for (int i = 0; i < djPrivs.length; i++) {
            ok(Modifier.isPrivate(Class.forName("dj").getDeclaredField(djPrivs[i]).getModifiers()),
                    "dj." + djPrivs[i] + " private");
        }
        for (Field f : Class.forName("dj").getDeclaredFields()) {
            ok(f.getType() != boolean.class, "dj no boolean field（判决①）");
        }
        // 3) fm 睡眠三槽全 private 无 protected（1501 x protected 断代）：a:Z/c:I/b:Lay;
        ok(Modifier.isPrivate(Class.forName("fm").getDeclaredField("a").getModifiers())
                && Class.forName("fm").getDeclaredField("a").getType() == boolean.class,
                "fm.a private boolean sleeping");
        ok(Modifier.isPrivate(Class.forName("fm").getDeclaredField("c").getModifiers())
                && Class.forName("fm").getDeclaredField("c").getType() == int.class,
                "fm.c private int sleepTimer");
        Field fmb = Class.forName("fm").getDeclaredField("b");
        ok(Modifier.isPrivate(fmb.getModifiers()) && fmb.getType() == Class.forName("ay"),
                "fm.b private ay bedChunkCoordinates");
        // 4) hx.a private hi[]（27 槽）；oy public int a/c + implements jp
        Field hxa = Class.forName("hx").getDeclaredField("a");
        ok(Modifier.isPrivate(hxa.getModifiers()) && hxa.getType().isArray()
                && hxa.getType().getComponentType() == Class.forName("hi"), "hx.a private hi[]");
        ok(Class.forName("hx").getSuperclass() == Class.forName("ma"), "hx extends ma");
        ok(Class.forName("jp").isAssignableFrom(Class.forName("hx")), "hx implements jp");
        ok(Class.forName("oy").getField("a").getType() == int.class, "oy.a public int");
        ok(Class.forName("oy").getField("c").getType() == int.class, "oy.c public int");
        ok(Class.forName("jp").isAssignableFrom(Class.forName("oy")), "oy implements jp");
        ok(Modifier.isPrivate(Class.forName("oy").getDeclaredField("h").getModifiers()), "oy.h private hi[]");
        // 5) eb 同字母异描述符共存 + 断代账：e(IIII)Z + e(III)I；d(IIII)Z 返回 boolean（1501 V）；
        //    j 字段 int + l()V/l(III)I 共存
        ok(Class.forName("eb").getMethod("e", int.class, int.class, int.class, int.class)
                .getReturnType() == boolean.class, "eb.e(IIII)Z setBlock");
        ok(Class.forName("eb").getMethod("e", int.class, int.class, int.class)
                .getReturnType() == int.class, "eb.e(III)I getMeta 共存");
        ok(Class.forName("eb").getMethod("d", int.class, int.class, int.class, int.class)
                .getReturnType() == boolean.class, "eb.d(IIII)Z setMetaSilent 返回 boolean（断代账）");
        ok(Class.forName("eb").getField("j").getType() == int.class, "eb.j public int difficulty");
        ok(Class.forName("eb").getMethod("l", int.class, int.class, int.class)
                .getReturnType() == int.class, "eb.l(III)I getLight 共存");
        ok(Class.forName("eb").getMethod("l").getReturnType() == void.class, "eb.l()V tick 共存");
        // 6) eb.w()Ldj 返回型 + eb.x()V public + eb.f()Z public + eb.t public boolean +
        //    eb.m public final sy + sy.c public boolean
        ok(Class.forName("eb").getMethod("w").getReturnType() == Class.forName("dj"), "eb.w()Ldj;");
        ok(Class.forName("eb").getMethod("x").getReturnType() == void.class, "eb.x()V public 旗标刷新");
        ok(Class.forName("eb").getMethod("f").getReturnType() == boolean.class, "eb.f()Z public");
        ok(Class.forName("eb").getField("t").getType() == boolean.class, "eb.t public boolean multiplayerWorld");
        Field ebM = Class.forName("eb").getField("m");
        ok(Modifier.isFinal(ebM.getModifiers()) && ebM.getType() == Class.forName("sy"), "eb.m public final sy");
        ok(Class.forName("sy").getField("c").getType() == boolean.class, "sy.c public boolean");
        // 7) 判决①负半轴 World 侧：eb 无任何 float 字段（B/C/D/E 雨强四槽零命中）
        for (Field f : Class.forName("eb").getDeclaredFields()) {
            ok(f.getType() != float.class, "eb no float field（判决①）");
        }
        // 8) cb 真 enum + a public static cb（sleepOkF 引用相等面）
        ok(Class.forName("cb").isEnum(), "cb isEnum");
        Field cbA = Class.forName("cb").getField("a");
        ok(Modifier.isStatic(cbA.getModifiers()) && cbA.getType() == Class.forName("cb"), "cb.a public static cb");
        // 9) ay (III)V public + 三 int public；pb aJ public double + b(DDDFF)V public；
        //    jm V public int；fm f public hh
        Class.forName("ay").getConstructor(int.class, int.class, int.class);
        checks++;
        ok(Class.forName("ay").getField("a").getType() == int.class, "ay.a public int");
        ok(Class.forName("pb").getField("aJ").getType() == double.class, "pb.aJ public double");
        ok(Class.forName("pb").getDeclaredMethod("b", double.class, double.class, double.class,
                float.class, float.class) != null, "pb.b(DDDFF)V");
        ok(Class.forName("jm").getField("V").getType() == int.class, "jm.V public int health");
        ok(Class.forName("fm").getField("f").getType() == Class.forName("hh"), "fm.f public hh");
        // 10) 睡眠方法形：fm.b(III)Lcb;（isEnum 门）+ fm.a(ZZZ)V + fm.M()Z + fm.N()Z（勘误件）
        ok(Class.forName("fm").getDeclaredMethod("b", int.class, int.class, int.class)
                .getReturnType() == Class.forName("cb"), "fm.b(III)Lcb;");
        ok(Class.forName("fm").getDeclaredMethod("a", boolean.class, boolean.class, boolean.class)
                .getReturnType() == void.class, "fm.a(ZZZ)V");
        ok(Class.forName("fm").getDeclaredMethod("M").getReturnType() == boolean.class, "fm.M()Z");
        ok(Class.forName("fm").getDeclaredMethod("N").getReturnType() == boolean.class, "fm.N()Z isPlayerFullyAsleep");
        ok(Class.forName("pb").getDeclaredMethod("U").getReturnType() == boolean.class, "pb.U()Z isEntityAlive");
        // 11) saveWorld 双参实体声明面 + teGet 返回型 ma + fg.c public static fg[]
        ok(Class.forName("eb").getDeclaredMethod("a", boolean.class, Class.forName("tw"))
                .getReturnType() == void.class, "eb.a(ZLtw;)V declared");
        ok(Class.forName("eb").getMethod("b", int.class, int.class, int.class)
                .getReturnType() == Class.forName("ma"), "eb.b(III)Lma; teGet");
        Field fgc = Class.forName("fg").getField("c");
        ok(Modifier.isStatic(fgc.getModifiers()) && fgc.getType().isArray()
                && fgc.getType().getComponentType() == Class.forName("fg"), "fg.c public static fg[]");
        Class.forName("fg").getDeclaredConstructor(int.class);
        checks++;
        // 12) 判决②群系下界：假件树零 tq/jj 群系件（WorldChunkManager 不建=UNSUPPORTED 自证）
        boolean noTq = false;
        try { Class.forName("tq"); } catch (ClassNotFoundException e) { noTq = true; }
        ok(noTq, "no tq fake（判决②）");
        // 13) jp 接口三方法形（invSizeM/invGetM/invSetM 消费面）
        ok(Class.forName("jp").isInterface(), "jp interface");
        ok(Class.forName("jp").getMethod("c_", int.class).getReturnType() == Class.forName("hi"), "jp.c_(I)Lhi;");
        // 14) hj.b static String(pb)（entityListLookupM 判据面）
        java.lang.reflect.Method hjB = Class.forName("hj").getDeclaredMethod("b", Class.forName("pb"));
        ok(Modifier.isStatic(hjB.getModifiers()) && hjB.getReturnType() == String.class,
                "hj.b static String(pb)");
        checks++;
        // 15) 继承链身份（pb←jm←fm；hx/oy←ma；hh implements jp）
        ok(Class.forName("fm").getSuperclass() == Class.forName("jm"), "fm extends jm");
        ok(Class.forName("jm").getSuperclass() == Class.forName("pb"), "jm extends pb");
        ok(Class.forName("jp").isAssignableFrom(Class.forName("hh")), "hh implements jp");

        System.out.println("A2Table1401Test PASS (" + checks + " checks)");
    }
}
