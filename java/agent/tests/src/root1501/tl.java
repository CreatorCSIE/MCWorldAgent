// b1.5_01 GuiIngame=tl 假件（A2Table1501Test note：uq→tl 断代）：
// renderGameOverlay=a(FZII)V public ✓hook 形态同族（173 uq.a/181 abj.a/
// pre2 adm.a 同字母）；构造 (Minecraft)V。jz.B=showDebugInfo 门读面
// （@117 A=thirdPersonView/@1142 B=showDebugInfo，偏移与 173 逐位同形——
// 同字母异名陷阱：非 173 kv.B=showFPS 语义）。
// ≥a1.2.3 边界线=XYZ 注入免做（hudXyzPaint=false），onHud 仅观测不绘制。
public class tl {
    private net.minecraft.client.Minecraft w;
    public int hud;
    public tl(net.minecraft.client.Minecraft m) { w = m; }
    public void a(float f, boolean b, int i, int j) { hud++; }
}
