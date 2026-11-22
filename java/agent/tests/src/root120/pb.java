public class pb {
    private final cu world;
    public fy[] d;
    private final fy plains = new fy();
    private final fy desert = new fy();
    private final fy ice = new fy();
    protected pb() {
        this.world = null;
        desert.m = "Desert";
        ice.m = "Ice Desert";
    }
    public pb(cu w) {
        world = w;
        desert.m = "Desert";
        ice.m = "Ice Desert";
        d = new fy[] { plains, desert, ice };
    }
    public fy a(int x, int z) {
        if (x >= 64 && z >= 64) { return desert; }
        if (x <= -64 && z <= -64) { return ice; }
        return plains;
    }
    public fy[] a(int x, int z, int w2, int h2) {
        fy[] r = new fy[w2 * h2];
        for (int i = 0; i < r.length; i++) { r[i] = a(x, z); }
        return r;
    }
    public double b(int x, int z) { return 0.0; }
}
