package pl.szymanski.wiktor.ta.infrastructure

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling
import pl.szymanski.wiktor.ta.infrastructure.config.GeneratorConfig
import pl.szymanski.wiktor.ta.infrastructure.config.MongoDBConfig

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(GeneratorConfig::class, MongoDBConfig::class)
class TravelAgencyApplication

fun main(args: Array<String>) {
    runApplication<TravelAgencyApplication>(*args)
}


