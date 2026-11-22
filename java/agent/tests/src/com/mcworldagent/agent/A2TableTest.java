// [B5-ARCHIVE 2026-11-22] headless test archived from %TEMP%\mcaut (source batch: A2 (0618/0629/1121)); run via java/agent/tests/run.ps1
package com.mcworldagent.agent;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A2 验收 1+2：build0618 逐字段对锚库 JSON（targets/confidence/capabilities 三面）
 * + capabilities 16 OK/19 unconfirmed 对 null 槽映射逐一对榫（spec §6.1/§6.2）。
 * 锚库三层嵌套超 MiniJson 深度限（协议面一层），本测试自带宽松递归 JSON 读。
 */
public class A2TableTest {

    static int checks = 0;

    static void ok(boolean c, String msg) {
        checks++;
        if (!c) {
            throw new AssertionError(msg);
        }
    }

    static Object slot(VersionTable t, String name) throws Exception {
        Field f = VersionTable.class.getDeclaredField(name);
        f.setAccessible(true);
        return f.get(t);
    }

    // ---------- 宽松 JSON ----------
    static Object parseJson(String s) {
        int[] p = {0};
        Object v = pv(s, p);
        ws(s, p);
        ok(p[0] == s.length(), "json trailing");
        return v;
    }

    static void ws(String s, int[] p) {
        while (p[0] < s.length() && Character.isWhitespace(s.charAt(p[0]))) {
            p[0]++;
        }
    }

    static Object pv(String s, int[] p) {
        ws(s, p);
        char c = s.charAt(p[0]);
        if (c == '{') {
            p[0]++;
            Map<String, Object> m = new HashMap<String, Object>();
            ws(s, p);
            if (s.charAt(p[0]) == '}') {
                p[0]++;
                return m;
            }
            while (true) {
                ws(s, p);
                ok(s.charAt(p[0]) == '"', "key quote");
                String k = (String) pstr(s, p);
                ws(s, p);
                ok(s.charAt(p[0]) == ':', "colon");
                p[0]++;
                m.put(k, pv(s, p));
                ws(s, p);
                char d = s.charAt(p[0]);
                if (d == ',') {
                    p[0]++;
                    continue;
                }
                ok(d == '}', "obj end");
                p[0]++;
                return m;
            }
        }
        if (c == '[') {
            p[0]++;
            List<Object> l = new ArrayList<Object>();
            ws(s, p);
            if (s.charAt(p[0]) == ']') {
                p[0]++;
                return l;
            }
            while (true) {
                l.add(pv(s, p));
                ws(s, p);
                char d = s.charAt(p[0]);
                if (d == ',') {
                    p[0]++;
                    continue;
                }
                ok(d == ']', "arr end");
                p[0]++;
                return l;
            }
        }
        if (c == '"') {
            return pstr(s, p);
        }
        if (s.startsWith("true", p[0])) {
            p[0] += 4;
            return Boolean.TRUE;
        }
        if (s.startsWith("false", p[0])) {
            p[0] += 5;
            return Boolean.FALSE;
        }
        if (s.startsWith("null", p[0])) {
            p[0] += 4;
            return null;
        }
        int st = p[0];
        while (p[0] < s.length() && "+-.eE0123456789".indexOf(s.charAt(p[0])) >= 0) {
            p[0]++;
        }
        return Double.valueOf(s.substring(st, p[0]));
    }

