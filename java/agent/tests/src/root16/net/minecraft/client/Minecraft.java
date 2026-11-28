package net.minecraft.client;

// b1.6 主类假件（源码级最小件，1501/1401 同例）：真件字段面 f:fb theWorld
// （⚠️1401/1501=e 断代）/ h:da thePlayer（声明类型=da=EntityPlayerSP——
// 1401/1501 声明 EntityLiving 断代）/ v:uk ingameGUI / z:kr gameSettings /
// q:se fontRenderer / Z:File mcDataDir（六项 vs 1401/1501 全洗牌，JSON 主类
// 字段面钉）+ a(Lfb;Ljava/lang/String;Lgq;)V changeWorld 3 参实体 + 2 参
// 委托壳（@3 aconst_null 实锤）+ k()V runTick（public，⚠️1401/1501=j——
// j→k 翻面下界=b1.6，173/181/pre2=k 同字母；fb.l()V 调用点 @1260）。
// Java 禁 named package 引默认包类型，真形字节由 InjectSmoke16Test 走 ASM
// 合成；本类仅保 uk 假件编译引用（181/19pre2/1501/1401 同例）。
public class Minecraft {
    public void k() { }
}
