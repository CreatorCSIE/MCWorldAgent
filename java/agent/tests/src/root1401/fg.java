// b1.4_01 Item=fg 假件（blocks\fg.txt javap -p：public static fg[] c=ITEMS 注册表
// （itemsF=c）、protected fg(int) 声明构造在位（自注册面假件省略，测试经反射注入
// reg[id]；id 槽=public final int bd——不消费=省略）。⚠️ 1401 fg=Item↔1501 fg=Skeleton
// 撞车位，严禁互抄。
public class fg {
    public static fg[] c = new fg[256];
    protected final int id;
    protected fg(int id) { this.id = id; }
}
