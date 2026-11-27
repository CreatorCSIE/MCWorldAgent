// b1.6 假件面活体（JSON-only 版，无头窗 V-7 批 2026-11-27）：root16 假件树 + WorldOps
// 全反射面。表源=b1.6.json（mca-fingerprint-v1 档案件，经 A2Table16Test.injectTable()
// 测试侧转写注入 BY_ID/BY_SHA1/active——非 FingerprintLoader 运行时件，详 A2 头注）。
// 重点腿：方块读写（setBlockM=f/getBlockM=a——1401/1501=e 断代）、tp（d.get(0)+aL..aQ
// 直写+b(DDDFF)V）、getinfo/settime/setspawn（WorldInfo 分支：fb.x()Leg; 五 private 槽）、
// save 双参 (false,null)（a(ZLxs;)V null 守卫逐位同形 1401）、**weather 双写 OK 腿**
// （判决①翻面：fb protected float j/l+eg l/n 旗标 setter b/a——1501 C/E private 配方
// 换 protected 面，WorldOps getDeclaredField+setAccessible 同法消费）、**getbiome 首测腿**
// （判决②翻面：fb.a()Lxm;→xm.a(II)Ljz;→jz.n——1501 wl/jj/m 三锚换形 a/a/n）、
// sleep 四门（16 真实序 e→b→c→d，gq_c @0-329 实锤：门1 N()Z||!W()Z、门2 fb.t.wr.c、
// 门3 fb.f()、门4 距离 3/2/3；OK 尾 u/b/a+!fb.B→fb.y()V）、tick NPE（a=null→am() 裸
// 解引用）、wake null-safe（@24 ifnull）、setchest d_ 回环（jo private iw[] a 27 槽+
// iw.d private+sf 熔炉门）、UNSUPPORTED 响亮三件（grow/invulnerable/genscatter——
// weather/getbiome 已翻面为 OK 腿，与 1401 五件响亮对照）。
package com.mcworldagent.agent;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

public class FakeOps16Test {

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

    static Object getv(Object o, String f) throws Exception {
        Field fd = o.getClass().getField(f);
        fd.setAccessible(true);
        return fd.get(o);
    }

