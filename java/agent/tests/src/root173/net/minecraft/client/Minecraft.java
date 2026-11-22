package net.minecraft.client;

// b1.7.3 主类假件（源码级最小件）：真实面为 f:fd theWorld / h:gs thePlayer /
// v:uq ingameGUI / z:kv gameSettings / Z:String mcDataDir（详表 §7）
// + a(fd,String)V loadWorld + k()V runTick（⚠️字母断代 i→k，钩面 RUN_TICK）。
// Java 禁止具名包源码引用默认包类型（fd/gs/uq 在默认包），故该实形无法以 .java 表达；
// 四项钩实跑所需实形字节由 InjectSmoke173Test 用 ASM 合成后经 Transformers 织入、
// parent-last Loader 定义。本类仅保证 uq 假类编译引用。
public class Minecraft {
    public void k() { }
}
