import me.helloc.iot.tunnel.ConnectionManager
import me.helloc.iot.tunnel.PathFilterBuilder

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

    repeat(10) {
        val pair = manager.messageBuffer.poll()
        if (pair != null) {
            val (_, message) = pair
            val value = PathFilterBuilder.from(message)
                .addValueFilter("$.sensor[0].value")
                .extractFirst(Int::class.java)
            println("Extracted value: ${'$'}value")
        }
        Thread.sleep(500)
    }

    manager.disconnect()
}
