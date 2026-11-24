// b1.8.1 EntityLiving=wd 假件（详表 §2，⚠️ 撞车位：b1.7.3 wd=WorldProviderHell）。
// health=bz（⚠️b1.7.3=Y；NBT "Health" 保底 10）。
// D10 效果面（EntityLiving.java 直读）：bR=activePotionsMap、a(Lade;)V=addPotionEffect、
// f(I)V=removePotionEffect（damageEntity 误名 L1019 bR.remove 实锤）。
public class wd extends kj {
    public int bz = 10;
    public java.util.HashMap bR = new java.util.HashMap();
    public wd(rv w) { super(w); }
    public void a(ade e) { bR.put(Integer.valueOf(e.a), e); }
    public void f(int id) { bR.remove(Integer.valueOf(id)); }
}
