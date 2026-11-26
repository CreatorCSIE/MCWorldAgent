// b1.7.3 ItemStack=iz 假件（h10 批翻案修正 §5 旧述"全 public"误记）：
// a=stackSize/c=itemID **public**、**d=itemDamage private**（真 jar javap 实锤——
// 活体 setchest NoSuchFieldException: d 根因，旧假件 public 掩盖断代）；
// b=animationsToGo public。仅 (III)V public 构造（**无 (II)V**——173 javap 实锤，
// WorldOps resolveTe isCtor2 容忍 null + newStack (III)V dmg=0 等价配方）。
public final class iz {
    public int a;               // stackSize
    public int b;               // animationsToGo
    public int c;               // itemID
    private int d;              // itemDamage（private=h10 回归面）
    public iz(int id, int n, int dmg) { c = id; a = n; d = dmg; }
    public int dmg() { return d; }   // 仅假件测试自证用（真 iz 为 b()/h() 族，不建模）
    public iz k() { return new iz(c, a, d); }
}
