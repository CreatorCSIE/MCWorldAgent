// [B5-ARCHIVE 2026-11-22] headless test archived from %TEMP%\mcaut (source batch: 0327 fake-world (pre-A2, legacy)); run via java/agent/tests/run.ps1
package mcaut;

import com.mcworldagent.agent.MiniJson;

import java.util.Map;

public class MiniJsonTest {
    /** 0327 Entity 基类替身（net.minecraft.a.c.b 指纹）：d/e/f=lastTick, g/h/i=pos, j/k/l=motion, m/n=yaw/pitch, u=眼偏移。
     *  非 final：假 mob/item 类（真混淆 FQCN 上 classpath）extends 本类（spawn-explode-drop 批）。 */
    public static class FakeEntity {
        public double d, e, f;
        public double g, h, i;
        public double j, k, l;
        public float m, n;
        public float u = 1.62f;
        public int W = 10; // EntityLiving.Health 替身（getplayer）
        public int K = 0;  // Entity.heartsLife 替身（invulnerable i-frame，sed-1 批）
        public boolean dead; // isDead 替身（clear 批 setEntityDead 标记面）
        public FakeInv b = new FakeInv(); // EntityHuman.b=InventoryPlayer 替身（give/inv）

        public FakeEntity() {
        }

        /** Entity(World) 语义对齐：仅记 worldObj+pos(0,0,0)，不入世界列表（构造零副作用实锤）。 */
        public FakeEntity(FakeWorld w) {
            this.world = w;
        }

        public FakeWorld world;

        /** b(DDDFF)V=setPositionAndRotation：d=g=x; e=h=y+u; f=i=z; m/n 重设（AABB 重建头替身省略）。 */
        public final void b(double x, double y, double z, float yaw, float pitch) {
            this.g = this.d = x;
            this.h = this.e = y + this.u;
            this.i = this.f = z;
            this.m = yaw;
            this.n = pitch;
        }

        /** q()String=getEntityType（entitylist 面；非 final，假 pig 覆写）。 */
        public String q() {
            return "Entity";
        }
    }

    /** ItemStack（net.minecraft.a.b.k）替身：a=Count、b=拾取延迟、c=id、d=Damage；双构造器 (II)/(III)。 */
    public static final class FakeStack {
        public int a;
        public int b;
        public final int c;
        public final int d;

        public FakeStack(int id, int count) {
            this(id, count, 0);
        }

        public FakeStack(int id, int count, int damage) {
            this.c = id;
            this.a = count;
            this.d = damage;
        }
    }

    /** InventoryPlayer（a.c.e.b）替身：a[36] 主槽/b[4] 盔甲/c 当前槽/a(ItemStack)Z=addStack。 */
    public static final class FakeInv {
        public final FakeStack[] a = new FakeStack[36];
        public final FakeStack[] b = new FakeStack[4];
        public int c;

        /** addStack 替身（0327 语义简化）：同 id 且 dmg==0 且装得下才合并，否则首个空槽；无→false=装不下。 */
        public final boolean a(FakeStack s) {
            if (s.d == 0) {
                for (int i = 0; i < a.length; i++) {
                    if (a[i] != null && a[i].c == s.c && a[i].a + s.a <= 64) {
                        a[i].a += s.a;
                        return true;
                    }
                }
            }
            for (int i = 0; i < a.length; i++) {
                if (a[i] == null) {
                    a[i] = s;
                    return true;
                }
            }
            return false;
        }
    }

    /** 渲染观察者替身（World.u 元素，接口方法 a(III)V）。 */
    public static final class FakeObserver {
        public int calls;

        public final void a(int x, int y, int z) {
            calls++;
        }
    }

    /**
     * 0327 World 指纹替身：写侧 c(IIII)Z + 读侧 a(III)I/d(III)I + meta 静默写 b(IIII)V
     * + 光照 6 邻 e(IIII)V + 观察者表 u(private List) + 字段 b/x/g/h/i/d(difficulty)/c。
     * blocks/metaStore 键="x,y,z"；blocks 空→a() 回退 readId（getblocks RLE 面兼容）。
     */
    public static final class FakeWorld {
        public long b = 12345L;
        private long x = -99L;
        public int g = 10, h = 64, i = -20;
        public int d = 2; // difficultySetting 替身（字段与方法 d 同名合法）
        public FakeEntity c;
        public boolean setBlockResult = true; // fill changed 计数面
        public int readId = 0;                // getblocks RLE 面
        public final java.util.Map<String, Integer> blocks = new java.util.HashMap<String, Integer>();
        public final java.util.Map<String, Integer> metaStore = new java.util.HashMap<String, Integer>();
        private final java.util.List<Object> u = new java.util.ArrayList<Object>();
        public int light6Calls;
        public final java.util.Map<String, Integer> lightStore = new java.util.HashMap<String, Integer>();
        public int lightDefault = 0; // getlight 面缺省（f(III)I=getBrightness 替身）

        public void addObs(Object o) {
            u.add(o);
        }

        public final boolean c(int a, int bb, int cc, int dd) {
            return setBlockResult;
        }

        public final int a(int aa, int bb, int cc) {
            Integer v = blocks.get(aa + "," + bb + "," + cc);
            return v == null ? readId : v.intValue();
        }

        public final int d(int aa, int bb, int cc) {
            Integer v = metaStore.get(aa + "," + bb + "," + cc);
            return v == null ? 0 : v.intValue();
        }

        public final void b(int aa, int bb, int cc, int m) {
            metaStore.put(aa + "," + bb + "," + cc, Integer.valueOf(m));
        }

        // grow/clear 批替身：World.b(Entity)V **static** setEntityDead（仅置 dead，真移除在 tick）。
        public static final void b(FakeEntity e) {
            e.dead = true;
        }

        public final void e(int aa, int bb, int cc, int id) {
            light6Calls++;
        }

        public final int f(int aa, int bb, int cc) {
            Integer v = lightStore.get(aa + "," + bb + "," + cc);
            return v == null ? lightDefault : v.intValue();
        }

        // 实体面替身（spawn-explode-drop 批）：a(Entity)V=spawnEntityInWorld、
        // a(Entity,DDDF)V=createExplosion（source 可为 null——判等实现零 NPE，记录供对账）。
        public final java.util.List<Object> spawnedEntities =
                new java.util.ArrayList<Object>();
        public double[] lastExplosion;
        public float lastExplosionSize = -1f;
        public FakeEntity lastExplosionSource = new FakeEntity(); // null 区分哨兵

        public final void a(FakeEntity e) {
            spawnedEntities.add(e);
        }

        public final void a(FakeEntity e, double x, double y, double z, float size) {
            lastExplosionSource = e;
            lastExplosion = new double[] { x, y, z };
            lastExplosionSize = size;
        }

        // tileentity 面替身（tileentity-batch）：h(III)=getBlockTileEntity。
        // **复刻 0327 原版地雷（te-1 实跑字节码实锤）**：map miss 且方块=空气→重建路径 NPE；
        // miss 但方块非空气→返回 null。
        public final java.util.Map<String, Object> teStore =
                new java.util.HashMap<String, Object>();

        public final Object h(int aa, int bb, int cc) {
            String k = aa + "," + bb + "," + cc;
            if (teStore.containsKey(k)) {
                return teStore.get(k);
            }
            Integer id = blocks.get(k);
            if (id == null || id.intValue() == 0) {
                throw new NullPointerException("vanilla Chunk.d landmine");
            }
            return null;
        }

        // 零取证五 op 批替身：i()List=getLoadedEntityList + playSoundEffect 记录面。
        public final java.util.List<Object> i() {
            java.util.List<Object> out = new java.util.ArrayList<Object>();
            out.addAll(spawnedEntities);
            return out;
        }

        public String lastSoundName;
        public double lastSoundX, lastSoundY, lastSoundZ;
        public float lastSoundVol, lastSoundPitch;

        public final void a(double x, double y, double z, String name, float vol, float pitch) {
            lastSoundName = name;
            lastSoundX = x;
            lastSoundY = y;
            lastSoundZ = z;
            lastSoundVol = vol;
            lastSoundPitch = pitch;
        }

        private void a(boolean z) {
        }
    }

    static int pass = 0, fail = 0;

    static void ok(boolean cond, String name) {
        if (cond) { pass++; } else { fail++; System.out.println("FAIL: " + name); }
    }

    static void shouldThrow(String json, String name) {
        try {
            MiniJson.parse(json);
            fail++; System.out.println("FAIL(no-throw): " + name);
        } catch (IllegalArgumentException e) {
            pass++;
        }
    }

