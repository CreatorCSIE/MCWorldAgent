// b1.2_02 Entity=nl 假件（详表 §3）：pos aF/aG/aH（⚠a1.2.0 ap..ar 整体断代）、
// prev aC/aD/aE、mot aI/aJ/aK、yaw/pitch aL/aM、isDead aW、worldObj aB。
public class nl {
    public double aC;
    public double aD;
    public double aE;
    public double aF;
    public double aG;
    public double aH;
    public double aI;
    public double aJ;
    public double aK;
    public float aL;
    public float aM;
    public boolean aW;          // isDead
    public int aw;              // entityId
    public nl aA;               // ridingEntity
    public dn aB;               // worldObj
    public nl(dn w) { aB = w; }
    public void b(double x, double y, double z, float ya, float pi) {
        aC = aF; aD = aG; aE = aH;
        aF = x; aG = y; aH = z;
        aL = ya; aM = pi;
    }
    public void c(double x, double y, double z, float ya, float pi) { b(x, y, z, ya, pi); }
    public void a(double x, double y, double z) { aF = x; aG = y; aH = z; }
    public void F() { aW = true; }
}
