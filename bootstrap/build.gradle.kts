val coroutinesVersion = "1.9.0-RC"
val ktorVersion = "3.2.0"
val logbackVersion = "1.4.14"
val mongoVersion = "5.1.0"


plugins {
    kotlin("jvm")
    id("io.ktor.plugin") version "3.2.1"
}

group = "pl.szymanski.wiktor"
version = "0.0.1"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${coroutinesVersion}")
    implementation("io.ktor:ktor-server-openapi")
    implementation("io.ktor:ktor-server-swagger")
    implementation("io.ktor:ktor-server-core:${ktorVersion}")
    implementation("io.ktor:ktor-server-cio:${ktorVersion}")
    implementation("io.ktor:ktor-server-config-yaml:${ktorVersion}")
    implementation("io.ktor:ktor-server-content-negotiation:${ktorVersion}")
    implementation("io.ktor:ktor-serialization-kotlinx-json:${ktorVersion}")
    implementation("org.openfolder:kotlin-asyncapi-ktor:3.1.1")
    implementation("ch.qos.logback:logback-classic:${logbackVersion}")
    implementation("org.mongodb:mongodb-driver-kotlin-coroutine:${mongoVersion}")
    implementation(project(":domain"))
    implementation(project(":application"))
    implementation(project(":infrastructure"))
    implementation(project(":presentation"))

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}