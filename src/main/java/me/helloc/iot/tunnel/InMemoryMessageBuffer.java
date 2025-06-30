package me.helloc.iot.tunnel;

import java.util.AbstractMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Simple in-memory implementation of {@link MessageBuffer}.
 */
public class InMemoryMessageBuffer implements MessageBuffer {
    private final Queue<Map.Entry<String, String>> queue = new ConcurrentLinkedQueue<>();

    @Override
    public void add(String topic, String message) {
        queue.add(new AbstractMap.SimpleEntry<>(topic, message));
    }

    @Override
    public Map.Entry<String, String> poll() {
        return queue.poll();
    }
}
