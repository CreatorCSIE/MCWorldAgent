// b1.5_01 Entity=rj 假件（dump.txt L6659 javap -p 全量：public et aG=worldObj、
// pos aK/aL/aM、mot aN/aO/aP、look aQ/aR 全 public、setPosRot b(DDDFF)V public、
// W()Z public、w_()V public）。⚠️ pos 字母=aK..aR 双字母（1501 整体左移两位 vs
// 173 aM..aT，JSON note 严禁借表）。
public class rj {
    public et aG;                 // worldObj（dump 字段面 public et aG）
    public double aK, aL, aM;     // posX/Y/Z（posXF/YF/ZF=aK/aL/aM）
    public double aN, aO, aP;     // motX/Y/Z（motXF/YF/ZF=aN/aO/aP）
    public float aQ, aR;          // yaw/pitch（yawF/pitchF=aQ/aR）
    public rj(et w) { aG = w; }
    public void w_() { }          // Entity 基类 tick（gh.w_() 覆写，不回调 super）
    public boolean W() { return true; } // Entity.W()Z（gh.b(III) 门1 探针：!W→co.e；假件恒可睡）
    public void b(double x, double y, double z, float yaw, float pitch) {
        aK = x; aL = y; aM = z; aQ = yaw; aR = pitch;   // setPosRotM=b (DDDFF)V
    }
}
