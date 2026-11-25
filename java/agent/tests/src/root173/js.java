// b1.7.3 TileEntityChest=js 假件（h8 批 2026-11-26，真 jar tiny+javap 实形）：
// extends ow implements lw；chestContents=**private iz[] a**（27 槽）——与 teBurnF="a"
// 撞字母但撞的是数组字段（getFurnace int 门命中数组型→not-furnace，非 181 fh/
// pre2 fv 的 boolean 撞形——173 无 numPlayersUsing 字段，javap 全量核）。
public class js extends ow {
    private iz[] a;
    public js() {
        a = new iz[27];
    }
    public int a() { return a.length; }
    public iz f_(int slot) { return a[slot]; }
    public iz a(int slot, int count) { iz s = a[slot]; a[slot] = null; return s; }
    public void a(int slot, iz s) { a[slot] = s; }
}
