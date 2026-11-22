package net.minecraft.client;

// b1.8.1 主类假件（源码级最小件）：真实面为 f:rv theWorld / h:sz thePlayer /
// v:abj ingameGUI / z:fv gameSettings / Z:String mcDataDir（详表 §1，MC dump 与
// b1.8 字节级全同）+ a(rv,String)V loadWorld + k()V runTick。
// Java 禁止具名包源码引用默认包类型（rv/sz/abj 在默认包），故该实形无法以 .java 表达；
// 四项钩实跑所需实形字节由 InjectSmoke181Test 用 ASM 合成后经 Transformers 织入、
// parent-last Loader 定义。本类仅保证 abj 假类编译引用。
public class Minecraft {
    public void k() { }
}
