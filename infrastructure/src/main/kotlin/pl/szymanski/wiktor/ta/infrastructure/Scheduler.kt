package pl.szymanski.wiktor.ta.infrastructure

import io.github.flaxoos.ktor.server.plugins.taskscheduling.TaskScheduling
import io.github.flaxoos.ktor.server.plugins.taskscheduling.managers.lock.database.mongoDb
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.config.*
import kotlinx.coroutines.launch
import pl.szymanski.wiktor.ta.CommandBus
import pl.szymanski.wiktor.ta.EventBus
import pl.szymanski.wiktor.ta.EventEnvelope
import pl.szymanski.wiktor.ta.Metadata
import pl.szymanski.wiktor.ta.command.AccommodationCommand
import pl.szymanski.wiktor.ta.command.AttractionCommand
import pl.szymanski.wiktor.ta.command.CommuteCommand
import pl.szymanski.wiktor.ta.domain.aggregate.Accommodation
import pl.szymanski.wiktor.ta.domain.aggregate.Attraction
import pl.szymanski.wiktor.ta.domain.aggregate.Commute
import pl.szymanski.wiktor.ta.domain.event.AccommodationEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteEvent
import pl.szymanski.wiktor.ta.domain.repository.AccommodationRepository
import pl.szymanski.wiktor.ta.domain.repository.AttractionRepository
import pl.szymanski.wiktor.ta.domain.repository.CommuteRepository
import pl.szymanski.wiktor.ta.event.AccommodationDateMetEvent
import pl.szymanski.wiktor.ta.event.AttractionDateMetEvent
import pl.szymanski.wiktor.ta.event.CommuteDateMetEvent
import pl.szymanski.wiktor.ta.generator.AccommodationGenerator
import pl.szymanski.wiktor.ta.generator.AttractionGenerator
import pl.szymanski.wiktor.ta.generator.CommuteGenerator
import pl.szymanski.wiktor.ta.infrastructure.config.DatabaseProvider
import pl.szymanski.wiktor.ta.infrastructure.config.GeneratorConfig
import pl.szymanski.wiktor.ta.infrastructure.repository.DelayedEventRepository
import java.time.LocalDateTime
import java.util.UUID

fun main(args: Array<String>) {
    EngineMain
        .main(args)
}

fun Application.scheduler(
    commandBus: CommandBus,
    eventBus: EventBus,
    databaseProvider: DatabaseProvider,
    accommodationGenerator: AccommodationGenerator,
    accommodationRepository: AccommodationRepository,
    attractionRepository: AttractionRepository,
    commuteRepository: CommuteRepository,
    delayedEventRepository: DelayedEventRepository,
    attractionGenerator: AttractionGenerator,
    commuteGenerator: CommuteGenerator,
) {
    val generatorConfig = property<GeneratorConfig>("generator")

    kotlin.runCatching {
        val level = environment.config.propertyOrNull("logging.level")?.getString()?.uppercase()
        if (level != null) {
            val ctx = org.slf4j.LoggerFactory.getILoggerFactory() as ch.qos.logback.classic.LoggerContext
            val root = ctx.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME)
            root.level = ch.qos.logback.classic.Level.valueOf(level)
        }
    }.onFailure {
        log.warn("Failed to configure logging level from config", it)
    }

    log.info("Test class: ${databaseProvider.mongoConfig.uri}")

    install(TaskScheduling) {
        mongoDb {
            databaseName = databaseProvider.mongoConfig.dbName
            client = databaseProvider.mongoClient
        }

        task {
            name = "DelayedEventPublisher"
            task = {
                val now = LocalDateTime.now()
                val events = delayedEventRepository.findReadyToPublish(now)
                if (events.isNotEmpty()) {
                    log.info("Publishing ${events.size} delayed events")
                }
                events.forEach { event ->
                    try {
                        (eventBus as KafkaEventBus).publishRaw(event.topic, event.key, event.payload)
                        delayedEventRepository.markAsProcessed(event.id)
                    } catch (e: Exception) {
                        log.error("Failed to publish delayed event ${event.id}", e)
                    }
                }
            }
            kronSchedule = {
                seconds {
                    0 every 1
                }
            }
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
                        launch {
                            eventBus.publishAtGivenTime(
                                EventEnvelope(
                                    AccommodationDateMetEvent(
                                        accommodationId = entity.id,
                                    ),
                                    metadata = Metadata(UUID.randomUUID(),0)),
                                date = entity.rent.from
                            )
                        }
                    }
            }
            kronSchedule = {
                seconds {
                    0 every generatorConfig.intervalSeconds
                }
            }
        }

        task {
            name = "AttractionGenerator"
            task = { taskExecutionTime ->
                log.info("AttractionGenerator is running: $taskExecutionTime")
                attractionGenerator
                    .generate()
                    .forEach {
                        val (entity, events) = commandBus.dispatch<AttractionCommand, Attraction>(it.command)
                        attractionRepository.save(entity, events[0] as AttractionEvent)
                        launch {
                            eventBus.publishAtGivenTime(
                                EventEnvelope(
                                    AttractionDateMetEvent(
                                        attractionId = entity.id,
                                    ),
                                    metadata = Metadata(UUID.randomUUID(),0)),
                                date = entity.date
                            )
                        }
                    }
            }
            kronSchedule = {
                seconds {
                    0 every generatorConfig.intervalSeconds
                }
            }
        }

        task {
            name = "CommuteGenerator"
            task = { taskExecutionTime ->
                log.info("CommuteGenerator is running: $taskExecutionTime")
                commuteGenerator
                    .generate()
                    .forEach {
                        val (entity, events) = commandBus.dispatch<CommuteCommand, Commute>(it.command)
                        commuteRepository.save(entity, events[0] as CommuteEvent)
                        launch {
                            eventBus.publishAtGivenTime(
                                EventEnvelope(
                                    CommuteDateMetEvent(
                                        commuteId = entity.id,
                                    ),
                                    metadata = Metadata(UUID.randomUUID(),0)),
                                date = entity.departure.time
                            )
                        }
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