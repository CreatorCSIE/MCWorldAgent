// b1.5_01 EntityPlayer=gh 假件（gh_disasm.txt javap -c 全量 + dump.txt L2520）。
// 睡眠四门真实序（b(III)Lco; 反汇编 @0-104）：
//   门1 @0:  O()Z || !W()Z → co.e（已睡/不可睡）
//   门2 @18: aG.o:Lvr;.c:Z → co.b（provider 禁睡）
//   门3 @35: aG.f()Z → co.c（白天）
//   门4 @49: |aK-x|>3 || |aL-y|>2 || |aM-z|>3 → co.d（距离 3/2/3）
//   OK 尾 @270-325: x=true、b=0、a=new bk(x,y,z)、aN/aO/aP=0、!aG.v→aG.y()V、返 co.a
// ⚠️ 1501 头部无 isRemote 旁路（173=fd.B/181=rv.I 头旁路断代不适用——b(III) 反汇编
//   @0 直接 O()Z；et.v:Z 仅门控 OK 尾/wake 旗标刷新与 tick 黎明段 @49/@115）。
// 字段访问位（dump 实锤）：public ih f / protected boolean x / private bk a / private int b。
public class gh extends kw {
    public ih f;           // inventory（invF=f public）
    protected boolean x;   // sleeping（protected，同 181 访问位形态）
    private bk a;          // bedChunkCoordinates（private bk a——⚠️非 173 v:Lbr; public 形）
    private int b;         // sleepTimer（private int b）
    private bk c;          // 床向次级坐标（disasm 字段面 private bk c/d，仅留形）
    private bk d;
    private int e;
    public gh(et w) { super(w); f = new ih(this); }
    public boolean O() { return x; }   // isSleeping=gh.O()Z（disasm @0 直返 x）
    public co b(int px, int py, int pz) {              // sleepInBedAt (III)Lco;
        if (O() || !W()) { return co.e; }
        if (aG.o != null && aG.o.c) { return co.b; }
        if (aG.f()) { return co.c; }
        if (Math.abs(aK - px) > 3.0 || Math.abs(aL - py) > 2.0 || Math.abs(aM - pz) > 3.0) {
            return co.d;
        }
        x = true;
        b = 0;
        a = new bk(px, py, pz);
        aN = 0.0; aO = 0.0; aP = 0.0;
        if (!aG.v) { aG.y(); }
        return co.a;
    }
    public void a(boolean p1, boolean p2, boolean p3) { // wakeUpPlayer (ZZZ)V——a:Lbk; null-safe（disasm @24 ifnull 跳床清理）
        if (a != null) {
            // 床清理判定面（disasm @24-177：et.a(bk a/b/c)==to.S.bl(26)→tx.a/tx.f→c(DDD)）
            // 假件仅走读面，不建模方块变更
            aG.a(a.a, a.b, a.c);
        }
        x = false;
        if (!aG.v && p2) { aG.y(); }
        b = p1 ? 0 : 100;
    }
    private boolean am() {             // isInBed——裸解引用 a（W173 同款 NPE 根因，disasm @32 invokespecial am）
        return aG.a(a.a, a.b, a.c) == 26;   // to.S.bl=Block.bed.blockID=26
    }
    public void w_() {                 // tick（disasm @0-110：睡眠段 b++ 钳 100 + am 离床唤醒 + 黎明段 et.v/et.f 旁路）
        if (O()) {
            b = b + 1;
            if (b > 100) { b = 100; }
            if (!am()) {
                a(true, true, false);
            } else if (!aG.v && aG.f()) {
                a(false, true, true);
            }
        } else if (b > 0) {
            b = b + 1;
            if (b >= 110) { b = 0; }
        }
    }
}
