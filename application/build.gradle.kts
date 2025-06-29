val exposedVersion = "0.50.1"
val mokkVersion = "1.13.7"
val kotlinxCoroutinesTestVersion = "1.10.2"

plugins {
    kotlin("jvm")
}

group = "pl.szymanski.wiktor"
version = "0.0.1"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":domain"))
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    testImplementation(kotlin("test"))
    testImplementation("io.mockk:mockk:$mokkVersion")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$kotlinxCoroutinesTestVersion")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}
