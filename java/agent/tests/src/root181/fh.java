// b1.8.1 TileEntityChest=fh 假件（h7 批 2026-11-26，真 jar tiny+javap 实形）：
// extends ij implements gl；items=private ul[] p=chestContents 27 槽；三方法同 aaa 形。
// ⚠️ public boolean a=numPlayersUsing **撞 teBurnF="a"**（getFurnace int 类型门
// 消费面，pre2 fv 撞名同形复现钉）。
public class fh extends ij {
    public boolean a;
    private ul[] p;
    public fh() {
        p = new ul[27];
    }
    public int a() { return p.length; }
    public ul d_(int i) { return p[i]; }
    public void a(int i, ul s) { p[i] = s; }
}
