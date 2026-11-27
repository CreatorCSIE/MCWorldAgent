// b1.4_01 EntityPlayer=fm 假件（fm_c.txt javap -c 全量新取证 2026-11-27 + blocks\fm.txt）。
// 睡眠四门真实序（b(III)Lcb; @0-329 反汇编实锤）：
//   门1 @0:   M()Z ifne 14 / U()Z ifne 18 → cb.e（已睡/不可睡——M=isPlayerSleeping 读
//             private a:Z；U=pb.U()Z isEntityAlive；⚠️边界批 N()Z 系误，N=isPlayerFullyAsleep）
//   门2 @18:  aF:Leb;.m:Lsy;.c:Z → cb.b（provider 禁睡）
//   门3 @35:  aF.f()Z → cb.c（白天）
//   门4 @49:  |aJ-x|>3 || |aK-y|>2 || |aL-z|>3 → cb.d（距离 3/2/3）
//   OK 尾 @104-329: a=true@272、c=0@277、b=new ay(x,y,z)@291、aM/aN/aO=0、
//             @313 !aF.t:Z→aF.x()V（updateAllPlayersSleepingFlag）、返 cb.a
//   （@125 eb.h(III)Z 床块判定仅选朝向吸附偏移，不改 OK 结果=假件省略；
//     @105-109 b(0.2f,0.2f)V setSize 与 @143 rj.c(I)I 床向面不消费=省略）
// wakeUpPlayer a(ZZZ)V（@1723 起）：@26 b:Lay; ifnull 178=**null-safe**；@178 a=false；
//   @183 !aF.t && p2 → aF.x()V；@204 c = p1?0:100；@222 p3→a(b)Lay;（本 op 恒 false=省略）。
// tick w_()V（@157 起）：M() 段 c++ 钳100 → @33 invokespecial ah()Z 离床→a(true,true,false)；
//   @49 !aF.t && aF.f() → a(false,true,true)；非睡段 c>0 递增钳110；@111 invokespecial
//   jm.w_()V（1401 回调 super——1501 假件不回调，本假件按实形回调）。
// ah()Z（@1838）：**裸解引用 b:Lay;.a/b/c**==ra.S.bl(26)——b=null+sleeping 下 tick 必 NPE
//   （W173 同款崩溃面，1401 翻版）。
// 字段访问位（blocks\fm.txt javap -p 实锤）：public hh f / private boolean a /
//   private ay b / private int c——⚠️ 1401 全 private 无 protected（1501 x protected 断代）。
public class fm extends jm {
    public hh f;           // inventory（invF=f public hh）
    private boolean a;     // sleeping（private a:Z——1501=x protected 断代）
    private ay b;          // bedChunkCoordinates（private b:Lay;——1501=a 断代）
    private int c;         // sleepTimer（private c:I——1501=b 断代）
    private ay d;          // 床向次级坐标（disasm 字段面 private ay d，仅留形）
    private int e;         // （仅留形）
    public fm(eb w) { super(w); f = new hh(this); }
    public boolean M() { return a; }                    // isPlayerSleeping（@0 直返 a）
    public boolean N() { return a && c >= 100; }        // isPlayerFullyAsleep（JSON 勘误件）
    public cb b(int px, int py, int pz) {               // sleepInBedAt (III)Lcb;
        if (M() || !U()) { return cb.e; }
        if (aF.m != null && aF.m.c) { return cb.b; }
        if (aF.f()) { return cb.c; }
        if (Math.abs(aJ - px) > 3.0 || Math.abs(aK - py) > 2.0 || Math.abs(aL - pz) > 3.0) {
            return cb.d;
        }
        a = true;
        c = 0;
        b = new ay(px, py, pz);
        aM = 0.0; aN = 0.0; aO = 0.0;
        if (!aF.t) { aF.x(); }
        return cb.a;
    }
    public void a(boolean p1, boolean p2, boolean p3) { // wakeUpPlayer (ZZZ)V——b null-safe
        if (b != null) {
            // 床清理判定面（@26-137：eb.a(ay)==26→rj.a/rj.f→c(DDD)）假件仅走读面
            aF.a(b.a, b.b, b.c);
        }
        a = false;
        if (!aF.t && p2) { aF.x(); }
        c = p1 ? 0 : 100;
    }
    private boolean ah() {             // isInBed——裸解引用 b（tick NPE 根因 @32 invokespecial）
        return aF.a(b.a, b.b, b.c) == 26;   // ra.S.bl=Block.bed.blockID=26
    }
    public void w_() {                 // tick（@0-115 睡眠段+离床唤醒+黎明段 eb.t/eb.f 旁路）
        if (M()) {
            c = c + 1;
            if (c > 100) { c = 100; }
            if (!ah()) {
                a(true, true, false);
            } else if (!aF.t && aF.f()) {
                a(false, true, true);
            }
        } else if (c > 0) {
            c = c + 1;
            if (c >= 110) { c = 0; }
        }
        super.w_();                    // @111 invokespecial jm.w_()V（1401 实形回调）
    }
}
