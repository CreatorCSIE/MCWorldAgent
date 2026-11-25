// [GUARD 2026-11-25] headless guard batch; run via java/agent/tests/run.ps1
package com.mcworldagent.agent;

import java.io.File;
import java.io.FileInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * 守卫批（2026-11-25 自主窗口①）：把"产线宽容"关进笼子。
 * A. FingerprintLoader strict 入口=标记件坏必抛；生产 loadAll 宽容语义不变。
 * B. 13 JSON 闸：registry/fingerprints 全量盘点（件数/宽松 JSON 合法性/
 *    标记分布=2/标记件 buildOrThrow 全过/非标记件必拒 schema）。
 * C. buildOrThrow 负例十四连（unknown slot、sha 形态、钩六元组、mode 白名单…）。
 * D. MiniJson 边界断言：MAX_STRING=256（解码后）、深度≤2、整数子集、
 *    null/数组/浮点/重复键/尾随/控制符/转义/4096 顶限/Long 溢出。
 * E. 信封 fuzz：CommandChannel.validate 拒绝面（协议版本/id 形态/op 已知/
 *    args 形态/targetGeneration）+ 真实 JSON 端到端。
 * F. KNOWN_OPS ↔ LISTOPS 自表面一致性（防"同步维护"注释失守）。
 * args[0]=registry/fingerprints 目录（必填，不存在=FAIL）。
 */
public final class GuardTest {

    private static int checks = 0;
    private static int fails = 0;

    private static void eq(String what, Object expected, Object actual) {
        checks++;
        boolean ok = expected == null ? actual == null : expected.equals(actual);
        if (!ok) {
            fails++;
            System.out.println("FAIL " + what + " expected=" + expected + " actual=" + actual);
        }
    }

    private static void ok(String what, boolean cond) {
        checks++;
        if (!cond) {
            fails++;
            System.out.println("FAIL " + what);
        }
    }

    /** cond 必须抛异常才算过（负例断言）。 */
    private static void throwsEx(String what, Runnable r) {
        checks++;
        try {
            r.run();
            fails++;
            System.out.println("FAIL " + what + " (no throw)");
        } catch (RuntimeException expected) {
            // pass
        }
    }

