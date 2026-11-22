import java.util.HashMap;
import java.util.Map;

// b1.7.3 EntityList=jc 假件（详表 §4）：static b(sn)String ✓同字母；注册表 26 项
// （Giant=nt:53 存活、XPOrb 未生）。
public class jc {
    public static Map names = new HashMap();
    public static int a(sn e) { return 0; }
    public static String b(sn e) {
        Object n = names.get(e.getClass());
        return n == null ? e.getClass().getSimpleName() : (String) n;
    }
}
