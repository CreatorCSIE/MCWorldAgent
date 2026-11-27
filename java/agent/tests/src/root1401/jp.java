// b1.4_01 IInventory=jp 假件（blocks\jp.txt javap -p interface）：a()I=getSizeInventory
// （invSizeM=a）、c_(I)Lhi;=getStackInSlot（invGetM=c_——⚠️尾下划线链 1301 c_→1401 c_→
// 1501 d_，严禁按 1501 抄 d_）、a(ILhi;)V=setInventorySlotContents（invSetM=a）。
// 其余原版方法（a(II)Lhi;/c()String/d()I/x_()V/a_(fm)Z）不消费=省略。
public interface jp {
    int a();
    hi c_(int i);
    void a(int i, hi s);
}
