// [全覆盖批产 2026-11-28] SlotVerify：邻版继承校验工具 v1（b1.6 家族试点）。
// 输入=基准版指纹 JSON + 目标 jar + 新版 ID；输出=逐槽 PASS/MISS 报告 + 新版
// JSON 草稿（继承值，MISS 槽列入待重取证清单）。
// 核心机制=宿主类派生链（全部从 jar 实际类层次派生，不借跨代表字母）：
//   World=hooks.SAVE_WORLD.class；EntityPlayer=LOAD_WORLD desc 第 3 参；
//   Entity=ops.entityClassG；EntityLiving=super(Entity)；
//   TileEntity=World.teGetM 返回类型；WorldInfo=World.getWorldInfoM 返回类型；
//   InventoryPlayer=EntityPlayer.invF 字段类型；ItemStack=InventoryPlayer.addStackM
//   参数类型；EnumStatus=EntityPlayer.sleepInBedM 返回类型。
// v1 纪律：只验「继承字母是否仍成立」，不自动搜索新字母（断代=MISS 人工重取证）。
package mca.tools;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class SlotVerify {

    static final Map<String, byte[]> CLS = new HashMap<String, byte[]>();
    static final Map<String, Info> CACHE = new HashMap<String, Info>();
    static int pass = 0;
    static int fail = 0;
    static final List<String> misses = new ArrayList<String>();

    static class F {
        String name;
        String desc;
        int access;
    }

    static class M {
        String name;
        String desc;
        int access;
    }

    static class Info {
        String name;
        String superName;
        List<F> fields = new ArrayList<F>();
        List<M> methods = new ArrayList<M>();
    }

    static Info load(String internal) {
        if (CACHE.containsKey(internal)) {
            return (Info) CACHE.get(internal);
        }
        byte[] b = (byte[]) CLS.get(internal);
        if (b == null) {
            CACHE.put(internal, null);
            return null;
        }
        final Info i = new Info();
        new ClassReader(b).accept(new ClassVisitor(Opcodes.ASM9) {
            public void visit(int v, int acc, String n, String sig, String sup, String[] it) {
                i.name = n;
                i.superName = sup;
            }
            public FieldVisitor visitField(int acc, String n, String d, String s, Object cv) {
                F f = new F();
                f.name = n; f.desc = d; f.access = acc;
                i.fields.add(f);
                return null;
            }
            public MethodVisitor visitMethod(int acc, String n, String d, String s, String[] e) {
                M m = new M();
                m.name = n; m.desc = d; m.access = acc;
                i.methods.add(m);
                return null;
            }
        }, 0);
        CACHE.put(internal, i);
        return i;
    }

    static boolean hasField(String cls, String fname, String fdesc) {
        Info i = load(cls);
        if (i == null) {
            return false;
        }
        for (F f : i.fields) {
            if (f.name.equals(fname) && f.desc.equals(fdesc)) {
                return true;
            }
        }
        return false;
    }

    static boolean hasMethod(String cls, String mname, String mdesc) {
        Info i = load(cls);
        if (i == null) {
            return false;
        }
        for (M m : i.methods) {
            if (m.name.equals(mname) && m.desc.equals(mdesc)) {
                return true;
            }
        }
        return false;
    }

    static String fieldType(String cls, String fname) {
        Info i = load(cls);
        if (i == null) {
            return null;
        }
        for (F f : i.fields) {
            if (f.name.equals(fname)) {
                return f.desc;
            }
        }
        return null;
    }

    static String methodDesc(String cls, String mname) {
        Info i = load(cls);
        if (i == null) {
            return null;
        }
        for (M m : i.methods) {
            if (m.name.equals(mname)) {
                return m.desc;
            }
        }
        return null;
    }

    /** desc 参数表最后一个 L 类型（LOAD_WORLD=(Lfb;Lj.l/S;Lgq;)V → gq）。 */
    static String lastArgLType(String desc) {
        if (desc == null) {
            return null;
        }
        int p = desc.indexOf('(');
        int q = desc.indexOf(')', p);
        String args = desc.substring(p + 1, q);
        String last = null;
        int i = 0;
        while (i < args.length()) {
            char c = args.charAt(i);
            if (c == 'L') {
                int e = args.indexOf(';', i);
                last = args.substring(i + 1, e);
                i = e + 1;
            } else {
                i++;
            }
        }
        return last;
    }

    /** 语义锚 1：LDC "Health"（EntityLiving NBT 读写面）后首个 int GETFIELD
     *  = health 字段（healthF 勘误批同源判据；构造器 10/20 撞车判据已废）。 */
    static String semanticHealth(String living) {
        final boolean[] after = new boolean[1];
        final String[] hit = new String[1];
        byte[] b = (byte[]) CLS.get(living);
        if (b == null) {
            return null;
        }
        new ClassReader(b).accept(new ClassVisitor(Opcodes.ASM9) {
            public MethodVisitor visitMethod(int acc, String n, String d, String s, String[] e) {
                return new MethodVisitor(Opcodes.ASM9) {
                    public void visitLdcInsn(Object cv) {
                        after[0] = "Health".equals(cv);
                    }
                    public void visitFieldInsn(int op, String o, String fn, String fd) {
                        if (after[0] && op == Opcodes.GETFIELD && "I".equals(fd)) {
                            hit[0] = fn;
                            after[0] = false;
                        } else if (op != Opcodes.GETFIELD) {
                            after[0] = false;
                        }
                    }
                };
            }
        }, 0);
        return hit[0];
    }

    /** 语义锚 2：setPosRotM (DDDFF)V 方法体**参数槽追踪**——(this=0,d1@1,d2@3,
     *  d3@5,f1@7,f2@8)：ALOAD 7 后首个 PUTFIELD F=yaw、ALOAD 8 后=pitch
     *  （防 fallDistance=0 等旁路 PUTFIELD 混入序列位）。 */
    static String[] semanticYawPitch(String entity, String mname) {
        final String[] hit = new String[2];
        final int[] pend = new int[]{-1};
        byte[] b = (byte[]) CLS.get(entity);
        if (b == null) {
            return null;
        }
        new ClassReader(b).accept(new ClassVisitor(Opcodes.ASM9) {
            public MethodVisitor visitMethod(int acc, String n, String d, String s, String[] e) {
                // 不限方法名：b(DDDFF)V 可能是委托壳（真写字段在 c(DDDFF)V）——
                // 扫全部 (DDDFF)V 找 FLOAD 7/8→PUTFIELD F 的实体写者
                if (!"(DDDFF)V".equals(d)) {
                    return null;
                }
                return new MethodVisitor(Opcodes.ASM9) {
                    public void visitVarInsn(int op, int v) {
                        // float 参入栈=FLOAD（非 ALOAD）；f1@7、f2@8
                        pend[0] = (op == Opcodes.FLOAD && (v == 7 || v == 8)) ? v : -1;
                    }
                    public void visitFieldInsn(int op, String o, String fn, String fd) {
                        if (op == Opcodes.PUTFIELD && "F".equals(fd)) {
                            if (pend[0] == 7 && hit[0] == null) {
                                hit[0] = fn;
                            } else if (pend[0] == 8 && hit[1] == null) {
                                hit[1] = fn;
                            }
                        }
                        pend[0] = -1;
                    }
                    public void visitInsn(int op) {
                        pend[0] = -1;
                    }
                };
            }
        }, 0);
        return (hit[0] != null && hit[1] != null) ? hit : null;
    }

    /** name + 「单 L 类型参数返回 Z」精确匹配，抽参数类型（addStackM 派生）。 */
    static String singleLArgRetZ(String cls, String mname) {
        Info i = load(cls);
        if (i == null) {
            return null;
        }
        for (M m : i.methods) {
            if (m.name.equals(mname) && m.desc.startsWith("(L") && m.desc.endsWith(";)Z")) {
                return m.desc.substring(2, m.desc.length() - 3);
            }
        }
        return null;
    }

    /** 按 name + 参数前缀匹配方法，抽返回类型 Lxx;（同名重载安全）。 */
    static String methodRetType(String cls, String mname, String argPrefix) {
        Info i = load(cls);
        if (i == null) {
            return null;
        }
        for (M m : i.methods) {
            if (m.name.equals(mname) && m.desc.startsWith("(" + argPrefix)
                    && m.desc.endsWith(";") && m.desc.lastIndexOf('L') >= 0) {
                int s = m.desc.lastIndexOf('L');
                return m.desc.substring(s + 1, m.desc.length() - 1);
            }
        }
        return null;
    }

    /** 从 "()Lxx;" / "(Lxx;)Z" 抽第一个 L 类型内部名（去 L;）。 */
    static String firstLType(String desc) {
        if (desc == null) {
            return null;
        }
        int s = desc.indexOf('L');
        if (s < 0) {
            return null;
        }
        int e = desc.indexOf(';', s);
        if (e < 0) {
            return null;
        }
        return desc.substring(s + 1, e);
    }

    static void check(String slot, boolean ok, String detail) {
        if (ok) {
            pass++;
        } else {
            fail++;
            misses.add("MISS " + slot + " :: " + detail);
        }
    }

    @SuppressWarnings("unchecked")
    public static void main(String[] a) throws Exception {
        if (a.length < 3) {
            System.out.println("usage: SlotVerify <baseJson> <targetJar> <newVersionId> [outJson]");
            System.exit(2);
        }
        byte[] jb = readAll(new FileInputStream(a[0]));
        Map<String, Object> base = J.parse(new String(jb, "UTF-8"));
        Map<String, Object> ops = (Map<String, Object>) base.get("ops");
        Map<String, Object> hooks = (Map<String, Object>) base.get("hooks");

        // ---- 载 jar 全部类 ----
        ZipFile zf = new ZipFile(a[1]);
        java.util.Enumeration<? extends ZipEntry> en = zf.entries();
        while (en.hasMoreElements()) {
            ZipEntry e = en.nextElement();
            if (e.getName().endsWith(".class")) {
                String in = e.getName().substring(0, e.getName().length() - 6);
                CLS.put(in, readAll(zf.getInputStream(e)));
            }
        }
        zf.close();
        String sha1 = sha1Hex(a[1]);

        // ---- 派生链 ----
        Map<String, Object> sw = (Map<String, Object>) hooks.get("SAVE_WORLD");
        String world = (String) sw.get("class");
        Map<String, Object> lw = (Map<String, Object>) hooks.get("LOAD_WORLD");
        // LOAD_WORLD desc=(LWorld;Ljava/lang/String;LEntityPlayer;)V——EP=最后一个 L 参
        String ep = lastArgLType((String) lw.get("desc"));
        String gi = (String) ((Map<String, Object>) hooks.get("HUD_RENDER")).get("class");
        String entity = (String) ops.get("entityClassG");
        // EntityLiving=EntityPlayer 的超类（gq→lo；Entity 本身 extends Object）
        Info pi = load(ep);
        String eliving = pi == null ? null : pi.superName;
        // teGetM 需按 (III)Lxx; 全形匹配（World 同名 b 有重载）
        String te = methodRetType(world, (String) ops.get("teGetM"), "III)");
        String wi = firstLType(methodDesc(world, (String) ops.get("getWorldInfoM")));
        String inv = trimL(fieldType(ep, (String) ops.get("invF")));
        // addStackM 需精确匹配「单 L 参返回 Z」（InventoryPlayer 同名 a 有重载）
        String is = singleLArgRetZ(inv, (String) ops.get("addStackM"));
        // sleepInBedM 需精确匹配「(III)Lxx;」（EntityPlayer 同名 b 重载多）
        String es = methodRetType(ep, (String) ops.get("sleepInBedM"), "III)");
        // IInventory 宿主派生：TE 具体子类（TileEntityChest=super==te 且有 [LIS; 数组字段）
        String chest = null;
        for (String cn : CLS.keySet()) {
            Info ci = load(cn);
            if (ci != null && te.equals(ci.superName) && hasField(cn, "a", "[L" + is + ";")) {
                chest = cn;
            }
        }

        System.out.println("derived: World=" + world + " EP=" + ep + " GI=" + gi
                + " Entity=" + entity + " EntityLiving=" + eliving
                + " TileEntity=" + te + " WorldInfo=" + wi + " Chest=" + chest
                + " InventoryPlayer=" + inv + " ItemStack=" + is + " EnumStatus=" + es);

        // ---- hooks 四项全验（class+method+desc+access）----
        String[] hk = {"SAVE_WORLD", "LOAD_WORLD", "RUN_TICK", "HUD_RENDER"};
        for (String k : hk) {
            Map<String, Object> h = (Map<String, Object>) hooks.get(k);
            String hc = (String) h.get("class");
            String hm = (String) h.get("method");
            String hd = (String) h.get("desc");
            int acc = Integer.parseInt(((String) h.get("access")).substring(2), 16);
            Info hi = load(hc);
            boolean found = false;
            if (hi != null) {
                for (M m : hi.methods) {
                    if (m.name.equals(hm) && m.desc.equals(hd) && (m.access & 0xFFFF) == acc) {
                        found = true;
                    }
                }
            }
            check("hook." + k, found, hm + hd + " access=" + Integer.toHexString(acc) + " @ " + hc);
        }

        // ---- World 槽 ----
        String w = world;
        check("setBlockM", hasMethod(w, s(ops, "setBlockM"), "(IIII)Z"), "");
        check("getBlockM", hasMethod(w, s(ops, "getBlockM"), "(III)I"), "");
        check("getMetaM", hasMethod(w, s(ops, "getMetaM"), "(III)I"), "");
        check("setMetaSilentM", hasMethod(w, s(ops, "setMetaSilentM"), "(IIII)Z"), "");
        check("getLightM", hasMethod(w, s(ops, "getLightM"), "(III)I"), "");
        check("teGetM", hasMethod(w, s(ops, "teGetM"), "(III)L" + te + ";"), "TE=" + te);
        check("getWorldInfoM", hasMethod(w, s(ops, "getWorldInfoM"), "()L" + wi + ";"), "WI=" + wi);
        check("playSoundM", hasMethod(w, s(ops, "playSoundM"), "(DDDLjava/lang/String;FF)V"), "");
        check("spawnEntityM", hasMethod(w, s(ops, "spawnEntityM"), "(L" + entity + ";)Z"), "");
        check("setEntityDeadM", hasMethod(w, s(ops, "setEntityDeadM"), "(L" + entity + ";)V"), "");
        check("entityListM", hasMethod(w, s(ops, "entityListM"), "()Ljava/util/List;"), "");
        String em = s(ops, "explodeM");
        boolean explodeOk = false;
        Info wi2 = load(w);
        if (wi2 != null) {
            for (M m : wi2.methods) {
                if (m.name.equals(em) && m.desc.startsWith("(L" + entity + ";DDDF)")) {
                    explodeOk = true;
                }
            }
        }
        check("explodeM", explodeOk, em + "(L" + entity + ";DDDF)*");
        check("difficultyF", hasField(w, s(ops, "difficultyF"), "I"), "");
        check("rainStrF", hasField(w, s(ops, "rainStrF"), "F"), "");
        check("thunderStrF", hasField(w, s(ops, "thunderStrF"), "F"), "");
        check("playerListF", hasField(w, s(ops, "playerListF"), "Ljava/util/List;"), "");

        // ---- WorldInfo 槽 ----
        if (wi != null) {
            check("worldTimeF", hasField(wi, s(ops, "worldTimeF"), "J"), "");
            check("randSeedF", hasField(wi, s(ops, "randSeedF"), "J"), "");
            check("spawnXF", hasField(wi, s(ops, "spawnXF"), "I"), "");
            check("spawnYF", hasField(wi, s(ops, "spawnYF"), "I"), "");
            check("spawnZF", hasField(wi, s(ops, "spawnZF"), "I"), "");
            check("wiSetRainM", hasMethod(wi, s(ops, "wiSetRainM"), "(Z)V"), "");
            check("wiSetThunderM", hasMethod(wi, s(ops, "wiSetThunderM"), "(Z)V"), "");
        } else {
            check("WorldInfo-derive", false, "getWorldInfoM 返回类型派生失败");
        }

        // ---- Entity/EntityLiving 槽 ----
        check("posXF", hasField(entity, s(ops, "posXF"), "D"), "");
        check("posYF", hasField(entity, s(ops, "posYF"), "D"), "");
        check("posZF", hasField(entity, s(ops, "posZF"), "D"), "");
        check("motXF", hasField(entity, s(ops, "motXF"), "D"), "");
        check("motYF", hasField(entity, s(ops, "motYF"), "D"), "");
        check("motZF", hasField(entity, s(ops, "motZF"), "D"), "");
        String[] yp = semanticYawPitch(entity, s(ops, "setPosRotM"));
        check("yawF", yp != null && yp[0].equals(s(ops, "yawF")),
                "semantic=" + (yp == null ? "?" : yp[0]) + " table=" + s(ops, "yawF"));
        check("pitchF", yp != null && yp[1].equals(s(ops, "pitchF")),
                "semantic=" + (yp == null ? "?" : yp[1]) + " table=" + s(ops, "pitchF"));
        check("setPosRotM", hasMethod(entity, s(ops, "setPosRotM"), (String) ops.get("setPosRotDesc")), "");
        String hf = s(ops, "healthF");
        String sh = semanticHealth(eliving);
        check("healthF", hf.equals(sh), "semantic=" + sh + " table=" + hf);

        // ---- EntityPlayer 槽 ----
        check("sleepInBedM", es != null, "ret=" + es);
        check("wakeUpM", hasMethod(ep, s(ops, "wakeUpM"), "(ZZZ)V"), "");
        check("invF", hasField(ep, s(ops, "invF"), "L" + inv + ";"), "inv=" + inv);
        if (es != null) {
            check("sleepOkF", hasField(es, s(ops, "sleepOkF"), "L" + es + ";"), "EnumStatus=" + es);
        }

        // ---- Minecraft 槽 ----
        String mc = "net/minecraft/client/Minecraft";
        check("hudPlayerF", fieldType(mc, s(ops, "hudPlayerF")) != null, "");
        check("hudFontF", fieldType(mc, s(ops, "hudFontF")) != null, "");
        check("hudSettingsF", fieldType(mc, s(ops, "hudSettingsF")) != null, "");
        check("hudDebugGateF", fieldType(mc, s(ops, "hudDebugGateF")) != null, "");

        // ---- InventoryPlayer/ItemStack/Item 槽 ----
        // InventoryPlayer main/armor：G2 系=List、G3(b1.6+)=ItemStack[] 数组形——两形皆过
        check("invMainF", hasField(inv, s(ops, "invMainF"), "Ljava/util/List;")
                || hasField(inv, s(ops, "invMainF"), "[L" + is + ";"), "inv=" + inv);
        check("invArmorF", hasField(inv, s(ops, "invArmorF"), "Ljava/util/List;")
                || hasField(inv, s(ops, "invArmorF"), "[L" + is + ";"), "");
        check("invCurF", hasField(inv, s(ops, "invCurF"), "I"), "");
        check("addStackM", hasMethod(inv, s(ops, "addStackM"), "(L" + is + ";)Z"), "IS=" + is);
        check("invSizeM", hasMethod(chest, s(ops, "invSizeM"), "()I"), "chest=" + chest);
        check("isCountF", hasField(is, s(ops, "isCountF"), "I"), "IS=" + is);
        check("isIdF", hasField(is, s(ops, "isIdF"), "I"), "");
        check("isDmgF", hasField(is, s(ops, "isDmgF"), "I"), "");
        String item = s(ops, "itemClassG");
        check("itemClassG", CLS.containsKey(item), item);
        check("itemsF", hasField(item, s(ops, "itemsF"), "[L" + item + ";"), "");
        check("invGetM", hasMethod(chest, s(ops, "invGetM"), "(I)L" + is + ";"), "chest=" + chest);
        check("invSetM", hasMethod(chest, s(ops, "invSetM"), "(IL" + is + ";)V"), "");

        // ---- EntityList 槽 ----
        String el = s(ops, "entityListClassG");
        check("entityListClassG", CLS.containsKey(el), el);
        check("entityListLookupM", hasMethod(el, s(ops, "entityListLookupM"),
                "(L" + entity + ";)Ljava/lang/String;"), "");

        // ---- itemEntityG（EntityItem：public ItemStack 字段 a）----
        String ie = s(ops, "itemEntityG");
        check("itemEntityG", CLS.containsKey(ie) && hasField(ie, "a", "L" + is + ";"), ie + ".a:L" + is + ";");

        // ---- mobClasses ----
        Map<String, Object> mobs = (Map<String, Object>) ops.get("mobClasses");
        for (String mk : mobs.keySet()) {
            String mv = (String) mobs.get(mk);
            if (mv == null) {
                continue;
            }
            Info mi = load(mv);
            boolean ctor = false;
            if (mi != null) {
                for (M m : mi.methods) {
                    if (m.name.equals("<init>") && m.desc.contains("L" + world + ";")) {
                        ctor = true;
                    }
                }
            }
            check("mob." + mk, ctor, mv + " ctor(*L" + world + ";*)");
        }

        // ---- 输出草稿 JSON ----
        String out = a.length > 3 ? a[3] : (a[0].substring(0, a[0].lastIndexOf('\\') + 1)
                + a[2].replaceAll("[^A-Za-z0-9._-]", "_") + ".slotverify.json");
        StringBuilder sb = new StringBuilder();
        sb.append("{\n  \"schema\": \"mca-fingerprint-v1\",\n");
        sb.append("  \"versionId\": \"").append(a[2]).append("\",\n");
        sb.append("  \"family\": \"").append(base.get("family")).append("\",\n");
        sb.append("  \"jar\": \"%APPDATA%/.betacraft/versions/").append(a[2]).append(".jar\",\n");
        sb.append("  \"jarSha1\": \"").append(sha1).append("\",\n");
        sb.append("  \"provenance\": {\n    \"evidence\": \"SlotVerify v1 自动继承校验（base=")
                .append(base.get("versionId")).append("），PASS=").append(pass)
                .append(" MISS=").append(fail).append("\",\n");
        sb.append("    \"mode\": \"slotverify-auto\",\n");
        sb.append("    \"note\": \"").append(fail == 0 ? "全槽继承成立（邻版零断代）" : "MISS 槽清单见 evidence 尾部 "
                .concat(String.join(" | ", misses)).replace("\"", "'")).append("\"\n  },\n");
        // 语义腿新字采纳（断代槽草稿直接带正确字母，取证依据=字节码语义锚）
        List<String> adopted = new ArrayList<String>();
        if (sh != null && !sh.equals(hf)) {
            ops.put("healthF", sh);
            adopted.add("healthF=" + sh + "(was " + hf + ")");
        }
        if (yp != null && !yp[0].equals(s(ops, "yawF"))) {
            ops.put("yawF", yp[0]);
            adopted.add("yawF=" + yp[0]);
        }
        if (yp != null && !yp[1].equals(s(ops, "pitchF"))) {
            ops.put("pitchF", yp[1]);
            adopted.add("pitchF=" + yp[1]);
        }
        // ops/hooks/capabilities 继承（语义断代槽已采纳新字）
        if (!adopted.isEmpty()) {
            System.out.println("ADOPTED: " + String.join(" ", adopted));
        }
        sb.append("  \"ops\": ").append(jsonOf(ops)).append(",\n");
        sb.append("  \"hooks\": ").append(jsonOf(hooks)).append(",\n");
        sb.append("  \"capabilities\": ").append(jsonOf((Map<String, Object>) base.get("capabilities"))).append("\n}\n");
        FileOutputStream fo = new FileOutputStream(out);
        fo.write(sb.toString().getBytes("UTF-8"));
        fo.close();

        System.out.println("RESULT PASS=" + pass + " MISS=" + fail);
        for (String m : misses) {
            System.out.println(m);
        }
        System.out.println("draft written: " + out);
        System.exit(fail == 0 ? 0 : 1);
    }

    static String s(Map<String, Object> m, String k) {
        Object v = m.get(k);
        return v == null ? "?" : (String) v;
    }

    static String trimL(String desc) {
        return desc == null ? null : firstLType(desc);
    }

    /** 极简 JSON 序列化（仅覆盖锚库 schema 用到的 Map/List/String/Number/Boolean）。 */
    @SuppressWarnings("unchecked")
    static String jsonOf(Object o) {
        if (o == null) {
            return "null";
        }
        if (o instanceof Map) {
            StringBuilder sb = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<String, Object> e : ((Map<String, Object>) o).entrySet()) {
                if (!first) {
                    sb.append(",");
                }
                first = false;
                sb.append('"').append(e.getKey()).append("\":").append(jsonOf(e.getValue()));
            }
            return sb.append("}").toString();
        }
        if (o instanceof List) {
            StringBuilder sb = new StringBuilder("[");
            boolean first = true;
            for (Object x : (List<Object>) o) {
                if (!first) {
                    sb.append(",");
                }
                first = false;
                sb.append(jsonOf(x));
            }
            return sb.append("]").toString();
        }
        if (o instanceof String) {
            return "\"" + ((String) o).replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
        }
        if (o instanceof Boolean) {
            return o.toString();
        }
        if (o instanceof Double) {
            double d = (Double) o;
            if (d == Math.floor(d) && !Double.isInfinite(d)) {
                return String.valueOf((long) d);
            }
            return String.valueOf(d);
        }
        return o.toString();
    }

    static byte[] readAll(InputStream in) throws Exception {
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) {
            bo.write(buf, 0, n);
        }
        in.close();
        return bo.toByteArray();
    }

    /** 锚库 JSON 解析（对象/数组/字符串/Double/Boolean/null 全支持——
     *  与协议面 MiniJson 子集无关，工具侧专用）。 */
    static final class J {
        private final String s;
        private int i;
        J(String s) { this.s = s; }
        static Map<String, Object> parse(String t) {
            J p = new J(t);
            p.ws();
            Object v = p.val();
            if (!(v instanceof Map)) {
                throw new RuntimeException("top not object");
            }
            return (Map<String, Object>) v;
        }
        void ws() { while (i < s.length() && Character.isWhitespace(s.charAt(i))) { i++; } }
        Object val() {
            ws();
            char c = s.charAt(i);
            if (c == '{') { return obj(); }
            if (c == '[') { return arr(); }
            if (c == '"') { return str(); }
            if (c == 't') { i += 4; return Boolean.TRUE; }
            if (c == 'f') { i += 5; return Boolean.FALSE; }
            if (c == 'n') { i += 4; return null; }
            int st = i;
            while (i < s.length() && "+-.0123456789eE".indexOf(s.charAt(i)) >= 0) { i++; }
            return Double.parseDouble(s.substring(st, i));
        }
        Map<String, Object> obj() {
            LinkedHashMap<String, Object> m = new LinkedHashMap<String, Object>();
            i++;
            ws();
            if (s.charAt(i) == '}') { i++; return m; }
            while (true) {
                ws();
                String k = str();
                ws();
                if (s.charAt(i) != ':') { throw new RuntimeException("expect : at " + i); }
                i++;
                m.put(k, val());
                ws();
                char c = s.charAt(i++);
                if (c == '}') { return m; }
                if (c != ',') { throw new RuntimeException("expect , at " + (i - 1)); }
            }
        }
        List<Object> arr() {
            List<Object> l = new ArrayList<Object>();
            i++;
            ws();
            if (s.charAt(i) == ']') { i++; return l; }
            while (true) {
                l.add(val());
                ws();
                char c = s.charAt(i++);
                if (c == ']') { return l; }
                if (c != ',') { throw new RuntimeException("expect , in arr"); }
            }
        }
        String str() {
            if (s.charAt(i) != '"') { throw new RuntimeException("expect str at " + i); }
            i++;
            StringBuilder b = new StringBuilder();
            while (s.charAt(i) != '"') {
                char c = s.charAt(i++);
                if (c == '\\') {
                    char e = s.charAt(i++);
                    if (e == 'n') { b.append('\n'); }
                    else if (e == 't') { b.append('\t'); }
                    else if (e == 'u') { b.append((char) Integer.parseInt(s.substring(i, i + 4), 16)); i += 4; }
                    else { b.append(e); }
                } else {
                    b.append(c);
                }
            }
            i++;
            return b.toString();
        }
    }

    static String sha1Hex(String path) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        FileInputStream fi = new FileInputStream(path);
        byte[] buf = new byte[65536];
        int n;
        while ((n = fi.read(buf)) > 0) {
            md.update(buf, 0, n);
        }
        fi.close();
        StringBuilder sb = new StringBuilder();
        for (byte b : md.digest()) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
