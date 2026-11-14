package com.mcworldagent.agent;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * 离线调试工具：对原始 jar 的 5 个钩目标类跑同一 instrument 路径，
 * 注入结果落盘（供 javap / -Xverify:all 复盘）。不启动游戏。
 *
 * 用法：java -cp "out/classes;libs/asm-9.7.jar" com.mcworldagent.agent.TransformDump <0327jar> <outDir>
 */
public final class TransformDump {

    public static void main(String[] argv) throws Exception {
        if (argv.length != 2) {
            System.err.println("usage: TransformDump <jar> <outDir>");
            System.exit(2);
        }
        MCWorldAgent.transformLog = new java.io.PrintWriter(System.out, true);
        File outDir = new File(argv[1]);
        //noinspection ResultOfMethodCallIgnored
        outDir.mkdirs();
        JarFile jar = new JarFile(new File(argv[0]));
        try {
            for (HookTargets.Target t : HookTargets.TARGETS_0327) {
                dumpOne(jar, t.className, outDir);
            }
        } finally {
            jar.close();
        }
    }

    private static void dumpOne(JarFile jar, String className, File outDir) throws Exception {
        JarEntry e = jar.getJarEntry(className + ".class");
        if (e == null) {
            System.out.println("MISSING " + className);
            return;
        }
        byte[] bytes;
        InputStream in = jar.getInputStream(e);
        try {
            java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) {
                bos.write(buf, 0, n);
            }
            bytes = bos.toByteArray();
        } finally {
            in.close();
        }
        int major = ((bytes[6] & 0xFF) << 8) | (bytes[7] & 0xFF);
        HookTargets.Target[] targets = HookTargets.forClass(className);
        // 复用 Transformers.instrument 的注入逻辑（反射调私有静态）
        java.lang.reflect.Method m = Transformers.class.getDeclaredMethod(
                "instrument", String.class, byte[].class, HookTargets.Target[].class);
        m.setAccessible(true);
        byte[] out = (byte[]) m.invoke(null, className, bytes, targets);
        File f = new File(outDir, className + ".class");
        //noinspection ResultOfMethodCallIgnored
        f.getParentFile().mkdirs();
        FileOutputStream fos = new FileOutputStream(f);
        try {
            fos.write(out == null ? bytes : out);
        } finally {
            fos.close();
        }
        System.out.println(className + " major=" + major
                + " injected=" + (out != null) + " -> " + f);
    }

    private TransformDump() {
    }
}
