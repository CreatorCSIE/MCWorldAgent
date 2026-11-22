// [B5-ARCHIVE 2026-11-22] headless test archived from %TEMP%\mcaut (source batch: A2 b1.2_02); run via java/agent/tests/run.ps1
package com.mcworldagent.agent;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

public class FakeOps1202Test {

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
            if (!m.contains("slot " + slot) || !m.contains("b1.2_02")) {
                throw new AssertionError(op + " msg=" + m);
            }
        } catch (Exception e) {
            throw new AssertionError(op + " wrong: " + e);
        }
    }

    @SuppressWarnings("unchecked")
    public static void main(String[] a) throws Exception {
        ok(VersionRegistry.activate("b1.2_02"), "activate 1202");
        HookTargets.activate("b1.2_02");

        // ---- 布景：dn 玩家面=字段 d=playerEntities；玩家 eu 构造 (dn)V ----
        Class<?> wc = Class.forName("dn");
        Object w = wc.getConstructor(java.io.File.class, String.class)
                .newInstance(new java.io.File("saves/test"), "world");
        Object player = Class.forName("eu").getConstructor(wc).newInstance(w);
        ((List<Object>) get(w, "d")).add(player);
        ok(((List<Object>) get(w, "d")).size() == 1, "playerEntities 首件=活体玩家");
        WorldOps.resolve(wc);

        // ---- 写读面（主写面断代=e(IIII)Z，裸 b 留档）----
        ok(WorldOps.setBlock(w, 5, 64, 5, 1), "place e(IIII)Z");
        ok(WorldOps.getBlockId(w, 5, 64, 5) == 1, "getblock a(III)I");
        WorldOps.setBlock(w, 5, 64, 5, 0);
        ok(WorldOps.getBlockId(w, 5, 64, 5) == 0, "dig");
        ok(WorldOps.getblocks(w, 0, 1, 0, 1, 2, 1).startsWith("getblocks 2x2x2 rle="), "getblocks");
        ok(WorldOps.getMeta(w, 5, 64, 5) == 0, "getmeta e(III)I");

        // ---- tp：d.get(0) 回退 + b(DDDFF)V 双写 aF..aH+aC..aE（pos 断代面）----
        ok(WorldOps.tp(w, 10, 20, 30).startsWith("teleported"), "tp via d.get(0)");
        ok(((Double) get(player, "aF")).doubleValue() == 10.5
                && ((Double) get(player, "aG")).doubleValue() == 20.0
                && ((Double) get(player, "aH")).doubleValue() == 30.5, "tp pos aF/aG/aH");
        ok(((Double) get(player, "aC")).doubleValue() == 10.5
                && ((Double) get(player, "aD")).doubleValue() == 20.0
                && ((Double) get(player, "aE")).doubleValue() == 30.5, "tp prevPos aC..aE 同步双写");
        ok(((Double) get(player, "aI")).doubleValue() == 0.0
                && ((Double) get(player, "aJ")).doubleValue() == 0.0
                && ((Double) get(player, "aK")).doubleValue() == 0.0, "tp mot aI/aJ/aK zeroed");

        // ---- setlook/setmotion（aL/aM、aI/aJ/aK 直写）----
        ok(WorldOps.setLook(w, 90, -10).startsWith("look yaw"), "setlook");
        ok(((Float) get(player, "aL")).floatValue() == 90f
                && ((Float) get(player, "aM")).floatValue() == -10f, "setlook aL/aM");
        WorldOps.setMotion(w, 500, 1000, -500);
        ok(((Double) get(player, "aI")).doubleValue() == 0.5
                && ((Double) get(player, "aJ")).doubleValue() == 1.0
                && ((Double) get(player, "aK")).doubleValue() == -0.5, "setmotion aI/aJ/aK");

        // ---- 时间/出生点（e / m/n/o 断代）----
        WorldOps.setTime(w, 1000L);
        ok(((Long) get(w, "e")).longValue() == 1000L, "settime e");
        WorldOps.setSpawn(w, 8, 64, 8);
        ok(((Integer) get(w, "m")) == 8 && ((Integer) get(w, "n")) == 64
                && ((Integer) get(w, "o")) == 8, "setspawn m/n/o");

        // ---- getinfo ----
        set(w, "k", 2);
        String info = WorldOps.getInfo(w);
        ok(info.equals("time=1000 spawnX=8 spawnY=64 spawnZ=8 seed=0 difficulty=2"),
                "getinfo 全串对账 " + info);

        // ---- getlight l(III)I ----
        ok(WorldOps.getLightValue(w, 5, 64, 5) == 15, "getlight l(III)I");

        // ---- saveWorld：a(Lru;)V 包装委托 a(ZLru)V 本体（null 守卫+调 r()V）----
        WorldOps.saveWorld(w);
        ok((Integer) get(w, "saveCalls") == 1, "save via a(Lru;)V wrapper->body");
        ok((Integer) get(w, "levelSaves") == 1, "body 调 saveLevel r()V");

        // ---- getbiome：dn.a()ro -> ro.a(II)hf -> hf.m 三锚链 ----
        String b1 = WorldOps.biomeAt(w, 100, 100);
        String b2 = WorldOps.biomeAt(w, -100, -100);
        String b3 = WorldOps.biomeAt(w, 0, 0);
        ok(b1 != null && b1.length() > 0, "getbiome 非空串 " + b1);
        ok("Desert".equals(b1), "getbiome (100,100)=Desert got " + b1);
        ok("Ice Desert".equals(b2), "getbiome (-100,-100)=Ice Desert got " + b2);
        ok("Plains".equals(b3), "getbiome (0,0)=Plains got " + b3);

        // ---- 物品栏面（fallback 双路径重点）----
        // gl a=gm[36]/b=gm[4]/c=cur；gm 仅 (III)V、d private——
        // inv 快照先触发 resolveInvFace：isDmg getDeclaredField fallback + isCtor2=null 容忍。
        Object inv = get(player, "f");
        Object[] mainArr = (Object[]) get(inv, "a");
        ok(mainArr.length == 36, "invMain gm[36]");
        Constructor<?> isCtor3 = Class.forName("gm").getConstructor(int.class, int.class, int.class);
        mainArr[3] = isCtor3.newInstance(5, 1, 0);
        ok(WorldOps.inv(w).equals("3=1x5 cur=0"), "inv via d.get(0)");
        // give dmg=0：newStack 走 (III)V（isCtor2=null 断代配方，dmg=0 等价）
        Object[] reg = (Object[]) Class.forName("eo").getField("c").get(null);
        Constructor<?> eoCtor = Class.forName("eo").getDeclaredConstructor(int.class);
        eoCtor.setAccessible(true);
        reg[5] = eoCtor.newInstance(5);
        ok(WorldOps.give(w, 5, 2, 0).startsWith("gave"), "give via newStack (III)V");
        // give dmg=3：isDmg 私有面写读全经 setAccessible Field（inv 快照显 " d3"）
        ok("gave 5 x1 dmg=3".equals(WorldOps.give(w, 5, 1, 3)), "give dmg=3");
        ok(WorldOps.inv(w).contains(" d3"), "inv reads private d via setAccessible");
        ok(WorldOps.clearInv(w).startsWith("cleared 36+4"), "clearinv 36+4");

        // ---- health 面（ig.S 消费）；iframesF=null 档位一 ----
        ok(WorldOps.getPlayer(w).contains("health=10"), "getplayer");
        ok("health 10 -> 15".equals(WorldOps.setHealth(w, 15)), "sethealth");
        ok((Integer) get(player, "S") == 15, "sethealth 直写 S");

        // ---- entitylist 双轨：gn.b(nl) 查表 + 类简名保底 + [h=] 条件段（mb 非 Living）----
        Class<?> gnCls = Class.forName("gn");
        java.util.Map<Object, Object> names =
                (java.util.Map<Object, Object>) gnCls.getField("names").get(null);
        names.put(Class.forName("qk"), "Pig");
        names.put(Class.forName("mb"), "PrimedTnt");
        Object pig = Class.forName("qk").getConstructor(wc).newInstance(w);
        set(pig, "aF", 1.2); set(pig, "aG", 64.0); set(pig, "aH", 2.7);
        Object tnt = Class.forName("mb").getConstructor(wc, double.class, double.class, double.class)
                .newInstance(w, 0.0, 0.0, 0.0);
        Object ghost = Class.forName("fz").getConstructor(wc).newInstance(w); // 未注册→类简名保底
        Method addE = wc.getMethod("a", Class.forName("nl")); // spawnEntity a(nl)Z
        addE.invoke(w, pig);
        addE.invoke(w, tnt);
        addE.invoke(w, ghost);
        String el = WorldOps.entityList(w);
        ok(el.contains("Pig@1.2,64.0,2.7[h=10]"), "entitylist 查表+health 段 " + el);
        ok(el.contains("PrimedTnt@0.0,0.0,0.0"), "entitylist tnt 无 h 段（mb 非 ig 系）");
        ok(el.contains("fz@"), "entitylist 类简名保底");

        // ---- spawn/drop/explode/playsound/clear ----
        String sp = WorldOps.spawn(w, "sheep", 1, 64, 1);
        ok(sp.startsWith("spawned sheep"), "spawn " + sp);
        List<?> pList = (List<?>) get(w, "b");
        ok(pList.size() == 4, "spawn 入列 loadedEntityList b");
        ok(WorldOps.spawn(w, "mob", 1, 64, 1).startsWith("spawned mob"), "spawn mob=fz EntityCreature");
        ok(WorldOps.spawn(w, "giant", 1, 64, 1).startsWith("spawned giant"), "spawn giant=jv 构造健在");
        // tnt 重点：FFF miss→DDD fallback（b1.2_02 断代=本批唯一构造器断代）
        ok(WorldOps.spawn(w, "tnt", 1, 64, 1).startsWith("spawned tnt"), "spawn tnt=mb DDD fallback");
        ok(WorldOps.drop(w, 5, 1, 0, 1, 64, 1).startsWith("dropped"), "drop fh");
        Object item = pList.get(pList.size() - 1);
        ok(item.getClass().getName().equals("fh"), "drop 实体=fh");
        names.put(Class.forName("fh"), "Item");
        ok(WorldOps.entityList(w).contains("Item@"), "entitylist Item=fh 注册名");
        ok(WorldOps.explode(w, 1, 64, 1, 1).startsWith("exploded"), "explode");
        ok((Integer) get(w, "expl") == 1, "explode 调用面 a(Lnl;DDDF)Lmc;");
        ok(WorldOps.playSound(w, 1, 64, 1, "random.fizz", 5, 10).startsWith("played"), "playsound");
        ok((Integer) get(w, "snd") == 1, "playsound 调用面 a(DDD,String,FF)V");
        // clear 核心：setEntityDead=实例 d(Lnl)V——置 aW=dead
        ok(WorldOps.clearGroundItems(w, null, null, null, null).startsWith("cleared 1"),
                "clear fh 实体（loaded 仅 fh 命中）");
        ok((Boolean) get(item, "aW"), "setEntityDead 实例 d(Lnl)V 置 aW=dead");
        ok((Integer) get(w, "killCalls") == 1, "clear 收 world 调用（实例版对账）");

        // ---- 7 件 unconfirmed → UnsupportedOp 带槽名（invulnerable=本批降档）----
        set(w, "teStore", Class.forName("kp").getConstructor().newInstance());
        unsupported("invulnerable", new RunnableEx() {
            public void run() throws Exception { WorldOps.invulnerable(w, 100); } }, "iframesF");
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

        System.out.println("FakeOps1202Test PASS (" + checks + " checks)");
    }
}
