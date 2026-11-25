// b1.9-pre2 PotionEffect=afk 假件（dump L1645，ade→afk 断代）：ctor (III)V
// （id,durationTicks,amplifier）✓历代同形（tiny L1478）；字段 a=potionID/
// b=duration/c=amplifier 全 public int（tiny L1476/1482/1484）。
public class afk {
    public int a;
    public int b;
    public int c;
    public afk(int id, int dur, int amp) { a = id; b = dur; c = amp; }
}
