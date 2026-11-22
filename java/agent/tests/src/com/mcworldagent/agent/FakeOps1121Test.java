// [B5-ARCHIVE 2026-11-22] headless test archived from %TEMP%\mcaut (source batch: A2 (0618/0629/1121)); run via java/agent/tests/run.ps1
package com.mcworldagent.agent;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

/**
 * a1.1.2_01 接入批验收：假类=**默认包根类**（root1121\*.java，表值锚
 * Class.forName("cn"/"kh"/"ge"/"dm"/"ew"/"mv"…——G3-alpha 扁平混淆形态的无头复刻，
 * 全部经反射驱动；主类 net.minecraft.client.Minecraft 为命名包假类）。
 * 核心覆盖=**档位三玩家回退**：假 World cn 无玩家字段、只有 public List k（playerEntities），
 * WorldOps.worldPlayer 经 k.get(0) 取活体玩家（tp/getplayer/give/inv/clearinv 等全走此路）；
 * 档位二差异面逐个真跑：saveWorld a(Lnu)V 包装（el1121-1 翻案）、spawnEntity a(kh)Z、
 * setEntityDead **实例** d(kh)V（clear 按 Modifier.isStatic 收 world 调用，killCalls 对账）、
 * getinfo/getlight/invulnerable 三件 0618/0629 欠账本版转 OK；
 * 6 件 unconfirmed UnsupportedOp 带槽名（setmeta 连坐=observersF 非 setMetaSilentM）。
 */
