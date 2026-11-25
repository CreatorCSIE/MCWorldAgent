// [b1.9-pre2 转正批 2026-11-25] 仿 FakeOps181Test（WorldOps 真代码 × root19pre2 假件实形）
package com.mcworldagent.agent;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

/**
 * b1.9-pre2 假件 ops 全链实跑（WorldOps 真代码 × root19pre2 假件实形）。
 * 漂移槽全翻译（vs b1.8.1，禁借表）：World=rv→ti/Entity=kj→lh(abstract)/
 * EntityLiving=wd→xx/EntityPlayer=sz→uo(abstract，真身 sd)/InventoryPlayer=ui→vz
 * (as→ar)/ItemStack=ul→wc(d→e 且 private)/Item=sv→uk(f→e)/EntityList=um→wd/
 * WorldInfo=rl→sx(字母全同)/IProgressUpdate=ny→pd/TileEntity=ij→je/EntityItem=ee→eq/
 * GuiIngame=abj→adm/GameSettings=fv→gk；getWorldInfoM x→y、saveWorldDesc (Lny;)V→
 * (Lpd;)V、setPosRot b→c(setLocationAndAngles)、health bz→a(private)、iframes aa→Z、
 * setMetaSilent f(IIII)V→c(IIII)Z 裸写、biomeName l→r、foodStats at/kc→av/la、
 * xp aX/aY/aZ/aG/ay→aW/aY/aX/aF/ax、effects ade/bR→afk/bP、clearEffects 无→aG(留档)。
 * 玩家布景=sd（uo abstract 不可直造——断代主证）；spawn mob=REJECTED（vp abstract，
 * el181-1 同判）；setmeta=UNSUPPORTED（observersF 三件套，转正批判决修正 181 同判）。
 */
public class FakeOps19pre2Test {

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

