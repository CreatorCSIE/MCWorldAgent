// b1.6 WorldInfo=eg 假件（blocks\eg.txt javap -p 全量）。五槽全 private（WorldOps
// WorldInfo 分支前提）：a=randSeed:J/b/c/d=spawnXYZ:I/e=worldTime:J——与 1401 dj/
// 1501 dy 同字母（五版同形链）。
// **天气键在位（capWeather OK 世代，1401 判决①负锚对照翻面）**：l=raining:Z/
// m=rainTime:I/n=thundering:Z/o=thunderTime:I private + public setter
// **b(Z)V=setRaining/a(Z)V=setThundering**（tiny 命名+WorldOps weather L1754
// getMethod(M_WI_SET_RAIN, boolean.class) 消费面；1501 dy 同字母 b/a）。
public class eg {
    private long a;        // randSeed
    private int b, c, d;   // spawnXYZ
    private long e;        // worldTime
    private boolean l;     // raining
    private int m;         // rainTime
    private boolean n;     // thundering
    private int o;         // thunderTime
    public void b(boolean v) { l = v; }    // setRainingM=b (Z)V
    public void a(boolean v) { n = v; }    // setThunderingM=a (Z)V
}
