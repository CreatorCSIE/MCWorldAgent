// [B5-ARCHIVE 2026-11-22] headless test archived from %TEMP%\mcaut (source batch: A2 b1.2_02); run via java/agent/tests/run.ps1
package com.mcworldagent.agent;

import java.util.Map;

public class A2Table1202Test {
    static int checks = 0;
    static void ok(boolean c, String n) {
        checks++;
        if (!c) { System.out.println("FAIL: " + n); throw new RuntimeException("check failed: " + n); }
    }

    public static void main(String[] args) throws Exception {
        byte[] buf = java.nio.file.Files.readAllBytes(new java.io.File(
                "E:/4k/github-projects/MCWorldAgent/java/agent/registry/fingerprints/b1.2_02.json").toPath());
        Map j = A2TableTest.obj(A2TableTest.parseJson(new String(buf, "UTF-8")));
        // 两态同名版：主表件+LW 别名件双 sha 同表（详表 §0）
        VersionTable bySha = VersionRegistry.byJarSha1("551be32655e81a410d81e76259502dde83ebe525");
        ok(bySha != null && "b1.2_02".equals(bySha.versionId), "byJarSha1 main");
        VersionTable byAlias = VersionRegistry.byJarSha1("093f371e1a05d89664cfb8068d607953687d5d94");
        ok(byAlias != null && "b1.2_02".equals(byAlias.versionId), "byJarSha1 LW alias");
        ok(byAlias == bySha, "alias same table");
        ok(VersionRegistry.activate("b1.2_02"), "activate");
        VersionTable t = VersionRegistry.current();
        ok(t != null && "b1.2_02".equals(t.versionId), "current");
        ok("G3".equals(t.family), "family");
        Map targets = A2TableTest.obj(j.get("targets"));
        Map caps = A2TableTest.obj(j.get("capabilities"));

        // 消费槽位（41 槽=120 的 42 减 iframes——本批 iframesF=null 档位一，单独断言）
        String[] consumed = {
            "setBlockM", "getBlockM", "getMetaM", "saveWorldM", "worldTimeF",
            "randSeedF", "spawnXF", "spawnYF", "spawnZF", "teGetM",
            "setPosRotM", "posXF", "posYF", "posZF", "motXF", "motYF", "motZF",
            "yawF", "pitchF", "healthF", "invF", "invMainF", "invArmorF",
            "invCurF", "isCountF", "isIdF", "isDmgF", "addStackM", "itemsF",
            "explodeM", "spawnEntityM", "setEntityDeadM", "entityListM", "playSoundM",
            "entityListLookupM", "difficultyF", "getLightM", "setMetaSilentM",
            "biomeWcmM", "biomeAtM", "biomeNameF",
        };
        String[] consumedAnchor = {
            "setBlock", "getBlockId", "getMeta", "saveWorld", "worldTime",
            "randSeed", "spawnX", "spawnY", "spawnZ", "teGet",
            "setPosRot", "posX", "posY", "posZ", "motX", "motY", "motZ",
            "yaw", "pitch", "health", "invField", "invMain", "invArmor",
            "invCur", "isCount", "isId", "isDmg", "addStack", "itemRegistry",
            "explode", "spawnEntity", "setEntityDead", "entityList", "playSound",
            "entityListLookup", "difficulty", "getLight", "setMetaSilent",
            "getWorldChunkManager", "biomeAt", "biomeNameField",
        };
        ok(consumed.length == 41 && consumed.length == consumedAnchor.length, "consumed 41");
        for (int i = 0; i < consumed.length; i++) {
            Map tg = A2TableTest.obj(targets.get(consumedAnchor[i]));
            ok(tg != null, "anchor " + consumedAnchor[i]);
            ok(A2TableTest.str(tg.get("name")).equals(A2TableTest.slot(t, consumed[i])),
                    consumed[i] + "==" + consumedAnchor[i]);
        }
        // 断代字母重点复核（详表 §2/§3/§5）
        ok("e".equals(t.setBlockM), "setBlockM e");
        ok("S".equals(t.healthF), "healthF S");
        ok("aF".equals(t.posXF), "posXF aF");
        ok("u".equals(t.randSeedF), "randSeedF u");
        ok("m".equals(t.spawnXF), "spawnXF m");
        ok("f".equals(t.invF), "invF f");
        ok("c".equals(t.invCurF), "invCurF c");
        ok("l".equals(t.entityListM), "entityListM l");
        ok("l".equals(t.getLightM), "getLightM l");
        ok("c".equals(t.setMetaSilentM), "setMetaSilentM c");

        // 描述符对榫
        ok("(Lru;)V".equals(t.saveWorldDesc), "saveWorldDesc");
        ok("(DDDFF)V".equals(t.setPosRotDesc), "setPosRotDesc");
        ok("(Lru;)V".equals(A2TableTest.str(A2TableTest.obj(targets.get("saveWorld")).get("descriptor"))),
                "saveWorld desc anchor");
        ok("(III)Lkp;".equals(A2TableTest.str(A2TableTest.obj(targets.get("teGet")).get("descriptor"))),
                "teGet desc");
        ok("(Lnl;)Z".equals(A2TableTest.str(A2TableTest.obj(targets.get("spawnEntity")).get("descriptor"))),
                "spawnEntity desc");
        ok("(Lnl;)V".equals(A2TableTest.str(A2TableTest.obj(targets.get("setEntityDead")).get("descriptor"))),
                "setEntityDead desc");
        ok("()Ljava/util/List;".equals(A2TableTest.str(A2TableTest.obj(targets.get("entityList")).get("descriptor"))),
                "entityList desc");
        ok("(DDDLjava/lang/String;FF)V".equals(A2TableTest.str(A2TableTest.obj(targets.get("playSound")).get("descriptor"))),
                "playSound desc");
        ok("(Lnl;DDDF)Lmc;".equals(A2TableTest.str(A2TableTest.obj(targets.get("explode")).get("descriptor"))),
                "explode desc"); // c11b777 翻案：V→Lmc;（Explosion 返回值，getMethod 面不消费）
        ok("(Lnl;)Ljava/lang/String;".equals(A2TableTest.str(A2TableTest.obj(targets.get("entityListLookup")).get("descriptor"))),
                "entityListLookup desc");
        ok("(III)I".equals(A2TableTest.str(A2TableTest.obj(targets.get("getBlockId")).get("descriptor"))),
                "getBlockId desc");
        ok("(IIII)Z".equals(A2TableTest.str(A2TableTest.obj(targets.get("setBlock")).get("descriptor"))),
                "setBlock desc");
        ok("(III)I".equals(A2TableTest.str(A2TableTest.obj(targets.get("getMeta")).get("descriptor"))),
                "getMeta desc");
        ok("(IIII)V".equals(A2TableTest.str(A2TableTest.obj(targets.get("setMetaSilent")).get("descriptor"))),
                "setMetaSilent desc");
        ok("(III)I".equals(A2TableTest.str(A2TableTest.obj(targets.get("getLight")).get("descriptor"))),
                "getLight desc");
        ok("[Leo;".equals(A2TableTest.str(A2TableTest.obj(targets.get("itemRegistry")).get("descriptor"))),
                "itemRegistry desc");
        ok("Lgl;".equals(A2TableTest.str(A2TableTest.obj(targets.get("invField")).get("descriptor"))),
                "invField desc");
        ok("[Lgm;".equals(A2TableTest.str(A2TableTest.obj(targets.get("invMain")).get("descriptor"))),
                "invMain desc");
        ok("[Lgm;".equals(A2TableTest.str(A2TableTest.obj(targets.get("invArmor")).get("descriptor"))),
                "invArmor desc");
        ok("I".equals(A2TableTest.str(A2TableTest.obj(targets.get("isCount")).get("descriptor"))),
                "isCount desc");
        ok("I".equals(A2TableTest.str(A2TableTest.obj(targets.get("isId")).get("descriptor"))),
                "isId desc");
        ok("I".equals(A2TableTest.str(A2TableTest.obj(targets.get("isDmg")).get("descriptor"))),
                "isDmg desc");
        ok("J".equals(A2TableTest.str(A2TableTest.obj(targets.get("worldTime")).get("descriptor"))),
                "worldTime desc");
        ok("J".equals(A2TableTest.str(A2TableTest.obj(targets.get("randSeed")).get("descriptor"))),
                "randSeed desc");
        // setBlockNoNotify 留档面：裸 setBlock=b(IIII)Z（主写面=e，严禁按签名猜名陷阱）
        ok("b".equals(A2TableTest.str(A2TableTest.obj(targets.get("setBlockNoNotify")).get("name"))),
                "setBlockNoNotify b");
        ok("(IIII)Z".equals(A2TableTest.str(A2TableTest.obj(targets.get("setBlockNoNotify")).get("descriptor"))),
                "setBlockNoNotify desc");

        // 类面锚
        ok("dn".equals(A2TableTest.str(A2TableTest.obj(targets.get("worldClass")).get("class"))), "worldClass dn");
        ok("(Ljava/io/File;Ljava/lang/String;)V".equals(
                A2TableTest.str(A2TableTest.obj(targets.get("worldCtor")).get("descriptor"))), "worldCtor desc");
        ok("nl".equals(A2TableTest.str(A2TableTest.obj(targets.get("entityClass")).get("class"))), "entityClass nl");
        ok("nl".equals(t.entityClassG), "entityClassG");
        ok("kp".equals(A2TableTest.str(A2TableTest.obj(targets.get("tileEntityClass")).get("class"))), "tileEntity kp");
        ok("ig".equals(A2TableTest.str(A2TableTest.obj(targets.get("entityLivingClass")).get("class"))), "entityLiving ig");
        ok("eu".equals(A2TableTest.str(A2TableTest.obj(targets.get("entityPlayerClass")).get("class"))), "entityPlayer eu");
        ok("gl".equals(A2TableTest.str(A2TableTest.obj(targets.get("inventoryPlayerClass")).get("class"))), "inventoryPlayer gl");
        ok("gm".equals(A2TableTest.str(A2TableTest.obj(targets.get("itemStackClass")).get("class"))), "itemStack gm");
        ok("(III)V".equals(A2TableTest.str(A2TableTest.obj(targets.get("itemStackCtor")).get("descriptor"))),
                "itemStackCtor desc");
        ok("eo".equals(t.itemClassG), "itemClassG eo");
        ok("fh".equals(t.itemEntityG), "itemEntityG fh");
        ok("gn".equals(t.entityListClassG), "entityListClassG gn");

        // biome 三锚类面 + 假件链可解析性（dn.a()ro / ro.a(II)hf / hf.m String）
        ok("ro".equals(A2TableTest.str(A2TableTest.obj(targets.get("worldChunkManagerClass")).get("class"))), "wcm class ro");
        ok("hf".equals(A2TableTest.str(A2TableTest.obj(targets.get("biomeClass")).get("class"))), "biome class hf");
        ok("()Lro;".equals(A2TableTest.str(A2TableTest.obj(targets.get("getWorldChunkManager")).get("descriptor"))), "biomeWcm desc");
        ok("(II)Lhf;".equals(A2TableTest.str(A2TableTest.obj(targets.get("biomeAt")).get("descriptor"))), "biomeAt desc");
        ok("Ljava/lang/String;".equals(A2TableTest.str(A2TableTest.obj(targets.get("biomeNameField")).get("descriptor"))), "biomeName desc");
        ok(Class.forName("dn").getMethod("a").getReturnType() == Class.forName("ro"), "fake dn.a()Lro;");
        ok(Class.forName("ro").getMethod("a", int.class, int.class).getReturnType() == Class.forName("hf"), "fake ro.a(II)Lhf;");
        ok(Class.forName("hf").getField("m").getType() == String.class, "fake hf.m String");

        // null 面三件：playerField 档位三 / snowCovered 不存在 / iframes 档位一（aW=isDead 撞车）
        ok(targets.get("playerField") == null, "anchor playerField null");
        ok(t.playerF == null, "playerF null");
        ok("d".equals(t.playerListF), "playerListF d");
        ok("Ljava/util/List;".equals(A2TableTest.str(A2TableTest.obj(targets.get("playerListField")).get("descriptor"))),
                "playerListField desc");
        ok(targets.get("snowCovered") == null, "anchor snowCovered null");
        ok(targets.get("iframes") == null, "anchor iframes null");
        ok(t.iframesF == null, "iframesF null");

        // hook 面锚
        ok("net/minecraft/client/Minecraft".equals(A2TableTest.str(A2TableTest.obj(targets.get("minecraftClass")).get("class"))), "mc class");
        ok("e".equals(A2TableTest.str(A2TableTest.obj(targets.get("theWorldField")).get("name"))), "theWorld e");
        ok("Ldn;".equals(A2TableTest.str(A2TableTest.obj(targets.get("theWorldField")).get("descriptor"))), "theWorld desc");
        ok("g".equals(A2TableTest.str(A2TableTest.obj(targets.get("thePlayerField")).get("name"))), "thePlayer g");
        ok("Lbz;".equals(A2TableTest.str(A2TableTest.obj(targets.get("thePlayerField")).get("descriptor"))), "thePlayer desc");
        ok("u".equals(A2TableTest.str(A2TableTest.obj(targets.get("ingameGuiField")).get("name"))), "ingameGui u");
        ok("Lpe;".equals(A2TableTest.str(A2TableTest.obj(targets.get("ingameGuiField")).get("descriptor"))), "ingameGui desc");
        ok("a".equals(A2TableTest.str(A2TableTest.obj(targets.get("loadWorld")).get("name"))), "loadWorld a");
        ok("(Ldn;Ljava/lang/String;Leu;)V".equals(A2TableTest.str(A2TableTest.obj(targets.get("loadWorld")).get("descriptor"))), "loadWorld desc（判决 B 移植批=3 参实体）");
        ok("i".equals(A2TableTest.str(A2TableTest.obj(targets.get("runTick")).get("name"))), "runTick i");
        ok("()V".equals(A2TableTest.str(A2TableTest.obj(targets.get("runTick")).get("descriptor"))), "runTick desc");
        ok("pe".equals(A2TableTest.str(A2TableTest.obj(targets.get("guiIngame")).get("class"))), "guiIngame pe");
        ok("a".equals(A2TableTest.str(A2TableTest.obj(targets.get("guiIngame")).get("name"))), "guiIngame a");
        ok("(FZII)V".equals(A2TableTest.str(A2TableTest.obj(targets.get("guiIngame")).get("descriptor"))), "guiIngame desc");

        // 断代实形三取证（假件=真 jar 实形复刻，WorldOps fallback 消费面）
        // 1) gm.itemDamage=d private（WorldOps getDeclaredField+setAccessible）
        ok(java.lang.reflect.Modifier.isPrivate(Class.forName("gm").getDeclaredField("d").getModifiers()),
                "gm.d private");
        // 2) gm 无 (II)V 构造（WorldOps newStack 走 (III)V dmg=0 等价）
        boolean noCtor2 = false;
        try { Class.forName("gm").getConstructor(int.class, int.class); }
        catch (NoSuchMethodException e) { noCtor2 = true; }
        ok(noCtor2, "gm no (II)V");
        Class.forName("gm").getConstructor(int.class, int.class, int.class);
        checks++;
        // 3) mb=(dn,DDD)V 非历代 FFF（WorldOps spawn tnt fallback）
        Class mbC = Class.forName("mb");
        mbC.getConstructor(Class.forName("dn"), double.class, double.class, double.class);
        checks++;
        boolean noFFF = false;
        try { mbC.getConstructor(Class.forName("dn"), float.class, float.class, float.class); }
        catch (NoSuchMethodException e) { noFFF = true; }
        ok(noFFF, "mb no (dn,FFF)V");
        ok("(Ldn;DDD)V".equals(A2TableTest.str(A2TableTest.obj(targets.get("mobTntCtor")).get("descriptor"))),
                "mobTntCtor desc");
        // itemEntityCtor fh(dn,DDD,gm)V 实形存在
        ok("fh".equals(A2TableTest.str(A2TableTest.obj(targets.get("itemEntityCtor")).get("class"))), "itemEntityCtor class fh");
        Class.forName("fh").getConstructor(Class.forName("dn"), double.class, double.class,
                double.class, Class.forName("gm"));
        checks++;

        // mob 白名单 9 项对榫
        ok(t.mobClasses.size() == 9, "mobClasses 9");
        String[][] mobs = {
            {"pig", "qk", "mobPig"}, {"sheep", "ch", "mobSheep"}, {"creeper", "eh", "mobCreeper"},
            {"skeleton", "dy", "mobSkeleton"}, {"zombie", "pn", "mobZombie"}, {"spider", "bn", "mobSpider"},
            {"tnt", "mb", "mobTnt"}, {"giant", "jv", "mobGiant"}, {"mob", "fz", "mobCreatureClass"},
        };
        for (int i = 0; i < mobs.length; i++) {
            ok(mobs[i][1].equals(t.mobClasses.get(mobs[i][0])), "mob " + mobs[i][0]);
            ok(mobs[i][1].equals(A2TableTest.str(A2TableTest.obj(targets.get(mobs[i][2])).get("class"))),
                    "mob anchor " + mobs[i][0]);
        }
        // 未入白名单留档：cow=bb chicken=qs squid=rm（a1.2.0 三动物 id=91 怪癖修复批，详表 §4）
        ok("bb".equals(A2TableTest.str(A2TableTest.obj(targets.get("mobCow")).get("class"))), "mobCow bb record");
        ok("qs".equals(A2TableTest.str(A2TableTest.obj(targets.get("mobChicken")).get("class"))), "mobChicken qs record");
        ok("rm".equals(A2TableTest.str(A2TableTest.obj(targets.get("mobSquid")).get("class"))), "mobSquid rm record");

        // capabilities：29 OK / 7 unconfirmed（invulnerable 降档=本批 120→1202 唯一档位变化）
        int okN = 0;
        int unN = 0;
        for (Object v : caps.values()) {
            String s = A2TableTest.str(v);
            if ("OK".equals(s)) { okN++; }
            else if ("fingerprint-unconfirmed".equals(s)) { unN++; }
            else { throw new RuntimeException("bad cap value " + s); }
        }
        ok(okN == 29, "caps 29 OK got " + okN);
        ok(unN == 7, "caps 7 unconfirmed got " + unN);
        String[][] unPairs = {
            {"invulnerable", "iframesF"}, {"setmeta", "observersF"}, {"grow", "idCrops"},
            {"genscatter", "scatterClassG"}, {"getchest", "invSizeM"}, {"setchest", "invSizeM"},
            {"getfurnace", "teBurnF"},
        };
        for (int i = 0; i < unPairs.length; i++) {
            ok("fingerprint-unconfirmed".equals(A2TableTest.str(caps.get(unPairs[i][0]))), "unconfirmed " + unPairs[i][0]);
            ok(A2TableTest.slot(t, unPairs[i][1]) == null, unPairs[i][0] + " slot " + unPairs[i][1] + " null");
        }
        String[][] okPairs = {
            {"place", "setBlockM"}, {"dig", "setBlockM"}, {"getblock", "getBlockM"},
            {"getblocks", "getBlockM"}, {"fill", "setBlockM"}, {"replace", "getBlockM"},
            {"save", "saveWorldM"}, {"give", "addStackM"}, {"inv", "invF"},
            {"setarmor", "addStackM"}, {"clearinv", "invMainF"}, {"entitylist", "entityListM"},
            {"playsound", "playSoundM"}, {"getmeta", "getMetaM"}, {"settime", "worldTimeF"},
            {"setspawn", "spawnXF"}, {"getinfo", "difficultyF"}, {"getlight", "getLightM"},
            {"tp", "setPosRotM"}, {"setlook", "yawF"},
            {"setmotion", "motXF"}, {"getplayer", "healthF"}, {"sethealth", "healthF"},
            {"spawn", "spawnEntityM"}, {"drop", "spawnEntityM"}, {"explode", "explodeM"},
            {"clear", "setEntityDeadM"}, {"getbiome", "biomeWcmM"},
        };
        for (int i = 0; i < okPairs.length; i++) {
            ok("OK".equals(A2TableTest.str(caps.get(okPairs[i][0]))), "OK " + okPairs[i][0]);
            ok(A2TableTest.slot(t, okPairs[i][1]) != null, okPairs[i][0] + " slot " + okPairs[i][1] + " non-null");
        }
        ok("OK".equals(A2TableTest.str(caps.get("listops"))), "listops OK"); // 通道级，无表槽

        // null 槽断言（14 件=120 的 13 件 + iframesF 档位一）
        String[] nullSlots = {
            "light6M", "observersF", "scatterClassG", "entityTypeM", "invSizeM",
            "invGetM", "invSetM", "teBurnF", "teCookF", "idCrops",
            "idSapling", "cropsRipeMeta", "playerF", "iframesF",
        };
        for (int i = 0; i < nullSlots.length; i++) {
            ok(A2TableTest.slot(t, nullSlots[i]) == null, nullSlots[i] + " null");
        }

        System.out.println("A2Table1202Test PASS (" + checks + " checks)");
    }
}
