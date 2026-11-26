import java.util.ArrayList;
import java.util.List;

// b1.8.1 World=rv 假件（详表 §1/§2，实件 javap 复刻）：
// 写读面 g(IIII)Z=setBlockWithNotify（⚠️b1.7.3=f；裸 setBlock=d(IIII)Z、
// setBlockMetadata=e(IIII)Z 同形陷阱群）/ a(III)I=getBlockId /
// e(III)I=getBlockMetadata / f(IIII)V=setBlockMetadataWithNotify（⚠️b1.7.3=d）/
// n(III)I=getBrightness / b(III)Lij;=getBlockTileEntity（TE ow→ij 漂移）/
// a(Lny;)V saveWorld 包装 + a(ZLny)V 本体 null 守卫（IProgressUpdate yb→ny）+
// private r()V saveLevel / x()Lrl getWorldInfo / a()Lnu getWorldChunkManager（唯一无参 a）/
// a(Lkj;DDDF)Lzf explode / a(Lkj;)Z entityJoinedWorld（⚠️b1.7.3=b 回漂）/
// b(Lkj;)V setEntityDead（⚠️b1.7.3=e 回漂）/ p()List getLoadedEntityList /
// a(DDD,String,F,F)V playSound / a(Z)Z=TickUpdates 禁钩 decoy / g(III)Z decoy。
// 字段面：v=difficultySetting（⚠️b1.7.3=q）、i=playerEntities（⚠️d）、
// g=loadedEntityList（⚠️b）、c/e List 留档、private long H、I:Z=isRemote（h10 钉，
// 旧留档 long I 系 173 fd 误抄已删）。
public class rv {
    public List g;              // loadedEntityList
    public List c;              // 其余 List 面留档
    public List i;              // playerEntities
    public List e;              // 其余 List 面留档
    public int v;               // difficultySetting
    public float o;             // D12 rainingStrength（真件 protected，假件 public 便于 getDeclaredField）
    public float q;             // D12 thunderingStrength
    private long H;             // 留档 private 面（⚠️ 旧留档 I 删除——真形 rv.I:Z=isRemote，
                                // h10 睡眠门消费面，long 版系 173 fd 误抄）

    public int saveCalls;
    public int levelSaves;
    public int expl;
    public int snd;
    public int killCalls;
    public ij teStore;
    // h9 批 sleep 面（真 jar tiny：rv.y()V=updateAllPlayersSleepingFlag public——
    // h10 复核真形无误，sleepInBedAt/wakeUpPlayer 内部即调此）+ h10 门消费面：
    // I=isRemote（真形 rv.I:Z，173=B 断代）、y:Lacz;=provider 字段（与方法 y()
    // 同字母不同 namespace——真形即如此）、daytime→k()Z（173=f 断代）、
    // d(III)Z=床向修正分支门（173=i 断代）
    public int sleepFlagRefreshes;
    public void y() { sleepFlagRefreshes++; }
    public acz y;
    public boolean I;
    public boolean daytime;
    final int[] blocks = new int[4096];
    final int[] meta = new int[4096];
    private final rl info = new rl();   // WorldInfo 承载体（rv.x() 返回）
    private nu wcm;

    public rv() {
        g = new ArrayList();
        c = new ArrayList();
        i = new ArrayList();
        e = new ArrayList();
    }
    static int idx(int x, int y, int z) { return (y & 15) * 256 + (x & 15) * 16 + (z & 15); }
    public rl x() { return info; }
    public nu a() { if (wcm == null) { wcm = new nu(this); } return wcm; }
    public int a(int x, int y, int z) { return blocks[idx(x, y, z)]; }
    public boolean d(int x, int y, int z, int id) {   // 裸 setBlock decoy
        int j = idx(x, y, z);
        boolean ch = blocks[j] != id;
        blocks[j] = id;
        return ch;
    }
    public boolean e(int x, int y, int z, int m) {    // setBlockMetadata decoy
        meta[idx(x, y, z)] = m;
        return true;
    }
    public boolean g(int x, int y, int z, int id) { return d(x, y, z, id); }  // 主写面
    public int e(int x, int y, int z) { return meta[idx(x, y, z)]; }
    public boolean g(int x, int y, int z) { return meta[idx(x, y, z)] != 0; } // decoy: real rv has g(III)Z
    public void f(int x, int y, int z, int m) { meta[idx(x, y, z)] = m; }
    public int n(int x, int y, int z) { return 15; }
    public boolean k() { return daytime; }              // isDaytime（h10 睡眠门 3，173=f 断代）
    public boolean d(int x, int y, int z) { return blocks[idx(x, y, z)] != 0; } // 173=i 断代
    public ij b(int x, int y, int z) {
        if (teStore == null) { throw new NullPointerException("TE map miss"); }
        return teStore;
    }
    public void a(ny p) { a(true, p); }
    public void a(boolean flag, ny p) {
        // 本体 null 守卫=0629/a1.2.0/b1.2_02/b1.7.3 配方五连用；ny 方法面未钉（零调用）
        r();
        saveCalls++;
    }
    public boolean a(boolean tickUpdates) { return false; }   // TickUpdates 禁钩 decoy
    private void r() { levelSaves++; }
    public zf a(kj e2, double x, double y, double z, float r2) { expl++; return new zf(); }
    public boolean a(kj e2) { g.add(e2); return true; }
    public void b(kj e2) { killCalls++; e2.G = true; }
    public List p() { return g; }
    public void a(double x, double y, double z, String n2, float v1, float v2) { snd++; }
}
