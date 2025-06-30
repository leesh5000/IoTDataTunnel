package me.helloc.iot.tunnel;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MqttBufferedSubscriberBuilderTest {
    @Test
    void subscribesWithConfiguredQos() throws Exception {
        MqttAsyncClient client = mock(MqttAsyncClient.class);
        ScheduledExecutorService scheduler = mock(ScheduledExecutorService.class);
        when(scheduler.schedule(any(Runnable.class), any(), any()))
                .thenReturn(mock(ScheduledFuture.class));

        MqttBufferedSubscriber manager = MqttBufferedSubscriber.builder()
                .brokerUrl("tcp://localhost:1883")
                .addTopic("test")
                .clientSupplier(() -> client)
                .scheduler(scheduler)
                .qos(2)
                .build();

        ArgumentCaptor<IMqttActionListener> captor = ArgumentCaptor.forClass(IMqttActionListener.class);
        when(client.connect(any(MqttConnectOptions.class), isNull(), captor.capture()))
                .thenReturn(mock(IMqttToken.class));

        manager.connect();
        captor.getValue().onSuccess(mock(IMqttToken.class));

        verify(client).subscribe("test", 2);
    }

    @Test
    void invalidQosThrowsException() {
        assertThrows(IllegalArgumentException.class, () ->
            MqttBufferedSubscriber.builder()
                    .brokerUrl("tcp://localhost:1883")
                    .qos(3)
        );
    }
}
