// [B5-ARCHIVE 2026-11-22] headless test archived from %TEMP%\mcaut (source batch: B b1.7.3); run via java/agent/tests/run.ps1
package com.mcworldagent.agent;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

public class FakeOps173Test {

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

    // ei 私有面直读（WorldInfo 分支对账通道=与 WorldOps 同配方 getDeclaredField+setAccessible）
    static Object pri(Object o, String f) throws Exception {
        Field fd = o.getClass().getDeclaredField(f);
        fd.setAccessible(true);
        return fd.get(o);
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
            if (!m.contains("slot " + slot) || !m.contains("b1.7.3")) {
                throw new AssertionError(op + " msg=" + m);
            }
        } catch (Exception e) {
            throw new AssertionError(op + " wrong: " + e);
        }
    }

    @SuppressWarnings("unchecked")
    public static void main(String[] a) throws Exception {
        ok(VersionRegistry.activate("b1.7.3"), "activate 173");
        HookTargets.activate("b1.7.3");

        // ---- 布景：fd 玩家面=字段 d=playerEntities；玩家 gs 构造 (fd)V ----
        Class<?> wc = Class.forName("fd");
        Object w = wc.getConstructor().newInstance();
        Object player = Class.forName("gs").getConstructor(wc).newInstance(w);
        ((List<Object>) get(w, "d")).add(player);
        ok(((List<Object>) get(w, "d")).size() == 1, "playerEntities 首件=活体玩家");
        WorldOps.resolve(wc);

        // ---- 写读面（主写面断代=f(IIII)Z，裸 c 留档）----
        ok(WorldOps.setBlock(w, 5, 64, 5, 1), "place f(IIII)Z");
        ok(WorldOps.getBlockId(w, 5, 64, 5) == 1, "getblock a(III)I");
        WorldOps.setBlock(w, 5, 64, 5, 0);
        ok(WorldOps.getBlockId(w, 5, 64, 5) == 0, "dig");
        ok(WorldOps.getblocks(w, 0, 1, 0, 1, 2, 1).startsWith("getblocks 2x2x2 rle="), "getblocks");
        ok(WorldOps.getMeta(w, 5, 64, 5) == 0, "getmeta g(III)I");

        // ---- tp：d.get(0) 回退 + b(DDDFF)V 双写 aM..aO（pos 断代面 aF..aH→aM..aO）----
        ok(WorldOps.tp(w, 10, 20, 30).startsWith("teleported"), "tp via d.get(0)");
        ok(((Double) get(player, "aM")).doubleValue() == 10.5
                && ((Double) get(player, "aN")).doubleValue() == 20.0
                && ((Double) get(player, "aO")).doubleValue() == 30.5, "tp pos aM/aN/aO");
        ok(((Double) get(player, "aP")).doubleValue() == 0.0
                && ((Double) get(player, "aQ")).doubleValue() == 0.0
                && ((Double) get(player, "aR")).doubleValue() == 0.0, "tp mot aP/aQ/aR zeroed");

        // ---- setlook/setmotion（aS/aT、aP/aQ/aR 直写）----
        ok(WorldOps.setLook(w, 90, -10).startsWith("look yaw"), "setlook");
        ok(((Float) get(player, "aS")).floatValue() == 90f
                && ((Float) get(player, "aT")).floatValue() == -10f, "setlook aS/aT");
        WorldOps.setMotion(w, 500, 1000, -500);
        ok(((Double) get(player, "aP")).doubleValue() == 0.5
                && ((Double) get(player, "aQ")).doubleValue() == 1.0
                && ((Double) get(player, "aR")).doubleValue() == -0.5, "setmotion aP/aQ/aR");

        // ---- WorldInfo 链重点：settime/setspawn 经 fd.x()Lei 五 private 槽读写 ----
        WorldOps.setTime(w, 1000L);
        Object info = wc.getMethod("x").invoke(w);
        ok(info.getClass().getName().equals("ei"), "fd.x() 返回 ei");
        ok(((Long) pri(info, "e")).longValue() == 1000L, "settime 写 ei.e private 经 setAccessible");
        WorldOps.setSpawn(w, 8, 64, 8);
        ok(((Integer) pri(info, "b")) == 8 && ((Integer) pri(info, "c")) == 64
                && ((Integer) pri(info, "d")) == 8, "setspawn 写 ei.b/c/d");
        ok(pri(info, "a") instanceof Long && ((Long) pri(info, "a")).longValue() == 0L,
                "seed=ei.a 非 fd 面");

        // ---- getinfo（同走 timeHolder=WorldInfo 对象）----
        set(w, "q", 2);
        String got = WorldOps.getInfo(w);
        ok(got.equals("time=1000 spawnX=8 spawnY=64 spawnZ=8 seed=0 difficulty=2"),
                "getinfo 全串对账 " + got);

        // ---- getlight n(III)I ----
        ok(WorldOps.getLightValue(w, 5, 64, 5) == 15, "getlight n(III)I");

        // ---- saveWorld：a(Lyb;)V 传 null（IProgressUpdate ru→yb 断代，null 守卫四连用）----
        WorldOps.saveWorld(w);
        ok((Integer) get(w, "saveCalls") == 1, "save via a(Lyb;)V wrapper->body");
        ok((Integer) get(w, "levelSaves") == 1, "body 调 saveLevel r()V");

        // ---- getbiome：fd.a()Lxv -> xv.a(II)Lkd -> kd.n 三锚链 ----
        String b1 = WorldOps.biomeAt(w, 100, 100);
        String b2 = WorldOps.biomeAt(w, -100, -100);
        String b3 = WorldOps.biomeAt(w, 0, 0);
        ok(b1 != null && b1.length() > 0, "getbiome 非空串 " + b1);
        ok("Desert".equals(b1), "getbiome (100,100)=Desert got " + b1);
        ok("Ice Desert".equals(b2), "getbiome (-100,-100)=Ice Desert got " + b2);
        ok("Plains".equals(b3), "getbiome (0,0)=Plains got " + b3);

        // ---- 物品栏面（gs.c→ix；iz.d public 本版无 private 断代）----
        Object inv = get(player, "c");
        Object[] mainArr = (Object[]) get(inv, "a");
        ok(mainArr.length == 36, "invMain iz[36]");
        Constructor<?> isCtor3 = Class.forName("iz").getConstructor(int.class, int.class, int.class);
        mainArr[3] = isCtor3.newInstance(5, 1, 0);
        ok(WorldOps.inv(w).equals("3=1x5 cur=0"), "inv via d.get(0)");
        // give dmg=0：iz 无 (II)V——newStack 走 (III)V 等价配方
        Object[] reg = (Object[]) Class.forName("gm").getField("c").get(null);
        Constructor<?> gmCtor = Class.forName("gm").getDeclaredConstructor(int.class);
        gmCtor.setAccessible(true);
        reg[5] = gmCtor.newInstance(5);
        ok(WorldOps.give(w, 5, 2, 0).startsWith("gave"), "give via newStack (III)V");
        ok("gave 5 x1 dmg=3".equals(WorldOps.give(w, 5, 1, 3)), "give dmg=3");
        ok(WorldOps.inv(w).contains(" d3"), "inv reads iz.d");
        ok(WorldOps.clearInv(w).startsWith("cleared 36+4"), "clearinv 36+4");

        // ---- health 面（ls.Y 消费）----
        ok(WorldOps.getPlayer(w).contains("health=10"), "getplayer");
        ok("health 10 -> 15".equals(WorldOps.setHealth(w, 15)), "sethealth");
        ok((Integer) get(player, "Y") == 15, "sethealth 直写 Y");

        // ---- invulnerable 复活（iframesF=by 档位一欠账清偿，sn.by 直写）----
        ok("iframes 0 -> 100".equals(WorldOps.invulnerable(w, 100)), "invulnerable via sn.by");
        ok((Integer) get(player, "by") == 100, "invulnerable 直写 by");

        // ---- entitylist 双轨：jc.b(sn) 查表 + 类简名保底 + [h=] 条件段（qw 非 ls 系）----
        Class<?> jcCls = Class.forName("jc");
        java.util.Map<Object, Object> names =
                (java.util.Map<Object, Object>) jcCls.getField("names").get(null);
        names.put(Class.forName("wh"), "Pig");
        names.put(Class.forName("qw"), "PrimedTnt");
        Object pig = Class.forName("wh").getConstructor(wc).newInstance(w);
        set(pig, "aM", 1.2); set(pig, "aN", 64.0); set(pig, "aO", 2.7);
        Object tnt = Class.forName("qw").getConstructor(wc, double.class, double.class, double.class)
                .newInstance(w, 0.0, 0.0, 0.0);
        Object ghost = Class.forName("ii").getConstructor(wc).newInstance(w); // 未注册→类简名保底
        Method addE = wc.getMethod("b", Class.forName("sn")); // spawnEntity b(Lsn;)Z 断代
        addE.invoke(w, pig);
        addE.invoke(w, tnt);
        addE.invoke(w, ghost);
        String el = WorldOps.entityList(w);
        ok(el.contains("Pig@1.2,64.0,2.7[h=10]"), "entitylist 查表+health 段 " + el);
        ok(el.contains("PrimedTnt@0.0,0.0,0.0") && !el.contains("PrimedTnt@0.0,0.0,0.0[h="),
                "entitylist tnt 无 h 段（qw 非 ls 系）");
        ok(el.contains("ii@"), "entitylist 类简名保底");

        // ---- spawn/drop/explode/playsound/clear ----
        String sp = WorldOps.spawn(w, "sheep", 1, 64, 1);
        ok(sp.startsWith("spawned sheep"), "spawn " + sp);
        List<?> pList = (List<?>) get(w, "b");
        ok(pList.size() == 4, "spawn 入列 loadedEntityList b（o()List 同表）");
        ok(WorldOps.spawn(w, "mob", 1, 64, 1).startsWith("spawned mob"), "spawn mob=ii EntityCreature");
        ok(WorldOps.spawn(w, "giant", 1, 64, 1).startsWith("spawned giant"), "spawn giant=nt 构造健在");
        // tnt 重点：FFF miss→DDD fallback（断代延续 b1.2_02）
        ok(WorldOps.spawn(w, "tnt", 1, 64, 1).startsWith("spawned tnt"), "spawn tnt=qw DDD fallback");
        ok(WorldOps.drop(w, 5, 1, 0, 1, 64, 1).startsWith("dropped"), "drop hl");
        Object item = pList.get(pList.size() - 1);
        ok(item.getClass().getName().equals("hl"), "drop 实体=hl");
        names.put(Class.forName("hl"), "Item");
        ok(WorldOps.entityList(w).contains("Item@"), "entitylist Item=hl 注册名");
        // explode：返回 qx 对象但 invoke 面不消费返回值（返回值 V→qx 断代留档）
        ok(WorldOps.explode(w, 1, 64, 1, 1).startsWith("exploded"), "explode");
        ok((Integer) get(w, "expl") == 1, "explode 调用面 a(Lsn;DDDF)Lqx");
        ok(WorldOps.playSound(w, 1, 64, 1, "random.fizz", 5, 10).startsWith("played"), "playsound");
        ok((Integer) get(w, "snd") == 1, "playsound 调用面 a(DDD,String,FF)V");
        // clear 核心：setEntityDead=实例 e(Lsn)V 断代——置 be=dead
        ok(WorldOps.clearGroundItems(w, null, null, null, null).startsWith("cleared 1"),
                "clear hl 实体（loaded 仅 hl 命中）");
        ok((Boolean) get(item, "be"), "setEntityDead 实例 e(Lsn)V 置 be=dead");
        ok((Integer) get(w, "killCalls") == 1, "clear 收 world 调用（实例版对账）");

        // ---- D8 无头半壁（案卷 §6.1 零取证四连+门控首发）----
        // weather：capWeather=TRUE（build173 verscan 锚）+ j/l 浮值面满值写
        String wx = WorldOps.weather(w, true, false);
        ok(wx.contains("raining=true") && wx.contains("thunder=false"), "weather " + wx);
        ok(((Float) get(w, "j")).floatValue() == 1.0F, "rainingStrength=1.0 满值");
        ok(((Float) get(w, "l")).floatValue() == 0.0F, "thunderingStrength=0.0");
        // h6-B 旗标双写转正：ei public b(Z)→l:Z=raining/a(Z)→n:Z=thundering
        // （getter 字母洗牌 o()Z→l/m()Z→n，NBT 地面真值 javap 对榫）
        ok(wx.contains("flag setters"), "weather 双写回显 " + wx);
        Object eiWi = pri(w, "info");
        java.lang.reflect.Field eifL = eiWi.getClass().getDeclaredField("l");
        java.lang.reflect.Field eifN = eiWi.getClass().getDeclaredField("n");
        eifL.setAccessible(true);
        eifN.setAccessible(true);
        ok(eifL.getBoolean(eiWi) && !eifN.getBoolean(eiWi), "weather 旗标 ei.l=true/ei.n=false 双写");
        WorldOps.weather(w, false, true);
        ok(!eifL.getBoolean(eiWi) && eifN.getBoolean(eiWi), "weather 反向旗标双写 (l=false/n=true)");
        // kill：clear 泛化=全表除玩家（gs 在 d 玩家列不在 b 实体表，身份豁免双保险）
        int alive = pList.size();
        String kl = WorldOps.killEntities(w, null, null, null, null);
        ok(kl.startsWith("killed " + alive), "kill 全表 " + kl);
        ok((Integer) get(w, "killCalls") == 1 + alive, "kill 调用面=setEntityDead 同 clear");
        // kill 圆柱过滤：r=0 中心 (9,64,9) 无实体→killed 0 不可能（全表已死仍计数）——
        // 改验半径拒参：缺 r→Rejected
        boolean rej = false;
        try {
            WorldOps.killEntities(w, Integer.valueOf(9), Integer.valueOf(64),
                    Integer.valueOf(9), null);
        } catch (WorldOps.Rejected rj) {
            rej = true;
        }
        ok(rej, "kill(x,y,z)缺 r→Rejected");
        // clone：2x1x1 源（石+气）→目标 (3,64,3)；石改 1 格，气对气零改
        WorldOps.setBlock(w, 1, 64, 1, 1);
        WorldOps.setBlock(w, 2, 64, 1, 0);
        String cl = WorldOps.cloneRegion(w, 1, 64, 1, 2, 64, 1, 3, 64, 3);
        ok(cl.contains("changed=1") && cl.contains("2x1x1"), "clone " + cl);
        ok(WorldOps.getBlockId(w, 3, 64, 3) == 1, "clone 目标=石（setBlockWithNotify 面）");
        boolean nc = false;
        try {
            WorldOps.cloneRegion(w, 1, 64, 1, 1, 64, 1, 1, 64, 1);
        } catch (WorldOps.NoChange e) {
            nc = true;
        }
        ok(nc, "clone 原位零变更→NoChange");
        // clone 越界目标角→Rejected（validateCoords 面）
        rej = false;
        try {
            WorldOps.cloneRegion(w, 0, 0, 0, 1, 1, 1, 128, 128, 128);
        } catch (WorldOps.Rejected rj) {
            rej = true;
        }
        ok(rej, "clone 目标越界→Rejected");
        // setdifficulty：q 字段面+语义标注（level-side，游戏闸在客户端）
        String sd = WorldOps.setDifficulty(w, 3);
        ok(((Integer) get(w, "q")) == 3, "setdifficulty q=3");
        ok(sd.contains("level-side") && sd.contains("GameSettings"), "setdifficulty 语义标清");
        rej = false;
        try {
            WorldOps.setDifficulty(w, 4);
        } catch (WorldOps.Rejected rj) {
            rej = true;
        }
        ok(rej, "difficulty 越界 [0,3]→Rejected");
        // kind 大小写归一化（"Pig"→pig 表键）
        ok(WorldOps.spawn(w, "Pig", 1, 64, 1).startsWith("spawned pig"), "kind 归一化");
        ok(WorldOps.spawn(w, " TnT ", 1, 64, 1).startsWith("spawned tnt"), "kind trim+归一");

        // ---- 档位一 UNSUPPORTED 响亮（6 件；invulnerable 已复活不在此列）----
        set(w, "teStore", Class.forName("ow").getConstructor().newInstance());
        unsupported("setmeta", new RunnableEx() {
            public void run() throws Exception { WorldOps.setMeta(w, 1, 64, 1, 1); } }, "observersF");
        unsupported("grow", new RunnableEx() {
            public void run() throws Exception { WorldOps.growCrops(w, 1, 64, 1); } }, "idCrops");
        // h8 批：TE 三件翻正向（lw 三槽 a/f_/a + sk burn=a/cook=c + js 27 槽 a 撞
        // 数组形，真 jar tiny+javap 转正），仅 genscatter 维持 unsupported。
        java.lang.reflect.Field teStore = w.getClass().getField("teStore");
        Object furn = Class.forName("sk").getConstructor().newInstance();
        teStore.set(w, furn);
        ok(WorldOps.setInv(w, 1, 64, 1, 1, 5, 3, 0, false).endsWith("slot=1 <- 5 x3"),
                "setchest sk slot1 <- 5x3");
        ok(WorldOps.getInv(w, 1, 64, 1).equals("slots=3 1=3x5"), "getchest sk 3 槽");
        furn.getClass().getField("a").setInt(furn, 20);
        furn.getClass().getField("c").setInt(furn, 5);
        ok(WorldOps.getFurnace(w, 1, 64, 1).startsWith("furnace burn=20 cook=5 burning=true 1=3x5"),
                "getfurnace sk a/c（tiny furnaceBurnTime/furnaceCookTime）");
        Object chest = Class.forName("js").getConstructor().newInstance();
        teStore.set(w, chest);
        ok(WorldOps.getInv(w, 2, 64, 2).equals("slots=27 empty"), "getchest js 27 槽空");
        WorldOps.setInv(w, 2, 64, 2, 26, 5, 64, 0, false);
        ok(WorldOps.getInv(w, 2, 64, 2).endsWith("26=64x5"), "setchest slot26 边界");
        WorldOps.setInv(w, 2, 64, 2, 26, 0, 0, 0, true);
        ok(WorldOps.getInv(w, 2, 64, 2).equals("slots=27 empty"), "setchest clear 回空");
        boolean slotRej = false;
        try {
            WorldOps.setInv(w, 2, 64, 2, 27, 5, 1, 0, false);
        } catch (Exception e) {
            slotRej = e instanceof WorldOps.Rejected;
        }
        ok(slotRej, "slot-out-of-range Rejected");
        boolean nf = false;
        String nfMsg = "";
        try {
            WorldOps.getFurnace(w, 2, 64, 2);
        } catch (WorldOps.Rejected e) {
            nf = true;
            nfMsg = e.getMessage();
        } catch (Exception e) {
            nfMsg = e.getClass().getSimpleName();
        }
        ok(nf && "not-furnace".equals(nfMsg),
                "getfurnace on js → Rejected not-furnace（a 撞数组型 int 门）实=" + nfMsg);
        teStore.set(w, null);
        unsupported("genscatter", new RunnableEx() {
            public void run() throws Exception { WorldOps.genScatter(w, 14, 1, 64, 1, null); } }, "scatterClassG");

        // ---- h9 批 sleep 面转正（capSleep b1.5+；gs.u/a + fd.m protected 上溯泛化）----
        java.lang.reflect.Field gsU = player.getClass().getDeclaredField("u");
        java.lang.reflect.Field gsA = player.getClass().getDeclaredField("a");
        gsU.setAccessible(true);
        gsA.setAccessible(true);
        String sl = WorldOps.setSleep(w, true);
        ok(sl.startsWith("sleep off -> on timer 0 -> 100"), "sleep 回显 " + sl);
        ok(gsU.getBoolean(player), "gs.u sleeping=true");
        ok(gsA.getInt(player) == 100, "gs.a sleepTimer=100");
        ok(((Integer) get(w, "sleepFlagRefreshes")).intValue() == 1, "fd.m 刷新 x1（protected 泛化）");
        WorldOps.setSleep(w, false);
        ok(!gsU.getBoolean(player) && gsA.getInt(player) == 0, "wake 清零 u=false/timer=0");
        ok(((Integer) get(w, "sleepFlagRefreshes")).intValue() == 2, "fd.m 刷新 x2");

        System.out.println("FakeOps173Test PASS (" + checks + " checks)");
    }
}
