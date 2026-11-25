// b1.9-pre2 InventoryPlayer=vz 假件（dump L10474，ix→ui→vz 断代）：
// main=a(wc[36])/armor=b(wc[4])/cur=c(int) ✓全同 181 ui 字母；
// ⚠️ d=player:uo / e=inventoryChanged:boolean 互换陷阱延续（ui 注同款）；
// a(wc)Z=addStack ✓（181 ui.a 同形）。
public class vz {
    public wc[] a = new wc[36];
    public wc[] b = new wc[4];
    public int c;
    public uo d;
    public vz(uo p) { d = p; }
    public boolean a(wc s) {
        for (int i = 0; i < a.length; i++) {
            if (a[i] == null) { a[i] = s; return true; }
        }
        return false;
    }
}
