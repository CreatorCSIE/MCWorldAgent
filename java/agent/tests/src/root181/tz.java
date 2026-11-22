// b1.8.1 EntityCreature=tz 假件（详表 §2，ii→tz 断代）。
// el181-1 活体判决：真件 **public abstract class tz extends wd**（javap 实锤，b1.8-pre1-2 起
// EntityCreature 抽象化=类树断代；173 ii=concrete 故 173 活体 mob OK）——假件复刻真面纪律
// （173 bug①教训沿用），abstract 不得放宽。mob 槽已从表摘除。
public abstract class tz extends wd {
    public tz(rv w) { super(w); } // 真件 javap=public tz(rv)（abstract 类构造仍 public）
}
