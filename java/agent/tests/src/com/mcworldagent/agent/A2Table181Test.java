// [B5-ARCHIVE 2026-11-22] headless test archived from %TEMP%\mcaut (source batch: B b1.8.1); run via java/agent/tests/run.ps1
package com.mcworldagent.agent;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Map;

public class A2Table181Test {
    static int checks = 0;
    static void ok(boolean c, String n) {
        checks++;
        if (!c) { System.out.println("FAIL: " + n); throw new RuntimeException("check failed: " + n); }
    }

    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {
        byte[] buf = java.nio.file.Files.readAllBytes(new java.io.File(
                "E:/4k/github-projects/MCWorldAgent/java/agent/registry/fingerprints/b1.8.1.json").toPath());
        Map j = A2TableTest.obj(A2TableTest.parseJson(new String(buf, "UTF-8")));
        // 纯独立件（内嵌 stamp 自证，详表 §0）：单 sha 单表
        VersionTable bySha = VersionRegistry.byJarSha1("6b562463ccc2c7ff12ff350a2b04a67b3adcd37b");
        ok(bySha != null && "b1.8.1".equals(bySha.versionId), "byJarSha1 main");
        ok(A2TableTest.str(j.get("jarSha1")).equals("6b562463ccc2c7ff12ff350a2b04a67b3adcd37b"),
                "anchor sha");
        ok(bySha.jarSha1.equals(A2TableTest.str(j.get("jarSha1"))), "table jarSha1==anchor");
        ok(VersionRegistry.activate("b1.8.1"), "activate");
        VersionTable t = VersionRegistry.current();
        ok(t != null && "b1.8.1".equals(t.versionId), "current");
        ok(t == bySha, "activate==bySha same table");
        ok("G3".equals(t.family), "family");

        // ops 全表逐槽对榫（锚库 ops 键=VersionTable 字段名，63 槽+mobClasses 嵌套）
        Map ops = A2TableTest.obj(j.get("ops"));
        ok(ops.size() == 68, "ops 68 keys got " + ops.size());
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
        ok(compared == 67, "67 slots compared got " + compared);

        // b1.8.1 断代面重点复核（详表 §2 vs b1.7.3 漂移项全列）
        ok("x".equals(t.getWorldInfoM), "getWorldInfoM x（rl=ei 复刻，同字母保留）");
        ok("e".equals(t.worldTimeF), "worldTimeF e（rl.e:J）");
        ok("a".equals(t.randSeedF), "randSeedF a（rl.a:J）");
        ok("b".equals(t.spawnXF) && "c".equals(t.spawnYF) && "d".equals(t.spawnZF),
                "spawnXYZ b/c/d（rl 三 int）");
        ok("aa".equals(t.iframesF), "iframesF aa（⚠️b1.7.3=by）");
        ok("h".equals(t.hudPlayerF) && "q".equals(t.hudFontF) && "z".equals(t.hudSettingsF),
                "HUD 次级链 h/q/z（与 173 同形=锚数据；≥a1.2.3 免做不消费）");
        ok("E".equals(t.hudDebugGateF), "hudDebugGateF E=showDebugInfo（D=thirdPersonView 非门；⚠️173=B）");
        ok(Boolean.FALSE.equals(t.hudXyzPaint), "hudXyzPaint=false：a1.2.3 边界线 XYZ 注入免做（§5.1）");
        ok("g".equals(t.setBlockM), "setBlockM g（⚠️b1.7.3=f）");
        ok("e".equals(t.getMetaM), "getMetaM e（✓同 b1.7.3 翻案值）");
        ok("f".equals(t.setMetaSilentM), "setMetaSilentM f（⚠️b1.7.3=d）");
        ok("n".equals(t.getLightM), "getLightM n");
        ok("v".equals(t.difficultyF), "difficultyF v（⚠️b1.7.3=q）");
        ok("p".equals(t.entityListM), "entityListM p（⚠️b1.7.3=o）");
        ok("a".equals(t.spawnEntityM), "spawnEntityM a（⚠️b1.7.3=b 回漂）");
        ok("b".equals(t.setEntityDeadM), "setEntityDeadM b（⚠️b1.7.3=e 回漂）");
        ok("bz".equals(t.healthF), "healthF bz（⚠️b1.7.3=Y）");
        ok("o".equals(t.posXF) && "p".equals(t.posYF) && "q".equals(t.posZF),
                "posXF o/p/q（⚠️b1.7.3=aM-aO 整体重写）");
        ok("r".equals(t.motXF) && "s".equals(t.motYF) && "t".equals(t.motZF), "mot r/s/t");
        ok("u".equals(t.yawF) && "v".equals(t.pitchF), "look u/v");
        ok("as".equals(t.invF), "invF as（⚠️b1.7.3=c）");
        ok("i".equals(t.playerListF), "playerListF i（⚠️b1.7.3=d）");
        ok("l".equals(t.biomeNameF), "biomeNameF l（⚠️b1.7.3=kd.n）");
        ok("b".equals(t.teGetM), "teGetM b（TileEntity ow→ij 字母保留）");
        ok("(Lny;)V".equals(t.saveWorldDesc), "saveWorldDesc（IProgressUpdate yb→ny 断代）");
        ok("kj".equals(t.entityClassG), "entityClassG kj（⚠️b1.7.3 sn=Entity↔b1.8.1 sn=Creeper 撞车位）");
        ok("ee".equals(t.itemEntityG), "itemEntityG ee（hl→ee）");
        ok("sv".equals(t.itemClassG) && "f".equals(t.itemsF), "itemClassG sv.itemsF f（⚠️gm.c）");
        ok("um".equals(t.entityListClassG), "entityListClassG um（⚠️b1.7.3 jc=EntityList↔b1.8.1 jc=RegionFile）");
        ok("b".equals(t.entityListLookupM), "entityListLookupM b（✓同字母）");

        // 跨版本 getWorldInfoM=null 面（WorldInfo 分支仅 G3-region 批激活）
        VersionTable t1202 = VersionRegistry.byJarSha1("551be32655e81a410d81e76259502dde83ebe525");
        ok(t1202 != null && "b1.2_02".equals(t1202.versionId), "1202 by sha");
        ok(t1202.getWorldInfoM == null, "1202 getWorldInfoM null");
        VersionTable t0327 = VersionRegistry.byJarSha1("47b1b32430a211520993552ba0a5e00c1af44724");
        ok(t0327.getWorldInfoM == null, "0327 getWorldInfoM null");
        VersionTable t173 = VersionRegistry.byJarSha1("43db9b498cb67058d2e12d394e6507722e71bb45");
        ok(t173 != null && "b1.7.3".equals(t173.versionId), "173 by sha（共库双表对榫）");

        // hooks 四节对榫 TARGETS_181（五项全中纪律：class/method/desc/access/hits/mode）
        Map hooks = A2TableTest.obj(j.get("hooks"));
        ok(hooks.size() == 4, "hooks 4 got " + hooks.size());
        HookTargets.activate("b1.8.1");
        String[][] hookTriples = {
            {"SAVE_WORLD", HookTargets.SAVE_WORLD, "rv"},
            {"LOAD_WORLD", HookTargets.LOAD_WORLD, "net/minecraft/client/Minecraft"},
            {"RUN_TICK", HookTargets.RUN_TICK, "net/minecraft/client/Minecraft"},
            {"HUD_RENDER", HookTargets.HUD_RENDER, "abj"},
        };
        for (int i = 0; i < hookTriples.length; i++) {
            Map h = A2TableTest.obj(hooks.get(hookTriples[i][0]));
            ok(h != null, "anchor hook " + hookTriples[i][0]);
            HookTargets.Target tg = null;
            HookTargets.Target[] all = HookTargets.forClass(hookTriples[i][2]);
            for (int k = 0; k < all.length; k++) {
                if (hookTriples[i][1].equals(all[k].hookId)) { tg = all[k]; }
            }
            ok(tg != null, "TARGETS_181 has " + hookTriples[i][1]);
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
        // WORLD_CTOR 省略断言（rv 构造族=(acq,String,dx) 系——WorldSettings dx 参）
        ok(hooks.get("WORLD_CTOR") == null, "no WORLD_CTOR hook");
        ok(HookTargets.forClass("rv").length == 1, "rv 单钩（仅 SAVE_WORLD）");

        // mob 白名单 8 项对榫（全表断代；mob 槽已摘——el181-1 判决：真 tz=EntityCreature
        // abstract（javap 实锤，b1.8-pre1-2 起类树断代；173 ii=concrete 故 173 mob OK 不矛盾），
        // tb/aq/pe 中间层全 abstract=无泛用可实例化 mob 基类）
        Map mobJson = A2TableTest.obj(ops.get("mobClasses"));
        ok(t.mobClasses.size() == 8 && mobJson.size() == 8, "mobClasses 8");
        ok(!t.mobClasses.containsKey("mob") && mobJson.get("mob") == null, "mob 槽=摘除（tz abstract 断代）");
        String[][] mobs = {
            {"pig", "mx"}, {"sheep", "bx"}, {"creeper", "sn"}, {"skeleton", "se"},
            {"zombie", "abq"}, {"spider", "bh"}, {"tnt", "ze"}, {"giant", "hv"},
        };
        for (int i = 0; i < mobs.length; i++) {
            ok(mobs[i][1].equals(t.mobClasses.get(mobs[i][0])), "mob " + mobs[i][0]);
            ok(mobs[i][1].equals(A2TableTest.str(mobJson.get(mobs[i][0]))), "mob json " + mobs[i][0]);
        }

        // capabilities：28 OK / 7 UNSUPPORTED（b1.7.3 同判决——旁证直转零增删）
        Map caps = A2TableTest.obj(j.get("capabilities"));
        int okN = 0;
        int unN = 0;
        for (Object v : caps.values()) {
            String s = A2TableTest.str(v);
            if ("OK".equals(s)) { okN++; }
            else if ("UNSUPPORTED".equals(s)) { unN++; }
            else { throw new RuntimeException("bad cap value " + s); }
        }
        ok(okN == 28, "caps 28 OK got " + okN);
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
        ok("OK".equals(A2TableTest.str(caps.get("listops"))), "listops OK");

        // null 槽断言（13 件：档位欠账面同 b1.7.3）
        String[] nullSlots = {
            "light6M", "observersF", "scatterClassG", "entityTypeM", "invSizeM",
            "invGetM", "invSetM", "teBurnF", "teCookF", "idCrops",
            "idSapling", "cropsRipeMeta", "playerF",
        };
        for (int i = 0; i < nullSlots.length; i++) {
            ok(A2TableTest.slot(t, nullSlots[i]) == null, nullSlots[i] + " null");
        }

        // 假件实形访问位复核（钉表纪律：private 就 private，防假绿）
        // 1) rl 五槽全 private（WorldOps WorldInfo 分支 getDeclaredField+setAccessible 前提）
        String[] rlPrivs = {"a", "e", "b", "c", "d"};
        for (int i = 0; i < rlPrivs.length; i++) {
            Field f = Class.forName("rl").getDeclaredField(rlPrivs[i]);
            ok(Modifier.isPrivate(f.getModifiers()), "rl." + rlPrivs[i] + " private");
        }
        // 2) rv 无 time/spawn 承载面：rv.e=List 非 long（五槽不在 World 本体）
        ok(java.util.List.class == Class.forName("rv").getField("e").getType(), "rv.e is List not worldTime");
        // 3) ul.d public（ItemStack 无 private 断代——getField 直取路径）
        ok(Modifier.isPublic(Class.forName("ul").getDeclaredField("d").getModifiers()), "ul.d public");
        // 4) kj.aa public int（iframes 实形）+ G=isDead 撞车位区分
        Field aa = Class.forName("kj").getField("aa");
        ok(Modifier.isPublic(aa.getModifiers()) && aa.getType() == int.class, "kj.aa public int");
        ok(Class.forName("kj").getField("G").getType() == boolean.class, "kj.G boolean isDead");
        // 5) 钩面访问位：rv.a(Lny;)V public、rv.x()Lrl public
        ok(Modifier.isPublic(Class.forName("rv").getMethod("a", Class.forName("ny")).getModifiers()),
                "rv.a(Lny;)V public 0x0001");
        ok(Class.forName("rv").getMethod("x").getReturnType() == Class.forName("rl"), "rv.x()Lrl");
        // 6) biome 链可解析：rv.a()Lnu / nu.a(II)Lvh / vh.l String
        ok(Class.forName("rv").getMethod("a").getReturnType() == Class.forName("nu"), "fake rv.a()Lnu;");
        ok(Class.forName("nu").getMethod("a", int.class, int.class).getReturnType()
                == Class.forName("vh"), "fake nu.a(II)Lvh;");
        ok(Class.forName("vh").getField("l").getType() == String.class, "fake vh.l String");
        // 7) explode 返回值面：a(Lkj;DDDF)Lzf（invoke 面不消费）
        ok(Class.forName("rv").getMethod("a", Class.forName("kj"), double.class, double.class,
                double.class, float.class).getReturnType() == Class.forName("zf"), "rv.a(Lkj;DDDF)Lzf;");
        // 8) tnt 断代延续：ze 仅 (rv,DDD)V 无 FFF
        Class zeC = Class.forName("ze");
        zeC.getConstructor(Class.forName("rv"), double.class, double.class, double.class);
        checks++;
        boolean noFFF = false;
        try { zeC.getConstructor(Class.forName("rv"), float.class, float.class, float.class); }
        catch (NoSuchMethodException e) { noFFF = true; }
        ok(noFFF, "ze no (rv,FFF)V");
        // 9) ul 仅 (III)V 无 (II)V（newStack dmg=0 等价配方前提）
        boolean noCtor2 = false;
        try { Class.forName("ul").getConstructor(int.class, int.class); }
        catch (NoSuchMethodException e) { noCtor2 = true; }
        ok(noCtor2, "ul no (II)V");
        Class.forName("ul").getConstructor(int.class, int.class, int.class);
        checks++;
        // 10) 撞车位自证：root181 内 sn=EntityCreeper（extends tz），非 Entity
        ok(Class.forName("sn").getSuperclass() == Class.forName("tz"), "sn=Creeper extends tz（撞车位区分）");
        ok(Class.forName("kj").getSuperclass() == null || !Class.forName("kj").getName().equals("sn"),
                "kj!=sn 身份分离");
        // 11) rv 同形陷阱群实形：g(IIII)Z 主写面 + g(III)Z decoy + d(IIII)Z 裸 + e(IIII)Z meta
        ok(Class.forName("rv").getMethod("g", int.class, int.class, int.class, int.class)
                .getReturnType() == boolean.class, "rv.g(IIII)Z 主写面");
        ok(Class.forName("rv").getMethod("g", int.class, int.class, int.class)
                .getReturnType() == boolean.class, "rv.g(III)Z decoy 在位");
        ok(Class.forName("rv").getMethod("d", int.class, int.class, int.class, int.class)
                .getReturnType() == boolean.class, "rv.d(IIII)Z 裸 setBlock decoy");
        ok(Class.forName("rv").getMethod("e", int.class, int.class, int.class, int.class)
                .getReturnType() == boolean.class, "rv.e(IIII)Z setBlockMetadata decoy");

        System.out.println("A2Table181Test PASS (" + checks + " checks)");
    }
}
