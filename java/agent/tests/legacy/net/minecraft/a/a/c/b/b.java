package net.minecraft.a.a.c.b;

import java.util.Random;

import mcaut.MiniJsonTest.FakeWorld;

/**
 * 0327 散布器替身（测试专用，真 FQCN 上 classpath 供 WorldOps.resolveScatter 正路径）。
 * 真类语义：椭球曲线、命中格仅当现为 grass 时静默 a(IIII)Z 写入构造器 id、恒返 true。
 * 替身简化：影响盒 [x+6..x+10]x[y+2..y+4]x[z+6..z+10] 内现 id=2(grass) 全写 target（直写 blocks 面）。
 */
public class b {
    public final int target;

    public b(int id) {
        this.target = id;
    }

    public final boolean a(FakeWorld w, Random r, int x, int y, int z) {
        for (int cy = y + 2; cy <= y + 4; cy++) {
            for (int cx = x + 6; cx <= x + 10; cx++) {
                for (int cz = z + 6; cz <= z + 10; cz++) {
                    String k = cx + "," + cy + "," + cz;
                    Integer cur = w.blocks.get(k);
                    int id = cur == null ? w.readId : cur.intValue();
                    if (id == 2) {
                        w.blocks.put(k, Integer.valueOf(target));
                    }
                }
            }
        }
        return true;
    }
}
