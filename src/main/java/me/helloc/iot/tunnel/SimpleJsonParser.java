package me.helloc.iot.tunnel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Minimal JSON parser supporting objects, arrays, strings, numbers, booleans and null.
 * It converts JSON into Java types: Map for objects, List for arrays, String, Long/Double,
 * Boolean and null.
 */
class SimpleJsonParser {
    private final String json;
    private int index = 0;
    private final int length;

    SimpleJsonParser(String json) {
        this.json = json;
        this.length = json.length();
    }

    public Object parse() {
        skipWhitespace();
        Object value = parseValue();
        skipWhitespace();
        return value;
    }

    private Object parseValue() {
        skipWhitespace();
        if (index >= length) return null;
        switch (json.charAt(index)) {
            case '{':
                return parseObject();
            case '[':
                return parseArray();
            case '"':
                return parseString();
            case 't':
                consumeLiteral("true");
                return Boolean.TRUE;
            case 'f':
                consumeLiteral("false");
                return Boolean.FALSE;
            case 'n':
                consumeLiteral("null");
                return null;
            default:
                return parseNumber();
        }
    }

    private Map<String, Object> parseObject() {
        expect('{');
        skipWhitespace();
        Map<String, Object> map = new HashMap<>();
        if (peek() != null && peek() == '}') {
            index++;
            return map;
        }
        while (index < length) {
            skipWhitespace();
            String key = parseString();
            skipWhitespace();
            expect(':');
            skipWhitespace();
            Object value = parseValue();
            map.put(key, value);
            skipWhitespace();
            Character next = peek();
            if (next == null) break;
            switch (next) {
                case ',':
                    index++;
                    continue;
                case '}':
                    index++;
                    return map;
                default:
                    throw new IllegalStateException("Unexpected character at position " + index);
            }
        }
        throw new IllegalStateException("Unterminated object");
    }

    private List<Object> parseArray() {
        expect('[');
        skipWhitespace();
        List<Object> list = new ArrayList<>();
        if (peek() != null && peek() == ']') {
            index++;
            return list;
        }
        while (index < length) {
            Object value = parseValue();
            list.add(value);
            skipWhitespace();
            Character next = peek();
            if (next == null) break;
            switch (next) {
                case ',':
                    index++;
                    continue;
                case ']':
                    index++;
                    return list;
                default:
                    throw new IllegalStateException("Unexpected character at position " + index);
            }
        }
        throw new IllegalStateException("Unterminated array");
    }

    private String parseString() {
        expect('"');
        StringBuilder sb = new StringBuilder();
        while (index < length) {
            char ch = json.charAt(index++);
            switch (ch) {
                case '\\':
                    if (index >= length) throw new IllegalStateException("Incomplete escape sequence");
                    char esc = json.charAt(index++);
                    switch (esc) {
                        case '"': sb.append('"'); break;
                        case '\\': sb.append('\\'); break;
                        case '/': sb.append('/'); break;
                        case 'b': sb.append('\b'); break;
                        case 'f': sb.append('\f'); break;
                        case 'n': sb.append('\n'); break;
                        case 'r': sb.append('\r'); break;
                        case 't': sb.append('\t'); break;
                        case 'u':
                            String hex = json.substring(index, index + 4);
                            index += 4;
                            sb.append((char) Integer.parseInt(hex, 16));
                            break;
                        default:
                            sb.append(esc);
                    }
                    break;
                case '"':
                    return sb.toString();
                default:
                    sb.append(ch);
            }
        }
        throw new IllegalStateException("Unterminated string");
    }

    private Object parseNumber() {
        int start = index;
        if (json.charAt(index) == '-') index++;
        while (index < length && Character.isDigit(json.charAt(index))) index++;
        if (index < length && json.charAt(index) == '.') {
            index++;
            while (index < length && Character.isDigit(json.charAt(index))) index++;
        }
        if (index < length && (json.charAt(index) == 'e' || json.charAt(index) == 'E')) {
            index++;
            if (index < length && (json.charAt(index) == '+' || json.charAt(index) == '-')) index++;
            while (index < length && Character.isDigit(json.charAt(index))) index++;
        }
        String numberStr = json.substring(start, index);
        try {
            return Long.parseLong(numberStr);
        } catch (NumberFormatException e) {
            return Double.parseDouble(numberStr);
        }
    }

    private void skipWhitespace() {
        while (index < length && Character.isWhitespace(json.charAt(index))) index++;
    }

    private void expect(char ch) {
        if (index >= length || json.charAt(index) != ch) {
            throw new IllegalStateException("Expected '" + ch + "' at position " + index);
        }
        index++;
    }

    private void consumeLiteral(String lit) {
        for (int i = 0; i < lit.length(); i++) {
            if (index >= length || json.charAt(index) != lit.charAt(i)) {
                throw new IllegalStateException("Expected literal " + lit + " at position " + index);
            }
            index++;
        }
    }

    private Character peek() {
        return index < length ? json.charAt(index) : null;
    }
}
