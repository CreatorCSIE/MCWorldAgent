// b1.2_02 ItemStack=gm 假件（详表 §5，断代双陷阱）：
// 1) itemDamage=d 为 **private**（WorldOps getDeclaredField+setAccessible fallback）；
// 2) 无 (II)V 构造——仅 (III)V（WorldOps newStack dmg=0 等价配方）。
// 字母 a=count/c=id ✓同 a1.2.0 fi。
public final class gm {
    public int a;               // stackSize
    public int b;               // animationsToGo
    public int c;               // itemID
    private int d;              // itemDamage（private=本批断代实形）
    public gm(int id, int n, int dmg) { c = id; a = n; d = dmg; }
    public int getDamage() { return d; }
}