public class FakeOps1121Test {

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
            if (!m.contains("slot " + slot) || !m.contains("a1.1.2_01")) {
                throw new AssertionError(op + " msg=" + m);
            }
        } catch (Exception e) {
            throw new AssertionError(op + " wrong: " + e);
        }
    }

    @SuppressWarnings("unchecked")
    public static void main(String[] a) throws Exception {
        ok(VersionRegistry.activate("a1.1.2_01"), "activate 1121");
        HookTargets.activate("a1.1.2_01");

        // ---- 档位三布景：cn 无玩家字段，玩家只入 k=playerEntities 列表 ----
        Class<?> wc = Class.forName("cn");
        Object w = wc.getConstructor(java.io.File.class, String.class)
                .newInstance(new java.io.File("saves/test"), "world");
        Object player = newInstance("dm");
        ((List<Object>) get(w, "k")).add(player);
        ok(((List<Object>) get(w, "k")).size() == 1, "playerEntities 首件=活体玩家");
        WorldOps.resolve(wc);

        // ---- 写读面 ----
        ok(WorldOps.setBlock(w, 5, 64, 5, 1), "place d(IIII)Z");
        ok(WorldOps.getBlockId(w, 5, 64, 5) == 1, "getblock a(III)I");
        WorldOps.setBlock(w, 5, 64, 5, 0);
        ok(WorldOps.getBlockId(w, 5, 64, 5) == 0, "dig");
        ok(WorldOps.getblocks(w, 0, 1, 0, 1, 2, 1).startsWith("getblocks 2x2x2 rle="), "getblocks");
        ok(WorldOps.getMeta(w, 5, 64, 5) == 0, "getmeta e(III)I");

        // ---- tp：经 worldPlayer 回退 + b(DDDFF)V 双写 ak..am+ah..aj（pos 消费面=ak/al/am） ----
        ok(WorldOps.tp(w, 10, 20, 30).startsWith("teleported"), "tp via k.get(0)");
        ok(((Double) get(player, "ak")).doubleValue() == 10.5
                && ((Double) get(player, "al")).doubleValue() == 20.0
                && ((Double) get(player, "am")).doubleValue() == 30.5, "tp pos ak/al/am");
        ok(((Double) get(player, "ah")).doubleValue() == 10.5
                && ((Double) get(player, "ai")).doubleValue() == 20.0
                && ((Double) get(player, "aj")).doubleValue() == 30.5, "tp prevPos ah..aj 同步双写");
        ok(((Double) get(player, "an")).doubleValue() == 0.0
                && ((Double) get(player, "ao")).doubleValue() == 0.0
                && ((Double) get(player, "ap")).doubleValue() == 0.0, "tp mot an/ao/ap zeroed");

        // ---- setlook/setmotion 面（aq/ar、an/ao/ap 直写；setmotion 协议=整数千分位） ----
        ok(WorldOps.setLook(w, 90, -10).startsWith("look yaw"), "setlook");
        ok(((Float) get(player, "aq")).floatValue() == 90f
                && ((Float) get(player, "ar")).floatValue() == -10f, "setlook aq/ar");
        WorldOps.setMotion(w, 500, 1000, -500);
        ok(((Double) get(player, "an")).doubleValue() == 0.5
                && ((Double) get(player, "ao")).doubleValue() == 1.0
                && ((Double) get(player, "ap")).doubleValue() == -0.5, "setmotion an/ao/ap");

        // ---- 时间/出生点 ----
        WorldOps.setTime(w, 1000L);
        ok(((Long) get(w, "c")).longValue() == 1000L, "settime c");
        WorldOps.setSpawn(w, 8, 64, 8);
        ok(((Integer) get(w, "o")) == 8 && ((Integer) get(w, "p")) == 64
                && ((Integer) get(w, "q")) == 8, "setspawn o/p/q");

        // ---- getinfo：difficultyF=l 已钉（0618/0629 欠账本版转 OK 正面） ----
        set(w, "l", 2);
        String info = WorldOps.getInfo(w);
        ok(info.equals("time=1000 spawnX=8 spawnY=64 spawnZ=8 seed=0 difficulty=2"),
                "getinfo 全串对账 " + info);

        // ---- getlight：getLightM=j 已钉（转 OK 正面） ----
        ok(WorldOps.getLightValue(w, 5, 64, 5) == 15, "getlight j(III)I");

        // ---- saveWorld：(Lnu;)V 包装（el1121-1 翻案后 0629 同形：desc 解析参数类 nu+null 直调） ----
        WorldOps.saveWorld(w);
        ok((Integer) get(w, "saveCalls") == 1, "save via a(Lnu;)V wrapper->body");

        // ---- 物品栏面（eu a=ev[37]/b=ev[4]/d=cur；ItemStack ev a/c/d 全同形） ----
        Object inv = get(player, "b");
        Object[] mainArr = (Object[]) get(inv, "a");
        ok(mainArr.length == 37, "invMain ev[37]（37/36 漂移面 give 自适应）");
        Constructor<?> isCtor = Class.forName("ev").getConstructor(int.class, int.class);
        mainArr[3] = isCtor.newInstance(5, 1);
        ok(WorldOps.inv(w).equals("3=1x5 cur=0"), "inv via k.get(0)");
        // give：注册表校验（di.c[5] 非 null）+ addStack a(ev)Z
        Object[] reg = (Object[]) Class.forName("di").getField("c").get(null);
        reg[5] = newInstance("di");
        ok(WorldOps.give(w, 5, 2, 0).startsWith("gave"), "give");
        ok(WorldOps.clearInv(w).startsWith("cleared 37+4"), "clearinv 37+4");

        // ---- health 面（ge.E 消费）+ iframes（kh.aW 转 OK） ----
        ok(WorldOps.getPlayer(w).contains("health=10"), "getplayer");
        ok("health 10 -> 15".equals(WorldOps.setHealth(w, 15)), "sethealth");
        ok((Integer) get(player, "E") == 15, "sethealth 直写 E");
        ok(WorldOps.invulnerable(w, 100).startsWith("iframes"), "invulnerable aW 转 OK");
        ok((Integer) get(player, "aW") == 100, "invulnerable 直写 aW");

        // ---- entitylist 双轨：ew.b(kh) 查表 + 类简名保底 + [h=] 条件段（jd 非 Living） ----
        Class<?> ewCls = Class.forName("ew");
        java.util.Map<Object, Object> names =
                (java.util.Map<Object, Object>) ewCls.getField("names").get(null);
        names.put(Class.forName("mv"), "Pig");
        names.put(Class.forName("jd"), "PrimedTnt");
        Object pig = Class.forName("mv").getConstructor(wc).newInstance(w);
        set(pig, "ak", 1.2); set(pig, "al", 64.0); set(pig, "am", 2.7);
        Object tnt = Class.forName("jd").getConstructor(wc, float.class, float.class, float.class)
                .newInstance(w, 0f, 0f, 0f);
        Object ghost = Class.forName("ek").getConstructor(wc).newInstance(w); // 未注册→类简名保底
        Method addE = wc.getMethod("a", Class.forName("kh")); // spawnEntity a(kh)Z 档位二
        addE.invoke(w, pig);
        addE.invoke(w, tnt);
        addE.invoke(w, ghost);
        String el = WorldOps.entityList(w);
        ok(el.contains("Pig@1.2,64.0,2.7[h=10]"), "entitylist 查表+health 段 " + el);
        ok(el.contains("PrimedTnt@0.0,0.0,0.0"), "entitylist tnt 无 h 段（jd 非 ge 系）");
        ok(el.contains("ek@"), "entitylist 类简名保底");

        // ---- spawn/drop/explode/playsound/clear ----
        String sp = WorldOps.spawn(w, "sheep", 1, 64, 1);
        ok(sp.startsWith("spawned sheep"), "spawn " + sp);
        List<?> pList = (List<?>) get(w, "a");
        ok(pList.size() == 4, "spawn 入列 loadedEntityList a");
        ok(WorldOps.spawn(w, "mob", 1, 64, 1).startsWith("spawned mob"), "spawn mob=ek EntityCreature");
        ok(WorldOps.spawn(w, "giant", 1, 64, 1).startsWith("spawned giant"), "spawn giant=hl 构造健在");
        ok(WorldOps.spawn(w, "tnt", 1, 64, 1).startsWith("spawned tnt"), "spawn tnt=jd(cn,FFF)V");
        ok(WorldOps.drop(w, 5, 1, 0, 1, 64, 1).startsWith("dropped"), "drop dx");
        Object item = pList.get(pList.size() - 1);
        ok(item.getClass().getName().equals("dx"), "drop 实体=dx");
        names.put(Class.forName("dx"), "Item");
        ok(WorldOps.entityList(w).contains("Item@"), "entitylist Item=dx 注册名");
        ok(WorldOps.explode(w, 1, 64, 1, 1).startsWith("exploded"), "explode");
        ok((Integer) get(w, "expl") == 1, "explode 调用面");
        ok(WorldOps.playSound(w, 1, 64, 1, "random.fizz", 5, 10).startsWith("played"), "playsound");
        ok((Integer) get(w, "snd") == 1, "playsound 调用面");
        // clear 档位二核心：setEntityDead=**实例** d(kh)V——Modifier.isStatic=false 收 world 调用；
        // 若误按 static 传 null 必 IllegalArgumentException，killCalls 对账收方正确
        ok(WorldOps.clearGroundItems(w, null, null, null, null).startsWith("cleared 1"),
                "clear dx 实体（loaded 含 pig/tnt/ghost/sheep/mob/giant/jd 仅 dx 命中）");
        ok((Boolean) get(item, "dead"), "setEntityDead 实例 d(kh)V 置 dead");
        ok((Integer) get(w, "killCalls") == 1, "clear 收 world 调用（实例版对账）");

        // ---- 6 件 unconfirmed → UnsupportedOp 带槽名 ----
        set(w, "teStore", new Object());
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

        System.out.println("FakeOps1121Test PASS (" + checks + " checks)");
    }
}
