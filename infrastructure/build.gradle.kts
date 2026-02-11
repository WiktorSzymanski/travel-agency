val mongoVersion = "5.1.0"
val ktorVersion = "3.2.0"
val schedulerVersion = "2.2.1"
plugins {
    kotlin("jvm")
    kotlin("plugin.serialization") version "2.1.10"
}

group = "pl.szymanski.wiktor"
version = "0.0.1"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":application"))
    implementation("io.ktor:ktor-server-core:${ktorVersion}")
    implementation("io.ktor:ktor-server-cio:${ktorVersion}")
    implementation("io.ktor:ktor-server-config-yaml:${ktorVersion}")
    implementation("io.ktor:ktor-server-di:${ktorVersion}")

    implementation("org.mongodb:mongodb-driver-kotlin-coroutine:${mongoVersion}")
    implementation("org.mongodb:bson-kotlinx:${mongoVersion}")

    implementation("io.github.flaxoos:ktor-server-task-scheduling-mongodb:$schedulerVersion")

    // Kafka and JSON serialization
    implementation("org.apache.kafka:kafka-clients:3.7.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    testImplementation(kotlin("test"))
    testImplementation("io.mockk:mockk:1.13.10")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.1")
}

kotlin {
    jvmToolchain(21)
}

tasks.test {
    useJUnitPlatform()
}