package me.helloc.iot.tunnel;

import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MessageBufferTest {
    @Test
    void messageArrivedStoresPayloadToBuffer() throws Exception {
        MqttAsyncClient client = mock(MqttAsyncClient.class);
        ScheduledExecutorService scheduler = mock(ScheduledExecutorService.class);
        when(scheduler.schedule(any(Runnable.class), anyLong(), any()))
                .thenReturn(mock(ScheduledFuture.class));

        MqttBufferedSubscriber manager = MqttBufferedSubscriber.builder()
                .brokerUrl("tcp://localhost:1883")
                .clientSupplier(() -> client)
                .scheduler(scheduler)
                .build();

        MqttBufferedSubscriber.MessageListener msgListener = mock(MqttBufferedSubscriber.MessageListener.class);
        manager.addMessageListener(msgListener);

        ArgumentCaptor<MqttCallbackExtended> captor = ArgumentCaptor.forClass(MqttCallbackExtended.class);
        verify(client).setCallback(captor.capture());

        MqttMessage message = new MqttMessage("data".getBytes());
        captor.getValue().messageArrived("test/topic", message);

        verify(msgListener).onMessageReceived("test/topic", "data");
        verify(msgListener).onBufferedBefore("test/topic", "data");
        verify(msgListener).onBufferedAfter("test/topic", "data");

        Map.Entry<String, String> pair = manager.messageBuffer.poll();
        assertNotNull(pair);
        assertEquals("test/topic", pair.getKey());
        assertEquals("data", pair.getValue());
    }
}
