// b1.8.1 ItemStack=ul 假件（详表 §2）：a=stackSize/c=itemID/d=itemDamage 全 public
// ✓同 b1.7.3 字母；仅 (III)V 构造（无 (II)V——WorldOps newStack 走 (III)V dmg=0
// 等价配方，形参序 arg1=id、arg2=count、arg3=damage）；k()Lul;=copy ✓同字母。
public final class ul {
    public int a;               // stackSize
    public int b;               // animationsToGo
    public int c;               // itemID
    public int d;               // itemDamage
    public ul(int id, int n, int dmg) { c = id; a = n; d = dmg; }
    public ul k() { return new ul(c, a, d); }
}
