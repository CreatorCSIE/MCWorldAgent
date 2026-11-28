// b1.6 EntityLiving=lo 假件（blocks\lo.txt javap -p）：health=**X:I** public（活体勘误批
// 2026-11-28——原稿 Y 系抄 173 字母违 +1/版规律；NBT "Health" 双向+保底 10+clamp 20
// 三重实锤钉 X；Y=hurt 快照备份非 health；1401=V/1501=W/16=X/173=Y 洗牌链）、
// hurtTime=aa/maxHurtTime=ab（1401 X/Y、1501 Z/Y 再洗牌）。
// ⚠️ iframes 判决=null：全 tiny 零 heartsLife 命中→invulnerable UNSUPPORTED——假件不建免伤槽。
// 真 lo abstract——假件收敛具体；W()/w_() 继承 si 不覆写。
public class lo extends si {
    public int X = 20;   // healthF=X public int（勘误件）
    public int Y = 20;   // hurt 快照备份（attackEntityFrom X→Y，非 health）
    public int aa;       // hurtTime
    public int ab = 20;  // maxHurtTime
    public lo(fb w) { super(w); }
}
