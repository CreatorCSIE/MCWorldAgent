// b1.9-pre2 Entity=lh 假件（**abstract**，dump L5823；⚠️ 撞车位：181 lh=EntityDamageSource）。
// pos o/p/q、mot r/s/t、look u/v（右移规律在案）；Z=heartsLife public int
// （iframesF，tiny L5057——1301 移除→pre2 回归，181=aa 断代）；G=isDead boolean
// （tiny L5242 ✓同 181 字母）；k=worldObj:Lti；
// c(DDDFF)V=setLocationAndAngles **主写面**（setPosRotM，tiny L5037——⚠️181 主写=
// b setPositionAndRotation，断代翻面；b(DDDFF)V 同形 decoy 在位 L4991）；
// u()V=setEntityDead（Entity 级，留档）。
public abstract class lh {
    public double o;
    public double p;
    public double q;
    public double r;
    public double s;
    public double t;
    public float u;
    public float v;
    public int Z;               // heartsLife（i-frame 计时器，回归钉）
    public boolean G;           // isDead
    public ti k;                // worldObj
    protected lh(ti w) { k = w; }
    public void b(double x, double y, double z, float ya, float pi) {  // decoy 同形
        o = x; p = y; q = z;
        u = ya; v = pi;
    }
    public void c(double x, double y, double z, float ya, float pi) {  // 主写面
        o = x; p = y; q = z;
        u = ya; v = pi;
    }
    protected abstract void b();   // 真件 onEntityUpdate 抽象面（dump L5892）
}
