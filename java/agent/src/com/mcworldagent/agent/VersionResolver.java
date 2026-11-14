package com.mcworldagent.agent;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.MessageDigest;

/**
 * 版本选择器（A2，spec version-registry-a2 §1）。premain 期执行，顺序：
 * 1. agent 参数 {@code version=<versionId>} 强制选表（A3 实跑保险丝——launcher/
 *    legacyfix 可能使 classpath jar 非原始件，SHA1 扫不中时人工钉表）。
 * 2. 否则扫 {@code java.class.path} 各 .jar 算 SHA1，对 VersionRegistry 已知表
 *    严格匹配（戒律 14：SHA1 门=原版+高仿双覆盖的索引圭臬）。
 * 3. 无匹配 → 保持默认 inf-20100327 + 日志 VERSION_UNKNOWN fallback=inf-20100327
 *    （现行为零变化；严格指纹匹配本就防错版错钩——Transformers 五项全中才织入）。
 * 命中 → VersionRegistry.activate + 返回 versionId（调用方再 HookTargets.activate）。
 */
public final class VersionResolver {

    /** 默认表（spec §1.3：无匹配=inf-20100327，现行为零变化）。 */
    static final String DEFAULT_ID = "inf-20100327";

    private VersionResolver() {
    }

    /** @return 最终激活的 versionId（forced→注册表命中→默认 0327 三态皆返回生效值） */
    public static String resolve(String forced) {
        if (forced != null && !forced.isEmpty()) {
            if (VersionRegistry.activate(forced)) {
                MCWorldAgent.logTransform("VERSION id=" + forced
                        + " forced=agent-param (jar not sha-verified)");
                return forced;
            }
            VersionRegistry.activate(DEFAULT_ID);
            MCWorldAgent.logTransform("VERSION_UNKNOWN forced=" + forced
                    + " not in registry fallback=" + DEFAULT_ID);
            return DEFAULT_ID;
        }
        return resolveFromClasspath(System.getProperty("java.class.path", ""));
    }

    /** 与 resolve(null) 同逻辑但类路径显式传入（无头测试注入面）。 */
    static String resolveFromClasspath(String classpath) {
        for (String entry : classpath.split(File.pathSeparator)) {
            if (!entry.toLowerCase().endsWith(".jar")) {
                continue;
            }
            File f = new File(entry);
            if (!f.isFile()) {
                continue;
            }
            String sha = sha1Hex(f);
            VersionTable t = VersionRegistry.byJarSha1(sha);
            if (t != null && VersionRegistry.activate(t.versionId)) {
                MCWorldAgent.logTransform("VERSION id=" + t.versionId + " sha=" + sha
                        + " jar=" + f.getAbsolutePath());
                return t.versionId;
            }
        }
        VersionRegistry.activate(DEFAULT_ID);
        MCWorldAgent.logTransform("VERSION_UNKNOWN fallback=" + DEFAULT_ID);
        return DEFAULT_ID;
    }

    private static String sha1Hex(File f) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            InputStream in = new FileInputStream(f);
            try {
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) > 0) {
                    md.update(buf, 0, n);
                }
            } finally {
                in.close();
            }
            byte[] d = md.digest();
            StringBuilder sb = new StringBuilder(d.length * 2);
            for (byte b : d) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16));
                sb.append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (Exception e) {
            MCWorldAgent.logTransform("VERSION-SHA1-ERROR " + f + ": " + e);
            return null;
        }
    }
}
