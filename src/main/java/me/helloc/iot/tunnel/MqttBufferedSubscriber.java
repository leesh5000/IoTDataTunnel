package me.helloc.iot.tunnel;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Manages MQTT connection with automatic reconnection and subscription restoration.
 */
public class MqttBufferedSubscriber {

    public interface ConnectionListener {
        void onConnected();
        void onConnectionLost(Throwable cause);
        void onDisconnected();
    }

    public interface MessageListener {
        void onMessageReceived(String topic, String message);
        void onBufferedBefore(String topic, String message);
        void onBufferedAfter(String topic, String message);
    }

    private final String brokerUrl;
    private final String clientId;
    private final String[] topics;
    private final MqttConnectOptions options;
    private final ScheduledExecutorService scheduler;
    public final MessageBuffer messageBuffer;
    private final Supplier<MqttAsyncClient> clientSupplier;
    private final MqttAsyncClient client;
    private final CopyOnWriteArrayList<ConnectionListener> listeners = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<MessageListener> messageListeners = new CopyOnWriteArrayList<>();
    private final long initialDelay;
    private final long maxDelay;
    private final int qos;
    private volatile long currentDelay;

    private MqttBufferedSubscriber(Builder builder) {
        this.brokerUrl = Objects.requireNonNull(builder.brokerUrl, "brokerUrl");
        this.clientId = builder.clientId != null ? builder.clientId : MqttAsyncClient.generateClientId();
        this.topics = builder.topics.toArray(new String[0]);
        this.options = builder.options;
        this.scheduler = builder.scheduler != null ? builder.scheduler : Executors.newSingleThreadScheduledExecutor();
        this.messageBuffer = builder.messageBuffer;
        this.clientSupplier = builder.clientSupplier != null ? builder.clientSupplier : () -> {
            try {
                return new MqttAsyncClient(brokerUrl, clientId);
            } catch (MqttException e) {
                throw new RuntimeException(e);
            }
        };
        this.client = clientSupplier.get();
        this.client.setCallback(new InternalCallback());
        this.initialDelay = builder.initialDelay;
        this.maxDelay = builder.maxDelay;
        this.qos = builder.qos;
        this.currentDelay = initialDelay;
    }

    public void addListener(ConnectionListener listener) {
        listeners.add(listener);
    }

    public void removeListener(ConnectionListener listener) {
        listeners.remove(listener);
    }

    public void addMessageListener(MessageListener listener) {
        messageListeners.add(listener);
    }

    public void removeMessageListener(MessageListener listener) {
        messageListeners.remove(listener);
    }

    public void connect() {
        try {
            client.connect(options, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    currentDelay = initialDelay;
                    subscribeAll();
                    for (ConnectionListener l : listeners) {
                        l.onConnected();
                    }
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    scheduleReconnect();
                }
            });
        } catch (MqttException e) {
            scheduleReconnect();
        }
    }

    public void disconnect() {
        scheduler.shutdownNow();
        try {
            client.disconnect();
            for (ConnectionListener l : listeners) {
                l.onDisconnected();
            }
        } catch (MqttException ignore) {
        }
    }

    private void scheduleReconnect() {
        scheduler.schedule(this::connect, currentDelay, TimeUnit.MILLISECONDS);
        currentDelay = Math.min(currentDelay * 2, maxDelay);
    }

    private void subscribeAll() {
        for (String topic : topics) {
            try {
                client.subscribe(topic, qos);
            } catch (MqttException ignore) {
            }
        }
    }

    private class InternalCallback implements MqttCallbackExtended {
        @Override
        public void connectComplete(boolean reconnect, String serverURI) {
            if (reconnect) {
                subscribeAll();
                for (ConnectionListener l : listeners) {
                    l.onConnected();
                }
            }
        }

        @Override
        public void connectionLost(Throwable cause) {
            if (cause != null) {
                for (ConnectionListener l : listeners) {
                    l.onConnectionLost(cause);
                }
            }
            scheduleReconnect();
        }

        @Override
        public void messageArrived(String topic, MqttMessage message) {
            if (topic != null && message != null) {
                String payload = new String(message.getPayload());
                for (MessageListener l : messageListeners) {
                    l.onMessageReceived(topic, payload);
                }
                for (MessageListener l : messageListeners) {
                    l.onBufferedBefore(topic, payload);
                }
                messageBuffer.add(topic, payload);
                for (MessageListener l : messageListeners) {
                    l.onBufferedAfter(topic, payload);
                }
            }
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken token) {
        }
    }

    public static class Builder {
        private String brokerUrl;
        private String clientId;
        private final List<String> topics = new ArrayList<>();
        private MqttConnectOptions options = new MqttConnectOptions();
        private ScheduledExecutorService scheduler;
        private Supplier<MqttAsyncClient> clientSupplier;
        private long initialDelay = 1000;
        private long maxDelay = 60000;
        private MessageBuffer messageBuffer = new InMemoryMessageBuffer();
        private int qos = 1;

        public Builder brokerUrl(String brokerUrl) { this.brokerUrl = brokerUrl; return this; }
        public Builder clientId(String clientId) { this.clientId = clientId; return this; }
        public Builder addTopic(String topic) { this.topics.add(topic); return this; }
        public Builder options(MqttConnectOptions options) { this.options = options; return this; }
        public Builder scheduler(ScheduledExecutorService scheduler) { this.scheduler = scheduler; return this; }
        public Builder clientSupplier(Supplier<MqttAsyncClient> supplier) { this.clientSupplier = supplier; return this; }
        public Builder initialDelay(long delay) { this.initialDelay = delay; return this; }
        public Builder maxDelay(long delay) { this.maxDelay = delay; return this; }
        public Builder messageBuffer(MessageBuffer buffer) { this.messageBuffer = buffer; return this; }
        public Builder qos(int qos) {
            if (qos < 0 || qos > 2) throw new IllegalArgumentException("QoS must be between 0 and 2");
            this.qos = qos;
            return this;
        }

        public MqttBufferedSubscriber build() {
            if (brokerUrl == null || brokerUrl.isEmpty()) {
                throw new IllegalArgumentException("brokerUrl must not be null or empty");
            }
            return new MqttBufferedSubscriber(this);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Default adapter implementation of ConnectionListener.
     * Provides empty implementations of all methods.
     */
    public static abstract class DefaultConnectionListener implements ConnectionListener {
        @Override
        public void onConnected() {
        }

        @Override
        public void onConnectionLost(Throwable cause) {
        }

        @Override
        public void onDisconnected() {
        }
    }

    /**
     * Default adapter implementation of MessageListener.
     * Provides empty implementations of all methods.
     */
    public static abstract class DefaultMessageListener implements MessageListener {
        @Override
        public void onMessageReceived(String topic, String message) {
        }

        @Override
        public void onBufferedBefore(String topic, String message) {
        }

        @Override
        public void onBufferedAfter(String topic, String message) {
        }
    }

    /**
     * Default adapter implementation of both ConnectionListener and MessageListener.
     * Provides empty implementations of all methods from both interfaces.
     */
    public static abstract class DefaultMqttListener implements ConnectionListener, MessageListener {
        @Override
        public void onConnected() {
        }

        @Override
        public void onConnectionLost(Throwable cause) {
        }

        @Override
        public void onDisconnected() {
        }

        @Override
        public void onMessageReceived(String topic, String message) {
        }

        @Override
        public void onBufferedBefore(String topic, String message) {
        }

        @Override
        public void onBufferedAfter(String topic, String message) {
        }
    }
}
