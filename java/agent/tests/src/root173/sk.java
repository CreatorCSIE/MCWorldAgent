// b1.7.3 TileEntityFurnace=sk 假件（h8 批 2026-11-26，真 jar tiny+javap 实形）：
// extends ow implements lw；public int a=furnaceBurnTime / b=currentItemBurnTime /
// c=furnaceCookTime；private iz[] i=items 3 槽；IInventory a()I/f_(I)Liz/a(II)Liz/a(ILiz)V。
public class sk extends ow {
    public int a;
    public int b;
    public int c;
    private iz[] i;
    public sk() {
        i = new iz[3];
    }
    public int a() { return i.length; }
    public iz f_(int slot) { return i[slot]; }
    public iz a(int slot, int count) { iz s = i[slot]; i[slot] = null; return s; }
    public void a(int slot, iz s) { i[slot] = s; }
}
