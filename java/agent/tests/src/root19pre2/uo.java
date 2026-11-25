// b1.9-pre2 EntityPlayer=uo 假件（**abstract**，dump L9905，tiny L9416——活体玩家
// 真身=sd EntityPlayerSP concrete；181 sz 具体可直造的形态本版破裂，FakeOps 布景走 sd）。
// ar=inventory:vz（invF ✓public）/ ax=score / aF=xpCooldown / aW=xp / aX=playerLevel
// （XpTotal 载体）/ aY=xpLevel——全 public int（tiny L9459-9519 实锤；D10 表注：
// 与 181 aX/aY/aZ/aG/ay 整体不同，互抄即错）；
// au=foodStats:Lla 真件 **protected**（假件包可见便于对账——181 sz.av public 同例）；
// av()Lla;=getFoodStats（tiny L9418 实锤——⚠️181=at()Lkc 断代）；
// f(I)V=increaseXP（dump L10030 public；addXpM 留档，setxp 直写面不消费）。
public abstract class uo extends xx {
    // 活体批2 复现钉（W19P2 复跑实锤）：真件 EntityPlayer.a=sleepTimer **private int**
    // 与 xx.a=health 撞名，劫持玩家链就近 int 扫描——livingClass 必须走 mob 链探针。
    private int a;              // sleepTimer（撞车面，非 health）
    // T2 sleep 面（dump 访问位实锤）：protected boolean sleeping——WorldOps.setSleep
    // 按本类解析（aM 无撞名），与上方 a 同声明类。
    protected boolean aM;       // sleeping
    public vz ar;
    public int ax;              // score
    public int aF;              // xpCooldown
    public int aW;              // xp（NBT "Xp"）
    public int aX;              // playerLevel（NBT "XpTotal" 载体怪癖）
    public int aY;              // xpLevel（NBT "XpLevel"）
    public la au;               // foodStats（真件 protected，假件放宽=181 同例）
    protected uo(ti w) {
        super(w);
        ar = new vz(this);
        au = new la();
    }
    public la av() { return au; }
    public void f(int n) { aW += n; }
    public abstract void r();   // 真件抽象面（dump L10004）
}
