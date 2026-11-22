// [B5-ARCHIVE 2026-11-22] headless test archived from %TEMP%\mcaut (source batch: F1 anchor registry equality); run via java/agent/tests/run.ps1
import com.mcworldagent.agent.VersionRegistry;
import com.mcworldagent.agent.VersionTable;
import java.lang.reflect.Field;

/** A1 等值断言：Registry 全字段 + WorldOps private 常量反射读值 == 迁移前快照（防表引用手滑）。 */
public class RegistryEqualsTest {
    static int fails = 0;
    static void eq(String name, String got, String want) {
        if (!want.equals(got)) { fails++; System.out.println("FAIL " + name + " got=" + got + " want=" + want); }
    }
    static void eqi(String name, int got, int want) {
        if (got != want) { fails++; System.out.println("FAIL " + name + " got=" + got + " want=" + want); }
    }
    static String wo(String fieldName) throws Exception {
        Field f = Class.forName("com.mcworldagent.agent.WorldOps").getDeclaredField(fieldName);
        f.setAccessible(true);
        return String.valueOf(f.get(null));
    }
    public static void main(String[] a) throws Exception {
        VersionTable t = VersionRegistry.current();
        eq("versionId", t.versionId, "inf-20100327");
        eq("family", t.family, "G2");
        eq("setBlockM", t.setBlockM, "c");
        eq("getBlockM", t.getBlockM, "a");
        eq("saveWorldM", t.saveWorldM, "a");
        eq("worldTimeF", t.worldTimeF, "b");
        eq("randSeedF", t.randSeedF, "x");
        eq("spawnXF", t.spawnXF, "g");
        eq("spawnYF", t.spawnYF, "h");
        eq("spawnZF", t.spawnZF, "i");
        eq("playerF", t.playerF, "c");
        eq("setPosRotM", t.setPosRotM, "b");
        eq("posXF", t.posXF, "g");
        eq("posYF", t.posYF, "h");
        eq("posZF", t.posZF, "i");
        eq("motXF", t.motXF, "j");
        eq("motYF", t.motYF, "k");
        eq("motZF", t.motZF, "l");
        eq("yawF", t.yawF, "m");
        eq("pitchF", t.pitchF, "n");
        eq("healthF", t.healthF, "W");
        eq("iframesF", t.iframesF, "K");
        eq("invF", t.invF, "b");
        eq("invMainF", t.invMainF, "a");
        eq("invArmorF", t.invArmorF, "b");
        eq("invCurF", t.invCurF, "c");
        eq("isCountF", t.isCountF, "a");
        eq("isIdF", t.isIdF, "c");
        eq("isDmgF", t.isDmgF, "d");
        eq("addStackM", t.addStackM, "a");
        eq("itemClassG", t.itemClassG, "net.minecraft.a.b.i");
        eq("itemsF", t.itemsF, "b");
        eq("itemEntityG", t.itemEntityG, "net.minecraft.a.c.c.b");
        eq("explodeM", t.explodeM, "a");
        eq("spawnEntityM", t.spawnEntityM, "a");
        eq("setEntityDeadM", t.setEntityDeadM, "b");
        eq("entityListM", t.entityListM, "i");
        eq("entityTypeM", t.entityTypeM, "a");
        // EntityList 双轨批：0327 查表槽留 null=维持现状（per-instance 已活体全绿）
        if (t.entityListClassG != null || t.entityListLookupM != null) {
            fails++; System.out.println("FAIL 0327 lookup slots must be null");
        }
        eq("playSoundM", t.playSoundM, "a");
        eq("getMetaM", t.getMetaM, "d");
        eq("setMetaSilentM", t.setMetaSilentM, "b");
        eq("light6M", t.light6M, "e");
        eq("getLightM", t.getLightM, "f");
        eq("observersF", t.observersF, "u");
        eq("difficultyF", t.difficultyF, "d");
        eq("scatterClassG", t.scatterClassG, "net.minecraft.a.a.c.b.b");
        eq("teGetM", t.teGetM, "h");
        eq("invSizeM", t.invSizeM, "a");
        eq("invGetM", t.invGetM, "a");
        eq("invSetM", t.invSetM, "a");
        eq("teBurnF", t.teBurnF, "f");
        eq("teCookF", t.teCookF, "h");
        eqi("idCrops", t.idCrops, 59);
        eqi("idSapling", t.idSapling, 6);
        eqi("cropsRipeMeta", t.cropsRipeMeta, 7);
        eq("mob.pig", t.mobClasses.get("pig"), "net.minecraft.a.c.b.c");
        eq("mob.sheep", t.mobClasses.get("sheep"), "net.minecraft.a.c.b.b");
        eq("mob.creeper", t.mobClasses.get("creeper"), "net.minecraft.a.c.a.d");
        eq("mob.skeleton", t.mobClasses.get("skeleton"), "net.minecraft.a.c.a.c");
        eq("mob.zombie", t.mobClasses.get("zombie"), "net.minecraft.a.c.a.f");
        eq("mob.spider", t.mobClasses.get("spider"), "net.minecraft.a.c.a.b");
        eq("mob.tnt", t.mobClasses.get("tnt"), "net.minecraft.a.c.c.a");
        eq("mob.giant", t.mobClasses.get("giant"), "net.minecraft.a.c.a.a");
        eq("mob.mob", t.mobClasses.get("mob"), "net.minecraft.a.c.c");
        if (t.mobClasses.size() != 9) { fails++; System.out.println("FAIL mobClasses.size=" + t.mobClasses.size()); }

        // WorldOps private 常量反射读值（表引用映射防手滑——每个常量==迁移前原值）
        eq("WO.M_SET_BLOCK", wo("M_SET_BLOCK"), "c");
        eq("WO.M_GET_BLOCK", wo("M_GET_BLOCK"), "a");
        eq("WO.M_SAVE_WORLD", wo("M_SAVE_WORLD"), "a");
        eq("WO.F_WORLD_TIME", wo("F_WORLD_TIME"), "b");
        eq("WO.F_RAND_SEED", wo("F_RAND_SEED"), "x");
        eq("WO.F_SPAWN_X", wo("F_SPAWN_X"), "g");
        eq("WO.F_SPAWN_Y", wo("F_SPAWN_Y"), "h");
        eq("WO.F_SPAWN_Z", wo("F_SPAWN_Z"), "i");
        eq("WO.F_WORLD_PLAYER", wo("F_WORLD_PLAYER"), "c");
        eq("WO.M_SET_POS_ROT", wo("M_SET_POS_ROT"), "b");
        eq("WO.E_POS_X", wo("E_POS_X"), "g");
        eq("WO.E_POS_Y", wo("E_POS_Y"), "h");
        eq("WO.E_POS_Z", wo("E_POS_Z"), "i");
        eq("WO.E_MOT_X", wo("E_MOT_X"), "j");
        eq("WO.E_MOT_Y", wo("E_MOT_Y"), "k");
        eq("WO.E_MOT_Z", wo("E_MOT_Z"), "l");
        eq("WO.E_YAW", wo("E_YAW"), "m");
        eq("WO.E_PITCH", wo("E_PITCH"), "n");
        eq("WO.E_HEALTH", wo("E_HEALTH"), "W");
        eq("WO.F_INV", wo("F_INV"), "b");
        eq("WO.INV_MAIN", wo("INV_MAIN"), "a");
        eq("WO.INV_ARMOR", wo("INV_ARMOR"), "b");
        eq("WO.INV_CUR", wo("INV_CUR"), "c");
        eq("WO.IS_COUNT", wo("IS_COUNT"), "a");
        eq("WO.IS_ID", wo("IS_ID"), "c");
        eq("WO.IS_DMG", wo("IS_DMG"), "d");
        eq("WO.M_ADDSTACK", wo("M_ADDSTACK"), "a");
        eq("WO.G_ITEM_CLASS", wo("G_ITEM_CLASS"), "net.minecraft.a.b.i");
        eq("WO.ITEMS", wo("ITEMS"), "b");
        eq("WO.M_EXPLODE", wo("M_EXPLODE"), "a");
        eq("WO.M_SPAWN_ENTITY", wo("M_SPAWN_ENTITY"), "a");
        eq("WO.G_ITEM_ENTITY", wo("G_ITEM_ENTITY"), "net.minecraft.a.c.c.b");
        eq("WO.M_SET_ENTITY_DEAD", wo("M_SET_ENTITY_DEAD"), "b");
        eq("WO.ID_CROPS", wo("ID_CROPS"), "59");
        eq("WO.ID_SAPLING", wo("ID_SAPLING"), "6");
        eq("WO.CROPS_RIPE_META", wo("CROPS_RIPE_META"), "7");
        eq("WO.E_IFRAMES", wo("E_IFRAMES"), "K");
        eq("WO.M_TE_GET", wo("M_TE_GET"), "h");
        eq("WO.M_INV_SIZE", wo("M_INV_SIZE"), "a");
        eq("WO.M_INV_GET", wo("M_INV_GET"), "a");
        eq("WO.M_INV_SET", wo("M_INV_SET"), "a");
        eq("WO.F_TE_BURN", wo("F_TE_BURN"), "f");
        eq("WO.F_TE_COOK", wo("F_TE_COOK"), "h");
        eq("WO.M_ENTITY_LIST", wo("M_ENTITY_LIST"), "i");
        eq("WO.M_ENTITY_TYPE", wo("M_ENTITY_TYPE"), "a");
        eq("WO.G_ENTITY_LIST_CLASS", wo("G_ENTITY_LIST_CLASS"), "null");
        eq("WO.M_ENTITY_LIST_LOOKUP", wo("M_ENTITY_LIST_LOOKUP"), "null");
        eq("WO.M_PLAY_SOUND", wo("M_PLAY_SOUND"), "a");
        eq("WO.M_GET_META", wo("M_GET_META"), "d");
        eq("WO.M_SET_META_SILENT", wo("M_SET_META_SILENT"), "b");
        eq("WO.M_LIGHT6", wo("M_LIGHT6"), "e");
        eq("WO.M_GET_LIGHT", wo("M_GET_LIGHT"), "f");
        eq("WO.F_OBSERVERS", wo("F_OBSERVERS"), "u");
        eq("WO.F_DIFFICULTY", wo("F_DIFFICULTY"), "d");
        eq("WO.G_SCATTER_CLASS", wo("G_SCATTER_CLASS"), "net.minecraft.a.a.c.b.b");
        eq("WO.MOB.pig", String.valueOf(((java.util.Map) woMap("MOB_CLASSES")).get("pig")), "net.minecraft.a.c.b.c");
        eq("WO.MOB.mob", String.valueOf(((java.util.Map) woMap("MOB_CLASSES")).get("mob")), "net.minecraft.a.c.c");
        if (((java.util.Map) woMap("MOB_CLASSES")).size() != 9) { fails++; System.out.println("FAIL WO.MOB_CLASSES.size"); }

        System.out.println(fails == 0 ? "REGISTRY-EQUALS OK (registry 66 + worldops 57)" : "REGISTRY-EQUALS FAILS=" + fails);
        if (fails != 0) System.exit(1);
    }
    static Object woMap(String fieldName) throws Exception {
        Field f = Class.forName("com.mcworldagent.agent.WorldOps").getDeclaredField(fieldName);
        f.setAccessible(true);
        return f.get(null);
    }
}
