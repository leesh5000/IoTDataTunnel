package me.helloc.iot.tunnel

import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.IMqttToken
import org.eclipse.paho.client.mqttv3.MqttAsyncClient
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.MqttMessage
import me.helloc.iot.tunnel.MessageBuffer
import me.helloc.iot.tunnel.MessageBufferFactory
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

/**
 * Manages MQTT connection with automatic reconnection and subscription restoration.
 */
class MqttBufferedSubscriber private constructor(builder: Builder) {

    interface ConnectionListener {
        fun onConnected()
        fun onConnectionLost(cause: Throwable)
        fun onDisconnected()
    }

    interface MessageListener {
        fun onMessageReceived(topic: String, message: String)
        fun onBufferedBefore(topic: String, message: String)
        fun onBufferedAfter(topic: String, message: String)
    }

    private val brokerUrl: String = builder.brokerUrl!!
    private val clientId: String = builder.clientId ?: MqttAsyncClient.generateClientId()
    private val topics: Array<String> = builder.topics.toTypedArray()
    private val options: MqttConnectOptions = builder.options
    private val scheduler: ScheduledExecutorService = builder.scheduler ?: Executors.newSingleThreadScheduledExecutor()
    val messageBuffer: MessageBuffer = builder.messageBuffer
    private val clientSupplier: () -> MqttAsyncClient = builder.clientSupplier ?: {
        MqttAsyncClient(brokerUrl, clientId)
    }
    private val client: MqttAsyncClient = clientSupplier.invoke().apply {
        setCallback(InternalCallback())
    }
    private val listeners = CopyOnWriteArrayList<ConnectionListener>()
    private val messageListeners = CopyOnWriteArrayList<MessageListener>()
    private val initialDelay: Long = builder.initialDelay
    private val maxDelay: Long = builder.maxDelay
    private val qos: Int = builder.qos
    @Volatile private var currentDelay: Long = initialDelay

    fun addListener(listener: ConnectionListener) {
        listeners.add(listener)
    }

    fun removeListener(listener: ConnectionListener) {
        listeners.remove(listener)
    }

    fun addMessageListener(listener: MessageListener) {
        messageListeners.add(listener)
    }

    fun removeMessageListener(listener: MessageListener) {
        messageListeners.remove(listener)
    }

    fun connect() {
        try {
            client.connect(options, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    currentDelay = initialDelay
                    subscribeAll()
                    listeners.forEach { it.onConnected() }
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    scheduleReconnect()
                }
            })
        } catch (e: MqttException) {
            scheduleReconnect()
        }
    }

    fun disconnect() {
        scheduler.shutdownNow()
        try {
            client.disconnect()
            listeners.forEach { it.onDisconnected() }
        } catch (_: MqttException) {
        }
    }

    private fun scheduleReconnect() {
        scheduler.schedule({ connect() }, currentDelay, TimeUnit.MILLISECONDS)
        currentDelay = (currentDelay * 2).coerceAtMost(maxDelay)
    }

    private fun subscribeAll() {
        for (topic in topics) {
            try {
                client.subscribe(topic, qos)
            } catch (_: MqttException) {
            }
        }
    }

    private inner class InternalCallback : MqttCallbackExtended {
        override fun connectComplete(reconnect: Boolean, serverURI: String?) {
            if (reconnect) {
                subscribeAll()
                listeners.forEach { it.onConnected() }
            }
        }

        override fun connectionLost(cause: Throwable?) {
            if (cause != null) listeners.forEach { it.onConnectionLost(cause) }
            scheduleReconnect()
        }

        override fun messageArrived(topic: String?, message: MqttMessage?) {
            if (topic != null && message != null) {
                val payload = String(message.payload)
                messageListeners.forEach { it.onMessageReceived(topic, payload) }
                messageListeners.forEach { it.onBufferedBefore(topic, payload) }
                messageBuffer.add(topic, payload)
                messageListeners.forEach { it.onBufferedAfter(topic, payload) }
            }
        }

        override fun deliveryComplete(token: IMqttDeliveryToken?) {
        }
    }

    class Builder {
        var brokerUrl: String? = null
            private set
        var clientId: String? = null
            private set
        internal val topics = mutableListOf<String>()
        var options: MqttConnectOptions = MqttConnectOptions()
            private set
        var scheduler: ScheduledExecutorService? = null
            private set
        var clientSupplier: (() -> MqttAsyncClient)? = null
            private set
        var initialDelay: Long = 1000
            private set
        var maxDelay: Long = 60000
            private set
        var messageBuffer: MessageBuffer = MessageBufferFactory.fromConfig()
            private set
        var qos: Int = 1
            private set

        fun brokerUrl(brokerUrl: String) = apply { this.brokerUrl = brokerUrl }
        fun clientId(clientId: String) = apply { this.clientId = clientId }
        fun addTopic(topic: String) = apply { this.topics.add(topic) }
        fun options(options: MqttConnectOptions) = apply { this.options = options }
        fun scheduler(scheduler: ScheduledExecutorService) = apply { this.scheduler = scheduler }
        fun clientSupplier(supplier: () -> MqttAsyncClient) = apply { this.clientSupplier = supplier }
        fun initialDelay(delay: Long) = apply { this.initialDelay = delay }
        fun maxDelay(delay: Long) = apply { this.maxDelay = delay }
        fun messageBuffer(buffer: MessageBuffer) = apply { this.messageBuffer = buffer }
        fun qos(qos: Int) = apply {
            require(qos in 0..2) { "QoS must be between 0 and 2" }
            this.qos = qos
        }

        fun build(): MqttBufferedSubscriber {
            require(!brokerUrl.isNullOrEmpty()) { "brokerUrl" }
            return MqttBufferedSubscriber(this)
        }
    }

    companion object {
        fun builder() = Builder()
    }
}
