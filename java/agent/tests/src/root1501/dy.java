// b1.5_01 WorldInfo=dy 假件（dump.txt L1416 javap -p 全量）。五槽全 private（WorldOps
// WorldInfo 分支 getDeclaredField+setAccessible 前提）：a=randSeed long、b/c/d=spawnXYZ
// int、e=worldTime long；天气旗标 l=raining/n=thundering private boolean（NBT 键
// raining/thundering 对榫，JSON note⑰）。getter/setter 全 public：o()Z→l、m()Z→n、
// b(Z)V→l（wiSetRainM=b）、a(Z)V→n（wiSetThunderM=a）。
public class dy {
    private long a;        // randSeed
    private int b, c, d;   // spawnXYZ
    private long e;        // worldTime
    private boolean l;     // raining
    private boolean n;     // thundering
    public boolean o() { return l; }
    public boolean m() { return n; }
    public void b(boolean z) { l = z; }     // setIsRaining
    public void a(boolean z) { n = z; }     // setIsThundering
}
