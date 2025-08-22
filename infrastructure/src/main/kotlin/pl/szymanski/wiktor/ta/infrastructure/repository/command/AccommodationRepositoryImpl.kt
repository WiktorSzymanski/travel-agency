package pl.szymanski.wiktor.ta.infrastructure.repository.command

import com.mongodb.kotlin.client.coroutine.MongoCollection
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import io.grpc.Status
import io.grpc.StatusRuntimeException
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
    companion object {
        private val log = org.slf4j.LoggerFactory.getLogger(this::class.java)
    }
    private val kurrentClient: KurrentDBClient = KurrentDbProvider.client

    override suspend fun findById(accommodationId: UUID): Accommodation {
        val streamName = "accommodation-$accommodationId"

        val options = ReadStreamOptions.get()
            .forwards()
            .fromStart()

            try {
                val readResult = retryOnUnavailable { kurrentClient.readStream(streamName, options).await() }
                val events: List<Pair<AccommodationEvent, Int>> = readResult.events.map { resolvedEvent ->
                    val eventTypeName = resolvedEvent.event.eventType
                    val eventClass: Class<*> = Class.forName(eventTypeName)

                    (EventJsonSerializer.fromBytes(resolvedEvent.event.eventData, eventClass) to resolvedEvent.event.revision.toInt()) as Pair<AccommodationEvent, Int>
                }

                return Accommodation.fromEvents(events)
                    ?: throw NoSuchElementException("Accommodation with ID $accommodationId not found")

            } catch (e: StatusRuntimeException) {
                if (e.status.code == Status.Code.DEADLINE_EXCEEDED) {
                    log.error("Call failed with ${e.status.code} for stream $streamName")
                }
                throw e
            }
    }

    override suspend fun save(event: Event) {
        if (event !is AccommodationEvent) {
            throw IllegalArgumentException("Event must be an AccommodationEvent")
        }

        try {
            val streamName = "accommodation-${event.accommodationId}"
            val serializedEvent = EventJsonSerializer.toBytes(event)
            val eventData = EventData.builderAsJson(event::class.simpleName, serializedEvent).build()
            retryOnUnavailable {
                kurrentClient.appendToStream(streamName, eventData)
            }
        } catch (e: Exception) {
            println("Failed to save event to KurrentDb: ${e.message}")
            throw e
        }
    }
}
