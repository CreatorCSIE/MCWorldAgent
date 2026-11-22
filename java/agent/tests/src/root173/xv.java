// b1.7.3 WorldChunkManager=xv 假件（详表 §8）：a(II)Lkd; getBiomeAt ✓同形 ro/pb。
public class xv {
    private final fd world;
    public kd[] d;
    private final kd plains = new kd();
    private final kd desert = new kd();
    private final kd ice = new kd();
    protected xv() {
        this.world = null;
        desert.n = "Desert";
        ice.n = "Ice Desert";
    }
    public xv(fd w) {
        world = w;
        desert.n = "Desert";
        ice.n = "Ice Desert";
        d = new kd[] { plains, desert, ice };
    }
    public kd a(int x, int z) {
        if (x >= 64 && z >= 64) { return desert; }
        if (x <= -64 && z <= -64) { return ice; }
        return plains;
    }
}
