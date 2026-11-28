package net.minecraft.client;

// b1.4_01 主类假件（源码级最小件，1501/181/19pre2 同例）：真件字段面
// e:eb theWorld / h:jm thePlayer（声明类型 jm=EntityLiving 同形）/ u:qv
// ingameGUI / y:is gameSettings / p:ox fontRenderer / W:File mcDataDir
// （六项全同 1501 字母，JSON 主类字段面钉）+ a(Leb;Ljava/lang/String;Lfm;)V
// changeWorld 3 参实体 + 2 参委托壳 + j()V runTick（⚠️173=k 断代同 1501
// 字母——j→k 翻面下界=b1.6）。Java 禁 named package 引默认包类型，真形字节
// 由 InjectSmoke1401Test 走 ASM 合成；本类仅保 qv 假件编译引用。
public class Minecraft {
    public void j() { }
}