    public static void main(String[] a) throws Exception {
        Map<String, Object> env = MiniJson.parse(
                "{\"protocolVersion\":1,\"id\":\"c1\",\"op\":\"place\",\"args\":{\"x\":0,\"y\":71,\"z\":0,\"blockId\":4},\"save\":true}");
        ok(Long.valueOf(1).equals(env.get("protocolVersion")), "proto");
        ok("c1".equals(env.get("id")), "id");
        ok("place".equals(env.get("op")), "op");
        ok(Boolean.TRUE.equals(env.get("save")), "save");
        Map<?, ?> args = (Map<?, ?>) env.get("args");
        ok(Long.valueOf(71).equals(args.get("y")), "args.y");
        ok(Long.valueOf(4).equals(args.get("blockId")), "args.blockId");

        Map<String, Object> neg = MiniJson.parse("{\"protocolVersion\":1,\"id\":\"x-1\",\"op\":\"dig\",\"args\":{\"x\":-5,\"y\":70,\"z\":-5},\"save\":false}");
        Map<?, ?> na = (Map<?, ?>) neg.get("args");
        ok(Long.valueOf(-5).equals(na.get("x")), "negative int");
        ok(Boolean.FALSE.equals(neg.get("save")), "save=false");

        // 注：{} 是合法 JSON 子集（协议必填项校验在 CommandChannel.validate，不在解析器）。
        shouldThrow("{\"a\":1,\"a\":2}", "duplicate key");
        shouldThrow("{\"a\":{\"b\":{\"c\":1}}}", "depth>1");
        shouldThrow("{\"a\":[1,2]}", "array");
        shouldThrow("{\"a\":null}", "null literal");
        shouldThrow("{\"a\":1.5}", "float");
        shouldThrow("{\"a\":1e3}", "exponent");
        shouldThrow("{\"a\":1} trailing", "trailing");
        shouldThrow("{\"a\":\"\\q\"}", "bad escape");
        shouldThrow("[1,2]", "top not object");
        shouldThrow("{\"a\"", "unterminated");
        shouldThrow("{\"a\":}", "missing value");
        StringBuilder big = new StringBuilder("{\"a\":\"");
        for (int i = 0; i < 5000; i++) { big.append('x'); }
        shouldThrow(big.toString() + "\"}", "size>4KB");
        StringBuilder longStr = new StringBuilder("{\"a\":\"");
        for (int i = 0; i < 300; i++) { longStr.append('x'); }
        shouldThrow(longStr.toString() + "\"}", "string>256");
        shouldThrow("{\"a\":\"\u0001\"}", "raw control char");

        System.out.println("MiniJsonTest pass=" + pass + " fail=" + fail);

        // CommandChannel.validate（private static）反射断言：null=通过，非 null=拒绝原因
        java.lang.reflect.Method v = com.mcworldagent.agent.CommandChannel.class
                .getDeclaredMethod("validate", Map.class);
        v.setAccessible(true);
        ok(v.invoke(null, MiniJson.parse(
                "{\"protocolVersion\":1,\"id\":\"c1\",\"op\":\"getblock\",\"args\":{\"x\":1,\"y\":2,\"z\":3}}")) == null, "validate ok");
        ok(String.valueOf(v.invoke(null, MiniJson.parse(
                "{\"protocolVersion\":2,\"id\":\"c1\",\"op\":\"getblock\"}"))).contains("protocolVersion 2"), "proto mismatch reason");
        ok(String.valueOf(v.invoke(null, MiniJson.parse(
                "{\"protocolVersion\":1,\"id\":\"c1\",\"op\":\"fly\"}"))).contains("unknown op"), "unknown op reason");
        ok(v.invoke(null, MiniJson.parse(
                "{\"protocolVersion\":1,\"op\":\"save\"}")) != null, "missing id rejected");
        ok(v.invoke(null, MiniJson.parse(
                "{\"protocolVersion\":1,\"id\":\"bad id!\",\"op\":\"save\"}")) != null, "bad id charset rejected");
        ok(v.invoke(null, MiniJson.parse(
                "{\"protocolVersion\":1,\"id\":\"c2\",\"op\":\"save\",\"args\":5}")) != null, "args non-object rejected");

        // P1 targetGeneration 形态校验（信封字段，非 args）
        ok(v.invoke(null, MiniJson.parse(
                "{\"protocolVersion\":1,\"id\":\"c3\",\"op\":\"getblock\",\"args\":{\"x\":1,\"y\":2,\"z\":3},\"targetGeneration\":2}")) == null,
                "targetGeneration integer passes");
        ok(String.valueOf(v.invoke(null, MiniJson.parse(
                "{\"protocolVersion\":1,\"id\":\"c4\",\"op\":\"save\",\"targetGeneration\":\"2\"}")))
                .contains("targetGeneration must be integer"), "targetGeneration string rejected");
        ok(v.invoke(null, MiniJson.parse(
                "{\"protocolVersion\":1,\"id\":\"c5\",\"op\":\"save\",\"targetGeneration\":true}")) != null,
                "targetGeneration bool rejected");

        // Query getinfo：validate 放行（无 args）
        ok(v.invoke(null, MiniJson.parse(
                "{\"protocolVersion\":1,\"id\":\"g1\",\"op\":\"getinfo\"}")) == null,
                "getinfo passes validate");

        // WorldOps 读侧反射：FakeWorld 复刻 0327 指纹（b/x/g/h/i/d + c/a/d/b/e/u 签名）
        java.lang.reflect.Method resolve = com.mcworldagent.agent.WorldOps.class
                .getDeclaredMethod("resolve", Class.class);
        resolve.setAccessible(true);
        // 先负后正（resolve 有静态缓存）
        boolean unsupported = false;
        try {
            resolve.invoke(null, Object.class);
        } catch (java.lang.reflect.InvocationTargetException ite) {
            unsupported = ite.getCause() instanceof com.mcworldagent.agent.WorldOps.UnsupportedOp;
        }
        ok(unsupported, "resolve(Object) -> UnsupportedOp");
        resolve.invoke(null, FakeWorld.class);
        String info = com.mcworldagent.agent.WorldOps.getInfo(new FakeWorld());
        ok("time=12345 spawnX=10 spawnY=64 spawnZ=-20 seed=-99 difficulty=2".equals(info),
                "getInfo snapshot: " + info);

        // settime/setspawn 写侧（读写同字段纪律）：validateTime 边界 + 字段写入 + getInfo 回读互证
        com.mcworldagent.agent.WorldOps.validateTime(0L); // 0 合法（世界时间起点）
        boolean timeRej = false;
        try {
            com.mcworldagent.agent.WorldOps.validateTime(-1L);
        } catch (com.mcworldagent.agent.WorldOps.Rejected e) {
            timeRej = true;
        }
        ok(timeRej, "validateTime(-1) -> Rejected");
        FakeWorld fw = new FakeWorld();
        long old = com.mcworldagent.agent.WorldOps.setTime(fw, 6000L);
        ok(old == 12345L && fw.b == 6000L, "setTime returns old, writes b");
        com.mcworldagent.agent.WorldOps.setSpawn(fw, 100, 70, -100);
        ok(fw.g == 100 && fw.h == 70 && fw.i == -100, "setSpawn writes g/h/i");
        ok("time=6000 spawnX=100 spawnY=70 spawnZ=-100 seed=-99 difficulty=2"
                        .equals(com.mcworldagent.agent.WorldOps.getInfo(fw)),
                "getInfo reads back post-write");
        ok(v.invoke(null, MiniJson.parse(
                "{\"protocolVersion\":1,\"id\":\"t1\",\"op\":\"settime\",\"args\":{\"time\":6000}}")) == null,
                "settime passes validate");
        ok(v.invoke(null, MiniJson.parse(
                "{\"protocolVersion\":1,\"id\":\"t2\",\"op\":\"setspawn\",\"args\":{\"x\":100,\"y\":70,\"z\":-100}}")) == null,
                "setspawn passes validate");

        // tp：validate 放行 + no-player 拒绝 + 复刻载入配方（pos 块中心/清 motion/lastTick 同步/朝向保持/眼偏移 u）
        ok(v.invoke(null, MiniJson.parse(
                "{\"protocolVersion\":1,\"id\":\"p1\",\"op\":\"tp\",\"args\":{\"x\":100,\"y\":70,\"z\":-100}}")) == null,
                "tp passes validate");
        FakeWorld noPl = new FakeWorld();
        boolean noPlayerRej = false;
        try {
            com.mcworldagent.agent.WorldOps.tp(noPl, 5, 70, 5);
        } catch (com.mcworldagent.agent.WorldOps.Rejected e) {
            noPlayerRej = "no-player".equals(e.getMessage());
        }
        ok(noPlayerRej, "tp(null player) -> Rejected no-player");
        FakeWorld tw = new FakeWorld();
        FakeEntity pe = new FakeEntity();
        pe.g = 5; pe.h = 64; pe.i = 5;
        pe.j = 1; pe.k = 2; pe.l = 3;
        pe.m = 10f; pe.n = 20f;
        tw.c = pe;
        String tpDetail = com.mcworldagent.agent.WorldOps.tp(tw, 100, 70, -100);
        ok(tpDetail.contains("from x=5.0 y=64.0 z=5.0") && tpDetail.contains("-> x=100 y=70 z=-100"),
                "tp returns old->new: " + tpDetail);
        ok(pe.g == 100.5 && pe.i == -99.5, "tp pos x/z block-center (+0.5)");
        ok(Math.abs(pe.h - 71.62) < 1e-6, "tp h = y + u(1.62) via b(): " + pe.h);
        ok(pe.d == pe.g && pe.e == pe.h && pe.f == pe.i, "tp lastTick d/e/f synced");
        ok(pe.j == 0 && pe.k == 0 && pe.l == 0, "tp motion zeroed");
        ok(pe.m == 10f && pe.n == 20f, "tp yaw/pitch preserved");

        // fill/getblocks/getplayer：validate 放行（三 op 入 KNOWN_OPS）
        ok(v.invoke(null, MiniJson.parse(
                "{\"protocolVersion\":1,\"id\":\"f1\",\"op\":\"fill\",\"args\":{\"x1\":0,\"y1\":70,\"z1\":0,\"x2\":1,\"y2\":71,\"z2\":1,\"blockId\":4}}")) == null,
                "fill passes validate");
        ok(v.invoke(null, MiniJson.parse(
                "{\"protocolVersion\":1,\"id\":\"f2\",\"op\":\"getblocks\",\"args\":{\"x1\":0,\"y1\":70,\"z1\":0,\"x2\":1,\"y2\":71,\"z2\":1}}")) == null,
                "getblocks passes validate");
        ok(v.invoke(null, MiniJson.parse(
                "{\"protocolVersion\":1,\"id\":\"f3\",\"op\":\"getplayer\"}")) == null,
                "getplayer passes validate");

        // normalizeRegion：反角归一化 + 边界值 + cap
        int[] r = com.mcworldagent.agent.WorldOps.normalizeRegion(10, 70, 10, 0, 68, 0);
        ok(r[0] == 0 && r[1] == 68 && r[2] == 0 && r[3] == 10 && r[4] == 70 && r[5] == 10,
                "normalizeRegion swaps corners");
        int[] r2 = com.mcworldagent.agent.WorldOps.normalizeRegion(0, 64, 0, 15, 79, 15);
        ok(r2 != null, "volume exactly 4096 allowed");
        boolean volRej = false;
        try {
            com.mcworldagent.agent.WorldOps.normalizeRegion(0, 64, 0, 15, 79, 16); // 4352
        } catch (com.mcworldagent.agent.WorldOps.Rejected e) {
            volRej = e.getMessage().startsWith("volume-too-large: 4352");
        }
        ok(volRej, "volume-too-large rejected");
        boolean coordRej = false;
        try {
            com.mcworldagent.agent.WorldOps.normalizeRegion(0, 0, 0, 1, 1, 1); // y=0 越界
        } catch (com.mcworldagent.agent.WorldOps.Rejected e) {
            coordRej = true;
        }
        ok(coordRej, "region corner coords validated");

        // fill：changed 计数 + 零变更 NoChange（NO_CHANGE 生产者之一）
        FakeWorld fw2 = new FakeWorld();
        String fillDetail = com.mcworldagent.agent.WorldOps.fill(fw2, 0, 70, 0, 1, 71, 1, 4);
        ok(fillDetail.equals("filled [0..1] [70..71] [0..1] id=4 changed=8"),
                "fill detail+changed count: " + fillDetail);
        FakeWorld fw3 = new FakeWorld();
        fw3.setBlockResult = false; // 同 id 短路（Chunk.setTile 语义）
        boolean noChange = false;
        try {
            com.mcworldagent.agent.WorldOps.fill(fw3, 0, 70, 0, 1, 71, 1, 4);
        } catch (com.mcworldagent.agent.WorldOps.NoChange e) {
            noChange = e.getMessage().contains("changed=0");
        }
        ok(noChange, "fill zero-change -> NoChange(detail)");

        // getblocks：RLE 单 run + 维度串
        FakeWorld fw4 = new FakeWorld();
        fw4.readId = 4;
        String gb = com.mcworldagent.agent.WorldOps.getblocks(fw4, 0, 70, 0, 1, 71, 1);
        ok(gb.equals("getblocks 2x2x2 rle=4:8"), "getblocks RLE: " + gb);

        // getplayer：全字段快照（含惰性 W）+ no-player
        FakeWorld gw = new FakeWorld();
        FakeEntity ge = new FakeEntity();
        ge.g = 100.5; ge.h = 71.62; ge.i = -99.5;
        ge.j = 0.1; ge.k = -0.2; ge.l = 0.3;
        ge.m = 45f; ge.n = -10f;
        ge.W = 7;
        gw.c = ge;
        String gp = com.mcworldagent.agent.WorldOps.getPlayer(gw);
        ok(gp.contains("x=100.5 y=71.62 z=-99.5") && gp.contains("yaw=45.0 pitch=-10.0")
                && gp.contains("health=7"), "getplayer snapshot: " + gp);
        boolean gpNoPlayer = false;
        try {
            com.mcworldagent.agent.WorldOps.getPlayer(new FakeWorld());
        } catch (com.mcworldagent.agent.WorldOps.Rejected e) {
            gpNoPlayer = "no-player".equals(e.getMessage());
        }
        ok(gpNoPlayer, "getplayer(null player) -> Rejected no-player");

        // ===== meta-tree 批：getmeta/setmeta/genscatter/listops =====
        ok(v.invoke(null, MiniJson.parse(
                "{\"protocolVersion\":1,\"id\":\"m1\",\"op\":\"getmeta\",\"args\":{\"x\":1,\"y\":2,\"z\":3}}")) == null,
                "getmeta passes validate");
        ok(v.invoke(null, MiniJson.parse(
                "{\"protocolVersion\":1,\"id\":\"m2\",\"op\":\"setmeta\",\"args\":{\"x\":1,\"y\":2,\"z\":3,\"meta\":7}}")) == null,
                "setmeta passes validate");
        ok(v.invoke(null, MiniJson.parse(
                "{\"protocolVersion\":1,\"id\":\"m3\",\"op\":\"genscatter\",\"args\":{\"x\":1,\"y\":64,\"z\":1,\"blockId\":37,\"seed\":7}}")) == null,
                "genscatter passes validate");
        ok(v.invoke(null, MiniJson.parse(
                "{\"protocolVersion\":1,\"id\":\"m4\",\"op\":\"listops\"}")) == null,
                "listops passes validate");

        // getmeta 默认 0 + setmeta 往返 + 通知复刻（观察者 a(III)V + e(IIII)V 各一次）
        FakeWorld mw = new FakeWorld();
        FakeObserver obs = new FakeObserver();
        mw.addObs(obs);
        ok(com.mcworldagent.agent.WorldOps.getMeta(mw, 5, 70, 5) == 0, "getMeta default 0");
        String smDetail = com.mcworldagent.agent.WorldOps.setMeta(mw, 5, 70, 5, 3);
        ok("meta 0 -> 3".equals(smDetail), "setMeta old->new: " + smDetail);
        ok(com.mcworldagent.agent.WorldOps.getMeta(mw, 5, 70, 5) == 3, "setMeta roundtrip");
        ok(obs.calls == 1 && mw.light6Calls == 1, "setMeta notify replicated (obs a(III)V + e(IIII)V)");
        boolean metaHi = false, metaLo = false, metaCoord = false;
        try {
            com.mcworldagent.agent.WorldOps.setMeta(mw, 5, 70, 5, 16);
        } catch (com.mcworldagent.agent.WorldOps.Rejected e) {
            metaHi = e.getMessage().startsWith("meta-out-of-range");
        }
        try {
            com.mcworldagent.agent.WorldOps.setMeta(mw, 5, 70, 5, -1);
        } catch (com.mcworldagent.agent.WorldOps.Rejected e) {
            metaLo = true;
        }
        try {
            com.mcworldagent.agent.WorldOps.setMeta(mw, 5, 0, 5, 3);
        } catch (com.mcworldagent.agent.WorldOps.Rejected e) {
            metaCoord = true;
        }
        ok(metaHi && metaLo && metaCoord, "setMeta bounds: meta 0..15 + coords");

        // genscatter：假生成器按真 FQCN net.minecraft.a.a.c.b.b 上测试 classpath，签名 (FakeWorld,Random,III)Z
        FakeWorld sw = new FakeWorld();
        FakeObserver sobs = new FakeObserver();
        sw.addObs(sobs);
        sw.blocks.put("8,66,8", Integer.valueOf(2)); // 盒 [7..11]×[66..68]×[7..11] 内两格草地
        sw.blocks.put("9,66,9", Integer.valueOf(2));
        String sc = com.mcworldagent.agent.WorldOps.genScatter(sw, 37, 1, 64, 1, Long.valueOf(7));
        ok(sc.contains("scattered id=37 changed=2") && sc.contains("seed=7"), "genScatter detail: " + sc);
        ok(Integer.valueOf(37).equals(sw.blocks.get("8,66,8"))
                && Integer.valueOf(37).equals(sw.blocks.get("9,66,9")), "genScatter grass->37");
        ok(sobs.calls == 2 && sw.light6Calls == 2, "genScatter per-changed-cell notify");
        boolean scNo = false;
        try {
            com.mcworldagent.agent.WorldOps.genScatter(new FakeWorld(), 37, 1, 64, 1, null);
        } catch (com.mcworldagent.agent.WorldOps.NoChange e) {
            scNo = e.getMessage().contains("changed=0");
        }
        ok(scNo, "genScatter zero-grass -> NoChange (second producer)");
        boolean scId = false;
        try {
            com.mcworldagent.agent.WorldOps.genScatter(new FakeWorld(), 999, 1, 64, 1, null);
        } catch (com.mcworldagent.agent.WorldOps.Rejected e) {
            scId = true;
        }
        ok(scId, "genScatter id=999 -> Rejected (validateBlockId)");

        // ===== light-look-motion 批：getlight/setlook/setmotion =====
        ok(v.invoke(null, MiniJson.parse(
                "{\"protocolVersion\":1,\"id\":\"l1\",\"op\":\"getlight\",\"args\":{\"x\":5,\"y\":70,\"z\":5}}")) == null,
                "getlight passes validate");
        ok(v.invoke(null, MiniJson.parse(
                "{\"protocolVersion\":1,\"id\":\"l2\",\"op\":\"setlook\",\"args\":{\"yaw\":180,\"pitch\":-45}}")) == null,
                "setlook passes validate");
        ok(v.invoke(null, MiniJson.parse(
                "{\"protocolVersion\":1,\"id\":\"l3\",\"op\":\"setmotion\",\"args\":{\"vx\":0,\"vy\":5000,\"vz\":-1500}}")) == null,
                "setmotion passes validate");

        // getlight：纯读回显 + 坐标校验
        FakeWorld lw = new FakeWorld();
        lw.lightDefault = 15;
        ok(com.mcworldagent.agent.WorldOps.getLightValue(lw, 5, 70, 5) == 15, "getLight default 15");
        lw.lightStore.put("5,60,5", Integer.valueOf(3));
        ok(com.mcworldagent.agent.WorldOps.getLightValue(lw, 5, 60, 5) == 3, "getLight store value 3");
        boolean glCoord = false;
        try {
            com.mcworldagent.agent.WorldOps.getLightValue(lw, 5, 0, 5);
        } catch (com.mcworldagent.agent.WorldOps.Rejected e) {
            glCoord = true;
        }
        ok(glCoord, "getLight y=0 -> Rejected");

        // setlook：直写 m/n + 旧值回显 + 越界拒绝（yaw [0,360) / pitch [-90,90]）+ no-player
        FakeWorld lkw = new FakeWorld();
        FakeEntity ke = new FakeEntity();
        ke.m = 90f;
        ke.n = 10f;
        lkw.c = ke;
        String lookDetail = com.mcworldagent.agent.WorldOps.setLook(lkw, 180, -45);
        ok("look yaw 90.0 -> 180 pitch 10.0 -> -45".equals(lookDetail), "setLook old->new: " + lookDetail);
        ok(ke.m == 180f && ke.n == -45f, "setLook writes m/n");
        boolean yawHi = false, yawLo = false, pitchHi = false, pitchLo = false, lookNo = false;
        try {
            com.mcworldagent.agent.WorldOps.setLook(lkw, 360, 0);
        } catch (com.mcworldagent.agent.WorldOps.Rejected e) {
            yawHi = e.getMessage().startsWith("yaw-out-of-range");
        }
        try {
            com.mcworldagent.agent.WorldOps.setLook(lkw, -1, 0);
        } catch (com.mcworldagent.agent.WorldOps.Rejected e) {
            yawLo = true;
        }
        try {
            com.mcworldagent.agent.WorldOps.setLook(lkw, 0, 91);
        } catch (com.mcworldagent.agent.WorldOps.Rejected e) {
            pitchHi = e.getMessage().startsWith("pitch-out-of-range");
        }
        try {
            com.mcworldagent.agent.WorldOps.setLook(lkw, 0, -91);
        } catch (com.mcworldagent.agent.WorldOps.Rejected e) {
            pitchLo = true;
        }
        try {
            com.mcworldagent.agent.WorldOps.setLook(new FakeWorld(), 0, 0);
        } catch (com.mcworldagent.agent.WorldOps.Rejected e) {
            lookNo = "no-player".equals(e.getMessage());
        }
        ok(yawHi && yawLo && pitchHi && pitchLo && lookNo, "setLook bounds + no-player");
        ok(ke.m == 180f && ke.n == -45f, "setLook rejections do not mutate");
        ok("look yaw 180.0 -> 0 pitch -45.0 -> 90"
                        .equals(com.mcworldagent.agent.WorldOps.setLook(lkw, 0, 90)),
                "setLook edge accept yaw=0 pitch=90");

        // setmotion：千分位转换 j/k=l=v/1000 + 旧值千分位回显 + ±10000 拒绝 + no-player
        FakeWorld mvw = new FakeWorld();
        FakeEntity me = new FakeEntity();
        me.j = 0.5;
        me.k = -0.25;
        me.l = 0.0;
        mvw.c = me;
        String motDetail = com.mcworldagent.agent.WorldOps.setMotion(mvw, 0, 5000, -1500);
        ok("motion x=500 y=-250 z=0 -> x=0 y=5000 z=-1500".equals(motDetail),
                "setMotion old->new thousandths: " + motDetail);
        ok(me.j == 0.0 && me.k == 5.0 && me.l == -1.5, "setMotion writes j/k/l = v/1000");
        boolean mvx = false, mvy = false, mvz = false, motNo = false;
        try {
            com.mcworldagent.agent.WorldOps.setMotion(mvw, 10001, 0, 0);
        } catch (com.mcworldagent.agent.WorldOps.Rejected e) {
            mvx = e.getMessage().startsWith("vx-out-of-range");
        }
        try {
            com.mcworldagent.agent.WorldOps.setMotion(mvw, 0, -10001, 0);
        } catch (com.mcworldagent.agent.WorldOps.Rejected e) {
            mvy = true;
        }
        try {
            com.mcworldagent.agent.WorldOps.setMotion(mvw, 0, 0, 20000);
        } catch (com.mcworldagent.agent.WorldOps.Rejected e) {
            mvz = true;
        }
        try {
            com.mcworldagent.agent.WorldOps.setMotion(new FakeWorld(), 0, 0, 0);
        } catch (com.mcworldagent.agent.WorldOps.Rejected e) {
            motNo = "no-player".equals(e.getMessage());
        }
        ok(mvx && mvy && mvz && motNo, "setMotion bounds +-10000 + no-player");
        ok(me.k == 5.0, "setMotion rejections do not mutate");
        ok(com.mcworldagent.agent.WorldOps.setMotion(mvw, -10000, 10000, 0)
                        .contains("-> x=-10000 y=10000 z=0"),
                "setMotion edge +-10000 accepted");

        // ===== give/inv/sethealth 批（物品栏三 op）=====
        ok(v.invoke(null, MiniJson.parse(
                "{\"protocolVersion\":1,\"id\":\"gi1\",\"op\":\"give\",\"args\":{\"itemId\":5,\"count\":64}}")) == null,
                "give passes validate");
        ok(v.invoke(null, MiniJson.parse(
                "{\"protocolVersion\":1,\"id\":\"gi2\",\"op\":\"give\",\"args\":{\"itemId\":35,\"count\":1,\"dmg\":2}}")) == null,
                "give with dmg passes validate");
        ok(v.invoke(null, MiniJson.parse(
                "{\"protocolVersion\":1,\"id\":\"gi3\",\"op\":\"inv\"}")) == null,
                "inv passes validate");
        ok(v.invoke(null, MiniJson.parse(
                "{\"protocolVersion\":1,\"id\":\"gi4\",\"op\":\"sethealth\",\"args\":{\"health\":10}}")) == null,
                "sethealth passes validate");

        // give 正常：注册表 id=5 放行，入主槽 0（count/id/dmg 对账）
        FakeWorld gvw = new FakeWorld();
        FakeEntity gve = new FakeEntity();
        gvw.c = gve;
        String giveDetail = com.mcworldagent.agent.WorldOps.give(gvw, 5, 64, 0);
        ok("gave 5 x64".equals(giveDetail), "give detail: " + giveDetail);
        ok(gve.b.a[0] != null && gve.b.a[0].a == 64 && gve.b.a[0].c == 5 && gve.b.a[0].d == 0,
                "give writes slot0 (count/id/dmg)");

        // give 合并：同 id 且 dmg==0 装得下→并栈，不新开槽
        FakeWorld gmw = new FakeWorld();
        FakeEntity gme = new FakeEntity();
        gmw.c = gme;
        com.mcworldagent.agent.WorldOps.give(gmw, 5, 32, 0);
        com.mcworldagent.agent.WorldOps.give(gmw, 5, 32, 0);
        ok(gme.b.a[0] != null && gme.b.a[0].a == 64 && gme.b.a[1] == null, "give merges same-id dmg0");

        // give damage 路径：dmg!=0 走 (III)V，不合并、d 字段落槽
        com.mcworldagent.agent.WorldOps.give(gmw, 35, 1, 2);
        ok(gme.b.a[1] != null && gme.b.a[1].c == 35 && gme.b.a[1].d == 2, "give dmg path (III)V");

        // bad-itemid：越界(999)/null 空洞(2)/未注册(0) 全拒（注册表直索引 NPE 防线）
        boolean biHi = false, biHole = false, biZero = false;
        try {
            com.mcworldagent.agent.WorldOps.give(gvw, 999, 1, 0);
        } catch (com.mcworldagent.agent.WorldOps.Rejected e) {
            biHi = e.getMessage().startsWith("bad-itemid");
        }
        try {
            com.mcworldagent.agent.WorldOps.give(gvw, 2, 1, 0);
        } catch (com.mcworldagent.agent.WorldOps.Rejected e) {
            biHole = e.getMessage().startsWith("bad-itemid");
        }
        try {
            com.mcworldagent.agent.WorldOps.give(gvw, 0, 1, 0);
        } catch (com.mcworldagent.agent.WorldOps.Rejected e) {
            biZero = e.getMessage().startsWith("bad-itemid");
        }
        ok(biHi && biHole && biZero, "give bad-itemid: out-of-range/null-hole/unregistered");
        ok(gve.b.a[1] == null, "bad-itemid give does not mutate inventory");

        // count/dmg 边界：count∈[1,64]、dmg∈[0,32767] 越界拒绝
        boolean cntLo = false, cntHi = false, dmgLo = false, dmgHi = false;
        try {
            com.mcworldagent.agent.WorldOps.give(gvw, 5, 0, 0);
        } catch (com.mcworldagent.agent.WorldOps.Rejected e) {
            cntLo = e.getMessage().startsWith("count-out-of-range");
        }
        try {
            com.mcworldagent.agent.WorldOps.give(gvw, 5, 65, 0);
        } catch (com.mcworldagent.agent.WorldOps.Rejected e) {
            cntHi = true;
        }
        try {
            com.mcworldagent.agent.WorldOps.give(gvw, 5, 1, -1);
        } catch (com.mcworldagent.agent.WorldOps.Rejected e) {
            dmgLo = e.getMessage().startsWith("dmg-out-of-range");
        }
        try {
            com.mcworldagent.agent.WorldOps.give(gvw, 5, 1, 32768);
        } catch (com.mcworldagent.agent.WorldOps.Rejected e) {
            dmgHi = true;
        }
        ok(cntLo && cntHi && dmgLo && dmgHi, "give count/dmg bounds rejected");

        // inventory-full：36 槽占满（dmg!=0 防合并）→addStack false→Rejected，原栈未消耗
        FakeWorld ifw = new FakeWorld();
        FakeEntity ife = new FakeEntity();
        for (int i = 0; i < 36; i++) {
            ife.b.a[i] = new FakeStack(1, 1, i + 1);
        }
        ifw.c = ife;
        boolean invFull = false;
        try {
            com.mcworldagent.agent.WorldOps.give(ifw, 5, 1, 0);
        } catch (com.mcworldagent.agent.WorldOps.Rejected e) {
            invFull = "inventory-full".equals(e.getMessage());
        }
        ok(invFull, "give full inventory -> Rejected inventory-full");
        boolean giveNoPlayer = false;
        try {
            com.mcworldagent.agent.WorldOps.give(new FakeWorld(), 5, 1, 0);
        } catch (com.mcworldagent.agent.WorldOps.Rejected e) {
            giveNoPlayer = "no-player".equals(e.getMessage());
        }
        ok(giveNoPlayer, "give(null player) -> Rejected no-player");

        // inv：空包→empty；混合槽对账（主槽/伤害/盔甲/cur）
        FakeWorld evw = new FakeWorld();
        evw.c = new FakeEntity();
        ok("empty".equals(com.mcworldagent.agent.WorldOps.inv(evw)), "inv empty -> 'empty'");
        FakeWorld ivw = new FakeWorld();
        FakeEntity ie = new FakeEntity();
        ie.b.a[0] = new FakeStack(5, 64);
        ie.b.a[3] = new FakeStack(35, 1, 2);
        ie.b.b[0] = new FakeStack(298, 1);
        ie.b.c = 2;
        ivw.c = ie;
        ok("0=64x5 3=1x35 d2 A0=1x298 cur=2".equals(com.mcworldagent.agent.WorldOps.inv(ivw)),
                "inv mixed slots: " + com.mcworldagent.agent.WorldOps.inv(ivw));

        // sethealth：旧值回显 + 直写 W + 0/20 边界通过 + -1/21 拒绝不截断 + no-player
        FakeWorld hw = new FakeWorld();
        FakeEntity he = new FakeEntity();
        he.W = 10;
        hw.c = he;
        ok("health 10 -> 20".equals(com.mcworldagent.agent.WorldOps.setHealth(hw, 20))
                && he.W == 20, "setHealth old->new + writes W");
        ok("health 20 -> 0".equals(com.mcworldagent.agent.WorldOps.setHealth(hw, 0))
                && he.W == 0, "setHealth edge 0 accepted (death flow takeover, vanilla)");
        boolean hLo = false, hHi = false, hNo = false;
        try {
            com.mcworldagent.agent.WorldOps.setHealth(hw, -1);
        } catch (com.mcworldagent.agent.WorldOps.Rejected e) {
            hLo = e.getMessage().startsWith("health-out-of-range");
        }
        try {
            com.mcworldagent.agent.WorldOps.setHealth(hw, 21);
        } catch (com.mcworldagent.agent.WorldOps.Rejected e) {
            hHi = true;
        }
        try {
            com.mcworldagent.agent.WorldOps.setHealth(new FakeWorld(), 10);
        } catch (com.mcworldagent.agent.WorldOps.Rejected e) {
            hNo = "no-player".equals(e.getMessage());
        }
        ok(hLo && hHi && hNo, "setHealth bounds 0..20 rejected + no-player");
        ok(he.W == 0, "setHealth rejections do not mutate W");

        // ===== spawn-explode-drop 批（实体面三 op）=====
        ok(v.invoke(null, MiniJson.parse(
                "{\"protocolVersion\":1,\"id\":\"se1\",\"op\":\"explode\",\"args\":{\"x\":0,\"y\":64,\"z\":0,\"size\":4}}")) == null,
                "explode passes validate");
        ok(v.invoke(null, MiniJson.parse(
                "{\"protocolVersion\":1,\"id\":\"se2\",\"op\":\"spawn\",\"args\":{\"kind\":\"pig\",\"x\":1,\"y\":64,\"z\":2}}")) == null,
                "spawn passes validate");
        ok(v.invoke(null, MiniJson.parse(
                "{\"protocolVersion\":1,\"id\":\"se3\",\"op\":\"drop\",\"args\":{\"x\":1,\"y\":64,\"z\":2,\"itemId\":55,\"count\":3}}")) == null,
                "drop passes validate");

        FakeWorld sedw2 = new FakeWorld();
        sedw2.c = new FakeEntity();
        // explode：null-source + 参数记录对账 + size 边界 0/10 通过、-1/11 拒绝
        com.mcworldagent.agent.WorldOps.explode(sedw2, 10, 64, 20, 4);
        ok(sedw2.lastExplosionSource == null && sedw2.lastExplosion[0] == 10.0
                && sedw2.lastExplosion[1] == 64.0 && sedw2.lastExplosionSize == 4f,
                "explode null-source + args recorded");
        com.mcworldagent.agent.WorldOps.explode(sedw2, 0, 0, 0, 0);
        com.mcworldagent.agent.WorldOps.explode(sedw2, 0, 0, 0, 10);
        ok(sedw2.lastExplosionSize == 10f, "explode size 0/10 accepted");
        boolean eHi = false, eLo = false;
        try {
            com.mcworldagent.agent.WorldOps.explode(sedw2, 0, 0, 0, 11);
        } catch (com.mcworldagent.agent.WorldOps.Rejected e) {
            eHi = e.getMessage().startsWith("size-out-of-range");
        }
        try {
            com.mcworldagent.agent.WorldOps.explode(sedw2, 0, 0, 0, -1);
        } catch (com.mcworldagent.agent.WorldOps.Rejected e) {
            eLo = true;
        }
        ok(eHi && eLo, "explode size bounds rejected");

        // spawn 七 kind 全放行：假类真 FQCN 构造→setPosRot 落位（g=x+0.5、h=y+1.62 眼偏移、i=z+0.5）→世界表
        String[] kinds = { "pig", "sheep", "creeper", "skeleton", "zombie", "spider", "tnt" };
        boolean allSpawn = true;
        for (int i2 = 0; i2 < kinds.length; i2++) {
            String d2 = com.mcworldagent.agent.WorldOps.spawn(sedw2, kinds[i2], 5, 64, 7);
            FakeEntity ent = (FakeEntity) sedw2.spawnedEntities.get(i2);
            if (!("spawned " + kinds[i2] + " at 5,64,7").equals(d2)
                    || ent.g != 5.5 || Math.abs(ent.h - (64 + 1.62)) >= 1e-5 || ent.i != 7.5 || ent.world != sedw2) {
                allSpawn = false;
            }
        }
        ok(allSpawn && sedw2.spawnedEntities.size() == 7, "spawn 7 kinds: ctor+setPosRot+list（h 含眼偏移）");
        boolean badKind = false;
        try {
            com.mcworldagent.agent.WorldOps.spawn(sedw2, "cow", 0, 64, 0);
        } catch (com.mcworldagent.agent.WorldOps.Rejected e) {
            badKind = e.getMessage().startsWith("bad-kind");
        }
        ok(badKind && sedw2.spawnedEntities.size() == 7, "spawn bad-kind(cow) rejected, no spawn");
        // giant 复活（NEW_CTOR 补全首例，giant-1 批）：白名单+假类 (FakeWorld)V
        ok("spawned giant at 5,64,7".equals(com.mcworldagent.agent.WorldOps.spawn(sedw2, "giant", 5, 64, 7))
                && sedw2.spawnedEntities.size() == 8
                && sedw2.spawnedEntities.get(7) instanceof net.minecraft.a.c.a.a,
                "spawn giant accepted (resurrected kind)");

        // drop：EntityItem 收到构造出的 ItemStack（id/count 对账）+ 落位 + 世界表
        String dd = com.mcworldagent.agent.WorldOps.drop(sedw2, 5, 3, 0, 1, 64, 2);
        FakeEntity item = (FakeEntity) sedw2.spawnedEntities.get(8);
        ok(("dropped 5 x3 at 1,64,2").equals(dd) && sedw2.spawnedEntities.size() == 9
                && item instanceof net.minecraft.a.c.c.b && ((net.minecraft.a.c.c.b) item).stack.c == 5
                && ((net.minecraft.a.c.c.b) item).stack.a == 3 && item.g == 1.5 && item.h == 64.0,
                "drop -> EntityItem(stack,pos,list)");
        // dmg 路径复用（(III)V 构造）
        com.mcworldagent.agent.WorldOps.drop(sedw2, 17, 1, 10, 0, 64, 0);
        ok(((net.minecraft.a.c.c.b) sedw2.spawnedEntities.get(9)).stack.d == 10, "drop dmg path (III)V");
        boolean dBad = false, dCnt = false, dNoP = false;
        try {
            com.mcworldagent.agent.WorldOps.drop(sedw2, 999, 1, 0, 0, 64, 0);
        } catch (com.mcworldagent.agent.WorldOps.Rejected e) {
            dBad = e.getMessage().startsWith("bad-itemid");
        }
        try {
            com.mcworldagent.agent.WorldOps.drop(sedw2, 5, 0, 0, 0, 64, 0);
        } catch (com.mcworldagent.agent.WorldOps.Rejected e) {
            dCnt = true;
        }
        try {
            com.mcworldagent.agent.WorldOps.drop(new FakeWorld(), 5, 1, 0, 0, 64, 0);
        } catch (com.mcworldagent.agent.WorldOps.Rejected e) {
            dNoP = "no-player".equals(e.getMessage());
        }
        ok(dBad && dCnt && dNoP && sedw2.spawnedEntities.size() == 10,
                "drop bad-itemid/count/no-player rejected, no spawn");

        // ===== invulnerable（i-frame 写，sed-1 批前批准）=====
        ok(v.invoke(null, MiniJson.parse(
                "{\"protocolVersion\":1,\"id\":\"iv01\",\"op\":\"invulnerable\",\"args\":{\"ticks\":20000}}")) == null,
                "invulnerable passes validate");
        FakeEntity ive = new FakeEntity();
        FakeWorld ivw2 = new FakeWorld();
        ivw2.c = ive;
        ok("iframes 0 -> 20000".equals(com.mcworldagent.agent.WorldOps.invulnerable(ivw2, 20000))
                && ive.K == 20000, "invulnerable old->new echo + writes K");
        ok("iframes 20000 -> 0".equals(com.mcworldagent.agent.WorldOps.invulnerable(ivw2, 0))
                && ive.K == 0, "invulnerable 0 cancels");
        boolean iLo = false, iHi = false, iNo = false;
        try {
            com.mcworldagent.agent.WorldOps.invulnerable(ivw2, -1);
        } catch (com.mcworldagent.agent.WorldOps.Rejected e) {
            iLo = e.getMessage().startsWith("ticks-out-of-range");
        }
        try {
            com.mcworldagent.agent.WorldOps.invulnerable(ivw2, 32768);
        } catch (com.mcworldagent.agent.WorldOps.Rejected e) {
            iHi = true;
        }
        try {
            com.mcworldagent.agent.WorldOps.invulnerable(new FakeWorld(), 100);
        } catch (com.mcworldagent.agent.WorldOps.Rejected e) {
            iNo = "no-player".equals(e.getMessage());
        }
        ok(iLo && iHi && iNo && ive.K == 0, "invulnerable bounds + no-player rejected, K unchanged");

        // ===== tileentity 批（getchest/setchest/getfurnace）=====
        ok(v.invoke(null, MiniJson.parse(
                "{\"protocolVersion\":1,\"id\":\"te1\",\"op\":\"getchest\",\"args\":{\"x\":1,\"y\":2,\"z\":3}}")) == null,
                "getchest passes validate");
        ok(v.invoke(null, MiniJson.parse(
                "{\"protocolVersion\":1,\"id\":\"te2\",\"op\":\"setchest\",\"args\":{\"x\":1,\"y\":2,\"z\":3,\"slot\":0,\"itemId\":5,\"count\":3}}")) == null,
                "setchest passes validate");
        ok(v.invoke(null, MiniJson.parse(
                "{\"protocolVersion\":1,\"id\":\"te3\",\"op\":\"setchest\",\"args\":{\"x\":1,\"y\":2,\"z\":3,\"slot\":0,\"clear\":true}}")) == null,
                "setchest clear=true passes validate");
        ok(v.invoke(null, MiniJson.parse(
                "{\"protocolVersion\":1,\"id\":\"te4\",\"op\":\"getfurnace\",\"args\":{\"x\":4,\"y\":5,\"z\":6}}")) == null,
                "getfurnace passes validate");

        FakeWorld tew = new FakeWorld();
        tew.c = new FakeEntity();
        boolean noTe = false;
        try {
            com.mcworldagent.agent.WorldOps.getInv(tew, 0, 0, 0);
        } catch (com.mcworldagent.agent.WorldOps.Rejected e) {
            noTe = "no-tileentity".equals(e.getMessage());
        }
        ok(noTe, "getchest(null TE) -> Rejected no-tileentity");
        boolean teNull = false;
        tew.blocks.put("7,8,9", Integer.valueOf(1)); // 石方块但无 TE→h(III) 回 null 分支
        try {
            com.mcworldagent.agent.WorldOps.getInv(tew, 7, 8, 9);
        } catch (com.mcworldagent.agent.WorldOps.Rejected e) {
            teNull = "no-tileentity".equals(e.getMessage());
        }
        ok(teNull, "getchest(non-air no TE, null return) -> Rejected no-tileentity");

        net.minecraft.a.a.b.a.c chest = new net.minecraft.a.a.b.a.c();
        tew.teStore.put("1,2,3", chest);
        ok("slots=27 empty".equals(com.mcworldagent.agent.WorldOps.getInv(tew, 1, 2, 3)),
                "getchest empty chest");
        String sd = com.mcworldagent.agent.WorldOps.setInv(tew, 1, 2, 3, 0, 5, 3, 0, false);
        ok("setchest 1,2,3 slot=0 <- 5 x3".equals(sd) && chest.items[0] != null
                && chest.items[0].c == 5 && chest.items[0].a == 3, "setchest writes slot0");
        ok("slots=27 0=3x5".equals(com.mcworldagent.agent.WorldOps.getInv(tew, 1, 2, 3)),
                "getchest reads back");
        // 直替换无合并（同槽覆盖）+ dmg 面
        com.mcworldagent.agent.WorldOps.setInv(tew, 1, 2, 3, 0, 35, 1, 2, false);
        ok(chest.items[0].c == 35 && chest.items[0].d == 2, "setchest replaces (no merge)");
        boolean slotBad = false, teBadId = false;
        try {
            com.mcworldagent.agent.WorldOps.setInv(tew, 1, 2, 3, 27, 5, 1, 0, false);
        } catch (com.mcworldagent.agent.WorldOps.Rejected e) {
            slotBad = e.getMessage().startsWith("slot-out-of-range");
        }
        try {
            com.mcworldagent.agent.WorldOps.setInv(tew, 1, 2, 3, 0, 999, 1, 0, false);
        } catch (com.mcworldagent.agent.WorldOps.Rejected e) {
            teBadId = e.getMessage().startsWith("bad-itemid");
        }
        ok(slotBad && teBadId && chest.items[0].c == 35, "setchest slot/bad-itemid rejected, no mutate");
        ok("setchest 1,2,3 slot=0 cleared".equals(
                com.mcworldagent.agent.WorldOps.setInv(tew, 1, 2, 3, 0, 0, 0, 0, true))
                && chest.items[0] == null, "setchest clear=true nulls slot");

        // furnace：not-furnace 判（chest 无 f/h）+ burn/cook 读出（private getDeclaredField）
        boolean notFur = false;
        try {
            com.mcworldagent.agent.WorldOps.getFurnace(tew, 1, 2, 3);
        } catch (com.mcworldagent.agent.WorldOps.Rejected e) {
            notFur = "not-furnace".equals(e.getMessage());
        }
        ok(notFur, "getfurnace on chest -> Rejected not-furnace");
        net.minecraft.a.a.b.a.b fur = new net.minecraft.a.a.b.a.b();
        fur.setTimers(300, 42);
        tew.teStore.put("4,5,6", fur);
        com.mcworldagent.agent.WorldOps.setInv(tew, 4, 5, 6, 1, 5, 1, 0, false);
        ok("furnace burn=300 cook=42 burning=true 1=1x5".equals(
                com.mcworldagent.agent.WorldOps.getFurnace(tew, 4, 5, 6)),
                "getfurnace private f/h + slots");
        ok("slots=3 1=1x5".equals(com.mcworldagent.agent.WorldOps.getInv(tew, 4, 5, 6)),
                "getchest works on furnace (IInventory 通用面)");

        // ===== 零取证五 op 批 =====
        ok(v.invoke(null, MiniJson.parse(
                "{\"protocolVersion\":1,\"id\":\"oz1\",\"op\":\"entitylist\"}")) == null,
                "entitylist passes validate");
        ok(v.invoke(null, MiniJson.parse(
                "{\"protocolVersion\":1,\"id\":\"oz2\",\"op\":\"playsound\",\"args\":{\"x\":1,\"y\":2,\"z\":3,\"name\":\"random.explode\",\"vol\":4,\"pitch\":10}}")) == null,
                "playsound passes validate");
        ok(v.invoke(null, MiniJson.parse(
                "{\"protocolVersion\":1,\"id\":\"oz3\",\"op\":\"setarmor\",\"args\":{\"slot\":0,\"itemId\":298,\"count\":1}}")) == null,
                "setarmor passes validate");
        ok(v.invoke(null, MiniJson.parse(
                "{\"protocolVersion\":1,\"id\":\"oz4\",\"op\":\"clearinv\"}")) == null,
                "clearinv passes validate");
        ok(v.invoke(null, MiniJson.parse(
                "{\"protocolVersion\":1,\"id\":\"oz5\",\"op\":\"replace\",\"args\":{\"x1\":0,\"y1\":64,\"z1\":0,\"x2\":0,\"y2\":64,\"z2\":2,\"oldId\":1,\"newId\":4}}")) == null,
                "replace passes validate");

        // entitylist：空→none；混合=kind/坐标/health 省略分支（Painting 无 W）
        FakeWorld ow = new FakeWorld();
        ow.c = new FakeEntity();
        ok("none".equals(com.mcworldagent.agent.WorldOps.entityList(ow)), "entitylist empty -> none");
        FakeEntity oe1 = new FakeEntity();
        oe1.g = 1.25;
        oe1.h = 64.62;
        oe1.i = 2.0;
        oe1.W = 7;
        ow.spawnedEntities.add(oe1);
        ow.spawnedEntities.add(new net.minecraft.a.c.b.c(ow));      // pig: q()="Pig", W=10
        ow.spawnedEntities.add(new net.minecraft.a.c.d(3, 4, 5));   // painting: 无 W
        ok("N=3=Entity@1.3,64.6,2.0[h=7] Pig@0.0,0.0,0.0[h=10] Painting@3.0,4.0,5.0".equals(
                com.mcworldagent.agent.WorldOps.entityList(ow)),
                "entitylist mixed: " + com.mcworldagent.agent.WorldOps.entityList(ow));

        // playsound：参数记录（十分位换算）+vol/pitch/name 越界拒绝
        String psd = com.mcworldagent.agent.WorldOps.playSound(ow, 1, 2, 3, "random.explode", 4, 10);
        ok("played random.explode at 1,2,3 vol=4 pitch=10".equals(psd)
                && "random.explode".equals(ow.lastSoundName) && ow.lastSoundVol == 0.4f
                && ow.lastSoundPitch == 1.0f, "playsound record + tenth conversion");
        boolean volBad = false, pitBad = false, nameBad = false;
        try {
            com.mcworldagent.agent.WorldOps.playSound(ow, 0, 0, 0, "x", 11, 10);
        } catch (com.mcworldagent.agent.WorldOps.Rejected e) {
            volBad = e.getMessage().startsWith("vol-out-of-range");
        }
        try {
            com.mcworldagent.agent.WorldOps.playSound(ow, 0, 0, 0, "x", 5, 21);
        } catch (com.mcworldagent.agent.WorldOps.Rejected e) {
            pitBad = true;
        }
        try {
            com.mcworldagent.agent.WorldOps.playSound(ow, 0, 0, 0, "", 5, 10);
        } catch (com.mcworldagent.agent.WorldOps.Rejected e) {
            nameBad = "bad-name".equals(e.getMessage());
        }
        ok(volBad && pitBad && nameBad, "playsound vol/pitch/name rejected");

        // setarmor：b[4] 直写 + inv A0 回读 + slot/bad-itemid 拒绝
        FakeWorld aw = new FakeWorld();
        FakeEntity ae = new FakeEntity();
        aw.c = ae;
        ok("setarmor slot=0 <- 35 x1".equals(com.mcworldagent.agent.WorldOps.setArmor(aw, 0, 35, 1, 0))
                && ae.b.b[0] != null && ae.b.b[0].c == 35, "setarmor writes armor slot0");
        ok("A0=1x35 cur=0".equals(com.mcworldagent.agent.WorldOps.inv(aw)), "inv reads back armor A0");
        boolean asBad = false, aiBad = false;
        try {
            com.mcworldagent.agent.WorldOps.setArmor(aw, 4, 35, 1, 0);
        } catch (com.mcworldagent.agent.WorldOps.Rejected e) {
            asBad = e.getMessage().startsWith("slot-out-of-range");
        }
        try {
            com.mcworldagent.agent.WorldOps.setArmor(aw, 1, 999, 1, 0);
        } catch (com.mcworldagent.agent.WorldOps.Rejected e) {
            aiBad = e.getMessage().startsWith("bad-itemid");
        }
        ok(asBad && aiBad && ae.b.b[1] == null, "setarmor slot/itemid rejected, no mutate");

        // clearinv：全清 + inv empty
        ok("cleared 36+4".equals(com.mcworldagent.agent.WorldOps.clearInv(aw))
                && "empty".equals(com.mcworldagent.agent.WorldOps.inv(aw)), "clearinv wipes 36+4");

        // replace：条件替换计数 + same-id 拒绝 + 零变更 NoChange
        FakeWorld rw = new FakeWorld();
        rw.c = new FakeEntity();
        rw.blocks.put("0,64,0", Integer.valueOf(1));
        rw.blocks.put("0,64,1", Integer.valueOf(1));
        rw.blocks.put("0,64,2", Integer.valueOf(5));
        ok("replaced [0..0] [64..64] [0..2] 1->4 changed=2".equals(
                com.mcworldagent.agent.WorldOps.replace(rw, 0, 64, 0, 0, 64, 2, 1, 4)),
                "replace counts only matching oldId");
        boolean sameId = false;
        try {
            com.mcworldagent.agent.WorldOps.replace(rw, 0, 64, 0, 0, 64, 2, 1, 1);
        } catch (com.mcworldagent.agent.WorldOps.Rejected e) {
            sameId = "same-id: 1".equals(e.getMessage());
        }
        boolean repNoChange = false;
        try {
            com.mcworldagent.agent.WorldOps.replace(rw, 0, 64, 0, 0, 64, 2, 8, 4);
        } catch (com.mcworldagent.agent.WorldOps.NoChange nc) {
            repNoChange = nc.getMessage().endsWith("changed=0");
        }
        ok(sameId && repNoChange, "replace same-id + zero-change NoChange rejected");

        // ===== grow/clear 批（grow-clear spec §3） =====
        ok(v.invoke(null, MiniJson.parse(
                "{\"protocolVersion\":1,\"id\":\"gc1\",\"op\":\"grow\",\"args\":{\"x\":5,\"y\":64,\"z\":5}}")) == null,
                "grow passes validate");
        ok(v.invoke(null, MiniJson.parse(
                "{\"protocolVersion\":1,\"id\":\"gc2\",\"op\":\"clear\"}")) == null,
                "clear bare passes validate");
        ok(v.invoke(null, MiniJson.parse(
                "{\"protocolVersion\":1,\"id\":\"gc3\",\"op\":\"clear\",\"args\":{\"x\":10,\"y\":64,\"z\":10,\"r\":4}}")) == null,
                "clear radius passes validate");
        ok("unknown op 'growx'".equals(v.invoke(null, MiniJson.parse(
                "{\"protocolVersion\":1,\"id\":\"gc4\",\"op\":\"growx\"}"))),
                "growx still unknown op");

        // resolve 主段已在 L313 反射跑过（getMeta/setMetaSilent/light6/observers 静态已填）
        FakeWorld gww = new FakeWorld();
        gww.c = new FakeEntity();
        gww.blocks.put("5,64,5", Integer.valueOf(59));
        gww.metaStore.put("5,64,5", Integer.valueOf(0));
        ok("grown 59 meta 0->7".equals(com.mcworldagent.agent.WorldOps.growCrops(gww, 5, 64, 5))
                && gww.metaStore.get("5,64,5").intValue() == 7 && gww.light6Calls == 1,
                "grow crops OK + meta7 + notify");
        boolean growNoChange = false, growUnsupported = false, growRejected = false;
        try {
            com.mcworldagent.agent.WorldOps.growCrops(gww, 5, 64, 5);
        } catch (com.mcworldagent.agent.WorldOps.NoChange nc) {
            growNoChange = "crops already ripe meta=7 at 5,64,5".equals(nc.getMessage());
        }
        gww.blocks.put("6,64,6", Integer.valueOf(6));
        try {
            com.mcworldagent.agent.WorldOps.growCrops(gww, 6, 64, 6);
        } catch (com.mcworldagent.agent.WorldOps.UnsupportedOp u) {
            growUnsupported = "sapling->tree not a inf-20100327 capability (introduced Beta 1.2)"
                    .equals(u.getMessage());
        }
        gww.blocks.put("7,64,7", Integer.valueOf(1));
        try {
            com.mcworldagent.agent.WorldOps.growCrops(gww, 7, 64, 7);
        } catch (com.mcworldagent.agent.WorldOps.Rejected rj) {
            growRejected = "not growable: id=1".equals(rj.getMessage());
        }
        ok(growNoChange && growUnsupported && growRejected,
                "grow tri-state: NO_CHANGE / sapling UNSUPPORTED / not-growable REJECTED");

        // clear：全清只杀 EntityItem（mob/玩家不动）；计数串
        FakeWorld cw = new FakeWorld();
        cw.c = new FakeEntity();
        net.minecraft.a.c.c.b ci1 = new net.minecraft.a.c.c.b(cw, 1.5, 64.0, 2.5, new FakeStack(5, 1));
        net.minecraft.a.c.c.b ci2 = new net.minecraft.a.c.c.b(cw, 8.5, 64.0, 8.5, new FakeStack(264, 1));
        net.minecraft.a.c.b.c cpig = new net.minecraft.a.c.b.c(cw);
        cw.spawnedEntities.add(ci1);
        cw.spawnedEntities.add(ci2);
        cw.spawnedEntities.add(cpig);
        ok("cleared 2 ground items".equals(
                com.mcworldagent.agent.WorldOps.clearGroundItems(cw, null, null, null, null))
                && ci1.dead && ci2.dead && !cpig.dead && !cw.c.dead,
                "clear all: EntityItem-only, mob/player untouched");
        // 半径圆柱过滤（格中心距，同 drop 语义）：水平 3.5>2 与垂直 3>2 皆存活
        FakeWorld rw2 = new FakeWorld();
        rw2.c = new FakeEntity();
        net.minecraft.a.c.c.b ri1 = new net.minecraft.a.c.c.b(rw2, 10.5, 64.0, 10.5, new FakeStack(5, 1));
        net.minecraft.a.c.c.b ri2 = new net.minecraft.a.c.c.b(rw2, 14.0, 64.0, 10.5, new FakeStack(5, 1));
        net.minecraft.a.c.c.b ri3 = new net.minecraft.a.c.c.b(rw2, 10.5, 67.0, 10.5, new FakeStack(5, 1));
        rw2.spawnedEntities.add(ri1);
        rw2.spawnedEntities.add(ri2);
        rw2.spawnedEntities.add(ri3);
        ok("cleared 1 ground items within r=2 of 10,64,10".equals(
                com.mcworldagent.agent.WorldOps.clearGroundItems(rw2,
                        Integer.valueOf(10), Integer.valueOf(64), Integer.valueOf(10), Integer.valueOf(2)))
                && ri1.dead && !ri2.dead && !ri3.dead,
                "clear radius filter (horiz+vert)");
        boolean clearNoChange = false, clearRBad = false, clearPartial = false, clearYBad = false;
        try {
            com.mcworldagent.agent.WorldOps.clearGroundItems(new FakeWorld(), null, null, null, null);
        } catch (com.mcworldagent.agent.WorldOps.NoChange nc) {
            clearNoChange = "cleared 0".equals(nc.getMessage());
        }
        try {
            com.mcworldagent.agent.WorldOps.clearGroundItems(cw,
                    Integer.valueOf(10), Integer.valueOf(64), Integer.valueOf(10), Integer.valueOf(65));
        } catch (com.mcworldagent.agent.WorldOps.Rejected rj) {
            clearRBad = rj.getMessage().startsWith("r-out-of-range");
        }
        try {
            com.mcworldagent.agent.WorldOps.clearGroundItems(cw,
                    Integer.valueOf(10), null, null, null);
        } catch (com.mcworldagent.agent.WorldOps.Rejected rj) {
            clearPartial = "x/y/z/r must come together".equals(rj.getMessage());
        }
        try {
            com.mcworldagent.agent.WorldOps.clearGroundItems(cw,
                    Integer.valueOf(10), Integer.valueOf(0), Integer.valueOf(10), Integer.valueOf(4));
        } catch (com.mcworldagent.agent.WorldOps.Rejected rj) {
            clearYBad = rj.getMessage().startsWith("y out of range");
        }
        ok(clearNoChange && clearRBad && clearPartial && clearYBad,
                "clear NO_CHANGE + r/coords partial rejected");

        // HUD 面（hud-overlay）：agentArgs 无 -javaagent 态=none（织入链由实跑 hud-1 验收，
        // ASM EXIT 注入无头不可测=RUN_TICK 先例）。
        java.lang.reflect.Method agentM = com.mcworldagent.agent.MCAHooks.class
                .getDeclaredMethod("agentArgs");
        agentM.setAccessible(true);
        java.util.List<?> ags = (java.util.List<?>) agentM.invoke(null);
        ok(ags.size() == 1 && "-javaagent: none".equals(ags.get(0)),
                "agentArgs no-agent -> none list");

        // shortAgent（HUD 显示压缩，hud-1 用户判决=头行+列表结构）三态：
        // none 态剥前缀留描述；agent 项剥 '=参数' 尾+取 jar 文件名。
        java.lang.reflect.Method saM = com.mcworldagent.agent.MCAHooks.class
                .getDeclaredMethod("shortAgent", String.class);
        saM.setAccessible(true);
        ok("none".equals(saM.invoke(null, "-javaagent: none")),
                "shortAgent none -> bare description");
        ok("mcworldagent-agent.jar".equals(saM.invoke(null,
                        "-javaagent:E:\\4k\\github-projects\\MCWorldAgent\\java\\agent\\out\\mcworldagent-agent.jar=log=x;hooks=y")),
                "shortAgent strips args + path -> jar name");
        ok("legacyfix.jar".equals(saM.invoke(null,
                        "-javaagent:C:/Users/GAME/AppData/Roaming/.attic/legacyfix/legacyfix.jar")),
                "shortAgent forward-slash path");

        // P0/P1 生命周期（无头模拟世界切换）。out=null 时 log 静默，只断言状态。
        java.lang.reflect.Field qf = com.mcworldagent.agent.MCAHooks.class.getDeclaredField("QUEUE");
        qf.setAccessible(true);
        @SuppressWarnings("unchecked")
        java.util.concurrent.ConcurrentLinkedQueue<Runnable> q =
                (java.util.concurrent.ConcurrentLinkedQueue<Runnable>) qf.get(null);

        com.mcworldagent.agent.MCAHooks.capturedWorld = null;
        com.mcworldagent.agent.MCAHooks.capturedWorldGeneration = 1;
        com.mcworldagent.agent.MCAHooks.noFirstBrick = true;
        com.mcworldagent.agent.MCAHooks.onSaveWorld(new Object(), false); // 首次捕获
        ok(q.isEmpty(), "noFirstBrick=true 不提交 FirstBrick");
        ok(com.mcworldagent.agent.MCAHooks.capturedWorldGeneration == 1, "首次捕获世代=1");
        com.mcworldagent.agent.MCAHooks.onSaveWorld(new Object(), false); // 异实例=切换
        ok(com.mcworldagent.agent.MCAHooks.capturedWorldGeneration == 2, "WORLD_SWITCHED 世代+1");
        Object cur = com.mcworldagent.agent.MCAHooks.capturedWorld;
        com.mcworldagent.agent.MCAHooks.onSaveWorld(cur, false); // 同实例不重复计数
        ok(com.mcworldagent.agent.MCAHooks.capturedWorldGeneration == 2, "同实例不增世代");
        com.mcworldagent.agent.MCAHooks.noFirstBrick = false;
        com.mcworldagent.agent.MCAHooks.capturedWorld = null;
        com.mcworldagent.agent.MCAHooks.onSaveWorld(new Object(), false); // 默认行为回归
        ok(q.size() == 1, "noFirstBrick=false 仍自动提交 FirstBrick");
        q.clear();
        com.mcworldagent.agent.MCAHooks.capturedWorld = null;
        com.mcworldagent.agent.MCAHooks.capturedWorldGeneration = 1;
        com.mcworldagent.agent.MCAHooks.noFirstBrick = false;

        System.out.println("TOTAL pass=" + pass + " fail=" + fail);
        if (fail > 0) { System.exit(1); }
    }
}
