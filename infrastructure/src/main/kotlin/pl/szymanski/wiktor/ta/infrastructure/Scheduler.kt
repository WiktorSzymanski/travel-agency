package pl.szymanski.wiktor.ta.infrastructure

import io.github.flaxoos.ktor.server.plugins.taskscheduling.TaskScheduling
import io.github.flaxoos.ktor.server.plugins.taskscheduling.managers.lock.database.mongoDb
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.config.*
import pl.szymanski.wiktor.ta.CommandBus
import pl.szymanski.wiktor.ta.command.AccommodationCommand
import pl.szymanski.wiktor.ta.command.AttractionCommand
import pl.szymanski.wiktor.ta.command.CommuteCommand
import pl.szymanski.wiktor.ta.domain.aggregate.Accommodation
import pl.szymanski.wiktor.ta.domain.aggregate.Attraction
import pl.szymanski.wiktor.ta.domain.aggregate.Commute
import pl.szymanski.wiktor.ta.domain.event.AccommodationEvent
import pl.szymanski.wiktor.ta.domain.repository.AccommodationRepository
import pl.szymanski.wiktor.ta.generator.AccommodationGenerator
import pl.szymanski.wiktor.ta.generator.AttractionGenerator
import pl.szymanski.wiktor.ta.generator.CommuteGenerator
import pl.szymanski.wiktor.ta.infrastructure.config.DatabaseProvider
import pl.szymanski.wiktor.ta.infrastructure.config.GeneratorConfig

fun main(args: Array<String>) {
    EngineMain
        .main(args)
}

fun Application.scheduler(
    commandBus: CommandBus,
    databaseProvider: DatabaseProvider,
    accommodationGenerator: AccommodationGenerator,
    accommodationRepository: AccommodationRepository,
//    attractionGenerator: AttractionGenerator,
//    commuteGenerator: CommuteGenerator,
) {
    val generatorConfig = property<GeneratorConfig>("generator")

    log.info("Test class: ${databaseProvider.mongoConfig.uri}")

    install(TaskScheduling) {
        mongoDb {
            databaseName = databaseProvider.mongoConfig.dbName
            client = databaseProvider.mongoClient
        }

        task {
            name = "AccommodationGenerator"
            task = { taskExecutionTime ->
                log.info("AccommodationGenerator is running: $taskExecutionTime")
                accommodationGenerator
                    .generate()
                    .forEach {
                        val (entity, events) = commandBus.dispatch<AccommodationCommand, Accommodation>(it.command)
                        accommodationRepository.save(entity, events[0] as AccommodationEvent)
                    }
            }
            kronSchedule = {
                seconds {
                    0 every generatorConfig.intervalSeconds
                }
            }
        }

//        task {
//            name = "AttractionGenerator"
//            task = { taskExecutionTime ->
//                log.info("AttractionGenerator is running: $taskExecutionTime")
//                attractionGenerator
//                    .generate()
//                    .forEach {
//                        commandBus.dispatch<AttractionCommand, Attraction>(it.command)
//                    }
//            }
//            kronSchedule = {
//                seconds {
//                    0 every generatorConfig.intervalSeconds
//                }
//            }
//        }
//
//        task {
//            name = "CommuteGenerator"
//            task = { taskExecutionTime ->
//                log.info("CommuteGenerator is running: $taskExecutionTime")
//                commuteGenerator
//                    .generate()
//                    .forEach {
//                        commandBus.dispatch<CommuteCommand, Commute>(it.command)
//                    }
//            }
//            kronSchedule = {
//                seconds {
//                    0 every generatorConfig.intervalSeconds
//                }
//            }
//        }
    }
}