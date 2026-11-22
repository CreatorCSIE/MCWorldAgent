package net.minecraft.a.b;

/**
 * Item 注册表（net.minecraft.a.b.i）替身：public static i[] b 按 id 索引，
 * 含 null 空洞槽（0327 真实注册表有未使用 id——give 必须拒 null 洞/越界）。
 * 假散布器类 net.minecraft.a.a.c.b.b 同法上测试 classpath。
 */
public class i {
    public int ar;
    public static i[] b = new i[256];

    static {
        int[] registered = {1, 4, 5, 17, 35};
        for (int n = 0; n < registered.length; n++) {
            b[registered[n]] = new i();
            b[registered[n]].ar = registered[n];
        }
    }
}
