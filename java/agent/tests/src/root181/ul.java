// b1.8.1 ItemStack=ul 假件（详表 §2；h10 批翻案修正"全 public"误记）：
// a=stackSize/c=itemID public、**d=itemDamage private**（真 jar javap 实锤——
// setchest getDeclaredField 修复对 181 同样必要）；仅 (III)V 构造（无 (II)V——
// WorldOps newStack 走 (III)V dmg=0 等价配方，形参序 arg1=id、arg2=count、
// arg3=damage）；k()Lul;=copy ✓同字母。
public final class ul {
    public int a;               // stackSize
    public int b;               // animationsToGo
    public int c;               // itemID
    private int d;              // itemDamage（private=h10 回归面）
    public ul(int id, int n, int dmg) { c = id; a = n; d = dmg; }
    public ul k() { return new ul(c, a, d); }
}
