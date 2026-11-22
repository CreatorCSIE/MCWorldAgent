import java.util.HashMap;
import java.util.Map;

public class fj {
    public static Map names = new HashMap();
    public static int a(lk e) { return 0; }
    public static String b(lk e) {
        Object n = names.get(e.getClass());
        return n == null ? e.getClass().getSimpleName() : (String) n;
    }
}
