// b1.6 World=fb 假件（blocks\fb.txt javap -p 全量 + fb_c.txt 定向 -c）。关键面：
//   d=playerEntities public List（playerListF=d 三版稳定）、b=loadedEntityList、
//   B=public boolean multiplayerWorld（⚠️ 1401=t/1501=v 断代——16 起 B 与 173 同字母；
//   门位三版同构=仅 OK 尾/wake/tick 黎明段，非 173 头部旁路）、q=public int difficulty、
//   t=public final wr provider（门2 链）、x=protected eg WorldInfo、I=private boolean
//   allPlayersSleeping、**j/l=protected float rainStrength/thunderStrength**（⚠️ 1401 无
//   float 判决①翻面——1501 private C/E→16 protected j/l；fb.E()V 反汇编实锤 eg.o()Z→j/
//   eg.m()Z→l；WorldOps getDeclaredField+setAccessible 同配方消费 protected 面）、
//   x()Leg getWorldInfo（1401=w 断代/1501=x 同字母）、y()V updateAllPlayersSleepingFlag
//   （1401=x 断代/1501=y 同字母）、f()Z isDaytime 三版稳定、
//   a(III)I getBlockId、**f(IIII)Z setBlockWithNotify**（1401/1501=e 断代）、
//   e(III)I getBlockMetadata、**e(IIII)Z setBlockMetadata**（setMetaSilentM——同字母异
//   描述符共存同 1401 形；返回 boolean 1501 V 断代回摆；d(IIII)V=setBlockMetadataWithNotify
//   不消费）、n(III)I getBlockLightValue（1401/1501=l 断代）、b(III)Los teGet、
//   b(Lsi;)Z entityJoinedWorld/e(Lsi;)V setEntityDead/o()List getLoadedEntityList
//   （三件全回摆 1501 字母——1401 a/d/n 断代带止于 1501）、
//   a(ZLxs;)V saveWorld 实体（无 1 参包装断代带延续；xs null 守卫 @14/@30 与 1401 逐位
//   同形实锤；前置 v:Lcj;.b()Z 旗假件不建模；a(Z)Z=TickUpdates 禁钩同形预避）、
//   l()V World.tick（三版同字母；RUN_TICK 调用点 mc_c @1260）、
//   **a()Lxm getWorldChunkManager**（群系链首跳——biomeWcmM=a；⚠️与 a(III)I 同字母共存）。
public class fb {
    public java.util.List b = new java.util.ArrayList();   // loadedEntityList
    public java.util.List d = new java.util.ArrayList();   // playerEntities
    public int q;                                          // difficultySetting
    public final wr t;                                     // provider
    protected eg x;                                        // WorldInfo（x() 返回体）
    public boolean B;                                      // multiplayerWorld（默认 false=单人）
    private boolean I;                                     // allPlayersSleeping
    protected float i;                                     // prevRainingStrength
    protected float j;                                     // rainStrength（fb.E()V 实锤）
    protected float k;                                     // prevThunderingStrength
    protected float l;                                     // thunderStrength（eg.m()Z 驱动）
    // ---- 测试脚手架（非游戏面）----
    public boolean daytime;                                // f()Z 数据源
    public int sleepFlagRefreshes;                         // y()V 计数
    public int saveCalls;                                  // a(ZLxs;)V 计数
    public os teStore;                                     // b(III)Los; 数据源
    public java.util.Map blocks = new java.util.HashMap(); // a/f 方块存储
    public java.util.Map metaStore = new java.util.HashMap();
    public xm wcmScaffold = new xm();                      // a()Lxm; 返回体
    public fb() { t = new wr(); x = new eg(); }
    public eg x() { return x; }                            // getWorldInfoM=x
    public void y() {                                      // updateAllPlayersSleepingFlag
        sleepFlagRefreshes++;
        I = !d.isEmpty();
        for (Object o : d) {
            if (!((gq) o).N()) { I = false; break; }
        }
    }
    public boolean f() { return daytime; }                 // isDaytime（门3 数据源）
    public int a(int x, int y, int z) {
        Integer v = (Integer) blocks.get(x + "," + y + "," + z);
        return v == null ? 0 : v.intValue();
    }
    public boolean f(int x, int y, int z, int id) {        // setBlockM=f (IIII)Z
        blocks.put(x + "," + y + "," + z, Integer.valueOf(id));
        return true;
    }
    public int e(int x, int y, int z) {                    // getMetaM=e (III)I
        Integer v = (Integer) metaStore.get(x + "," + y + "," + z);
        return v == null ? 0 : v.intValue();
    }
    public boolean e(int x, int y, int z, int m2) {        // setMetaSilentM=e (IIII)Z——同字母共存
        metaStore.put(x + "," + y + "," + z, Integer.valueOf(m2));
        return true;
    }
    public int n(int x, int y, int z) { return 15; }       // getLightM=n (III)I
    public os b(int x, int y, int z) { return teStore; }   // teGetM=b (III)Los;
    public void a(boolean p, xs prog) { saveCalls++; }     // saveWorld 实体 (ZLxs;)V
    public xm a() { return wcmScaffold; }                  // biomeWcmM=a 零参
    public java.util.List o() {                            // entityListM=o ()Ljava/util/List;
        java.util.List l2 = new java.util.ArrayList();
        l2.addAll(b);
        l2.addAll(d);
        return l2;
    }
    public boolean b(si e2) { b.add(e2); return true; }    // spawnEntityM=b (Lsi;)Z
    public void e(si e2) { /* setEntityDeadM=e (Lsi;)V——isDead 字母未钉 JSON，假件不建模 */ }
    public void l() { /* World.tick（RUN_TICK 调用点 fb.l()V @1260） */ }
}
