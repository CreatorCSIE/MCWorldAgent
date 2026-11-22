// b1.8.1 GuiIngame=abj 假件（详表 §1，uq→abj 断代）：renderGameOverlay=a(FZII)V
// ✓钩形同 uq/pe/mz。2026-11-18 判决：181 ≥a1.2.3 边界线=XYZ 注入免做
// （hudXyzPaint=false，原版 F3 自带坐标）→onHud 仅 HUD_RENDER 观测不绘；链字母留锚数据。
public class abj {
    private net.minecraft.client.Minecraft w;
    public int hud;
    public abj(net.minecraft.client.Minecraft m) { w = m; }
    public void a(float f, boolean b, int i, int j) { hud++; }
}
