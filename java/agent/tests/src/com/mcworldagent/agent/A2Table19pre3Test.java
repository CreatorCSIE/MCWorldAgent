package com.mcworldagent.agent;

/**
 * A2Table19pre3Test（药水活体落地批 2026-11-28，最小转正验收）：
 * ①SHA1 反查（b1.9-pre3-1402.jar 实算 sha1=5b7fe76a… 对榫 BY_SHA1+activate）；
 * ②药水链槽逐值 vs T1 取证表（potcmp all-p3.txt javap 全量对榫，判决注释详
 *   VersionTable 药水链段/build19pre3 头注）；
 * ③TARGETS_19pre3 六元组对榫（class|name|desc|access|hits|mode）。
 * 无 JSON 锚（b1.9-pre3-1402.json 未建——内建表转正批，A2 对榫腿不适用）；
 * 无假件（最小转正不含写读面，形状复验留全 76 槽批）。
 * 顺带复验 pre2 药水链槽已钉（同 op 双版依赖）+ 门控边界（其余版本 capPotionTest=null）。
 */
public class A2Table19pre3Test {

    static int checks = 0;

    static void eq(String what, Object expected, Object actual) {
        checks++;
        if (!String.valueOf(expected).equals(String.valueOf(actual))) {
            throw new AssertionError("[" + what + "] expected=" + expected + " actual=" + actual);
        }
    }

    static void ok(String what, boolean cond) {
        checks++;
        if (!cond) {
            throw new AssertionError("[" + what + "] false");
        }
    }

    /** 六元组断言（体例同 A2Table19pre2Test.hook）。 */
    static void hook(String tag, HookTargets.Target h, String cls, String name, String desc,
                     int access, int hits, int mode, String id) {
        eq("19pre3 " + tag + ".class", cls, h.className);
        eq("19pre3 " + tag + ".name", name, h.methodName);
        eq("19pre3 " + tag + ".desc", desc, h.desc);
        eq("19pre3 " + tag + ".access", access, h.access);
        eq("19pre3 " + tag + ".hits", hits, h.expectedHits);
        eq("19pre3 " + tag + ".mode", mode, h.injectMode);
        eq("19pre3 " + tag + ".id", id, h.hookId);
    }

