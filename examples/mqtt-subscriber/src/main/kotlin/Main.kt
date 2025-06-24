package me.helloc.example.mqttsubscriber

import me.helloc.iot.tunnel.ConnectionManager

fun main() {
    val manager = ConnectionManager.builder()
        .brokerUrl("tcp://broker.hivemq.com:1883")
        .addTopic("sensors/data")
        .build()

    manager.addListener(DefaultConnectionListener())
    manager.connect()
}
