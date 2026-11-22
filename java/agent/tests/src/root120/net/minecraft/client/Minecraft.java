package net.minecraft.client;

// a1.2.0 主类假件（源码级最小件）：真实面为 e:cu theWorld / g:bo thePlayer / u:mz ingameGUI
// + a(cu,String)V loadWorld + i()V runTick（sig_mc.txt）。
// Java 禁止具名包源码引用默认包类型（cu/bo/mz 在默认包），故该实形无法以 .java 表达；
// 五钩实跑所需的实形字节由 InjectSmoke120Test 用 ASM 合成（与 sig_mc 逐描述符对榫）后经
// Transformers 织入、parent-last Loader 定义。本类仅保证 mz/jq 假类编译引用。
public class Minecraft {
    public void i() { }
}
