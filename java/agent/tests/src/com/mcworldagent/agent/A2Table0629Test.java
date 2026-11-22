// [B5-ARCHIVE 2026-11-22] headless test archived from %TEMP%\mcaut (source batch: A2 (0618/0629/1121)); run via java/agent/tests/run.ps1
package com.mcworldagent.agent;

import java.util.Map;

/**
 * 0629 接入批验收 1：build0629 逐字段对锚库 JSON（targets/confidence/capabilities
 * 三面）+ capabilities 26 OK/9 unconfirmed 对 null 槽映射逐一对榫（spec §6.1/§6.2）。
 * 宽松递归 JSON 读复用 A2TableTest 解析器（同包，包级 static）。
 * 本批两处翻案入对账：saveWorld=(Liw;)V confirmed（旧详表 a(Z)Z 作废）+
 * saveWorldBody=(ZLiw;)V（钩挂私有本体）；getinfo 随 difficultyF 未钉归 unconfirmed
 * （WorldOps require 实码为准，锚 JSON 曾误标 OK——本测试即抓出该对账）。
 */
public class A2Table0629Test {

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
                : "E:/4k/github-projects/MCWorldAgent/java/agent/registry/fingerprints/inf-20100629.json";
        byte[] buf = java.nio.file.Files.readAllBytes(new java.io.File(path).toPath());
        Map<String, Object> j = obj(A2TableTest.parseJson(new String(buf, "UTF-8")));

        VersionTable t = VersionRegistry.byJarSha1(str(j.get("jarSha1")));
        ok(t != null, "byJarSha1 finds 0629 table");
        ok(t.versionId.equals(str(j.get("versionId"))), "versionId");
        ok("G3".equals(t.family), "family G3");
        ok(str(j.get("jarSha1")).equals("eb50bce3cb542488b3039aa0f4c3c0ec7595ab24"), "anchor sha");
        ok(t.jarSha1.equals(str(j.get("jarSha1"))), "table jarSha1==anchor");

        Map<String, Object> targets = obj(j.get("targets"));

