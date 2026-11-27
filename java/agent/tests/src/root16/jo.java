// b1.6 TileEntityChest=jo 假件（blocks\jo.txt javap -p extends os implements ls）：
// private iw[] a=chestContents 27 槽（同 1401 hx/1501 iy——槽字母 a 三版稳定）；
// jo 无 public int a→getFurnace int 类型门天然区分 chest/furnace 三版同形。
public class jo extends os implements ls {
    private iw[] a = new iw[27];
    public jo() { }
    public int a() { return 27; }
    public iw d_(int i) { return a[i]; }
    public void a(int i, iw s) { a[i] = s; }
}
