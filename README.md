# IoTDataTunnel

[![](https://jitpack.io/v/leesh5000/IoTDataTunnel.svg)](https://jitpack.io/#leesh5000/IoTDataTunnel)
![MIT License](https://img.shields.io/badge/license-MIT-blue.svg)

```json
{
  "id":1,
  "gateways":[{"id":1},{"id":2}],
  "companyCode":"0012",
  "sensor":[
    {"type":"temp","value":39},
    {"type":"airFlow","value":12},
    {"type":"humidity","value":57}
  ]
}
```

```kotlin
val subscriber = MqttBufferedSubscriber.builder()
    .brokerUrl("tcp://broker.hivemq.com:1883")
    .addTopic("sensors/data")
    .build()
subscriber.connect()
val pair = subscriber.messageBuffer.poll()
if (pair != null) {
    val (_, message) = pair
    val value = PathFilterBuilder.from(message)
        .addPathFilter("$.id", 1)
        .addPathFilter("$.sensor[0].type", "temp")
        .addValueFilter("$.sensor[0].value")
        .extractFirst(Int::class.java)
    println("Extracted value: $value") // Extracted value: 39
}
```

## About

산업 현장의 불안정한 네트워크 환경에서 MQTT 보일러플레이트를 제거하고
센서 데이터에 특화된 JSON 추출 API를 제공하는 라이브러리 입니다.

## Features

- MQTT 자동 연결 관리 및 실패 복구
- JSON 경로 기반 필드 추출
- 메세지 버퍼 인터페이스 지원
- 세밀한 MQTT 설정

## Installation

### Maven

1. Add to pom.xml

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>
```

2. Add the dependency

```xml
<dependency>
  <groupId>me.helloc</groupId>
  <artifactId>iot-data-tunnel</artifactId>
  <version>1.0.3</version>
</dependency>
```

### Gradle

1. Add it in your root settings.gradle at the end of repositories

```groovy
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}
```

Step 2. Add the dependency

```groovy
dependencies {
        implementation 'com.github.leesh5000:IoTDataTunnel:Tag'
}
```

## Documentation

- [예제 프로젝트](#예제-프로젝트)
- [자동 배포](#자동-배포)
- [업데이트 내역](#업데이트-내역)
- [API References](#API-References)

## 예제 프로젝트

`examples` 디렉터리에 본 라이브러리의 사용법을 보여주는 간단한 프로젝트들이 있습니다.

## 자동 배포

Main 브랜치로 머지되면 GitHub Actions 파이프라인이 실행되어 테스트 후
`build.gradle.kts`에 지정된 버전으로 태그와 릴리스를 자동 생성합니다.

## 업데이트 내역

업데이트 내역은 [Release 페이지](https://github.com/leesh5000/IoTDataTunnel/releases)에서 확인할 수 있습니다.

## API References

- [MqttBufferedSubscriber](#MqttBufferedSubscriber)
- [MessageBuffer](#MessageBuffer)
- [PathFilterBuilder](#PathFilterBuilder)
- [SimpleJsonParser](#SimpleJsonParser)

### MqttBufferedSubscriber

MQTT 클라이언트를 관리하고 자동 재연결 및 구독 복원 기능을 제공합니다.

#### 생성자

```kotlin
// 직접 생성자 호출 대신 빌더 패턴을 사용합니다
val subscriber = MqttBufferedSubscriber.builder()
    .brokerUrl("tcp://broker.hivemq.com:1883")
    .addTopic("sensors/data")
    .build()
```

#### 사용 예시

##### 기본 사용법
```kotlin
// MQTT 클라이언트 생성 및 설정
val subscriber = MqttBufferedSubscriber.builder()
    .brokerUrl("tcp://broker.hivemq.com:1883")
    .addTopic("sensors/data")
    .build()

// 브로커에 연결
subscriber.connect()

// 메시지 수신 및 처리
val pair = subscriber.messageBuffer.poll()
if (pair != null) {
    val (topic, message) = pair
    println("Received message on topic '$topic': $message")
}

// 연결 종료
subscriber.disconnect()
```

##### 리스너 등록 및 고급 설정
```kotlin
// 고급 설정으로 MQTT 클라이언트 생성
val subscriber = MqttBufferedSubscriber.builder()
    .brokerUrl("tcp://broker.hivemq.com:1883")
    .clientId("my-client-id")  // 클라이언트 ID 지정
    .addTopic("sensors/data")
    .addTopic("sensors/control")  // 여러 토픽 구독
    .initialDelay(1000)  // 재연결 초기 지연 시간(ms)
    .maxDelay(60000)     // 재연결 최대 지연 시간(ms)
    .qos(1)              // QoS 레벨 설정
    .build()

// 연결 상태 및 메시지 수신 리스너 등록
subscriber.addListener(object : MqttBufferedSubscriber.ConnectionListener, MqttBufferedSubscriber.MessageListener {
    // 연결 상태 변경 이벤트 처리
    override fun onConnected() {
        println("Connected to MQTT broker")
    }

    override fun onConnectionLost(cause: Throwable) {
        println("Connection lost: ${cause.message}")
    }

    override fun onDisconnected() {
        println("Disconnected from MQTT broker")
    }

    // 메시지 수신 이벤트 처리
    override fun onMessageReceived(topic: String, message: String) {
        println("Message received on topic '$topic': $message")
    }

    override fun onBufferedBefore(topic: String, message: String) {
        println("Message about to be buffered on topic '$topic'")
    }

    override fun onBufferedAfter(topic: String, message: String) {
        println("Message buffered on topic '$topic'")
    }
})

// 브로커에 연결
subscriber.connect()
```

#### 메서드

| 메서드                                                | 설명                                     |
|----------------------------------------------------|----------------------------------------|
| `connect()`                                        | MQTT 브로커에 연결합니다. 연결이 실패하면 자동으로 재시도합니다. |
| `disconnect()`                                     | MQTT 브로커와의 연결을 종료합니다.                  |
| `addListener(listener: ConnectionListener)`        | 연결 상태 변경 이벤트를 수신할 리스너를 추가합니다.          |
| `removeListener(listener: ConnectionListener)`     | 연결 상태 변경 이벤트 리스너를 제거합니다.               |
| `addMessageListener(listener: MessageListener)`    | 메시지 수신 이벤트를 수신할 리스너를 추가합니다.            |
| `removeMessageListener(listener: MessageListener)` | 메시지 수신 이벤트 리스너를 제거합니다.                 |

#### 인터페이스

##### ConnectionListener

```kotlin
interface ConnectionListener {
    fun onConnected()
    fun onConnectionLost(cause: Throwable)
    fun onDisconnected()
}
```

##### MessageListener

```kotlin
interface MessageListener {
    fun onMessageReceived(topic: String, message: String)
    fun onBufferedBefore(topic: String, message: String)
    fun onBufferedAfter(topic: String, message: String)
}
```

#### Builder 클래스

| 메서드                                              | 설명                                  | 기본값                   |
|--------------------------------------------------|-------------------------------------|-----------------------|
| `brokerUrl(url: String)`                         | MQTT 브로커 URL을 설정합니다.                | 필수                    |
| `clientId(id: String)`                           | MQTT 클라이언트 ID를 설정합니다.               | 자동 생성                 |
| `addTopic(topic: String)`                        | 구독할 토픽을 추가합니다.                      | 없음                    |
| `options(options: MqttConnectOptions)`           | MQTT 연결 옵션을 설정합니다.                  | 기본 옵션                 |
| `scheduler(scheduler: ScheduledExecutorService)` | 재연결 스케줄러를 설정합니다.                    | 단일 스레드 스케줄러           |
| `initialDelay(delay: Long)`                      | 초기 재연결 지연 시간(ms)을 설정합니다.            | 1000                  |
| `maxDelay(delay: Long)`                          | 최대 재연결 지연 시간(ms)을 설정합니다.            | 60000                 |
| `messageBuffer(buffer: MessageBuffer)`           | 메시지 버퍼를 설정합니다.                      | InMemoryMessageBuffer |
| `qos(qos: Int)`                                  | MQTT QoS 레벨을 설정합니다(0-2).            | 1                     |
| `build()`                                        | MqttBufferedSubscriber 인스턴스를 생성합니다. | -                     |

### MessageBuffer

MQTT를 통해 전달된 메시지를 저장하는 버퍼 인터페이스입니다. 기본으로 InMemoryMessageBuffer가 제공되며, 인터페이스를 구현하여 Kafka, Redis 등 다른 저장소에 저장할 수 있습니다.

#### 메서드

| 메서드                                   | 설명                                                     |
|---------------------------------------|--------------------------------------------------------|
| `add(topic: String, message: String)` | 지정된 토픽에 대한 메시지를 버퍼에 추가합니다.                             |
| `poll(): Pair<String, String>?`       | 가장 오래된 버퍼링된 메시지를 검색하고 제거합니다. 버퍼가 비어 있으면 `null`을 반환합니다. |

#### 구현체

##### InMemoryMessageBuffer

메모리 내 메시지 버퍼 구현체입니다. ConcurrentLinkedQueue를 사용하여 스레드 안전성을 보장합니다.

```kotlin
val buffer = InMemoryMessageBuffer()
```

#### 사용 예시

##### 독립적인 버퍼 사용
```kotlin
// 메모리 내 메시지 버퍼 생성
val buffer = InMemoryMessageBuffer()

// 메시지 추가
buffer.add("sensors/temperature", """{"value": 25.5, "unit": "celsius"}""")
buffer.add("sensors/humidity", """{"value": 60, "unit": "percent"}""")

// 메시지 처리
while (true) {
    val pair = buffer.poll()
    if (pair == null) {
        println("버퍼가 비어 있습니다.")
        break
    }

    val (topic, message) = pair
    println("토픽: $topic, 메시지: $message")
}
```

##### MqttBufferedSubscriber와 함께 사용
```kotlin
// MQTT 클라이언트 생성 및 설정
val buffer = InMemoryMessageBuffer()
val subscriber = MqttBufferedSubscriber.builder()
    .brokerUrl("tcp://broker.hivemq.com:1883")
    .addTopic("sensors/+")  // 와일드카드를 사용한 토픽 구독
    .messageBuffer(buffer)  // 사용자 정의 버퍼 설정
    .build()

// 브로커에 연결
subscriber.connect()

// 백그라운드 스레드에서 메시지 처리
Thread {
    while (true) {
        val pair = buffer.poll()
        if (pair != null) {
            val (topic, message) = pair
            println("처리 중인 메시지: $topic - $message")
        }
        Thread.sleep(100)  // CPU 사용량 감소를 위한 짧은 대기
    }
}.start()
```

### PathFilterBuilder

JSON 메시지를 경로 표현식을 사용하여 필터링하고 값을 추출합니다.

#### 생성자

```kotlin
val builder = PathFilterBuilder.from(jsonMessage)
```

#### 메서드

| 메서드                                               | 설명                                            |
|---------------------------------------------------|-----------------------------------------------|
| `addPathFilter(path: String, expectedValue: Any)` | 지정된 경로의 값이 예상 값과 일치하는지 확인하는 필터를 추가합니다.        |
| `addValueFilter(path: String)`                    | 지정된 경로에서 값을 추출하기 위한 필터를 추가합니다.                |
| `extractFirst(clazz: Class<T>): T?`               | 모든 경로 필터가 일치할 때 첫 번째 값 필터에서 지정된 타입의 값을 추출합니다. |

#### 경로 표현식 구문

| 표현식                          | 설명              | 예시                  |
|------------------------------|-----------------|---------------------|
| `$`                          | 루트 객체           | `$`                 |
| `$.property`                 | 객체의 속성          | `$.id`              |
| `$[index]`                   | 배열의 인덱스         | `$[0]`              |
| `$.property[index]`          | 객체 속성 내 배열의 인덱스 | `$.sensor[0]`       |
| `$.property[index].property` | 배열 요소의 속성       | `$.sensor[0].value` |

#### 사용 예시

##### 기본 값 추출
```kotlin
val jsonMessage = """{
  "id": 1,
  "sensor": [
    {"type": "temperature", "value": 25.5},
    {"type": "humidity", "value": 60}
  ]
}"""

// 단순 값 추출
val temperature = PathFilterBuilder.from(jsonMessage)
    .addValueFilter("$.sensor[0].value")
    .extractFirst(Double::class.java)

println("온도: $temperature") // 온도: 25.5
```

##### 조건부 필터링 및 값 추출
```kotlin
val jsonMessage = """{
  "id": 1,
  "gateways": [{"id": 1}, {"id": 2}],
  "companyCode": "0012",
  "sensor": [
    {"type": "temp", "value": 39},
    {"type": "airFlow", "value": 12},
    {"type": "humidity", "value": 57}
  ]
}"""

// 조건부 필터링 및 값 추출
val humidityValue = PathFilterBuilder.from(jsonMessage)
    .addPathFilter("$.id", 1)                  // id가 1인 경우만 필터링
    .addPathFilter("$.companyCode", "0012")    // companyCode가 "0012"인 경우만 필터링
    .addPathFilter("$.sensor[2].type", "humidity") // 세 번째 센서가 습도 센서인 경우만 필터링
    .addValueFilter("$.sensor[2].value")       // 세 번째 센서의 값 추출
    .extractFirst(Int::class.java)

println("습도: $humidityValue") // 습도: 57
```

##### 다양한 타입 처리
```kotlin
val jsonMessage = """{
  "device": {
    "id": "dev-001",
    "active": true,
    "config": {
      "updateInterval": 60,
      "threshold": 10.5
    }
  }
}"""

// 문자열 값 추출
val deviceId = PathFilterBuilder.from(jsonMessage)
    .addValueFilter("$.device.id")
    .extractFirst(String::class.java)

// 불리언 값 추출
val isActive = PathFilterBuilder.from(jsonMessage)
    .addValueFilter("$.device.active")
    .extractFirst(Boolean::class.java)

// 정수 값 추출
val updateInterval = PathFilterBuilder.from(jsonMessage)
    .addValueFilter("$.device.config.updateInterval")
    .extractFirst(Int::class.java)

// 실수 값 추출
val threshold = PathFilterBuilder.from(jsonMessage)
    .addValueFilter("$.device.config.threshold")
    .extractFirst(Double::class.java)

println("장치 ID: $deviceId, 활성 상태: $isActive")
println("업데이트 간격: $updateInterval초, 임계값: $threshold")
```

### SimpleJsonParser

JSON을 파싱하여 Kotlin 타입으로 변환하는 내부 클래스입니다. 객체는 Map으로, 배열은 List로, 문자열, 숫자, 불리언 및 null 값을 지원합니다.

> 이 클래스는 내부적으로 사용되며, 일반적으로 직접 사용하지 않습니다. 대신 `PathFilterBuilder`를 통해 JSON 메시지를 처리합니다.

## 🤝 기여하기

1. 이슈를 등록하거나 토론에 참여하세요.
2. 버그 리포트나 기능 요청을 남겨주세요.
3. Pull Request를 보내주시면 리뷰 후 병합합니다.

## 📄 라이선스

본 프로젝트는 [MIT 라이선스](LICENSE)로 배포됩니다.
