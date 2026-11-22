// b1.8.1 WorldChunkManager=nu 假件（详表 §2，xv→nu 断代）：a(II)Lvh; getBiomeAt ✓同形。
public class nu {
    private final rv world;
    public vh[] d;
    private final vh plains = new vh();
    private final vh desert = new vh();
    private final vh ice = new vh();
    protected nu() {
        this.world = null;
        desert.l = "Desert";
        ice.l = "Ice Desert";
    }
    public nu(rv w) {
        world = w;
        desert.l = "Desert";
        ice.l = "Ice Desert";
        d = new vh[] { plains, desert, ice };
    }
    public vh a(int x, int z) {
        if (x >= 64 && z >= 64) { return desert; }
        if (x <= -64 && z <= -64) { return ice; }
        return plains;
    }
}
