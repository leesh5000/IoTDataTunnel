package me.helloc.iot.tunnel

import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttToken
import org.eclipse.paho.client.mqttv3.MqttAsyncClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.*
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

class MqttBufferedSubscriberTest {

    @Test
    fun connectSuccessNotifiesListener() {
        val client = mock(MqttAsyncClient::class.java)
        val scheduler = mock(ScheduledExecutorService::class.java)
        `when`(scheduler.schedule(any(Runnable::class.java), anyLong(), any())).thenReturn(
            mock(ScheduledFuture::class.java) as ScheduledFuture<*>
        )

        val listener = mock(MqttBufferedSubscriber.ConnectionListener::class.java)
        val manager = MqttBufferedSubscriber.builder()
            .brokerUrl("tcp://localhost:1883")
            .addTopic("test")
            .qos(1)
            .clientSupplier { client }
            .scheduler(scheduler)
            .build()
        manager.addListener(listener)

        val captor = ArgumentCaptor.forClass(IMqttActionListener::class.java)
        `when`(client.connect(any(MqttConnectOptions::class.java), isNull(), captor.capture()))
            .thenReturn(mock(IMqttToken::class.java))

        manager.connect()

        captor.value.onSuccess(mock(IMqttToken::class.java))

        verify(client).subscribe("test", 1)
        verify(listener).onConnected()
    }

    @Test
    fun reconnectionScheduledOnFailure() {
        val client = mock(MqttAsyncClient::class.java)
        val scheduler = mock(ScheduledExecutorService::class.java)
        `when`(scheduler.schedule(any(Runnable::class.java), anyLong(), any())).thenAnswer {
            val r = it.getArgument<Runnable>(0)
            r.run()
            mock(ScheduledFuture::class.java) as ScheduledFuture<*>
        }

        val manager = MqttBufferedSubscriber.builder()
            .brokerUrl("tcp://localhost:1883")
            .clientSupplier { client }
            .scheduler(scheduler)
            .initialDelay(10)
            .maxDelay(20)
            .qos(1)
            .build()

        val captor = ArgumentCaptor.forClass(IMqttActionListener::class.java)
        `when`(client.connect(any(MqttConnectOptions::class.java), isNull(), captor.capture()))
            .thenReturn(mock(IMqttToken::class.java))

        manager.connect()
        captor.value.onFailure(mock(IMqttToken::class.java), MqttException(0))

        verify(scheduler).schedule(any(Runnable::class.java), eq(10L), eq(TimeUnit.MILLISECONDS))
        verify(client, times(2)).connect(any(MqttConnectOptions::class.java), isNull(), any())
    }

    @Test
    fun reconnectsAndResubscribesAfterConnectionLost() {
        val client = mock(MqttAsyncClient::class.java)
        val scheduler = mock(ScheduledExecutorService::class.java)
        `when`(scheduler.schedule(any(Runnable::class.java), anyLong(), any())).thenAnswer {
            val r = it.getArgument<Runnable>(0)
            r.run()
            mock(ScheduledFuture::class.java) as ScheduledFuture<*>
        }

        val listener = mock(MqttBufferedSubscriber.ConnectionListener::class.java)
        val manager = MqttBufferedSubscriber.builder()
            .brokerUrl("tcp://localhost:1883")
            .addTopic("test")
            .clientSupplier { client }
            .scheduler(scheduler)
            .initialDelay(10)
            .maxDelay(20)
            .qos(2)
            .build()
        manager.addListener(listener)

        val connectCaptor = ArgumentCaptor.forClass(IMqttActionListener::class.java)
        `when`(client.connect(any(MqttConnectOptions::class.java), isNull(), connectCaptor.capture()))
            .thenReturn(mock(IMqttToken::class.java))

        val callbackCaptor = ArgumentCaptor.forClass(MqttCallbackExtended::class.java)
        verify(client).setCallback(callbackCaptor.capture())

        manager.connect()
        connectCaptor.allValues[0].onSuccess(mock(IMqttToken::class.java))

        callbackCaptor.value.connectionLost(MqttException(0))
        connectCaptor.allValues[1].onSuccess(mock(IMqttToken::class.java))

        verify(scheduler).schedule(any(Runnable::class.java), eq(10L), eq(TimeUnit.MILLISECONDS))
        verify(client, times(2)).subscribe("test", 2)
        verify(listener, times(2)).onConnected()
    }
}
