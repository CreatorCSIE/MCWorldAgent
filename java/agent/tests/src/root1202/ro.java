// b1.2_02 WorldChunkManager=ro 假件（详表 §8）：a(II)Lhf; func_4073_a ✓同形 pb。
public class ro {
    private final dn world;
    public hf[] d;
    private final hf plains = new hf();
    private final hf desert = new hf();
    private final hf ice = new hf();
    protected ro() {
        this.world = null;
        desert.m = "Desert";
        ice.m = "Ice Desert";
    }
    public ro(dn w) {
        world = w;
        desert.m = "Desert";
        ice.m = "Ice Desert";
        d = new hf[] { plains, desert, ice };
    }
    public hf a(int x, int z) {
        if (x >= 64 && z >= 64) { return desert; }
        if (x <= -64 && z <= -64) { return ice; }
        return plains;
    }
    public hf[] a(int x, int z, int w2, int h2) {
        hf[] r = new hf[w2 * h2];
        for (int i = 0; i < r.length; i++) { r[i] = a(x, z); }
        return r;
    }
}
