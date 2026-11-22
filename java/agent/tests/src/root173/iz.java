// b1.7.3 ItemStack=iz 假件（详表 §5）：a=stackSize/c=itemID/d=itemDamage 全 public
// （⚠️b1.2_02 gm.d private 断代不复现——本版 getField 直取路径）；仅 (III)V 构造
// （无 (II)V——WorldOps newStack 走 (III)V dmg=0 等价配方，形参序以 WorldOps 源码为准：
// arg1=id、arg2=count、arg3=damage，同 b1.2_02 gm 实形）；k()Liz;=copy。
public final class iz {
    public int a;               // stackSize
    public int b;               // animationsToGo
    public int c;               // itemID
    public int d;               // itemDamage
    public iz(int id, int n, int dmg) { c = id; a = n; d = dmg; }
    public iz k() { return new iz(c, a, d); }
}
