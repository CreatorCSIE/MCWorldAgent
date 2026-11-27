// b1.6 Entity=si 假件（blocks\si.txt javap -p + gq_c.txt 反汇编交叉）。⚠️真 jar 实形
// （sister tiny 错位一位勿信：tiny aI=worldObj/aM=posX）：worldObj=**aH:Lfb;**、
// pos=**aL/aM/aN**（gq.b 门4 @50-84 实锤）、mot=**aO/aP/aQ**、look=**aR/aS**、
// isEntityAlive=**W()Z**（gq.b 门1 @8 探针；1501=rj.W 同字母）、setPosRot=b(DDDFF)V、
// tick=w_()V。真 si abstract——假件按 root1401/1501 惯例收敛具体仅留消费面。
public class si {
    public fb aH;                 // worldObj（真 jar aH——tiny 错位件 aI 勿借）
    public double aL, aM, aN;     // posX/Y/Z（posXF/YF/ZF=aL/aM/aN——+1/版规律 16 站）
    public double aO, aP, aQ;     // motX/Y/Z
    public float aR, aS;          // yaw/pitch
    public si(fb w) { aH = w; }
    public void w_() { }          // Entity 基类 tick（gq.w_() super 回调 @111 实锤，假件空转）
    public boolean W() { return true; } // isEntityAlive=si.W()Z（门1 !W→cu.e 探针）
    public void b(double x, double y, double z, float yaw, float pitch) {
        aL = x; aM = y; aN = z; aR = yaw; aS = pitch;   // setPosRotM=b (DDDFF)V
    }
}
