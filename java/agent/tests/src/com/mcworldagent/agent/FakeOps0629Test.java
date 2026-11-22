// [B5-ARCHIVE 2026-11-22] headless test archived from %TEMP%\mcaut (source batch: A2 (0618/0629/1121)); run via java/agent/tests/run.ps1
package com.mcworldagent.agent;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

/**
 * 0629 接入批验收：假类=**默认包根类**（root0629\*.java，表值锚
 * Class.forName("iw"/"ht"/"gt"/"hx"…）——G3 扁平混淆形态的无头复刻，
 * 全部经反射驱动（默认包类不可被命名包 import）。
 * 覆盖：26 OK 面走通（含 saveWorld (Liw;)V 传 null 路径——el0629-2 活体翻案：
 * iw=具体类非接口，本体 null 守卫，Proxy 作废=教训 20）+
 * 9 件 unconfirmed UnsupportedOp 带槽名（getinfo 随 difficultyF 未钉归 unconfirmed）。
 */
public class FakeOps0629Test {

    static int checks = 0;

    static void ok(boolean c, String msg) {
        checks++;
        if (!c) {
            throw new AssertionError(msg);
        }
    }

    static Object newInstance(String cn) throws Exception {
        return Class.forName(cn).getConstructor().newInstance();
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
            if (!m.contains("slot " + slot) || !m.contains("inf-20100629")) {
                throw new AssertionError(op + " msg=" + m);
            }
        } catch (Exception e) {
            throw new AssertionError(op + " wrong: " + e);
        }
    }

    public static void main(String[] a) throws Exception {
        ok(VersionRegistry.activate("inf-20100629"), "activate 0629");
        HookTargets.activate("inf-20100629");

        Class<?> wc = Class.forName("ez");
        Object w = wc.getConstructor().newInstance();
        Object player = newInstance("gc");
        set(w, "f", player);
        WorldOps.resolve(wc);

        // ---- 写读面 ----
        ok(WorldOps.setBlock(w, 5, 64, 5, 1), "place");
        ok(WorldOps.getBlockId(w, 5, 64, 5) == 1, "getblock");
        WorldOps.setBlock(w, 5, 64, 5, 0);
        ok(WorldOps.getBlockId(w, 5, 64, 5) == 0, "dig");
        ok(WorldOps.getblocks(w, 0, 1, 0, 1, 2, 1).startsWith("getblocks 2x2x2 rle="), "getblocks");
        ok(WorldOps.getMeta(w, 5, 64, 5) == 0, "getmeta");

        // ---- tp：b(DDDFF)V 双写 g..i+j..l，pos 消费面=j/k/l（翻案②对榫） ----
        ok(WorldOps.tp(w, 10, 20, 30).startsWith("teleported"), "tp");
        ok(((Double) get(player, "j")).doubleValue() == 10.5
                && ((Double) get(player, "k")).doubleValue() == 20.0
                && ((Double) get(player, "l")).doubleValue() == 30.5, "tp pos j/k/l");
        ok(((Double) get(player, "g")).doubleValue() == 10.5, "tp lastTick g 同步双写");
        ok(((Double) get(player, "m")).doubleValue() == 0.0
                && ((Double) get(player, "n")).doubleValue() == 0.0
                && ((Double) get(player, "o")).doubleValue() == 0.0, "tp mot m/n/o zeroed");

        // ---- setlook/setmotion 面（p/q、m/n/o 直写；setmotion 协议=整数千分位） ----
        ok(WorldOps.setLook(w, 90, -10).startsWith("look yaw"), "setlook");
        ok(((Float) get(player, "p")).floatValue() == 90f
                && ((Float) get(player, "q")).floatValue() == -10f, "setlook p/q");
        WorldOps.setMotion(w, 500, 1000, -500);
        ok(((Double) get(player, "m")).doubleValue() == 0.5
                && ((Double) get(player, "n")).doubleValue() == 1.0
                && ((Double) get(player, "o")).doubleValue() == -0.5, "setmotion m/n/o");

        // ---- 时间/出生点 ----
        WorldOps.setTime(w, 1000L);
        ok(((Long) get(w, "b")).longValue() == 1000L, "settime b");
        WorldOps.setSpawn(w, 8, 64, 8);
        ok(((Integer) get(w, "i")) == 8 && ((Integer) get(w, "k")) == 8, "setspawn i/j/k");

        // ---- saveWorld：(Liw;)V 传 null（el0629-2 活体翻案复刻——iw=具体类，
        // 假类体内 null 守卫 if (prog != null)，Proxy 作废） ----
        WorldOps.saveWorld(w);
        ok((Integer) get(w, "saveCalls") == 1, "save via (Liw;)V null-pass");

        // ---- 物品栏面 ----
        Object inv = get(player, "b");
        Object[] mainArr = (Object[]) get(inv, "a");
        Constructor<?> isCtor = Class.forName("hr").getConstructor(int.class, int.class);
        mainArr[3] = isCtor.newInstance(5, 1);
        ok(WorldOps.inv(w).equals("3=1x5 cur=0"), "inv");
        // give：注册表校验（fz.b[5] 非 null）+ addStack
        Object[] reg = (Object[]) Class.forName("fz").getField("b").get(null);
        reg[5] = newInstance("fz");
        ok(WorldOps.give(w, 5, 2, 0).startsWith("gave"), "give");
        ok(WorldOps.clearInv(w).startsWith("cleared 36+4"), "clearinv");

        // ---- health 面（jv.ab 三重实锤消费） ----
        ok(WorldOps.getPlayer(w).contains("health=10"), "getplayer");
        ok("health 10 -> 15".equals(WorldOps.setHealth(w, 15)), "sethealth");
        ok((Integer) get(player, "ab") == 15, "sethealth 直写 ab");

        // ---- entitylist 双轨：ht 查表 + 类简名保底 + [h=] 条件段 ----
        Class<?> htCls = Class.forName("ht");
        @SuppressWarnings("unchecked")
        java.util.Map<Object, Object> names =
                (java.util.Map<Object, Object>) htCls.getField("names").get(null);
        names.put(Class.forName("hx"), "Pig");
        names.put(Class.forName("cy"), "PrimedTnt");
        Object pig = Class.forName("hx").getConstructor(wc).newInstance(w);
        set(pig, "j", 1.2); set(pig, "k", 64.0); set(pig, "l", 2.7);
        Object tnt = Class.forName("cy").getConstructor(wc, float.class, float.class, float.class)
                .newInstance(w, 0f, 0f, 0f);
        Object ghost = Class.forName("dk").getConstructor(wc).newInstance(w); // 未注册（ht.names 无 dk）→类简名保底
        Method addE = wc.getMethod("a", Class.forName("em"));
        addE.invoke(w, pig);
        addE.invoke(w, tnt);
        addE.invoke(w, ghost);
        String el = WorldOps.entityList(w);
        ok(el.contains("Pig@1.2,64.0,2.7[h=10]"), "entitylist 查表+health 段 " + el);
        ok(el.contains("PrimedTnt@0.0,0.0,0.0"), "entitylist tnt 无 h 段（cy 非 jv 系）");
        ok(el.contains("dk@"), "entitylist 类简名保底");

        // ---- spawn/drop/explode/playsound/clear（0629 比 0618 多钉的四面） ----
        String sp = WorldOps.spawn(w, "sheep", 1, 64, 1);
        ok(sp.startsWith("spawned sheep"), "spawn " + sp);
        List<?> pList = (List<?>) get(w, "p");
        ok(pList.size() == 4, "spawn 入列 p");
        ok(WorldOps.spawn(w, "mob", 1, 64, 1).startsWith("spawned mob"), "spawn mob=gl");
        ok(WorldOps.spawn(w, "giant", 1, 64, 1).startsWith("spawned giant"), "spawn giant=aa 构造健在");
        ok(WorldOps.drop(w, 5, 1, 0, 1, 64, 1).startsWith("dropped"), "drop gt");
        Object item = pList.get(pList.size() - 1);
        ok(item.getClass().getName().equals("gt"), "drop 实体=gt");
        // el0629-2 活体更正：gt 在 ht 注册面（"Item"）——静态扫描漏计第 12 件
        names.put(Class.forName("gt"), "Item");
        ok(WorldOps.entityList(w).contains("Item@"), "entitylist Item=gt 注册名");
        ok(WorldOps.explode(w, 1, 64, 1, 1).startsWith("exploded"), "explode");
        ok((Integer) get(w, "expl") == 1, "explode 调用面");
        ok(WorldOps.playSound(w, 1, 64, 1, "random.fizz", 5, 10).startsWith("played"), "playsound");
        ok(WorldOps.clearGroundItems(w, null, null, null, null).startsWith("cleared 1"), "clear gt 实体");
        ok((Boolean) get(item, "dead"), "setEntityDead static b(em)V");

        // ---- 10 件 unconfirmed → UnsupportedOp 带槽名 ----
        unsupported("getinfo", new RunnableEx() {
            public void run() throws Exception { WorldOps.getInfo(w); } }, "difficultyF");
        unsupported("invulnerable", new RunnableEx() {
            public void run() throws Exception { WorldOps.invulnerable(w, 100); } }, "iframesF");
        set(w, "teStore", newInstance("bf"));
        unsupported("getchest", new RunnableEx() {
            public void run() throws Exception { WorldOps.getInv(w, 1, 64, 1); } }, "invSizeM");
        unsupported("setchest", new RunnableEx() {
            public void run() throws Exception { WorldOps.setInv(w, 1, 64, 1, 0, 5, 1, 0, false); } }, "invSizeM");
        unsupported("getfurnace", new RunnableEx() {
            public void run() throws Exception { WorldOps.getFurnace(w, 1, 64, 1); } }, "teBurnF");
        unsupported("genscatter", new RunnableEx() {
            public void run() throws Exception { WorldOps.genScatter(w, 14, 1, 64, 1, null); } }, "scatterClassG");
        unsupported("getlight", new RunnableEx() {
            public void run() throws Exception { WorldOps.getLightValue(w, 1, 64, 1); } }, "getLightM");
        unsupported("setmeta", new RunnableEx() {
            public void run() throws Exception { WorldOps.setMeta(w, 1, 64, 1, 1); } }, "setMetaSilentM");
        unsupported("grow", new RunnableEx() {
            public void run() throws Exception { WorldOps.growCrops(w, 1, 64, 1); } }, "idCrops");

        System.out.println("FakeOps0629Test PASS (" + checks + " checks)");
    }
}
