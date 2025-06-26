# IoTDataTunnel

[![](https://jitpack.io/v/leesh5000/IoTDataTunnel.svg)](https://jitpack.io/#leesh5000/IoTDataTunnel)
![MIT License](https://img.shields.io/badge/license-MIT-blue.svg)

```kotlin
val subscriber = MqttBufferedSubscriber.builder()
    .brokerUrl("tcp://broker.hivemq.com:1883")
    .addTopic("sensors/data")
    .build()
subscriber.connect()
val pair = subscriber.messageBuffer.poll()
if (pair != null) {
    val (_, message) = pair
    println("received: $message")
}
```

## About

산업 현장의 불안정한 네트워크 환경에서 MQTT 보일러플레이트를 제거하고
센서 데이터 JSON 추출을 돕는 라이브러리입니다.

## Features

- MQTT 자동 연결 관리 및 실패 복구
- JSON 경로 기반 필드 추출
- 메세지 버퍼 인터페이스를 통한 다양한 저장소 지원
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

- [예제 프로젝트](#example-projects)
- [자동 배포](#자동-배포)
- [업데이트 내역](https://github.com/leesh5000/IoTDataTunnel/releases)

## Example Projects

`examples` 디렉터리에 MQTT 연동과 JSON 추출을 보여 주는 간단한 프로젝트들이 있습니다.

## 자동 배포

Main 브랜치로 머지되면 GitHub Actions 파이프라인이 실행되어 테스트 후
`build.gradle.kts`에 지정된 버전으로 태그와 릴리스를 자동 생성합니다.

## 🤝 기여하기

1. 이슈를 등록하거나 토론에 참여하세요.
2. 버그 리포트나 기능 요청을 남겨주세요.
3. Pull Request를 보내주시면 리뷰 후 병합합니다.

## 📄 라이선스

본 프로젝트는 [MIT 라이선스](LICENSE)로 배포됩니다.
