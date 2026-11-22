// b1.7.3 Entity=sn 假件（详表 §3）：pos aM/aN/aO（⚠️b1.2_02=aF..aH 整体断代，
// NBT "Pos" 写读配对）、mot aP/aQ/aR、yaw/pitch aS/aT、by=heartsLife public int
// （iframesF 复活=invulnerable 档位一欠账清偿，⚠️非 be=isDead 撞车位）、
// be=isDead、aI=worldObj（⚠️b1.2_02=aB）、b(DDDFF)V setPositionAndRotation ✓历代同形。
public class sn {
    public double aM;
    public double aN;
    public double aO;
    public double aP;
    public double aQ;
    public double aR;
    public float aS;
    public float aT;
    public int by;              // heartsLife（i-frame 计时器）
    public boolean be;          // isDead
    public fd aI;               // worldObj
    public sn(fd w) { aI = w; }
    public void b(double x, double y, double z, float ya, float pi) {
        aM = x; aN = y; aO = z;
        aS = ya; aT = pi;
    }
    public void a(double x, double y, double z) { aM = x; aN = y; aO = z; }
    public void F() { be = true; }
}
