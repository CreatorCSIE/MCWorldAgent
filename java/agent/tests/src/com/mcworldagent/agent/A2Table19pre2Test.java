package com.mcworldagent.agent;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Map;

/**
 * b1.9-pre2 表对榫测试（活体测试转正批 2026-11-25，仿 A2Table181Test）。
 * 戒律 14 双对榫：① VersionRegistry 硬编码 build19pre2() 逐字段 == 锚库
 * java/agent/registry/fingerprints/b1.9-pre2.json ops 段（95 键逐值，含转正批
 * 补钉 hunger/gamemode 7 键——锚件原缺，tiny L4896/L9418/L8128 实锤后回填）；
 * ② SHA1 反查=b0d40cf4…（索引行对榫件）；③ 钩四项 vs TARGETS_19pre2 对榫
 * （ti/Minecraft/Minecraft/adm——IProgressUpdate pd 断代、changeWorld 3 参
 * 实体 Luo、runTick k 字母保留、GuiIngame adm 断代）；④ capabilities 48 键
 * 对偶全检（42 OK/7 UNSUPPORTED——setmeta 判决修正=observersF+light6M 三件套
 * 未钉，181 同判，原锚件 OK 为候选误标已改）；⑤ 假件实形访问位复核
 * （钉表纪律：private 就 private，防假绿——sx 六槽/xx.a health/wc.e dmg/
 * la a/b 全 private 断代面 + uo/vp/lh abstract 面 + ti/lh decoy 同形陷阱群）。
 * 无头验收=run.ps1 全绿；活体冒烟另批。
 */
public class A2Table19pre2Test {

    private static int failures = 0;

    private static void ok(boolean c, String msg) {
        if (c) {
            System.out.println("  [ok] " + msg);
        } else {
            failures++;
            System.out.println("  [FAIL] " + msg);
        }
    }

    private static String s(Map m, String k) {
        return A2TableTest.str(m.get(k));
    }

