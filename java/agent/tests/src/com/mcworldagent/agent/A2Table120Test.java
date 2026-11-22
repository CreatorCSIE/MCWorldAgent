// [B5-ARCHIVE 2026-11-22] headless test archived from %TEMP%\mcaut (source batch: A2 a1.2.0); run via java/agent/tests/run.ps1
package com.mcworldagent.agent;

import java.util.Map;

public class A2Table120Test {
    static int checks = 0;
    static void ok(boolean c, String n) {
        checks++;
        if (!c) { System.out.println("FAIL: " + n); throw new RuntimeException("check failed: " + n); }
    }

    public static void main(String[] args) throws Exception {
        byte[] buf = java.nio.file.Files.readAllBytes(new java.io.File(
                "E:/4k/github-projects/MCWorldAgent/java/agent/registry/fingerprints/a1.2.0.json").toPath());
        Map j = A2TableTest.obj(A2TableTest.parseJson(new String(buf, "UTF-8")));
        String sha = "8632ea716fd083c2975f16d612306fd80bee46db";
        VersionTable bySha = VersionRegistry.byJarSha1(sha);
        ok(bySha != null && "a1.2.0".equals(bySha.versionId), "byJarSha1");
        ok(VersionRegistry.activate("a1.2.0"), "activate");
        VersionTable t = VersionRegistry.current();
        ok(t != null && "a1.2.0".equals(t.versionId), "current");
        ok("G3".equals(t.family), "family");
        Map targets = A2TableTest.obj(j.get("targets"));
        Map caps = A2TableTest.obj(j.get("capabilities"));

        // 消费槽位（42 槽 = 1121 的 39 + biome 三槽）：表值==锚库 name/desc 逐一对榫
        String[] consumed = {
            "setBlockM", "getBlockM", "getMetaM", "saveWorldM", "worldTimeF",
            "randSeedF", "spawnXF", "spawnYF", "spawnZF", "teGetM",
            "setPosRotM", "posXF", "posYF", "posZF", "motXF", "motYF", "motZF",
            "yawF", "pitchF", "healthF", "iframesF", "invF", "invMainF", "invArmorF",
            "invCurF", "isCountF", "isIdF", "isDmgF", "addStackM", "itemsF",
            "explodeM", "spawnEntityM", "setEntityDeadM", "entityListM", "playSoundM",
            "entityListLookupM", "difficultyF", "getLightM", "setMetaSilentM",
            "biomeWcmM", "biomeAtM", "biomeNameF",
        };
        String[] consumedAnchor = {
            "setBlock", "getBlockId", "getMeta", "saveWorld", "worldTime",
            "randSeed", "spawnX", "spawnY", "spawnZ", "teGet",
            "setPosRot", "posX", "posY", "posZ", "motX", "motY", "motZ",
            "yaw", "pitch", "health", "iframes", "invField", "invMain", "invArmor",
            "invCur", "isCount", "isId", "isDmg", "addStack", "itemRegistry",
            "explode", "spawnEntity", "setEntityDead", "entityList", "playSound",
            "entityListLookup", "difficulty", "getLight", "setMetaSilent",
            "getWorldChunkManager", "biomeAt", "biomeNameField",
        };
        ok(consumed.length == 42 && consumed.length == consumedAnchor.length, "consumed 42");
        for (int i = 0; i < consumed.length; i++) {
            Map tg = A2TableTest.obj(targets.get(consumedAnchor[i]));
            ok(tg != null, "anchor " + consumedAnchor[i]);
            ok(A2TableTest.str(tg.get("name")).equals(A2TableTest.slot(t, consumed[i])),
                    consumed[i] + "==" + consumedAnchor[i]);
        }
        // biome 三槽字母实钉（getbiome 批新增面）
        ok("a".equals(t.biomeWcmM), "biomeWcmM a");
        ok("a".equals(t.biomeAtM), "biomeAtM a");
        ok("m".equals(t.biomeNameF), "biomeNameF m");

        // 描述符对榫
        ok("(Lph;)V".equals(t.saveWorldDesc), "saveWorldDesc");
        ok("(DDDFF)V".equals(t.setPosRotDesc), "setPosRotDesc");
        ok("(Lph;)V".equals(A2TableTest.str(A2TableTest.obj(targets.get("saveWorld")).get("descriptor"))),
                "saveWorld desc anchor");
        ok("(DDDFF)V".equals(A2TableTest.str(A2TableTest.obj(targets.get("setPosRot")).get("descriptor"))),
                "setPosRot desc anchor");
        ok("(III)Liz;".equals(A2TableTest.str(A2TableTest.obj(targets.get("teGet")).get("descriptor"))),
                "teGet desc");
        ok("(Llk;)Z".equals(A2TableTest.str(A2TableTest.obj(targets.get("spawnEntity")).get("descriptor"))),
                "spawnEntity desc");
        ok("(Llk;)V".equals(A2TableTest.str(A2TableTest.obj(targets.get("setEntityDead")).get("descriptor"))),
                "setEntityDead desc");
        ok("()Ljava/util/List;".equals(A2TableTest.str(A2TableTest.obj(targets.get("entityList")).get("descriptor"))),
                "entityList desc");
        ok("(DDDLjava/lang/String;FF)V".equals(A2TableTest.str(A2TableTest.obj(targets.get("playSound")).get("descriptor"))),
                "playSound desc");
        ok("(Llk;DDDF)V".equals(A2TableTest.str(A2TableTest.obj(targets.get("explode")).get("descriptor"))),
                "explode desc");
        ok("(Llk;)Ljava/lang/String;".equals(A2TableTest.str(A2TableTest.obj(targets.get("entityListLookup")).get("descriptor"))),
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
        ok("[Ldr;".equals(A2TableTest.str(A2TableTest.obj(targets.get("itemRegistry")).get("descriptor"))),
                "itemRegistry desc");
        ok("Lfh;".equals(A2TableTest.str(A2TableTest.obj(targets.get("invField")).get("descriptor"))),
                "invField desc");
        ok("[Lfi;".equals(A2TableTest.str(A2TableTest.obj(targets.get("invMain")).get("descriptor"))),
                "invMain desc");
        ok("[Lfi;".equals(A2TableTest.str(A2TableTest.obj(targets.get("invArmor")).get("descriptor"))),
                "invArmor desc");
        ok("I".equals(A2TableTest.str(A2TableTest.obj(targets.get("isCount")).get("descriptor"))),
                "isCount desc");
        ok("I".equals(A2TableTest.str(A2TableTest.obj(targets.get("isId")).get("descriptor"))),
                "isId desc");
        ok("I".equals(A2TableTest.str(A2TableTest.obj(targets.get("isDmg")).get("descriptor"))),
                "isDmg desc");

        // 类面锚（worldClass/构造器、Entity 族、biome 三件）
        ok("cu".equals(A2TableTest.str(A2TableTest.obj(targets.get("worldClass")).get("class"))), "worldClass cu");
        ok("(Ljava/io/File;Ljava/lang/String;)V".equals(
                A2TableTest.str(A2TableTest.obj(targets.get("worldCtor")).get("descriptor"))), "worldCtor desc");
        ok("lk".equals(A2TableTest.str(A2TableTest.obj(targets.get("entityClass")).get("class"))), "entityClass lk");
        ok("lk".equals(t.entityClassG), "entityClassG");
        ok("iz".equals(A2TableTest.str(A2TableTest.obj(targets.get("tileEntityClass")).get("class"))), "tileEntity iz");
        ok("gx".equals(A2TableTest.str(A2TableTest.obj(targets.get("entityLivingClass")).get("class"))), "entityLiving gx");
        ok("dv".equals(A2TableTest.str(A2TableTest.obj(targets.get("entityPlayerClass")).get("class"))), "entityPlayer dv");
        ok("fh".equals(A2TableTest.str(A2TableTest.obj(targets.get("inventoryPlayerClass")).get("class"))), "inventoryPlayer fh");
        ok("fi".equals(A2TableTest.str(A2TableTest.obj(targets.get("itemStackClass")).get("class"))), "itemStack fi");
        ok("dr".equals(t.itemClassG), "itemClassG dr");
        ok("ei".equals(t.itemEntityG), "itemEntityG ei");
        ok("fj".equals(t.entityListClassG), "entityListClassG fj");

        // biome 三锚类面 + 假件链可解析性（cu.a()pb / pb.a(II)fy / fy.m String）
        ok("pb".equals(A2TableTest.str(A2TableTest.obj(targets.get("worldChunkManagerClass")).get("class"))), "wcm class pb");
        ok("fy".equals(A2TableTest.str(A2TableTest.obj(targets.get("biomeClass")).get("class"))), "biome class fy");
        ok("()Lpb;".equals(A2TableTest.str(A2TableTest.obj(targets.get("getWorldChunkManager")).get("descriptor"))), "biomeWcm desc");
        ok("(II)Lfy;".equals(A2TableTest.str(A2TableTest.obj(targets.get("biomeAt")).get("descriptor"))), "biomeAt desc");
        ok("Ljava/lang/String;".equals(A2TableTest.str(A2TableTest.obj(targets.get("biomeNameField")).get("descriptor"))), "biomeName desc");
        ok(Class.forName("cu").getMethod("a").getReturnType() == Class.forName("pb"), "fake cu.a()Lpb;");
        ok(Class.forName("pb").getMethod("a", int.class, int.class).getReturnType() == Class.forName("fy"), "fake pb.a(II)Lfy;");
        ok(Class.forName("fy").getField("m").getType() == String.class, "fake fy.m String");

        // playerField 删除面（120 无 EntityClientPlayerMP 等价物）：锚 null ⇔ 表槽 null
        ok(targets.get("playerField") == null, "anchor playerField null");
        ok(t.playerF == null, "playerF null");
        ok("d".equals(t.playerListF), "playerListF d");
        ok("Ljava/util/List;".equals(A2TableTest.str(A2TableTest.obj(targets.get("playerListField")).get("descriptor"))),
                "playerListField desc");
        // snowCovered 同批删除面：锚 null（VersionTable 无 snowCoveredF 字段，1121 先例同）
        ok(targets.get("snowCovered") == null, "anchor snowCovered null");

        // hook 面锚
        ok("net/minecraft/client/Minecraft".equals(A2TableTest.str(A2TableTest.obj(targets.get("minecraftClass")).get("class"))), "mc class");
        ok("e".equals(A2TableTest.str(A2TableTest.obj(targets.get("theWorldField")).get("name"))), "theWorld e");
        ok("Lcu;".equals(A2TableTest.str(A2TableTest.obj(targets.get("theWorldField")).get("descriptor"))), "theWorld desc");
        ok("g".equals(A2TableTest.str(A2TableTest.obj(targets.get("thePlayerField")).get("name"))), "thePlayer g");
        ok("Lbo;".equals(A2TableTest.str(A2TableTest.obj(targets.get("thePlayerField")).get("descriptor"))), "thePlayer desc");
        ok("u".equals(A2TableTest.str(A2TableTest.obj(targets.get("ingameGuiField")).get("name"))), "ingameGui u");
        ok("Lmz;".equals(A2TableTest.str(A2TableTest.obj(targets.get("ingameGuiField")).get("descriptor"))), "ingameGui desc");
        ok("a".equals(A2TableTest.str(A2TableTest.obj(targets.get("loadWorld")).get("name"))), "loadWorld a");
        ok("(Lcu;Ljava/lang/String;Ldv;)V".equals(A2TableTest.str(A2TableTest.obj(targets.get("loadWorld")).get("descriptor"))), "loadWorld desc（判决 B 移植批=3 参实体）");
        ok("i".equals(A2TableTest.str(A2TableTest.obj(targets.get("runTick")).get("name"))), "runTick i");
        ok("()V".equals(A2TableTest.str(A2TableTest.obj(targets.get("runTick")).get("descriptor"))), "runTick desc");
        ok("mz".equals(A2TableTest.str(A2TableTest.obj(targets.get("guiIngame")).get("class"))), "guiIngame mz");
        ok("a".equals(A2TableTest.str(A2TableTest.obj(targets.get("guiIngame")).get("name"))), "guiIngame a");
        ok("(FZII)V".equals(A2TableTest.str(A2TableTest.obj(targets.get("guiIngame")).get("descriptor"))), "guiIngame desc");
        ok("kg".equals(A2TableTest.str(A2TableTest.obj(targets.get("mobTntCtor")).get("class"))), "mobTntCtor kg");
        ok("(Lcu;FFF)V".equals(A2TableTest.str(A2TableTest.obj(targets.get("mobTntCtor")).get("descriptor"))), "mobTntCtor desc");
        // itemEntityCtor：JSON targets 行 descriptor "(Lcu;DDDLei;)V" 与其 provenance
        // "javap -p ei(cu,DDD,fi)V" 自相矛盾——本批对真 jar javap 实锤为 (cu,DDD,fi)V，
        // 即 targets 行 "Lei;" 系 "Lfi;" 笔误（数据面问题；runtime drop 面不消费该 descriptor 行，
        // WorldOps.drop 以 G_ITEM_ENTITY=ei + 反射 getConstructor(cu,DDD,fi) 实构）。
        // 此处按真值对榫假件构造器存在性；JSON 笔误只入报告，不改仓。
        ok("ei".equals(A2TableTest.str(A2TableTest.obj(targets.get("itemEntityCtor")).get("class"))), "itemEntityCtor class ei");
        ok("<init>".equals(A2TableTest.str(A2TableTest.obj(targets.get("itemEntityCtor")).get("name"))), "itemEntityCtor name");
        Class eiC = Class.forName("ei");
        eiC.getConstructor(Class.forName("cu"), double.class, double.class, double.class, Class.forName("fi"));
        checks++;

        // mob 白名单 9 项对榫
        ok(t.mobClasses.size() == 9, "mobClasses 9");
        String[][] mobs = {
            {"pig", "oc", "mobPig"}, {"sheep", "bu", "mobSheep"}, {"creeper", "dl", "mobCreeper"},
            {"skeleton", "dd", "mobSkeleton"}, {"zombie", "nh", "mobZombie"}, {"spider", "bc", "mobSpider"},
            {"tnt", "kg", "mobTnt"}, {"giant", "ig", "mobGiant"}, {"mob", "ex", "mobCreatureClass"},
        };
        for (int i = 0; i < mobs.length; i++) {
            ok(mobs[i][1].equals(t.mobClasses.get(mobs[i][0])), "mob " + mobs[i][0]);
            ok(mobs[i][1].equals(A2TableTest.str(A2TableTest.obj(targets.get(mobs[i][2])).get("class"))),
                    "mob anchor " + mobs[i][0]);
        }
        // 未入白名单仅留档：cow=aq chicken=oh
        ok("aq".equals(A2TableTest.str(A2TableTest.obj(targets.get("mobCow")).get("class"))), "mobCow aq record");
        ok("oh".equals(A2TableTest.str(A2TableTest.obj(targets.get("mobChicken")).get("class"))), "mobChicken oh record");

        // capabilities：30 OK / 6 unconfirmed（getbiome 新增后 OK=30）
        int okN = 0;
        int unN = 0;
        for (Object v : caps.values()) {
            String s = A2TableTest.str(v);
            if ("OK".equals(s)) { okN++; }
            else if ("fingerprint-unconfirmed".equals(s)) { unN++; }
            else { throw new RuntimeException("bad cap value " + s); }
        }
        ok(okN == 30, "caps 30 OK got " + okN);
        ok(unN == 6, "caps 6 unconfirmed got " + unN);
        String[][] unPairs = {
            {"setmeta", "observersF"}, {"grow", "idCrops"}, {"genscatter", "scatterClassG"},
            {"getchest", "invSizeM"}, {"setchest", "invSizeM"}, {"getfurnace", "teBurnF"},
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
            {"invulnerable", "iframesF"}, {"tp", "setPosRotM"}, {"setlook", "yawF"},
            {"setmotion", "motXF"}, {"getplayer", "healthF"}, {"sethealth", "healthF"},
            {"spawn", "spawnEntityM"}, {"drop", "spawnEntityM"}, {"explode", "explodeM"},
            {"clear", "setEntityDeadM"}, {"getbiome", "biomeWcmM"},
        };
        for (int i = 0; i < okPairs.length; i++) {
            ok("OK".equals(A2TableTest.str(caps.get(okPairs[i][0]))), "OK " + okPairs[i][0]);
            ok(A2TableTest.slot(t, okPairs[i][1]) != null, okPairs[i][0] + " slot " + okPairs[i][1] + " non-null");
        }
        ok("OK".equals(A2TableTest.str(caps.get("listops"))), "listops OK"); // 通道级，无表槽

        // null 槽断言（13 件；snowCoveredF 非 VersionTable 字段，删除面以锚 JSON null 断言）
        String[] nullSlots = {
            "light6M", "observersF", "scatterClassG", "entityTypeM", "invSizeM",
            "invGetM", "invSetM", "teBurnF", "teCookF", "idCrops",
            "idSapling", "cropsRipeMeta", "playerF",
        };
        for (int i = 0; i < nullSlots.length; i++) {
            ok(A2TableTest.slot(t, nullSlots[i]) == null, nullSlots[i] + " null");
        }

        System.out.println("A2Table120Test PASS (" + checks + " checks)");
    }
}
