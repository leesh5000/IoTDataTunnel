package me.helloc.iot.tunnel;

import org.eclipse.paho.client.mqttv3.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MqttBufferedSubscriberTest {

    @Test
    void connectSuccessNotifiesListener() throws Exception {
        MqttAsyncClient client = mock(MqttAsyncClient.class);
        ScheduledExecutorService scheduler = mock(ScheduledExecutorService.class);
        when(scheduler.schedule(any(Runnable.class), anyLong(), any()))
                .thenReturn(mock(ScheduledFuture.class));

        MqttBufferedSubscriber.ConnectionListener listener = mock(MqttBufferedSubscriber.ConnectionListener.class);
        MqttBufferedSubscriber manager = MqttBufferedSubscriber.builder()
                .brokerUrl("tcp://localhost:1883")
                .addTopic("test")
                .qos(1)
                .clientSupplier(() -> client)
                .scheduler(scheduler)
                .build();
        manager.addListener(listener);

        ArgumentCaptor<IMqttActionListener> captor = ArgumentCaptor.forClass(IMqttActionListener.class);
        when(client.connect(any(MqttConnectOptions.class), isNull(), captor.capture()))
                .thenReturn(mock(IMqttToken.class));

        manager.connect();
        captor.getValue().onSuccess(mock(IMqttToken.class));

        verify(client).subscribe("test", 1);
        verify(listener).onConnected();
    }

    @Test
    void reconnectionScheduledOnFailure() throws Exception {
        MqttAsyncClient client = mock(MqttAsyncClient.class);
        ScheduledExecutorService scheduler = mock(ScheduledExecutorService.class);
        when(scheduler.schedule(any(Runnable.class), anyLong(), any())).thenAnswer(invocation -> {
            Runnable r = invocation.getArgument(0);
            r.run();
            return mock(ScheduledFuture.class);
        });

        MqttBufferedSubscriber manager = MqttBufferedSubscriber.builder()
                .brokerUrl("tcp://localhost:1883")
                .clientSupplier(() -> client)
                .scheduler(scheduler)
                .initialDelay(10)
                .maxDelay(20)
                .qos(1)
                .build();

        ArgumentCaptor<IMqttActionListener> captor = ArgumentCaptor.forClass(IMqttActionListener.class);
        when(client.connect(any(MqttConnectOptions.class), isNull(), captor.capture()))
                .thenReturn(mock(IMqttToken.class));

        manager.connect();
        captor.getValue().onFailure(mock(IMqttToken.class), new MqttException(0));

        verify(scheduler).schedule(any(Runnable.class), eq(10L), eq(TimeUnit.MILLISECONDS));
        verify(client, times(2)).connect(any(MqttConnectOptions.class), isNull(), any());
    }

    @Test
    void reconnectsAndResubscribesAfterConnectionLost() throws Exception {
        MqttAsyncClient client = mock(MqttAsyncClient.class);
        ScheduledExecutorService scheduler = mock(ScheduledExecutorService.class);
        when(scheduler.schedule(any(Runnable.class), anyLong(), any())).thenAnswer(invocation -> {
            Runnable r = invocation.getArgument(0);
            r.run();
            return mock(ScheduledFuture.class);
        });

        MqttBufferedSubscriber.ConnectionListener listener = mock(MqttBufferedSubscriber.ConnectionListener.class);
        MqttBufferedSubscriber manager = MqttBufferedSubscriber.builder()
                .brokerUrl("tcp://localhost:1883")
                .addTopic("test")
                .clientSupplier(() -> client)
                .scheduler(scheduler)
                .initialDelay(10)
                .maxDelay(20)
                .qos(2)
                .build();
        manager.addListener(listener);

        ArgumentCaptor<IMqttActionListener> connectCaptor = ArgumentCaptor.forClass(IMqttActionListener.class);
        when(client.connect(any(MqttConnectOptions.class), isNull(), connectCaptor.capture()))
                .thenReturn(mock(IMqttToken.class));

        ArgumentCaptor<MqttCallbackExtended> callbackCaptor = ArgumentCaptor.forClass(MqttCallbackExtended.class);
        verify(client).setCallback(callbackCaptor.capture());

        manager.connect();
        connectCaptor.getAllValues().get(0).onSuccess(mock(IMqttToken.class));

        callbackCaptor.getValue().connectionLost(new MqttException(0));
        connectCaptor.getAllValues().get(1).onSuccess(mock(IMqttToken.class));

        verify(scheduler).schedule(any(Runnable.class), eq(10L), eq(TimeUnit.MILLISECONDS));
        verify(client, times(2)).subscribe("test", 2);
        verify(listener, times(2)).onConnected();
    }
}
