package me.helloc.iot.tunnel

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.eclipse.paho.client.mqttv3.MqttAsyncClient
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture

class MessageBufferTest : StringSpec({
    "messageArrived stores payload to buffer" {
        val client = mock<MqttAsyncClient>()
        val scheduler = mock<ScheduledExecutorService>()
        whenever(scheduler.schedule(org.mockito.kotlin.any<Runnable>(), org.mockito.kotlin.any<Long>(), org.mockito.kotlin.any()))
            .thenReturn(mock<ScheduledFuture<*>>())

        val manager = MqttBufferedSubscriber.builder()
            .brokerUrl("tcp://localhost:1883")
            .clientSupplier { client }
            .scheduler(scheduler)
            .build()

        val msgListener = mock<MqttBufferedSubscriber.MessageListener>()
        manager.addMessageListener(msgListener)

        val captor = argumentCaptor<MqttCallbackExtended>()
        verify(client).setCallback(captor.capture())

        val message = MqttMessage("data".toByteArray())
        captor.firstValue.messageArrived("test/topic", message)

        verify(msgListener).onMessageReceived("test/topic", "data")
        verify(msgListener).onBufferedBefore("test/topic", "data")
        verify(msgListener).onBufferedAfter("test/topic", "data")

        manager.messageBuffer.poll() shouldBe ("test/topic" to "data")
    }
})
