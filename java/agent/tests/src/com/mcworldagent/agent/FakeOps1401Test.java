// b1.4_01 假件面活体（JSON-only 版，无头窗 V-2 批 2026-11-27）：root1401 假件树 + WorldOps
// 全反射面。表源=b1.4_01.json（mca-fingerprint-v1 档案件，经 A2Table1401Test.injectTable()
// 测试侧转写注入 BY_ID/BY_SHA1/active——非 FingerprintLoader 运行时件，详 A2 头注）。
// 重点腿：方块读写（setBlockM=e/getBlockM=a）、tp（playerListF=d + aJ..aO 直写 +
// setPosRot b(DDDFF)V）、getinfo/settime/setspawn（WorldInfo 分支：eb.w()Ldj; 五 private 槽
// ——⚠️取用器 w 非 1501 x）、save 双参 (false,null)（a(ZLtw;)V）、sleep 四门（1401 真实序
// e→b→c→d，fm_c b(III)Lcb; @0-329 实锤：门1 M()Z||!U()Z、门2 eb.m.sy.c、门3 eb.f()、
// 门4 距离 3/2/3；OK 尾 a/c/b 三 private 槽+!eb.t→eb.x()V）、tick NPE 崩溃面（b=null→ah()
// 裸解引用）、wake null-safe（@26 ifnull）、setchest c_ 回环（hx private hi[] a 27 槽 +
// hi.d private + oy 熔炉门）、UNSUPPORTED 响亮五件：grow/invulnerable/genscatter/getbiome
// 走 require 槽名（idCrops/iframesF/scatterClassG/biomeWcmM），weather 走 capWeather
// Boolean 门（WorldOps L1738 判决：FALSE=version-gated 响亮，非槽名格式——断言分道）。
package com.mcworldagent.agent;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

public class FakeOps1401Test {

    static int checks = 0;

    static void ok(boolean c, String msg) {
        checks++;
        if (!c) {
            throw new AssertionError(msg);
        }
    }

    static void set(Object o, String f, Object v) throws Exception {
        Field fd = o.getClass().getField(f);
        fd.setAccessible(true);
        fd.set(o, v);
    }

    static Object get(Object o, String f) throws Exception {
        Field fd = o.getClass().getField(f);
        fd.setAccessible(true);
        return fd.get(o);
    }

    // fm/dj 私有面直读（与 WorldOps 同配方 getDeclaredField+setAccessible——a/b/c 与 a-e）
    static Object pri(Object o, String f) throws Exception {
        Field fd = o.getClass().getDeclaredField(f);
        fd.setAccessible(true);
        return fd.get(o);
    }

    static void priSet(Object o, String f, Object v) throws Exception {
        Field fd = o.getClass().getDeclaredField(f);
        fd.setAccessible(true);
        fd.set(o, v);
    }

    interface RunnableEx {
        void run() throws Exception;
    }

    static void unsupported(String op, RunnableEx r, String slot) {
        checks++;
        try {
            r.run();
            throw new AssertionError(op + " should UnsupportedOp(" + slot + ")");
        } catch (WorldOps.UnsupportedOp u) {
            String m = u.getMessage();
            if (!m.contains("slot " + slot) || !m.contains("b1.4_01")) {
                throw new AssertionError(op + " msg=" + m);
            }
        } catch (Exception e) {
            throw new AssertionError(op + " wrong: " + e);
        }
    }

