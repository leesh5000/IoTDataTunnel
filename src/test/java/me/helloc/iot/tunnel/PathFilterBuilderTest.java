package me.helloc.iot.tunnel;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PathFilterBuilderTest {
    private final String message = "{" +
            "\"id\":1," +
            "\"gateways\":[{\"id\":1},{\"id\":2}]," +
            "\"companyCode\":\"0012\"," +
            "\"sensor\":[{" +
            "\"type\":\"temp\",\"value\":39}," +
            "{\"type\":\"airFlow\",\"value\":12}," +
            "{\"type\":\"humidity\",\"value\":57}]" +
            "}";

    @Test
    void extractsValueWhenFiltersMatch() {
        Integer value = PathFilterBuilder.from(message)
                .addPathFilter("$.id", 1)
                .addPathFilter("$.gateways[0].id", 1)
                .addPathFilter("$.companyCode", "0012")
                .addValueFilter("$.sensor[0].value")
                .extractFirst(Integer.class);
        assertEquals(39, value);
    }

    @Test
    void returnsNullWhenPathFilterDoesNotMatch() {
        Integer value = PathFilterBuilder.from(message)
                .addPathFilter("$.id", 2)
                .addValueFilter("$.sensor[0].value")
                .extractFirst(Integer.class);
        assertNull(value);
    }

    @Test
    void extractsValueFromArrayRoot() {
        String arrayMsg = "[{" + "\"id\":1,\"value\":10},{\"id\":2,\"value\":20}]";
        Integer result = PathFilterBuilder.from(arrayMsg)
                .addPathFilter("$[0].id", 1)
                .addValueFilter("$[1].value")
                .extractFirst(Integer.class);
        assertEquals(20, result);
    }

    @Test
    void extractsNestedArrayValueWhenTypeMatches() {
        Integer nested = PathFilterBuilder.from(message)
                .addPathFilter("$.sensor[1].type", "airFlow")
                .addValueFilter("$.sensor[1].value")
                .extractFirst(Integer.class);
        assertEquals(12, nested);
    }

    @Test
    void extractsValueFromSingleObject() {
        String objMsg = "{\"active\":true,\"count\":5}";
        Boolean result = PathFilterBuilder.from(objMsg)
                .addPathFilter("$.count", 5)
                .addValueFilter("$.active")
                .extractFirst(Boolean.class);
        assertTrue(result);
    }

    @Test
    void matchesNumericPathFiltersAcrossTypes() {
        Double result = PathFilterBuilder.from(message)
                .addPathFilter("$.id", 1L)
                .addValueFilter("$.sensor[0].value")
                .extractFirst(Double.class);
        assertEquals(39.0, result);
    }

    @Test
    void convertsExtractedNumberToTargetType() {
        Long result = PathFilterBuilder.from(message)
                .addValueFilter("$.sensor[0].value")
                .extractFirst(Long.class);
        assertEquals(39L, result);
    }
}
