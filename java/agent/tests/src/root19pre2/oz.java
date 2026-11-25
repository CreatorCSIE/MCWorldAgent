// b1.9-pre2 WorldChunkManager=oz 假件（dump L7466，nu→oz 断代）：
// a(II)Lwz;=getBiome ✓同形（181 nu 同配方，象限布景 Desert/Ice Desert/Plains）。
public class oz {
    private final ti world;
    private final wz plains = new wz();
    private final wz desert = new wz();
    private final wz ice = new wz();
    public oz(ti w) {
        world = w;
        desert.r = "Desert";
        ice.r = "Ice Desert";
    }
    public wz a(int x, int z) {
        if (x >= 64 && z >= 64) { return desert; }
        if (x <= -64 && z <= -64) { return ice; }
        return plains;
    }
}
