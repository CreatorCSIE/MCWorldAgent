// [B5-ARCHIVE 2026-11-22] headless test archived from %TEMP%\mcaut (source batch: A2 (0618/0629/1121)); run via java/agent/tests/run.ps1
package com.mcworldagent.agent;

import java.util.Map;

/**
 * a1.1.2_01 接入批验收 1：build1121 逐字段对锚库 JSON（targets/confidence/capabilities
 * 三面）+ capabilities 29 OK/6 unconfirmed 对 null 槽映射逐一对榫（照 A2Table0629Test 结构）。
 * 本批三处新面对榫：档位三玩家回退（playerField=null ⇔ playerListF="k"+entityClassG="kh"）、
 * saveWorld=(Lnu;)V 包装（el1121-1 翻案：a(Z)Z=tick 本体）、setEntityDead 实例 d(kh)V；
 * 0618/0629 欠账本版转 OK：getinfo(difficultyF=l)、getlight(getLightM=j)、
 * invulnerable(iframesF=aW)、setmeta 随 observersF/light6M 未钉仍 unconfirmed（连坐判决）。
 */
public class A2Table1121Test {

    static int checks = 0;

    static void ok(boolean c, String msg) {
        checks++;
        if (!c) {
            throw new AssertionError(msg);
        }
    }

