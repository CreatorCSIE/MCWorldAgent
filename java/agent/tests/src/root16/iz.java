// b1.6 EntityList=iz 假件（blocks\iz.txt javap -p：public static String b(si) 查表面
// entityListLookupM=b——**字母 b 三版稳定**（1401 hj/1501 ik 同形）。
// ⚠️ 16 iz=EntityList↔1401 iz=Render 系撞车位。真形 private static Map 假件收敛 names。
public class iz {
    public static final java.util.Map names = new java.util.HashMap();
    public static String b(si e) {
        return e == null ? null : (String) names.get(e.getClass());
    }
}
