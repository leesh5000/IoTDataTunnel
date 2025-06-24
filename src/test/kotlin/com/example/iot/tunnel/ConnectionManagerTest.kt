package com.example.iot.tunnel

import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttToken
import org.eclipse.paho.client.mqttv3.MqttAsyncClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttException
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.*
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

class ConnectionManagerTest {

    @Test
    fun connectSuccessNotifiesListener() {
        val client = mock(MqttAsyncClient::class.java)
        val scheduler = mock(ScheduledExecutorService::class.java)
        `when`(scheduler.schedule(any(Runnable::class.java), anyLong(), any())).thenReturn(
            mock(ScheduledFuture::class.java) as ScheduledFuture<*>
        )

        val listener = mock(ConnectionManager.ConnectionListener::class.java)
        val manager = ConnectionManager.builder()
            .brokerUrl("tcp://localhost:1883")
            .addTopic("test")
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

        val manager = ConnectionManager.builder()
            .brokerUrl("tcp://localhost:1883")
            .clientSupplier { client }
            .scheduler(scheduler)
            .initialDelay(10)
            .maxDelay(20)
            .build()

        val captor = ArgumentCaptor.forClass(IMqttActionListener::class.java)
        `when`(client.connect(any(MqttConnectOptions::class.java), isNull(), captor.capture()))
            .thenReturn(mock(IMqttToken::class.java))

        manager.connect()
        captor.value.onFailure(mock(IMqttToken::class.java), MqttException(0))

        verify(scheduler).schedule(any(Runnable::class.java), eq(10L), eq(TimeUnit.MILLISECONDS))
        verify(client, times(2)).connect(any(MqttConnectOptions::class.java), isNull(), any())
    }
}
