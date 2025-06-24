package me.helloc.iot.tunnel

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class MessageBufferFactoryTest : StringSpec({
    "creates in-memory buffer" {
        val buffer = MessageBufferFactory.fromConfig("buffer-inmemory.yml")
        buffer.shouldBeInstanceOf<InMemoryMessageBuffer>()
    }

    "creates redis buffer with host and port" {
        val buffer = MessageBufferFactory.fromConfig("buffer-redis.yml")
        buffer.shouldBeInstanceOf<RedisMessageBuffer>()
        buffer as RedisMessageBuffer
        buffer.host shouldBe "localhost"
        buffer.port shouldBe 1234
    }

    "creates kafka buffer" {
        val buffer = MessageBufferFactory.fromConfig("buffer-kafka.yml")
        buffer.shouldBeInstanceOf<KafkaMessageBuffer>()
        buffer as KafkaMessageBuffer
        buffer.host shouldBe "kserver"
        buffer.port shouldBe 9092
    }
})
