import java.util.ArrayList;
import java.util.List;

// b1.9-pre2 World=ti 假件（实件 javap 复刻，dump L9054-9255）：
// 写读面 g(IIII)Z=setBlockWithNotify（✓同 181）/ a(III)I=getBlockId /
// e(III)I=getBlockMetadata / c(IIII)Z=setBlockMetadata **静默写主面**
// （⚠️181=f(IIII)V WithNotify——断代翻面，tiny L8293/8400 对榫；
// 同形陷阱群 d(IIII)Z=setBlock / e(IIII)Z=doChunksNearChunkExist /
// f(IIII)V=setBlockMetadataWithNotify 全在位）/ n(III)I=getBrightness /
// a(Lpd;)V saveWorldIndirectly public 包装 + a(ZLpd)V 本体 null 守卫 /
// y()Lsx getWorldInfo（⚠️181=x 断代）/ a()Loz getWorldChunkManager（无参唯一）/
// a(Llh;DDDF)Labf explode / a(Llh;)Z entityJoinedWorld / b(Llh;)V setEntityDead /
// p()List getLoadedEntityList / a(DDD,String,F,F)V playSoundEffect。
// 字段面：i=playerEntities（playerListF ✓同 181）/ g=loadedEntityList /
// v=difficultySetting / o/q=raining/thunderingStrength（真件 protected float，
// 假件 public 便于 getDeclaredField——181 rv 同例）。
public class ti {
    public List g;              // loadedEntityList
    public List i;              // playerEntities
    public int v;               // difficultySetting
    public float o;             // rainingStrength
    public float q;             // thunderingStrength
    public int saveCalls;
    public int levelSaves;
    public int expl;
    public int snd;
    public int killCalls;
    final int[] blocks = new int[4096];
    final int[] meta = new int[4096];
    private final sx info = new sx();   // WorldInfo 承载体（ti.y() 返回）
    private oz wcm;
    public je teStore;                  // getchest/setchest 门先响占位（181 rv 同例）

    public ti() {
        g = new ArrayList();
        i = new ArrayList();
    }
    static int idx(int x, int y, int z) { return (y & 15) * 256 + (x & 15) * 16 + (z & 15); }
    public sx y() { return info; }
    public oz a() { if (wcm == null) { wcm = new oz(this); } return wcm; }
    public int a(int x, int y, int z) { return blocks[idx(x, y, z)]; }
    public boolean d(int x, int y, int z, int id) {   // 裸 setBlock decoy
        int j = idx(x, y, z);
        boolean ch = blocks[j] != id;
        blocks[j] = id;
        return ch;
    }
    public boolean e(int x, int y, int z, int m) { return true; }  // doChunks decoy
    public boolean c(int x, int y, int z, int m) {   // 主写面 setMetaSilent（裸静默）
        meta[idx(x, y, z)] = m;
        return true;
    }
    public void f(int x, int y, int z, int m) { meta[idx(x, y, z)] = m; }  // WithNotify decoy
    public int e(int x, int y, int z) { return meta[idx(x, y, z)]; }
    public boolean g(int x, int y, int z, int id) { return d(x, y, z, id); }  // setBlockWithNotify
    public int n(int x, int y, int z) { return 15; }
    public je b(int x, int y, int z) { return teStore; }  // getBlockTileEntity（门先响面）
    public void a(pd p) { a(true, p); }
    public void a(boolean flag, pd p) {
        // 本体 null 守卫=0629..b1.8.1 配方六连用；pd 方法面未钉（零调用）
        r();
        saveCalls++;
    }
    private void r() { levelSaves++; }
    public abf a(lh e2, double x, double y, double z, float r2) { expl++; return new abf(); }
    public boolean a(lh e2) { g.add(e2); return true; }
    public void b(lh e2) { killCalls++; e2.G = true; }
    public List p() { return g; }
    public void a(double x, double y, double z, String n2, float v1, float v2) { snd++; }
}
