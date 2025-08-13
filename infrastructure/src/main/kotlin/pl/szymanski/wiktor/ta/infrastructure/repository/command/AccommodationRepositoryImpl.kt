package pl.szymanski.wiktor.ta.infrastructure.repository.command

import com.mongodb.kotlin.client.coroutine.MongoCollection
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import io.kurrent.dbclient.EventData
import io.kurrent.dbclient.KurrentDBClient
import io.kurrent.dbclient.ReadStreamOptions
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.future.await
import kotlinx.serialization.json.Json
import org.bson.Document
import pl.szymanski.wiktor.ta.domain.AccommodationStatusEnum
import pl.szymanski.wiktor.ta.domain.aggregate.Accommodation
import pl.szymanski.wiktor.ta.domain.event.AccommodationBookFailedEvent
import pl.szymanski.wiktor.ta.domain.event.AccommodationBookedEvent
import pl.szymanski.wiktor.ta.domain.event.AccommodationBookingCancelFailedEvent
import pl.szymanski.wiktor.ta.domain.event.AccommodationBookingCanceledEvent
import pl.szymanski.wiktor.ta.domain.event.AccommodationCreatedEvent
import pl.szymanski.wiktor.ta.domain.event.AccommodationEvent
import pl.szymanski.wiktor.ta.domain.event.AccommodationExpireFailedEvent
import pl.szymanski.wiktor.ta.domain.event.AccommodationExpiredEvent
import pl.szymanski.wiktor.ta.domain.event.Event
import pl.szymanski.wiktor.ta.domain.repository.AccommodationRepository
import pl.szymanski.wiktor.ta.event.AccommodationBookedCompensatedEvent
import pl.szymanski.wiktor.ta.event.AccommodationBookingCanceledCompensatedEvent
import pl.szymanski.wiktor.ta.event.AccommodationDateMetEvent
import pl.szymanski.wiktor.ta.infrastructure.repository.EventJsonSerializer
import pl.szymanski.wiktor.ta.infrastructure.repository.KurrentDbProvider
import java.util.UUID

// Projection model for Accommodation queries
data class AccommodationProjection(
    val _id: UUID,
    val status: AccommodationStatusEnum
)

// Event store model for storing events
data class AccommodationEventRecord(
    val _id: UUID,
    val aggregateId: UUID,
    val eventType: String,
    val eventData: String,
    val timestamp: Long = System.currentTimeMillis()
)

class AccommodationRepositoryImpl(
    database: MongoDatabase,
) : AccommodationRepository {
    private val kurrentClient: KurrentDBClient = KurrentDbProvider.client

    override suspend fun findById(accommodationId: UUID): Accommodation {
        val streamName = "accommodation-$accommodationId"

        val options = ReadStreamOptions.get()
            .fromStart()
            .maxCount(100)

        val readResult = kurrentClient.readStream(streamName, options).await()

        val events: List<AccommodationEvent> = readResult.events.map { resolvedEvent ->
            val eventTypeName = resolvedEvent.event.eventType
            val eventClass = accommodationEventTypeRegistry[eventTypeName]
                ?: throw IllegalArgumentException("Unknown event type: $eventTypeName")

            EventJsonSerializer.fromBytes(resolvedEvent.event.eventData, eventClass)
        }

        return Accommodation.fromEvents(events)
            ?: throw NoSuchElementException("Accommodation with ID $accommodationId not found")
    }

    override suspend fun save(event: Event) {
        if (event !is AccommodationEvent) {
            throw IllegalArgumentException("Event must be an AccommodationEvent")
        }

        try {
            val streamName = "accommodation-${event.accommodationId}"
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
val accommodationEventTypeRegistry: Map<String, Class<out AccommodationEvent>> = mapOf(
    AccommodationBookedEvent::class.simpleName!! to AccommodationBookedEvent::class.java,
    AccommodationBookingCanceledEvent::class.simpleName!! to AccommodationBookingCanceledEvent::class.java,
    AccommodationExpiredEvent::class.simpleName!! to AccommodationExpiredEvent::class.java,
    AccommodationCreatedEvent::class.simpleName!! to AccommodationCreatedEvent::class.java,
    AccommodationDateMetEvent::class.simpleName!! to AccommodationDateMetEvent::class.java,

    AccommodationBookingCanceledCompensatedEvent::class.simpleName!! to AccommodationBookingCanceledCompensatedEvent::class.java,
    AccommodationBookedCompensatedEvent::class.simpleName!! to AccommodationBookedCompensatedEvent::class.java,

    // Failure events
    AccommodationExpireFailedEvent::class.simpleName!! to AccommodationExpireFailedEvent::class.java,
    AccommodationBookFailedEvent::class.simpleName!! to AccommodationBookFailedEvent::class.java,
    AccommodationBookingCancelFailedEvent::class.simpleName!! to AccommodationBookingCancelFailedEvent::class.java
)
