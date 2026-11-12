package com.mcworldagent.agent;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;

/**
 * 严格匹配织入器：先验 HookTargets 指纹（类名→方法名→描述符→access→命中数），
 * 全中才 ASM 注入；任一不符 → UNSUPPORTED 日志 + 整类跳过（绝不静默错钩）。
 *
 * 注入序列均为对称压弹（压入局部变量/复制返回值 → INVOKESTATIC 弹回原形），
 * 不新增控制流，故不重算 StackMapTable（ClassWriter 仅 COMPUTE_MAXS，
 * 不触发 getCommonSuperClass 类加载）。
 */
public final class Transformers implements ClassFileTransformer {

    private static final String HOOKS = "com/mcworldagent/agent/MCAHooks";

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> beingRedefined,
                            ProtectionDomain protectionDomain, byte[] classFileBuffer) {
        if (className == null) {
            return null;
        }
        MCWorldAgent.logLoad(className, loader);
        HookTargets.Target[] all = HookTargets.forClass(className);
        if (all.length == 0) {
            return null;
        }
        java.util.List<HookTargets.Target> enabled =
                new java.util.ArrayList<HookTargets.Target>();
        for (HookTargets.Target t : all) {
            if (!MCWorldAgent.disabled.contains(t.hookId)
                    && !MCWorldAgent.disabled.contains(className)) {
                enabled.add(t);
            }
        }
        if (enabled.isEmpty()) {
            MCWorldAgent.logTransform("SKIP-DISABLED " + className);
            return null;
        }
        HookTargets.Target[] targets = enabled.toArray(new HookTargets.Target[0]);
        try {
            return instrument(className, classFileBuffer, targets);
        } catch (Throwable t) {
            MCWorldAgent.logTransform("ERROR " + className + ": " + t);
            return null;
        }
    }

    private static byte[] instrument(String className, byte[] bytes, HookTargets.Target[] targets) {
        // 第一遍：指纹校验（只数方法，不读码）
        final int[] hits = new int[targets.length];
        final int[] accessMismatch = new int[targets.length];
        new ClassReader(bytes).accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc,
                                             String signature, String[] exceptions) {
                for (int i = 0; i < targets.length; i++) {
                    HookTargets.Target t = targets[i];
                    if (t.methodName.equals(name) && t.desc.equals(desc)) {
                        hits[i]++;
                        if (access != t.access) {
                            accessMismatch[i]++;
                        }
                    }
                }
                return null;
            }
        }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);

        boolean ok = true;
        for (int i = 0; i < targets.length; i++) {
            HookTargets.Target t = targets[i];
            if (hits[i] != t.expectedHits) {
                MCWorldAgent.logTransform("UNSUPPORTED " + t.label + " @ " + className
                        + ": hits=" + hits[i] + " expected=" + t.expectedHits);
                ok = false;
            } else if (accessMismatch[i] > 0) {
                MCWorldAgent.logTransform("UNSUPPORTED " + t.label + " @ " + className
                        + ": access mismatch (" + accessMismatch[i] + " method(s) flags != 0x"
                        + Integer.toHexString(t.access) + ")");
                ok = false;
            }
        }
        if (!ok) {
            return null; // 保守：整类不碰
        }

        // 第二遍：注入
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        new ClassReader(bytes).accept(new ClassVisitor(Opcodes.ASM9, writer) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc,
                                             String signature, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
                for (HookTargets.Target t : targets) {
                    if (t.methodName.equals(name) && t.desc.equals(desc)) {
                        return new InjectMV(mv, t);
                    }
                }
                return mv;
            }
        }, 0);

        for (HookTargets.Target t : targets) {
            MCWorldAgent.logTransform("HOOKED " + t.label + " @ " + className
                    + " [" + t.methodName + t.desc + "]");
        }
        byte[] out = writer.toByteArray();
        if (MCWorldAgent.dumpDir != null) {
            try {
                java.io.File f = new java.io.File(MCWorldAgent.dumpDir,
                        className.replace('/', '.') + ".class");
                java.io.FileOutputStream fos = new java.io.FileOutputStream(f);
                try {
                    fos.write(out);
                } finally {
                    fos.close();
                }
                MCWorldAgent.logTransform("DUMPED " + f.getAbsolutePath());
            } catch (Throwable t) {
                MCWorldAgent.logTransform("DUMP-ERROR " + className + ": " + t);
            }
        }
        return out;
    }

    /** 单方法注入器：按 hookId dispatch。 */
    private static final class InjectMV extends MethodVisitor {
        private final HookTargets.Target target;

        InjectMV(MethodVisitor mv, HookTargets.Target target) {
            super(Opcodes.ASM9, mv);
            this.target = target;
        }

        @Override
        public void visitCode() {
            super.visitCode();
            if (target.injectMode == HookTargets.ENTRY
                    || target.injectMode == HookTargets.ENTRY_AND_RESULT) {
                switch (target.hookId) {
                    case HookTargets.SAVE_WORLD: // (Z)V: slot0=this(已初始化) slot1=boolean
                        mv.visitVarInsn(Opcodes.ALOAD, 0);
                        mv.visitVarInsn(Opcodes.ILOAD, 1);
                        mv.visitMethodInsn(Opcodes.INVOKESTATIC, HOOKS, "onSaveWorld",
                                "(Ljava/lang/Object;Z)V", false);
                        break;
                    case HookTargets.WORLD_CTOR: // <init>(File,String): slot1=File（引用槽合法，slot0=this 构造期不可作实参）
                        mv.visitVarInsn(Opcodes.ALOAD, 1);
                        mv.visitMethodInsn(Opcodes.INVOKESTATIC, HOOKS, "onWorldDir",
                                "(Ljava/io/File;)V", false);
                        break;
                    case HookTargets.RUN_TICK: // ()V
                        mv.visitMethodInsn(Opcodes.INVOKESTATIC, HOOKS, "onTick", "()V", false);
                        break;
                    case HookTargets.LOAD_CHUNK: // (II)LChunk;: slot1=x slot2=z
                        mv.visitVarInsn(Opcodes.ILOAD, 1);
                        mv.visitVarInsn(Opcodes.ILOAD, 2);
                        mv.visitMethodInsn(Opcodes.INVOKESTATIC, HOOKS, "onLoadChunkEnter", "(II)V", false);
                        break;
                    case HookTargets.GEN_HEIGHT: // ()V
                        mv.visitMethodInsn(Opcodes.INVOKESTATIC, HOOKS, "onGenerateHeightMap", "()V", false);
                        break;
                    default:
                        throw new IllegalStateException("unknown entry hook " + target.hookId);
                }
            }
        }

        @Override
        public void visitInsn(int opcode) {
            if (opcode == Opcodes.ARETURN
                    && target.injectMode == HookTargets.ENTRY_AND_RESULT) {
                if (!HookTargets.LOAD_CHUNK.equals(target.hookId)) {
                    throw new IllegalStateException("unknown areturn hook " + target.hookId);
                }
                mv.visitInsn(Opcodes.DUP);
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, HOOKS, "onLoadChunkResult",
                        "(Ljava/lang/Object;)V", false);
            }
            super.visitInsn(opcode);
        }
    }
}
