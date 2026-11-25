// b1.9-pre2 EntityMob=uq 假件（**abstract**，dump L10049 extends vp）——
// ⚠️ 撞车位：173 uq=GuiIngame↔pre2 uq=EntityMob（forClass("uq") 零钩断言面）。
// 中间层不实例化，仅 ip（EntityGiantZombie）extends 链保真。
public abstract class uq extends vp {
    protected uq(ti w) { super(w); }
}
