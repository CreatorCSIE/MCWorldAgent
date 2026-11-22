// b1.8.1 Entity=kj 假件（详表 §2，⚠️ 撞车位：b1.7.3 sn=Entity↔b1.8.1 sn=EntityCreeper）。
// pos o/p/q（⚠️b1.7.3=aM..aO 整体重写，NBT "Pos" 配对）、mot r/s/t、yaw/pitch u/v、
// aa=heartsLife public int（iframesF，⚠️b1.7.3=by）、G=isDead boolean、
// k=worldObj:Lrv;（tiny 实钉）、b(DDDFF)V setPositionAndRotation ✓历代同形、
// v()V=setEntityDead（Entity 基类，tiny 实钉）。
public class kj {
    public double o;
    public double p;
    public double q;
    public double r;
    public double s;
    public double t;
    public float u;
    public float v;
    public int aa;              // heartsLife（i-frame 计时器）
    public boolean G;           // isDead
    public rv k;                // worldObj
    public kj(rv w) { k = w; }
    public void b(double x, double y, double z, float ya, float pi) {
        o = x; p = y; q = z;
        u = ya; v = pi;
    }
    public void a(double x, double y, double z) { o = x; p = y; q = z; }
    public void v() { G = true; }
}
