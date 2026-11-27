// b1.6 ItemStack=iw 假件（blocks\iw.txt javap -p）：public int a=count（isCountF=a）、
// public int b、public int c=itemId（isIdF=c）、**private int d=itemDamage**（isDmgF=d——
// 四版伤害字段全 private 链）。int 参构造仅 (III)V public（arg1→c id/arg2→a count/
// arg3→d dmg）——**无 (II)V**（V-5 javap 实锤，newStack dmg=0 等价配方前提）；
// ra/gk 参构造系他型=省略。⚠️ 16 iw=ItemStack↔1501 iw=GuiIngame 撞车位。
public class iw {
    public int a;        // count
    public int b;        // animationsToGo
    public int c;        // itemId
    private int d;       // itemDamage（private！）
    public iw(int id, int count, int dmg) { c = id; a = count; d = dmg; }
}
