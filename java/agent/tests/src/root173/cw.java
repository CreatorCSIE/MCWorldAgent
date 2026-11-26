// b1.7.3 EnumStatus=cw 假件（h10 批，javap 实锤 extends java.lang.Enum 五常量）：
// a=OK（sleepInBedAt 成功尾返）、b=provider 禁睡（fd.t.xa.c）、c=白天（fd.f()Z）、
// d=TOO_FAR（>3/2/3）、e=已睡/不可睡（N()Z||!W()Z）。WorldOps 比对用引用相等
// （sleepOkF=a），isEnum() 返回型门——必须真 enum 建模。
public enum cw {
    a, b, c, d, e
}
