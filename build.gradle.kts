import org.jlleitschuh.gradle.ktlint.reporter.ReporterType

val kotlinVersion: String by project
val logbackVersion: String by project
val ktlintVersion: String by project

plugins {
    kotlin("jvm") version "2.1.10"
    id("org.jlleitschuh.gradle.ktlint") version "13.0.0"
    id("io.gitlab.arturbosch.detekt") version "1.23.6"
    id("org.jetbrains.kotlinx.kover") version "0.9.3"
}

group = "pl.szymanski.wiktor"
version = "0.0.1"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlinVersion")
    ktlint("com.pinterest.ktlint:ktlint-cli:$ktlintVersion")
}

subprojects {
    apply(plugin = "kotlin")
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
    apply(plugin = "io.gitlab.arturbosch.detekt")
    apply(plugin = "org.jetbrains.kotlinx.kover")

    ktlint {
        version.set("1.5.0")
        verbose.set(true)
        android.set(false)
        outputToConsole.set(true)
        reporters {
            reporter(ReporterType.PLAIN)
        }
    }

    detekt {
        config.setFrom("${project.rootDir}/detekt.yaml")
        buildUponDefaultConfig = true
        parallel = true
    }

    tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
        ignoreFailures = true
        jvmTarget = "21"
        reports {
            sarif {
                required.set(true)
                outputLocation.set(layout.buildDirectory.file("reports/detekt/report.sarif"))
            }
        }
    }
}
