val kotlinVersion: String by project
val logbackVersion: String by project

plugins {
    kotlin("jvm") version "2.1.10"
    id("io.ktor.plugin") version "3.1.3"
    id("org.jlleitschuh.gradle.ktlint") version "13.0.0-rc.1"
    id("io.gitlab.arturbosch.detekt") version "1.23.6"
}

group = "pl.szymanski.wiktor"
version = "0.0.1"

application {
    mainClass = "io.ktor.server.cio.EngineMain"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.openfolder:kotlin-asyncapi-ktor:3.1.1")
    implementation("io.ktor:ktor-server-core-jvm")
    implementation("io.ktor:ktor-server-cio")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("io.ktor:ktor-server-core")
    implementation("io.ktor:ktor-server-config-yaml")
    testImplementation("io.ktor:ktor-server-test-host")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlinVersion")
}

subprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
    apply(plugin = "io.gitlab.arturbosch.detekt")
}

ktlint {
    version.set("1.2.1")
    verbose.set(true)
    android.set(false)
    outputToConsole.set(true)
    coloredOutput.set(true)
}

detekt {
    buildUponDefaultConfig = true
    parallel = true
}

tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    ignoreFailures = true

    reports {
        sarif {
            required.set(true)
            outputLocation.set(layout.buildDirectory.file("reports/detekt/report.sarif"))
        }
    }
}