    /** String/Integer/Boolean 三型槽值对榫（null 两侧同型）。 */
    private static boolean slotEq(Object jsonVal, Object tableVal) {
        if (jsonVal == null) {
            return tableVal == null;
        }
        return jsonVal.toString().equals(String.valueOf(tableVal));
    }

    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {
        byte[] buf = java.nio.file.Files.readAllBytes(new java.io.File(
                "E:/4k/github-projects/MCWorldAgent/java/agent/registry/fingerprints/b1.9-pre2.json").toPath());
        Map j = A2TableTest.obj(A2TableTest.parseJson(new String(buf, "UTF-8")));
        Map ops = A2TableTest.obj(j.get("ops"));
        Map hooks = A2TableTest.obj(j.get("hooks"));
        Map caps = A2TableTest.obj(j.get("capabilities"));

        // ---- 0. 身份三锚（versionId/family/SHA1 反查）----
        ok("b1.9-pre2".equals(s(j, "versionId")), "JSON versionId=b1.9-pre2");
        ok("G3".equals(s(j, "family")), "JSON family=G3");
        VersionTable bySha = VersionRegistry.byJarSha1("b0d40cf43b625631af65e2a645c34b533251da0e");
        ok(bySha != null && "b1.9-pre2".equals(bySha.versionId),
                "byJarSha1(b0d40cf4…)=b1.9-pre2（戒律 14 严格指纹反查）");
        ok(VersionRegistry.activate("b1.9-pre2"), "activate(b1.9-pre2)");
        VersionTable t = VersionRegistry.current();
        ok(t == bySha, "激活表==SHA1 反查表（同实例）");
        ok("G3".equals(t.family), "表 family=G3");

        // ---- 1. ops 段逐字段对榫（95 键全量；mobClasses 另段）----
        int compared = 0;
        for (Object keyObj : ops.keySet()) {
            String key = (String) keyObj;
            if ("mobClasses".equals(key)) {
                continue;
            }
            Object jsonVal = ops.get(key);
            Object tableVal = slot(t, key);
            ok(tableVal != null || jsonVal == null,
                    "槽 " + key + " 存在于 VersionTable");
            ok(slotEq(jsonVal, tableVal), "ops." + key + " JSON=" + jsonVal
                    + " 表=" + tableVal);
            compared++;
        }
        ok(ops.size() == 101, "ops 键数=101（88 原钉+7 转正补钉+2 T1 旗标 setter+4 T2 sleep） 实=" + ops.size());
        ok(compared == 100, "对榫槽数=100（mobClasses 另段） 实=" + compared);

        // 断代关键槽复核（禁借 173/181 判决面——HANDOFF 钉死清单）
        ok("y".equals(t.getWorldInfoM), "getWorldInfoM=y（⚠️181=x 断代翻面）");
        ok("e".equals(t.worldTimeF) && "a".equals(t.randSeedF), "sx 时间/种子面 e/a");
        ok("b".equals(t.spawnXF) && "c".equals(t.spawnYF) && "d".equals(t.spawnZF), "sx 出生点面 b/c/d");
        ok("Z".equals(t.iframesF), "iframesF=Z heartsLife（⚠️181=aa 断代）");
        ok("a".equals(t.healthF), "healthF=a（⚠️181=bz——**private** 断代，见假件复核 2）");
        ok("c".equals(t.setPosRotM), "setPosRotM=c setLocationAndAngles（⚠️181=b 断代翻面）");
        ok("c".equals(t.setMetaSilentM), "setMetaSilentM=c 裸静默写（⚠️181=f WithNotify 断代翻面）");
        ok("o".equals(t.posXF) && "p".equals(t.posYF) && "q".equals(t.posZF), "pos 面 o/p/q（右移规律）");
        ok("r".equals(t.motXF) && "s".equals(t.motYF) && "t".equals(t.motZF), "mot 面 r/s/t");
        ok("u".equals(t.yawF) && "v".equals(t.pitchF), "look 面 u/v");
        ok("ar".equals(t.invF), "invF=ar（⚠️181=as 右移一位）");
        ok("e".equals(t.isDmgF), "isDmgF=e（⚠️181=d——**private** 断代，见假件复核 3）");
        ok("e".equals(t.itemsF), "itemsF=e（⚠️181=f 断代）");
        ok("i".equals(t.playerListF), "playerListF=i ✓同 181 字母");
        ok("r".equals(t.biomeNameF), "biomeNameF=r（⚠️181=vh.l 断代）");
        ok("lh".equals(t.entityClassG), "entityClassG=lh（⚠️撞车位 181 lh=EntityDamageSource）");
        ok("eq".equals(t.itemEntityG), "itemEntityG=eq（hl→ee→eq 断代）");
        ok("uk".equals(t.itemClassG), "itemClassG=uk（gm→sv→uk 断代）");
        ok("wd".equals(t.entityListClassG), "entityListClassG=wd（⚠️撞车位 181 wd=EntityLiving）");
        ok("b".equals(t.entityListLookupM), "entityListLookupM=b ✓同字母");
        ok("(Lpd;)V".equals(t.saveWorldDesc), "saveWorldDesc=(Lpd;)V（IProgressUpdate pd 断代）");
        ok("v".equals(t.difficultyF), "difficultyF=v ✓同 181 字母");
        ok("p".equals(t.entityListM) && "a".equals(t.spawnEntityM) && "b".equals(t.setEntityDeadM),
                "实体事件面 p/a/b ✓同 181");
        ok("i".equals(t.hudPlayerF) && "q".equals(t.hudFontF) && "z".equals(t.hudSettingsF),
                "HUD 链面 i/q/z（主类字段面 JSON 钉）");
        ok("E".equals(t.hudDebugGateF), "hudDebugGateF=E（gk.E=showDebugInfo tiny L3627）");
        ok(Boolean.FALSE.equals(t.hudXyzPaint), "hudXyzPaint=false（≥a1.2.3 边界线）");
        ok("aW".equals(t.xpF) && "aY".equals(t.xpLevelF) && "aX".equals(t.playerLevelF),
                "xp 三槽 aW/aY/aX（双 level 并存怪癖——181 aX/aY/aZ 整体不同）");
        ok("ax".equals(t.scoreF) && "aF".equals(t.xpCooldownF), "score/xpCooldown=ax/aF（181 ay/aG 断代）");
        ok("f".equals(t.addXpM) && "aL".equals(t.increaseLevelM), "addXpM=f/increaseLevelM=aL（留档面）");
        ok("bP".equals(t.effectsMapF) && "aG".equals(t.clearEffectsM), "effectsMapF=bP/clearEffectsM=aG（181 无清空面）");
        ok("afk".equals(t.potionEffectClassG) && "(III)V".equals(t.potionEffectCtorDesc), "afk ctor (III)V ✓历代同形");
        ok("vt".equals(t.potionClassG), "potionClassG=vt（MobEffectList 名=Potion 命名陷阱）");
        ok("bi".equals(t.xpOrbClassG), "xpOrbClassG=bi（留档，spawn 面后续批）");
        ok("av".equals(t.foodStatsM) && "la".equals(t.foodStatsClassG), "foodStatsM=av/la（⚠️181=at/kc 断代）");
        ok("a".equals(t.foodLevelF) && "b".equals(t.foodSatF) && "c".equals(t.foodExhaustF) && "d".equals(t.foodTimerF),
                "la 四槽 a/b/c/d ✓同 181 kc 字母");
        ok("p".equals(t.gameTypeF), "gameTypeF=p（sx=rl 字段字母全同判决兑现）");
        ok("o".equals(t.rainStrF) && "q".equals(t.thunderStrF), "rainStrF=o/thunderStrF=q ✓同 181 字母");

        // ---- 2. hooks 段对榫（四项 vs TARGETS_19pre2；JSON 键=常量名）----
        ok(hooks.size() == 4, "hooks 节数=4 实=" + hooks.size());
        HookTargets.activate("b1.9-pre2");
        String[][] hookTriples = {
            {"SAVE_WORLD", HookTargets.SAVE_WORLD, "ti"},
            {"LOAD_WORLD", HookTargets.LOAD_WORLD, "net/minecraft/client/Minecraft"},
            {"RUN_TICK", HookTargets.RUN_TICK, "net/minecraft/client/Minecraft"},
            {"HUD_RENDER", HookTargets.HUD_RENDER, "adm"},
        };
        int hooksCmp = 0;
        for (int i = 0; i < hookTriples.length; i++) {
            String name = hookTriples[i][0];
            Map hj = A2TableTest.obj(hooks.get(name));
            ok(hj != null, "anchor hook " + name);
            if (hj == null) {
                continue;
            }
            HookTargets.Target target = null;
            HookTargets.Target[] all = HookTargets.forClass(hookTriples[i][2]);
            for (int k = 0; k < all.length; k++) {
                if (hookTriples[i][1].equals(all[k].hookId)) {
                    target = all[k];
                }
            }
            ok(target != null, "TARGETS_19pre2 含 " + hookTriples[i][1]);
            if (target == null) {
                continue;
            }
            hooksCmp++;
            ok(s(hj, "class").equals(target.className), target.hookId + " class JSON=" + s(hj, "class")
                    + " 表=" + target.className);
            ok(s(hj, "method").equals(target.methodName), target.hookId + " method ✓");
            ok(s(hj, "desc").equals(target.desc), target.hookId + " desc ✓");
            ok(Integer.parseInt(s(hj, "access").substring(2), 16) == target.access,
                    target.hookId + " access 0x0001");
            ok(((Number) hj.get("expectedHits")).intValue() == target.expectedHits,
                    target.hookId + " hits ✓");
            String mode = s(hj, "injectMode");
            ok("ENTRY".equals(mode) ? target.injectMode == HookTargets.ENTRY
                            : "EXIT".equals(mode) && target.injectMode == HookTargets.EXIT,
                    target.hookId + " mode " + mode);
        }
        ok(hooksCmp == 4, "hooks 对榫计数=4（五项全中纪律） 实=" + hooksCmp);
        ok(hooks.get("WORLD_CTOR") == null, "no WORLD_CTOR hook（173/181 先例，dir 面走 cmd=）");
        ok(HookTargets.forClass("ti").length == 1, "forClass(ti)=1（save 包装 ENTRY）");

        // ---- 3. mobClasses 段（8 键；mob=null 判决面）----
        Map mobs = A2TableTest.obj(ops.get("mobClasses"));
        ok(mobs.size() == 9, "mobClasses 键数=9（8 在册+mob 显式 null） 实=" + mobs.size());
        String[] mobKeys = {"pig", "sheep", "creeper", "skeleton", "zombie", "spider", "tnt", "giant", "mob"};
        int mcCmp = 0;
        for (int i = 0; i < mobKeys.length; i++) {
            Object jsonVal = mobs.get(mobKeys[i]);
            Object tableVal = t.mobClasses.get(mobKeys[i]);
            boolean eq = jsonVal == null ? tableVal == null : jsonVal.equals(tableVal);
            ok(eq, "mobClasses." + mobKeys[i] + " JSON=" + jsonVal + " 表=" + tableVal);
            mcCmp++;
        }
        ok(mcCmp == 9, "mobClasses 对榫计数=9（8 在册+mob null）");

        // ---- 4. capabilities 对偶全检（41 OK/7 UNSUPPORTED）----
        ok(caps.size() == 49, "capabilities 键数=49（42 OK/7 UNSUPPORTED，T2 sleep 入列） 实=" + caps.size());
        String[][] unPairs = {
            {"setmeta", "observersF"},   // 转正批判决修正：三件套未钉=181 同判
            {"grow", "idCrops"},
            {"genscatter", "scatterClassG"},
            {"getchest", "invSizeM"},
            {"setchest", "invSizeM"},
            {"getfurnace", "teBurnF"},
            {"setfurnace", "teBurnF"},
        };
        int unN = 0;
        for (int i = 0; i < unPairs.length; i++) {
            ok("UNSUPPORTED".equals(s(caps, unPairs[i][0])), unPairs[i][0] + "=UNSUPPORTED");
            ok(slot(t, unPairs[i][1]) == null, unPairs[i][0] + " 门槽 " + unPairs[i][1] + "=null");
            unN++;
        }
        String[][] okPairs = {
            {"place", "setBlockM"}, {"dig", "setBlockM"}, {"getblock", "getBlockM"},
            {"getblocks", "getBlockM"}, {"fill", "setBlockM"}, {"replace", "setBlockM"},
            {"getmeta", "getMetaM"}, {"getlight", "getLightM"}, {"getinfo", "worldTimeF"},
            {"settime", "worldTimeF"}, {"setspawn", "spawnXF"}, {"save", "saveWorldM"},
            {"tp", "setPosRotM"}, {"setlook", "yawF"}, {"setmotion", "motXF"},
            {"getplayer", "healthF"}, {"sethealth", "healthF"}, {"give", "addStackM"},
            {"inv", "invF"}, {"setarmor", "invArmorF"}, {"clearinv", "invMainF"},
            {"spawn", "spawnEntityM"}, {"drop", "itemEntityG"}, {"explode", "explodeM"},
            {"clear", "setEntityDeadM"}, {"entitylist", "entityListM"}, {"playsound", "playSoundM"},
            {"getbiome", "biomeWcmM"}, {"invulnerable", "iframesF"}, {"kill", "setEntityDeadM"},
            {"clone", "getBlockM"}, {"setdifficulty", "difficultyF"},
            {"weather", "rainStrF"}, {"setxp", "xpF"}, {"giveeffect", "addEffectM"},
            {"removeeffect", "removeEffectM"}, {"sethunger", "foodStatsM"},
            {"setgamemode", "gameTypeF"}, {"getxp", "xpLevelF"}, {"gethunger", "foodSatF"},
        };
        int okN = 0;
        for (int i = 0; i < okPairs.length; i++) {
            ok("OK".equals(s(caps, okPairs[i][0])), okPairs[i][0] + "=OK");
            ok(slot(t, okPairs[i][1]) != null, okPairs[i][0] + " 门槽 " + okPairs[i][1] + " 非 null");
            okN++;
        }
        ok("OK".equals(s(caps, "listops")), "listops=OK（恒真面）");
        int nOk = 0;
        int nUn = 0;
        for (Object keyObj : caps.keySet()) {
            String v = s(caps, (String) keyObj);
            if ("OK".equals(v)) {
                nOk++;
            } else if ("UNSUPPORTED".equals(v)) {
                nUn++;
            } else {
                ok(false, "capabilities 值域异常: " + keyObj + "=" + v);
            }
        }
        ok(nOk == 42 && nUn == 7, "caps 计数对账 OK=42/UNSUPPORTED=7 实=" + nOk + "/" + nUn);
        ok(unN == 7 && okN == 40, "对偶断言计数 unN=7/okN=40（listops 另计） 实=" + unN + "/" + okN);

        // ---- 5. null 槽复核（档位一缺失面=13 槽）----
        String[] nullSlots = {"light6M", "observersF", "scatterClassG", "entityTypeM",
                "invSizeM", "invGetM", "invSetM", "teBurnF", "teCookF",
                "idCrops", "idSapling", "cropsRipeMeta", "playerF"};
        for (int i = 0; i < nullSlots.length; i++) {
            ok(slot(t, nullSlots[i]) == null, "null 槽 " + nullSlots[i]);
        }

        // ---- 6. 假件实形复核（钉表纪律：private 就 private，防假绿）----
        // 1) WorldInfo=sx 六槽 private（time/seed/spawn×3/gameType）
        String[] sxPriv = {"a", "e", "b", "c", "d", "p"};
        for (int i = 0; i < sxPriv.length; i++) {
            Field f = Class.forName("sx").getDeclaredField(sxPriv[i]);
            ok(Modifier.isPrivate(f.getModifiers()), "sx." + sxPriv[i] + " private（javap 实锤）");
        }
        // 2) EntityLiving=xx.a=health **private**（断代访问位翻面主证）
        Field hA = Class.forName("xx").getDeclaredField("a");
        ok(Modifier.isPrivate(hA.getModifiers()), "xx.a health private（WorldOps 回退消费面）");
        ok(!Modifier.isStatic(hA.getModifiers()), "xx.a 非 static（实例字段）");
        // 3) ItemStack=wc.e=itemDamage **private**（b1.2_02 同配方消费面）
        Field wcE = Class.forName("wc").getDeclaredField("e");
        ok(Modifier.isPrivate(wcE.getModifiers()), "wc.e dmg private");
        // 4) Entity=lh.G=isDead boolean / lh.Z=heartsLife int
        Field g = Class.forName("lh").getField("G");
        ok(g.getType() == boolean.class, "lh.G isDead boolean ✓");
        Field z = Class.forName("lh").getField("Z");
        ok(z.getType() == int.class, "lh.Z heartsLife int ✓（iframesF 主证）");
        // 5) save 包装 ti.a(Lpd;)V public
        java.lang.reflect.Method sw = Class.forName("ti").getMethod("a", Class.forName("pd"));
        ok(Modifier.isPublic(sw.getModifiers()), "ti.a(Lpd;)V public（SAVE_WORLD 注入位）");
        // 6) getWorldInfoM 面 ti.y()Lsx public
        java.lang.reflect.Method gy = Class.forName("ti").getMethod("y");
        ok(gy.getReturnType() == Class.forName("sx"), "ti.y()Lsx ✓（getWorldInfoM=y 主证）");
        // 7) biome 三锚链：ti.a()Loz→oz.a(II)Lwz→wz.r String
        java.lang.reflect.Method gwcm = Class.forName("ti").getMethod("a");
        ok(gwcm.getReturnType() == Class.forName("oz"), "ti.a()Loz ✓（无参唯一）");
        java.lang.reflect.Method bat = Class.forName("oz").getMethod("a", int.class, int.class);
        ok(bat.getReturnType() == Class.forName("wz"), "oz.a(II)Lwz ✓");
        Field bnm = Class.forName("wz").getField("r");
        ok(bnm.getType() == String.class, "wz.r biomeName String ✓（⚠️181=vh.l）");
        // 8) explode 返回 Explosion(abf)
        java.lang.reflect.Method ex = Class.forName("ti").getMethod("a",
                Class.forName("lh"), double.class, double.class, double.class, float.class);
        ok(ex.getReturnType() == Class.forName("abf"), "ti.a(Llh;DDDF)Labf ✓");
        // 9) TNT=abg 构造 (ti,DDD)V 在位、(ti,FFF)V 缺席
        Class<?> ze = Class.forName("abg");
        ze.getConstructor(Class.forName("ti"), double.class, double.class, double.class);
        boolean zeFff = false;
        try {
            ze.getConstructor(Class.forName("ti"), float.class, float.class, float.class);
            zeFff = true;
        } catch (NoSuchMethodException e) {
            // 预期：FFF 断代延续
        }
        ok(!zeFff, "abg 无 (ti,FFF)V（DDD fallback 前提实锤）");
        // 10) ItemStack=wc 无 (II)V、(III)V 在位（newStack 等价配方前提）
        Class<?> ul = Class.forName("wc");
        boolean ul2 = false;
        try {
            ul.getConstructor(int.class, int.class);
            ul2 = true;
        } catch (NoSuchMethodException e) {
            // 预期：(II)V 缺席延续
        }
        ok(!ul2, "wc 无 (II)V（newStack 等价配方前提）");
        ul.getConstructor(int.class, int.class, int.class);
        ok(true, "wc (III)V 在位");
        // 11) EntityPlayer=uo **abstract** + 活体玩家真身 sd concrete（断代主证）
        ok(Modifier.isAbstract(Class.forName("uo").getModifiers()), "uo EntityPlayer abstract");
        ok(!Modifier.isAbstract(Class.forName("sd").getModifiers()), "sd EntityPlayerSP concrete");
        ok(Class.forName("lh").isAssignableFrom(Class.forName("sd")), "sd instanceof lh");
        // 12) EntityCreature=vp abstract（mob UNSUPPORTED 判决面）
        ok(Modifier.isAbstract(Class.forName("vp").getModifiers()), "vp EntityCreature abstract");
        // 13) setMetaSilent 主写面 c(IIII)Z + 同形陷阱群 d/e/f 在位
        Class<?> wcW = Class.forName("ti");
        java.lang.reflect.Method cSilent = wcW.getMethod("c", int.class, int.class, int.class, int.class);
        ok(cSilent.getReturnType() == boolean.class, "ti.c(IIII)Z 主写面 ✓");
        java.lang.reflect.Method fNotify = wcW.getMethod("f", int.class, int.class, int.class, int.class);
        ok(fNotify.getReturnType() == void.class, "ti.f(IIII)V WithNotify decoy ✓");
        wcW.getMethod("d", int.class, int.class, int.class, int.class);
        wcW.getMethod("e", int.class, int.class, int.class, int.class);
        ok(true, "ti d(IIII)Z/e(IIII)Z 同形陷阱在位");
        // 14) setPosRot 主写面 c(DDDFF)V + b decoy 在位
        Class<?> lhC = Class.forName("lh");
        lhC.getMethod("c", double.class, double.class, double.class, float.class, float.class);
        lhC.getMethod("b", double.class, double.class, double.class, float.class, float.class);
        ok(true, "lh c(DDDFF)V 主写 + b(DDDFF)V decoy ✓（断代翻面实形）");
        // 15) getFoodStats 面 uo.av()Lla public
        java.lang.reflect.Method gf = Class.forName("uo").getMethod("av");
        ok(gf.getReturnType() == Class.forName("la") && Modifier.isPublic(gf.getModifiers()),
                "uo.av()Lla public ✓（foodStatsM=av 主证）");
        // 16) la 字段面 a/b private（181 kc 同形）
        Field laA = Class.forName("la").getDeclaredField("a");
        Field laB = Class.forName("la").getDeclaredField("b");
        ok(Modifier.isPrivate(laA.getModifiers()) && laA.getType() == int.class, "la.a foodLevel private int");
        ok(Modifier.isPrivate(laB.getModifiers()) && laB.getType() == float.class, "la.b saturation private float");
        // 17) GuiIngame=adm 构造 (Minecraft)V + a(FZII)V 在位
        Class.forName("adm").getConstructor(Class.forName("net.minecraft.client.Minecraft"));
        Class.forName("adm").getMethod("a", float.class, boolean.class, int.class, int.class);
        ok(true, "adm (Minecraft)V + a(FZII)V ✓");
        // 18) GameSettings=gk.E public boolean
        Field gkE = Class.forName("gk").getField("E");
        ok(gkE.getType() == boolean.class && Modifier.isPublic(gkE.getModifiers()), "gk.E showDebugInfo public boolean ✓");
        // 19) EntityList=wd.b(lh)String static
        java.lang.reflect.Method wdB = Class.forName("wd").getMethod("b", lhC);
        ok(Modifier.isStatic(wdB.getModifiers()) && wdB.getReturnType() == String.class, "wd.b(lh)String static ✓");
        // 20) Item=uk.e public static uk[]
        Field ukE = Class.forName("uk").getField("e");
        ok(Modifier.isStatic(ukE.getModifiers()) && ukE.getType() == Class.forName("[Luk;"), "uk.e public static uk[] ✓");
        // 21) InventoryPlayer=vz 三面 public
        Class.forName("vz").getField("a");
        Class.forName("vz").getField("b");
        Class.forName("vz").getField("c");
        ok(true, "vz a/b/c public（main/armor/cur）✓");

        System.out.println("A2Table19pre2Test: " + (failures == 0 ? "PASS" : failures + " FAILURES"));
        if (failures != 0) {
            System.exit(1);
        }
    }

    private static Object slot(VersionTable t, String key) {
        try {
            Field f = VersionTable.class.getField(key);
            return f.get(t);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }
}