    static String str(Object o) {
        return A2TableTest.str(o);
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> obj(Object o) {
        return A2TableTest.obj(o);
    }

    static Object slot(VersionTable t, String name) throws Exception {
        return A2TableTest.slot(t, name);
    }

    static Map<String, Object> tgt(Map<String, Object> targets, String key) {
        Map<String, Object> tg = obj(targets.get(key));
        ok(tg != null, "anchor target " + key);
        return tg;
    }

    public static void main(String[] a) throws Exception {
        String path = a.length > 0 ? a[0]
                : "E:/4k/github-projects/MCWorldAgent/java/agent/registry/fingerprints/a1.1.2_01.json";
        byte[] buf = java.nio.file.Files.readAllBytes(new java.io.File(path).toPath());
        Map<String, Object> j = obj(A2TableTest.parseJson(new String(buf, "UTF-8")));

        VersionTable t = VersionRegistry.byJarSha1(str(j.get("jarSha1")));
        ok(t != null, "byJarSha1 finds 1121 table");
        ok(t.versionId.equals(str(j.get("versionId"))), "versionId");
        ok("a1.1.2_01".equals(t.versionId), "versionId literal");
        ok("G3".equals(t.family), "family G3");
        ok(str(j.get("jarSha1")).equals("daa4b9f192d2c260837d3b98c39432324da28e86"), "anchor sha");
        ok(t.jarSha1.equals(str(j.get("jarSha1"))), "table jarSha1==anchor");
        ok(VersionRegistry.activate("a1.1.2_01"), "activate by id");
        ok(VersionRegistry.current() == t, "current==1121 table");

        Map<String, Object> targets = obj(j.get("targets"));

        // 消费槽：表值==锚库 name（+desc 对榫：saveWorldDesc/setPosRotDesc）
        String[][] consumed = {
                {"setBlockM", "setBlock", null},
                {"getBlockM", "getBlockId", null},
                {"getMetaM", "getMeta", null},
                {"saveWorldM", "saveWorld", "saveWorldDesc"},
                {"worldTimeF", "worldTime", null},
                {"randSeedF", "randSeed", null},
                {"spawnXF", "spawnX", null},
                {"spawnYF", "spawnY", null},
                {"spawnZF", "spawnZ", null},
                {"teGetM", "teGet", null},
                {"setPosRotM", "setPosRot", "setPosRotDesc"},
                {"posXF", "posX", null},
                {"posYF", "posY", null},
                {"posZF", "posZ", null},
                {"motXF", "motX", null},
                {"motYF", "motY", null},
                {"motZF", "motZ", null},
                {"yawF", "yaw", null},
                {"pitchF", "pitch", null},
                {"healthF", "health", null},
                {"iframesF", "iframes", null},
                {"invF", "invField", null},
                {"invMainF", "invMain", null},
                {"invArmorF", "invArmor", null},
                {"invCurF", "invCur", null},
                {"isCountF", "isCount", null},
                {"isIdF", "isId", null},
                {"isDmgF", "isDmg", null},
                {"addStackM", "addStack", null},
                {"itemsF", "itemRegistry", null},
                {"explodeM", "explode", null},
                {"spawnEntityM", "spawnEntity", null},
                {"setEntityDeadM", "setEntityDead", null},
                {"entityListM", "entityList", null},
                {"playSoundM", "playSound", null},
                {"entityListLookupM", "entityListLookup", null},
                {"difficultyF", "difficulty", null},
                {"getLightM", "getLight", null},
                {"setMetaSilentM", "setMetaSilent", null},
        };
        ok(consumed.length == 39, "39 consumed slots");
        for (String[] c : consumed) {
            Map<String, Object> tg = tgt(targets, c[1]);
            ok(str(tg.get("name")).equals(slot(t, c[0])), c[0] + " name");
            if (c[2] != null) {
                ok(str(tg.get("descriptor")).equals(slot(t, c[2])), c[2] + " desc");
            }
        }

        // 档位三玩家面：playerField=null ⇔ playerF null；playerListF/entityClassG 新槽入对榫面
        ok(targets.get("playerField") == null, "anchor playerField null");
        ok(t.playerF == null, "playerF null (档位三)");
        Map<String, Object> pl = tgt(targets, "playerListField");
        ok("k".equals(str(pl.get("name"))) && "k".equals(slot(t, "playerListF")),
                "playerListF=k 对榫");
        ok("Ljava/util/List;".equals(str(pl.get("descriptor"))), "playerListField desc");
        ok("cn".equals(str(pl.get("class"))), "playerListField class=worldClass");
        Map<String, Object> ec = tgt(targets, "entityClass");
        ok("kh".equals(str(ec.get("class"))) && "kh".equals(slot(t, "entityClassG")),
                "entityClassG=kh 对榫");
        ok("confirmed".equals(str(obj(j.get("confidence")).get("playerListField"))),
                "playerListField confirmed");
        ok("confirmed".equals(str(obj(j.get("confidence")).get("entityClass"))),
                "entityClass confirmed");

        // 活体翻案面：saveWorld=a(Lnu;)V 包装（a(Z)Z=tick 本体，el1121-1）、spawnEntity 返 Z、setEntityDead 实例
        ok("(Lnu;)V".equals(t.saveWorldDesc), "saveWorldDesc (Lnu;)V");
        ok("confirmed".equals(str(obj(j.get("confidence")).get("saveWorld"))), "saveWorld confirmed");
        ok("(Lkh;)Z".equals(str(tgt(targets, "spawnEntity").get("descriptor"))),
                "spawnEntity (Lkh;)Z 档位二");
        ok("(Lkh;)V".equals(str(tgt(targets, "setEntityDead").get("descriptor"))),
                "setEntityDead (Lkh;)V 实例面（clear 收 world，详表 §2）");

        // 类形槽（G3 根类无包路径）+ health 三重钉（类节=ge、字段名 E、I）
        ok("di".equals(str(tgt(targets, "itemClass").get("class")))
                && "di".equals(slot(t, "itemClassG")), "itemClassG 对榫");
        ok("dx".equals(str(tgt(targets, "itemEntityClass").get("class")))
                && "dx".equals(slot(t, "itemEntityG")), "itemEntityG 对榫");
        ok("ew".equals(str(tgt(targets, "entityListClass").get("class")))
                && "ew".equals(slot(t, "entityListClassG")), "entityListClassG 对榫");
        ok("(Lkh;)Ljava/lang/String;".equals(str(tgt(targets, "entityListLookup").get("descriptor"))),
                "entityListLookup desc");
        Map<String, Object> h = tgt(targets, "health");
        ok("ge".equals(str(h.get("class"))) && "E".equals(str(h.get("name")))
                && "I".equals(str(h.get("descriptor"))) && "E".equals(slot(t, "healthF")),
                "health ge/E 对榫+槽已钉");
        Map<String, Object> ifr = tgt(targets, "iframes");
        ok("kh".equals(str(ifr.get("class"))) && "aW".equals(str(ifr.get("name")))
                && "aW".equals(slot(t, "iframesF")), "iframes kh/aW 对榫（0629 欠账本版钉）");

        // 钩面锚（表无槽，对榫 JSON；InjectSmoke1121Test 再对 HookTargets）
        ok("cn".equals(str(tgt(targets, "worldClass").get("class"))), "worldClass=cn");
        ok("(Ljava/io/File;Ljava/lang/String;)V".equals(
                str(tgt(targets, "worldCtor").get("descriptor"))), "worldCtor desc");
        ok("net/minecraft/client/Minecraft".equals(
                str(tgt(targets, "minecraftClass").get("class"))), "minecraftClass 未混淆");
        ok("(Lcn;Ljava/lang/String;)V".equals(str(tgt(targets, "loadWorld").get("descriptor"))),
                "loadWorld desc");
        ok("i".equals(str(tgt(targets, "runTick").get("name")))
                && "()V".equals(str(tgt(targets, "runTick").get("descriptor"))), "runTick i()V");
        ok("lu".equals(str(tgt(targets, "guiIngame").get("class")))
                && "(FZII)V".equals(str(tgt(targets, "guiIngame").get("descriptor"))),
                "guiIngame lu a(FZII)V 换形（a(F)V 失效）");
        ok("(Lcn;FFF)V".equals(str(tgt(targets, "mobTntCtor").get("descriptor"))), "mobTntCtor desc");
        ok("(Lcn;DDDLev;)V".equals(str(tgt(targets, "itemEntityCtor").get("descriptor"))),
                "itemEntityCtor desc");

        // candidate 判决面（表消费但 confidence 留档 candidate）
        Map<String, Object> conf = obj(j.get("confidence"));
        // 注：setBlock/spawnX 已在 ee92e39「活体闭环(el1121-1/2)」提交升为 confirmed，
        // 本测试件（%TEMP 产物）原断言系升档前旧态，按仓内权威 JSON 现值对榫。
        ok("confirmed".equals(str(conf.get("setBlock"))), "setBlock confirmed");
        ok("candidate".equals(str(conf.get("setBlockNoNotify"))), "setBlockNoNotify candidate");
        ok("confirmed".equals(str(conf.get("spawnX"))), "spawnX confirmed");
        ok("candidate".equals(str(conf.get("mobGiant"))), "mobGiant candidate");
        ok("confirmed".equals(str(conf.get("setMetaSilent"))), "setMetaSilent confirmed");
        ok("confirmed".equals(str(conf.get("getLight"))), "getLight confirmed");
        ok("confirmed".equals(str(conf.get("difficulty"))), "difficulty confirmed");

        // spawn 白名单 9 件（mob=ek=EntityCreature——0327 同语义层，非 0629 EntityMonster 漂移；
        // cow=am/chicken=mz JSON 留档不入白名单）
        String[][] mobs = {
                {"pig", "mobPig", "mv"}, {"sheep", "mobSheep", "bo"},
                {"creeper", "mobCreeper", "dd"}, {"skeleton", "mobSkeleton", "cw"},
                {"zombie", "mobZombie", "mb"}, {"spider", "mobSpider", "ax"},
                {"tnt", "mobTnt", "jd"}, {"giant", "mobGiant", "hl"},
                {"mob", "mobCreatureClass", "ek"},
        };
        ok(t.mobClasses.size() == 9, "mobClasses 9, got " + t.mobClasses.size());
        for (String[] m : mobs) {
            ok(m[2].equals(str(tgt(targets, m[1]).get("class"))), "anchor " + m[1]);
            ok(m[2].equals(t.mobClasses.get(m[0])), "mobClasses " + m[0]);
        }
        ok("am".equals(str(tgt(targets, "mobCow").get("class")))
                && !t.mobClasses.containsKey("cow"), "mobCow JSON 留档不入白名单");
        ok("mz".equals(str(tgt(targets, "mobChicken").get("class")))
                && !t.mobClasses.containsKey("chicken"), "mobChicken 同上");

        // 档位一 null 槽 13 件（playerF 单列于档位三面）
        String[] nullSlots = {"light6M", "observersF", "scatterClassG", "entityTypeM",
                "invSizeM", "invGetM", "invSetM", "teBurnF", "teCookF",
                "idCrops", "idSapling", "cropsRipeMeta", "playerF"};
        for (String s : nullSlots) {
            ok(slot(t, s) == null, s + " null");
        }

        // capabilities 三面计数
        Map<String, Object> caps = obj(j.get("capabilities"));
        int okN = 0;
        int unN = 0;
        for (Map.Entry<String, Object> e : caps.entrySet()) {
            String v = str(e.getValue());
            if ("OK".equals(v)) {
                okN++;
            } else if ("fingerprint-unconfirmed".equals(v)) {
                unN++;
            } else {
                throw new AssertionError("bad cap " + e.getKey() + "=" + v);
            }
        }
        ok(okN == 29, "29 OK, got " + okN);
        ok(unN == 6, "6 unconfirmed, got " + unN);

        // unconfirmed ⇔ null 槽对榫（setmeta 连坐=observersF 非 setMetaSilentM——本批已钉 b(IIII)V，
        // 通知段未钉才是响亮根因；grow 连坐 idCrops）
        String[][] unPairs = {
                {"getchest", "invSizeM"}, {"setchest", "invSizeM"}, {"getfurnace", "teBurnF"},
                {"genscatter", "scatterClassG"}, {"setmeta", "observersF"}, {"grow", "idCrops"},
        };
        ok(unPairs.length == 6, "6 pairs");
        for (String[] p : unPairs) {
            ok("fingerprint-unconfirmed".equals(str(caps.get(p[0]))), "unconfirmed " + p[0]);
            ok(slot(t, p[1]) == null, p[0] + " slot " + p[1] + " null");
        }

        // OK ⇔ 依赖槽非 null（getinfo/getlight/invulnerable 三件=0618/0629 欠账本版转 OK）
        String[][] okPairs = {
                {"place", "setBlockM"}, {"dig", "setBlockM"}, {"getblock", "getBlockM"},
                {"getblocks", "getBlockM"}, {"fill", "setBlockM"}, {"replace", "getBlockM"},
                {"getmeta", "getMetaM"}, {"settime", "worldTimeF"}, {"setspawn", "spawnXF"},
                {"setlook", "yawF"}, {"setmotion", "motXF"}, {"tp", "setPosRotM"},
                {"inv", "invF"}, {"clearinv", "invMainF"}, {"save", "saveWorldM"},
                {"entitylist", "entityListM"}, {"getplayer", "healthF"}, {"sethealth", "healthF"},
                {"give", "addStackM"}, {"setarmor", "addStackM"},
                {"spawn", "spawnEntityM"}, {"drop", "spawnEntityM"}, {"explode", "explodeM"},
                {"playsound", "playSoundM"}, {"clear", "setEntityDeadM"},
                {"getinfo", "difficultyF"}, {"getlight", "getLightM"}, {"invulnerable", "iframesF"},
        };
        ok(okPairs.length == 28, "28 okPairs");
        for (String[] p : okPairs) {
            ok("OK".equals(str(caps.get(p[0]))), "OK " + p[0]);
            ok(slot(t, p[1]) != null, p[0] + " slot " + p[1] + " non-null");
        }
        ok("OK".equals(str(caps.get("listops"))), "listops OK"); // 通道级，无表槽

        System.out.println("A2Table1121Test PASS (" + checks + " checks)");
    }
}
