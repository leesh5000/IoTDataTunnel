package me.helloc.iot.tunnel

import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Simple in-memory implementation of [MessageBuffer].
 */
class InMemoryMessageBuffer : MessageBuffer {
    private val queue = ConcurrentLinkedQueue<Pair<String, String>>()

    override fun add(topic: String, message: String) {
        queue.add(topic to message)
    }

    override fun poll(): Pair<String, String>? = queue.poll()
}
