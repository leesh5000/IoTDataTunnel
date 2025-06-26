package me.helloc.iot.tunnel

import io.kotest.core.spec.style.StringSpec
import io.kotest.assertions.throwables.shouldThrow
import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttToken
import org.eclipse.paho.client.mqttv3.MqttAsyncClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture

class MqttBufferedSubscriberBuilderTest : StringSpec({
    "subscribes with configured qos" {
        val client = mock<MqttAsyncClient>()
        val scheduler = mock<ScheduledExecutorService>()
        whenever(scheduler.schedule(org.mockito.kotlin.any<Runnable>(), org.mockito.kotlin.any(), org.mockito.kotlin.any()))
            .thenReturn(mock<ScheduledFuture<*>>())

        val manager = MqttBufferedSubscriber.builder()
            .brokerUrl("tcp://localhost:1883")
            .addTopic("test")
            .clientSupplier { client }
            .scheduler(scheduler)
            .qos(2)
            .build()

        val captor = argumentCaptor<IMqttActionListener>()
        whenever(client.connect(org.mockito.kotlin.any<MqttConnectOptions>(), org.mockito.kotlin.isNull(), captor.capture()))
            .thenReturn(mock<IMqttToken>())

        manager.connect()
        captor.firstValue.onSuccess(mock())

        verify(client).subscribe("test", 2)
    }

    "invalid qos throws exception" {
        shouldThrow<IllegalArgumentException> {
            MqttBufferedSubscriber.builder()
                .brokerUrl("tcp://localhost:1883")
                .qos(3)
        }
    }
})
