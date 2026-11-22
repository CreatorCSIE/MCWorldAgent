// b1.7.3 WorldInfo=ei 假件（详表 §2 结构断代①）：字段面 **全 private**（javap 实锤）——
// a=randomSeed:J / e=worldTime:J / b/c/d=spawnXYZ:I / i=dimension:I /
// l=raining:Z / m=rainTime:I / n=thundering:Z / o=thunderTime:I（天气键持久化复活）。
// WorldOps resolve WorldInfo 分支经 getDeclaredField+setAccessible 读写五槽——
// 假件若写成 public 即假绿，访问位必须钉死。
// 方法面（public 存取器，javap 复刻）：f()J/a(J)V/b()J/c()I/d()I/e()I/a(III)V/
// i()I/o()Z/p()I/m()Z/n()I/b(Z)V/f(I)V/a(Z)V/e(I)V。
public class ei {
    private long a;             // randomSeed（NBT "RandomSeed"）
    private long e;             // worldTime（NBT "Time"）
    private int b;              // spawnX
    private int c;              // spawnY
    private int d;              // spawnZ
    private int i;              // dimension
    private boolean l;          // raining
    private int m;              // rainTime
    private boolean n;          // thundering
    private int o;              // thunderTime

    public long f() { return e; }
    public void a(long v) { e = v; }
    public long b() { return a; }
    public int c() { return b; }
    public int d() { return c; }
    public int e() { return d; }
    public void a(int x, int y, int z) { b = x; c = y; d = z; }
    public int i() { return i; }
    public boolean o() { return l; }
    public int p() { return m; }
    public boolean m() { return n; }
    public int n() { return o; }
    public void b(boolean v) { l = v; }
    public void f(int v) { m = v; }
    public void a(boolean v) { n = v; }
    public void e(int v) { o = v; }
}
