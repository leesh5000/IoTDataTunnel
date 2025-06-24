# 🚀 IoTDataTunnel

> **경량 Java 라이브러리**로, 저전력·저대역폭 IoT 환경에서 MQTT 연동과 센서 데이터 JSON 추출을 간편화합니다.

[![Maven Central](https://img.shields.io/maven-central/v/me.helloc/iot-data-tunnel)](https://search.maven.org/artifact/me.helloc/iot-data-tunnel)
[![MIT License](https://img.shields.io/badge/license-MIT-blue)](LICENSE)

---

## 🌐 배경

산업 현장, 설비 등 네트워크 품질이 불안정하고 센서 배터리 수명이 중요한 환경에서는 주로 [MQTT 프로토콜](https://mqtt.org/)을 사용하여 센서에서 발생하는 데이터를 수집합니다. 이 경우, 다음과 같은 BoilerPlate 코드가 발생하게 됩니다.

* 브로커 연결 관리
* 장애 발생 시 자동 재연결 및 토픽 재구독
* 복잡한 센서 JSON 메시지에서 필요한 필드 파싱

**IoTDataTunnel**은 이 보일러플레이트를 제거하고, 센서 데이터에 특화된 JSON 추출 API를 제공하여 비즈니스 로직 구현에 집중할 수 있도록 돕습니다.

## ⚙️ 문제 정의

**1. 브로커 연결 관리**

* URI, 클라이언트 ID, 인증 등 설정이 매 프로젝트마다 반복

**2. Failover(페일오버) 처리**

* 브로커 장애 시 재연결 로직·백오프 알고리즘 구현 필요

**3. JSON 데이터 추출**

* 중첩된 센서 데이터에서 특정 값을 뽑아오기 위한 파싱 코드가 복잡

## ✨ 주요 기능

* **자동 연결 관리**: `TunnelConfig` 빌더 또는 애너테이션으로 간단 설정
* **Failover 지원**: 지수 백오프 기반 자동 재연결 및 토픽 재구독
* **JSON 필드 추출**: JSONPath 기반 `PathFilterBuilder`로 손쉬운 값 조회
* **임시 메시지 버퍼**: MQTT로 수신한 메시지를 버퍼에 저장(인메모리/Redis/Kafka 지원)

## 📦 빌드

Gradle Wrapper가 포함되어 있으므로 다음 명령어로 프로젝트를 빌드할 수 있습니다.

```bash
./gradlew build
```

## 🛠️ 설치

**Maven**

```xml
<dependency>
  <groupId>me.helloc</groupId>
  <artifactId>iot-data-tunnel</artifactId>
  <version>1.0.0</version>
</dependency>
```

**Gradle**

```groovy
dependencies {
  implementation 'me.helloc:iot-data-tunnel:1.0.0'
}
```

## 🚀 사용 예시

### MQTT 연결 설정

```kotlin
val config = TunnelConfig.builder()
    .brokerUrl("tcp://broker.hivemq.com:1883")
    .clientId("my-client-id")
    .topic("sensors/data")
    .build()

val tunnel = IoTDataTunnel(config)
tunnel.connect()
```

### 연결 및 구독

```kotlin
tunnel.subscribe { topic, message ->
    // message: JSON 문자열
}
```
### ConnectionManager 사용

기존 `IoTDataTunnel` 을 그대로 사용할 수도 있지만, MQTT 연결만 필요할 경우 `ConnectionManager` 클래스를 활용할 수 있습니다.

```kotlin
val manager = ConnectionManager.builder()
    .brokerUrl("tcp://broker.hivemq.com:1883")
    .addTopic("sensors/data")
    .build()

manager.addListener(object : ConnectionManager.ConnectionListener {
    override fun onConnected() {
        println("connected")
    }

    override fun onConnectionLost(cause: Throwable) {
        println("lost: ${'$'}{cause.message}")
    }

    override fun onDisconnected() {
        println("disconnected")
    }
})

manager.connect()

// 수신된 메시지는 messageBuffer 에 임시 저장됩니다.
val buffered = manager.messageBuffer.poll()
```

### 메시지 버퍼 설정

`application.yml` 파일에서 `iotdatatunnel.buffer.type` 을 지정하면 버퍼 구현체를 바꿀 수 있습니다.
예를 들어 Redis 버퍼를 사용하려면 다음과 같이 설정합니다.

```yaml
iotdatatunnel:
  buffer:
    type: redis
    host: localhost
    port: 6379
```

타입으로 `inmemory`, `redis`, `kafka` 를 지원하며 기본값은 `inmemory` 입니다.

### JSON 추출

다음 예시는 `id=1`, `gateways[0].id=1`, `companyCode="0012"` 조건을 만족하는 메시지에서
`0번째 sensor.value` 를 추출합니다.

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
val tempValue = PathFilterBuilder.from(message)
    .addPathFilter("$.id", 1)
    .addPathFilter("$.gateways[0].id", 1)
    .addPathFilter("$.companyCode", "0012")
    .addValueFilter("$.sensor[0].value")
    .extractFirst(Int::class.java)
println("추출된 온도: $tempValue")
```


## 💡 예제 프로젝트

`examples` 디렉터리에는 라이브러리 사용 방법을 보여 주는 간단한 데모 프로젝트가 포함되어 있습니다.

- **mqtt-subscriber**: `ConnectionManager` 로 MQTT 브로커에 연결하여 메시지를 수신합니다.
- **json-filter**: `PathFilterBuilder` 를 이용해 JSON 메시지에서 값을 추출하는 방법을 보여 줍니다.
- **pipeline-demo**: MQTT 연결 후 메시지를 버퍼에 저장하고 `PathFilterBuilder` 로 필요한 값을 추출하는 과정을 보여 줍니다.

## 🤝 기여하기

1. 이슈(issue)를 등록하거나 토론에 참여하세요.
2. 기능 요청 또는 버그 리포트를 남겨주세요.
3. 풀 리퀘스트(PR)를 보내주시면 리뷰 후 머지합니다.

## 📄 라이선스

본 프로젝트는 [MIT 라이선스](LICENSE) 하에 배포됩니다.
