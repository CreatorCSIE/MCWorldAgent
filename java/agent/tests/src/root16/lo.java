// b1.6 EntityLiving=lo 假件（blocks\lo.txt javap -p）：health=**Y:I** public（173 同字母；
// 1401=V/1501=W 洗牌链）、hurtTime=aa/maxHurtTime=ab（1401 X/Y、1501 Z/Y 再洗牌）。
// ⚠️ iframes 判决=null：全 tiny 零 heartsLife 命中→invulnerable UNSUPPORTED——假件不建免伤槽。
// 真 lo abstract——假件收敛具体；W()/w_() 继承 si 不覆写。
public class lo extends si {
    public int Y = 20;   // healthF=Y public int
    public int aa;       // hurtTime
    public int ab = 20;  // maxHurtTime
    public lo(fb w) { super(w); }
}
