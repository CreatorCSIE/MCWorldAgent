// [B5-ARCHIVE 2026-11-22] headless test archived from %TEMP%\mcaut (source batch: B b1.8.1); run via java/agent/tests/run.ps1
package com.mcworldagent.agent;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

/**
 * b1.8.1 假件 ops 全链实跑（WorldOps 真代码 × root181 假件实形）。
 * 漂移槽全翻译（详表 §2 vs b1.7.3）：World=rv/Entity=kj/EntityLiving=wd/
 * EntityPlayer=sz(as=inventory)/InventoryPlayer=ui/ItemStack=ul/Item=sv(f 注册表)/
 * EntityList=um/WorldInfo=rl/IProgressUpdate=ny/TileEntity=ij/EntityItem=ee/
 * EntityCreature=tz/pig=mx/sheep=bx/tnt=ze/giant=hv；playerEntities d→i、
 * loadedEntityList b→g、difficulty q→v、pos aM-aO→o/p/q、mot→r/s/t、look→u/v、
 * health Y→bz、iframes by→aa、entityJoinedWorld b→a（回漂）、setEntityDead e→b（回漂）。
 */
public class FakeOps181Test {

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

    // rl 私有面直读（WorldInfo 分支对账通道=与 WorldOps 同配方 getDeclaredField+setAccessible）
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
            if (!m.contains("slot " + slot) || !m.contains("b1.8.1")) {
                throw new AssertionError(op + " msg=" + m);
            }
        } catch (Exception e) {
            throw new AssertionError(op + " wrong: " + e);
        }
    }

    @SuppressWarnings("unchecked")
    public static void main(String[] a) throws Exception {
        ok(VersionRegistry.activate("b1.8.1"), "activate 181");
        HookTargets.activate("b1.8.1");

        // ---- 布景：rv 玩家面=字段 i=playerEntities；玩家 sz 构造 (rv)V ----
        Class<?> wc = Class.forName("rv");
        Object w = wc.getConstructor().newInstance();
        Object player = Class.forName("sz").getConstructor(wc).newInstance(w);
        ((List<Object>) getv(w, "i")).add(player);
        ok(((List<Object>) getv(w, "i")).size() == 1, "playerEntities i 首件=活体玩家");
        WorldOps.resolve(wc);

        // ---- 写读面（主写面断代=g(IIII)Z，裸 d/e 同形陷阱群留档假件）----
        ok(WorldOps.setBlock(w, 5, 64, 5, 1), "place g(IIII)Z");
        ok(WorldOps.getBlockId(w, 5, 64, 5) == 1, "getblock a(III)I");
        WorldOps.setBlock(w, 5, 64, 5, 0);
        ok(WorldOps.getBlockId(w, 5, 64, 5) == 0, "dig");
        ok(WorldOps.getblocks(w, 0, 1, 0, 1, 2, 1).startsWith("getblocks 2x2x2 rle="), "getblocks");
        ok(WorldOps.getMeta(w, 5, 64, 5) == 0, "getmeta e(III)I");

        // ---- tp：i.get(0) 回退 + b(DDDFF)V 双写 o/p/q（pos 断代面 aM-aO→o/p/q）----
        ok(WorldOps.tp(w, 10, 20, 30).startsWith("teleported"), "tp via i.get(0)");
        ok(((Double) getv(player, "o")).doubleValue() == 10.5
                && ((Double) getv(player, "p")).doubleValue() == 20.0
                && ((Double) getv(player, "q")).doubleValue() == 30.5, "tp pos o/p/q");
        ok(((Double) getv(player, "r")).doubleValue() == 0.0
                && ((Double) getv(player, "s")).doubleValue() == 0.0
                && ((Double) getv(player, "t")).doubleValue() == 0.0, "tp mot r/s/t zeroed");

        // ---- setlook/setmotion（u/v、r/s/t 直写）----
        ok(WorldOps.setLook(w, 90, -10).startsWith("look yaw"), "setlook");
        ok(((Float) getv(player, "u")).floatValue() == 90f
                && ((Float) getv(player, "v")).floatValue() == -10f, "setlook u/v");
        WorldOps.setMotion(w, 500, 1000, -500);
        ok(((Double) getv(player, "r")).doubleValue() == 0.5
                && ((Double) getv(player, "s")).doubleValue() == 1.0
                && ((Double) getv(player, "t")).doubleValue() == -0.5, "setmotion r/s/t");

        // ---- WorldInfo 链重点：settime/setspawn 经 rv.x()Lrl 五 private 槽读写 ----
        WorldOps.setTime(w, 1000L);
        Object info = wc.getMethod("x").invoke(w);
        ok(info.getClass().getName().equals("rl"), "rv.x() 返回 rl（=ei 复刻）");
        ok(((Long) pri(info, "e")).longValue() == 1000L, "settime 写 rl.e private 经 setAccessible");
        WorldOps.setSpawn(w, 8, 64, 8);
        ok(((Integer) pri(info, "b")) == 8 && ((Integer) pri(info, "c")) == 64
                && ((Integer) pri(info, "d")) == 8, "setspawn 写 rl.b/c/d");
        ok(pri(info, "a") instanceof Long && ((Long) pri(info, "a")).longValue() == 0L,
                "seed=rl.a 非 rv 面");

        // ---- getinfo（同走 timeHolder=WorldInfo 对象；difficulty v 断代）----
        set(w, "v", 2);
        String got = WorldOps.getInfo(w);
        ok(got.equals("time=1000 spawnX=8 spawnY=64 spawnZ=8 seed=0 difficulty=2"),
                "getinfo 全串对账 " + got);

        // ---- getlight n(III)I ----
        ok(WorldOps.getLightValue(w, 5, 64, 5) == 15, "getlight n(III)I");

        // ---- saveWorld：a(Lny;)V 传 null（IProgressUpdate yb→ny 断代，null 守卫五连用）----
        WorldOps.saveWorld(w);
        ok((Integer) getv(w, "saveCalls") == 1, "save via a(Lny;)V wrapper->body");
        ok((Integer) getv(w, "levelSaves") == 1, "body 调 saveLevel r()V");

        // ---- getbiome：rv.a()Lnu -> nu.a(II)Lvh -> vh.l 三锚链（全断代）----
        String b1 = WorldOps.biomeAt(w, 100, 100);
        String b2 = WorldOps.biomeAt(w, -100, -100);
        String b3 = WorldOps.biomeAt(w, 0, 0);
        ok(b1 != null && b1.length() > 0, "getbiome 非空串 " + b1);
        ok("Desert".equals(b1), "getbiome (100,100)=Desert got " + b1);
        ok("Ice Desert".equals(b2), "getbiome (-100,-100)=Ice Desert got " + b2);
        ok("Plains".equals(b3), "getbiome (0,0)=Plains got " + b3);

        // ---- 物品栏面（sz.as→ui；ul.d public 本版无 private 断代）----
        Object inv = getv(player, "as");
        Object[] mainArr = (Object[]) getv(inv, "a");
        ok(mainArr.length == 36, "invMain ul[36]");
        Constructor<?> isCtor3 = Class.forName("ul").getConstructor(int.class, int.class, int.class);
        mainArr[3] = isCtor3.newInstance(5, 1, 0);
        ok(WorldOps.inv(w).equals("3=1x5 cur=0"), "inv via i.get(0)");
        // give dmg=0：ul 无 (II)V——newStack 走 (III)V 等价配方
        Object[] reg = (Object[]) Class.forName("sv").getField("f").get(null);
        Constructor<?> svCtor = Class.forName("sv").getDeclaredConstructor(int.class);
        svCtor.setAccessible(true);
        reg[5] = svCtor.newInstance(5);
        ok(WorldOps.give(w, 5, 2, 0).startsWith("gave"), "give via newStack (III)V");
        ok("gave 5 x1 dmg=3".equals(WorldOps.give(w, 5, 1, 3)), "give dmg=3");
        ok(WorldOps.inv(w).contains(" d3"), "inv reads ul.d");
        ok(WorldOps.clearInv(w).startsWith("cleared 36+4"), "clearinv 36+4");

        // ---- health 面（wd.bz 消费，Y→bz 断代）----
        ok(WorldOps.getPlayer(w).contains("health=10"), "getplayer");
        ok("health 10 -> 15".equals(WorldOps.setHealth(w, 15)), "sethealth");
        ok((Integer) getv(player, "bz") == 15, "sethealth 直写 bz");

        // ---- invulnerable（iframesF=aa——kj.aa 直写，by→aa 断代）----
        ok("iframes 0 -> 100".equals(WorldOps.invulnerable(w, 100)), "invulnerable via kj.aa");
        ok((Integer) getv(player, "aa") == 100, "invulnerable 直写 aa");

        // ---- entitylist 双轨：um.b(kj) 查表 + 类简名保底 + [h=] 条件段（ze 非 wd 系）----
        Class<?> umCls = Class.forName("um");
        java.util.Map<Object, Object> names =
                (java.util.Map<Object, Object>) umCls.getField("names").get(null);
        names.put(Class.forName("mx"), "Pig");
        names.put(Class.forName("ze"), "PrimedTnt");
        Object pig = Class.forName("mx").getConstructor(wc).newInstance(w);
        set(pig, "o", 1.2); set(pig, "p", 64.0); set(pig, "q", 2.7);
        Object tnt = Class.forName("ze").getConstructor(wc, double.class, double.class, double.class)
                .newInstance(w, 0.0, 0.0, 0.0);
        // ghost=sn（Creeper 具体件；tz=abstract 不可实例化——el181-1 断代判决）未注册→类简名保底
        Object ghost = Class.forName("sn").getConstructor(wc).newInstance(w);
        Method addE = wc.getMethod("a", Class.forName("kj")); // entityJoinedWorld a(Lkj;)Z 回漂
        addE.invoke(w, pig);
        addE.invoke(w, tnt);
        addE.invoke(w, ghost);
        String el = WorldOps.entityList(w);
        ok(el.contains("Pig@1.2,64.0,2.7[h=10]"), "entitylist 查表+health 段 " + el);
        ok(el.contains("PrimedTnt@0.0,0.0,0.0") && !el.contains("PrimedTnt@0.0,0.0,0.0[h="),
                "entitylist tnt 无 h 段（ze 非 wd 系）");
        ok(el.contains("sn@"), "entitylist 类简名保底 sn");

        // ---- spawn/drop/explode/playsound/clear ----
        String sp = WorldOps.spawn(w, "sheep", 1, 64, 1);
        ok(sp.startsWith("spawned sheep"), "spawn " + sp);
        List<?> pList = (List<?>) getv(w, "g");
        ok(pList.size() == 4, "spawn 入列 loadedEntityList g（p()List 同表）");
        // mob 槽已摘（el181-1 判决：真 tz=EntityCreature abstract，b1.8+ 无泛用可实例化 mob 基类）
        try {
            WorldOps.spawn(w, "mob", 1, 64, 1);
            ok(false, "spawn mob must reject (slot removed)");
        } catch (WorldOps.Rejected r) {
            ok(String.valueOf(r.getMessage()).contains("bad-kind: mob"), "spawn mob=REJECTED bad-kind");
        }
        ok(WorldOps.spawn(w, "giant", 1, 64, 1).startsWith("spawned giant"), "spawn giant=hv 构造健在");
        // tnt 重点：FFF miss→DDD fallback（断代延续）
        ok(WorldOps.spawn(w, "tnt", 1, 64, 1).startsWith("spawned tnt"), "spawn tnt=ze DDD fallback");
        ok(WorldOps.drop(w, 5, 1, 0, 1, 64, 1).startsWith("dropped"), "drop ee");
        Object item = pList.get(pList.size() - 1);
        ok(item.getClass().getName().equals("ee"), "drop 实体=ee");
        names.put(Class.forName("ee"), "Item");
        ok(WorldOps.entityList(w).contains("Item@"), "entitylist Item=ee 注册名");
        // explode：返回 zf 对象但 invoke 面不消费返回值（V→zf 断代留档）
        ok(WorldOps.explode(w, 1, 64, 1, 1).startsWith("exploded"), "explode");
        ok((Integer) getv(w, "expl") == 1, "explode 调用面 a(Lkj;DDDF)Lzf");
        ok(WorldOps.playSound(w, 1, 64, 1, "random.fizz", 5, 10).startsWith("played"), "playsound");
        ok((Integer) getv(w, "snd") == 1, "playsound 调用面 a(DDD,String,FF)V");
        // clear 核心：setEntityDead=实例 b(Lkj)V 回漂——置 G=dead
        ok(WorldOps.clearGroundItems(w, null, null, null, null).startsWith("cleared 1"),
                "clear ee 实体（loaded 仅 ee 命中）");
        ok((Boolean) getv(item, "G"), "setEntityDead 实例 b(Lkj)V 置 G=dead");
        ok((Integer) getv(w, "killCalls") == 1, "clear 收 world 调用（实例版对账）");

        // ---- D10 setxp（capXp=TRUE；sz.aX/aY/aZ/aG/ay 直写，字段全 public int）----
        ok(WorldOps.setXp(w, 10, 5).equals("xp level 0 -> 10 xp 0 -> 5"), "setxp 旧值回显");
        ok((Integer) getv(player, "aY") == 10, "setxp 写 aY=XpLevel");
        ok((Integer) getv(player, "aX") == 5, "setxp 写 aX=Xp");
        ok((Integer) getv(player, "aZ") == 105, "setxp 写 aZ=XpTotal=level*10+xp");
        ok((Integer) getv(player, "aG") == 0, "setxp 清 aG=XpCooldown");
        try {
            WorldOps.setXp(w, -1, 0);
            ok(false, "setxp level<0 must reject");
        } catch (WorldOps.Rejected r) {
            ok(String.valueOf(r.getMessage()).contains("level-out-of-range"), "setxp level REJECTED");
        }
        try {
            WorldOps.setXp(w, 0, 200000);
            ok(false, "setxp xp>100000 must reject");
        } catch (WorldOps.Rejected r) {
            ok(String.valueOf(r.getMessage()).contains("xp-out-of-range"), "setxp xp REJECTED");
        }

        // ---- D10 giveeffect/removeeffect（capEffect=TRUE；wd.a(Lade;)V/f(I)V + ade (III)V）----
        ok(WorldOps.giveEffect(w, 1, 200, 0).startsWith("giveeffect id=1"), "giveeffect 回显");
        ok(((java.util.Map) getv(player, "bR")).size() == 1, "giveeffect 入 bR（键=effectId）");
        Object eff = ((java.util.Map) getv(player, "bR")).get(Integer.valueOf(1));
        ok(eff != null && eff.getClass().getName().equals("ade"), "bR 值=ade 件");
        ok((Integer) getv(eff, "b") == 200, "ade.duration=200 对账");
        WorldOps.giveEffect(w, 4, 100, 2);
        ok(((java.util.Map) getv(player, "bR")).size() == 2, "giveeffect 第二效果");
        ok(WorldOps.removeEffect(w, 1).startsWith("removeeffect id=1"), "removeeffect 回显");
        ok(!((java.util.Map) getv(player, "bR")).containsKey(Integer.valueOf(1)),
                "removeeffect 移 id=1（f(I)V bR.remove）");
        ok(((java.util.Map) getv(player, "bR")).containsKey(Integer.valueOf(4)), "id=4 保留");
        try {
            WorldOps.giveEffect(w, 0, 100, 0);
            ok(false, "giveeffect id=0 must reject");
        } catch (WorldOps.Rejected r) {
            ok(String.valueOf(r.getMessage()).contains("effect-id-out-of-range"), "giveeffect id REJECTED");
        }
        try {
            WorldOps.removeEffect(w, 99);
            ok(false, "removeeffect id=99 must reject");
        } catch (WorldOps.Rejected r) {
            ok(String.valueOf(r.getMessage()).contains("effect-id-out-of-range"), "removeeffect id REJECTED");
        }

        // ---- D11 sethunger（capHunger=TRUE；sz.at()Lkc -> kc.a/b private 直写）----
        // 假件 kc 初值 a=20/b=5.0（FoodStats.java L4-5 实值）
        ok(WorldOps.setHunger(w, 10, 3).equals("hunger food 20 -> 10 sat 5.0 -> 3"), "sethunger 旧值回显");
        Object fs = Class.forName("sz").getMethod("at").invoke(player);
        ok((Integer) pri(fs, "a") == 10, "sethunger 写 kc.a=foodLevel private");
        ok((Float) pri(fs, "b") == 3.0f, "sethunger 写 kc.b=saturation private");
        // saturation 缺省（-1）=不动
        ok(WorldOps.setHunger(w, 5, -1).equals("hunger food 10 -> 5"), "sethunger sat 缺省不动");
        ok((Float) pri(fs, "b") == 3.0f, "sethunger sat 保留");
        try {
            WorldOps.setHunger(w, 21, 0);
            ok(false, "sethunger food>20 must reject");
        } catch (WorldOps.Rejected r) {
            ok(String.valueOf(r.getMessage()).contains("food-out-of-range"), "sethunger food REJECTED");
        }

        // ---- D12 weather 缺口闭合（capWeather=TRUE；rv.o/q protected 浮值面 getDeclaredField）----
        ok(WorldOps.weather(w, true, true).startsWith("weather raining=true thunder=true"),
                "weather 回显");
        ok((Float) getv(w, "o") == 1.0f, "weather 写 rv.o=rainingStrength 1.0");
        ok((Float) getv(w, "q") == 1.0f, "weather 写 rv.q=thunderingStrength 1.0");
        WorldOps.weather(w, false, true);
        ok((Float) getv(w, "o") == 0.0f, "weather raining=false 写 0.0");
        ok((Float) getv(w, "q") == 1.0f, "weather thunder 保留 1.0");

        // ---- D13 setgamemode（capGamemode=TRUE；WorldInfo rl.p private int 直写）----
        Object winfo = pri(w, "info");        // rv.x()Lrl 承载体
        ok(WorldOps.setGameMode(w, 1).startsWith("gamemode 0 -> 1"), "setgamemode 旧值回显");
        ok((Integer) pri(winfo, "p") == 1, "setgamemode 写 rl.p=GameType private=1(creative)");
        ok(WorldOps.setGameMode(w, 0).startsWith("gamemode 1 -> 0"), "setgamemode 回 survival");
        ok((Integer) pri(winfo, "p") == 0, "setgamemode 写回 0");
        try {
            WorldOps.setGameMode(w, 2);
            ok(false, "setgamemode mode=2 must reject (b1.8 no adventure)");
        } catch (WorldOps.Rejected r) {
            ok(String.valueOf(r.getMessage()).contains("gamemode-out-of-range"), "setgamemode mode REJECTED");
        }

        // ---- D14 getxp/gethunger 读面（复用已写字段回读，只读不写）----
        // 此前 setxp(10,5) 成功、sethunger(5,-1) 末态 food=5
        String rx = WorldOps.getXp(w);
        ok(rx.contains("level=10") && rx.contains("xp=5") && rx.contains("total=105")
                && rx.contains("score=0"), "getxp 回读四字段: " + rx);
        String rh = WorldOps.getHunger(w);
        ok(rh.contains("food=5") && rh.contains("sat=3.0"), "gethunger 回读 food/sat: " + rh);

        // ---- UNSUPPORTED 响亮（6 件；同 b1.7.3 判决）----
        set(w, "teStore", Class.forName("ij").getConstructor().newInstance());
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

        System.out.println("FakeOps181Test PASS (" + checks + " checks)");
    }
}
