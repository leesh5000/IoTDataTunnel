package me.helloc.iot.tunnel

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class PathFilterBuilderTest : StringSpec({
    val message = """
        {
          "id":1,
          "gateways":[{"id":1},{"id":2}],
          "companyCode":"0012",
          "sensor":[
            {"type":"temp","value":39},
            {"type":"airFlow","value":12},
            {"type":"humidity","value":57}
          ]
        }
    """

    "extracts value when filters match" {
        val value = PathFilterBuilder.from(message)
            .addPathFilter("$.id", 1)
            .addPathFilter("$.gateways[0].id", 1)
            .addPathFilter("$.companyCode", "0012")
            .addValueFilter("$.sensor[0].value")
            .extractFirst(Int::class.java)

        value shouldBe 39
    }

    "returns null when path filter does not match" {
        val value = PathFilterBuilder.from(message)
            .addPathFilter("$.id", 2)
            .addValueFilter("$.sensor[0].value")
            .extractFirst(Int::class.java)

        value shouldBe null
    }

    "extracts value from array root" {
        val arrayMsg = """
            [
              {"id":1,"value":10},
              {"id":2,"value":20}
            ]
        """

        val result = PathFilterBuilder.from(arrayMsg)
            .addPathFilter("$[0].id", 1)
            .addValueFilter("$[1].value")
            .extractFirst(Int::class.java)

        result shouldBe 20
    }

    "extracts nested array value when type matches" {
        val nested = PathFilterBuilder.from(message)
            .addPathFilter("$.sensor[1].type", "airFlow")
            .addValueFilter("$.sensor[1].value")
            .extractFirst(Int::class.java)

        nested shouldBe 12
    }

    "extracts value from single object" {
        val objMsg = "{" +
            "\"active\":true," +
            "\"count\":5" +
            "}"

        val result = PathFilterBuilder.from(objMsg)
            .addPathFilter("$.count", 5)
            .addValueFilter("$.active")
            .extractFirst(Boolean::class.java)

        result shouldBe true
    }

    "matches numeric path filters across types" {
        val result = PathFilterBuilder.from(message)
            .addPathFilter("$.id", 1L)
            .addValueFilter("$.sensor[0].value")
            .extractFirst(Double::class.java)

        result shouldBe 39.0
    }

    "converts extracted number to target type" {
        val result = PathFilterBuilder.from(message)
            .addValueFilter("$.sensor[0].value")
            .extractFirst(Long::class.java)

        result shouldBe 39L
    }
})
