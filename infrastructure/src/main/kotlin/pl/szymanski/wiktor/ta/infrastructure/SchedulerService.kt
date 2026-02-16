package pl.szymanski.wiktor.ta.infrastructure

import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
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
import pl.szymanski.wiktor.ta.repository.AccommodationRepository
import pl.szymanski.wiktor.ta.repository.AttractionRepository
import pl.szymanski.wiktor.ta.repository.CommuteRepository
import pl.szymanski.wiktor.ta.event.AccommodationDateMetEvent
import pl.szymanski.wiktor.ta.event.AttractionDateMetEvent
import pl.szymanski.wiktor.ta.event.CommuteDateMetEvent
import pl.szymanski.wiktor.ta.generator.AccommodationGenerator
import pl.szymanski.wiktor.ta.generator.AttractionGenerator
import pl.szymanski.wiktor.ta.generator.CommuteGenerator
import pl.szymanski.wiktor.ta.infrastructure.repository.DelayedEventRepository
import java.time.LocalDateTime
import java.util.UUID

@Component
class SchedulerService(
    private val commandBus: CommandBus,
    private val eventBus: EventBus,
    private val accommodationGenerator: AccommodationGenerator,
    private val accommodationRepository: AccommodationRepository,
    private val attractionRepository: AttractionRepository,
    private val commuteRepository: CommuteRepository,
    private val delayedEventRepository: DelayedEventRepository,
    private val attractionGenerator: AttractionGenerator,
    private val commuteGenerator: CommuteGenerator,
) {
    private val log = LoggerFactory.getLogger(SchedulerService::class.java)

    @Scheduled(fixedDelay = 1000) // Run every 1 second
    fun publishDelayedEvents() = runBlocking {
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

    @Scheduled(fixedDelayString = "\${generator.interval-seconds:10}000") // Convert seconds to milliseconds
    fun generateAccommodations() = runBlocking {
        log.info("AccommodationGenerator is running: ${LocalDateTime.now()}")
        accommodationGenerator
            .generate()
            .forEach {
                val (entity, events) = commandBus.dispatch<AccommodationCommand, Accommodation>(it.command)
                accommodationRepository.create(entity, events[0] as AccommodationEvent)

                eventBus.publish(
                    EventEnvelope(
                        events[0],
                        Metadata(UUID.randomUUID(), 0)
                    )
                )
                eventBus.publishAtGivenTime(
                    EventEnvelope(
                        AccommodationDateMetEvent(
                            accommodationId = entity.id,
                        ),
                        metadata = Metadata(UUID.randomUUID(), 0)
                    ),
                    date = entity.rent.from
                )
            }
    }

    @Scheduled(fixedDelayString = "\${generator.interval-seconds:10}000")
    fun generateAttractions() = runBlocking {
        log.info("AttractionGenerator is running: ${LocalDateTime.now()}")
        attractionGenerator
            .generate()
            .forEach {
                val (entity, events) = commandBus.dispatch<AttractionCommand, Attraction>(it.command)
                attractionRepository.create(entity, events[0] as AttractionEvent)

                eventBus.publish(
                    EventEnvelope(
                        events[0],
                        Metadata(UUID.randomUUID(), 0)
                    )
                )
                eventBus.publishAtGivenTime(
                    EventEnvelope(
                        AttractionDateMetEvent(
                            attractionId = entity.id,
                        ),
                        metadata = Metadata(UUID.randomUUID(), 0)
                    ),
                    date = entity.date
                )
            }
    }

    @Scheduled(fixedDelayString = "\${generator.interval-seconds:10}000")
    fun generateCommutes() = runBlocking {
        log.info("CommuteGenerator is running: ${LocalDateTime.now()}")
        commuteGenerator
            .generate()
            .forEach {
                val (entity, events) = commandBus.dispatch<CommuteCommand, Commute>(it.command)
                commuteRepository.create(entity, events[0] as CommuteEvent)

                eventBus.publish(
                    EventEnvelope(
                        events[0],
                        Metadata(UUID.randomUUID(), 0)
                    )
                )
                eventBus.publishAtGivenTime(
                    EventEnvelope(
                        CommuteDateMetEvent(
                            commuteId = entity.id,
                        ),
                        metadata = Metadata(UUID.randomUUID(), 0)
                    ),
                    date = entity.departure.time
                )
            }
    }
}

