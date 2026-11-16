package com.mcworldagent.storage;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/**
 * 0327 NBT 只读解析器（chunk-export 批，纯 JDK8 零依赖）。
 * 输出通用树：Compound=LinkedHashMap<String,Object>、List=ArrayList<Object>、
 * Byte/Short/Integer/Long/Float/Double、ByteArray=byte[]、String=String、IntArray=int[]。
 * 完整性纪律（audit nbtskel 同款）：解析必须消费解压后全部字节，
 * 不符=IOException——解析错位=结构假设失效，绝不静默出半棵树。
 */
public final class NbtReader {

    private final DataInputStream in;
    private final int total;
    private int consumed;

    private NbtReader(byte[] uncompressed) {
        this.in = new DataInputStream(new ByteArrayInputStream(uncompressed));
        this.total = uncompressed.length;
    }

    /** gzip .dat 字节 → root compound（root 名不消费出树；全消费校验内置）。 */
    public static Map<String, Object> readGz(byte[] gzipped) throws IOException {
        return readRaw(gunzip(gzipped));
    }

    /** 未压缩 NBT 字节 → root compound（McRegion 版本=2 zlib 解后形态，b1.7.3 批）。 */
    public static Map<String, Object> readRaw(byte[] uncompressed) throws IOException {
        NbtReader r = new NbtReader(uncompressed);
        int t = r.u8();
        r.str(); // root name（0327 chunk .dat 实盘=""）
        if (t != 10) {
            throw new IOException("root not compound: tag=" + t);
        }
        Object root = r.value(10);
        if (r.consumed != r.total) {
            throw new IOException("NBT not fully consumed: " + r.consumed + "/" + r.total);
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> m = (Map<String, Object>) root;
        return m;
    }

    public static byte[] gunzip(byte[] gzipped) throws IOException {
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        GZIPInputStream g = new GZIPInputStream(new ByteArrayInputStream(gzipped));
        byte[] b = new byte[8192];
        int n;
        while ((n = g.read(b)) > 0) {
            bo.write(b, 0, n);
        }
        g.close();
        return bo.toByteArray();
    }

    private int u8() throws IOException {
        int v = in.read();
        if (v < 0) {
            throw new IOException("EOF at " + consumed);
        }
        consumed++;
        return v;
    }

    private int u16() throws IOException {
        int v = in.readUnsignedShort();
        consumed += 2;
        return v;
    }

    private int i32() throws IOException {
        int v = in.readInt();
        consumed += 4;
        return v;
    }

    private long i64() throws IOException {
        long v = in.readLong();
        consumed += 8;
        return v;
    }

    private String str() throws IOException {
        int n = u16();
        byte[] b = new byte[n];
        in.readFully(b);
        consumed += n;
        return new String(b, "UTF-8");
    }

    private Object value(int type) throws IOException {
        switch (type) {
            case 1: return (byte) u8();
            case 2: { int v = in.readShort(); consumed += 2; return (short) v; }
            case 3: return i32();
            case 4: return i64();
            case 5: { float v = in.readFloat(); consumed += 4; return v; }
            case 6: { double v = in.readDouble(); consumed += 8; return v; }
            case 7: {
                int n = i32();
                byte[] b = new byte[n];
                in.readFully(b);
                consumed += n;
                return b;
            }
            case 8: return str();
            case 9: {
                int et = u8();
                int n = i32();
                ArrayList<Object> list = new ArrayList<Object>(n);
                for (int i = 0; i < n; i++) {
                    list.add(value(et));
                }
                return list;
            }
            case 10: {
                LinkedHashMap<String, Object> m = new LinkedHashMap<String, Object>();
                for (;;) {
                    int t = u8();
                    if (t == 0) {
                        break;
                    }
                    String k = str();
                    m.put(k, value(t));
                }
                return m;
            }
            case 11: {
                int n = i32();
                int[] a = new int[n];
                for (int i = 0; i < n; i++) {
                    a[i] = i32();
                }
                return a;
            }
            default:
                throw new IOException("bad tag type: " + type);
        }
    }
}
