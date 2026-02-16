plugins {
    kotlin("jvm")
    kotlin("plugin.spring") version "2.1.10"
    id("org.springframework.boot") version "4.0.2"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "pl.szymanski.wiktor"
version = "0.0.1"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":infrastructure"))

    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
}

kotlin {
    jvmToolchain(21)
}

tasks.test {
    useJUnitPlatform()
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    archiveClassifier.set("boot")
    mainClass.set("pl.szymanski.wiktor.ta.bootstrap.TravelAgencyApplicationKt")
}

tasks.named<Jar>("jar") {
    enabled = false
}
