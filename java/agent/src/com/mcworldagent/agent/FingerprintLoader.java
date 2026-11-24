package com.mcworldagent.agent;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * B5 运行时装载器：agent 资源 fingerprints/*.json（schema=mca-runtime-v1）→ VersionTable。
 * 指纹工业化收口件——新内容版本=数据操作（转写脚本 tools/f1-anchor-compiler/
 * b5_transcribe.py 产 JSON，build.ps1 打进 jar 资源，本类装载），不再加 Java 表。
 * 资源定位双形态：agent 以 jar 运行 → JarFile 枚举 fingerprints/*.json；
 * 无头测试以 classes 目录运行 → 目录枚举。读取一律 getResourceAsStream 之外的
 * 枚举路径（ClassLoader 无法枚举资源名，jar/目录枚举=唯一零硬编码清单方案）。
 * 协议纪律（MiniJson 整数子集）：运行时 JSON 无 null（缺槽=键缺席=表值 null，
 * 档位一形态）、嵌套≤1 层（mobs/hooks 平铺字符串值）、全文≤4096 字符、
 * 单个解码字符串≤256（MiniJson MAX_STRING，provenance 补记勿超长，详账进提交消息）。
 * 容错纪律：锚库旧 schema 文件（无 mca-runtime-v1 标记）静默跳过（数据档案，
 * 非运行时件）；带标记但解析/校验失败=响亮跳过（VERSION-JSON-SKIP 日志），
 * 绝不让数据错误崩 premain。ops 键名=VersionTable 字段名，未知键=文件作废
 * （防手改打错槽静默生效）。hooks 段=织入锚数据（class|name|desc|access|
 * expectedHits|injectMode 平铺），装载时校验形态（六元组+access/hits 整数+
 * 钩名/注入位白名单），入 VersionTable.hookSpecs 由 HookTargets.activate 消费。
 */
public final class FingerprintLoader {

    /** 运行时 schema 标记（锚库档案 JSON 无此标记=不装载）。 */
    static final String RUNTIME_SCHEMA = "mca-runtime-v1";
    /** 资源目录（jar 内路径 / classes 目录名）。 */
    private static final String RES_DIR = "fingerprints";

    private FingerprintLoader() {
    }

    /** 装载全部运行时指纹表（资源名升序=确定性顺序）。单件失败只跳过不抛。 */
    public static List<VersionTable> loadAll() {
        return load(false);
    }

    /**
     * 守卫批 strict 入口（2026-11-25 守卫案）：与 loadAll 同扫描面，但任何
     * "带标记文件被静默吞掉"的异常（读取失败/解析失败/校验失败）直接抛出——
     * 生产路径保持宽容（绝不让数据错误崩 premain），测试路径必须响亮。
     */
    public static List<VersionTable> loadAllStrict() {
        return load(true);
    }

    private static List<VersionTable> load(boolean strict) {
        List<VersionTable> out = new ArrayList<VersionTable>();
        try {
            URL loc = FingerprintLoader.class.getProtectionDomain().getCodeSource().getLocation();
            File base = toFile(loc);
            if (base == null) {
                return out;
            }
            if (base.isFile() && base.getName().toLowerCase().endsWith(".jar")) {
                loadFromJar(base, out, strict);
            } else if (base.isDirectory()) {
                loadFromDir(base, out, strict);
            }
        } catch (RuntimeException re) {
            throw re; // strict 判决异常原样上抛
        } catch (Throwable t) {
            MCWorldAgent.logTransform("VERSION-JSON-LOAD-ERROR " + t);
        }
        return out;
    }

