package net.minecraft.client;

// b1.5_01 主类假件（源码级最小件，19pre2 同例）：真件字段面 e:et theWorld /
// h:kw thePlayer（声明类型=EntityLiving 同 1401 形）/ u:tl ingameGUI / y:jz
// gameSettings / p:rf fontRenderer / W:File mcDataDir（JSON 主类字段面钉）+
// a(Let;Ljava/lang/String;Lgh;)V changeWorld 3 参实体 + 2 参委托壳 + j()V
// runTick（public，⚠️173/181/pre2=k 断代——j→k 翻面下界=b1.6）。
// Java 禁 named package 引默认包类型，真形字节由 InjectSmoke1501Test 走 ASM
// 合成；本类仅保 tl 假件编译引用（181/19pre2 同例）。
public class Minecraft {
    public void j() { }
}
