// b1.4_01 ItemStack=hi 假件（blocks\hi.txt javap -p）：public int a=count（isCountF=a）、
// public int b=animationsToGo、public int c=itemId（isIdF=c）、private int d=itemDamage
// （isDmgF=d——private，resolveTe getDeclaredField 修复面）。int 参构造仅 (III)V public
// （arg1→c id、arg2→a count、arg3→d dmg）——无 (II)V（JSON note 同形；newStack dmg=0
// 等价配方前提）；ra/fg/lg 参构造系他型=省略。
public class hi {
    public int a;        // count
    public int b;        // animationsToGo
    public int c;        // itemId
    private int d;       // itemDamage（private！）
    public hi(int id, int count, int dmg) { c = id; a = count; d = dmg; }
}
