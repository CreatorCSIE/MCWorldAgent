// b1.5_01 EntityList=ik 假件（dump.txt L3345：public static String b(rj) 查表面
// entityListLookupM=b；真形 private static Map a-d 假件收敛为 public names 供测试注入）。
public class ik {
    public static final java.util.Map names = new java.util.HashMap();
    public static String b(rj e) {
        return e == null ? null : (String) names.get(e.getClass());
    }
}
