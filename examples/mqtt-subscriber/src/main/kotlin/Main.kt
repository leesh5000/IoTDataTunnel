package me.helloc.example.mqttsubscriber

import me.helloc.iot.tunnel.MqttBufferedSubscriber

fun main() {
    val manager = MqttBufferedSubscriber.builder()
        .brokerUrl("tcp://broker.hivemq.com:1883")
        .addTopic("sensors/data")
        .build()

    manager.addListener(DefaultConnectionListener())
    manager.connect()
}
