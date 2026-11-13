package com.mcworldagent.agent;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.instrument.Instrumentation;

/**
 * premain 入口。agent 参数格式：{@code log=<transform日志路径>;hooks=<捕获事件日志路径>}。
 * 类随游戏自然加载被发现，premain 不强加载任何游戏类。
 */
public final class MCWorldAgent {

    static PrintWriter transformLog; // 包内可见：离线调试工具可注入 stdout
    static java.util.Set<String> disabled = java.util.Collections.emptySet();
    static File dumpDir;
    static String cmdDir; // cmd= 覆盖命令通道目录；null=默认 <gameDir>\mca

    public static void premain(String args, Instrumentation inst) {
        String logPath = null;
        String hooksPath = null;
        if (args != null) {
            for (String kv : args.split(";")) {
                int eq = kv.indexOf('=');
                if (eq < 0) {
                    continue;
                }
                String k = kv.substring(0, eq).trim();
                String v = kv.substring(eq + 1).trim();
                if ("log".equals(k)) {
                    logPath = v;
                } else if ("hooks".equals(k)) {
                    hooksPath = v;
                } else if ("disable".equals(k)) {
                    disabled = new java.util.HashSet<String>(java.util.Arrays.asList(v.split(",")));
                } else if ("dump".equals(k)) {
                    dumpDir = new File(v);
                    if (!dumpDir.isDirectory()) {
                        //noinspection ResultOfMethodCallIgnored
                        dumpDir.mkdirs();
                    }
                } else if ("cmd".equals(k)) {
                    cmdDir = v;
                }
            }
        }
        try {
            if (logPath != null) {
                transformLog = openWriter(logPath);
            }
            if (hooksPath != null) {
                MCAHooks.out = openWriter(hooksPath);
            }
        } catch (IOException e) {
            System.err.println("[MCWorldAgent] log open failed: " + e);
        }
        logTransform("PREMAIN mcworldagent-agent args=[" + args + "] java="
                + System.getProperty("java.version") + " os=" + System.getProperty("os.name"));
        inst.addTransformer(new Transformers());
        CommandChannel.start(cmdDir);

        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    MCAHooks.logSummary();
                    logTransform("EXIT");
                } finally {
                    close(transformLog);
                    close(MCAHooks.out);
                }
            }
        }, "MCWorldAgent-Shutdown"));
    }

    static void logLoad(String className, ClassLoader loader) {
        logTransform("LOAD " + className + " loader="
                + (loader == null ? "boot" : loader.getClass().getName()
                + "@" + Integer.toHexString(System.identityHashCode(loader))));
    }

    static void logTransform(String s) {
        PrintWriter w = transformLog;
        if (w == null) {
            return;
        }
        synchronized (w) {
            w.println("[" + (System.currentTimeMillis() - START) + "ms] " + s);
            w.flush();
        }
    }

    private static final long START = System.currentTimeMillis();

    private static PrintWriter openWriter(String path) throws IOException {
        File f = new File(path);
        File parent = f.getAbsoluteFile().getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs() && !parent.isDirectory()) {
            throw new IOException("cannot create dir " + parent);
        }
        return new PrintWriter(new FileWriter(f, false));
    }

    private static void close(PrintWriter w) {
        if (w != null) {
            w.flush();
            w.close();
        }
    }

    private MCWorldAgent() {
    }
}