    public static void main(String[] args) throws Exception {
        // ---------- ①SHA1 反查 + activate ----------
        String sha = "5b7fe76a602b7511c97740e36dc25040ccb6e76b"; // 本窗 Get-FileHash 实算
        VersionTable bySha = VersionRegistry.byJarSha1(sha);
        ok("pre3 BY_SHA1 反查命中", bySha != null);
        eq("pre3 versionId", "b1.9-pre3-1402", bySha.versionId);
        eq("pre3 family", "G3", bySha.family);
        ok("pre3 activate 命中", VersionRegistry.activate("b1.9-pre3-1402"));
        VersionTable t = VersionRegistry.current();
        eq("pre3 activate current", "b1.9-pre3-1402", t.versionId);
        // 身份三元组（mainClass=net.minecraft.client.Minecraft 不混淆——VersionTable 无
        // mainClass 槽，钩组类名即证；launcher id 对齐）
        eq("pre3 jarSha1 回写", sha, t.jarSha1);

        // ---------- ②药水链槽逐值 vs T1 取证表 ----------
        eq("pre3 capPotionTest", Boolean.TRUE, t.capPotionTest);
        eq("pre3 PotionHelper 类字", "gv", t.potHelperClassG);   // static HashMap j/k+String[] l+解释器 a(String,III)I
        eq("pre3 dd_d 方法字", "a", t.potDddM);                   // aaaa_Aaa_ 重构（pre2 dd_d 改名+增 boolean 参）
        eq("pre3 dd_d 描述符", "(IZ)Ljava/util/List;", t.potDddDesc);
        eq("pre3 potionRequirements 字段", "j", t.potReqMapF);    // 静态块 8 put=判决指纹
        eq("pre3 ItemPotion 类字", "do", t.itemPotionClassG);     // extends vf+effectCache+a_(wy)List
        eq("pre3 onFoodEaten 方法字", "b", t.itemPotionOnEatM);   // b(wy,uc,vj)Lwy; 五锚方法体对榫
        eq("pre3 ItemStack 类字", "wy", t.itemStackClassG);
        eq("pre3 Item 类字", "vf", t.itemClassG);                 // vf[]=e itemsList
        eq("pre3 Item.potion 字段", "bs", t.potionItemF);         // vf.bs **Ldo;** 收窄声明
        eq("pre3 getPotionID", "a", t.peIdM);                     // agp 三 getter（getRecord 体逐锚）
        eq("pre3 getPotionDuration", "b", t.peDurM);
        eq("pre3 getPotionAmplifier", "c", t.peAmpM);
        // 效果回读面（potiontest readback 依赖）
        eq("pre3 capEffect", Boolean.TRUE, t.capEffect);
        eq("pre3 addEffectM", "a", t.addEffectM);                 // yv.a(Lagp;)V
        eq("pre3 addEffectDesc", "(Lagp;)V", t.addEffectDesc);    // 载体=agp（非 pre2 afk）
        eq("pre3 removeEffectM", "i", t.removeEffectM);           // yv.i(I)V
        eq("pre3 activePotionsMap 字段", "bP", t.effectsMapF);    // yv.bP 与 pre2 xx.bP 撞同字母
        eq("pre3 Potion 类字", "wo", t.potionClassG);             // wo[]=a potionTypes 32 槽
        eq("pre3 PotionEffect 类字", "agp", t.potionEffectClassG);
        eq("pre3 PotionEffect 构造", "(III)V", t.potionEffectCtorDesc);
        // worldPlayer 取玩家链
        eq("pre3 playerListF", "i", t.playerListF);               // uc.i（uc.a(Llu;)Z instanceof vj 对榫）
        eq("pre3 entityClassG", "lu", t.entityClassG);            // yv extends lu
        // 最小转正边界：未取证槽必须 null（响亮 UNSUPPORTED 而非错值）
        ok("pre3 最小转正: setBlockM null", t.setBlockM == null);
        ok("pre3 最小转正: getBlockM null", t.getBlockM == null);
        ok("pre3 最小转正: invF null", t.invF == null);
        ok("pre3 最小转正: spawnEntityM null", t.spawnEntityM == null);
        ok("pre3 最小转正: capXp null", t.capXp == null);
        ok("pre3 最小转正: capHunger null", t.capHunger == null);

        // ---------- ③TARGETS_19pre3 六元组对榫 ----------
        HookTargets.Target[] g3 = HookTargets.TARGETS_19pre3;
        eq("pre3 钩组条数", 4, g3.length);
        hook("SAVE_WORLD", g3[0], "uc", "a", "(Lpr;)V", 0x0001, 1, HookTargets.ENTRY, HookTargets.SAVE_WORLD);
        hook("LOAD_WORLD", g3[1], "net/minecraft/client/Minecraft", "a",
                "(Luc;Ljava/lang/String;Lvj;)V", 0x0001, 1, HookTargets.EXIT, HookTargets.LOAD_WORLD);
        hook("RUN_TICK", g3[2], "net/minecraft/client/Minecraft", "k", "()V",
                0x0001, 1, HookTargets.ENTRY, HookTargets.RUN_TICK);
        hook("HUD_RENDER", g3[3], "aeo", "a", "(FZII)V", 0x0001, 1, HookTargets.EXIT, HookTargets.HUD_RENDER);

        // ---------- ④pre2 药水链槽复验（同 op 双版依赖） ----------
        VersionTable p2 = VersionRegistry.byVersionId("b1.9-pre2");
        ok("pre2 表在", p2 != null);
        eq("pre2 capPotionTest", Boolean.TRUE, p2.capPotionTest);
        eq("pre2 PotionHelper 类字", "go", p2.potHelperClassG);
        eq("pre2 dd_d 方法字", "d", p2.potDddM);                  // go.d(I)Ljava/util/List;
        eq("pre2 dd_d 描述符", "(I)Ljava/util/List;", p2.potDddDesc);
        eq("pre2 potionRequirements 字段", "a", p2.potReqMapF);   // 20 put=判决指纹
        eq("pre2 ItemPotion 类字", "dn", p2.itemPotionClassG);
        eq("pre2 onFoodEaten 方法字", "b", p2.itemPotionOnEatM);  // dn.b(wc,ti,uo)Lwc;
        eq("pre2 ItemStack 类字", "wc", p2.itemStackClassG);
        eq("pre2 Item.potion 字段", "bs", p2.potionItemF);        // uk.bs
        eq("pre2 getPotionID", "a", p2.peIdM);
        eq("pre2 getPotionDuration", "b", p2.peDurM);
        eq("pre2 getPotionAmplifier", "c", p2.peAmpM);
        eq("pre2 PotionEffect 类字", "afk", p2.potionEffectClassG); // 既有钉复验
        eq("pre2 activePotionsMap 字段", "bP", p2.effectsMapF);     // 既有钉复验

        // ---------- ⑤门控边界：其余版本 capPotionTest=null ----------
        VersionTable b181 = VersionRegistry.byVersionId("b1.8.1");
        ok("181 capPotionTest null", b181 != null && b181.capPotionTest == null);
        VersionTable b1501 = VersionRegistry.byVersionId("b1.5_01");
        ok("1501 capPotionTest null", b1501 != null && b1501.capPotionTest == null);
        VersionTable b16 = VersionRegistry.byVersionId("b1.6");
        ok("b1.6 capPotionTest null", b16 != null && b16.capPotionTest == null);

        System.out.println("A2Table19pre3Test PASS (" + checks + " checks)");
    }
}
