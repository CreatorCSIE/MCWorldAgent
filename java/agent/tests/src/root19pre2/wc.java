// b1.9-pre2 ItemStack=wc 假件（**final**，dump L10548，iz→ul→wc 断代）：
// a=stackSize/c=itemID public int，**e=itemDamage private**（⚠️181=d public——
// 断代访问位翻面，dump L10553 实锤；resolveInvFace getDeclaredField 回退消费
// b1.2_02 同配方）；b=animationsToGo 留档；真件 public 构造仅 (III)V（L10560）
// ——(II)V 缺席延续（newStack 等价配方同 181）。
public final class wc {
    public int a;               // stackSize
    public int b;               // animationsToGo（留档）
    public int c;               // itemID
    private int e;              // itemDamage（**private** 断代）
    public wc(int id, int n, int dmg) { c = id; a = n; e = dmg; }
    public wc k() { return new wc(c, a, e); }
}