    static String pstr(String s, int[] p) {
        ok(s.charAt(p[0]) == '"', "str quote");
        p[0]++;
        StringBuilder b = new StringBuilder();
        while (s.charAt(p[0]) != '"') {
            char c = s.charAt(p[0]);
            if (c == '\\') {
                p[0]++;
                char e = s.charAt(p[0]);
                b.append(e == 'n' ? '\n' : e == 't' ? '\t' : e);
            } else {
                b.append(c);
            }
            p[0]++;
        }
        p[0]++;
        return b.toString();
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> obj(Object o) {
        return (Map<String, Object>) o;
    }

    static String str(Object o) {
        return o == null ? null : (String) o;
    }

    // ---------- 主流程 ----------
    public static void main(String[] a) throws Exception {
        String path = a.length > 0 ? a[0]
                : "E:/4k/github-projects/MCWorldAgent/java/agent/registry/fingerprints/inf-20100618.json";
        byte[] buf = java.nio.file.Files.readAllBytes(new java.io.File(path).toPath());
        Map<String, Object> j = obj(parseJson(new String(buf, "UTF-8")));

        VersionTable t = VersionRegistry.byJarSha1(str(j.get("jarSha1")));
        ok(t != null, "byJarSha1 finds 0618 table");
        ok(t.versionId.equals(str(j.get("versionId"))), "versionId");
        ok("G2".equals(t.family), "family G2");
        ok(str(j.get("jarSha1")).equals("89eab2c1a353707cc00f074dffba9cb7a4f5e304"), "anchor sha");
        ok(t.jarSha1.equals(str(j.get("jarSha1"))), "table jarSha1==anchor");

        Map<String, Object> targets = obj(j.get("targets"));

        // 消费槽：表值==锚库 name（+desc 对榫两处）
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
                {"invF", "invField", null},
                {"invMainF", "invMain", null},
                {"invArmorF", "invArmor", null},
                {"invCurF", "invCur", null},
                {"isCountF", "isCount", null},
                {"isIdF", "isId", null},
                {"isDmgF", "isDmg", null},
        };
        for (String[] c : consumed) {
            Map<String, Object> tg = obj(targets.get(c[1]));
            ok(tg != null, "anchor target " + c[1]);
            ok(str(tg.get("name")).equals(slot(t, c[0])), c[0] + " name");
            if (c[2] != null) {
                ok(str(tg.get("descriptor")).equals(slot(t, c[2])), c[2] + " desc");
            }
        }

        // EntityList 双轨批四槽（类名槽对榫 class 节，方法槽对榫 name 节）
        ok("j".equals(str(obj(targets.get("entityList")).get("name")))
                && "j".equals(slot(t, "entityListM")), "entityListM 对榫");
        ok("()Ljava/util/List;".equals(str(obj(targets.get("entityList")).get("descriptor"))),
                "entityList desc");
        // entityType rejected（el0618-1 活体翻案：o()=EntityLiving 皮肤路径 Y，贴图陷阱）
        // ——锚 target 留档但表槽必须 null，未注册件走类简名保底
        ok(obj(targets.get("entityType")) != null && slot(t, "entityTypeM") == null,
                "entityType rejected→槽 null");
        ok("net.minecraft.a.c.e".equals(str(obj(targets.get("entityListClass")).get("class")))
                && "net.minecraft.a.c.e".equals(slot(t, "entityListClassG")), "entityListClassG 对榫");
        ok("a".equals(str(obj(targets.get("entityListLookup")).get("name")))
                && "a".equals(slot(t, "entityListLookupM")), "entityListLookupM 对榫");
        ok("(Lnet/minecraft/a/c/b;)Ljava/lang/String;".equals(
                str(obj(targets.get("entityListLookup")).get("descriptor"))), "entityListLookup desc");
        // healthF 补钉批（2026-11-14 javap 三重实锤：NBT "Health" 读写配对+构造器默认 10）
        ok("net.minecraft.a.c.f".equals(str(obj(targets.get("health")).get("class")))
                && "Z".equals(str(obj(targets.get("health")).get("name")))
                && "I".equals(str(obj(targets.get("health")).get("descriptor")))
                && "Z".equals(slot(t, "healthF")), "health 对榫+槽已钉");

        // candidate 三件入表消费（判决：严格签名匹配即保底）——confidence 节独立
        Map<String, Object> conf = obj(j.get("confidence"));
        ok("candidate".equals(str(conf.get("saveWorld"))), "saveWorld candidate");
        ok("candidate".equals(str(conf.get("setPosRot"))), "setPosRot candidate");
        ok("candidate".equals(str(conf.get("invArmor"))), "invArmor candidate");
        ok("candidate".equals(str(conf.get("itemClass"))), "itemClass candidate");

        // 档位一 null 槽
        String[] nullSlots = {"iframesF", "addStackM", "itemClassG", "itemsF",
                "explodeM", "spawnEntityM", "setEntityDeadM", "entityTypeM",
                "playSoundM", "setMetaSilentM", "light6M", "getLightM", "observersF",
                "difficultyF", "scatterClassG", "invSizeM", "invGetM", "invSetM",
                "teBurnF", "teCookF", "itemEntityG", "idCrops", "idSapling", "cropsRipeMeta"};
        for (String s : nullSlots) {
            ok(slot(t, s) == null, s + " null");
        }
        ok(t.mobClasses.isEmpty(), "mobClasses empty");
        // itemClass candidate 不消费（spec §3 判决：addStackM null 连坐 give 面）
        ok(targets.get("itemClass") != null, "itemClass candidate exists in anchor");

        // capabilities 三面
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
        ok(okN == 19, "19 OK, got " + okN);
        ok(unN == 16, "16 unconfirmed, got " + unN);

        // unconfirmed ⇔ null 槽对榫（spec §3 表；getplayer/sethealth 已随 healthF 补钉转 OK）
        String[][] unPairs = {
                {"getinfo", "difficultyF"},
                {"give", "addStackM"}, {"setarmor", "addStackM"}, {"spawn", "spawnEntityM"},
                {"drop", "spawnEntityM"}, {"explode", "explodeM"}, {"invulnerable", "iframesF"},
                {"getchest", "invSizeM"}, {"setchest", "invSizeM"}, {"getfurnace", "invSizeM"},
                {"playsound", "playSoundM"},
                {"genscatter", "scatterClassG"}, {"getlight", "getLightM"},
                {"setmeta", "setMetaSilentM"}, {"grow", "idCrops"}, {"clear", "setEntityDeadM"},
        };
        ok(unPairs.length == 16, "16 pairs");
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
                {"entitylist", "entityListM"},
                {"getplayer", "healthF"}, {"sethealth", "healthF"},
        };
        for (String[] p : okPairs) {
            ok("OK".equals(str(caps.get(p[0]))), "OK " + p[0]);
            ok(slot(t, p[1]) != null, p[0] + " slot " + p[1] + " non-null");
        }
        ok("OK".equals(str(caps.get("listops"))), "listops OK"); // 通道级，无表槽

        // 0327 表新字段（A1 回归配套）
        VersionTable t327 = VersionRegistry.byJarSha1("47b1b32430a211520993552ba0a5e00c1af44724");
        ok(t327 != null && "inf-20100327".equals(t327.versionId), "0327 by sha");
        ok("(Z)V".equals(t327.saveWorldDesc), "0327 saveWorldDesc");
        ok("(DDDFF)V".equals(t327.setPosRotDesc), "0327 setPosRotDesc");

        System.out.println("A2TableTest PASS (" + checks + " checks)");
    }
}
