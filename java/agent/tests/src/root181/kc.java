// b1.8.1 FoodStats=kc 假件（FoodStats.java 直读）：四字段全 private（L4-7），
// a=foodLevel(int)/b=saturation(float)/c=exhaustion(float)/d=tickTimer(int)。
// WorldOps resolveHungerFace 走 getDeclaredField+setAccessible 写 a/b。
public class kc {
    private int a = 20;
    private float b = 5.0F;
    private float c;
    private int d;
}
