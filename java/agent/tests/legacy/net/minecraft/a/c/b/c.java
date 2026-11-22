package net.minecraft.a.c.b;

public class c extends mcaut.MiniJsonTest.FakeEntity {
    public c(mcaut.MiniJsonTest.FakeWorld w) { super(w); }

    /** getEntityType 覆写（entitylist kind 差异化用例）。 */
    public String q() {
        return "Pig";
    }
}
