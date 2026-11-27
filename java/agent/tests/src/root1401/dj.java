// b1.4_01 WorldInfo=dj 假件（blocks\dj.txt javap -p 全量）。五槽全 private（WorldOps
// WorldInfo 分支 getDeclaredField+setAccessible 前提）：a=randSeed long、b/c/d=spawnXYZ
// int、e=worldTime long（tiny 命名全中，JSON note④ 五版同形判决）。
// ⚠️ 判决①天气下界负半轴实锤：真 dj 字段面 a-k 无任何 boolean（raining/rainTime/
// thundering/thunderTime 四键零命中）→capWeather UNSUPPORTED——假件严格不建旗标字段，
// A2Table1401Test 反射复核「dj 无 boolean 字段」。
public class dj {
    private long a;        // randSeed
    private int b, c, d;   // spawnXYZ
    private long e;        // worldTime
}
