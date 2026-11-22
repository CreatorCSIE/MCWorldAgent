// b1.8.1 InventoryPlayer=ui 假件（详表 §2）：main=a[ul 36]/armor=b[4]/cur=c
// （⚠️ui.d=player/ui.e=inventoryChanged 互换陷阱延续——c 才是 currentItem）。
public class ui {
    public ul[] a = new ul[36];
    public ul[] b = new ul[4];
    public int c;
    public sz d;
    public ui(sz p) { d = p; }
    public boolean a(ul s) {
        for (int i = 0; i < a.length; i++) {
            if (a[i] == null) { a[i] = s; return true; }
        }
        return false;
    }
}
