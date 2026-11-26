// b1.8.1 EntityPlayer=sz 假件（详表 §2）：inventory=as（⚠️b1.7.3=c）。
// D10 xp 面（EntityPlayer.java 直读）：aX="Xp"/aY="XpLevel"/aZ="XpTotal"/aG="XpCooldown"/ay=score。
public class sz extends wd {
    public ui as;
    public int aX;   // NBT "Xp"
    public int aY;   // NBT "XpLevel"
    public int aZ;   // NBT "XpTotal"
    public int aG;   // NBT "XpCooldown"
    public int ay;   // score
    // h10 批 sleep 面翻案（同 173 配方，字母表独立）：aN=sleeping protected/
    // a=sleepTimer private/aO:Lps;=bedChunkCoordinates——d(III)Lbm;=sleepInBedAt
    // （门 an()Z/G()Z→e、rv.y.acz.c→b、rv.k()Z 白天→c、距 o/p/q>3/2/3→d；
    // 成功尾 aN=true/a=0/aO=new ps/r-s-t 清零/rv.y()V/返 bm.a）、
    // a(ZZZ)V=wakeUpPlayer（v==null null-safe）、w_()=tick 睡眠段（am 裸解引用
    // aO——null 即 NPE，W173 崩溃回归面 181 同构）。
    protected boolean aN;
    private int a;
    public ps aO;
    public kc av = new kc(); // D11 FoodStats（protected field_35217_av，假件 public 便于对账）
    public sz(rv w) { super(w); as = new ui(this); }
    public kc at() { return av; } // D11 func_35191_at()Lkc; getFoodStats
    public boolean an() { return aN; }
    public boolean G() { return true; }   // canSleep（下界门挂 rv.y.c，本假件恒真）
    public bm d(int x, int y, int z) {    // sleepInBedAt
        if (!k.I) {
            if (an() || !G()) return bm.e;
            if (k.y != null && k.y.c) return bm.b;
            if (k.k()) return bm.c;
            if (Math.abs(o - x) > 3 || Math.abs(p - y) > 2 || Math.abs(q - z) > 3) {
                return bm.d;
            }
        }
        aN = true;
        a = 0;
        aO = new ps(x, y, z);
        r = s = t = 0;
        if (!k.I) k.y();
        return bm.a;
    }
    public void a(boolean f1, boolean f2, boolean f3) {   // wakeUpPlayer
        if (aO != null && k.a(aO.a, aO.b, aO.c) == 26) {
            // 真形清占用+另一半+传送——假件仅留判决面
        }
        aN = false;
        if (!k.I && f2) k.y();
        a = f1 ? 0 : 100;
    }
    private boolean am() {            // isInBed——aO==null 裸解引用（真形复刻）
        return k.a(aO.a, aO.b, aO.c) == 26;
    }
    public void w_() {                // tick 睡眠段
        if (an()) {
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
}
