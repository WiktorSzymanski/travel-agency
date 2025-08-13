package pl.szymanski.wiktor.ta.infrastructure.repository.command

import com.mongodb.client.model.Aggregates
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Projections
import com.mongodb.kotlin.client.coroutine.MongoCollection
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import io.kurrent.dbclient.KurrentDBClient
import io.kurrent.dbclient.EventData
import io.kurrent.dbclient.ReadStreamOptions
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.future.await
import kotlinx.serialization.json.Json
import org.bson.Document
import pl.szymanski.wiktor.ta.domain.AccommodationStatusEnum
import pl.szymanski.wiktor.ta.domain.AttractionStatusEnum
import pl.szymanski.wiktor.ta.domain.CommuteStatusEnum
import pl.szymanski.wiktor.ta.domain.aggregate.TravelOffer
import pl.szymanski.wiktor.ta.domain.event.TravelOfferBookFailedEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferBookedEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferBookingCancelFailedEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferBookingCanceledEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferCreatedEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferExpireFailedEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferExpiredEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferMadeAvailableEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferMadeUnavailableEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferMakeAvailableFailedEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferMakeUnavailableFailedEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferRebookCompleteFailedEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferRebookedEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferReleaseCompleteFailedEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferReleaseEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferReservationCancelFailedEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferReservationCanceledEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferReserveFailedEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferReservedEvent
import pl.szymanski.wiktor.ta.domain.repository.TravelOfferRepository
import pl.szymanski.wiktor.ta.event.TravelOfferBookedCompensatedEvent
import pl.szymanski.wiktor.ta.event.TravelOfferBookingCanceledCompensatedEvent
import pl.szymanski.wiktor.ta.infrastructure.repository.EventJsonSerializer
import pl.szymanski.wiktor.ta.infrastructure.repository.KurrentDbProvider
import java.util.UUID

// Projection model for TravelOffer queries
data class TravelOfferProjection(
    val _id: UUID,
    val commuteId: UUID,
    val accommodationId: UUID,
    val attractionId: UUID?
)

// Event store model for storing events
data class EventRecord(
    val _id: UUID,
    val aggregateId: UUID,
    val eventType: String,
    val eventData: String,
    val timestamp: Long = System.currentTimeMillis()
)

class TravelOfferRepositoryImpl(
    database: MongoDatabase
) : TravelOfferRepository {
    private val kurrentClient: KurrentDBClient = KurrentDbProvider.client

    override suspend fun findById(travelOfferId: UUID): TravelOffer {
        val streamName = "travelOffer-$travelOfferId"

        val options = ReadStreamOptions.get()
            .fromStart()
            .maxCount(100)

        val readResult = kurrentClient.readStream(streamName, options).await()


        val events: List<TravelOfferEvent> = readResult.events.map { resolvedEvent ->
            val eventTypeName = resolvedEvent.event.eventType
            val eventClass = travelOfferEventTypeRegistry[eventTypeName]
                ?: throw IllegalArgumentException("Unknown event type: $eventTypeName")

            EventJsonSerializer.fromBytes(resolvedEvent.event.eventData, eventClass)
        }

        return TravelOffer.fromEvents(events)
            ?: throw NoSuchElementException("TravelOffer with ID $travelOfferId not found")
    }

    override suspend fun save(event: TravelOfferEvent) {
        try {
            val streamName = "travelOffer-${event.travelOfferId}"
            val serializedEvent = EventJsonSerializer.toBytes(event)
            val eventData = EventData.builderAsJson(event::class.simpleName, serializedEvent).build()
            kurrentClient.appendToStream(streamName, eventData)
        } catch (e: Exception) {
            println("Failed to save event to KurrentDb: ${e.message}")
            throw e
        }
    }
}

// Maps event type names in KurrentDb to their Kotlin classes
val travelOfferEventTypeRegistry: Map<String, Class<out TravelOfferEvent>> = mapOf(
    TravelOfferReservedEvent::class.simpleName!! to TravelOfferReservedEvent::class.java,
    TravelOfferReservationCanceledEvent::class.simpleName!! to TravelOfferReservationCanceledEvent::class.java,
    TravelOfferBookedEvent::class.simpleName!! to TravelOfferBookedEvent::class.java,
    TravelOfferReleaseEvent::class.simpleName!! to TravelOfferReleaseEvent::class.java,
    TravelOfferRebookedEvent::class.simpleName!! to TravelOfferRebookedEvent::class.java,
    TravelOfferBookingCanceledEvent::class.simpleName!! to TravelOfferBookingCanceledEvent::class.java,
    TravelOfferExpiredEvent::class.simpleName!! to TravelOfferExpiredEvent::class.java,
    TravelOfferCreatedEvent::class.simpleName!! to TravelOfferCreatedEvent::class.java,
    TravelOfferMadeUnavailableEvent::class.simpleName!! to TravelOfferMadeUnavailableEvent::class.java,
    TravelOfferMadeAvailableEvent::class.simpleName!! to TravelOfferMadeAvailableEvent::class.java,

    TravelOfferBookedCompensatedEvent::class.simpleName!! to TravelOfferBookedCompensatedEvent::class.java,
    TravelOfferBookingCanceledCompensatedEvent::class.simpleName!! to TravelOfferBookingCanceledCompensatedEvent::class.java,

    // Failure events
    TravelOfferBookFailedEvent::class.simpleName!! to TravelOfferBookFailedEvent::class.java,
    TravelOfferReserveFailedEvent::class.simpleName!! to TravelOfferReserveFailedEvent::class.java,
    TravelOfferMakeUnavailableFailedEvent::class.simpleName!! to TravelOfferMakeUnavailableFailedEvent::class.java,
    TravelOfferMakeAvailableFailedEvent::class.simpleName!! to TravelOfferMakeAvailableFailedEvent::class.java,
    TravelOfferExpireFailedEvent::class.simpleName!! to TravelOfferExpireFailedEvent::class.java,
    TravelOfferReservationCancelFailedEvent::class.simpleName!! to TravelOfferReservationCancelFailedEvent::class.java,
    TravelOfferBookingCancelFailedEvent::class.simpleName!! to TravelOfferBookingCancelFailedEvent::class.java,
    TravelOfferReleaseCompleteFailedEvent::class.simpleName!! to TravelOfferReleaseCompleteFailedEvent::class.java,
    TravelOfferRebookCompleteFailedEvent::class.simpleName!! to TravelOfferRebookCompleteFailedEvent::class.java
)
