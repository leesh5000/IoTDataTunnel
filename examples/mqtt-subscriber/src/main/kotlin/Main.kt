package me.helloc.example.mqttsubscriber

import me.helloc.iot.tunnel.MqttBufferedSubscriber

fun main() {
    val manager = MqttBufferedSubscriber.builder()
        .brokerUrl("tcp://broker.hivemq.com:1883")
        .initialDelay(1000)
        .maxDelay(60000)
        .addTopic("sensors/data")
        .qos(1)
        .build()

    manager.addListener(DefaultConnectionListener())
    manager.connect()
}
