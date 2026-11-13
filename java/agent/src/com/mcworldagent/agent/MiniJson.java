package com.mcworldagent.agent;

import java.util.HashMap;
import java.util.Map;

/**
 * 严格子集 JSON 解析器（案卷 agent-command-channel §8-1）。
 * 协议形态固定：顶层对象 + 至多一层嵌套（args），值为 String / 整数 Long / Boolean。
 * 拒绝：数组、null 字面量、嵌套深度>1、重复键、浮点/科学计数、尾随内容、非法转义、超长字符串。
 * 解析失败一律抛 IllegalArgumentException（调用方必须落 REJECTED，不得静默）。
 */
public final class MiniJson {

    private static final int MAX_STRING = 256;

    private final String s;
    private int i;

    private MiniJson(String s) {
        this.s = s;
    }

    /** 解析顶层对象。任何不符合协议子集的输入抛 IllegalArgumentException。 */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> parse(String text) {
        if (text == null || text.length() > 4096) {
            throw new IllegalArgumentException("size");
        }
        MiniJson p = new MiniJson(text);
        p.ws();
        Object v = p.value(0);
        if (!(v instanceof Map)) {
            throw new IllegalArgumentException("top must be object");
        }
        p.ws();
        if (p.i != p.s.length()) {
            throw new IllegalArgumentException("trailing");
        }
        return (Map<String, Object>) v;
    }

    private void ws() {
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c == ' ' || c == '\t' || c == '\n' || c == '\r') {
                i++;
            } else {
                return;
            }
        }
    }

    private Object value(int depth) {
        if (i >= s.length()) {
            throw new IllegalArgumentException("eof");
        }
        char c = s.charAt(i);
        if (c == '"') {
            return string();
        }
        if (c == '{') {
            return object(depth + 1);
        }
        if (c == '-' || (c >= '0' && c <= '9')) {
            return number();
        }
        if (c == 't') {
            literal("true");
            return Boolean.TRUE;
        }
        if (c == 'f') {
            literal("false");
            return Boolean.FALSE;
        }
        if (c == 'n') {
            throw new IllegalArgumentException("null not allowed");
        }
        if (c == '[') {
            throw new IllegalArgumentException("arrays not allowed");
        }
        throw new IllegalArgumentException("unexpected '" + c + "' @" + i);
    }

    private Map<String, Object> object(int depth) {
        if (depth > 2) { // 顶层=1，args=2；三层起拒绝
            throw new IllegalArgumentException("nested too deep");
        }
        Map<String, Object> m = new HashMap<String, Object>();
        i++; // consume '{'
        ws();
        if (i < s.length() && s.charAt(i) == '}') {
            i++;
            return m;
        }
        while (true) {
            ws();
            if (i >= s.length() || s.charAt(i) != '"') {
                throw new IllegalArgumentException("key expected @" + i);
            }
            String key = string();
            ws();
            if (i >= s.length() || s.charAt(i) != ':') {
                throw new IllegalArgumentException("':' expected @" + i);
            }
            i++;
            ws();
            if (m.containsKey(key)) {
                throw new IllegalArgumentException("duplicate key '" + key + "'");
            }
            m.put(key, value(depth));
            ws();
            if (i >= s.length()) {
                throw new IllegalArgumentException("eof in object");
            }
            char c = s.charAt(i);
            if (c == ',') {
                i++;
                continue;
            }
            if (c == '}') {
                i++;
                return m;
            }
            throw new IllegalArgumentException("',' or '}' expected @" + i);
        }
    }

    private String string() {
        i++; // consume '"'
        StringBuilder b = new StringBuilder();
        while (true) {
            if (i >= s.length()) {
                throw new IllegalArgumentException("unterminated string");
            }
            char c = s.charAt(i++);
            if (c == '"') {
                if (b.length() > MAX_STRING) {
                    throw new IllegalArgumentException("string too long");
                }
                return b.toString();
            }
            if (c == '\\') {
                if (i >= s.length()) {
                    throw new IllegalArgumentException("bad escape");
                }
                char e = s.charAt(i++);
                switch (e) {
                    case '"': b.append('"'); break;
                    case '\\': b.append('\\'); break;
                    case '/': b.append('/'); break;
                    case 'b': b.append('\b'); break;
                    case 'f': b.append('\f'); break;
                    case 'n': b.append('\n'); break;
                    case 'r': b.append('\r'); break;
                    case 't': b.append('\t'); break;
                    case 'u':
                        if (i + 4 > s.length()) {
                            throw new IllegalArgumentException("bad \\u");
                        }
                        String hex = s.substring(i, i + 4);
                        try {
                            b.append((char) Integer.parseInt(hex, 16));
                        } catch (NumberFormatException nfe) {
                            throw new IllegalArgumentException("bad \\u" + hex);
                        }
                        i += 4;
                        break;
                    default:
                        throw new IllegalArgumentException("bad escape '\\" + e + "'");
                }
            } else if (c < 0x20) {
                throw new IllegalArgumentException("raw control char");
            } else {
                b.append(c);
            }
        }
    }

    private Long number() {
        int start = i;
        if (s.charAt(i) == '-') {
            i++;
        }
        int digits = 0;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c >= '0' && c <= '9') {
                digits++;
                i++;
            } else if (c == '.' || c == 'e' || c == 'E') {
                throw new IllegalArgumentException("float not allowed");
            } else {
                break;
            }
        }
        if (digits == 0) {
            throw new IllegalArgumentException("bad number @" + start);
        }
        try {
            return Long.valueOf(s.substring(start, i));
        } catch (NumberFormatException nfe) {
            throw new IllegalArgumentException("number overflow");
        }
    }

    private void literal(String lit) {
        if (!s.startsWith(lit, i)) {
            throw new IllegalArgumentException("literal '" + lit + "' expected @" + i);
        }
        i += lit.length();
    }
}
