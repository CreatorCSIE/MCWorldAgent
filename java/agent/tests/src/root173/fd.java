import java.util.ArrayList;
import java.util.List;

// b1.7.3 World=fd 假件（详表 §2，javap 全量实锤复刻）：
// 写读面 f(IIII)Z=setBlockWithNotify（⚠️裸 setBlock=c(IIII)Z 同形陷阱群）/
// a(III)I=getBlockId / e(III)I=getBlockMetadata（el173-1 翻案；g(III)Z=decoy）/ d(IIII)V=setBlockMetadataWithNotify /
// n(III)I=getBrightness / b(III)Low=getBlockTileEntity（TE 基类 kp→ow 漂移）/
// a(Lyb;)V saveWorld 包装 + a(ZLyb)V 本体 null 守卫 + private r()V saveLevel /
// x()Lei getWorldInfo（结构断代①：time/seed/spawn 五字段不在 fd）/
// a()Lxv getWorldChunkManager（唯一无参 a）/ a(Lsn;DDDF)Lqx explode（返回值 V→Explosion
// 断代留档——invoke 面不消费返回值）/ b(Lsn;)Z entityJoinedWorld / e(Lsn;)V setEntityDead /
// o()List getLoadedEntityList / a(DDD,String,F,F)V playSound。
// 字段面：q=difficultySetting（⚠️b1.2_02=k）、五 List b/c/d/e（d=playerEntities、
// b=loadedEntityList 实形）、private long H/I（详表 §2 留档面）。
public class fd {
    public List b;              // loadedEntityList
    public List c;              // 其余 List 面留档（b/c/d/e 五连之一）
    public List d;              // playerEntities
    public List e;              // 其余 List 面留档
    public int q;               // difficultySetting
    public float j;             // rainingStrength（D8 weather，verscan b1.7.3 锚）
    public float l;             // thunderingStrength
    private long H;             // 留档 private 面（访问位=钉表实形）
    private long I;

    public int saveCalls;
    public int levelSaves;
    public int expl;
    public int snd;
    public int killCalls;
    public ow teStore;
    // h10 批 sleep 面翻案：真 sleepInBedAt/wakeUpPlayer 内部刷旗标走 **fd.y()V**
    // （gs_disasm L1753/L2066 区段 invokespecial→invokevirtual #217 实锤；h9 旧钉
    // m()V 系误钉——m 疑为 ()Z getter，旧裸写配方"刷新"实为空转）。假件按真形建模 y。
    public int sleepFlagRefreshes;
    public void y() { sleepFlagRefreshes++; }
    // h10 睡眠门消费面：B=isRemote（false=服务端侧，门全生效）、t=WorldProvider
    // （xa.c 禁睡旗标）、daytime→f()Z（isDaytime 门 3）、i(III)Z 床向修正分支门
    public boolean B;
    public xa t;
    public boolean daytime;
    final int[] blocks = new int[4096];
    final int[] meta = new int[4096];
    private final ei info = new ei();   // WorldInfo 承载体（fd.x() 返回）
    private xv wcm;

    public fd() {
        b = new ArrayList();
        c = new ArrayList();
        d = new ArrayList();
        e = new ArrayList();
    }
    static int idx(int x, int y, int z) { return (y & 15) * 256 + (x & 15) * 16 + (z & 15); }
    public ei x() { return info; }
    public xv a() { if (wcm == null) { wcm = new xv(this); } return wcm; }
    public int a(int x, int y, int z) { return blocks[idx(x, y, z)]; }
    public boolean c(int x, int y, int z, int id) {
        int i = idx(x, y, z);
        boolean ch = blocks[i] != id;
        blocks[i] = id;
        return ch;
    }
    public boolean f(int x, int y, int z, int id) { return c(x, y, z, id); }
    public int e(int x, int y, int z) { return meta[idx(x, y, z)]; }
    public boolean g(int x, int y, int z) { return meta[idx(x, y, z)] != 0; } // decoy: real fd has g(III)Z (el173-1 proved NOT metadata)
    public void d(int x, int y, int z, int m) { meta[idx(x, y, z)] = m; }
    public int n(int x, int y, int z) { return 15; }
    public boolean f() { return daytime; }              // isDaytime（h10 睡眠门 3）
    public boolean i(int x, int y, int z) { return blocks[idx(x, y, z)] != 0; } // 床向修正分支门
    public ow b(int x, int y, int z) {
        if (teStore == null) { throw new NullPointerException("TE map miss"); }
        return teStore;
    }
    public void a(yb p) { a(true, p); }
    public void a(boolean flag, yb p) {
        // 本体 null 守卫=0629/a1.2.0/b1.2_02 配方四连用；yb 方法面未钉（假件零调用）
        r();
        saveCalls++;
    }
    private void r() { levelSaves++; }
    public qx a(sn e2, double x, double y, double z, float r2) { expl++; return new qx(); }
    public boolean b(sn e2) { b.add(e2); return true; }
    public void e(sn e2) { killCalls++; e2.be = true; }
    public List o() { return b; }
    public void a(double x, double y, double z, String n2, float v1, float v2) { snd++; }
}
