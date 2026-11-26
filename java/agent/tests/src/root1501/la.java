// b1.5_01 IInventory=la 假件（dump.txt L4510 interface）：a()I=getSizeInventory
// （invSizeM=a）、d_(I)Lii;=getStackInSlot（invGetM=d_）、a(ILii;)V=
// setInventorySlotContents（invSetM=a）。其余原版方法不消费=省略。
public interface la {
    int a();
    ii d_(int i);
    void a(int i, ii s);
}
