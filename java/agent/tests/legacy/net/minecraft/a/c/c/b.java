package net.minecraft.a.c.c;

public class b extends mcaut.MiniJsonTest.FakeEntity {
    public mcaut.MiniJsonTest.FakeStack stack;

    public b(mcaut.MiniJsonTest.FakeWorld w, double x, double y, double z,
            mcaut.MiniJsonTest.FakeStack s) {
        super(w);
        this.stack = s;
        // EntityItem 构造语义：setPosition(x,y,z)（g/h/i 直写，无眼偏移）
        this.g = x;
        this.h = y;
        this.i = z;
    }
}
