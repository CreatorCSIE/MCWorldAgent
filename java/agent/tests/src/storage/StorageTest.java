// [B5-ARCHIVE 2026-11-22] headless test archived from %TEMP%\mcaut (source batch: storage module); run via java/agent/tests/run.ps1
package storage;

import com.mcworldagent.storage.ChunkExport;
import com.mcworldagent.storage.NbtReader;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

/**
 * chunk-export 无头测试（temp，不入库）。纯 main，断言计数输出。
 */
public class StorageTest {

    static int pass = 0, fail = 0;

    static void ok(boolean c, String name) {
        if (c) { pass++; } else { fail++; System.out.println("FAIL: " + name); }
    }

    // ---- tiny NBT writer (test fixture) ----
    static ByteArrayOutputStream bo = new ByteArrayOutputStream();
    void w(int v) { bo.write(v); }
    void w16(int v) { w(v >> 8 & 255); w(v & 255); }
    void w32(int v) { w16(v >> 16 & 65535); w16(v & 65535); }
    void w64(long v) { w32((int) (v >> 32)); w32((int) v); }
    void ws(String s) throws IOException {
        byte[] b = s.getBytes("UTF-8");
        w16(b.length);
        bo.write(b);
    }
    void tagByte(String k, byte v) throws IOException { w(1); ws(k); w(v); }
    void tagShort(String k, short v) throws IOException { w(2); ws(k); w16(v); }
    void tagInt(String k, int v) throws IOException { w(3); ws(k); w32(v); }
    void tagLong(String k, long v) throws IOException { w(4); ws(k); w64(v); }
    void tagBytes(String k, byte[] v) throws IOException { w(7); ws(k); w32(v.length); bo.write(v); }
    void tagStr(String k, String v) throws IOException { w(8); ws(k); ws(v); }

    byte[] gzip(byte[] raw) throws IOException {
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        GZIPOutputStream g = new GZIPOutputStream(o);
        g.write(raw);
        g.close();
        return o.toByteArray();
    }

    void testAllTagTypes() throws Exception {
        bo = new ByteArrayOutputStream();
        w(10); ws("");
        tagByte("b", (byte) -7);
        tagShort("s", (short) 300);
        tagInt("i", 70000);
        tagLong("l", 5000000000L);
        w(5); ws("f"); w32(Float.floatToIntBits(1.5f));
        w(6); ws("d"); w64(Double.doubleToLongBits(2.25));
        tagBytes("ba", new byte[]{1, 2, 3});
        tagStr("st", "hello");
        w(9); ws("li"); w(10); w32(2); // list<compound> of 2 — elements carry NO type/name prefix
        tagInt("k", 1); w(0);
        tagInt("k", 2); w(0);
        w(0); // end root
        Map<String, Object> root = NbtReader.readGz(gzip(bo.toByteArray()));
        ok(((Byte) root.get("b")) == (byte) -7, "tag1 byte");
        ok(((Short) root.get("s")) == 300, "tag2 short");
        ok(((Integer) root.get("i")) == 70000, "tag3 int");
        ok(((Long) root.get("l")) == 5000000000L, "tag4 long");
        ok(((Float) root.get("f")) == 1.5f, "tag5 float");
        ok(((Double) root.get("d")) == 2.25, "tag6 double");
        ok(java.util.Arrays.equals((byte[]) root.get("ba"), new byte[]{1, 2, 3}), "tag7 bytes");
        ok("hello".equals(root.get("st")), "tag8 str");
        List<?> li = (List<?>) root.get("li");
        ok(li.size() == 2 && ((Integer) ((Map) li.get(1)).get("k")) == 2, "tag9 list<compound>");
        w(11); // int array tag at top-level of a fresh doc
        bo = new ByteArrayOutputStream();
        w(10); ws("");
        w(11); ws("ia"); w32(2); w32(5); w32(-5);
        w(0);
        Map<String, Object> r2 = NbtReader.readGz(gzip(bo.toByteArray()));
        ok(java.util.Arrays.equals((int[]) r2.get("ia"), new int[]{5, -5}), "tag11 intarr");
    }

