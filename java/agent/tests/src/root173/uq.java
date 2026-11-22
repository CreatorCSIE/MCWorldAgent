// b1.7.3 GuiIngame=uq 假件（详表 §6/§9）：renderGameOverlay=a(FZII)V ✓钩形同 pe/mz。
// 2026-11-18 判决：173 ≥a1.2.3 边界线=XYZ 注入免做（hudXyzPaint=false，原版 F3 自带坐标）
// →onHud 仅 HUD_RENDER 观测不绘（InjectSmoke173Test 实钉）；链字母 h/q/z/B 留锚数据。
public class uq {
    private net.minecraft.client.Minecraft w;
    public int hud;
    public uq(net.minecraft.client.Minecraft m) { w = m; }
    public void a(float f, boolean b, int i, int j) { hud++; }
}
