// b1.5_01 TileEntityChest=iy 假件（dump.txt L3563 extends nx implements la）：
// private ii[] a=chestContents 27 槽（JSON note：槽字母=a 非 181 的 p——严禁互抄；
// iy 无 public int a，getFurnace int 类型门天然区分 chest/furnace）。
public class iy extends nx implements la {
    private ii[] a = new ii[27];
    public iy() { }
    public int a() { return 27; }
    public ii d_(int i) { return a[i]; }
    public void a(int i, ii s) { a[i] = s; }
}
