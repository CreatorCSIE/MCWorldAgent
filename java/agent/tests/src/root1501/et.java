// b1.5_01 World=et 假件（dump.txt L1703 javap -p 全量）。关键面：
//   d=playerEntities public List（playerListF=d）、b=loadedEntityList public List、
//   v=public boolean isRemote（OK 尾/wake/tick 黎明段旁路——非 173 fd.B/181 rv.I 头旁路）、
//   l=public int difficultySetting（difficultyF=l）、C/E=private float rain/thunderStrength
//   （dump javap -p：private！WorldOps getDeclaredField+setAccessible 同配方）、
//   o=public final vr provider（门2 链 et.o.vr.c）、w=public dy WorldInfo、
//   x()Ldy getWorldInfo、y()V updateAllPlayersSleepingFlag（public）、f()Z isDaytime、
//   a(III)I getBlockId、e(IIII)Z setBlockWithNotify、e(III)I getBlockMetadata、
//   d(IIII)V setBlockMetadata、l(III)I getBrightness、h(III)Z、b(III)Lnx teGet、
//   b(Lrj;)Z spawn、e(Lrj)V setEntityDead、o()List getLoadedEntityList、
//   a(ZLwr;)V saveWorld 实体（无 1 参 public 包装——W-6 批 resolve() 已补 (ZL...;)V
//   双参分支，save 腿 FakeOps1501Test 断言 (false,null) 配方；本假件仅计数）、
//   l()V World.tick（RUN_TICK note）。
public class et {
    public java.util.List b = new java.util.ArrayList();   // loadedEntityList
    public java.util.List d = new java.util.ArrayList();   // playerEntities（playerListF=d）
    public boolean v;                                      // isRemote（默认 false=单人）
    public int l;                                          // difficultySetting
    private float C;                                       // rainStrength（private！）
    private float E;                                       // thunderStrength（private！）
    public final vr o;                                     // provider（public final vr o）
    public dy w;                                           // WorldInfo（public dy w，x() 返回体）
    // ---- 测试脚手架（非游戏面）----
    public boolean daytime;                                // f()Z 数据源
    public int sleepFlagRefreshes;                         // y()V 计数
    public int saveCalls;                                  // a(ZLwr;)V 计数
    public nx teStore;                                     // b(III)Lnx; 数据源
    public java.util.Map blocks = new java.util.HashMap(); // a/e 方块存储
    public java.util.Map metaStore = new java.util.HashMap();
    public et() { o = new vr(); w = new dy(); }
    public dy x() { return w; }                            // getWorldInfoM=x
    public void y() { sleepFlagRefreshes++; }              // updateAllPlayersSleepingFlag
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
    public void d(int x, int y, int z, int m) {            // setMetaSilentM=d (IIII)V
        metaStore.put(x + "," + y + "," + z, Integer.valueOf(m));
    }
    public int l(int x, int y, int z) { return 15; }       // getLightM=l (III)I
    public boolean h(int x, int y, int z) { return a(x, y, z) != 0; }
    public nx b(int x, int y, int z) { return teStore; }   // teGetM=b (III)Lnx;
    public void a(boolean p, wr prog) { saveCalls++; }     // saveWorld 实体 (ZLwr;)V
    public wl a() { return new wl(); }                     // getWorldChunkManager（biomeWcmM=a 零参）
    public java.util.List o() {                            // entityListM=o ()Ljava/util/List;
        java.util.List l2 = new java.util.ArrayList();
        l2.addAll(b);
        l2.addAll(d);
        return l2;
    }
    public boolean b(rj e2) { b.add(e2); return true; }    // spawnEntityM=b (Lrj;)Z
    public void e(rj e2) { /* setEntityDeadM=e (Lrj)V——isDead 字母未钉 JSON，假件不建模 */ }
    public void l() { /* World.tick（RUN_TICK 调用点 et.l()V） */ }
}