    // gq/fb/eg 私有/保护面直读（与 WorldOps 同配方 getDeclaredField+setAccessible）
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
            if (!m.contains("slot " + slot) || !m.contains("b1.6")) {
                throw new AssertionError(op + " msg=" + m);
            }
        } catch (Exception e) {
            throw new AssertionError(op + " wrong: " + e);
        }
    }

    @SuppressWarnings("unchecked")
    public static void main(String[] a) throws Exception {
        A2Table16Test.injectTable();
        ok(VersionRegistry.activate("b1.6"), "activate 16");
        HookTargets.activate("b1.6");

        // ---- 布景：fb 玩家面=字段 d=playerEntities；玩家 gq 构造 (fb)V ----
        Class<?> wc = Class.forName("fb");
        Object w = wc.getConstructor().newInstance();
        Object player = Class.forName("gq").getConstructor(wc).newInstance(w);
        ((List<Object>) getv(w, "d")).add(player);
        ok(((List<Object>) getv(w, "d")).size() == 1, "playerEntities 首件=活体玩家");
        WorldOps.resolve(wc);

        // ---- 方块读写面（setBlockM=f (IIII)Z / getBlockM=a (III)I——1401/1501=e 断代）----
        WorldOps.setBlock(w, 24, 64, 24, 26);
        ok(WorldOps.getBlockId(w, 24, 64, 24) == 26, "setBlock/getBlock f/a 对榫");
        ok(WorldOps.getBlockId(w, 1, 1, 1) == 0, "空气保底 0");

        // ---- tp（playerListF=d get(0) + aL..aQ 直写 + setPosRot b(DDDFF)V）----
        String tp = WorldOps.tp(w, 10, 64, 10);
        ok(tp.startsWith("teleported from x=0.0 y=0.0 z=0.0 -> x=10 y=64 z=10"), "tp 回显 " + tp);
        ok(((Double) getv(player, "aL")).doubleValue() == 10.5, "tp aL=10.5（posX——16 站 +1/版规律）");
        ok(((Double) getv(player, "aM")).doubleValue() == 64.0, "tp aM=64（posY）");
        ok(((Double) getv(player, "aQ")).doubleValue() == 0.0, "tp aQ 清零（motZ）");

        // ---- getinfo/settime/setspawn（WorldInfo 分支：fb.x()Leg; 五 private 槽）----
        Object wi = wc.getMethod("x").invoke(w);
        ok(WorldOps.getInfo(w).contains("difficulty=0"), "getinfo 初值");
        WorldOps.setTime(w, 1000L);
        ok(((Long) pri(wi, "e")).longValue() == 1000L, "settime 写 eg.e private");
        WorldOps.setSpawn(w, 1, 2, 3);
        ok(((Integer) pri(wi, "b")).intValue() == 1 && ((Integer) pri(wi, "c")).intValue() == 2
                && ((Integer) pri(wi, "d")).intValue() == 3, "setspawn 写 eg.b/c/d");
        ok(WorldOps.getInfo(w).contains("time=1000 spawnX=1 spawnY=2 spawnZ=3"), "getinfo 回读");

        // ---- save 双参腿（断代带延续：resolve (ZLxs;)V 分支 + invoke (false,null)）----
        WorldOps.saveWorld(w);
        ok(((Integer) getv(w, "saveCalls")).intValue() == 1, "saveWorld 绑定 fb.a(ZLxs;)V 非 a()Lxm");
        WorldOps.saveWorld(w);
        ok(((Integer) getv(w, "saveCalls")).intValue() == 2, "saveWorld 可重复触发");

        // ---- weather 双写 OK 腿（判决①翻面：fb protected float j/l + eg l/n 旗标 setter）----
        String wx = WorldOps.weather(w, true, false);
        ok(wx.contains("raining=true") && wx.contains("flag setters"), "weather 回显 " + wx);
        ok(((Float) pri(w, "j")).floatValue() == 1.0F, "fb.j rainStrength=1.0（protected 面）");
        ok(((Float) pri(w, "l")).floatValue() == 0.0F, "fb.l thunderStrength=0.0");
        ok(Boolean.TRUE.equals(pri(wi, "l")), "eg.l raining 旗标（setter b(Z)V 走位）");
        ok(Boolean.FALSE.equals(pri(wi, "n")), "eg.n thundering 旗标（setter a(Z)V 走位）");

        // ---- inv 读面（invF=c / iu a[36]/b[4]/c + iw a/c public d private）----
        Object inv = getv(player, "c");
        Object[] mainArr = (Object[]) getv(inv, "a");
        Constructor<?> iwCtor = Class.forName("iw").getConstructor(int.class, int.class, int.class);
        mainArr[3] = iwCtor.newInstance(26, 1, 0);
        ok(WorldOps.inv(w).equals("3=1x26 cur=0"), "inv via d.get(0)");
        ok(WorldOps.clearInv(w).startsWith("cleared 36+4"), "clearinv 36+4");

        // ---- getbiome 首测腿（判决②翻面：fb.a()Lxm;→xm.a(II)Ljz;→jz.n 三锚 a/a/n）----
        ok(WorldOps.biomeAt(w, 24, 24).equals("Plains"), "getbiome 初值 Plains");
        Object wcm = getv(w, "wcmScaffold");
        Object biome = getv(wcm, "biome");
        biome.getClass().getField("n").set(biome, "Desert");
        ok(WorldOps.biomeAt(w, 24, 24).equals("Desert"), "getbiome 写读回环（jz.n public）");

        // ---- sleep 四门（16 真实序：门1 已睡/不可睡→e；门2 provider→b；门3 白天→c；门4 距离→d）----
        Field gqB = player.getClass().getDeclaredField("b");
        gqB.setAccessible(true);
        boolean rejS = false;
        String sMsg = "";
        // 门 1：已睡拒（protected u:Z→N()Z→cu.e；⚠️ 16 门1 探针=N/W 非 1501 O/W、1401 M/U）
        priSet(player, "u", Boolean.TRUE);
        try {
            WorldOps.setSleep(w, true, 24, 64, 24);
        } catch (WorldOps.Rejected rj) {
            rejS = true;
            sMsg = rj.getMessage();
        }
        ok(rejS && sMsg.contains("sleep-status=e"), "已睡→Rejected e " + sMsg);
        priSet(player, "u", Boolean.FALSE);
        // 门 2：provider 禁睡（fb.t:Lwr;.c:Z→cu.b）
        Object prov = getv(w, "t");
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
        // 门 3：白天拒（fb.f()Z→cu.c）
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
        // 门 4：距远拒（|aL-x|>3→cu.d；阈值 3/2/3）
        set(player, "aL", Double.valueOf(100.0));
        rejS = false;
        try {
            WorldOps.setSleep(w, true, 24, 64, 24);
        } catch (WorldOps.Rejected rj) {
            rejS = true;
            sMsg = rj.getMessage();
        }
        ok(rejS && sMsg.contains("sleep-status=d"), "距>3/2/3→Rejected d " + sMsg);
        set(player, "aL", Double.valueOf(24.5));
        set(player, "aN", Double.valueOf(24.5));
        // 合法态：床 26@(24,64,24)+夜+距内
        WorldOps.setBlock(w, 24, 64, 24, 26);
        String sl = WorldOps.setSleep(w, true, 24, 64, 24);
        ok(sl.startsWith("sleep on @24,64,24 -> OK"), "sleep on 回显 " + sl);
        ok(Boolean.TRUE.equals(pri(player, "u")), "u=sleeping true（protected 写面——真 jar 件）");
        ok(gqB.getInt(player) == 0, "b=timer 0 合法态（tiny 错位件 a 勿借）");
        ok(pri(player, "a") != null, "a=bedChunkCoordinates 置位（private bp a）");
        ok(((Integer) getv(w, "sleepFlagRefreshes")).intValue() == 1, "fb.y()V 刷新 x1（!fb.B 路径）");
        // tick 崩溃回归：合法态 w_() 存活（lo.w_ super 回调）；a=null 必 NPE（am() 裸解引用）
        Method tick = player.getClass().getMethod("w_");
        tick.invoke(player);
        ok(Boolean.TRUE.equals(pri(player, "u")), "合法态 tick 存活不崩（u 仍 true）");
        priSet(player, "a", null);
        boolean npe = false;
        try {
            tick.invoke(player);
        } catch (java.lang.reflect.InvocationTargetException e) {
            npe = e.getCause() instanceof NullPointerException;
        }
        ok(npe, "a=null+sleeping tick→NPE（am() 崩溃面三版翻版复现）");
        // off：wakeUpPlayer(true,true,false)（重挂 a 后 u=false/timer=0/刷新 x2）
        priSet(player, "a", Class.forName("bp").getConstructor(int.class, int.class, int.class)
                .newInstance(24, 64, 24));
        ok(WorldOps.setSleep(w, false, 0, 0, 0).startsWith("sleep off -> wakeUpPlayer"), "wake 回显");
        ok(!Boolean.TRUE.equals(pri(player, "u")) && gqB.getInt(player) == 0, "wake 清零 u=false/b=0");
        ok(((Integer) getv(w, "sleepFlagRefreshes")).intValue() == 2, "fb.y()V 刷新 x2");
        // wake null-safe：a=null 再 off 不抛（gq_c a(ZZZ)V @24 ifnull 实锤）
        priSet(player, "a", null);
        ok(WorldOps.setSleep(w, false, 0, 0, 0).startsWith("sleep off"), "wake a=null null-safe");

        // ---- setchest/getchest（jo private iw[] a 27 槽；iw.d private 回环；sf 熔炉门）----
        Object[] reg = (Object[]) Class.forName("gk").getField("c").get(null);
        Constructor<?> gkCtor = Class.forName("gk").getDeclaredConstructor(int.class);
        gkCtor.setAccessible(true);
        reg[26] = gkCtor.newInstance(26);
        Field teStore = w.getClass().getField("teStore");
        teStore.set(w, Class.forName("jo").getConstructor().newInstance());
        ok(WorldOps.setInv(w, 24, 64, 24, 0, 26, 5, 3, false).endsWith("slot=0 <- 26 x5 dmg=3"),
                "setchest jo slot0 <- 26x5 dmg3");
        ok(WorldOps.getInv(w, 24, 64, 24).equals("slots=27 0=5x26 d3"), "getchest d_ 回环读 d3（iw.d private）");
        boolean slotRej = false;
        try {
            WorldOps.setInv(w, 24, 64, 24, 27, 26, 1, 0, false);
        } catch (WorldOps.Rejected e) {
            slotRej = e.getMessage().contains("slot-out-of-range [0,27)");
        }
        ok(slotRej, "slot 27 越界 Rejected（27 槽=jo.a）");
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
        ok(nf && "not-furnace".equals(nfMsg), "getFurnace on jo→Rejected not-furnace（int 类型门）实=" + nfMsg);
        Object furn = Class.forName("sf").getConstructor().newInstance();
        teStore.set(w, furn);
        ok(WorldOps.setInv(w, 24, 64, 24, 1, 26, 2, 0, false).endsWith("slot=1 <- 26 x2"),
                "setchest sf slot1 <- 26x2");
        ok(WorldOps.getInv(w, 24, 64, 24).equals("slots=3 1=2x26"), "getchest sf 3 槽");
        furn.getClass().getField("a").setInt(furn, 20);
        furn.getClass().getField("c").setInt(furn, 5);
        ok(WorldOps.getFurnace(w, 24, 64, 24).startsWith("furnace burn=20 cook=5"),
                "getfurnace sf a/c（三版同字母）");
        teStore.set(w, null);
        boolean noTe = false;
        try {
            WorldOps.getInv(w, 5, 5, 5);
        } catch (WorldOps.Rejected e) {
            noTe = "no-tileentity".equals(e.getMessage());
        }
        ok(noTe, "teStore=null→Rejected no-tileentity");

        // ---- UNSUPPORTED 响亮三件（weather/getbiome 已翻面为 OK 腿——1401 五件对照）----
        unsupported("grow", new RunnableEx() {
            public void run() throws Exception { WorldOps.growCrops(w, 24, 64, 24); } }, "idCrops");
        unsupported("invulnerable", new RunnableEx() {
            public void run() throws Exception { WorldOps.invulnerable(w, 100); } }, "iframesF");
        unsupported("genscatter", new RunnableEx() {
            public void run() throws Exception { WorldOps.genScatter(w, 14, 1, 64, 1, null); } }, "scatterClassG");

        System.out.println("FakeOps16Test PASS (" + checks + " checks)");
    }
}
