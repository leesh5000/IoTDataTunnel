package me.helloc.iot.tunnel

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class SimpleJsonParserTest : StringSpec({
    "parses complex structures" {
        val json = """{"a":1,"b":[2,3],"c":{"d":4},"e":true,"f":"str"}"""
        val result = SimpleJsonParser(json).parse() as Map<*, *>
        result["a"] shouldBe 1L
        (result["b"] as List<*>)[1] shouldBe 3L
        ((result["c"] as Map<*, *>)["d"]) shouldBe 4L
        result["e"] shouldBe true
        result["f"] shouldBe "str"
    }
})
