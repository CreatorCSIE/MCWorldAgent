// b1.8.1 WorldInfo=rl 假件（详表 §2，=ei 字段表全同字母复刻+GameType/MapFeatures 扩展）：
// 字段面 **全 private**（javap 实锤）——a=randomSeed:J / e=worldTime:J /
// b/c/d=spawnXYZ:I / i=dimension:I / l=raining:Z / m=rainTime:I /
// n=thundering:Z / o=thunderTime:I（天气键同字母）。
// WorldOps resolve WorldInfo 分支经 getDeclaredField+setAccessible 读写五槽——
// 假件若写成 public 即假绿，访问位必须钉死。
public class rl {
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
    private int p;              // D13 GameType（tiny 实锤 rl.p=field_35920_p，NBT "GameType"）
}
