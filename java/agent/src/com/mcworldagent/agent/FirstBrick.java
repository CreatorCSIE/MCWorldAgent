package com.mcworldagent.agent;

import java.lang.reflect.Method;

/**
 * Phase 1 "第一块砖"：反射调用游戏自身的方块写入方法（零新字节码注入 → 无 old verifier 风险）。
 *
 * 写侧指纹表（0327 混淆实名，javap 字节码特征钉死，案卷 phase1-first-brick §T1）：
 *   World = net.minecraft.a.a.f
 *     setBlockWithNotify = c(IIII)Z   public final  —— 唯一写入口（自带光照/脏标记/渲染通知）
 *     getBlockId         = a(III)I    public final  —— 读回验证
 *     spawnX / spawnZ    = g / i      public int    —— NBT 锚点 SpawnX/SpawnZ
 *     saveWorld          = a(Z)V      private       —— Phase 0 已钉，显式存盘（G1 结论）
 *
 * 目标方块：cobblestone ID=4 —— 无重力、无更新副作用，且 0327 地表不会自然生成
 * （stone ID=1 会地表暴露导致"已存在"误判，故不用）。
 *
 * 执行纪律：仅经 MCAHooks.submit() 排队，在 onTick() drain（游戏线程 tick 边界）执行，
 * 与玩家挖/放方块同线程同方法，worldAccesses 渲染通知天然安全。
 */
public final class FirstBrick {

    private FirstBrick() {
    }

    private static final int COBBLESTONE_ID = 4;

    /** 一次性闸门：达到终态（放置/验证/占用/拒绝）后本进程不再执行；PROBE_FAIL 允许重试。
     *  防 save 命令触发 saveWorld 钩后重复补砖（案卷 v2 §7）。 */
    private static volatile boolean once;

    /** 在活体 World 上执行一次"读回或放置"。只在游戏线程调用。 */
    public static void run(Object world) {
        if (once) {
            return;
        }
        try {
            Class<?> wc = world.getClass();
            Method setBlock = wc.getMethod("c", int.class, int.class, int.class, int.class);
            Method getBlock = wc.getMethod("a", int.class, int.class, int.class);

            int x = wc.getField("g").getInt(world);
            int z = wc.getField("i").getInt(world);

            // 从 y=100 向下探地表：第一个非空气块
            int y0 = -1;
            for (int y = 100; y >= 1; y--) {
                if (((Integer) getBlock.invoke(world, x, y, z)).intValue() != 0) {
                    y0 = y;
                    break;
                }
            }
            if (y0 < 0) {
                MCAHooks.log("FIRST_BRICK_PROBE_FAIL x=" + x + " z=" + z + " 无地表");
                return;
            }

            int topId = ((Integer) getBlock.invoke(world, x, y0, z)).intValue();
            if (topId == COBBLESTONE_ID) {
                once = true;
                MCAHooks.log("FIRST_BRICK_VERIFY_OK x=" + x + " y=" + y0 + " z=" + z
                        + " id=" + topId + "（上一 run 放置的砖仍在，重进载入成功）");
                return;
            }

            int aboveId = ((Integer) getBlock.invoke(world, x, y0 + 1, z)).intValue();
            if (aboveId != 0) {
                once = true;
                MCAHooks.log("FIRST_BRICK_OCCUPIED x=" + x + " y=" + (y0 + 1) + " z=" + z
                        + " id=" + aboveId + " 目标格非空，放弃");
                return;
            }

            boolean placed = ((Boolean) setBlock.invoke(world,
                    Integer.valueOf(x), Integer.valueOf(y0 + 1), Integer.valueOf(z),
                    Integer.valueOf(COBBLESTONE_ID))).booleanValue();
            if (!placed) {
                once = true;
                MCAHooks.log("FIRST_BRICK_PLACE_REJECTED x=" + x + " y=" + (y0 + 1) + " z=" + z);
                return;
            }
            MCAHooks.log("FIRST_BRICK_PLACED x=" + x + " y=" + (y0 + 1) + " z=" + z
                    + " id=" + COBBLESTONE_ID + " surface=" + topId);

            // 显式存盘（G1：不可依赖游戏 autosave）
            Method saveWorld = wc.getDeclaredMethod("a", boolean.class);
            saveWorld.setAccessible(true);
            saveWorld.invoke(world, Boolean.FALSE);
            MCAHooks.log("FIRST_BRICK_SAVED saveWorld(false) 已调用");
            once = true;
        } catch (Throwable t) {
            once = true;
            MCAHooks.log("FIRST_BRICK_UNSUPPORTED " + t);
        }
    }
}
