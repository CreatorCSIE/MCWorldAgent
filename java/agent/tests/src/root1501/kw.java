// b1.5_01 EntityLiving=kw 假件（dump.txt L4349：public int W=health（ctor 默认 20）、
// Y=hurtTime/Z=maxHurtTime public int——JSON note「Entity 伤害面=kw.Y/Z 型」）。
public class kw extends rj {
    public int W = 20;   // healthF=W public int
    public int Y;        // hurtTime
    public int Z = 20;   // maxHurtTime
    public kw(et w) { super(w); }
}
