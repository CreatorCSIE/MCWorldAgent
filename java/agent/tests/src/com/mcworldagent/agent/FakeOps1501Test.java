// b1.5_01 假件面活体（JSON-only 版，无头窗批 2026-11-27）：root1501 假件树 + WorldOps
// 全反射面。表源=b1.5_01.json（mca-fingerprint-v1 档案件，经 A2Table1501Test.injectTable()
// 测试侧转写注入 BY_ID/BY_SHA1/active——非 FingerprintLoader 运行时件，详 A2 头注）。
// 重点腿：sleep 四门（1501 真实序 e→b→c→d，gh_disasm b(III)Lco; @0-104 实锤——无 173/181
// 头部 isRemote 旁路）、tick NPE 崩溃面（x=true+a=null→am() 裸解引用）、wake null-safe、
// setchest/getchest（ii.d private 回环 + iy private ii[] a 27 槽 + rg public a/c 熔炉门）。
package com.mcworldagent.agent;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

public class FakeOps1501Test {

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

    // gh/dy 私有面直读（与 WorldOps 同配方 getDeclaredField+setAccessible——a/b/x/C/E/l/n）
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
            if (!m.contains("slot " + slot) || !m.contains("b1.5_01")) {
                throw new AssertionError(op + " msg=" + m);
            }
        } catch (Exception e) {
            throw new AssertionError(op + " wrong: " + e);
        }
    }

    @SuppressWarnings("unchecked")
    public static void main(String[] a) throws Exception {
        A2Table1501Test.injectTable();
        ok(VersionRegistry.activate("b1.5_01"), "activate 1501");
        HookTargets.activate("b1.5_01");

        // ---- 布景：et 玩家面=字段 d=playerEntities；玩家 gh 构造 (et)V ----
        Class<?> wc = Class.forName("et");
        Object w = wc.getConstructor().newInstance();
        Object player = Class.forName("gh").getConstructor(wc).newInstance(w);
        ((List<Object>) get(w, "d")).add(player);
        ok(((List<Object>) get(w, "d")).size() == 1, "playerEntities 首件=活体玩家");
        WorldOps.resolve(wc);

        // ---- 方块读写面（setBlockM=e (IIII)Z / getBlockM=a (III)I）----
        WorldOps.setBlock(w, 24, 64, 24, 26);
        ok(WorldOps.getBlockId(w, 24, 64, 24) == 26, "setBlock/getBlock e/a 对榫");
        ok(WorldOps.getBlockId(w, 1, 1, 1) == 0, "空气保底 0");

        // ---- tp（playerListF=d get(0) + aK..aP 直写 + setPosRot b(DDDFF)V）----
        String tp = WorldOps.tp(w, 10, 64, 10);
        ok(tp.startsWith("teleported from x=0.0 y=0.0 z=0.0 -> x=10 y=64 z=10"), "tp 回显 " + tp);
        ok(((Double) get(player, "aK")).doubleValue() == 10.5, "tp aK=10.5");
        ok(((Double) get(player, "aL")).doubleValue() == 64.0, "tp aL=64");
        ok(((Double) get(player, "aP")).doubleValue() == 0.0, "tp aP 清零");

        // ---- getinfo/settime/setspawn（WorldInfo 分支：et.x()Ldy 五 private 槽）----
        Object wi = wc.getMethod("x").invoke(w);
        ok(WorldOps.getInfo(w).contains("difficulty=0"), "getinfo 初值");
        WorldOps.setTime(w, 1000L);
        ok(((Long) pri(wi, "e")).longValue() == 1000L, "settime 写 dy.e private");
        WorldOps.setSpawn(w, 1, 2, 3);
        ok(((Integer) pri(wi, "b")).intValue() == 1 && ((Integer) pri(wi, "c")).intValue() == 2
                && ((Integer) pri(wi, "d")).intValue() == 3, "setspawn 写 dy.b/c/d");
        ok(WorldOps.getInfo(w).contains("time=1000 spawnX=1 spawnY=2 spawnZ=3"), "getinfo 回读");

        // ---- save 双参腿（W-6：resolve (ZLwr;)V 分支 + invoke (false,null) 配方）----
        WorldOps.saveWorld(w);
        ok(((Integer) get(w, "saveCalls")).intValue() == 1, "saveWorld 绑定 et.a(ZLwr;)V 非 a()Lwl");
        WorldOps.saveWorld(w);
        ok(((Integer) get(w, "saveCalls")).intValue() == 2, "saveWorld 可重复触发");

        // ---- weather 双写（et private C/E 浮值 + dy l/n 旗标 setter b(Z)/a(Z)）----
        String wx = WorldOps.weather(w, true, false);
        ok(wx.contains("raining=true") && wx.contains("flag setters"), "weather 回显 " + wx);
        ok(((Float) pri(w, "C")).floatValue() == 1.0F, "et.C rainStrength=1.0（private）");
        ok(((Float) pri(w, "E")).floatValue() == 0.0F, "et.E thunderStrength=0.0");
        ok(Boolean.TRUE.equals(pri(wi, "l")), "dy.l raining 旗标");
        ok(Boolean.FALSE.equals(pri(wi, "n")), "dy.n thundering 旗标");

        // ---- inv 读面（invF=f / ih a[36]/b[4]/c + ii a/c public d private）----
        Object inv = get(player, "f");
        Object[] mainArr = (Object[]) get(inv, "a");
        Constructor<?> iiCtor = Class.forName("ii").getConstructor(int.class, int.class, int.class);
        mainArr[3] = iiCtor.newInstance(26, 1, 0);
        ok(WorldOps.inv(w).equals("3=1x26 cur=0"), "inv via d.get(0)");
        ok(WorldOps.clearInv(w).startsWith("cleared 36+4"), "clearinv 36+4");

        // ---- sleep 四门（1501 真实序：门1 已睡/不可睡→e；门2 provider→b；门3 白天→c；门4 距离→d）----
        Field gsB = player.getClass().getDeclaredField("b");
        gsB.setAccessible(true);
        boolean rejS = false;
        String sMsg = "";
        // 门 1：已睡拒（x=true→O()Z→co.e；⚠️ 1501 门序首位，无 isRemote 头旁路）
        priSet(player, "x", Boolean.TRUE);
        try {
            WorldOps.setSleep(w, true, 24, 64, 24);
        } catch (WorldOps.Rejected rj) {
            rejS = true;
            sMsg = rj.getMessage();
        }
        ok(rejS && sMsg.contains("sleep-status=e"), "已睡→Rejected e " + sMsg);
        priSet(player, "x", Boolean.FALSE);
        // 门 2：provider 禁睡（et.o:Lvr;.c:Z→co.b）
        Object prov = get(w, "o");
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
        // 门 3：白天拒（et.f()Z→co.c）
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
        // 门 4：距远拒（|aK-x|>3→co.d；阈值 3/2/3）
        set(player, "aK", Double.valueOf(100.0));
        rejS = false;
        try {
            WorldOps.setSleep(w, true, 24, 64, 24);
        } catch (WorldOps.Rejected rj) {
            rejS = true;
            sMsg = rj.getMessage();
        }
        ok(rejS && sMsg.contains("sleep-status=d"), "距>3/2/3→Rejected d " + sMsg);
        set(player, "aK", Double.valueOf(24.5));
        set(player, "aM", Double.valueOf(24.5));
        // 合法态：床 26@(24,64,24)+夜+距内
        WorldOps.setBlock(w, 24, 64, 24, 26);
        String sl = WorldOps.setSleep(w, true, 24, 64, 24);
        ok(sl.startsWith("sleep on @24,64,24 -> OK"), "sleep on 回显 " + sl);
        ok(Boolean.TRUE.equals(pri(player, "x")), "x=sleeping true（protected 写面）");
        ok(gsB.getInt(player) == 0, "b=timer 0 合法态（非旧裸写 100）");
        ok(pri(player, "a") != null, "a=bedChunkCoordinates 置位（private bk a）");
        ok(((Integer) get(w, "sleepFlagRefreshes")).intValue() == 1, "et.y()V 刷新 x1（!et.v 路径）");
        // tick 崩溃回归：合法态 w_() 存活；a=null 必 NPE（W173 同款 am() 裸解引用模型）
        Method tick = player.getClass().getMethod("w_");
        tick.invoke(player);
        ok(Boolean.TRUE.equals(pri(player, "x")), "合法态 tick 存活不崩（x 仍 true）");
        priSet(player, "a", null);
        boolean npe = false;
        try {
            tick.invoke(player);
        } catch (java.lang.reflect.InvocationTargetException e) {
            npe = e.getCause() instanceof NullPointerException;
        }
        ok(npe, "a=null+sleeping tick→NPE（am() 崩溃面复现）");
        // off：wakeUpPlayer(true,true,false)（重挂 a 后 x=false/timer=0/刷新 x2）
        priSet(player, "a", Class.forName("bk").getConstructor(int.class, int.class, int.class)
                .newInstance(24, 64, 24));
        ok(WorldOps.setSleep(w, false, 0, 0, 0).startsWith("sleep off -> wakeUpPlayer"), "wake 回显");
        ok(!Boolean.TRUE.equals(pri(player, "x")) && gsB.getInt(player) == 0, "wake 清零 x=false/timer=0");
        ok(((Integer) get(w, "sleepFlagRefreshes")).intValue() == 2, "et.y()V 刷新 x2");
        // wake null-safe：a=null 再 off 不抛（disasm @24 ifnull 跳床清理）
        priSet(player, "a", null);
        ok(WorldOps.setSleep(w, false, 0, 0, 0).startsWith("sleep off"), "wake a=null null-safe");

        // ---- setchest/getchest（iy private ii[] a 27 槽；ii.d private 回环；rg 熔炉门）----
        Object[] reg = (Object[]) Class.forName("gb").getField("c").get(null);
        Constructor<?> gbCtor = Class.forName("gb").getDeclaredConstructor(int.class);
        gbCtor.setAccessible(true);
        reg[26] = gbCtor.newInstance(26);
        Field teStore = w.getClass().getField("teStore");
        teStore.set(w, Class.forName("iy").getConstructor().newInstance());
        ok(WorldOps.setInv(w, 24, 64, 24, 0, 26, 5, 3, false).endsWith("slot=0 <- 26 x5 dmg=3"),
                "setchest iy slot0 <- 26x5 dmg3");
        ok(WorldOps.getInv(w, 24, 64, 24).equals("slots=27 0=5x26 d3"), "getchest 回读 d3（ii.d private）");
        boolean slotRej = false;
        try {
            WorldOps.setInv(w, 24, 64, 24, 27, 26, 1, 0, false);
        } catch (WorldOps.Rejected e) {
            slotRej = e.getMessage().contains("slot-out-of-range [0,27)");
        }
        ok(slotRej, "slot 27 越界 Rejected（27 槽=iy.a 非 181 p）");
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
        ok(nf && "not-furnace".equals(nfMsg), "getFurnace on iy→Rejected not-furnace（int 类型门）实=" + nfMsg);
        Object furn = Class.forName("rg").getConstructor().newInstance();
        teStore.set(w, furn);
        ok(WorldOps.setInv(w, 24, 64, 24, 1, 26, 2, 0, false).endsWith("slot=1 <- 26 x2"),
                "setchest rg slot1 <- 26x2");
        ok(WorldOps.getInv(w, 24, 64, 24).equals("slots=3 1=2x26"), "getchest rg 3 槽");
        furn.getClass().getField("a").setInt(furn, 20);
        furn.getClass().getField("c").setInt(furn, 5);
        ok(WorldOps.getFurnace(w, 24, 64, 24).startsWith("furnace burn=20 cook=5"),
                "getfurnace rg a/c（furnaceBurnTime/furnaceCookTime）");
        teStore.set(w, null);
        boolean noTe = false;
        try {
            WorldOps.getInv(w, 5, 5, 5);
        } catch (WorldOps.Rejected e) {
            noTe = "no-tileentity".equals(e.getMessage());
        }
        ok(noTe, "teStore=null→Rejected no-tileentity");

        // ---- UNSUPPORTED 响亮（null 槽三件）----
        unsupported("grow", new RunnableEx() {
            public void run() throws Exception { WorldOps.growCrops(w, 24, 64, 24); } }, "idCrops");
        unsupported("invulnerable", new RunnableEx() {
            public void run() throws Exception { WorldOps.invulnerable(w, 100); } }, "iframesF");
        unsupported("genscatter", new RunnableEx() {
            public void run() throws Exception { WorldOps.genScatter(w, 14, 1, 64, 1, null); } }, "scatterClassG");

        System.out.println("FakeOps1501Test PASS (" + checks + " checks)");
    }
}
