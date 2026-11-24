// b1.8.1 EntityPlayer=sz 假件（详表 §2）：inventory=as（⚠️b1.7.3=c）。
// D10 xp 面（EntityPlayer.java 直读）：aX="Xp"/aY="XpLevel"/aZ="XpTotal"/aG="XpCooldown"/ay=score。
public class sz extends wd {
    public ui as;
    public int aX;   // NBT "Xp"
    public int aY;   // NBT "XpLevel"
    public int aZ;   // NBT "XpTotal"
    public int aG;   // NBT "XpCooldown"
    public int ay;   // score
    public sz(rv w) { super(w); as = new ui(this); }
}
