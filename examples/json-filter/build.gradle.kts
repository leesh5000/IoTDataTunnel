plugins {
    kotlin("jvm") version "2.1.21"
    application
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("me.helloc:iot-data-tunnel:1.0.0")
}

application {
    mainClass.set("me.helloc.example.jsonfilter.MainKt")
}
