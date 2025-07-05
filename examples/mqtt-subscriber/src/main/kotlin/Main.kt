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

    // DefaultConnectionListener 어댑터를 사용하여 필요한 메서드만 구현
    manager.addListener(object : MqttBufferedSubscriber.DefaultConnectionListener() {
        override fun onConnected() {
            println("Connected to MQTT broker!")
        }

        override fun onConnectionLost(cause: Throwable) {
            println("Connection lost: ${cause.message}")
        }
        // onDisconnected()는 구현하지 않아도 됨 - 기본 빈 구현 사용
    })
    
    manager.connect()
    
    // 프로그램이 종료되지 않도록 대기
    println("Press Ctrl+C to exit...")
    Thread.currentThread().join()
}