    public static void main(String[] args) throws Exception {
        File fpDir = args.length > 0 ? new File(args[0]) : null;
        ok("fingerprints dir exists", fpDir != null && fpDir.isDirectory());
        if (fpDir == null || !fpDir.isDirectory()) {
            System.out.println("GuardTest ABORT: no fingerprints dir arg");
            System.exit(1);
        }

        // ---------- A. strict 装载入口 ----------
        List<VersionTable> strict = FingerprintLoader.loadAllStrict();
        eq("strict size", Integer.valueOf(2), Integer.valueOf(strict.size()));
        Set<String> ids = new HashSet<String>();
        for (VersionTable t : strict) {
            ids.add(t.versionId);
        }
        eq("strict ids", "[a1.0.3, inf-20100624]", String.valueOf(new TreeSet<String>(ids)));
        // strict 与宽容路径同果（无静默差异）
        List<VersionTable> lenient = FingerprintLoader.loadAll();
        eq("lenient size", Integer.valueOf(2), Integer.valueOf(lenient.size()));

        // ---------- B. 16 JSON 闸（扩版首波 +3 锚：b1.5_01/b1.9-pre2/b1.3_01） ----------
        File[] all = fpDir.listFiles();
        List<File> jsons = new ArrayList<File>();
        for (File f : all) {
            if (f.isFile() && f.getName().endsWith(".json")) {
                jsons.add(f);
            }
        }
        eq("json count", Integer.valueOf(17), Integer.valueOf(jsons.size()));
        int marked = 0;
        for (File f : jsons) {
            String text = readUtf8(f);
            ok("lenient-json " + f.getName(), isJson(text));
            boolean isRt = text.indexOf(FingerprintLoader.RUNTIME_SCHEMA) >= 0;
            if (isRt) {
                marked++;
                VersionTable t = FingerprintLoader.buildOrThrow("fingerprints/" + f.getName(), text);
                ok("runtime builds " + f.getName(), t != null);
            } else {
                throwsEx("anchor rejected " + f.getName(),
                        new RunnableBuilder(f, text));
            }
        }
        eq("runtime-marked count", Integer.valueOf(2), Integer.valueOf(marked));

        // ---------- C. buildOrThrow 负例十四连 ----------
        String good = "{\"schema\":\"mca-runtime-v1\",\"versionId\":\"zz\",\"family\":\"G9\","
                + "\"jarSha1\":\"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\","
                + "\"ops\":{\"setBlockM\":\"c\"}}";
        VersionTable g = FingerprintLoader.buildOrThrow("fingerprints/zz.json", good);
        ok("positive control builds", g != null && "c".equals(g.setBlockM));
        throwsEx("schema mismatch", new Runnable() {
            public void run() {
                FingerprintLoader.buildOrThrow("fingerprints/zz.json",
                        "{\"schema\":\"mca-fingerprint-v1\",\"versionId\":\"zz\","
                        + "\"family\":\"G9\",\"jarSha1\":\"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\","
                        + "\"ops\":{}}");
            }
        });
        throwsEx("missing versionId", new Runnable() {
            public void run() {
                FingerprintLoader.buildOrThrow("fingerprints/zz.json",
                        "{\"schema\":\"mca-runtime-v1\",\"family\":\"G9\","
                        + "\"jarSha1\":\"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\",\"ops\":{}}");
            }
        });
        throwsEx("sha not 40-hex", new Runnable() {
            public void run() {
                FingerprintLoader.buildOrThrow("fingerprints/zz.json",
                        "{\"schema\":\"mca-runtime-v1\",\"versionId\":\"zz\",\"family\":\"G9\","
                        + "\"jarSha1\":\"ABC123\",\"ops\":{}}");
            }
        });
        throwsEx("name != versionId", new Runnable() {
            public void run() {
                FingerprintLoader.buildOrThrow("fingerprints/other.json", good);
            }
        });
        throwsEx("ops missing", new Runnable() {
            public void run() {
                FingerprintLoader.buildOrThrow("fingerprints/zz.json",
                        "{\"schema\":\"mca-runtime-v1\",\"versionId\":\"zz\",\"family\":\"G9\","
                        + "\"jarSha1\":\"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\"}");
            }
        });
        throwsEx("unknown slot", new Runnable() {
            public void run() {
                FingerprintLoader.buildOrThrow("fingerprints/zz.json",
                        "{\"schema\":\"mca-runtime-v1\",\"versionId\":\"zz\",\"family\":\"G9\","
                        + "\"jarSha1\":\"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\","
                        + "\"ops\":{\"setBlokcM\":\"c\"}}");
            }
        });
        throwsEx("identity slot in ops", new Runnable() {
            public void run() {
                FingerprintLoader.buildOrThrow("fingerprints/zz.json",
                        "{\"schema\":\"mca-runtime-v1\",\"versionId\":\"zz\",\"family\":\"G9\","
                        + "\"jarSha1\":\"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\","
                        + "\"ops\":{\"versionId\":\"zz\"}}");
            }
        });
        throwsEx("string slot wants string", new Runnable() {
            public void run() {
                FingerprintLoader.buildOrThrow("fingerprints/zz.json",
                        "{\"schema\":\"mca-runtime-v1\",\"versionId\":\"zz\",\"family\":\"G9\","
                        + "\"jarSha1\":\"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\","
                        + "\"ops\":{\"setBlockM\":5}}");
            }
        });
        throwsEx("int slot wants int", new Runnable() {
            public void run() {
                FingerprintLoader.buildOrThrow("fingerprints/zz.json",
                        "{\"schema\":\"mca-runtime-v1\",\"versionId\":\"zz\",\"family\":\"G9\","
                        + "\"jarSha1\":\"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\","
                        + "\"ops\":{\"idCrops\":\"5\"}}");
            }
        });
        throwsEx("bool slot wants bool", new Runnable() {
            public void run() {
                FingerprintLoader.buildOrThrow("fingerprints/zz.json",
                        "{\"schema\":\"mca-runtime-v1\",\"versionId\":\"zz\",\"family\":\"G9\","
                        + "\"jarSha1\":\"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\","
                        + "\"ops\":{\"hudXyzPaint\":\"true\"}}");
            }
        });
        throwsEx("mob wants string", new Runnable() {
            public void run() {
                FingerprintLoader.buildOrThrow("fingerprints/zz.json",
                        "{\"schema\":\"mca-runtime-v1\",\"versionId\":\"zz\",\"family\":\"G9\","
                        + "\"jarSha1\":\"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\","
                        + "\"ops\":{},\"mobs\":{\"pig\":1}}");
            }
        });
        throwsEx("hook 5-field", new Runnable() {
            public void run() {
                FingerprintLoader.buildOrThrow("fingerprints/zz.json",
                        "{\"schema\":\"mca-runtime-v1\",\"versionId\":\"zz\",\"family\":\"G9\","
                        + "\"jarSha1\":\"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\","
                        + "\"ops\":{},\"hooks\":{\"world.tick\":\"a|b|V|1|1\"}}");
            }
        });
        throwsEx("hook unknown key", new Runnable() {
            public void run() {
                FingerprintLoader.buildOrThrow("fingerprints/zz.json",
                        "{\"schema\":\"mca-runtime-v1\",\"versionId\":\"zz\",\"family\":\"G9\","
                        + "\"jarSha1\":\"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\","
                        + "\"ops\":{},\"hooks\":{\"no.such.hook\":\"a|b|(I)V|1|1|enter\"}}");
            }
        });
        throwsEx("hook bad mode", new Runnable() {
            public void run() {
                FingerprintLoader.buildOrThrow("fingerprints/zz.json",
                        "{\"schema\":\"mca-runtime-v1\",\"versionId\":\"zz\",\"family\":\"G9\","
                        + "\"jarSha1\":\"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\","
                        + "\"ops\":{},\"hooks\":{\"world.tick\":\"a|b|(I)V|1|1|sideways\"}}");
            }
        });
        throwsEx("hook access non-int", new Runnable() {
            public void run() {
                FingerprintLoader.buildOrThrow("fingerprints/zz.json",
                        "{\"schema\":\"mca-runtime-v1\",\"versionId\":\"zz\",\"family\":\"G9\","
                        + "\"jarSha1\":\"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\","
                        + "\"ops\":{},\"hooks\":{\"world.tick\":\"a|b|(I)V|public|1|enter\"}}");
            }
        });

        // ---------- D. MiniJson 边界断言 ----------
        StringBuilder sb256 = new StringBuilder();
        for (int i = 0; i < 256; i++) {
            sb256.append('x');
        }
        Map<String, Object> m256 = MiniJson.parse("{\"k\":\"" + sb256 + "\"}");
        eq("string len 256 ok", Integer.valueOf(256),
                Integer.valueOf(((String) m256.get("k")).length()));
        throwsEx("string len 257 reject", new Runnable() {
            public void run() {
                StringBuilder b = new StringBuilder();
                for (int i = 0; i < 257; i++) {
                    b.append('x');
                }
                MiniJson.parse("{\"k\":\"" + b + "\"}");
            }
        });
        // 256 是解码后长度：转义展开同样计入（\\u 展开 1 字符）
        StringBuilder esc = new StringBuilder();
        for (int i = 0; i < 128; i++) {
            esc.append("\\u0041"); // 解码后 128 字符
        }
        for (int i = 128; i < 257; i++) {
            esc.append('y');
        }
        throwsEx("decoded 257 via escapes reject", new Runnable() {
            public void run() {
                StringBuilder b = new StringBuilder();
                for (int i = 0; i < 128; i++) {
                    b.append("\\u0041");
                }
                for (int i = 128; i < 257; i++) {
                    b.append('y');
                }
                MiniJson.parse("{\"k\":\"" + b + "\"}");
            }
        });
        ok("escapes decode", "Ay".equals(String.valueOf(
                MiniJson.parse("{\"k\":\"\\u0041y\"}").get("k"))));
        Map<String, Object> nest = MiniJson.parse("{\"a\":{\"b\":1}}");
        ok("depth 2 ok", nest.get("a") instanceof Map);
        throwsEx("depth 3 reject", new Runnable() {
            public void run() {
                MiniJson.parse("{\"a\":{\"b\":{\"c\":1}}}");
            }
        });
        throwsEx("duplicate key reject", new Runnable() {
            public void run() {
                MiniJson.parse("{\"a\":1,\"a\":2}");
            }
        });
        throwsEx("float reject", new Runnable() {
            public void run() {
                MiniJson.parse("{\"a\":1.5}");
            }
        });
        throwsEx("exponent reject", new Runnable() {
            public void run() {
                MiniJson.parse("{\"a\":1e3}");
            }
        });
        throwsEx("array reject", new Runnable() {
            public void run() {
                MiniJson.parse("{\"a\":[1,2]}");
            }
        });
        throwsEx("null reject", new Runnable() {
            public void run() {
                MiniJson.parse("{\"a\":null}");
            }
        });
        throwsEx("trailing reject", new Runnable() {
            public void run() {
                MiniJson.parse("{\"a\":1}x");
            }
        });
        throwsEx("bad escape reject", new Runnable() {
            public void run() {
                MiniJson.parse("{\"a\":\"\\q\"}");
            }
        });
        throwsEx("raw control reject", new Runnable() {
            public void run() {
                MiniJson.parse("{\"a\":\"x\ty\"}");
            }
        });
        throwsEx("oversize 4097 reject", new Runnable() {
            public void run() {
                StringBuilder b = new StringBuilder("{\"k\":\"");
                while (b.length() <= 4096) {
                    b.append('z');
                }
                b.append("\"}");
                MiniJson.parse(b.toString());
            }
        });
        // 顶限内 4096 字符文档必须通过（唯一长键拼装，避开 MAX_STRING/重复键）
        StringBuilder b = new StringBuilder("{\"a\":1");
        int idx = 0;
        while (4095 - b.length() >= 261) { // 条目=","+键+":1"，键 256 → 261
            b.append(",\"").append(padKey(idx++)).append("\":1");
        }
        int finalKey = 4095 - b.length() - 5; // 先算后拼（链式 append 中 length() 会被前缀污染）
        b.append(",\"").append(repeat("m", finalKey)).append("\":1").append('}');
        final String doc4096 = b.toString();
        eq("size 4096 boundary ok", Integer.valueOf(4096), Integer.valueOf(doc4096.length()));
        ok("doc4096 parses", !MiniJson.parse(doc4096).isEmpty());
        throwsEx("long overflow reject", new Runnable() {
            public void run() {
                MiniJson.parse("{\"a\":9223372036854775808}");
            }
        });
        eq("negative number", Long.valueOf(-7),
                MiniJson.parse("{\"a\":-7}").get("a"));
        eq("bool literals", Boolean.TRUE, MiniJson.parse("{\"a\":true}").get("a"));
        eq("false literal", Boolean.FALSE, MiniJson.parse("{\"a\":false}").get("a"));
        throwsEx("top non-object reject", new Runnable() {
            public void run() {
                MiniJson.parse("5");
            }
        });
        throwsEx("empty reject", new Runnable() {
            public void run() {
                MiniJson.parse("");
            }
        });
        ok("empty object ok", MiniJson.parse("{}").isEmpty());

        // ---------- E. 信封 fuzz ----------
        Map<String, Object> env = new HashMap<String, Object>();
        env.put("protocolVersion", Long.valueOf(1));
        env.put("id", "1717000000123-a1b2");
        env.put("op", "place");
        eq("valid envelope pass", null, CommandChannel.validate(env));
        eq("proto missing", "protocolVersion must be integer",
                CommandChannel.validate(sub(env, "protocolVersion", null)));
        eq("proto string", "protocolVersion must be integer",
                CommandChannel.validate(sub(env, "protocolVersion", "1")));
        eq("proto 2", "unsupported protocolVersion 2 (agent speaks 1)",
                CommandChannel.validate(sub(env, "protocolVersion", Long.valueOf(2))));
        eq("proto 0", "unsupported protocolVersion 0 (agent speaks 1)",
                CommandChannel.validate(sub(env, "protocolVersion", Long.valueOf(0))));
        eq("id missing", "id must be string", CommandChannel.validate(sub(env, "id", null)));
        eq("id 65ch", "bad id (charset/length)",
                CommandChannel.validate(sub(env, "id", repeat("i", 65))));
        eq("id dot", "bad id (charset/length)",
                CommandChannel.validate(sub(env, "id", "a.b")));
        eq("id empty", "bad id (charset/length)",
                CommandChannel.validate(sub(env, "id", "")));
        eq("op unknown", "unknown op 'warp'",
                CommandChannel.validate(sub(env, "op", "warp")));
        eq("op non-string", "op must be string",
                CommandChannel.validate(sub(env, "op", Long.valueOf(3))));
        eq("args string", "args must be object",
                CommandChannel.validate(sub(env, "args", "x")));
        eq("args number", "args must be object",
                CommandChannel.validate(sub(env, "args", Long.valueOf(1))));
        eq("tg string", "targetGeneration must be integer",
                CommandChannel.validate(sub(env, "targetGeneration", "2")));
        eq("tg long pass", null,
                CommandChannel.validate(sub(env, "targetGeneration", Long.valueOf(2))));
        eq("id 64ch pass", null, CommandChannel.validate(sub(env, "id", repeat("i", 64))));
        // 真实封包端到端：MiniJson→validate
        Map<String, Object> real = MiniJson.parse(
                "{\"protocolVersion\":1,\"id\":\"1717000000123-a1b2\",\"op\":\"place\","
                + "\"args\":{\"x\":0,\"y\":71,\"z\":0,\"blockId\":4},\"save\":true}");
        eq("real envelope pass", null, CommandChannel.validate(real));
        // 协议违例 JSON 一律 parse 抛（consume 侧落 REJECTED bad-json）
        throwsEx("envelope dup key reject", new Runnable() {
            public void run() {
                MiniJson.parse("{\"protocolVersion\":1,\"protocolVersion\":2,\"id\":\"a\",\"op\":\"save\"}");
            }
        });

        // ---------- F. KNOWN_OPS ↔ LISTOPS 自表面一致 ----------
        // 括号感知切分（';' 出现在 [..] 说明里不算分隔），首条目剥 "ops=" 前缀
        Set<String> fromList = new HashSet<String>();
        List<String> entries = new ArrayList<String>();
        StringBuilder cur = new StringBuilder();
        int depth = 0;
        for (char ch : CommandChannel.LISTOPS.toCharArray()) {
            if (ch == '[') {
                depth++;
            } else if (ch == ']') {
                depth--;
            }
            if (ch == ';' && depth == 0) {
                entries.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(ch);
            }
        }
        entries.add(cur.toString());
        for (String entry : entries) {
            String e = entry.trim();
            if (e.startsWith("ops=")) {
                e = e.substring(4);
            }
            if (e.isEmpty()) {
                continue;
            }
            int cut = e.indexOf('(');
            int sp = e.indexOf(' ');
            if (cut < 0) {
                cut = sp;
            } else if (sp >= 0 && sp < cut) {
                cut = sp;
            }
            fromList.add(cut < 0 ? e : e.substring(0, cut));
        }
        eq("listops self-surface == KNOWN_OPS",
                String.valueOf(new TreeSet<String>(CommandChannel.KNOWN_OPS)),
                String.valueOf(new TreeSet<String>(fromList)));
        ok("ops count >= 30", CommandChannel.KNOWN_OPS.size() >= 30);

        System.out.println("GuardTest checks=" + checks + " fails=" + fails);
        System.exit(fails == 0 ? 0 : 1);
    }

