// b1.9-pre2 Item=uk 假件（dump L9682，gm→sv→uk 断代）：注册表
// public static uk[] e（⚠️181=sv.f——itemsF c→e 断代钉）；构造 protected(I)V
// 面不消费（give 只校验 e[id] 非 null）。
public class uk {
    public static uk[] e = new uk[512];
    public final int id;
    public uk(int id) { this.id = id; }   // 真件构造 protected（Item ctor 历代 protected 面）——
    // 假件放宽 public：测试须造注册表条目（181 sv public 同例，WorldOps 只校验 e[id] 非 null）
}
