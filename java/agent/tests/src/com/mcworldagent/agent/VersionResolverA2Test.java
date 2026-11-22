// [B5-ARCHIVE 2026-11-22] headless test archived from %TEMP%\mcaut (source batch: A2 (0618/0629/1121)); run via java/agent/tests/run.ps1
package com.mcworldagent.agent;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.security.MessageDigest;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

/**
 * A2 验收 5：VersionResolver 三分支——SHA1 匹配（临时 jar 注入 BY_SHA1）/
 * 不匹配 fallback=inf-20100327 / version= 覆盖（含未知 id 回退）。
 */
public class VersionResolverA2Test {

    static int checks = 0;

    static void ok(boolean c, String msg) {
        checks++;
        if (!c) {
            throw new AssertionError(msg);
        }
    }

    static File makeJar(String name, String content) throws Exception {
        File dir = new File(System.getProperty("java.io.tmpdir"), "mcaut\\tests-out");
        if (!dir.isDirectory()) {
            ok(dir.mkdirs() || dir.isDirectory(), "tmpdir");
        }
        File f = new File(dir, name);
        JarOutputStream jo = new JarOutputStream(new FileOutputStream(f));
        try {
            jo.putNextEntry(new JarEntry("a.txt"));
            jo.write(content.getBytes("UTF-8"));
            jo.closeEntry();
        } finally {
            jo.close();
        }
        return f;
    }

    static String sha1(File f) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        md.update(java.nio.file.Files.readAllBytes(f.toPath()));
        byte[] d = md.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : d) {
            sb.append(Character.forDigit((b >> 4) & 0xF, 16));
            sb.append(Character.forDigit(b & 0xF, 16));
        }
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    public static void main(String[] a) throws Exception {
        // 分支一：version= 覆盖（已知 id）
        String r = VersionResolver.resolve("inf-20100618");
        ok("inf-20100618".equals(r), "forced known -> " + r);
        ok("inf-20100618".equals(VersionRegistry.currentVersionId()), "active=0618");

        // 0629（第三版，G3 族首例）
        r = VersionResolver.resolve("inf-20100629");
        ok("inf-20100629".equals(r), "forced 0629 -> " + r);
        ok("inf-20100629".equals(VersionRegistry.currentVersionId()), "active=0629");

        // 分支一b：version= 未知 → 回默认 0327
        r = VersionResolver.resolve("bogus-version");
        ok("inf-20100327".equals(r), "forced unknown -> fallback " + r);
        ok("inf-20100327".equals(VersionRegistry.currentVersionId()), "active back to 0327");

        // 分支二：classpath SHA1 命中（临时 jar 的 sha 注入 BY_SHA1，模拟已知表匹配）
        File jar = makeJar("a2match.jar", "match-me");
        String sha = sha1(jar);
        Field bf = VersionRegistry.class.getDeclaredField("BY_SHA1");
        bf.setAccessible(true);
        Map<String, VersionTable> bySha = (Map<String, VersionTable>) bf.get(null);
        VersionTable t0618 = VersionRegistry.byJarSha1(
                "89eab2c1a353707cc00f074dffba9cb7a4f5e304");
        bySha.put(sha, t0618);
        r = VersionResolver.resolveFromClasspath(
                "C:\\other\\x.jar" + File.pathSeparator + jar.getAbsolutePath());
        ok("inf-20100618".equals(r), "classpath sha match -> " + r);
        ok("inf-20100618".equals(VersionRegistry.currentVersionId()), "active=0618 via sha");
        bySha.remove(sha);

        // 分支三：无匹配 → VERSION_UNKNOWN fallback=inf-20100327
        File jar2 = makeJar("a2nomatch.jar", "no-body");
        r = VersionResolver.resolveFromClasspath(jar2.getAbsolutePath() + File.pathSeparator + "dir\\notajar");
        ok("inf-20100327".equals(r), "no match -> fallback " + r);
        ok("inf-20100327".equals(VersionRegistry.currentVersionId()), "active=0327");

        // 已知 sha 直查（戒律 14 索引）
        ok(VersionRegistry.byJarSha1("89eab2c1a353707cc00f074dffba9cb7a4f5e304") != null, "0618 sha indexed");
        ok(VersionRegistry.byJarSha1("47b1b32430a211520993552ba0a5e00c1af44724") != null, "0327 sha indexed");
        ok(VersionRegistry.byJarSha1("eb50bce3cb542488b3039aa0f4c3c0ec7595ab24") != null, "0629 sha indexed");
        ok(VersionRegistry.byJarSha1("deadbeef") == null, "unknown sha null");

        System.out.println("VersionResolverA2Test PASS (" + checks + " checks)");
    }
}
