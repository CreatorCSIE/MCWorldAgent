// b1.4_01 TileEntityFurnace=oy 假件（blocks\oy.txt javap -p extends ma implements jp）：
// public int a=furnaceBurnTime（teBurnF=a）、public int c=furnaceCookTime（teCookF=c）、
// public int b=cookTicks（留形）、private hi[] h=3 槽（tiny 命名全中，与 1501 rg 全同字母）。
public class oy extends ma implements jp {
    public int a;        // furnaceBurnTime
    public int b;        // cookTicks
    public int c;        // furnaceCookTime
    private hi[] h = new hi[3];
    public oy() { }
    public int a() { return 3; }
    public hi c_(int i) { return h[i]; }
    public void a(int i, hi s) { h[i] = s; }
}
