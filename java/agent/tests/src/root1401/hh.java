// b1.4_01 InventoryPlayer=hh 假件（blocks\hh.txt javap -p implements jp）：public hi[] a=main[36]
// （invMainF=a）、public hi[] b=armor[4]（invArmorF=b）、public int c=currentItem
// （invCurF=c）、public fm d=player 回指——四字母全同 1501 ih（JSON note 钉）；
// addStack a(Lhi;)Z public（addStackM=a）。
public class hh implements jp {
    public hi[] a = new hi[36];
    public hi[] b = new hi[4];
    public int c;
    public fm d;
    public hh(fm p) { d = p; }
    public int a() { return 36; }
    public hi c_(int i) { return a[i]; }
    public void a(int i, hi s) { a[i] = s; }
    public boolean a(hi s) {                 // addStack 简化模型：首个空槽直放
        for (int i = 0; i < 36; i++) {
            if (a[i] == null) { a[i] = s; return true; }
        }
        return false;
    }
}
