// [B5-ARCHIVE 2026-11-22] headless test archived from %TEMP%\mcaut (source batch: A2 (0618/0629/1121)); run via java/agent/tests/run.ps1
package com.mcworldagent.agent;

/**
 * A2 验收 3：假类按 verscan 0618 字母布局——11 件已钉 op 走通 +
 * 18 件 unconfirmed op 全部 UnsupportedOp 带槽名（spec §6.3；EntityList 双轨批
 * entitylist 转已钉，命名通路=查表/类简名各验——el0618-1 活体翻案：0618 无注册名
 * per-instance 面，o()=皮肤路径陷阱已 rejected）。
 * 本 JVM 先激活 0618 再首触 WorldOps（static final 表值别名=激活表）。
 */
public class FakeOps0618Test {

    static int checks = 0;

    static void ok(boolean c, String msg) {
        checks++;
        if (!c) {
            throw new AssertionError(msg);
        }
    }

    // ---- 假类布局（verscan §1/§3/§4：World=a.a.g Entity=a.c.b Inv=a.c.e.b IS=a.b.o）----
    public static class FakeWorld0618 {
        public long b;          // worldTime
        public long m;          // randSeed
        public int i, j, k;     // spawnX/Y/Z（与 teGet 方法 i 同名合法——字段/方法分命名空间）
        public FakeEntity0618 e; // player
        public Object teStore;  // teGet 返回
        public int saveCalls;
        public final java.util.List entities = new java.util.ArrayList(); // p=loadedEntityList 复刻
        final int[] blocks = new int[16 * 16 * 16];
        final int[] meta = new int[16 * 16 * 16];

        public final java.util.List j() {       // getLoadedEntityList()List 返 p
            return entities;
        }

        static int idx(int x, int y, int z) {
            return (y & 15) * 256 + (x & 15) * 16 + (z & 15);
        }

        public boolean c(int x, int y, int z, int id) {  // setBlockWithNotify 等价
            int n = idx(x, y, z);
            boolean ch = blocks[n] != id;
            blocks[n] = id;
            return ch;
        }

        public int a(int x, int y, int z) {             // getBlockId
            return blocks[idx(x, y, z)];
        }

        public int d(int x, int y, int z) {             // getBlockData
            return meta[idx(x, y, z)];
        }

        private void b(boolean z) {                     // F1 裁决 2026-11-21：core b(Z)V（0327 先例同型）
            saveCalls++;
        }

        public Object i(int x, int y, int z) {          // getBlockTileEntity——无 TE 时 NPE（原版地雷复刻）
            if (teStore == null) {
                throw new NullPointerException("Chunk.d map miss");
            }
            return teStore;
        }
    }

    public static class FakeEntity0618 {
        public double h, i, j;  // pos
        public double k, l, m;  // mot
        public float n, o;      // yaw/pitch
        public int Z = 10;      // health（healthF 补钉批：0618 EntityLiving.Z，构造器默认 10）
        public FakeInv0618 b;   // inventory

        public final void b(double x, double y, double z, float fy, float fp) {
            // setPositionAndRotation (DDDFF)V——F1 翻案 2026-11-20 对齐（原 a(DDD)V 纯 setPosition）
            h = x;
            i = y;
            j = z;
            n = fy;
            o = fp;
        }
    }

    // 命名双通路载体（el0618-1 翻案：0618 无注册名 per-instance 面，o()=皮肤路径陷阱）：
    // 查表命中 / 查表 miss→类简名
    public static class FakePig0618 extends FakeEntity0618 {
    }

    public static class FakeSheep0618 extends FakeEntity0618 {
    }

    public static class FakeGhost0618 extends FakeEntity0618 {
    }

    public static class FakeInv0618 {
        public FakeIS0618[] a = new FakeIS0618[36];
        public FakeIS0618[] b = new FakeIS0618[4];
        public int c;
    }

    public static class FakeIS0618 {
        public int a, c, d;

        public FakeIS0618(int id, int count) {
            c = id;
            a = count;
        }

        public FakeIS0618(int id, int count, int dmg) {
            c = id;
            a = count;
            d = dmg;
        }
    }

    static void unsupported(String op, RunnableEx r, String slot) {
        checks++;
        try {
            r.run();
            throw new AssertionError(op + " should UnsupportedOp(" + slot + ")");
        } catch (WorldOps.UnsupportedOp u) {
            String m = u.getMessage();
            if (!m.contains("slot " + slot) || !m.contains("inf-20100618")) {
                throw new AssertionError(op + " msg=" + m);
            }
        } catch (Exception e) {
            throw new AssertionError(op + " wrong: " + e);
        }
    }

    interface RunnableEx {
        void run() throws Exception;
    }

