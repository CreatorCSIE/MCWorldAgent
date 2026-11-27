// b1.6 EnumStatus=cu 假件（blocks\cu.txt javap -p：public final class cu extends
// java.lang.Enum，五常量 a-e）：a=OK（sleepOkF=a）、b=provider 禁睡（fb.t.wr.c）、
// c=白天（fb.f()Z）、d=TOO_FAR（3/2/3）、e=已睡/不可睡（N()Z/!W()Z）。
// 必须真 enum——WorldOps sleep 面 getDeclaredMethod("b",III) 返回型 isEnum() 门。
public enum cu {
    a, b, c, d, e
}
