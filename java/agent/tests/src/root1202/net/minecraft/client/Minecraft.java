package net.minecraft.client;

// b1.2_02 主类假件（源码级最小件）：真实面为 e:dn theWorld / g:bz thePlayer / u:pe ingameGUI
// + a(dn,String)V loadWorld + i()V runTick（详表 §7，钩形同 a1.2.0）。
// Java 禁止具名包源码引用默认包类型（dn/bz/pe 在默认包），故该实形无法以 .java 表达；
// 五钩实跑所需的实形字节由 InjectSmoke1202Test 用 ASM 合成后经 Transformers 织入、
// parent-last Loader 定义。本类仅保证 pe 假类编译引用。
public class Minecraft {
    public void i() { }
}
