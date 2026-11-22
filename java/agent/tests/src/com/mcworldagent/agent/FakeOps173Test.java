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

        // ---- 档位一 UNSUPPORTED 响亮（6 件；invulnerable 已复活不在此列）----
        set(w, "teStore", Class.forName("ow").getConstructor().newInstance());
        unsupported("setmeta", new RunnableEx() {
            public void run() throws Exception { WorldOps.setMeta(w, 1, 64, 1, 1); } }, "observersF");
        unsupported("grow", new RunnableEx() {
            public void run() throws Exception { WorldOps.growCrops(w, 1, 64, 1); } }, "idCrops");
        unsupported("getchest", new RunnableEx() {
            public void run() throws Exception { WorldOps.getInv(w, 1, 64, 1); } }, "invSizeM");
        unsupported("setchest", new RunnableEx() {
            public void run() throws Exception { WorldOps.setInv(w, 1, 64, 1, 0, 5, 1, 0, false); } }, "invSizeM");
        unsupported("getfurnace", new RunnableEx() {
            public void run() throws Exception { WorldOps.getFurnace(w, 1, 64, 1); } }, "teBurnF");
        unsupported("genscatter", new RunnableEx() {
            public void run() throws Exception { WorldOps.genScatter(w, 14, 1, 64, 1, null); } }, "scatterClassG");

        System.out.println("FakeOps173Test PASS (" + checks + " checks)");
    }
}
