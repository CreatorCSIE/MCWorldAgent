// b1.4_01 World=eb 假件（blocks\eb.txt javap -p 全量 + eb_c.txt javap -c 定向）。关键面：
//   d=playerEntities public List（playerListF=d）、b=loadedEntityList public List、
//   t=public boolean multiplayerWorld（⚠️ 1401 无 isRemote 字段——1501 et.v 位由此替代，
//   fm_c @313/@183 门控旗标刷新实锤）、j=public int difficulty（difficultyF=j）、
//   m=public final sy provider（门2 链 eb.m.sy.c）、q=protected dj WorldInfo、
//   A=private boolean allPlayersSleeping、w()Ldj; getWorldInfo（getWorldInfoM=w——
//   ⚠️ 1501=x 同形陷阱勿抄）、x()V updateAllPlayersSleepingFlag（eb_c @6560 段实锤：
//   A=!d.isEmpty() 后遍历 fm.M()Z 任一假→A=false；1501=y 断代）、f()Z isDaytime、
//   a(III)I getBlockId、e(IIII)Z setBlockWithNotify、e(III)I getBlockMetadata、
//   d(IIII)Z setBlockMetadata（⚠️ 本代返回 boolean——1501 d(IIII)V 断代；WorldOps
//   getMethod 名+四 int 形解析不依赖返回型，setmeta OK 判决成立）、l(III)I getBrightness、
//   h(III)Z、b(III)Lma; teGet、a(Lpb;)Z entityJoinedWorld（1501=b 断代）、
//   d(Lpb)V setEntityDead（1501=e 断代）、n()List getLoadedEntityList（1501=o 断代）、
//   a(ZLtw;)V saveWorld 实体（无 1 参 public 包装——(Z)Z=a(Z)Z TickUpdates 禁钩同形预避；
//   javap -c tw null 守卫 @14/@30 齐全；本假件仅计数）、l()V World.tick（RUN_TICK 调用点）。
// ⚠️ 判决①负半轴：真 eb 字段面无任何 float（B/C/D/E 雨强四槽零命中）→rainStrF/
//   thunderStrF=null、capWeather UNSUPPORTED——假件严格不建浮值字段。
// ⚠️ 判决②群系下界：真 eb 有 public tq a()（前群系气候 WCM）——getbiome UNSUPPORTED，
//   假件不建 tq 链，该方法省略。
public class eb {
    public java.util.List b = new java.util.ArrayList();   // loadedEntityList
    public java.util.List d = new java.util.ArrayList();   // playerEntities（playerListF=d）
    public int j;                                          // difficultySetting
    public final sy m;                                     // provider（public final sy m）
    protected dj q;                                        // WorldInfo（w() 返回体）
    public boolean t;                                      // multiplayerWorld（默认 false=单人）
    private boolean A;                                     // allPlayersSleeping
    // ---- 测试脚手架（非游戏面）----
    public boolean daytime;                                // f()Z 数据源
    public int sleepFlagRefreshes;                         // x()V 计数
    public int saveCalls;                                  // a(ZLtw;)V 计数
    public ma teStore;                                     // b(III)Lma; 数据源
    public java.util.Map blocks = new java.util.HashMap(); // a/e 方块存储
    public java.util.Map metaStore = new java.util.HashMap();
    public eb() { m = new sy(); q = new dj(); }
    public dj w() { return q; }                            // getWorldInfoM=w
    public void x() {                                      // updateAllPlayersSleepingFlag
        sleepFlagRefreshes++;
        A = !d.isEmpty();
        for (Object o : d) {
            if (!((fm) o).M()) { A = false; break; }
        }
    }
    public boolean f() { return daytime; }                 // isDaytime（门3 数据源）
    public int a(int x, int y, int z) {
        Integer v = (Integer) blocks.get(x + "," + y + "," + z);
        return v == null ? 0 : v.intValue();
    }
    public boolean e(int x, int y, int z, int id) {        // setBlockM=e (IIII)Z
        blocks.put(x + "," + y + "," + z, Integer.valueOf(id));
        return true;
    }
    public int e(int x, int y, int z) {                    // getMetaM=e (III)I——同字母异描述符共存
        Integer v = (Integer) metaStore.get(x + "," + y + "," + z);
        return v == null ? 0 : v.intValue();
    }
    public boolean d(int x, int y, int z, int m2) {        // setMetaSilentM=d (IIII)Z——⚠️返回 boolean
        metaStore.put(x + "," + y + "," + z, Integer.valueOf(m2));
        return true;
    }
    public int l(int x, int y, int z) { return 15; }       // getLightM=l (III)I
    public boolean h(int x, int y, int z) { return a(x, y, z) != 0; }
    public ma b(int x, int y, int z) { return teStore; }   // teGetM=b (III)Lma;
    public void a(boolean p, tw prog) { saveCalls++; }     // saveWorld 实体 (ZLtw;)V
    public java.util.List n() {                            // entityListM=n ()Ljava/util/List;
        java.util.List l2 = new java.util.ArrayList();
        l2.addAll(b);
        l2.addAll(d);
        return l2;
    }
    public boolean a(pb e2) { b.add(e2); return true; }    // spawnEntityM=a (Lpb;)Z
    public void d(pb e2) { /* setEntityDeadM=d (Lpb;)V——isDead 字母未钉 JSON，假件不建模 */ }
    public void l() { /* World.tick（RUN_TICK 调用点 eb.l()V） */ }
}
