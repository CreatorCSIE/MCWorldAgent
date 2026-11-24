package com.mcworldagent.storage;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

/**
 * D7 NBT 写入器（生产级，纯 JDK8 零依赖，与 NbtReader 树模型对称）。
 * 输入树：Byte/Short/Integer/Long/Float/Double/byte[]/String/ArrayList/Map/int[]
 * （即 NbtReader 读出的同一棵树，可直接回写）。
 * 验收纪律：产出必须能被 NbtReader.readRaw 全消费回读——往返即测试。
 * 响亮纪律：null 值 / 未知类型 / 异型 list = IOException，绝不静默丢字段。
 * 空 list = 元素类型 0 + 长度 0（对齐原版 NBTCompressedStreamTools 行为）。
 * Map 键序 = 迭代序（LinkedHashMap 保插入序，Reader 产出的正是它）。
 */
public final class NbtWriter {

    private final ByteArrayOutputStream bo = new ByteArrayOutputStream();

    private NbtWriter() { }

    /** root compound → gzip .dat 字节（实盘 level.dat / chunk .dat 均此形态）。 */
    public static byte[] writeGz(String rootName, Map<String, Object> root) throws IOException {
        byte[] raw = writeRaw(rootName, root);
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        GZIPOutputStream g = new GZIPOutputStream(o);
        g.write(raw);
        g.close();
        return o.toByteArray();
    }

    /** root compound → 未压缩 NBT 字节（TAG_Compound + name + body[含 END]）。 */
    public static byte[] writeRaw(String rootName, Map<String, Object> root) throws IOException {
        NbtWriter w = new NbtWriter();
        w.u8(10);
        w.str(rootName);
        w.compound(root);
        return w.bo.toByteArray();
    }

    /** Object → tag id；未知类型响亮拒绝。 */
    static int tagOf(Object v) throws IOException {
        if (v instanceof Byte) return 1;
        if (v instanceof Short) return 2;
        if (v instanceof Integer) return 3;
        if (v instanceof Long) return 4;
        if (v instanceof Float) return 5;
        if (v instanceof Double) return 6;
        if (v instanceof byte[]) return 7;
        if (v instanceof String) return 8;
        if (v instanceof List) return 9;
        if (v instanceof Map) return 10;
        if (v instanceof int[]) return 11;
        throw new IOException("unsupported NBT value type: " + v.getClass().getName());
    }

    private void u8(int v) { bo.write(v & 255); }

    private void u16(int v) { u8(v >> 8); u8(v); }

    private void i32(int v) { u16(v >> 16); u16(v); }

    private void i64(long v) { i32((int) (v >> 32)); i32((int) v); }

    private void str(String s) throws IOException {
        byte[] b = s.getBytes("UTF-8");
        u16(b.length);
        bo.write(b, 0, b.length);
    }

    @SuppressWarnings("unchecked")
    private void compound(Map<String, Object> m) throws IOException {
        for (Map.Entry<String, Object> e : m.entrySet()) {
            Object v = e.getValue();
            if (v == null) {
                throw new IOException("null value at key: " + e.getKey());
            }
            u8(tagOf(v));
            str(e.getKey());
            value(v);
        }
        u8(0); // TAG_End
    }

    private void list(List<Object> list) throws IOException {
        int et = 0;
        if (!list.isEmpty()) {
            Object first = list.get(0);
            if (first == null) {
                throw new IOException("null element in list");
            }
            et = tagOf(first);
        }
        u8(et);
        i32(list.size());
        for (int i = 0; i < list.size(); i++) {
            Object v = list.get(i);
            if (v == null) {
                throw new IOException("null element in list at index " + i);
            }
            int t = tagOf(v);
            if (t != et) {
                throw new IOException("heterogeneous list: element " + i + " tag=" + t + " != " + et);
            }
            value(v);
        }
    }

    private void value(Object v) throws IOException {
        if (v instanceof Byte) {
            u8(((Byte) v).intValue());
        } else if (v instanceof Short) {
            u16(((Short) v).intValue() & 65535);
        } else if (v instanceof Integer) {
            i32(((Integer) v).intValue());
        } else if (v instanceof Long) {
            i64(((Long) v).longValue());
        } else if (v instanceof Float) {
            i32(Float.floatToIntBits(((Float) v).floatValue()));
        } else if (v instanceof Double) {
            i64(Double.doubleToLongBits(((Double) v).doubleValue()));
        } else if (v instanceof byte[]) {
            byte[] b = (byte[]) v;
            i32(b.length);
            bo.write(b, 0, b.length);
        } else if (v instanceof String) {
            str((String) v);
        } else if (v instanceof List) {
            list((List<Object>) v);
        } else if (v instanceof Map) {
            compound((Map<String, Object>) v);
        } else if (v instanceof int[]) {
            int[] a = (int[]) v;
            i32(a.length);
            for (int x : a) {
                i32(x);
            }
        } else {
            throw new IOException("unsupported NBT value type: " + v.getClass().getName());
        }
    }
}
