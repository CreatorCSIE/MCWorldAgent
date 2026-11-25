// b1.9-pre2 EntityLiving=xx 假件（**abstract**，dump L11421；⚠️ 撞车位：181 xx 无此件）。
// health=a **private**（⚠️181 bz public——断代访问位翻面，dump L11444 实锤；
// WorldOps.healthField getDeclaredField 父链回退消费，b1.2_02 isDmgF 同配方）；
// bP=activePotionsMap protected HashMap（键=Potion id；假件 public 便于对账——
// 181 wd.bR 同例）；hurtTime=bz/maxHurtTime=bA（留档）；
// a(Lafk;)V=addPotionEffect / i(I)V=removePotionEffect / aG()V=clearActivePotions
// （tiny L11084/11115/11028——181 无 aG 清空面，本版新增留档）。
public abstract class xx extends lh {
    private int a = 10;         // health（NBT "Health" 保底 10，**private** 断代）
    public java.util.HashMap bP = new java.util.HashMap();
    public int bz;              // hurtTime（留档）
    public int bA;              // maxHurtTime（留档）
    protected xx(ti w) { super(w); }
    public void a(afk e) { bP.put(Integer.valueOf(e.a), e); }
    public void i(int id) { bP.remove(Integer.valueOf(id)); }
    public void aG() { bP.clear(); }
    protected void b() { }      // lh 抽象面落地（本批零消费）
}