    @SuppressWarnings("unchecked")
    public static void main(String[] a) throws Exception {
        A2Table1401Test.injectTable();
        ok(VersionRegistry.activate("b1.4_01"), "activate 1401");
        HookTargets.activate("b1.4_01");

        // ---- 布景：eb 玩家面=字段 d=playerEntities；玩家 fm 构造 (eb)V ----
        Class<?> wc = Class.forName("eb");
        Object w = wc.getConstructor().newInstance();
        Object player = Class.forName("fm").getConstructor(wc).newInstance(w);
        ((List<Object>) get(w, "d")).add(player);
        ok(((List<Object>) get(w, "d")).size() == 1, "playerEntities 首件=活体玩家");
        WorldOps.resolve(wc);

        // ---- 方块读写面（setBlockM=e (IIII)Z / getBlockM=a (III)I）----
        WorldOps.setBlock(w, 24, 64, 24, 26);
        ok(WorldOps.getBlockId(w, 24, 64, 24) == 26, "setBlock/getBlock e/a 对榫");
        ok(WorldOps.getBlockId(w, 1, 1, 1) == 0, "空气保底 0");

        // ---- tp（playerListF=d get(0) + aJ..aO 直写 + setPosRot b(DDDFF)V）----
        String tp = WorldOps.tp(w, 10, 64, 10);
        ok(tp.startsWith("teleported from x=0.0 y=0.0 z=0.0 -> x=10 y=64 z=10"), "tp 回显 " + tp);
        ok(((Double) get(player, "aJ")).doubleValue() == 10.5, "tp aJ=10.5（posX——1501=aK 断代）");
        ok(((Double) get(player, "aK")).doubleValue() == 64.0, "tp aK=64（posY）");
        ok(((Double) get(player, "aO")).doubleValue() == 0.0, "tp aO 清零（motZ）");

        // ---- getinfo/settime/setspawn（WorldInfo 分支：eb.w()Ldj; 五 private 槽）----
        Object wi = wc.getMethod("w").invoke(w);
        ok(WorldOps.getInfo(w).contains("difficulty=0"), "getinfo 初值");
        WorldOps.setTime(w, 1000L);
        ok(((Long) pri(wi, "e")).longValue() == 1000L, "settime 写 dj.e private");
        WorldOps.setSpawn(w, 1, 2, 3);
        ok(((Integer) pri(wi, "b")).intValue() == 1 && ((Integer) pri(wi, "c")).intValue() == 2
                && ((Integer) pri(wi, "d")).intValue() == 3, "setspawn 写 dj.b/c/d");
        ok(WorldOps.getInfo(w).contains("time=1000 spawnX=1 spawnY=2 spawnZ=3"), "getinfo 回读");

        // ---- save 双参腿（断代带延续：resolve (ZLtw;)V 分支 + invoke (false,null) 配方）----
        WorldOps.saveWorld(w);
        ok(((Integer) get(w, "saveCalls")).intValue() == 1, "saveWorld 绑定 eb.a(ZLtw;)V 非 a(Lpb;)Z");
        WorldOps.saveWorld(w);
        ok(((Integer) get(w, "saveCalls")).intValue() == 2, "saveWorld 可重复触发");

        // ---- inv 读面（invF=f / hh a[36]/b[4]/c + hi a/c public d private）----
        Object inv = get(player, "f");
        Object[] mainArr = (Object[]) get(inv, "a");
        Constructor<?> hiCtor = Class.forName("hi").getConstructor(int.class, int.class, int.class);
        mainArr[3] = hiCtor.newInstance(26, 1, 0);
        ok(WorldOps.inv(w).equals("3=1x26 cur=0"), "inv via d.get(0)");
        ok(WorldOps.clearInv(w).startsWith("cleared 36+4"), "clearinv 36+4");

        // ---- sleep 四门（1401 真实序：门1 已睡/不可睡→e；门2 provider→b；门3 白天→c；门4 距离→d）----
        Field fmC = player.getClass().getDeclaredField("c");
        fmC.setAccessible(true);
        boolean rejS = false;
        String sMsg = "";
        // 门 1：已睡拒（private a:Z→M()Z→cb.e；⚠️ 1401 门1 探针=M/U 非 1501 O/W）
        priSet(player, "a", Boolean.TRUE);
        try {
            WorldOps.setSleep(w, true, 24, 64, 24);
        } catch (WorldOps.Rejected rj) {
            rejS = true;
            sMsg = rj.getMessage();
        }
        ok(rejS && sMsg.contains("sleep-status=e"), "已睡→Rejected e " + sMsg);
        priSet(player, "a", Boolean.FALSE);
        // 门 2：provider 禁睡（eb.m:Lsy;.c:Z→cb.b）
        Object prov = get(w, "m");
        set(prov, "c", Boolean.TRUE);
        rejS = false;
        try {
            WorldOps.setSleep(w, true, 24, 64, 24);
        } catch (WorldOps.Rejected rj) {
            rejS = true;
            sMsg = rj.getMessage();
        }
        ok(rejS && sMsg.contains("sleep-status=b"), "provider 禁睡→Rejected b " + sMsg);
        set(prov, "c", Boolean.FALSE);
        // 门 3：白天拒（eb.f()Z→cb.c）
        set(w, "daytime", Boolean.TRUE);
        rejS = false;
        try {
            WorldOps.setSleep(w, true, 24, 64, 24);
        } catch (WorldOps.Rejected rj) {
            rejS = true;
            sMsg = rj.getMessage();
        }
        ok(rejS && sMsg.contains("sleep-status=c"), "白天→Rejected c " + sMsg);
        set(w, "daytime", Boolean.FALSE);
        // 门 4：距远拒（|aJ-x|>3→cb.d；阈值 3/2/3）
        set(player, "aJ", Double.valueOf(100.0));
        rejS = false;
        try {
            WorldOps.setSleep(w, true, 24, 64, 24);
        } catch (WorldOps.Rejected rj) {
            rejS = true;
            sMsg = rj.getMessage();
        }
        ok(rejS && sMsg.contains("sleep-status=d"), "距>3/2/3→Rejected d " + sMsg);
        set(player, "aJ", Double.valueOf(24.5));
        set(player, "aL", Double.valueOf(24.5));
        // 合法态：床 26@(24,64,24)+夜+距内
        WorldOps.setBlock(w, 24, 64, 24, 26);
        String sl = WorldOps.setSleep(w, true, 24, 64, 24);
        ok(sl.startsWith("sleep on @24,64,24 -> OK"), "sleep on 回显 " + sl);
        ok(Boolean.TRUE.equals(pri(player, "a")), "a=sleeping true（private 写面——1501 x protected 断代）");
        ok(fmC.getInt(player) == 0, "c=timer 0 合法态（1501 b 槽洗牌）");
        ok(pri(player, "b") != null, "b=bedChunkCoordinates 置位（private ay b——1501 a 槽洗牌）");
        ok(((Integer) get(w, "sleepFlagRefreshes")).intValue() == 1, "eb.x()V 刷新 x1（!eb.t 路径）");
        // tick 崩溃回归：合法态 w_() 存活（回调 jm.w_ 链）；b=null 必 NPE（ah() 裸解引用实锤）
        Method tick = player.getClass().getMethod("w_");
        tick.invoke(player);
        ok(Boolean.TRUE.equals(pri(player, "a")), "合法态 tick 存活不崩（a 仍 true）");
        priSet(player, "b", null);
        boolean npe = false;
        try {
            tick.invoke(player);
        } catch (java.lang.reflect.InvocationTargetException e) {
            npe = e.getCause() instanceof NullPointerException;
        }
        ok(npe, "b=null+sleeping tick→NPE（ah() 崩溃面复现，W173 同款模型 1401 翻版）");
        // off：wakeUpPlayer(true,true,false)（重挂 b 后 a=false/timer=0/刷新 x2）
        priSet(player, "b", Class.forName("ay").getConstructor(int.class, int.class, int.class)
                .newInstance(24, 64, 24));
        ok(WorldOps.setSleep(w, false, 0, 0, 0).startsWith("sleep off -> wakeUpPlayer"), "wake 回显");
        ok(!Boolean.TRUE.equals(pri(player, "a")) && fmC.getInt(player) == 0, "wake 清零 a=false/c=0");
        ok(((Integer) get(w, "sleepFlagRefreshes")).intValue() == 2, "eb.x()V 刷新 x2");
        // wake null-safe：b=null 再 off 不抛（fm_c a(ZZZ)V @26 ifnull 跳床清理实锤）
        priSet(player, "b", null);
        ok(WorldOps.setSleep(w, false, 0, 0, 0).startsWith("sleep off"), "wake b=null null-safe");

        // ---- setchest/getchest（hx private hi[] a 27 槽；hi.d private 回环；oy 熔炉门）----
        Object[] reg = (Object[]) Class.forName("fg").getField("c").get(null);
        Constructor<?> fgCtor = Class.forName("fg").getDeclaredConstructor(int.class);
        fgCtor.setAccessible(true);
        reg[26] = fgCtor.newInstance(26);
        Field teStore = w.getClass().getField("teStore");
        teStore.set(w, Class.forName("hx").getConstructor().newInstance());
        ok(WorldOps.setInv(w, 24, 64, 24, 0, 26, 5, 3, false).endsWith("slot=0 <- 26 x5 dmg=3"),
                "setchest hx slot0 <- 26x5 dmg3");
        ok(WorldOps.getInv(w, 24, 64, 24).equals("slots=27 0=5x26 d3"), "getchest 回读 d3（hi.d private）");
        boolean slotRej = false;
        try {
            WorldOps.setInv(w, 24, 64, 24, 27, 26, 1, 0, false);
        } catch (WorldOps.Rejected e) {
            slotRej = e.getMessage().contains("slot-out-of-range [0,27)");
        }
        ok(slotRej, "slot 27 越界 Rejected（27 槽=hx.a 非 181 p）");
        WorldOps.setInv(w, 24, 64, 24, 0, 0, 0, 0, true);
        ok(WorldOps.getInv(w, 24, 64, 24).equals("slots=27 empty"), "setchest clear 回空");
        boolean nf = false;
        String nfMsg = "";
        try {
            WorldOps.getFurnace(w, 24, 64, 24);
        } catch (WorldOps.Rejected e) {
            nf = true;
            nfMsg = e.getMessage();
        } catch (Exception e) {
            nfMsg = e.getClass().getSimpleName();
        }
        ok(nf && "not-furnace".equals(nfMsg), "getFurnace on hx→Rejected not-furnace（int 类型门）实=" + nfMsg);
        Object furn = Class.forName("oy").getConstructor().newInstance();
        teStore.set(w, furn);
        ok(WorldOps.setInv(w, 24, 64, 24, 1, 26, 2, 0, false).endsWith("slot=1 <- 26 x2"),
                "setchest oy slot1 <- 26x2");
        ok(WorldOps.getInv(w, 24, 64, 24).equals("slots=3 1=2x26"), "getchest oy 3 槽");
        furn.getClass().getField("a").setInt(furn, 20);
        furn.getClass().getField("c").setInt(furn, 5);
        ok(WorldOps.getFurnace(w, 24, 64, 24).startsWith("furnace burn=20 cook=5"),
                "getfurnace oy a/c（furnaceBurnTime/furnaceCookTime——同 1501 rg 字母）");
        teStore.set(w, null);
        boolean noTe = false;
        try {
            WorldOps.getInv(w, 5, 5, 5);
        } catch (WorldOps.Rejected e) {
            noTe = "no-tileentity".equals(e.getMessage());
        }
        ok(noTe, "teStore=null→Rejected no-tileentity");

        // ---- UNSUPPORTED 响亮（require 槽名四件）----
        unsupported("grow", new RunnableEx() {
            public void run() throws Exception { WorldOps.growCrops(w, 24, 64, 24); } }, "idCrops");
        unsupported("invulnerable", new RunnableEx() {
            public void run() throws Exception { WorldOps.invulnerable(w, 100); } }, "iframesF");
        unsupported("genscatter", new RunnableEx() {
            public void run() throws Exception { WorldOps.genScatter(w, 14, 1, 64, 1, null); } }, "scatterClassG");
        unsupported("getbiome", new RunnableEx() {
            public void run() throws Exception { WorldOps.biomeAt(w, 24, 24); } }, "biomeWcmM");

        // ---- weather 响亮（判决①：capWeather=FALSE 走 Boolean 门非槽名——WorldOps L1738
        //     "version-gated b1.5+ (capWeather unset for b1.4_01)"，断言分道）----
        checks++;
        try {
            WorldOps.weather(w, true, false);
            throw new AssertionError("weather should UnsupportedOp(capWeather)");
        } catch (WorldOps.UnsupportedOp u) {
            String m = u.getMessage();
            if (!m.contains("version-gated") || !m.contains("b1.4_01")) {
                throw new AssertionError("weather msg=" + m);
            }
        } catch (Exception e) {
            throw new AssertionError("weather wrong: " + e);
        }

        System.out.println("FakeOps1401Test PASS (" + checks + " checks)");
    }
}