    // 父链 private 直读（xx.a health / wc.e dmg / la.a/b——断代访问位对账通道）
    static Object pri(Object o, String f) throws Exception {
        Class<?> c = o.getClass();
        while (c != null) {
            try {
                Field fd = c.getDeclaredField(f);
                fd.setAccessible(true);
                return fd.get(o);
            } catch (NoSuchFieldException e) {
                c = c.getSuperclass();
            }
        }
        throw new NoSuchFieldException(f + " on " + o.getClass());
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
            if (!m.contains("slot " + slot) || !m.contains("b1.9-pre2")) {
                throw new AssertionError(op + " msg=" + m);
            }
        } catch (Exception e) {
            throw new AssertionError(op + " wrong: " + e);
        }
    }

    @SuppressWarnings("unchecked")
    public static void main(String[] a) throws Exception {
        ok(VersionRegistry.activate("b1.9-pre2"), "activate 19pre2");
        HookTargets.activate("b1.9-pre2");

        // ---- 布景：ti 玩家面=i=playerEntities；玩家=sd（uo abstract 断代主证）----
        Class<?> tiCls = Class.forName("ti");
        Object w = tiCls.getConstructor().newInstance();
        Object player = Class.forName("sd").getConstructor(tiCls).newInstance(w);
        ((List<Object>) getv(w, "i")).add(player);
        ok(((List<Object>) getv(w, "i")).size() == 1, "playerEntities i 首件=活体玩家");
        WorldOps.resolve(tiCls);

        // ---- 写读面（主写面 g(IIII)Z ✓同 181；setMetaSilent=c(IIII)Z 裸写断代）----
        ok(WorldOps.setBlock(w, 5, 64, 5, 1), "place g(IIII)Z");
        ok(WorldOps.getBlockId(w, 5, 64, 5) == 1, "getblock a(III)I");
        WorldOps.setBlock(w, 5, 64, 5, 0);
        ok(WorldOps.getBlockId(w, 5, 64, 5) == 0, "dig");
        ok(WorldOps.getblocks(w, 0, 1, 0, 1, 2, 1).startsWith("getblocks 2x2x2 rle="), "getblocks");
        ok(WorldOps.getMeta(w, 5, 64, 5) == 0, "getmeta e(III)I");

        // ---- tp：i.get(0) 回退 + c(DDDFF)V 双写 o/p/q（setPosRot b→c 断代翻面）----
        ok(WorldOps.tp(w, 10, 20, 30).startsWith("teleported"), "tp via i.get(0)");
        ok(((Double) getv(player, "o")).doubleValue() == 10.5
                && ((Double) getv(player, "p")).doubleValue() == 20.0
                && ((Double) getv(player, "q")).doubleValue() == 30.5, "tp pos o/p/q");
        ok(((Double) getv(player, "r")).doubleValue() == 0.0
                && ((Double) getv(player, "s")).doubleValue() == 0.0
                && ((Double) getv(player, "t")).doubleValue() == 0.0, "tp mot r/s/t zeroed");

        // ---- setlook/setmotion（u/v、r/s/t 直写 ✓同 181 字母）----
        ok(WorldOps.setLook(w, 90, -10).startsWith("look yaw"), "setlook");
        ok(((Float) getv(player, "u")).floatValue() == 90f
                && ((Float) getv(player, "v")).floatValue() == -10f, "setlook u/v");
        WorldOps.setMotion(w, 500, 1000, -500);
        ok(((Double) getv(player, "r")).doubleValue() == 0.5
                && ((Double) getv(player, "s")).doubleValue() == 1.0
                && ((Double) getv(player, "t")).doubleValue() == -0.5, "setmotion r/s/t");

        // ---- WorldInfo 链重点：settime/setspawn 经 ti.y()Lsx 五 private 槽读写
        //      （getWorldInfoM x→y 断代；sx 字母面=rl 全同判决兑现）----
        WorldOps.setTime(w, 1000L);
        Object info = tiCls.getMethod("y").invoke(w);
        ok(info.getClass().getName().equals("sx"), "ti.y() 返回 sx（=rl 复刻）");
        ok(((Long) pri(info, "e")).longValue() == 1000L, "settime 写 sx.e private 经 setAccessible");
        WorldOps.setSpawn(w, 8, 64, 8);
        ok(((Integer) pri(info, "b")) == 8 && ((Integer) pri(info, "c")) == 64
                && ((Integer) pri(info, "d")) == 8, "setspawn 写 sx.b/c/d");
        ok(pri(info, "a") instanceof Long && ((Long) pri(info, "a")).longValue() == 0L,
                "seed=sx.a 非 ti 面");

        // ---- getinfo（timeHolder=WorldInfo 对象；difficulty v ✓同 181）----
        set(w, "v", 2);
        String got = WorldOps.getInfo(w);
        ok(got.equals("time=1000 spawnX=8 spawnY=64 spawnZ=8 seed=0 difficulty=2"),
                "getinfo 全串对账 " + got);

        // ---- getlight n(III)I ✓同字母 ----
        ok(WorldOps.getLightValue(w, 5, 64, 5) == 15, "getlight n(III)I");

        // ---- saveWorld：a(Lpd;)V 传 null（IProgressUpdate yb→ny→pd 断代，守卫六连用）----
        WorldOps.saveWorld(w);
        ok((Integer) getv(w, "saveCalls") == 1, "save via a(Lpd;)V wrapper->body");
        ok((Integer) getv(w, "levelSaves") == 1, "body 调 saveLevel r()V");

        // ---- getbiome：ti.a()Loz -> oz.a(II)Lwz -> wz.r 三锚链（全断代，r≠181 l）----
        String b1 = WorldOps.biomeAt(w, 100, 100);
        String b2 = WorldOps.biomeAt(w, -100, -100);
        String b3 = WorldOps.biomeAt(w, 0, 0);
        ok(b1 != null && b1.length() > 0, "getbiome 非空串 " + b1);
        ok("Desert".equals(b1), "getbiome (100,100)=Desert got " + b1);
        ok("Ice Desert".equals(b2), "getbiome (-100,-100)=Ice Desert got " + b2);
        ok("Plains".equals(b3), "getbiome (0,0)=Plains got " + b3);

        // ---- 物品栏面（uo.ar→vz；wc.e **private** 断代=resolveInvFace 回退消费）----
        Object inv = getv(player, "ar");
        Object[] mainArr = (Object[]) getv(inv, "a");
        ok(mainArr.length == 36, "invMain wc[36]");
        Constructor<?> isCtor3 = Class.forName("wc").getConstructor(int.class, int.class, int.class);
        mainArr[3] = isCtor3.newInstance(5, 1, 0);
        ok(WorldOps.inv(w).equals("3=1x5 cur=0"), "inv via i.get(0)");
        // give dmg=0：wc 无 (II)V——newStack 走 (III)V 等价配方（181 ul 同形）
        Object[] reg = (Object[]) Class.forName("uk").getField("e").get(null);
        reg[5] = Class.forName("uk").getConstructor(int.class).newInstance(5);
        ok(WorldOps.give(w, 5, 2, 0).startsWith("gave"), "give via newStack (III)V");
        ok("gave 5 x1 dmg=3".equals(WorldOps.give(w, 5, 1, 3)), "give dmg=3");
        ok(WorldOps.inv(w).contains(" d3"), "inv reads wc.e（private 回退主证）");
        ok(WorldOps.clearInv(w).startsWith("cleared 36+4"), "clearinv 36+4");

        // ---- health 面（xx.a **private**——WorldOps.healthField 父链回退消费主证；
        // sd.a=String 遮蔽件在位，复现活体 mn 形态：旧 getField 首探即崩，
        // int 类型门必须跳过遮蔽取 xx.a——活体批 2026-11-25 W19P2 ERROR 回归钉）----
        ok(WorldOps.getPlayer(w).contains("health=10"), "getplayer（private a 读通、遮蔽跳过）");
        ok("health 10 -> 15".equals(WorldOps.setHealth(w, 15)), "sethealth");
        java.lang.reflect.Field hf = Class.forName("xx").getDeclaredField("a");
        hf.setAccessible(true);
        ok(hf.getInt(player) == 15, "sethealth 直写 xx.a=15（非 sd.a 遮蔽）");
        ok(WorldOps.getPlayer(w).contains("health=15"), "getPlayer 读回 15（int 门路径）");
        // 活体批2 防劫持钉：uo.a=sleepTimer 撞车字段必须零污染（mob 链探针选中 xx）
        java.lang.reflect.Field sf = Class.forName("uo").getDeclaredField("a");
        sf.setAccessible(true);
        ok(sf.getInt(player) == 0, "sleepTimer(uo.a) 未被 health 面劫持");

        // ---- invulnerable（iframesF=Z——heartsLife 回归钉，181 aa→Z 断代）----
        ok("iframes 0 -> 100".equals(WorldOps.invulnerable(w, 100)), "invulnerable via lh.Z");
        ok((Integer) getv(player, "Z") == 100, "invulnerable 直写 Z=100");

        // ---- entitylist 双轨：wd.b(lh) 查表 + 类简名保底 + [h=] 条件段（abg 非 xx 系）----
        Class<?> wdCls = Class.forName("wd");
        java.util.Map<Object, Object> names =
                (java.util.Map<Object, Object>) wdCls.getField("names").get(null);
        names.put(Class.forName("oa"), "Pig");
        names.put(Class.forName("abg"), "PrimedTnt");
        Object pig = Class.forName("oa").getConstructor(tiCls).newInstance(w);
        set(pig, "o", 1.2); set(pig, "p", 64.0); set(pig, "q", 2.7);
        Object tnt = Class.forName("abg").getConstructor(tiCls, double.class, double.class, double.class)
                .newInstance(w, 0.0, 0.0, 0.0);
        // ghost=ub（Creeper 具体件；vp=EntityCreature abstract 不可实例化——断代判决）
        Object ghost = Class.forName("ub").getConstructor(tiCls).newInstance(w);
        Method addE = tiCls.getMethod("a", Class.forName("lh")); // entityJoinedWorld a(Llh;)Z
        addE.invoke(w, pig);
        addE.invoke(w, tnt);
        addE.invoke(w, ghost);
        String el = WorldOps.entityList(w);
        ok(el.contains("Pig@1.2,64.0,2.7[h=10]"), "entitylist 查表+health 段 " + el);
        ok(el.contains("PrimedTnt@0.0,0.0,0.0") && !el.contains("PrimedTnt@0.0,0.0,0.0[h="),
                "entitylist tnt 无 h 段（abg 非 xx 系）");
        ok(el.contains("ub@"), "entitylist 类简名保底 ub");

        // ---- spawn/drop/explode/playsound/clear ----
        String sp = WorldOps.spawn(w, "sheep", 1, 64, 1);
        ok(sp.startsWith("spawned sheep"), "spawn " + sp);
        List<?> pList = (List<?>) getv(w, "g");
        ok(pList.size() == 4, "spawn 入列 loadedEntityList g（p()List 同表）");
        // mob 槽已摘（vp=EntityCreature abstract——181 el181-1 同判延续）
        try {
            WorldOps.spawn(w, "mob", 1, 64, 1);
            ok(false, "spawn mob must reject (slot removed)");
        } catch (WorldOps.Rejected r) {
            ok(String.valueOf(r.getMessage()).contains("bad-kind: mob"), "spawn mob=REJECTED bad-kind");
        }
        ok(WorldOps.spawn(w, "giant", 1, 64, 1).startsWith("spawned giant"), "spawn giant=ip 构造健在");
        // tnt 重点：FFF miss→DDD fallback（qw→ze→abg 断代延续）
        ok(WorldOps.spawn(w, "tnt", 1, 64, 1).startsWith("spawned tnt"), "spawn tnt=abg DDD fallback");
        ok(WorldOps.drop(w, 5, 1, 0, 1, 64, 1).startsWith("dropped"), "drop eq");
        Object item = pList.get(pList.size() - 1);
        ok(item.getClass().getName().equals("eq"), "drop 实体=eq（hl→ee→eq 断代）");
        names.put(Class.forName("eq"), "Item");
        ok(WorldOps.entityList(w).contains("Item@"), "entitylist Item=eq 注册名");
        // explode：返回 abf 对象但 invoke 面不消费返回值（V→Explosion 断代留档）
        ok(WorldOps.explode(w, 1, 64, 1, 1).startsWith("exploded"), "explode");
        ok((Integer) getv(w, "expl") == 1, "explode 调用面 a(Llh;DDDF)Labf");
        ok(WorldOps.playSound(w, 1, 64, 1, "random.fizz", 5, 10).startsWith("played"), "playsound");
        ok((Integer) getv(w, "snd") == 1, "playsound 调用面 a(DDD,String,FF)V");
        // clear 核心：setEntityDead=实例 b(Llh)V——置 G=dead ✓同 181 字母
        ok(WorldOps.clearGroundItems(w, null, null, null, null).startsWith("cleared 1"),
                "clear eq 实体（loaded 仅 eq 命中）");
        ok((Boolean) getv(item, "G"), "setEntityDead 实例 b(Llh)V 置 G=dead");
        ok((Integer) getv(w, "killCalls") == 1, "clear 收 world 调用（实例版对账）");

        // ---- D10 setxp/getxp（aW/aY/aX/aF/ax——双 level 并存怪癖主证，181 字母整体不同）----
        ok(WorldOps.setXp(w, 10, 5).equals("xp level 0 -> 10 xp 0 -> 5"), "setxp 旧值回显");
        ok((Integer) getv(player, "aY") == 10, "setxp 写 aY=XpLevel");
        ok((Integer) getv(player, "aW") == 5, "setxp 写 aW=Xp");
        ok((Integer) getv(player, "aX") == 105, "setxp 写 aX=playerLevel(XpTotal)=level*10+xp");
        ok((Integer) getv(player, "aF") == 0, "setxp 清 aF=XpCooldown");
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

        // ---- D10 giveeffect/removeeffect（xx.a(Lafk;)V/i(I)V + bP——19pre2 首次完整可钉）----
        ok(WorldOps.giveEffect(w, 1, 200, 0).startsWith("giveeffect id=1"), "giveeffect 回显");
        ok(((java.util.Map) getv(player, "bP")).size() == 1, "giveeffect 入 bP（键=effectId）");
        Object eff = ((java.util.Map) getv(player, "bP")).get(Integer.valueOf(1));
        ok(eff != null && eff.getClass().getName().equals("afk"), "bP 值=afk 件（ade→afk 断代）");
        ok((Integer) getv(eff, "b") == 200, "afk.duration=200 对账");
        WorldOps.giveEffect(w, 4, 100, 2);
        ok(((java.util.Map) getv(player, "bP")).size() == 2, "giveeffect 第二效果");
        ok(WorldOps.removeEffect(w, 1).startsWith("removeeffect id=1"), "removeeffect 回显");
        ok(!((java.util.Map) getv(player, "bP")).containsKey(Integer.valueOf(1)),
                "removeeffect 移 id=1（i(I)V bP.remove）");
        ok(((java.util.Map) getv(player, "bP")).containsKey(Integer.valueOf(4)), "id=4 保留");
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

        // ---- D11 sethunger/gethunger（uo.av()Lla -> la.a/b private——181 at/kc 断代主证）----
        ok(WorldOps.setHunger(w, 10, 3).equals("hunger food 20 -> 10 sat 5.0 -> 3"), "sethunger 旧值回显");
        Object fs = Class.forName("uo").getMethod("av").invoke(player);
        ok(fs.getClass().getName().equals("la"), "av() 返回 la（FoodStats 断代）");
        ok((Integer) pri(fs, "a") == 10, "sethunger 写 la.a=foodLevel private");
        ok((Float) pri(fs, "b") == 3.0f, "sethunger 写 la.b=saturation private");
        ok(WorldOps.setHunger(w, 5, -1).equals("hunger food 10 -> 5"), "sethunger sat 缺省不动");
        ok((Float) pri(fs, "b") == 3.0f, "sethunger sat 保留");
        try {
            WorldOps.setHunger(w, 21, 0);
            ok(false, "sethunger food>20 must reject");
        } catch (WorldOps.Rejected r) {
            ok(String.valueOf(r.getMessage()).contains("food-out-of-range"), "sethunger food REJECTED");
        }

        // ---- D12 weather（capWeather=TRUE；ti.o/q protected 浮值面 ✓同 181 字母；
        //      T1 批 2026-11-26：旗标 setter 双写首钉——sx.b/a(Z)V 消费面）----
        ok(WorldOps.weather(w, true, true).contains("flag setters"),
                "weather 回显含旗标面（T1 双写路径）");
        ok((Float) getv(w, "o") == 1.0f, "weather 写 ti.o=rainingStrength 1.0");
        ok((Float) getv(w, "q") == 1.0f, "weather 写 ti.q=thunderingStrength 1.0");
        WorldOps.weather(w, false, true);
        ok((Float) getv(w, "o") == 0.0f, "weather raining=false 写 0.0");
        ok((Float) getv(w, "q") == 1.0f, "weather thunder 保留 1.0");
        Object wiT1 = pri(w, "info");
        ok(!((Boolean) wiT1.getClass().getField("rainFlagSet").get(wiT1)).booleanValue(),
                "weather 旗标 setIsRaining(false) 双写（sx.b）");
        ok(((Boolean) wiT1.getClass().getField("thunderFlagSet").get(wiT1)).booleanValue(),
                "weather 旗标 setIsThundering(true) 双写（sx.a）");

        // ---- T2 sleep（capSleep=TRUE；uo.aM sleeping protected+uo.a sleepTimer
        //      private 声明类精确解析；ti.z() 旗标刷新计数；⚠️ a 撞名面与
        //      healthField 互不干扰——sleep 写后 health 面须原值）----
        java.lang.reflect.Field uoST = Class.forName("uo").getDeclaredField("a");
        uoST.setAccessible(true);
        ok(WorldOps.setSleep(w, true).startsWith("sleep off -> on timer 0 -> 100"),
                "sleep on 回显");
        ok(((Boolean) pri(player, "aM")).booleanValue(), "sleep 写 uo.aM=sleeping true");
        ok(((Integer) uoST.getInt(player)).intValue() == 100, "sleep 写 uo.a=sleepTimer=100");
        ok(((Integer) pri(w, "sleepFlagRefreshes")).intValue() == 1, "ti.z() 旗标刷新 1 次");
        ok(WorldOps.setSleep(w, false).startsWith("sleep on -> off timer 100 -> 0"),
                "sleep off 回显（旧值链）");
        ok(!((Boolean) pri(player, "aM")).booleanValue(), "wake 清 uo.aM");
        ok(uoST.getInt(player) == 0, "wake 清 uo.a=0");
        ok(((Integer) pri(w, "sleepFlagRefreshes")).intValue() == 2, "ti.z() 累计 2 次");
        // 撞名隔离钉：sleep 面读写零触碰 health（xx.a 仍 15，见上 sethealth 段）
        ok(WorldOps.getPlayer(w).contains("health=15"), "sleep 面零污染 health（uo.a vs xx.a 隔离）");

        // ---- D13 setgamemode（sx.p=GameType private int——rl→sx 字段全同判决兑现）----
        Object winfo = pri(w, "info");        // ti.y()Lsx 承载体
        ok(WorldOps.setGameMode(w, 1).startsWith("gamemode 0 -> 1"), "setgamemode 旧值回显");
        ok((Integer) pri(winfo, "p") == 1, "setgamemode 写 sx.p=GameType private=1(creative)");
        ok(WorldOps.setGameMode(w, 0).startsWith("gamemode 1 -> 0"), "setgamemode 回 survival");
        ok((Integer) pri(winfo, "p") == 0, "setgamemode 写回 0");
        try {
            WorldOps.setGameMode(w, 2);
            ok(false, "setgamemode mode=2 must reject (pre2 no adventure)");
        } catch (WorldOps.Rejected r) {
            ok(String.valueOf(r.getMessage()).contains("gamemode-out-of-range"), "setgamemode mode REJECTED");
        }

        // ---- D14 getxp/gethunger 读面（复用已写字段回读，只读不写）----
        String rx = WorldOps.getXp(w);
        ok(rx.contains("level=10") && rx.contains("xp=5") && rx.contains("total=105")
                && rx.contains("score=0"), "getxp 回读四字段: " + rx);
        String rh = WorldOps.getHunger(w);
        ok(rh.contains("food=5") && rh.contains("sat=3.0"), "gethunger 回读 food/sat: " + rh);

        // ---- UNSUPPORTED 响亮（7 件——setmeta 转正批判决修正入列，181 同判）----
        set(w, "teStore", Class.forName("je").getConstructor().newInstance());
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

        System.out.println("FakeOps19pre2Test PASS (" + checks + " checks)");
    }
}