    /** copy of base map, set/remove one key (null value = remove). */
    private static Map<String, Object> sub(Map<String, Object> base, String k, Object v) {
        Map<String, Object> m = new HashMap<String, Object>(base);
        if (v == null) {
            m.remove(k);
        } else {
            m.put(k, v);
        }
        return m;
    }

    private static String repeat(String s, int n) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < n; i++) {
            b.append(s);
        }
        return b.toString();
    }

    /** 256 字符唯一键（数字前缀+k 填充）。 */
    private static String padKey(int i) {
        StringBuilder k = new StringBuilder(String.valueOf(i));
        while (k.length() < 256) {
            k.append('k');
        }
        return k.toString();
    }

    /** anchor 文件负例包装（buildOrThrow 对非 runtime schema 必拒）。 */
    private static final class RunnableBuilder implements Runnable {
        private final File f;
        private final String text;

        RunnableBuilder(File f, String text) {
            this.f = f;
            this.text = text;
        }

        public void run() {
            FingerprintLoader.buildOrThrow("fingerprints/" + f.getName(), text);
        }
    }

    private static String readUtf8(File f) throws Exception {
        FileInputStream in = new FileInputStream(f);
        try {
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) {
                b.write(buf, 0, n);
            }
            return new String(b.toByteArray(), "UTF-8");
        } finally {
            in.close();
        }
    }

    // ---------- 宽松 JSON 校验器（全 JSON：null/数组/深嵌套允许，锚库档案合法形态） ----------

    private static boolean isJson(String text) {
        try {
            int[] p = { 0 };
            ws(text, p);
            val(text, p, 0);
            ws(text, p);
            return p[0] == text.length();
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static void ws(String s, int[] p) {
        while (p[0] < s.length() && " \t\n\r".indexOf(s.charAt(p[0])) >= 0) {
            p[0]++;
        }
    }

    private static void val(String s, int[] p, int depth) {
        if (depth > 64) {
            throw new IllegalArgumentException("deep");
        }
        if (p[0] >= s.length()) {
            throw new IllegalArgumentException("eof");
        }
        char c = s.charAt(p[0]);
        if (c == '{') {
            p[0]++;
            ws(s, p);
            if (p[0] < s.length() && s.charAt(p[0]) == '}') {
                p[0]++;
                return;
            }
            while (true) {
                ws(s, p);
                if (p[0] >= s.length() || s.charAt(p[0]) != '"') {
                    throw new IllegalArgumentException("key");
                }
                str(s, p);
                ws(s, p);
                expect(s, p, ':');
                ws(s, p);
                val(s, p, depth + 1);
                ws(s, p);
                if (p[0] >= s.length()) {
                    throw new IllegalArgumentException("eof obj");
                }
                char d = s.charAt(p[0]++);
                if (d == '}') {
                    return;
                }
                if (d != ',') {
                    throw new IllegalArgumentException("comma");
                }
            }
        } else if (c == '[') {
            p[0]++;
            ws(s, p);
            if (p[0] < s.length() && s.charAt(p[0]) == ']') {
                p[0]++;
                return;
            }
            while (true) {
                ws(s, p);
                val(s, p, depth + 1);
                ws(s, p);
                char d = s.charAt(p[0]++);
                if (d == ']') {
                    return;
                }
                if (d != ',') {
                    throw new IllegalArgumentException("arr comma");
                }
            }
        } else if (c == '"') {
            str(s, p);
        } else if (c == 't') {
            lit(s, p, "true");
        } else if (c == 'f') {
            lit(s, p, "false");
        } else if (c == 'n') {
            lit(s, p, "null");
        } else if (c == '-' || (c >= '0' && c <= '9')) {
            num(s, p);
        } else {
            throw new IllegalArgumentException("unexpected " + c);
        }
    }

    private static void expect(String s, int[] p, char c) {
        if (p[0] >= s.length() || s.charAt(p[0]) != c) {
            throw new IllegalArgumentException("want " + c);
        }
        p[0]++;
    }

    private static void lit(String s, int[] p, String l) {
        if (!s.startsWith(l, p[0])) {
            throw new IllegalArgumentException("lit " + l);
        }
        p[0] += l.length();
    }

    private static void num(String s, int[] p) {
        int start = p[0];
        if (s.charAt(p[0]) == '-') {
            p[0]++;
        }
        while (p[0] < s.length() && "0123456789.eE+-".indexOf(s.charAt(p[0])) >= 0) {
            p[0]++;
        }
        if (p[0] == start) {
            throw new IllegalArgumentException("num");
        }
    }

    private static void str(String s, int[] p) {
        expect(s, p, '"');
        while (true) {
            if (p[0] >= s.length()) {
                throw new IllegalArgumentException("unterm");
            }
            char c = s.charAt(p[0]++);
            if (c == '"') {
                return;
            }
            if (c == '\\') {
                char e = s.charAt(p[0]++);
                if (e == 'u') {
                    p[0] += 4;
                }
            }
        }
    }

    private GuardTest() {
    }
}
