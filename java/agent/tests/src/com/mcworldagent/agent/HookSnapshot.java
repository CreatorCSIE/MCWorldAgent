// [B5-ARCHIVE 2026-11-22] headless test archived from %TEMP%\mcaut (source batch: B5 regression snapshot tool); run via java/agent/tests/run.ps1
package com.mcworldagent.agent;

import java.util.Arrays;
import java.util.Comparator;

/** Dumps the 8 hardcoded hook groups (array form + activate/forClass form) canonically. */
public final class HookSnapshot {
    public static void main(String[] a) throws Exception {
        String[][] groups = {
            {"inf-20100327", "TARGETS_0327"}, {"inf-20100618", "TARGETS_0618"},
            {"inf-20100629", "TARGETS_0629"}, {"a1.1.2_01", "TARGETS_1121"},
            {"a1.2.0", "TARGETS_120"}, {"b1.2_02", "TARGETS_1202"},
            {"b1.7.3", "TARGETS_173"}, {"b1.8.1", "TARGETS_181"},
        };
        HookTargets.Target[] all = null;
        for (int i = 0; i < groups.length; i++) {
            HookTargets.Target[] g = groupOf(groups[i][1]);
            HookTargets.Target[] copy = new HookTargets.Target[g.length];
            System.arraycopy(g, 0, copy, 0, g.length);
            Arrays.sort(copy, new Comparator<HookTargets.Target>() {
                public int compare(HookTargets.Target x, HookTargets.Target y) {
                    return x.hookId.compareTo(y.hookId);
                }
            });
            System.out.println("== " + groups[i][0]);
            for (int j = 0; j < copy.length; j++) {
                HookTargets.Target t = copy[j];
                System.out.println(t.hookId + "|" + t.className + "|" + t.methodName + "|"
                        + t.desc + "|" + t.access + "|" + t.expectedHits + "|"
                        + t.injectMode + "|" + t.label + "|" + t.superOwner);
            }
            HookTargets.activate(groups[i][0]);
            String[] names = classNames(g);
            Arrays.sort(names);
            for (int j = 0; j < names.length; j++) {
                HookTargets.Target[] fc = HookTargets.forClass(names[j]);
                HookTargets.Target[] copy2 = new HookTargets.Target[fc.length];
                System.arraycopy(fc, 0, copy2, 0, fc.length);
                Arrays.sort(copy2, new Comparator<HookTargets.Target>() {
                    public int compare(HookTargets.Target x, HookTargets.Target y) {
                        return x.hookId.compareTo(y.hookId);
                    }
                });
                System.out.println("  forClass " + names[j] + " n=" + copy2.length);
                for (int k = 0; k < copy2.length; k++) {
                    HookTargets.Target t = copy2[k];
                    System.out.println("    " + t.hookId + "|" + t.className + "|" + t.methodName
                            + "|" + t.desc + "|" + t.access + "|" + t.expectedHits + "|"
                            + t.injectMode + "|" + t.label + "|" + t.superOwner);
                }
            }
            if (all == null) {
                all = new HookTargets.Target[0];
            }
        }
    }

    private static String[] classNames(HookTargets.Target[] g) {
        String[] s = new String[g.length];
        for (int i = 0; i < g.length; i++) {
            s[i] = g[i].className;
        }
        Arrays.sort(s);
        int n = 0;
        for (int i = 0; i < s.length; i++) {
            if (i == 0 || !s[i].equals(s[i - 1])) {
                s[n++] = s[i];
            }
        }
        String[] out = new String[n];
        System.arraycopy(s, 0, out, 0, n);
        return out;
    }

    private static HookTargets.Target[] groupOf(String field) throws Exception {
        return (HookTargets.Target[]) HookTargets.class.getField(field).get(null);
    }
}
