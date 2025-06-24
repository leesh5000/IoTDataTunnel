package me.helloc.iot.tunnel

/**
 * Buffer storage for messages delivered via MQTT.
 * Implementations may store messages in memory or external systems.
 */
interface MessageBuffer {
    /**
     * Adds a message for the given topic to the buffer.
     */
    fun add(topic: String, message: String)

    /**
     * Retrieves and removes the earliest buffered message or returns `null` if empty.
     */
    fun poll(): Pair<String, String>?
}
