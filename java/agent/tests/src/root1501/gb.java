// b1.5_01 Item=gb 假件（dump.txt L2323：public static gb[] c=ITEMS 注册表（itemsF=c）、
// protected gb(int) 声明构造在位（自注册面假件省略，测试经反射注入 reg[id]）。
public class gb {
    public static gb[] c = new gb[256];
    protected final int id;
    protected gb(int id) { this.id = id; }
}
