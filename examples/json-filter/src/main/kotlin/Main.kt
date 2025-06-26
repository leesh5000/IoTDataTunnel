package me.helloc.example.jsonfilter

import me.helloc.iot.tunnel.PathFilterBuilder

fun main() {
    val message = """{
          "id":1,
          "gateways":[{"id":1},{"id":2}],
          "companyCode":"0012",
          "sensor":[
            {"type":"temp","value":39},
            {"type":"airFlow","value":12},
            {"type":"humidity","value":57}
          ]
    }"""

    val temp = PathFilterBuilder.from(message)
        .addPathFilter("$.id", 1)
        .addPathFilter("$.gateways[0].id", 1)
        .addPathFilter("$.companyCode", "0012")
        .addValueFilter("$.sensor[0].value")
        .extractFirst(Int::class.java)

    println("Extracted temperature value: $temp")
}
