// b1.6 ChunkCoordinates=bp 假件（blocks\bp.txt javap -p：public int a/b/c、
// bp()/bp(III)V/bp(bp)V 三构造；Comparable 面不消费=省略。1401=ay/1501=bk 断代字母）。
public class bp {
    public int a, b, c;
    public bp() { }
    public bp(int x, int y, int z) { a = x; b = y; c = z; }
    public bp(bp o) { a = o.a; b = o.b; c = o.c; }
}