    public static void main(String[] a) throws Exception {
        ok(VersionRegistry.activate("inf-20100618"), "activate 0618");
        HookTargets.activate("inf-20100618");

        FakeWorld0618 w = new FakeWorld0618();
        w.e = new FakeEntity0618();
        w.e.b = new FakeInv0618();
        WorldOps.resolve(FakeWorld0618.class);

        // ---- 已钉面走通 ----
        ok(WorldOps.setBlock(w, 5, 64, 5, 1), "place");
        ok(WorldOps.getBlockId(w, 5, 64, 5) == 1, "getblock");
        WorldOps.setBlock(w, 5, 64, 5, 0);
        ok(WorldOps.getBlockId(w, 5, 64, 5) == 0, "dig");
        String gb = WorldOps.getblocks(w, 0, 1, 0, 1, 2, 1);
        ok(gb.startsWith("getblocks 2x2x2 rle="), "getblocks " + gb);
        ok(WorldOps.getMeta(w, 5, 64, 5) == 0, "getmeta");
        String tp = WorldOps.tp(w, 10, 20, 30);
        ok(tp.startsWith("teleported"), "tp " + tp);
        ok(w.e.h == 10.5 && w.e.i == 20.0 && w.e.j == 30.5, "tp pos direct-write (DDDFF)V");
        ok(w.e.k == 0.0 && w.e.l == 0.0 && w.e.m == 0.0, "tp mot zeroed");
        ok(w.e.n == 0f && w.e.o == 0f, "tp 3-param: yaw/pitch untouched");
        WorldOps.setTime(w, 1000L);
        ok(w.b == 1000L, "settime");
        WorldOps.setSpawn(w, 8, 64, 8);
        ok(w.i == 8 && w.j == 64 && w.k == 8, "setspawn");
        WorldOps.saveWorld(w);
        ok(w.saveCalls == 1, "save (Z)V core-false");
        w.e.b.a[3] = new FakeIS0618(5, 1);
        String iv = WorldOps.inv(w);
        ok(iv.equals("3=1x5 cur=0"), "inv " + iv);
        String ci = WorldOps.clearInv(w);
        ok(ci.startsWith("cleared 36+4"), "clearinv " + ci);

        // getplayer/sethealth 转正（healthF 补钉批 0618=Z）
        String gp = WorldOps.getPlayer(w);
        ok(gp.contains("health=10"), "getplayer " + gp);
        ok("health 10 -> 15".equals(WorldOps.setHealth(w, 15)), "sethealth");
        ok(w.e.Z == 15, "sethealth 直写 Z");

        // ---- 16 件 unconfirmed → UnsupportedOp 带槽名 ----
        unsupported("getinfo", new RunnableEx() {
            public void run() throws Exception { WorldOps.getInfo(w); } }, "difficultyF");
        unsupported("give", new RunnableEx() {
            public void run() throws Exception { WorldOps.give(w, 5, 1, 0); } }, "addStackM");
        unsupported("setarmor", new RunnableEx() {
            public void run() throws Exception { WorldOps.setArmor(w, 0, 5, 1, 0); } }, "addStackM");
        unsupported("spawn", new RunnableEx() {
            public void run() throws Exception { WorldOps.spawn(w, "pig", 1, 64, 1); } }, "spawnEntityM");
        unsupported("drop", new RunnableEx() {
            public void run() throws Exception { WorldOps.drop(w, 5, 1, 0, 1, 64, 1); } }, "spawnEntityM");
        unsupported("explode", new RunnableEx() {
            public void run() throws Exception { WorldOps.explode(w, 1, 64, 1, 1); } }, "explodeM");
        unsupported("invulnerable", new RunnableEx() {
            public void run() throws Exception { WorldOps.invulnerable(w, 100); } }, "iframesF");
        w.teStore = new Object();
        unsupported("getchest", new RunnableEx() {
            public void run() throws Exception { WorldOps.getInv(w, 1, 64, 1); } }, "invSizeM");
        unsupported("setchest", new RunnableEx() {
            public void run() throws Exception { WorldOps.setInv(w, 1, 64, 1, 0, 5, 1, 0, false); } }, "invSizeM");
        unsupported("getfurnace", new RunnableEx() {
            public void run() throws Exception { WorldOps.getFurnace(w, 1, 64, 1); } }, "teBurnF");
        // entitylist 已钉（双轨批）：查表两档+类简名保底；healthF=Z（补钉批）→带 [h=] 段
        net.minecraft.a.c.e.names.put(FakePig0618.class, "Pig");
        net.minecraft.a.c.e.names.put(FakeSheep0618.class, "Sheep");
        FakePig0618 pig = new FakePig0618();
        pig.h = 1.2; pig.i = 64.0; pig.j = 2.7;
        FakeSheep0618 sheep = new FakeSheep0618();
        sheep.h = 3.0; sheep.i = 4.0; sheep.j = 5.0;
        FakeGhost0618 ghost = new FakeGhost0618();
        w.entities.add(pig);
        w.entities.add(sheep);
        w.entities.add(ghost);
        String el = WorldOps.entityList(w);
        ok(el.equals("N=3=Pig@1.2,64.0,2.7[h=10] Sheep@3.0,4.0,5.0[h=10] FakeGhost0618@0.0,0.0,0.0[h=10]"),
                "entitylist dual-track " + el);
        ok(el.contains("[h=10]"), "healthF 已钉→h 段带出");
        unsupported("playsound", new RunnableEx() {
            public void run() throws Exception { WorldOps.playSound(w, 1, 64, 1, "x", 5, 10); } }, "playSoundM");
        unsupported("genscatter", new RunnableEx() {
            public void run() throws Exception { WorldOps.genScatter(w, 14, 1, 64, 1, null); } }, "scatterClassG");
        unsupported("getlight", new RunnableEx() {
            public void run() throws Exception { WorldOps.getLightValue(w, 1, 64, 1); } }, "getLightM");
        unsupported("setmeta", new RunnableEx() {
            public void run() throws Exception { WorldOps.setMeta(w, 1, 64, 1, 1); } }, "setMetaSilentM");
        unsupported("grow", new RunnableEx() {
            public void run() throws Exception { WorldOps.growCrops(w, 1, 64, 1); } }, "idCrops");
        unsupported("clear", new RunnableEx() {
            public void run() throws Exception {
                WorldOps.clearGroundItems(w, null, null, null, null); } }, "setEntityDeadM");

        System.out.println("FakeOps0618Test PASS (" + checks + " checks)");
    }
}
