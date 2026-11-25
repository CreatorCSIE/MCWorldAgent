import java.util.HashMap;
import java.util.Map;

// b1.9-pre2 EntityList=wd 假件（dump L10606，jc→um→wd 断代；⚠️ 撞车位：
// 181 wd=EntityLiving↔pre2 wd=EntityList——root19pre2 独立编译目录隔离，
// 跨目录借字母严禁）。public static b(lh)String=lookup ✓同 181 um.b 字母；
// a(lh)I=getId 留档；names 表=假件便利（真件 c:Map 私有，op 不直读）。
public class wd {
    public static Map names = new HashMap();
    public static int a(lh e) { return 0; }
    public static String b(lh e) {
        Object n = names.get(e.getClass());
        return n == null ? e.getClass().getSimpleName() : (String) n;
    }
}
