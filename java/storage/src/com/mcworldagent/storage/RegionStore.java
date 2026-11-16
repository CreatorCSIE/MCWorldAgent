package com.mcworldagent.storage;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Map;
import java.util.zip.InflaterInputStream;

/**
 * McRegion (.mcr) 只读解析器（b1.7.3 存储断代批，verscan/b1.7.3.md §5）。
 * Beta 1.3 起 chunk 存储从散件 c.&lt;x&gt;.&lt;z&gt;.dat 目录树切换为 32x32 chunk 区域文件：
 * 路径 = &lt;worldDir&gt;/region/r.&lt;x&gt;&gt;5.&lt;z&gt;&gt;5.mcr（下界 = &lt;worldDir&gt;/DIM-1/region/——
 * 传 DIM-1 目录作 worldDir 即自然命中，本件零特判）。
 * 文件形制（字节码 RegionFile 对榫+el173-1 实盘 hexdump 判决）：
 *   [0,4096)   定位表 1024 个 BE int：高 3 字节=**分配单位号**、低字节=单位数
 *              （b1.7.3 游戏固定每 chunk 1 单位=恒 1；0=未生成）；
 *   [4096,8192) 时间表（最后修改秒，本件只读不消费）；
 *   数据区：chunk 头 = 4B BE 负载长度 + 1B 压缩版本（1=gzip、2=zlib——NbtReader
 *            两路解码），其余=压缩 NBT。**偏移 = 单位号 × 4096 绝对定位**——
 *            b1.3–b1.7 beta McRegion 每 chunk 固定分配 8 扇区（4096B）容量，
 *            单位 0–1 即头两表（首 chunk 单位=2→8192）；⚠️ 非 Anvil 的
 *            "扇区号×512+8192 紧排"语义（el173-1 实盘 303 chunk 全落 4096 步长判决）。
 * 只读红线（同 ChunkExport）：RandomAccessFile "r" 模式全程零写入，
 * 绝不触碰游戏目录生成语义。
 */
public final class RegionStore {

    public static final int SECTOR = 512;
    public static final int LOCATION_TABLE_BYTES = 4096;
    /** b1.7.3 分配单位 = 8 扇区 = 4096B（游戏 RegionFileChunkBuffer 固定容量）。 */
    public static final int UNIT_BYTES = 4096;

    private RegionStore() {
    }

    /** chunk 全局坐标 → 区域文件路径（负坐标=Integer.toString 原生 `-` 前缀，实盘 r.-1.2.mcr 对榫）。 */
    public static File regionFile(File worldDir, int x, int z) {
        return new File(new File(worldDir, "region"),
                "r." + (x >> 5) + "." + (z >> 5) + ".mcr");
    }

    /**
     * chunk 全局坐标 → root compound（Level 复合体键集与散件 .dat 同构——Blocks/Data/
     * SkyLight/BlockLight/HeightMap/TileEntities/xPos/zPos/LastUpdate）。
     * 区域文件缺失或定位表条目=0 → null（present=false 语义，与散件 miss 同判）。
     */
    public static Map<String, Object> readChunk(File worldDir, int x, int z) throws IOException {
        File rf = regionFile(worldDir, x, z);
        if (!rf.isFile()) {
            return null;
        }
        RandomAccessFile raf = new RandomAccessFile(rf, "r");
        try {
            if (raf.length() < LOCATION_TABLE_BYTES * 2L) {
                throw new IOException("region file truncated: " + rf);
            }
            int slot = (x & 31) + (z & 31) * 32;
            raf.seek(slot * 4L);
            int entry = raf.readInt();
            if (entry == 0) {
                return null;
            }
            int unit = entry >>> 8;
            int units = entry & 0xFF;
            long dataOff = (long) unit * UNIT_BYTES;
            if (units == 0 || dataOff < LOCATION_TABLE_BYTES * 2L || dataOff + 5L > raf.length()) {
                throw new IOException("bad location entry " + Integer.toHexString(entry)
                        + " at slot=" + slot + " in " + rf);
            }
            raf.seek(dataOff);
            int len = raf.readInt();
            int version = raf.readUnsignedByte();
            if (len < 1 || dataOff + 5L + len > raf.length()) {
                throw new IOException("chunk payload out of bounds: len=" + len
                        + " off=" + dataOff + " file=" + rf.length() + " in " + rf);
            }
            byte[] payload = new byte[len];
            raf.readFully(payload);
            if (version == 1) {
                return NbtReader.readGz(payload);
            }
            if (version == 2) {
                return NbtReader.readRaw(inflate(payload));
            }
            throw new IOException("unknown chunk compression version: " + version + " in " + rf);
        } finally {
            raf.close();
        }
    }

    private static byte[] inflate(byte[] zlib) throws IOException {
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        InflaterInputStream in = new InflaterInputStream(new ByteArrayInputStream(zlib));
        try {
            byte[] b = new byte[8192];
            int n;
            while ((n = in.read(b)) > 0) {
                bo.write(b, 0, n);
            }
        } finally {
            in.close();
        }
        return bo.toByteArray();
    }
}
