// b1.9-pre2 EntityPlayerSP=sd 假件（**concrete**，dump L8619 extends uo）——活体玩家
// 真身（uo abstract 不可直造，FakeOps19pre2 布景用本类；acp=EntityClientPlayerMP
// extends sd 链不建模，零消费面）。
public class sd extends uo {
    // 活体批复现（W19P2 实跑 ERROR 实锤）：真 pre2 EntityPlayer.a=mn **public 非 int**，
    // 遮蔽 xx.a=health（private int）——healthField/livingClass 旧版 getField/裸
    // getDeclaredField 均误中本字段致 getInt 崩。假件以 String 模拟非 int 遮蔽。
    public String a;
    public sd(ti w) { super(w); }
    public void r() { }
}