    private static void loadFromJar(File jarFile, List<VersionTable> out, boolean strict) {
        JarFile jar = null;
        try {
            jar = new JarFile(jarFile);
            List<String> names = new ArrayList<String>();
            for (Enumeration<JarEntry> en = jar.entries(); en.hasMoreElements();) {
                JarEntry e = en.nextElement();
                String n = e.getName();
                if (n.startsWith(RES_DIR + "/") && n.endsWith(".json") && !e.isDirectory()) {
                    names.add(n);
                }
            }
            String[] sorted = (String[]) names.toArray(new String[0]);
            Arrays.sort(sorted);
            for (int i = 0; i < sorted.length; i++) {
                try {
                    String text = readResource(jar.getInputStream(jar.getEntry(sorted[i])));
                    addIfRuntime(text, sorted[i], out, strict);
                } catch (RuntimeException re) {
                    throw re;
                } catch (Throwable perFile) {
                    if (strict) {
                        throw new IllegalStateException("strict: read fail " + sorted[i], perFile);
                    }
                    MCWorldAgent.logTransform("VERSION-JSON-SKIP " + sorted[i] + " read=" + perFile);
                }
            }
        } catch (RuntimeException re) {
            throw re; // strict 判决不被 jar 级容错吞掉
        } catch (Throwable t) {
            MCWorldAgent.logTransform("VERSION-JSON-LOAD-ERROR jar=" + jarFile + " " + t);
        } finally {
            if (jar != null) {
                try {
                    jar.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    private static void loadFromDir(File classesDir, List<VersionTable> out, boolean strict) {
        File dir = new File(classesDir, RES_DIR);
        File[] fs = dir.listFiles();
        if (fs == null) {
            return; // 无资源目录=无 JSON 版（现行为零变化）
        }
        Arrays.sort(fs);
        for (int i = 0; i < fs.length; i++) {
            File f = fs[i];
            if (!f.isFile() || !f.getName().endsWith(".json")) {
                continue;
            }
            try {
                InputStream in = new java.io.FileInputStream(f);
                String text;
                try {
                    text = readResource(in);
                } finally {
                    in.close();
                }
                addIfRuntime(text, RES_DIR + "/" + f.getName(), out, strict);
            } catch (RuntimeException re) {
                throw re;
            } catch (Throwable t) {
                if (strict) {
                    throw new IllegalStateException("strict: read fail " + f.getName(), t);
                }
                MCWorldAgent.logTransform("VERSION-JSON-SKIP " + f.getName() + " read=" + t);
            }
        }
    }

    /** 文本含运行时标记才解析；锚库档案件静默过，坏件响亮跳过。strict=坏件直接抛。 */
    private static void addIfRuntime(String text, String srcName, List<VersionTable> out,
            boolean strict) {
        if (text == null || text.indexOf(RUNTIME_SCHEMA) < 0) {
            return;
        }
        if (strict) {
            out.add(buildOrThrow(srcName, text));
            return;
        }
        VersionTable t = buildTable(srcName, text);
        if (t != null) {
            out.add(t);
        }
    }

    /** JSON 文本→表。任何协议/校验违例=响亮跳过返回 null。包内可见=无头直测面。 */
    static VersionTable buildTable(String srcName, String text) {
        try {
            return buildOrThrow(srcName, text);
        } catch (Throwable bad) {
            MCWorldAgent.logTransform("VERSION-JSON-SKIP " + srcName + " " + bad);
            return null;
        }
    }

    /** 校验唯一实现：违例抛 IllegalArgumentException（strict 路径与守卫负例直测面）。 */
    @SuppressWarnings("unchecked")
    static VersionTable buildOrThrow(String srcName, String text) {
        Map<String, Object> m;
        m = MiniJson.parse(text);
        if (!RUNTIME_SCHEMA.equals(m.get("schema"))) {
            throw new IllegalArgumentException("schema mismatch");
        }
        String versionId = reqStr(m, "versionId");
        String jarSha1 = reqStr(m, "jarSha1");
        if (!jarSha1.matches("[0-9a-f]{40}")) {
            throw new IllegalArgumentException("jarSha1 not 40-hex-lower");
        }
        if (!srcName.endsWith(versionId + ".json")) {
            throw new IllegalArgumentException("resource name != versionId");
        }
        VersionTable t = new VersionTable();
        t.versionId = versionId;
        t.family = reqStr(m, "family");
        t.jarSha1 = jarSha1;
        Map<String, Object> ops = (Map<String, Object>) m.get("ops");
        if (ops == null) {
            throw new IllegalArgumentException("ops missing");
        }
        for (Map.Entry<String, Object> e : ops.entrySet()) {
            String key = e.getKey();
            if (key.equals("versionId") || key.equals("family") || key.equals("jarSha1")) {
                throw new IllegalArgumentException("identity slot duplicated in ops");
            }
            Field f;
            try {
                f = VersionTable.class.getField(key);
            } catch (NoSuchFieldException nf) {
                throw new IllegalArgumentException("unknown slot '" + key + "'");
            }
            Object v = e.getValue();
            Class<?> ft = f.getType();
            if (ft == String.class) {
                if (!(v instanceof String)) {
                    throw new IllegalArgumentException("slot '" + key + "' wants string");
                }
            } else if (ft == Integer.class) {
                if (!(v instanceof Long)) {
                    throw new IllegalArgumentException("slot '" + key + "' wants int");
                }
                v = Integer.valueOf(((Long) v).intValue());
            } else if (ft == Boolean.class) {
                if (!(v instanceof Boolean)) {
                    throw new IllegalArgumentException("slot '" + key + "' wants boolean");
                }
            } else {
                throw new IllegalArgumentException("slot '" + key + "' type unsupported");
            }
            try {
                f.set(t, v);
            } catch (IllegalAccessException ia) {
                throw new IllegalArgumentException("slot '" + key + "' set failed", ia);
            }
        }
        Map<String, Object> mobs = (Map<String, Object>) m.get("mobs");
        if (mobs != null) {
            for (Map.Entry<String, Object> e : mobs.entrySet()) {
                if (!(e.getValue() instanceof String)) {
                    throw new IllegalArgumentException("mob '" + e.getKey() + "' wants string");
                }
                t.mobClasses.put(e.getKey(), (String) e.getValue());
            }
        }
        Map<String, Object> hooks = (Map<String, Object>) m.get("hooks");
        if (hooks != null) {
            for (Map.Entry<String, Object> e : hooks.entrySet()) {
                String v = (String) e.getValue();
                if (v == null || v.split("\\|", -1).length != 6) {
                    throw new IllegalArgumentException("hook '" + e.getKey()
                            + "' wants class|name|desc|access|hits|mode");
                }
                String[] p = v.split("\\|", -1);
                Integer.parseInt(p[3]); // access 十进制
                Integer.parseInt(p[4]); // expectedHits 十进制
                if (!HookTargets.isKnownHookKey(e.getKey())) {
                    throw new IllegalArgumentException("hook '" + e.getKey() + "' unknown");
                }
                if (HookTargets.modeOf(p[5]) < 0) {
                    throw new IllegalArgumentException("hook '" + e.getKey()
                            + "' bad mode '" + p[5] + "'");
                }
                t.hookSpecs.put(e.getKey(), v);
            }
        }
        return t;
    }

    private static String reqStr(Map<String, Object> m, String key) {
        Object v = m.get(key);
        if (!(v instanceof String) || ((String) v).length() == 0) {
            throw new IllegalArgumentException("missing '" + key + "'");
        }
        return (String) v;
    }

    private static String readResource(InputStream in) throws Exception {
        if (in == null) {
            return null;
        }
        try {
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            int total = 0;
            while ((n = in.read(buf)) > 0) {
                total += n;
                if (total > 65536) {
                    throw new IllegalArgumentException("oversize");
                }
                b.write(buf, 0, n);
            }
            return new String(b.toByteArray(), "UTF-8");
        } finally {
            in.close();
        }
    }

    /** codeSource URL→File（Windows 盘符前导斜杠剥除+百分号解码）。 */
    private static File toFile(URL url) {
        try {
            String p = URLDecoder.decode(url.getPath(), "UTF-8");
            if (p.length() > 2 && p.charAt(0) == '/' && p.charAt(2) == ':') {
                p = p.substring(1);
            }
            return new File(p);
        } catch (Exception e) {
            return null;
        }
    }
}
