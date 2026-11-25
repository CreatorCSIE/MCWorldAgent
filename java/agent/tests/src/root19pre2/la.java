// b1.9-pre2 FoodStats=la 假件（dump L5723，tiny L4896 块）：字段面 **全 private**
// （181 kc 同形同字母）——a=foodLevel:I（默认 20）/ b=foodSaturationLevel:F（默认 5.0）/
// c=foodExhaustionLevel:F / d=foodTickTimer:I / e=prevFoodLevel:I（留档）。
// WorldOps.setHunger/getHunger 经 uo.av() 取实例后 getDeclaredField+setAccessible。
public class la {
    private int a = 20;
    private float b = 5.0f;
    private float c;
    private int d;
    private int e;
}
