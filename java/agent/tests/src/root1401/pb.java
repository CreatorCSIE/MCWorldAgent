// b1.4_01 Entity=pb 假件（blocks\pb.txt javap -p 全量：public eb aF=worldObj、
// pos aJ/aK/aL、mot aM/aN/aO、look aP/aQ 全 public、setPosRot b(DDDFF)V public、
// U()Z isEntityAlive public、w_()V public）。⚠️ 真 pb 系 abstract（b()/b(lg)/a(lg)
// 三抽象法）——假件按 root1501 rj 惯例收敛为具体类，仅留消费面。
// pos 字母=aJ..aQ（+1/版规律 1301=aI..→1401=aJ..→1501=aK.. 严格成立，JSON note）。
public class pb {
    public eb aF;                 // worldObj（dump 字段面 public eb aF——1501=aG 断代）
    public double aJ, aK, aL;     // posX/Y/Z（posXF/YF/ZF=aJ/aK/aL）
    public double aM, aN, aO;     // motX/Y/Z（motXF/YF/ZF=aM/aN/aO）
    public float aP, aQ;          // yaw/pitch（yawF/pitchF=aP/aQ）
    public pb(eb w) { aF = w; }
    public void w_() { }          // Entity 基类 tick（fm.w_() 覆写后 super 回调，假件空转）
    public boolean U() { return true; } // isEntityAlive=pb.U()Z（fm.b(III) 门1 探针：!U→cb.e；
                                          // ⚠️边界批 N()Z 系误——N=isPlayerFullyAsleep，JSON 勘误实锤）
    public void b(double x, double y, double z, float yaw, float pitch) {
        aJ = x; aK = y; aL = z; aP = yaw; aQ = pitch;   // setPosRotM=b (DDDFF)V
    }
}
