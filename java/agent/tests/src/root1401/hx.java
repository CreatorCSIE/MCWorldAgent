// b1.4_01 TileEntityChest=hx 假件（blocks\hx.txt javap -p extends ma implements jp）：
// private hi[] a=chestContents 27 槽（JSON note：槽字母=a 同 1501 iy；hx 无 public int a
// →getFurnace int 类型门天然区分 chest/furnace 同形）。
public class hx extends ma implements jp {
    private hi[] a = new hi[27];
    public hx() { }
    public int a() { return 27; }
    public hi c_(int i) { return a[i]; }
    public void a(int i, hi s) { a[i] = s; }
}
