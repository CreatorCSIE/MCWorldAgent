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
}
