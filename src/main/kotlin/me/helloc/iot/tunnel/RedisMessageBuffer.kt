package me.helloc.iot.tunnel

import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Simple Redis-backed implementation placeholder.
 * For now it stores data locally but exposes host/port configuration.
 */
class RedisMessageBuffer(
    val host: String,
    val port: Int
) : MessageBuffer {
    private val queue = ConcurrentLinkedQueue<Pair<String, String>>()

    override fun add(topic: String, message: String) {
        queue.add(topic to message)
    }

    override fun poll(): Pair<String, String>? = queue.poll()
}
