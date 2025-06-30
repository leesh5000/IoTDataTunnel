package me.helloc.iot.tunnel;

import java.util.Map;

/**
 * Buffer storage for messages delivered via MQTT.
 * Implementations may store messages in memory or external systems.
 */
public interface MessageBuffer {
    /**
     * Adds a message for the given topic to the buffer.
     */
    void add(String topic, String message);

    /**
     * Retrieves and removes the earliest buffered message or returns {@code null} if empty.
     */
    Map.Entry<String, String> poll();
}
