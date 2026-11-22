import java.util.HashMap;
import java.util.Map;

// b1.2_02 EntityList=gn 假件（详表 §4）：static b(nl)String ✓同字母；a(nl)int getEntityID。
public class gn {
    public static Map names = new HashMap();
    public static int a(nl e) { return 0; }
    public static String b(nl e) {
        Object n = names.get(e.getClass());
        return n == null ? e.getClass().getSimpleName() : (String) n;
    }
}
