package me.helloc.iot.tunnel;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SimpleJsonParserTest {
    @Test
    void parsesComplexStructures() {
        String json = "{\"a\":1,\"b\":[2,3],\"c\":{\"d\":4},\"e\":true,\"f\":\"str\"}";
        Map<?,?> result = (Map<?,?>) new SimpleJsonParser(json).parse();
        assertEquals(1L, result.get("a"));
        assertEquals(3L, ((List<?>) result.get("b")).get(1));
        assertEquals(4L, ((Map<?,?>) result.get("c")).get("d"));
        assertEquals(true, result.get("e"));
        assertEquals("str", result.get("f"));
    }
}
