// b1.6 Item=gk 假件（blocks\gk.txt javap -p：public static gk[] c=ITEMS 注册表
// （itemsF=c）、protected gk(int) 声明构造在位（自注册面省略，测试经反射注入 reg[id]）。
public class gk {
    public static gk[] c = new gk[256];
    protected final int id;
    protected gk(int id) { this.id = id; }
}
