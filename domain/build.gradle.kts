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
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.0")
    implementation("com.fasterxml.jackson.core:jackson-annotations:2.20")

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}
