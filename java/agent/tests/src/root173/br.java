// b1.7.3 PairCoordinate=br 假件（h10 批）：a/b/c 三 int 床坐标载体——
// gs.v:Lbr;=bedChunkCoordinates，tick 睡眠段 am() 裸解引用面（崩溃根因）。
public class br {
    public int a;
    public int b;
    public int c;
    public br(int x, int y, int z) { a = x; b = y; c = z; }
}