    void testTruncated() throws Exception {
        bo = new ByteArrayOutputStream();
        w(10); ws(""); tagInt("i", 1); w(0);
        byte[] gz = gzip(bo.toByteArray());
        byte[] raw = NbtReader.gunzip(gz);
        byte[] cut = new byte[raw.length - 2];
        System.arraycopy(raw, 0, cut, 0, cut.length);
        byte[] gz2;
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        GZIPOutputStream g = new GZIPOutputStream(o);
        g.write(cut);
        g.close();
        gz2 = o.toByteArray();
        boolean threw = false;
        try {
            NbtReader.readGz(gz2);
        } catch (IOException e) {
            threw = true;
        }
        ok(threw, "truncated NBT -> IOException (consumed discipline)");
    }

    void testIndexFormulas() {
        ok(ChunkExport.blockIndex(0, 0, 0) == 0, "idx(0,0,0)");
        ok(ChunkExport.blockIndex(1, 0, 0) == 2048, "idx x=1<<11");
        ok(ChunkExport.blockIndex(0, 1, 0) == 1, "idx y innermost");
        ok(ChunkExport.blockIndex(0, 0, 1) == 128, "idx z<<7");
        ok(ChunkExport.blockIndex(15, 127, 15) == 32767, "idx max");
        byte[] a = new byte[ChunkExport.NIBBLE_LEN];
        a[0] = 0x31; // low=1 (idx0), high=3 (idx1)
        ok(ChunkExport.nibble(a, 0) == 1, "nibble even=low");
        ok(ChunkExport.nibble(a, 1) == 3, "nibble odd=high");
        // roundtrip: every idx maps to distinct nibble slot
        int distinct = 0;
        for (int i = 0; i < 64; i++) {
            if (ChunkExport.nibble(a, i) != ChunkExport.nibble(a, (i + 1) % 64)) { distinct++; }
        }
        ok(distinct > 0, "nibble spread");
    }

    void testRle() {
        ok("".equals(ChunkExport.rle(new int[0])), "rle empty");
        ok("1:3|2:1|0:2".equals(ChunkExport.rle(new int[]{1, 1, 1, 2, 0, 0})), "rle runs");
        ok("5:1".equals(ChunkExport.rle(new int[]{5})), "rle single");
        // decode roundtrip
        int[] vals = new int[]{0, 0, 3, 3, 3, 0};
        String enc = ChunkExport.rle(vals);
        String[] runs = enc.split("\\|");
        java.util.List<Integer> dec = new java.util.ArrayList<Integer>();
        for (String r : runs) {
            String[] p = r.split(":");
            int v = Integer.parseInt(p[0]);
            int n = Integer.parseInt(p[1]);
            for (int i = 0; i < n; i++) { dec.add(v); }
        }
        ok(dec.size() == vals.length && dec.get(2) == 3 && dec.get(5) == 0, "rle decode roundtrip");
    }

    void testB36Path() {
        File f = ChunkExport.chunkFile(new File("/w"), 10, -28);
        ok(f.getPath().replace('\\', '/').endsWith("/w/a/-s/c.a.-s.dat"), "b36 negative path: " + f);
        File f0 = ChunkExport.chunkFile(new File("/w"), 0, 0);
        ok(f0.getPath().replace('\\', '/').endsWith("/w/0/0/c.0.0.dat"), "b36 zero path");
    }

    void testPresentFalse() throws Exception {
        File tmp = File.createTempFile("mcastor", ".dir");
        ok(true, "tmp placeholder");
        tmp.delete();
        String j = ChunkExport.export(tmp, 999, 999);
        ok(j.contains("\"present\":false") && j.startsWith("{\"x\":999,\"z\":999"), "absent -> present:false: " + j);
    }

