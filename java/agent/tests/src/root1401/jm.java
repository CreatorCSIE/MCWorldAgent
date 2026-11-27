// b1.4_01 EntityLiving=jm 假件（blocks\jm.txt javap -p 全量：public int V=health
// （healthF=V——1501=W 断代）、X=hurtTime/Y=maxHurtTime public int（1501=Z/Y 洗牌））。
// ⚠️ jm 无 heartsLife 类免伤槽（JSON note：iframes=null 判决）→invulnerable UNSUPPORTED。
// 真 jm abstract——假件按惯例收敛具体；U()/w_() 继承 pb，不覆写（消费面等价）。
public class jm extends pb {
    public int V = 20;   // healthF=V public int（ctor 默认 20，同 1501 kw.W 语义）
    public int X;        // hurtTime
    public int Y = 20;   // maxHurtTime
    public jm(eb w) { super(w); }
}
