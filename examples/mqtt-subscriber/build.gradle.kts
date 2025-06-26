plugins {
    kotlin("jvm") version "2.1.21"
    application
}

repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    implementation("com.github.leesh5000:IoTDataTunnel:1.0.3")
    implementation(kotlin("stdlib"))
}

application {
    mainClass.set("Main")
}