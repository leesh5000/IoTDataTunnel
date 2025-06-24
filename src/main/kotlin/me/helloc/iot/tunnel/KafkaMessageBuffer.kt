package me.helloc.iot.tunnel

import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Simple Kafka-backed implementation placeholder.
 */
class KafkaMessageBuffer(
    val host: String,
    val port: Int
) : MessageBuffer {
    private val queue = ConcurrentLinkedQueue<Pair<String, String>>()

    override fun add(topic: String, message: String) {
        queue.add(topic to message)
    }

    override fun poll(): Pair<String, String>? = queue.poll()
}
