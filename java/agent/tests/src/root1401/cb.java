// b1.4_01 EnumStatus=cb 假件（blocks\cb.txt javap -p：public final class cb extends
// java.lang.Enum，五常量 a-e 实锤）：a=OK（sleepOkF=a）、b=provider 禁睡（eb.m.sy.c）、
// c=白天（eb.f()Z）、d=TOO_FAR（3/2/3）、e=已睡/不可睡（M()Z/!U()Z）。
// 必须真 enum——WorldOps sleep 面 getDeclaredMethod("b",III) 返回型 isEnum() 门。
public enum cb {
    a, b, c, d, e
}
