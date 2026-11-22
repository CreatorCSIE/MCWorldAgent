// b1.2_02 InventoryPlayer=gl 假件（详表 §5）：main=a[36]/armor=b[4]/cur=c（⚠a1.2.0 d 左移一位）。
public class gl {
    public gm[] a = new gm[36];
    public gm[] b = new gm[4];
    public int c;
    public boolean d;
    public eu e;
    public gm f;                // 拖拽栈
    public gl(eu p) { e = p; }
    public boolean a(gm s) {
        for (int i = 0; i < a.length; i++) {
            if (a[i] == null) { a[i] = s; return true; }
        }
        return false;
    }
}
