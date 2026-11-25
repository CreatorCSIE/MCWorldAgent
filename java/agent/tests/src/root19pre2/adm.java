// b1.9-pre2 GuiIngame=adm 假件（dump L1086，uq→abj→adm 断代；⚠️ 撞车位：
// 173 uq=GuiIngame↔pre2 uq=EntityMob）：renderGameOverlay=a(FZII)V public
// ✓hook 形态同族（173 kv.a/181 abj.a 同字母）；构造 (Minecraft)V（dump L1098）。
// ≥a1.2.3 边界线=XYZ 注入免做（hudXyzPaint=false），onHud 仅观测不绘制。
public class adm {
    private net.minecraft.client.Minecraft w;
    public int hud;
    public adm(net.minecraft.client.Minecraft m) { w = m; }
    public void a(float f, boolean b, int i, int j) { hud++; }
}
