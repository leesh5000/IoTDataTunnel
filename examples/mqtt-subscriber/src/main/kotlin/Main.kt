package me.helloc.example.mqttsubscriber

import me.helloc.iot.tunnel.ConnectionManager

fun main() {
    val manager = ConnectionManager.builder()
        .brokerUrl("tcp://broker.hivemq.com:1883")
        .addTopic("sensors/data")
        .build()

    manager.addListener(object : ConnectionManager.ConnectionListener {
        override fun onConnected() {
            println("Connected")
        }

        override fun onConnectionLost(cause: Throwable) {
            println("Connection lost: ${'$'}{cause.message}")
        }

        override fun onDisconnected() {
            println("Disconnected")
        }
    })

    manager.connect()
}