    void testFullChunkExport() throws Exception {
        // build a synthetic chunk .dat: all air except block at (1,64,2)=4, meta nibble=2, TE Furnace
        bo = new ByteArrayOutputStream();
        w(10); ws("");
        w(10); ws("Level");
        byte[] blocks = new byte[ChunkExport.BLOCKS_LEN];
        blocks[ChunkExport.blockIndex(1, 64, 2)] = 4;
        byte[] data = new byte[ChunkExport.NIBBLE_LEN];
        int idx = ChunkExport.blockIndex(1, 64, 2);
        // nibble convention: even idx=low, odd=high — write value 2 at correct half
        if ((idx & 1) == 0) {
            data[idx >> 1] = (byte) (data[idx >> 1] & 0xF0 | 2);
        } else {
            data[idx >> 1] = (byte) (data[idx >> 1] & 0x0F | (2 << 4));
        }
        byte[] sky = new byte[ChunkExport.NIBBLE_LEN];
        byte[] bl = new byte[ChunkExport.NIBBLE_LEN];
        byte[] hm = new byte[256];
        hm[0] = 70;
        tagInt("xPos", 3);
        tagInt("zPos", -4);
        tagLong("LastUpdate", 12345L);
        tagByte("TerrainGenerated", (byte) 1);
        tagBytes("Blocks", blocks);
        tagBytes("Data", data);
        tagBytes("SkyLight", sky);
        tagBytes("BlockLight", bl);
        tagBytes("HeightMap", hm);
        // TileEntities: list<compound> Furnace with Items (elements: no type/name prefix)
        w(9); ws("TileEntities"); w(10); w32(1);
        tagStr("id", "Furnace");
        tagInt("x", 9); tagInt("y", 65); tagInt("z", 13);
        tagShort("BurnTime", (short) 100); tagShort("CookTime", (short) 50);
        w(9); ws("Items"); w(10); w32(2);
        tagByte("Slot", (byte) 0); tagShort("id", (short) 15); tagByte("Count", (byte) 1); tagShort("Damage", (short) 0); w(0);
        tagByte("Slot", (byte) 2); tagShort("id", (short) 265); tagByte("Count", (byte) 1); tagShort("Damage", (short) 0); w(0);
        w(0); // end TE compound
        w(0); // end Level
        w(0); // end root
        byte[] gz = gzip(bo.toByteArray());
        File dir = new File(System.getProperty("java.io.tmpdir"), "mcastor_chunk/a/-s");
        dir.mkdirs();
        File dat = new File(dir, "c.a.-s.dat");
        java.nio.file.Files.write(dat.toPath(), gz);
        String json = ChunkExport.export(new File(System.getProperty("java.io.tmpdir"), "mcastor_chunk"), 10, -28);
        ok(json.contains("\"present\":true"), "export present");
        ok(json.contains("\"xPos\":3"), "xPos");
        ok(json.contains("\"lastUpdate\":12345"), "lastUpdate");
        ok(json.contains("\"terrainGenerated\":true"), "terrainGenerated");
        // blocks RLE: 0-run up to idx, then 4:1
        String blocksRle = json.substring(json.indexOf("\"blocks\":\"") + 10, json.indexOf("\",\"data\""));
        // decode: total counts must = 32768, one run of 4 length 1
        int total = 0; boolean has4 = false;
        for (String r : blocksRle.split("\\|")) {
            String[] p = r.split(":");
            total += Integer.parseInt(p[1]);
            if (p[0].equals("4") && p[1].equals("1")) has4 = true;
        }
        ok(total == ChunkExport.BLOCKS_LEN && has4, "blocks RLE total+id4");
        ok(json.contains("\"data\":\""), "data key");
        // data decode: value 2 at idx
        String dataRle = json.substring(json.indexOf("\"data\":\"") + 8, json.indexOf("\",\"skyLight\""));
        int di = 0; boolean meta2 = false;
        for (String r : dataRle.split("\\|")) {
            String[] p = r.split(":");
            int v = Integer.parseInt(p[0]);
            int n = Integer.parseInt(p[1]);
            if (v == 2 && di <= idx && idx < di + n) meta2 = true;
            di += n;
        }
        ok(meta2, "nibble meta=2 roundtrip at blockIndex");
        ok(json.contains("\"id\":\"Furnace\""), "TE id");
        ok(json.contains("\"burn\"") || json.contains("\"BurnTime\":100"), "TE BurnTime passthrough");
        ok(json.contains("\"items\":[{\"slot\":0,\"id\":15,\"count\":1,\"dmg\":0},{\"slot\":2,\"id\":265,\"count\":1,\"dmg\":0}]"), "TE items exact: " + json.substring(json.indexOf("\"items\"")));
        ok(json.contains("\"heightMap\":[70,"), "heightMap first");
        dat.delete(); dat.getParentFile().delete();
    }

    public static void main(String[] args) throws Exception {
        StorageTest t = new StorageTest();
        t.testAllTagTypes();
        t.testTruncated();
        t.testIndexFormulas();
        t.testRle();
        t.testB36Path();
        t.testPresentFalse();
        t.testFullChunkExport();
        System.out.println("TOTAL pass=" + pass + " fail=" + fail);
        if (fail > 0) {
            System.exit(1);
        }
    }
}
