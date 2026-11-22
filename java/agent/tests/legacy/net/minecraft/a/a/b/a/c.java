package net.minecraft.a.a.b.a;

/** TileEntityChest（a.a.b.a.c）替身：27 槽，IInventory 三方法直落实例类（不 implements——javac 结构限制案）。 */
public class c {
    public mcaut.MiniJsonTest.FakeStack[] items = new mcaut.MiniJsonTest.FakeStack[27];

    public final int a() {
        return items.length;
    }

    public final mcaut.MiniJsonTest.FakeStack a(int i) {
        return items[i];
    }

    public final void a(int i, mcaut.MiniJsonTest.FakeStack s) {
        items[i] = s;
    }
}
