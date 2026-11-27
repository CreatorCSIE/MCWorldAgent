// b1.4_01 EntityList=hj 假件（blocks\hj.txt javap -p：public static String b(pb) 查表面
// entityListLookupM=b（static+String 返回+单参判据命中，同 1501 ik 字母）；
// 三 spawn 路 a(String,eb)/a(lg,eb)/a(int,eb)+a(pb)I 不消费=省略；
// 真形 private static Map a-d 假件收敛为 public names 供测试注入）。
public class hj {
    public static final java.util.Map names = new java.util.HashMap();
    public static String b(pb e) {
        return e == null ? null : (String) names.get(e.getClass());
    }
}
