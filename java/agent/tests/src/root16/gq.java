// b1.6 EntityPlayer=gq 假件（gq_c.txt javap -c 全量 V-5 批取证 + blocks\gq.txt）。
// 睡眠四门真实序（b(III)Lcu; @0-329 反汇编实锤）：
//   门1 @1:   N()Z ifne 14 / W()Z ifne 18 → cu.e（已睡/不可睡——N=isPlayerSleeping 读
//             protected u:Z；W=si.W()Z isEntityAlive；⚠️ 1501=O/1401=M 字母漂移链）
//   门2 @22:  aH:Lfb;.t:Lwr;.c:Z → cu.b（provider 禁睡）
//   门3 @39:  aH.f()Z → cu.c（白天）
//   门4 @50/67/84: |aL-x|>3 || |aM-y|>2 || |aN-z|>3 → cu.d（距离 3/2/3）
//   OK 尾 @270-329: u=true@272、b=0@277、a=new bp(x,y,z)@291、aO/aP/aQ=0、
//             @313 !aH.B:Z→aH.y()V（updateAllPlayersSleepingFlag）、返 cu.a
//   （@125 fb 床块判定仅选朝向=省略，同 1401 假件纪律）
// wakeUpPlayer a(ZZZ)V（@1786 起）：@24 a:Lbp; ifnull 178=**null-safe**；@180 u=false；
//   @187 !aH.B && p2→aH.y()V；@204 b = p1?0:100；p3 面恒 false=省略。
// tick w_()（@157 起）：N() 段 b++ 钳100→@33 invokespecial am()Z 离床→a(true,true,false)；
//   @53 !aH.B && aH.f()→a(false,true,true)；非睡段 b>0 递增钳110；@111 invokespecial
//   lo.w_()V super 回调（1401 实形同）。
// am()Z（@~1900）：**裸解引用 a:Lbp;.a/b/c**==un.T.bn(26)——a=null+sleeping 下 tick 必
//   NPE（W173/1401 同款崩溃面三版翻版）。
// 字段访问位（真 jar javap 实锤——⚠️sister tiny u/a/v 错位勿借）：public iu c /
//   **protected boolean u**（1401 private a/1501 protected x）/ private bp a / private int b。
public class gq extends lo {
    public iu c;              // inventory（invF=c——1401/1501=f 断代）
    protected boolean u;      // sleeping（protected！真 jar javap；tiny 错位件勿借）
    private bp a;             // bedChunkCoordinates（private bp a——OK 尾 @291 实锤）
    private int b;            // sleepTimer（private int b——@277 实锤）
    public gq(fb w) { super(w); c = new iu(this); }
    public boolean N() { return u; }                    // isPlayerSleeping（@1 门1 探针）
    public boolean O() { return u && b >= 100; }        // isPlayerFullyAsleep
    public cu b(int px, int py, int pz) {               // sleepInBedAt (III)Lcu;
        if (N() || !W()) { return cu.e; }
        if (aH.t != null && aH.t.c) { return cu.b; }
        if (aH.f()) { return cu.c; }
        if (Math.abs(aL - px) > 3.0 || Math.abs(aM - py) > 2.0 || Math.abs(aN - pz) > 3.0) {
            return cu.d;
        }
        u = true;
        b = 0;
        a = new bp(px, py, pz);
        aO = 0.0; aP = 0.0; aQ = 0.0;
        if (!aH.B) { aH.y(); }
        return cu.a;
    }
    public void a(boolean p1, boolean p2, boolean p3) { // wakeUpPlayer (ZZZ)V——a null-safe
        if (a != null) {
            aH.a(a.a, a.b, a.c);   // 床清理判定面（@26-137）假件仅走读面
        }
        u = false;
        if (!aH.B && p2) { aH.y(); }
        b = p1 ? 0 : 100;
    }
    private boolean am() {             // isInBed——裸解引用 a（tick NPE 根因 @33）
        return aH.a(a.a, a.b, a.c) == 26;   // un.T.bn=Block.bed.blockID=26
    }
    public void w_() {
        if (N()) {
            b = b + 1;
            if (b > 100) { b = 100; }
            if (!am()) {
                a(true, true, false);
            } else if (!aH.B && aH.f()) {
                a(false, true, true);
            }
        } else if (b > 0) {
            b = b + 1;
            if (b >= 110) { b = 0; }
        }
        super.w_();                    // @111 invokespecial lo.w_()V（1401/16 实形回调）
    }
}
