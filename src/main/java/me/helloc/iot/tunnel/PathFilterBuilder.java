package me.helloc.iot.tunnel;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Filters JSON messages using simple path expressions and extracts values when
 * all path filters match. Does not rely on external JSON libraries.
 */
public final class PathFilterBuilder {
    private final String message;
    private final List<Map.Entry<String, Object>> pathFilters = new ArrayList<>();
    private final List<String> valueFilters = new ArrayList<>();

    private PathFilterBuilder(String message) {
        this.message = message;
    }

    public PathFilterBuilder addPathFilter(String path, Object expectedValue) {
        pathFilters.add(new AbstractMap.SimpleEntry<>(path, expectedValue));
        return this;
    }

    public PathFilterBuilder addValueFilter(String path) {
        valueFilters.add(path);
        return this;
    }

    public <T> T extractFirst(Class<T> clazz) {
        Object root;
        try {
            root = new SimpleJsonParser(message).parse();
        } catch (Exception e) {
            return null;
        }

        for (Map.Entry<String, Object> entry : pathFilters) {
            Object actual = evaluatePath(root, entry.getKey());
            if (actual == null) {
                return null;
            }
            Object expected = entry.getValue();
            if (actual instanceof Number && expected instanceof Number) {
                if (((Number) actual).doubleValue() != ((Number) expected).doubleValue()) {
                    return null;
                }
            } else if (!actual.equals(expected)) {
                return null;
            }
        }

        for (String path : valueFilters) {
            Object raw = evaluatePath(root, path);
            if (raw != null) {
                Class<?> targetClass = wrapperFor(clazz);
                if (targetClass.isInstance(raw)) {
                    @SuppressWarnings("unchecked")
                    T result = (T) targetClass.cast(raw);
                    return result;
                } else if (raw instanceof Number) {
                    Object converted = convertNumber((Number) raw, targetClass);
                    if (converted != null && targetClass.isInstance(converted)) {
                        @SuppressWarnings("unchecked")
                        T result = (T) converted;
                        return result;
                    }
                }
            }
        }
        return null;
    }

    private Object evaluatePath(Object root, String path) {
        List<PathToken> tokens = parseTokens(path);
        Object current = root;
        for (PathToken token : tokens) {
            if (token instanceof PathToken.Key) {
                if (current instanceof Map) {
                    current = ((Map<?, ?>) current).get(((PathToken.Key) token).name);
                } else {
                    return null;
                }
            } else if (token instanceof PathToken.Index) {
                if (current instanceof List) {
                    List<?> list = (List<?>) current;
                    int index = ((PathToken.Index) token).index;
                    if (index < 0 || index >= list.size()) return null;
                    current = list.get(index);
                } else {
                    return null;
                }
            }
            if (current == null) return null;
        }
        return current;
    }

    private interface PathToken {
        class Key implements PathToken {
            final String name;
            Key(String name) { this.name = name; }
        }
        class Index implements PathToken {
            final int index;
            Index(int index) { this.index = index; }
        }
    }

    private List<PathToken> parseTokens(String path) {
        if (!path.startsWith("$")) {
            throw new IllegalArgumentException("Path must start with $");
        }
        List<PathToken> tokens = new ArrayList<>();
        int i = 1;
        while (i < path.length()) {
            char c = path.charAt(i);
            if (c == '.') {
                i++;
                int start = i;
                while (i < path.length() && path.charAt(i) != '.' && path.charAt(i) != '[') i++;
                String key = path.substring(start, i);
                if (!key.isEmpty()) tokens.add(new PathToken.Key(key));
            } else if (c == '[') {
                i++;
                int start = i;
                while (i < path.length() && path.charAt(i) != ']') i++;
                int idx;
                try {
                    idx = Integer.parseInt(path.substring(start, i));
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Invalid numeric index in path: '" + path + "'");
                }
                tokens.add(new PathToken.Index(idx));
                if (i < path.length() && path.charAt(i) == ']') i++;
            } else {
                i++;
            }
        }
        return tokens;
    }

    private static Class<?> wrapperFor(Class<?> clazz) {
        if (!clazz.isPrimitive()) return clazz;
        if (clazz == boolean.class) return Boolean.class;
        if (clazz == byte.class) return Byte.class;
        if (clazz == char.class) return Character.class;
        if (clazz == short.class) return Short.class;
        if (clazz == int.class) return Integer.class;
        if (clazz == long.class) return Long.class;
        if (clazz == float.class) return Float.class;
        if (clazz == double.class) return Double.class;
        return clazz;
    }

    private static Object convertNumber(Number raw, Class<?> target) {
        if (target == Byte.class) return raw.byteValue();
        if (target == Short.class) return raw.shortValue();
        if (target == Integer.class) return raw.intValue();
        if (target == Long.class) return raw.longValue();
        if (target == Float.class) return raw.floatValue();
        if (target == Double.class) return raw.doubleValue();
        return null;
    }

    public static PathFilterBuilder from(String message) {
        return new PathFilterBuilder(message);
    }
}
