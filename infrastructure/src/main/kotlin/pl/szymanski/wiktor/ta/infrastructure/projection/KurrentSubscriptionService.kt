package pl.szymanski.wiktor.ta.infrastructure.projection

import io.kurrent.dbclient.*
import tools.jackson.databind.ObjectMapper
import jakarta.annotation.PostConstruct
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import pl.szymanski.wiktor.ta.Metadata
import pl.szymanski.wiktor.ta.EventEnvelope
import pl.szymanski.wiktor.ta.saga.SagaService
import pl.szymanski.wiktor.ta.commands.booking.complete.CompleteBookingCommandHandler
import pl.szymanski.wiktor.ta.commands.booking.fail.FailBookingCommandHandler
import pl.szymanski.wiktor.ta.commands.booking.cancel.CancelBookingCommandHandler
import pl.szymanski.wiktor.ta.commands.booking.failCancel.FailCancelBookingCommandHandler
import pl.szymanski.wiktor.ta.domain.event.*
import pl.szymanski.wiktor.ta.event.*
import pl.szymanski.wiktor.ta.eventHandlerLogic.*
import java.util.UUID

@Service
class KurrentSubscriptionService(
    private val client: KurrentDBClient,
    private val objectMapper: ObjectMapper,
    private val projectionHandlers: List<ProjectionHandler>,
    private val sagaService: SagaService,
    private val completeBookingCommandHandler: CompleteBookingCommandHandler,
    private val failBookingCommandHandler: FailBookingCommandHandler,
    private val cancelBookingCommandHandler: CancelBookingCommandHandler,
    private val failCancelBookingCommandHandler: FailCancelBookingCommandHandler,
    private val expireCommuteCommandHandler: pl.szymanski.wiktor.ta.commands.commute.expire.ExpireCommuteCommandHandler,
    private val expireAccommodationCommandHandler: pl.szymanski.wiktor.ta.commands.accommodation.expire.ExpireAccommodationCommandHandler,
    private val expireAttractionCommandHandler: pl.szymanski.wiktor.ta.commands.attraction.expire.ExpireAttractionCommandHandler,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @PostConstruct
    fun startSubscription() {
        client.subscribeToAll(object : SubscriptionListener() {
            override fun onEvent(subscription: Subscription, event: ResolvedEvent) {
                if (event.event.eventType.startsWith("$")) return
                
                runBlocking {
                    try {
                        handleEvent(event)
                    } catch (e: Exception) {
                        log.error("Error handling event ${event.event.eventType}: ${e.message}", e)
                    }
                }
            }
        }, SubscribeToAllOptions.get().fromStart())
    }

    private suspend fun handleEvent(resolvedEvent: ResolvedEvent) {
        val streamName = resolvedEvent.event.streamId
        val metadata = parseMetadata(resolvedEvent)

        // 1. Projections
        projectionHandlers.filter { it.canHandle(streamName) }.forEach { it.handle(resolvedEvent, metadata) }

        // 2. Saga Dispatching (Replacing Kafka)
        dispatchToSaga(resolvedEvent, metadata)
    }

    private suspend fun dispatchToSaga(resolvedEvent: ResolvedEvent, metadataArg: Metadata) {
        val eventType = resolvedEvent.event.eventType
        val data = resolvedEvent.event.eventData as ByteArray
        
        // This part replicates what the Kafka handlers were doing
        when (eventType) {
            "BookingCreatedEvent" -> {
                val event = objectMapper.readValue(data, BookingCreatedEvent::class.java)
                onCreatedEvent(sagaService, EventEnvelope(event, metadataArg))
            }
            "BookingCancelRequestedEvent" -> {
                val event = objectMapper.readValue(data, BookingCancelRequestedEvent::class.java)
                onCancelRequestedEvent(sagaService, EventEnvelope(event, metadataArg))
            }
            "BookingSagaCompletedEvent" -> {
                val event = objectMapper.readValue(data, BookingSagaCompletedEvent::class.java)
                onBookingSagaCompletedEvent(completeBookingCommandHandler, EventEnvelope(event, metadataArg))
            }
            "BookingSagaFailedEvent" -> {
                val event = objectMapper.readValue(data, BookingSagaFailedEvent::class.java)
                onBookingSagaFailedEvent(failBookingCommandHandler, EventEnvelope(event, metadataArg))
            }
            "BookingCancelSagaCompletedEvent" -> {
                val event = objectMapper.readValue(data, BookingCancelSagaCompletedEvent::class.java)
                onBookingCancelSagaCompletedEvent(cancelBookingCommandHandler, EventEnvelope(event, metadataArg))
            }
            "BookingCancelSagaFailedEvent" -> {
                val event = objectMapper.readValue(data, BookingCancelSagaFailedEvent::class.java)
                onBookingCancelSagaFailedEvent(failCancelBookingCommandHandler, EventEnvelope(event, metadataArg))
            }
            "AccommodationDateMetEvent" -> {
                val event = objectMapper.readValue(data, AccommodationDateMetEvent::class.java)
                onAccommodationDateMetEvent(expireAccommodationCommandHandler, EventEnvelope(event, metadataArg))
            }
            "CommuteDateMetEvent" -> {
                val event = objectMapper.readValue(data, CommuteDateMetEvent::class.java)
                onCommuteDateMetEvent(expireCommuteCommandHandler, EventEnvelope(event, metadataArg))
            }
            "AttractionDateMetEvent" -> {
                val event = objectMapper.readValue(data, AttractionDateMetEvent::class.java)
                onAttractionDateMetEvent(expireAttractionCommandHandler, EventEnvelope(event, metadataArg))
            }
        }
    }

    private fun parseMetadata(resolvedEvent: ResolvedEvent): Metadata {
        val userMetadata = resolvedEvent.event.userMetadata as? ByteArray
        val fallback = Metadata(
            correlationId = resolvedEvent.event.eventId,
            revision = resolvedEvent.originalEvent.revision + 1,
        )

        if (userMetadata == null || userMetadata.isEmpty()) {
            return fallback
        }

        return try {
            objectMapper.readValue(userMetadata, Metadata::class.java)
        } catch (_: Exception) {
            try {
                val tree = objectMapper.readTree(userMetadata)
                val correlationId = tree.get("correlationId")?.toString()?.trim('"')?.let { raw ->
                    runCatching { UUID.fromString(raw) }.getOrNull()
                } ?: fallback.correlationId

                val revision = tree.get("revision")?.asLong(fallback.revision) ?: fallback.revision
                Metadata(correlationId, revision)
            } catch (_: Exception) {
                log.warn(
                    "Falling back metadata for event {} in stream {} due to unreadable user metadata",
                    resolvedEvent.event.eventType,
                    resolvedEvent.event.streamId
                )
                fallback
            }
        }
    }
}
