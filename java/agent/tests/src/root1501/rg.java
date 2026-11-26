// b1.5_01 TileEntityFurnace=rg 假件（dump.txt L6587 extends nx implements la）：
// public int a=furnaceBurnTime（teBurnF=a）、public int c=furnaceCookTime（teCookF=c）、
// public int b=cookTicks（留形）、private ii[] h=3 槽（JSON note 实锤）。
public class rg extends nx implements la {
    public int a;        // furnaceBurnTime
    public int b;        // cookTicks
    public int c;        // furnaceCookTime
    private ii[] h = new ii[3];
    public rg() { }
    public int a() { return 3; }
    public ii d_(int i) { return h[i]; }
    public void a(int i, ii s) { h[i] = s; }
}
