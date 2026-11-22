import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

// b1.2_02 World=dn 假件（详表 §2，借 b1.2 tiny 直钉）：字母断代=
// setBlock e(IIII)Z 主写面 / 裸 b(IIII)Z / getMeta e(III)I / setMetaSilent c / getLight l /
// saveWorld a(Lru;)V 包装+a(ZLru)V 本体+private r()V saveLevel / entityList l()List /
// spawn a(Lnl;)Z / kill d(Lnl;)V / explode a(Lnl;DDDF)Lmc;（c11b777 翻案）/ playsound a(DDD,String,FF)V /
// wcm a()Lro;。字段 e:J worldTime、u:J seed、m/n/o spawn、k difficulty、b/d Lists、l Random。
public class dn {
    public List b;              // loadedEntityList
    public List d;              // playerEntities
    public long e;              // worldTime
    public int f;               // skylightSubtracted
    public int j;               // autosavePeriod
    public int k;               // difficultySetting
    public Random l;            // rand
    public int m;               // spawnX
    public int n;               // spawnY
    public int o;               // spawnZ
    public long u;              // randomSeed
    public long v;              // sizeOnDisk
    public String w;            // levelName
    public boolean z;           // multiplayerWorld
    public int saveCalls;
    public int levelSaves;
    public int expl;
    public int snd;
    public int killCalls;
    public kp teStore;
    final int[] blocks = new int[4096];
    final int[] meta = new int[4096];
    private ro wcm;

    public dn(File f, String s) {
        v = 0L; w = s;
        b = new ArrayList();
        d = new ArrayList();
        l = new Random(0L);
    }
    static int idx(int x, int y, int z) { return (y & 15) * 256 + (x & 15) * 16 + (z & 15); }
    public ro a() { if (wcm == null) { wcm = new ro(this); } return wcm; }
    public int a(int x, int y, int z) { return blocks[idx(x, y, z)]; }
    public boolean b(int x, int y, int z, int id) {
        int q = idx(x, y, z);
        boolean ch = blocks[q] != id;
        blocks[q] = id;
        return ch;
    }
    public boolean e(int x, int y, int z, int id) { return b(x, y, z, id); }
    public int e(int x, int y, int z) { return meta[idx(x, y, z)]; }
    public void c(int x, int y, int z, int mm) { meta[idx(x, y, z)] = mm; }
    public int l(int x, int y, int z) { return 15; }
    public void a(ru p) { a(true, p); }
    public void a(boolean flag, ru p) {
        if (p != null) { p.b("Saving level"); }
        r();
        if (p != null) { p.d("Saving chunks"); }
        saveCalls++;
    }
    private void r() { levelSaves++; }
    public kp b(int x, int y, int z) {
        if (teStore == null) { throw new NullPointerException("TE map miss"); }
        return teStore;
    }
    public void a(int x, int y, int z, kp te) { teStore = te; }
    public boolean a(nl e2) { b.add(e2); return true; }
    public void d(nl e2) { killCalls++; e2.aW = true; }
    public mc a(nl e2, double x, double y, double z, float r2) { expl++; return null; } // c11b777 翻案 V→Lmc;
    public List l() { return b; }
    public void a(double x, double y, double z, String n2, float v1, float v2) { snd++; }
}
