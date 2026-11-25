// [B5-ARCHIVE 2026-11-22] headless test archived from %TEMP%\mcaut (source batch: B b1.7.3); run via java/agent/tests/run.ps1
package com.mcworldagent.agent;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Map;

public class A2Table173Test {
    static int checks = 0;
    static void ok(boolean c, String n) {
        checks++;
        if (!c) { System.out.println("FAIL: " + n); throw new RuntimeException("check failed: " + n); }
    }

    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {
        byte[] buf = java.nio.file.Files.readAllBytes(new java.io.File(
                "E:/4k/github-projects/MCWorldAgent/java/agent/registry/fingerprints/b1.7.3.json").toPath());
        Map j = A2TableTest.obj(A2TableTest.parseJson(new String(buf, "UTF-8")));
        // 纯独立件（内嵌 stamp 自证，详表 §0）：单 sha 单表
        VersionTable bySha = VersionRegistry.byJarSha1("43db9b498cb67058d2e12d394e6507722e71bb45");
        ok(bySha != null && "b1.7.3".equals(bySha.versionId), "byJarSha1 main");
        ok(A2TableTest.str(j.get("jarSha1")).equals("43db9b498cb67058d2e12d394e6507722e71bb45"),
                "anchor sha");
        ok(bySha.jarSha1.equals(A2TableTest.str(j.get("jarSha1"))), "table jarSha1==anchor");
        ok(VersionRegistry.activate("b1.7.3"), "activate");
        VersionTable t = VersionRegistry.current();
        ok(t != null && "b1.7.3".equals(t.versionId), "current");
        ok(t == bySha, "activate==bySha same table");
        ok("G3".equals(t.family), "family");

        // ops 全表逐槽对榫（锚库 ops 键=VersionTable 字段名，63 槽+mobClasses 嵌套）
        Map ops = A2TableTest.obj(j.get("ops"));
        ok(ops.size() == 72, "ops 72 keys got " + ops.size());
        int compared = 0;
        for (Object ko : ops.keySet()) {
            String key = (String) ko;
            if ("mobClasses".equals(key)) { continue; }
            ok(A2TableTest.str(ops.get(key)) == null
                            ? A2TableTest.slot(t, key) == null
                            : ops.get(key).equals(A2TableTest.slot(t, key)),
                    "ops " + key);
            compared++;
        }
        ok(compared == 71, "71 slots compared got " + compared);

        // 三大结构断代重点复核（详表 §2/§9）
        ok("x".equals(t.getWorldInfoM), "getWorldInfoM x（WorldInfo 断代分支开关）");
        ok("e".equals(t.worldTimeF), "worldTimeF e（ei.e:J）");
        ok("a".equals(t.randSeedF), "randSeedF a（ei.a:J）");
        ok("b".equals(t.spawnXF) && "c".equals(t.spawnYF) && "d".equals(t.spawnZF),
                "spawnXYZ b/c/d（ei 三 int）");
        ok("by".equals(t.iframesF), "iframesF by 复活（b1.2_02 档位一欠账清偿）");
        ok("h".equals(t.hudPlayerF) && "q".equals(t.hudFontF) && "z".equals(t.hudSettingsF),
                "HUD 次级链 h/q/z（javap 实锤=锚数据；≥a1.2.3 免做不消费）");
        ok("B".equals(t.hudDebugGateF), "hudDebugGateF B=showDebugInfo（A=thirdPersonView 非门）");
        ok(Boolean.FALSE.equals(t.hudXyzPaint), "hudXyzPaint=false：a1.2.3 边界线 XYZ 注入免做（§5.1）");
        ok("f".equals(t.setBlockM), "setBlockM f（⚠️b1.2_02=e）");
        ok("e".equals(t.getMetaM), "getMetaM e");
        ok("d".equals(t.setMetaSilentM), "setMetaSilentM d");
        ok("n".equals(t.getLightM), "getLightM n");
        ok("q".equals(t.difficultyF), "difficultyF q（⚠️b1.2_02=k）");
        ok("o".equals(t.entityListM), "entityListM o（⚠️历代 l）");
        ok("b".equals(t.spawnEntityM), "spawnEntityM b（⚠️历代 a）");
        ok("e".equals(t.setEntityDeadM), "setEntityDeadM e（⚠️历代 d）");
        ok("Y".equals(t.healthF), "healthF Y（⚠️b1.2_02=S）");
        ok("aM".equals(t.posXF), "posXF aM（⚠️b1.2_02=aF 整体断代）");
        ok("c".equals(t.invF), "invF c（回漂）");
        ok("n".equals(t.biomeNameF), "biomeNameF n（⚠️b1.2_02=m）");
        ok("b".equals(t.teGetM), "teGetM b");
        ok("(Lyb;)V".equals(t.saveWorldDesc), "saveWorldDesc（IProgressUpdate ru→yb 断代）");
        ok("(Lsn;DDDF".equals("(L" + t.entityClassG + ";DDDF") , "entityClassG sanity");
        ok("sn".equals(t.entityClassG), "entityClassG sn");
        ok("hl".equals(t.itemEntityG), "itemEntityG hl");
        ok("gm".equals(t.itemClassG), "itemClassG gm");
        ok("jc".equals(t.entityListClassG), "entityListClassG jc");
        ok("b".equals(t.entityListLookupM), "entityListLookupM b");

        // 跨版本 getWorldInfoM=null 面（WorldInfo 分支仅本版激活——其余版本零行为变化）
        VersionTable t1202 = VersionRegistry.byJarSha1("551be32655e81a410d81e76259502dde83ebe525");
        ok(t1202 != null && "b1.2_02".equals(t1202.versionId), "1202 by sha");
        ok(t1202.getWorldInfoM == null, "1202 getWorldInfoM null");
        VersionTable t0327 = VersionRegistry.byJarSha1("47b1b32430a211520993552ba0a5e00c1af44724");
        ok(t0327.getWorldInfoM == null, "0327 getWorldInfoM null");

        // hooks 四节对榫 TARGETS_173（五项全中纪律：class/method/desc/access/hits/mode）
        Map hooks = A2TableTest.obj(j.get("hooks"));
        ok(hooks.size() == 4, "hooks 4 got " + hooks.size());
        HookTargets.activate("b1.7.3");
        String[][] hookTriples = {
            {"SAVE_WORLD", HookTargets.SAVE_WORLD, "fd"},
            {"LOAD_WORLD", HookTargets.LOAD_WORLD, "net/minecraft/client/Minecraft"},
            {"RUN_TICK", HookTargets.RUN_TICK, "net/minecraft/client/Minecraft"},
            {"HUD_RENDER", HookTargets.HUD_RENDER, "uq"},
        };
        for (int i = 0; i < hookTriples.length; i++) {
            Map h = A2TableTest.obj(hooks.get(hookTriples[i][0]));
            ok(h != null, "anchor hook " + hookTriples[i][0]);
            HookTargets.Target tg = null;
            HookTargets.Target[] all = HookTargets.forClass(hookTriples[i][2]);
            for (int k = 0; k < all.length; k++) {
                if (hookTriples[i][1].equals(all[k].hookId)) { tg = all[k]; }
            }
            ok(tg != null, "TARGETS_173 has " + hookTriples[i][1]);
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
        // WORLD_CTOR 省略断言（fd(File,String) 已消失——构造器族断代）
        ok(hooks.get("WORLD_CTOR") == null, "no WORLD_CTOR hook");
        ok(HookTargets.forClass("fd").length == 1, "fd 单钩（仅 SAVE_WORLD）");

        // mob 白名单 9 项对榫
        Map mobJson = A2TableTest.obj(ops.get("mobClasses"));
        ok(t.mobClasses.size() == 9 && mobJson.size() == 9, "mobClasses 9");
        String[][] mobs = {
            {"pig", "wh"}, {"sheep", "dl"}, {"creeper", "gb"}, {"skeleton", "fr"},
            {"zombie", "uz"}, {"spider", "cn"}, {"tnt", "qw"}, {"giant", "nt"}, {"mob", "ii"},
        };
        for (int i = 0; i < mobs.length; i++) {
            ok(mobs[i][1].equals(t.mobClasses.get(mobs[i][0])), "mob " + mobs[i][0]);
            ok(mobs[i][1].equals(A2TableTest.str(mobJson.get(mobs[i][0]))), "mob json " + mobs[i][0]);
        }

        // capabilities：28 OK / 7 UNSUPPORTED（档位一五+二件连坐）
        Map caps = A2TableTest.obj(j.get("capabilities"));
        int okN = 0;
        int unN = 0;
        for (Object v : caps.values()) {
            String s = A2TableTest.str(v);
            if ("OK".equals(s)) { okN++; }
            else if ("UNSUPPORTED".equals(s)) { unN++; }
            else { throw new RuntimeException("bad cap value " + s); }
        }
        ok(okN == 29, "caps 29 OK got " + okN);
        ok(unN == 7, "caps 7 UNSUPPORTED got " + unN);
        String[][] unPairs = {
            {"setmeta", "observersF"}, {"grow", "idCrops"}, {"genscatter", "scatterClassG"},
            {"getchest", "invSizeM"}, {"setchest", "invSizeM"}, {"getfurnace", "teBurnF"},
            {"setfurnace", "teBurnF"},
        };
        for (int i = 0; i < unPairs.length; i++) {
            ok("UNSUPPORTED".equals(A2TableTest.str(caps.get(unPairs[i][0]))), "unconfirmed " + unPairs[i][0]);
            ok(A2TableTest.slot(t, unPairs[i][1]) == null, unPairs[i][0] + " slot " + unPairs[i][1] + " null");
        }
        String[][] okPairs = {
            {"place", "setBlockM"}, {"dig", "setBlockM"}, {"getblock", "getBlockM"},
            {"getblocks", "getBlockM"}, {"fill", "setBlockM"}, {"replace", "getBlockM"},
            {"save", "saveWorldM"}, {"give", "addStackM"}, {"inv", "invF"},
            {"setarmor", "addStackM"}, {"clearinv", "invMainF"}, {"entitylist", "entityListM"},
            {"playsound", "playSoundM"}, {"getmeta", "getMetaM"},
            {"getinfo", "difficultyF"}, {"getlight", "getLightM"},
            {"settime", "worldTimeF"}, {"setspawn", "spawnXF"},
            {"tp", "setPosRotM"}, {"getplayer", "healthF"}, {"sethealth", "healthF"},
            {"spawn", "spawnEntityM"}, {"drop", "spawnEntityM"}, {"explode", "explodeM"},
            {"clear", "setEntityDeadM"}, {"getbiome", "biomeWcmM"},
            {"invulnerable", "iframesF"},
        };
        ok(okPairs.length == 27, "27 slot-backed OK pairs");
        for (int i = 0; i < okPairs.length; i++) {
            ok("OK".equals(A2TableTest.str(caps.get(okPairs[i][0]))), "OK " + okPairs[i][0]);
            ok(A2TableTest.slot(t, okPairs[i][1]) != null, okPairs[i][0] + " slot " + okPairs[i][1] + " non-null");
        }
        ok("OK".equals(A2TableTest.str(caps.get("listops"))), "listops OK"); // 通道级，无表槽

        // null 槽断言（13 件：档位一欠账面）
        String[] nullSlots = {
            "light6M", "observersF", "scatterClassG", "entityTypeM", "invSizeM",
            "invGetM", "invSetM", "teBurnF", "teCookF", "idCrops",
            "idSapling", "cropsRipeMeta", "playerF",
        };
        for (int i = 0; i < nullSlots.length; i++) {
            ok(A2TableTest.slot(t, nullSlots[i]) == null, nullSlots[i] + " null");
        }

        // 假件实形访问位复核（钉表纪律：private 就 private，防假绿）
        // 1) ei 五槽全 private（WorldOps WorldInfo 分支 getDeclaredField+setAccessible 前提）
        String[] eiPrivs = {"a", "e", "b", "c", "d"};
        for (int i = 0; i < eiPrivs.length; i++) {
            Field f = Class.forName("ei").getDeclaredField(eiPrivs[i]);
            ok(Modifier.isPrivate(f.getModifiers()), "ei." + eiPrivs[i] + " private");
        }
        ok(!Modifier.isPublic(Class.forName("ei").getDeclaredField("e").getModifiers()),
                "ei.e not public（假绿守卫）");
        // 2) fd 无 time/spawn 承载面：fd.e=List 非 long（五槽不在 World 本体实锤）
        ok(java.util.List.class == Class.forName("fd").getField("e").getType(), "fd.e is List not worldTime");
        // 3) iz.d public（本版 ItemStack 无 private 断代——getField 直取路径）
        ok(Modifier.isPublic(Class.forName("iz").getDeclaredField("d").getModifiers()), "iz.d public");
        // 4) sn.by public int（iframes 复活实形）+ be=isDead 撞车位区分
        Field by = Class.forName("sn").getField("by");
        ok(Modifier.isPublic(by.getModifiers()) && by.getType() == int.class, "sn.by public int");
        ok(Class.forName("sn").getField("be").getType() == boolean.class, "sn.be boolean isDead");
        // 5) 钩面访问位：fd.a(Lyb;)V public、fd.x()Lei public
        ok(Modifier.isPublic(Class.forName("fd").getMethod("a", Class.forName("yb")).getModifiers()),
                "fd.a(Lyb;)V public 0x0001");
        ok(Class.forName("fd").getMethod("x").getReturnType() == Class.forName("ei"), "fd.x()Lei");
        // 6) biome 链可解析：fd.a()Lxv / xv.a(II)Lkd / kd.n String
        ok(Class.forName("fd").getMethod("a").getReturnType() == Class.forName("xv"), "fake fd.a()Lxv;");
        ok(Class.forName("xv").getMethod("a", int.class, int.class).getReturnType()
                == Class.forName("kd"), "fake xv.a(II)Lkd;");
        ok(Class.forName("kd").getField("n").getType() == String.class, "fake kd.n String");
        // 7) explode 返回值断代留档：a(Lsn;DDDF)Lqx（getMethod 返回值无关，invoke 面不消费）
        ok(Class.forName("fd").getMethod("a", Class.forName("sn"), double.class, double.class,
                double.class, float.class).getReturnType() == Class.forName("qx"), "fd.a(Lsn;DDDF)Lqx;");
        // 8) tnt 断代延续：qw 仅 (fd,DDD)V 无 FFF（WorldOps spawn fallback 消费）
        Class qwC = Class.forName("qw");
        qwC.getConstructor(Class.forName("fd"), double.class, double.class, double.class);
        checks++;
        boolean noFFF = false;
        try { qwC.getConstructor(Class.forName("fd"), float.class, float.class, float.class); }
        catch (NoSuchMethodException e) { noFFF = true; }
        ok(noFFF, "qw no (fd,FFF)V");
        // 9) iz 仅 (III)V 无 (II)V（newStack dmg=0 等价配方前提）
        boolean noCtor2 = false;
        try { Class.forName("iz").getConstructor(int.class, int.class); }
        catch (NoSuchMethodException e) { noCtor2 = true; }
        ok(noCtor2, "iz no (II)V");
        Class.forName("iz").getConstructor(int.class, int.class, int.class);
        checks++;

        System.out.println("A2Table173Test PASS (" + checks + " checks)");
    }
}
