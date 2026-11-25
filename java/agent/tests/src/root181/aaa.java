// b1.8.1 TileEntityFurnace=aaa 假件（h7 批 2026-11-26，真 jar tiny+javap 实形）：
// extends ij implements gl；public int a=furnaceBurnTime / b=currentItemBurnTime /
// c=furnaceCookTime；private ul[] d=items 3 槽；IInventory 三方法 a()I/d_(I)Lul/a(ILul;)V。
// 访问位复刻纪律：数组 private，槽读写全走方法面（WorldOps getDeclaredField 仅 burn/cook）。
public class aaa extends ij {
    public int a;
    public int b;
    public int c;
    private ul[] d;
    public aaa() {
        d = new ul[3];
    }
    public int a() { return d.length; }
    public ul d_(int i) { return d[i]; }
    public void a(int i, ul s) { d[i] = s; }
}
