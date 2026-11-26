// b1.5_01 ChunkCoordinates=bk 假件（dump.txt L390：public int a/b/c 全 public、
// bk()/bk(III)V/bk(bk)V 三构造；Comparable 面不消费=省略）。
public class bk {
    public int a, b, c;
    public bk() { }
    public bk(int x, int y, int z) { a = x; b = y; c = z; }
    public bk(bk o) { a = o.a; b = o.b; c = o.c; }
}
