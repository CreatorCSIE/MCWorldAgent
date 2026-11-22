// b1.7.3 InventoryPlayer=ix 假件（详表 §5）：main=a[36]/armor=b[4]/cur=c
// （⚠️ix.d=player/ix.e=inventoryChanged 互换陷阱——c 才是 currentItem）。
public class ix {
    public iz[] a = new iz[36];
    public iz[] b = new iz[4];
    public int c;
    public gs d;
    public ix(gs p) { d = p; }
    public boolean a(iz s) {
        for (int i = 0; i < a.length; i++) {
            if (a[i] == null) { a[i] = s; return true; }
        }
        return false;
    }
}
