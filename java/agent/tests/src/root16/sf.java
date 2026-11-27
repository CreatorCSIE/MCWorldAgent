// b1.6 TileEntityFurnace=sf 假件（blocks\sf.txt javap -p extends os implements ls）：
// public int a=furnaceBurnTime/c=furnaceCookTime/b=cookTicks+private iw[] h=3 槽——
// **与 1401 oy/1501 rg 全同字母三版稳定**（teBurnF=a/teCookF=c）。
// ⚠️ 16 sf=TileEntityFurnace↔1401 sf=Hell 撞车位（边界批 §3 原误行已在 V-5 勘误）。
public class sf extends os implements ls {
    public int a;        // furnaceBurnTime
    public int b;        // cookTicks
    public int c;        // furnaceCookTime
    private iw[] h = new iw[3];
    public sf() { }
    public int a() { return 3; }
    public iw d_(int i) { return h[i]; }
    public void a(int i, iw s) { h[i] = s; }
}