        // 消费槽：表值==锚库 name（+desc 对榫两处：saveWorldDesc/setPosRotDesc）
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
                {"playerF", "playerField", null},
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
        };
        for (String[] c : consumed) {
            Map<String, Object> tg = tgt(targets, c[1]);
            ok(str(tg.get("name")).equals(slot(t, c[0])), c[0] + " name");
            if (c[2] != null) {
                ok(str(tg.get("descriptor")).equals(slot(t, c[2])), c[2] + " desc");
            }
        }

        // 翻案①：saveWorld 包装 (Liw;)V confirmed + 私有本体 (ZLiw;)V（钩挂本体，表无槽）
        ok("(Liw;)V".equals(t.saveWorldDesc), "saveWorldDesc (Liw;)V");
        ok("confirmed".equals(str(obj(j.get("confidence")).get("saveWorld"))), "saveWorld confirmed");
        Map<String, Object> body = tgt(targets, "saveWorldBody");
        ok("a".equals(str(body.get("name"))) && "(ZLiw;)V".equals(str(body.get("descriptor"))),
                "saveWorldBody (ZLiw;)V");
        ok("confirmed".equals(str(obj(j.get("confidence")).get("saveWorldBody"))),
                "saveWorldBody confirmed");

        // 类形槽（G3 根类无包路径）：对榫锚 class 节
        ok("fz".equals(str(tgt(targets, "itemClass").get("class")))
                && "fz".equals(slot(t, "itemClassG")), "itemClassG 对榫");
        ok("gt".equals(str(tgt(targets, "itemEntityClass").get("class")))
                && "gt".equals(slot(t, "itemEntityG")), "itemEntityG 对榫");
        ok("ht".equals(str(tgt(targets, "entityListClass").get("class")))
                && "ht".equals(slot(t, "entityListClassG")), "entityListClassG 对榫");
        ok("(Lem;)Ljava/lang/String;".equals(str(tgt(targets, "entityListLookup").get("descriptor"))),
                "entityListLookup desc");

        // health：jv.ab 三重实锤（类节=jv，字段名双字母 ab，档位一补钉形态）
        Map<String, Object> h = tgt(targets, "health");
        ok("jv".equals(str(h.get("class"))) && "ab".equals(str(h.get("name")))
                && "I".equals(str(h.get("descriptor"))) && "ab".equals(slot(t, "healthF")),
                "health jv/ab 对榫+槽已钉");

        // entityType rejected（em.r()=基类 aconst_null 死桩，0618 判决同族）——
        // 锚 target 留档但表槽必须 null，未注册件走类简名保底
        ok(tgt(targets, "entityType") != null && slot(t, "entityTypeM") == null,
                "entityType rejected→槽 null");
        ok("rejected".equals(str(obj(j.get("confidence")).get("entityType"))),
                "entityType confidence rejected");

        // setBlockSilent candidate 不消费（表无对应槽）
        ok("candidate".equals(str(obj(j.get("confidence")).get("setBlockSilent"))),
                "setBlockSilent candidate");

        // spawn 白名单 9 件（mob=gl=EntityMonster——档位三语义漂移候选，活体定案）
        String[][] mobs = {
                {"pig", "mobPig", "hx"}, {"sheep", "mobSheep", "dk"},
                {"creeper", "mobCreeper", "ft"}, {"skeleton", "mobSkeleton", "fj"},
                {"zombie", "mobZombie", "gr"}, {"spider", "mobSpider", "cf"},
                {"tnt", "mobTnt", "cy"}, {"giant", "mobGiant", "aa"},
                {"mob", "mobMonsterClass", "gl"},
        };
        ok(t.mobClasses.size() == 9, "mobClasses 9, got " + t.mobClasses.size());
        for (String[] m : mobs) {
            ok(m[2].equals(str(tgt(targets, m[1]).get("class"))), "anchor " + m[1]);
            ok(m[2].equals(t.mobClasses.get(m[0])), "mobClasses " + m[0]);
        }

        // 档位一 null 槽 16 件
        String[] nullSlots = {"iframesF", "entityTypeM", "setMetaSilentM", "light6M",
                "getLightM", "observersF", "difficultyF", "scatterClassG",
                "invSizeM", "invGetM", "invSetM", "teBurnF", "teCookF",
                "idCrops", "idSapling", "cropsRipeMeta"};
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
        ok(okN == 26, "26 OK, got " + okN);
        ok(unN == 9, "9 unconfirmed, got " + unN);

        // unconfirmed ⇔ null 槽对榫（getinfo 随 difficultyF——本批 JSON 曾误标 OK，对账纠正）
        String[][] unPairs = {
                {"getinfo", "difficultyF"},
                {"invulnerable", "iframesF"},
                {"getchest", "invSizeM"}, {"setchest", "invSizeM"}, {"getfurnace", "teBurnF"},
                {"genscatter", "scatterClassG"}, {"getlight", "getLightM"},
                {"setmeta", "setMetaSilentM"}, {"grow", "idCrops"},
        };
        ok(unPairs.length == 9, "9 pairs");
        for (String[] p : unPairs) {
            ok("fingerprint-unconfirmed".equals(str(caps.get(p[0]))), "unconfirmed " + p[0]);
            ok(slot(t, p[1]) == null, p[0] + " slot " + p[1] + " null");
        }

        // OK ⇔ 依赖槽非 null
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
        };
        ok(okPairs.length == 25, "25 okPairs");
        for (String[] p : okPairs) {
            ok("OK".equals(str(caps.get(p[0]))), "OK " + p[0]);
            ok(slot(t, p[1]) != null, p[0] + " slot " + p[1] + " non-null");
        }
        ok("OK".equals(str(caps.get("listops"))), "listops OK"); // 通道级，无表槽

        System.out.println("A2Table0629Test PASS (" + checks + " checks)");
    }
}
