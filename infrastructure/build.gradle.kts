val jacksonVersion = "2.18.4"
val coroutinesVersion = "1.9.0-RC"
val mockkVersion = "1.14.5"
val mongoVersion = "5.1.0"

plugins {
    kotlin("jvm") version "2.1.10"
    kotlin("plugin.serialization") version "2.1.10"
    id("com.microsoft.azure.azurefunctions") version "1.16.1"
}

group = "pl.szymanski.wiktor"
version = "0.0.1"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.fasterxml.jackson:jackson-bom:$jacksonVersion")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:$jacksonVersion")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")

    implementation("com.azure:azure-cosmos:4.73.1")
    implementation("com.microsoft.azure.functions:azure-functions-java-library:3.1.0")

    implementation(kotlin("stdlib-jdk8"))

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:1.10.2")

    implementation(project(":domain"))
    implementation(project(":application"))

    implementation("io.kurrent:kurrentdb-client:1.0.0")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    implementation("org.mongodb:bson-kotlinx:5.2.0")

    implementation("org.mongodb:mongodb-driver-kotlin-coroutine:$mongoVersion")

    testImplementation(kotlin("test"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutinesVersion")
    testImplementation("io.mockk:mockk:$mockkVersion")

    implementation("io.grpc:grpc-core:1.57.0")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}

azurefunctions {
    resourceGroup = "java-functions-group"
    appName = "azure-functions-sample-damn"
    region = "westeurope"
    pricingTier = "Consumption"
//    runtime {
//        os = "linux"
//        javaVersion = "21"
//    }
}