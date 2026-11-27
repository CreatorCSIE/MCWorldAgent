// b1.6 IInventory=ls 假件（blocks\ls.txt javap -p interface）：a()I=getSizeInventory、
// **d_(I)Liw;**=getStackInSlot（invGetM=d_——⚠️同 1501 la.d_；1401 jp.c_ 断代，
// 尾下划线链 1301 c_→1401 c_→1501/16 d_，严禁按 1401 抄 c_）、a(ILiw;)V=setInventorySlotContents。
public interface ls {
    int a();
    iw d_(int i);
    void a(int i, iw s);
}
