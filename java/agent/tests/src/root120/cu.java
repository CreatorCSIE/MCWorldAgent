import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class cu {
    public boolean a;
    public List b;
    public List c;
    public List d;
    public long e;
    public int f;
    public int k;
    public Random m;
    public int n;
    public int o;
    public int p;
    public final om r = new om();
    public File t;
    public File u;
    public long v;
    public long w;
    public final String x;
    public boolean y;
    public boolean A;
    public int saveCalls;
    public int levelSaves;
    public int expl;
    public int snd;
    public int killCalls;
    public iz teStore;
    final int[] blocks = new int[4096];
    final int[] meta = new int[4096];
    private pb wcm;

    public cu(File f, String s) {
        t = f; x = s;
        b = new ArrayList();
        c = new ArrayList();
        d = new ArrayList();
        m = new Random(0L);
    }
    static int idx(int x, int y, int z) { return (y & 15) * 256 + (x & 15) * 16 + (z & 15); }
    public pb a() { if (wcm == null) { wcm = new pb(this); } return wcm; }
    public int a(int x, int y, int z) { return blocks[idx(x, y, z)]; }
    public boolean a(int x, int y, int z, int id) { return d(x, y, z, id); }
    public boolean d(int x, int y, int z, int id) {
        int q = idx(x, y, z);
        boolean ch = blocks[q] != id;
        blocks[q] = id;
        return ch;
    }
    public int e(int x, int y, int z) { return meta[idx(x, y, z)]; }
    public void b(int x, int y, int z, int mm) { meta[idx(x, y, z)] = mm; }
    public int j(int x, int y, int z) { return 15; }
    public void a(ph p) { a(true, p); }
    public void a(boolean flag, ph p) {
        if (p != null) { p.b("Saving level"); }
        o();
        if (p != null) { p.d("Saving chunks"); }
        saveCalls++;
    }
    private void o() { levelSaves++; }
    public iz b(int x, int y, int z) {
        if (teStore == null) { throw new NullPointerException("TE map miss"); }
        return teStore;
    }
    public void a(int x, int y, int z, iz te) { teStore = te; }
    public boolean a(lk e2) { b.add(e2); return true; }
    public void d(lk e2) { killCalls++; e2.aF = true; }
    public void a(lk e2, double x, double y, double z, float r2) { expl++; }
    public List k() { return b; }
    public void a(double x, double y, double z, String n2, float v1, float v2) { snd++; }
}
