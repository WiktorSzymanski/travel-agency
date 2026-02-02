package pl.szymanski.wiktor.ta.infrastructure

import com.mongodb.kotlin.client.coroutine.MongoClient
import io.github.flaxoos.ktor.server.plugins.taskscheduling.TaskScheduling
import io.github.flaxoos.ktor.server.plugins.taskscheduling.managers.lock.database.mongoDb
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.cio.EngineMain
import io.ktor.server.config.property
import pl.szymanski.wiktor.ta.generator.AccommodationGenerator
import pl.szymanski.wiktor.ta.infrastructure.config.GeneratorConfig
import pl.szymanski.wiktor.ta.infrastructure.config.MongoDBConfig

fun main(args: Array<String>) {
    EngineMain
        .main(args)
}

fun Application.scheduler() {
    val mongoConfig = property<MongoDBConfig>("database")
    val generatorConfig = property<GeneratorConfig>("generator")

    val accommodationGenerator = AccommodationGenerator(
        generatorConfig.inAdvanceSeconds,
        generatorConfig.creationWindowSeconds
    )

    install(TaskScheduling) {
        mongoDb {
            databaseName = mongoConfig.dbName
            client = MongoClient.create(mongoConfig.uri)
        }

        task {
            name = "AccommodationGenerator"
            task = { taskExecutionTime ->
                log.info("AccommodationGenerator is running: $taskExecutionTime")
                accommodationGenerator
                    .generate(generatorConfig.accommodations)
                    .forEach {
                        log.info("Generated accommodation: $it")
                    }
            }
            kronSchedule = {
                seconds {
                    0 every generatorConfig.intervalSeconds
                }
            }
        }
    }
}