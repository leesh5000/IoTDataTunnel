import me.helloc.iot.tunnel.MqttBufferedSubscriber
import me.helloc.iot.tunnel.PathFilterBuilder

fun main() {

    val subscriber = MqttBufferedSubscriber.builder()
        .brokerUrl("tcp://broker.hivemq.com:1883")
        .addTopic("sensors/data")
        .build()

    subscriber.addListener(object : MqttBufferedSubscriber.ConnectionListener, MqttBufferedSubscriber.MessageListener {
        override fun onConnected() {
            println("Connected")
        }

        override fun onConnectionLost(cause: Throwable) {
            println("Connection lost: ${cause.message}")
        }

        override fun onDisconnected() {
            println("Disconnected")
        }

        override fun onMessageReceived(topic: String, message: String) {
            println("Message received on topic '$topic': $message")
            subscriber.messageBuffer.add(
                topic,
                message
            )
        }

        override fun onBufferedBefore(topic: String, message: String) {
            println("Buffered message on topic '$topic': $message")
        }

        override fun onBufferedAfter(topic: String, message: String) {
            println("Buffered message after processing on topic '$topic': $message")
        }

    })

    subscriber.connect()

    // 프로그램이 종료되지 않고 계속 메시지를 수신하도록 변경합니다.
    while (true) {
        val pair = subscriber.messageBuffer.poll()
        if (pair != null) {
            val (_, message) = pair
            val value = PathFilterBuilder.from(message)
                .addValueFilter("$.sensor[0].value")
                .extractFirst(Int::class.java)
            println("Extracted value: $value")
        }
        Thread.sleep(500)
    }

}
