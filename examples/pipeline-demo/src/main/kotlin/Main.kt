import me.helloc.iot.tunnel.MqttBufferedSubscriber
import me.helloc.iot.tunnel.PathFilterBuilder

fun main() {

    val subscriber = MqttBufferedSubscriber.builder()
        .brokerUrl("tcp://broker.hivemq.com:1883")
        .initialDelay(1000)
        .maxDelay(60000)
        .addTopic("sensors/data")
        .qos(1)
        .build()

    // DefaultMqttListener 어댑터를 사용하여 필요한 메서드만 구현
    val listener = object : MqttBufferedSubscriber.DefaultMqttListener() {
        override fun onConnected() {
            println("Connected")
        }

        override fun onConnectionLost(cause: Throwable) {
            println("Connection lost: ${cause.message}")
        }

        override fun onMessageReceived(topic: String, message: String) {
            println("Message received on topic '$topic': $message")
            subscriber.messageBuffer.add(topic, message)
        }
        
        // onDisconnected(), onBufferedBefore(), onBufferedAfter()는 
        // 구현하지 않아도 됨 - 기본 빈 구현 사용
    }
    
    // 하나의 리스너를 ConnectionListener와 MessageListener 모두에 등록
    subscriber.addListener(listener)
    subscriber.addMessageListener(listener)

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
