// b1.9-pre2 TileEntityChest=fv 假件（T7 批 2026-11-26，dump L3896 实形）：
// extends je implements hc；items=private wc[] p（27 槽）；三方法 public 同 acb 形。
// ⚠️ public boolean a=numPlayersUsing **撞 teBurnF="a"**（WorldOps.getFurnace
// int 类型门消费面——真件撞名复现钉）。
public class fv extends je {
    public boolean a;
    private wc[] p;
    public fv() {
        p = new wc[27];
    }
    public int a() { return p.length; }
    public wc e_(int i) { return p[i]; }
    public void a(int i, wc s) { p[i] = s; }
}
