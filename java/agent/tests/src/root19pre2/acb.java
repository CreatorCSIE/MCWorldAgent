// b1.9-pre2 TileEntityFurnace=acb 假件（T7 批 2026-11-26，tiny+dump L635 双源）：
// extends je implements hc；public int a=furnaceBurnTime/b=currentItemBurnTime/
// c=furnaceCookTime（**public 非 0327 private f/h**——字母+访问位双断代钉）；
// items d=private wc[]（3 槽 0 料/1 燃/2 产）。IInventory 三方法 public 实形。
public class acb extends je {
    public int a;
    public int b;
    public int c;
    private wc[] d;
    public acb() {
        d = new wc[3];
    }
    public int a() { return d.length; }                 // getSizeInventory
    public wc e_(int i) { return d[i]; }                // getStackInSlot（e_ 尾下划线）
    public void a(int i, wc s) { d[i] = s; }            // setInventorySlotContents
}
