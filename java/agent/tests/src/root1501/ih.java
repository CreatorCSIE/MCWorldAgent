// b1.5_01 InventoryPlayer=ih 假件（dump.txt L3248 implements la）：public ii[] a=main[36]
// （invMainF=a）、public ii[] b=armor[4]（invArmorF=b）、public int c=currentItem
// （invCurF=c）、public gh d=player 回指；addStack a(Lii;)Z public（addStackM=a）。
public class ih implements la {
    public ii[] a = new ii[36];
    public ii[] b = new ii[4];
    public int c;
    public gh d;
    public ih(gh p) { d = p; }
    public int a() { return 36; }
    public ii d_(int i) { return a[i]; }
    public void a(int i, ii s) { a[i] = s; }
    public boolean a(ii s) {                 // addStack 简化模型：首个空槽直放
        for (int i = 0; i < 36; i++) {
            if (a[i] == null) { a[i] = s; return true; }
        }
        return false;
    }
}
