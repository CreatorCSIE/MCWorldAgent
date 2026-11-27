// b1.6 InventoryPlayer=iu 假件（blocks\iu.txt javap -p implements ls）：public iw[] a=main[36]、
// public iw[] b=armor[4]、public int c=currentItem、public gq d=player 回指——
// **四字母三版全稳定**（1401 hh/1501 ih 同形）；addStack a(Liw;)Z public。
public class iu implements ls {
    public iw[] a = new iw[36];
    public iw[] b = new iw[4];
    public int c;
    public gq d;
    public iu(gq p) { d = p; }
    public int a() { return 36; }
    public iw d_(int i) { return a[i]; }
    public void a(int i, iw s) { a[i] = s; }
    public boolean a(iw s) {                 // addStack 简化模型：首个空槽直放
        for (int i = 0; i < 36; i++) {
            if (a[i] == null) { a[i] = s; return true; }
        }
        return false;
    }
}
