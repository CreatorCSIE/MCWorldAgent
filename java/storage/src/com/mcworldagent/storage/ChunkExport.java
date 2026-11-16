package com.mcworldagent.storage;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 0327 chunk 只读导出 CLI（chunk-export 批）。
 * 用法：java com.mcworldagent.storage.ChunkExport <worldDir> <x> <z> [out.json]
 * 只读红线：全程零写入游戏目录；文件缺失=present:false，绝不触碰生成语义
 * （plan.md §3.4/§4.3——provider 就地生成仅存在于活体面，本件直读盘天然免疫）。
 *
 * 索引/半字节公式=字节码实锤（spec §1 翻案版）：
 *   Blocks idx = x<<11 | z<<7 | y（Chunk.a(III)I baload e:[B）；
 *   nibble 偶 idx=低 4 位、奇=高 4 位（NibbleArray.a(III)I）。
 * 线性遍历序 idx 0..32767 = y 最内、z 中、x 最外（非 getblocks 活体序，导出侧自洽）。
 */
public final class ChunkExport {

    public static final int BLOCKS_LEN = 32768;   // 16*16*128
    public static final int NIBBLE_LEN = 16384;   // BLOCKS_LEN/2
    public static final int SECT = 16;
    public static final int HEIGHT = 128;

    /** 0327 方块索引（字节码实锤公式）。 */
    public static int blockIndex(int x, int y, int z) {
        return (x << 11) | (z << 7) | y;
    }

    /** nibble 读（偶 idx=低 4 位、奇=高 4 位；idx 为方块线性索引）。 */
    public static int nibble(byte[] arr, int idx) {
        int b = arr[idx >> 1] & 0xFF;
        return (idx & 1) == 0 ? b & 0x0F : (b >> 4) & 0x0F;
    }

    /** b36 存档路径（负数=Integer.toString 原生 `-` 前缀，实盘 c.0.-s.dat 对榫）。 */
    public static File chunkFile(File worldDir, int x, int z) {
        String sx = Integer.toString(x, 36);
        String sz = Integer.toString(z, 36);
        return new File(new File(new File(worldDir, sx), sz), "c." + sx + "." + sz + ".dat");
    }

    /** RLE：值数组 → `val:count|val:count`（getblocks 编码先例；count 十进制）。 */
    public static String rle(int[] vals) {
        StringBuilder sb = new StringBuilder();
        int runVal = -1, runLen = 0;
        for (int v : vals) {
            if (v == runVal) {
                runLen++;
            } else {
                if (runLen > 0) {
                    if (sb.length() > 0) {
                        sb.append('|');
                    }
                    sb.append(runVal).append(':').append(runLen);
                }
                runVal = v;
                runLen = 1;
            }
        }
        if (runLen > 0) {
            if (sb.length() > 0) {
                sb.append('|');
            }
            sb.append(runVal).append(':').append(runLen);
        }
        return sb.toString();
    }

    /** nibble 数组展开为值数组（线性 idx 序）。 */
    public static int[] expandNibble(byte[] arr) {
        int[] out = new int[BLOCKS_LEN];
        for (int i = 0; i < BLOCKS_LEN; i++) {
            out[i] = nibble(arr, i);
        }
        return out;
    }

    /** 主导出：world 目录+chunk 坐标 → JSON 字符串（present=false 也出合法 JSON）。
     *  b1.7.3 存储断代：散件 .dat 未命中 → McRegion 区域文件命中（只读自动分派，
     *  present 语义不变；下界传 DIM-1 目录作 worldDir 即自然命中 region/）。 */
    public static String export(File worldDir, int x, int z) throws IOException {
        File f = chunkFile(worldDir, x, z);
        StringBuilder sb = new StringBuilder();
        sb.append("{\"x\":").append(x).append(",\"z\":").append(z);
        Map<String, Object> root;
        if (f.isFile()) {
            byte[] gz = Files.readAllBytes(f.toPath());
            root = NbtReader.readGz(gz);
        } else {
            root = RegionStore.readChunk(worldDir, x, z);
            if (root == null) {
                sb.append(",\"present\":false}");
                return sb.toString();
            }
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> level = (Map<String, Object>) root.get("Level");
        if (level == null) {
            throw new IOException("chunk .dat without Level compound: " + f);
        }
        sb.append(",\"present\":true");
        sb.append(",\"xPos\":").append(num(level.get("xPos")));
        sb.append(",\"zPos\":").append(num(level.get("zPos")));
        sb.append(",\"lastUpdate\":").append(num(level.get("LastUpdate")));
        sb.append(",\"terrainGenerated\":").append(level.get("TerrainGenerated") == null
                ? "null" : (((Byte) level.get("TerrainGenerated")).byteValue() != 0 ? "true" : "false"));

        byte[] blocks = (byte[]) req(level, "Blocks");
        int[] ids = new int[BLOCKS_LEN];
        for (int i = 0; i < BLOCKS_LEN; i++) {
            ids[i] = blocks[i] & 0xFF;
        }
        sb.append(",\"blocks\":\"").append(rle(ids)).append('"');

        byte[] data = (byte[]) req(level, "Data");
        sb.append(",\"data\":\"").append(rle(expandNibble(data))).append('"');

        Object sky = level.get("SkyLight");
        Object bl = level.get("BlockLight");
        if (sky != null) {
            sb.append(",\"skyLight\":\"").append(rle(expandNibble((byte[]) sky))).append('"');
        }
        if (bl != null) {
            sb.append(",\"blockLight\":\"").append(rle(expandNibble((byte[]) bl))).append('"');
        }

        byte[] hm = (byte[]) req(level, "HeightMap");
        sb.append(",\"heightMap\":[");
        for (int i = 0; i < hm.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(hm[i] & 0xFF);
        }
        sb.append(']');

        sb.append(",\"tileEntities\":[");
        Object teList = level.get("TileEntities");
        if (teList instanceof List) {
            boolean first = true;
            for (Object o : (List<?>) teList) {
                @SuppressWarnings("unchecked")
                Map<String, Object> te = (Map<String, Object>) o;
                if (!first) {
                    sb.append(',');
                }
                first = false;
                appendTe(sb, te);
            }
        }
        sb.append("]}");
        return sb.toString();
    }

    /** TE：通用 id/x/y/z + 数值件直抄 + Items 槽位数组（getchest/getfurnace 活体面同构）。 */
    private static void appendTe(StringBuilder sb, Map<String, Object> te) {
        sb.append("{");
        boolean first = true;
        for (Map.Entry<String, Object> e : te.entrySet()) {
            String k = e.getKey();
            if ("Items".equals(k)) {
                continue;
            }
            if (!first) {
                sb.append(',');
            }
            first = false;
            sb.append('"').append(k).append("\":");
            Object v = e.getValue();
            if (v instanceof String) {
                sb.append('"').append(esc((String) v)).append('"');
            } else {
                sb.append(num(v));
            }
        }
        Object items = te.get("Items");
        if (items instanceof List) {
            if (!first) {
                sb.append(',');
            }
            sb.append("\"items\":[");
            boolean fi = true;
            for (Object o : (List<?>) items) {
                @SuppressWarnings("unchecked")
                Map<String, Object> it = (Map<String, Object>) o;
                if (!fi) {
                    sb.append(',');
                }
                fi = false;
                sb.append("{\"slot\":").append(num(it.get("Slot")))
                        .append(",\"id\":").append(num(it.get("id")))
                        .append(",\"count\":").append(num(it.get("Count")))
                        .append(",\"dmg\":").append(num(it.get("Damage")))
                        .append('}');
            }
            sb.append(']');
        }
        sb.append('}');
    }

    private static Object req(Map<String, Object> m, String k) throws IOException {
        Object v = m.get(k);
        if (v == null) {
            throw new IOException("chunk Level missing key: " + k);
        }
        return v;
    }

    private static long num(Object v) {
        if (v instanceof Number) {
            return ((Number) v).longValue();
        }
        throw new IllegalArgumentException("not a number: " + v);
    }

    private static String esc(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"' || c == '\\') {
                sb.append('\\').append(c);
            } else if (c < 0x20) {
                sb.append(String.format("\\u%04x", (int) c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 3) {
            System.err.println("usage: ChunkExport <worldDir> <x> <z> [out.json]");
            System.exit(2);
        }
        File worldDir = new File(args[0]);
        int x = Integer.parseInt(args[1]);
        int z = Integer.parseInt(args[2]);
        String json = export(worldDir, x, z);
        if (args.length >= 4) {
            Files.write(new File(args[3]).toPath(), json.getBytes(Charset.forName("UTF-8")));
            System.out.println("written " + args[3] + " (" + json.length() + " chars)");
        } else {
            System.out.println(json);
        }
    }
}
