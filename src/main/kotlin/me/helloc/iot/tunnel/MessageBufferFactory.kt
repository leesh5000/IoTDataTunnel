package me.helloc.iot.tunnel

import org.yaml.snakeyaml.Yaml

/**
 * Factory for creating [MessageBuffer] instances from configuration.
 */
object MessageBufferFactory {
    /**
     * Loads [MessageBuffer] configuration from a YAML resource on the classpath.
     * If the resource does not exist or is invalid, an [InMemoryMessageBuffer]
     * is returned.
     */
    fun fromConfig(resource: String = "application.yml"): MessageBuffer {
        val stream = javaClass.classLoader.getResourceAsStream(resource)
            ?: return InMemoryMessageBuffer()
        val yaml = Yaml().load<Map<String, Any?>>(stream)
        return fromMap(yaml)
    }

    internal fun fromMap(map: Map<String, Any?>): MessageBuffer {
        val buffer = ((map["iotdatatunnel"] as? Map<*, *>)?.get("buffer") as? Map<*, *>)
            ?: return InMemoryMessageBuffer()
        val type = buffer["type"]?.toString()?.lowercase() ?: "inmemory"
        val host = buffer["host"]?.toString() ?: "localhost"
        val port = buffer["port"]?.toString()?.toIntOrNull()
        return when (type) {
            "redis" -> RedisMessageBuffer(host, port ?: 6379)
            "kafka" -> KafkaMessageBuffer(host, port ?: 9092)
            else -> InMemoryMessageBuffer()
        }
    }
}
