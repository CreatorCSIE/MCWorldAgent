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
    // h10 批 sleep 面翻案（同 173/181 配方，字母表独立）：aM=sleeping/
    // a=sleepTimer/aN:Lrc;=bedChunkCoordinates——d(III)Lbr;=sleepInBedAt
    // （门 ap()Z/H()Z→e、ti.y:aff.c→b、ti.k()Z 白天→c、距 o/p/q>3/2/3→d；
    // 成功尾 aM=true/a=0/aN=new rc/r-s-t 清零/ti.z()V/返 br.a，disasm L2323 区段）、
    // a(ZZZ)V=wakeUpPlayer（L2538 区段：aM=false/iload_2→ti.z()/iload_1→a=0|100）、
    // w_()=tick 睡眠段（am 裸解引用 aN——null 即 NPE，崩溃回归面同构）。
    protected boolean aM;       // sleeping
    public rc aN;               // bedChunkCoordinates
    public boolean ap() { return aM; }
    public boolean H() { return true; }   // canSleep（下界门挂 ti.y.c，本假件恒真）
    public br d(int x, int y, int z) {    // sleepInBedAt
        if (!k.I) {
            if (ap() || !H()) return br.e;
            if (k.y != null && k.y.c) return br.b;
            if (k.k()) return br.c;
            if (Math.abs(o - x) > 3 || Math.abs(p - y) > 2 || Math.abs(q - z) > 3) {
                return br.d;
            }
        }
        aM = true;
        a = 0;
        aN = new rc(x, y, z);
        r = s = t = 0;
        if (!k.I) k.z();
        return br.a;
    }
    public void a(boolean f1, boolean f2, boolean f3) {   // wakeUpPlayer
        if (aN != null && k.a(aN.a, aN.b, aN.c) == 26) {
            // 真形清占用+另一半+传送——假件仅留判决面
        }
        aM = false;
        if (!k.I && f2) k.z();
        a = f1 ? 0 : 100;
    }
    private boolean am() {            // isInBed——aN==null 裸解引用（真形复刻）
        return k.a(aN.a, aN.b, aN.c) == 26;
    }
    public void w_() {                // tick 睡眠段
        if (ap()) {
            a++;
            if (a > 100) a = 100;
            if (!k.I) {
                if (!am()) {
                    a(true, true, false);
                } else if (k.k()) {
                    a(false, true, true);
                }
            }
        } else if (a > 0) {
            a++;
            if (a >= 110) a = 0;
        }
    }
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
