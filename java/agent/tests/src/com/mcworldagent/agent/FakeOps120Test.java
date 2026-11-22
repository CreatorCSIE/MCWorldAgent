// [B5-ARCHIVE 2026-11-22] headless test archived from %TEMP%\mcaut (source batch: A2 a1.2.0); run via java/agent/tests/run.ps1
package com.mcworldagent.agent;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

public class FakeOps120Test {

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
            if (!m.contains("slot " + slot) || !m.contains("a1.2.0")) {
                throw new AssertionError(op + " msg=" + m);
            }
        } catch (Exception e) {
            throw new AssertionError(op + " wrong: " + e);
        }
    }

    @SuppressWarnings("unchecked")
    public static void main(String[] a) throws Exception {
        ok(VersionRegistry.activate("a1.2.0"), "activate 120");
        HookTargets.activate("a1.2.0");

        // ---- 布景：cu 玩家面=字段 d=playerEntities；玩家 dv 构造 (cu)V ----
        Class<?> wc = Class.forName("cu");
        Object w = wc.getConstructor(java.io.File.class, String.class)
                .newInstance(new java.io.File("saves/test"), "world");
        Object player = Class.forName("dv").getConstructor(wc).newInstance(w);
        ((List<Object>) get(w, "d")).add(player);
        ok(((List<Object>) get(w, "d")).size() == 1, "playerEntities 首件=活体玩家");
        WorldOps.resolve(wc);

        // ---- 写读面 ----
        ok(WorldOps.setBlock(w, 5, 64, 5, 1), "place d(IIII)Z");
        ok(WorldOps.getBlockId(w, 5, 64, 5) == 1, "getblock a(III)I");
        WorldOps.setBlock(w, 5, 64, 5, 0);
        ok(WorldOps.getBlockId(w, 5, 64, 5) == 0, "dig");
        ok(WorldOps.getblocks(w, 0, 1, 0, 1, 2, 1).startsWith("getblocks 2x2x2 rle="), "getblocks");
        ok(WorldOps.getMeta(w, 5, 64, 5) == 0, "getmeta e(III)I");

        // ---- tp：worldPlayer 回退 + b(DDDFF)V 双写 ap..ar+am..ao（pos 消费面=ap/aq/ar） ----
        ok(WorldOps.tp(w, 10, 20, 30).startsWith("teleported"), "tp via d.get(0)");
        ok(((Double) get(player, "ap")).doubleValue() == 10.5
                && ((Double) get(player, "aq")).doubleValue() == 20.0
                && ((Double) get(player, "ar")).doubleValue() == 30.5, "tp pos ap/aq/ar");
        ok(((Double) get(player, "am")).doubleValue() == 10.5
                && ((Double) get(player, "an")).doubleValue() == 20.0
                && ((Double) get(player, "ao")).doubleValue() == 30.5, "tp prevPos am..ao 同步双写");
        ok(((Double) get(player, "as")).doubleValue() == 0.0
                && ((Double) get(player, "at")).doubleValue() == 0.0
                && ((Double) get(player, "au")).doubleValue() == 0.0, "tp mot as/at/au zeroed");

        // ---- setlook/setmotion 面（av/aw、as/at/au 直写；setmotion 协议=整数千分位） ----
        ok(WorldOps.setLook(w, 90, -10).startsWith("look yaw"), "setlook");
        ok(((Float) get(player, "av")).floatValue() == 90f
                && ((Float) get(player, "aw")).floatValue() == -10f, "setlook av/aw");
        WorldOps.setMotion(w, 500, 1000, -500);
        ok(((Double) get(player, "as")).doubleValue() == 0.5
                && ((Double) get(player, "at")).doubleValue() == 1.0
                && ((Double) get(player, "au")).doubleValue() == -0.5, "setmotion as/at/au");

        // ---- 时间/出生点 ----
        WorldOps.setTime(w, 1000L);
        ok(((Long) get(w, "e")).longValue() == 1000L, "settime e");
        WorldOps.setSpawn(w, 8, 64, 8);
        ok(((Integer) get(w, "n")) == 8 && ((Integer) get(w, "o")) == 64
                && ((Integer) get(w, "p")) == 8, "setspawn n/o/p");

        // ---- getinfo：difficultyF=k 已钉 ----
        set(w, "k", 2);
        String info = WorldOps.getInfo(w);
        ok(info.equals("time=1000 spawnX=8 spawnY=64 spawnZ=8 seed=0 difficulty=2"),
                "getinfo 全串对账 " + info);

        // ---- getlight ----
        ok(WorldOps.getLightValue(w, 5, 64, 5) == 15, "getlight j(III)I");

        // ---- saveWorld：(Lph;)V 包装委托 a(ZLph;)V 本体（含 ph null 守卫+调 o()V） ----
        WorldOps.saveWorld(w);
        ok((Integer) get(w, "saveCalls") == 1, "save via a(Lph;)V wrapper->body");
        ok((Integer) get(w, "levelSaves") == 1, "body 调 saveLevel o()V");

        // ---- getbiome（本批新增面）：cu.a()pb -> pb.a(II)fy -> fy.m 三锚链走通 ----
        String b1 = WorldOps.biomeAt(w, 100, 100);
        String b2 = WorldOps.biomeAt(w, -100, -100);
        String b3 = WorldOps.biomeAt(w, 0, 0);
        ok(b1 != null && b1.length() > 0, "getbiome 非空串 " + b1);
        ok("Desert".equals(b1), "getbiome (100,100)=Desert got " + b1);
        ok("Ice Desert".equals(b2), "getbiome (-100,-100)=Ice Desert got " + b2);
        ok("Plains".equals(b3), "getbiome (0,0)=Plains got " + b3);

        // ---- 物品栏面（fh a=fi[37]（javap -c fh 实锤 bipush 37）/b=fi[4]/d=cur；fi a/c/d 同形） ----
        Object inv = get(player, "e");
        Object[] mainArr = (Object[]) get(inv, "a");
        ok(mainArr.length == 37, "invMain fi[37]");
        Constructor<?> isCtor = Class.forName("fi").getConstructor(int.class, int.class);
        mainArr[3] = isCtor.newInstance(5, 1);
        ok(WorldOps.inv(w).equals("3=1x5 cur=0"), "inv via d.get(0)");
        // give：注册表校验（dr.c[5] 非 null，protected dr(int)）+ addStack a(fi)Z
        Object[] reg = (Object[]) Class.forName("dr").getField("c").get(null);
        java.lang.reflect.Constructor<?> drCtor = Class.forName("dr").getDeclaredConstructor(int.class);
        drCtor.setAccessible(true);
        reg[5] = drCtor.newInstance(5);
        ok(WorldOps.give(w, 5, 2, 0).startsWith("gave"), "give");
        ok(WorldOps.clearInv(w).startsWith("cleared 37+4"), "clearinv 37+4");

        // ---- health 面（gx.J 消费）+ iframes（lk.aW） ----
        ok(WorldOps.getPlayer(w).contains("health=10"), "getplayer");
        ok("health 10 -> 15".equals(WorldOps.setHealth(w, 15)), "sethealth");
        ok((Integer) get(player, "J") == 15, "sethealth 直写 J");
        ok(WorldOps.invulnerable(w, 100).startsWith("iframes"), "invulnerable aW");
        ok((Integer) get(player, "aW") == 100, "invulnerable 直写 aW");

        // ---- entitylist 双轨：fj.b(lk) 查表 + 类简名保底 + [h=] 条件段（kg 非 Living） ----
        Class<?> fjCls = Class.forName("fj");
        java.util.Map<Object, Object> names =
                (java.util.Map<Object, Object>) fjCls.getField("names").get(null);
        names.put(Class.forName("oc"), "Pig");
        names.put(Class.forName("kg"), "PrimedTnt");
        Object pig = Class.forName("oc").getConstructor(wc).newInstance(w);
        set(pig, "ap", 1.2); set(pig, "aq", 64.0); set(pig, "ar", 2.7);
        Object tnt = Class.forName("kg").getConstructor(wc, float.class, float.class, float.class)
                .newInstance(w, 0f, 0f, 0f);
        Object ghost = Class.forName("ex").getConstructor(wc).newInstance(w); // 未注册→类简名保底
        Method addE = wc.getMethod("a", Class.forName("lk")); // spawnEntity a(lk)Z
        addE.invoke(w, pig);
        addE.invoke(w, tnt);
        addE.invoke(w, ghost);
        String el = WorldOps.entityList(w);
        ok(el.contains("Pig@1.2,64.0,2.7[h=10]"), "entitylist 查表+health 段 " + el);
        ok(el.contains("PrimedTnt@0.0,0.0,0.0"), "entitylist tnt 无 h 段（kg 非 gx 系）");
        ok(el.contains("ex@"), "entitylist 类简名保底");

        // ---- spawn/drop/explode/playsound/clear ----
        String sp = WorldOps.spawn(w, "sheep", 1, 64, 1);
        ok(sp.startsWith("spawned sheep"), "spawn " + sp);
        List<?> pList = (List<?>) get(w, "b");
        ok(pList.size() == 4, "spawn 入列 loadedEntityList b");
        ok(WorldOps.spawn(w, "mob", 1, 64, 1).startsWith("spawned mob"), "spawn mob=ex EntityCreature");
        ok(WorldOps.spawn(w, "giant", 1, 64, 1).startsWith("spawned giant"), "spawn giant=ig 构造健在");
        ok(WorldOps.spawn(w, "tnt", 1, 64, 1).startsWith("spawned tnt"), "spawn tnt=kg(cu,FFF)V");
        ok(WorldOps.drop(w, 5, 1, 0, 1, 64, 1).startsWith("dropped"), "drop ei");
        Object item = pList.get(pList.size() - 1);
        ok(item.getClass().getName().equals("ei"), "drop 实体=ei");
        names.put(Class.forName("ei"), "Item");
        ok(WorldOps.entityList(w).contains("Item@"), "entitylist Item=ei 注册名");
        ok(WorldOps.explode(w, 1, 64, 1, 1).startsWith("exploded"), "explode");
        ok((Integer) get(w, "expl") == 1, "explode 调用面 a(Llk;DDDF)V");
        ok(WorldOps.playSound(w, 1, 64, 1, "random.fizz", 5, 10).startsWith("played"), "playsound");
        ok((Integer) get(w, "snd") == 1, "playsound 调用面 a(DDDLjava/lang/String;FF)V");
        // clear 核心：setEntityDead=实例 d(lk)V——Modifier.isStatic=false 收 world 调用
        ok(WorldOps.clearGroundItems(w, null, null, null, null).startsWith("cleared 1"),
                "clear ei 实体（loaded 含 pig/tnt/ghost/sheep/mob/giant/kg 仅 ei 命中）");
        ok((Boolean) get(item, "aF"), "setEntityDead 实例 d(lk)V 置 aF=dead");
        ok((Integer) get(w, "killCalls") == 1, "clear 收 world 调用（实例版对账）");

        // ---- 6 件 unconfirmed → UnsupportedOp 带槽名 ----
        set(w, "teStore", Class.forName("iz").getConstructor().newInstance());
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

        System.out.println("FakeOps120Test PASS (" + checks + " checks)");
    }
}
