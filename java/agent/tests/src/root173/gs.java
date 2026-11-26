// b1.7.3 EntityPlayer=gs 假件（详表 §3；h10 批床面全模型）：
// inventory=c（⚠️b1.2_02=f 字母回漂）。sleep 面（真 jar gs_disasm 逐段复刻）：
// u=sleeping protected / a=sleepTimer private / v=bedChunkCoordinates:Lbr;——
// b(III)Lcw;=sleepInBedAt（L1579 区段：门 e/b/c/d→写 u/a=0/v/清动量→fd.y()→cw.a）、
// a(ZZZ)V=wakeUpPlayer（L2066 区段：v==null ifnull 跳床段=null-safe、u=false、
// iload_2→fd.y()、iload_1→a=0|100）、w_()=tick 睡眠段（L173 区段：N()Z→++a 封顶
// 100→!fd.B→am() 裸解引用 v——**v=null 即 NPE=W173 崩溃根因回归面**）。
public class gs extends ls {
    public ix c;
    protected boolean u;          // sleeping
    private int a;                // sleepTimer
    public br v;                  // bedChunkCoordinates（h10：崩溃根因载体）
    public gs(fd w) { super(w); c = new ix(this); }
    public boolean N() { return u; }
    public boolean W() { return true; }   // canSleep（下界判决挂 xa.c 门，本假件恒真）
    public cw b(int x, int y, int z) {    // sleepInBedAt
        if (!aI.B) {
            if (N() || !W()) return cw.e;
            if (aI.t != null && aI.t.c) return cw.b;
            if (aI.f()) return cw.c;
            if (Math.abs(aM - x) > 3 || Math.abs(aN - y) > 2 || Math.abs(aO - z) > 3) {
                return cw.d;
            }
        }
        u = true;
        a = 0;                    // 合法态 timer=0（tick 自增），非旧裸写 100
        v = new br(x, y, z);
        aP = aQ = aR = 0;
        if (!aI.B) aI.y();
        return cw.a;
    }
    public void a(boolean f1, boolean f2, boolean f3) {   // wakeUpPlayer
        if (v != null && aI.a(v.a, v.b, v.c) == 26) {
            // 真形：ve.a 清占用 + ve.f 另一半 + 传送站立——假件仅留判决面
        }
        u = false;
        if (!aI.B && f2) aI.y();
        a = f1 ? 0 : 100;
    }
    private boolean am() {        // isInBed——v==null 裸解引用（真形 L1909 复刻）
        return aI.a(v.a, v.b, v.c) == 26;
    }
    public void w_() {            // tick 睡眠段（真形 L173 区段复刻）
        if (N()) {
            a++;
            if (a > 100) a = 100;
            if (!aI.B) {
                if (!am()) {
                    a(true, true, false);
                } else if (aI.f()) {
                    a(false, true, true);
                }
            }
        } else if (a > 0) {
            a++;
            if (a >= 110) a = 0;
        }
    }
}
