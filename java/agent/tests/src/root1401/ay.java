// b1.4_01 ChunkCoordinates=ay 假件（blocks\ay.txt javap -p：public int a/b/c 全 public、
// ay()/ay(III)V/ay(ay)V 三构造；Comparable 面不消费=省略。1501=bk 断代字母）。
public class ay {
    public int a, b, c;
    public ay() { }
    public ay(int x, int y, int z) { a = x; b = y; c = z; }
    public ay(ay o) { a = o.a; b = o.b; c = o.c; }
}
