package net.minecraft.a.a.b.a;

/** TileEntityFurnace（a.a.b.a.b）替身：3 槽 + private f=BurnTime/h=CookTime（getDeclaredField 面同真）。 */
public class b {
    public mcaut.MiniJsonTest.FakeStack[] items = new mcaut.MiniJsonTest.FakeStack[3];
    private int f;
    private int h;

    public final int a() {
        return items.length;
    }

    public final mcaut.MiniJsonTest.FakeStack a(int i) {
        return items[i];
    }

    public final void a(int i, mcaut.MiniJsonTest.FakeStack s) {
        items[i] = s;
    }

    /** 测试辅助：直写计时字段（模拟 updateEntity 点燃/冶炼）。 */
    public void setTimers(int burn, int cook) {
        this.f = burn;
        this.h = cook;
    }
}
