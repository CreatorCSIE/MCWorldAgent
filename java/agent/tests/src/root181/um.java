import java.util.HashMap;
import java.util.Map;

// b1.8.1 EntityList=um 假件（详表 §5，jc→um 断代；⚠️ b1.8.1 jc=RegionFile 撞车位）：
// static b(kj)String=lookup ✓同字母；注册表 26 项活体批复核（Giant=hv:53 预期存活）。
public class um {
    public static Map names = new HashMap();
    public static int a(kj e) { return 0; }
    public static String b(kj e) {
        Object n = names.get(e.getClass());
        return n == null ? e.getClass().getSimpleName() : (String) n;
    }
}
