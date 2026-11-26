// b1.5_01 ItemStack=ii 假件（dump.txt L3287 javap -p）：public int a=count（isCountF=a）、
// public int b=animationsToGo、public int c=itemId（isIdF=c）、private int d=itemDamage
// （isDmgF=d——private，setchest getDeclaredField 修复面）。构造仅 (III)V public
// （arg1→c id、arg2→a count、arg3→d dmg）——无 (II)V（newStack dmg=0 等价配方前提）。
public class ii {
    public int a;        // count
    public int b;        // animationsToGo
    public int c;        // itemId
    private int d;       // itemDamage（private！）
    public ii(int id, int count, int dmg) { c = id; a = count; d = dmg; }
}
