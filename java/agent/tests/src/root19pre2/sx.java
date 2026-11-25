// b1.9-pre2 WorldInfo=sx 假件（dump L8871-8927，ei→rl→sx 字段字母全同判决兑现）：
// 字段面 **全 private**（javap 实锤）——a=randomSeed:J / e=worldTime:J /
// b/c/d=spawnXYZ:I / i=dimension:I / l=raining:Z / m=rainTime:I /
// n=thundering:Z / o=thunderTime:I / p=gameType:I（NBT "GameType"，tiny L8128）。
// WorldOps resolve WorldInfo 分支经 getDeclaredField+setAccessible 读写——
// 假件若写成 public 即假绿，访问位必须钉死。
public class sx {
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
    private int p;              // GameType（setgamemode 消费面）
    // T1 批（无头窗 2026-11-26）：旗标 setter 面（dump L8871-8887 public 在册，
    // JSON note 钉 setIsRaining=b/setIsThundering=a）。记录位=public 观测面，
    // FakeOps19pre2 weather 断言消费（真件旗标即上方 private l/n——setter 写它，
    // 假件以独立记录位复现"写旗标"事件本身，字段面由 setAccessible 路径另验）。
    public boolean rainFlagSet;
    public boolean thunderFlagSet;
    public void b(boolean v) { rainFlagSet = v; }      // setIsRaining
    public void a(boolean v) { thunderFlagSet = v; }   // setIsThundering
}
