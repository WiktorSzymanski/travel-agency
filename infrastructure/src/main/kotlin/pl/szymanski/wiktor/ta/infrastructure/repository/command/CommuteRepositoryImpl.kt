package pl.szymanski.wiktor.ta.infrastructure.repository.command

import com.mongodb.kotlin.client.coroutine.MongoCollection
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import io.kurrent.dbclient.EventData
import io.kurrent.dbclient.KurrentDBClient
import io.kurrent.dbclient.ReadStreamOptions
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.future.await
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.bson.Document
import pl.szymanski.wiktor.ta.domain.CommuteStatusEnum
import pl.szymanski.wiktor.ta.domain.aggregate.Commute
import pl.szymanski.wiktor.ta.domain.event.CommuteCancelBookedSeatFailedEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteAvailableEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteBookSeatFailedEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteBookedEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteBookingCanceledEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteCreatedEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteExpireFailedEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteExpiredEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteFullEvent
import pl.szymanski.wiktor.ta.domain.event.Event
import pl.szymanski.wiktor.ta.domain.repository.CommuteRepository
import pl.szymanski.wiktor.ta.event.CommuteBookedCompensatedEvent
import pl.szymanski.wiktor.ta.event.CommuteBookingCanceledCompensatedEvent
import pl.szymanski.wiktor.ta.event.CommuteDateMetEvent
import pl.szymanski.wiktor.ta.infrastructure.repository.EventJsonSerializer
import pl.szymanski.wiktor.ta.infrastructure.repository.KurrentDbProvider
import java.util.UUID

// Projection model for Commute queries
data class CommuteProjection(
    val _id: UUID,
    val status: CommuteStatusEnum
)

// Event store model for storing events
data class CommuteEventRecord(
    val _id: UUID,
    val aggregateId: UUID,
    val eventType: String,
    val eventData: String,
    val timestamp: Long = System.currentTimeMillis()
)

class CommuteRepositoryImpl(
    database: MongoDatabase,
) : CommuteRepository {
    private val kurrentClient: KurrentDBClient = KurrentDbProvider.client

    override suspend fun findById(commuteId: UUID): Commute {
        val streamName = "commute-$commuteId"

        val options = ReadStreamOptions.get()
            .forwards()
            .fromStart()

        val readResult = kurrentClient.readStream(streamName, options).await()

        val events: List<Pair<CommuteEvent, Int>> = readResult.events.map { resolvedEvent ->
            val eventTypeName = resolvedEvent.event.eventType
            val eventClass = commuteEventTypeRegistry[eventTypeName]
                ?: throw IllegalArgumentException("Unknown event type: $eventTypeName")

            EventJsonSerializer.fromBytes(resolvedEvent.event.eventData, eventClass) to resolvedEvent.event.revision.toInt()
        }

        return Commute.fromEvents(events)
            ?: throw NoSuchElementException("Commute with ID $commuteId not found")
    }

    override suspend fun save(event: Event) {
        if (event !is CommuteEvent) {
            throw IllegalArgumentException("Event must be a CommuteEvent")
        }
        
        try {
            val streamName = "commute-${event.commuteId}"
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
val commuteEventTypeRegistry: Map<String, Class<out CommuteEvent>> = mapOf(
    CommuteBookedEvent::class.simpleName!! to CommuteBookedEvent::class.java,
    CommuteFullEvent::class.simpleName!! to CommuteFullEvent::class.java,
    CommuteAvailableEvent::class.simpleName!! to CommuteAvailableEvent::class.java,
    CommuteBookingCanceledEvent::class.simpleName!! to CommuteBookingCanceledEvent::class.java,
    CommuteExpiredEvent::class.simpleName!! to CommuteExpiredEvent::class.java,
    CommuteCreatedEvent::class.simpleName!! to CommuteCreatedEvent::class.java,
    CommuteDateMetEvent::class.simpleName!! to CommuteDateMetEvent::class.java,

    CommuteBookingCanceledCompensatedEvent::class.simpleName!! to CommuteBookingCanceledCompensatedEvent::class.java,
    CommuteBookedCompensatedEvent::class.simpleName!! to CommuteBookedCompensatedEvent::class.java,
    
    // Failure events
    CommuteExpireFailedEvent::class.simpleName!! to CommuteExpireFailedEvent::class.java,
    CommuteBookSeatFailedEvent::class.simpleName!! to CommuteBookSeatFailedEvent::class.java,
    CommuteCancelBookedSeatFailedEvent::class.simpleName!! to CommuteCancelBookedSeatFailedEvent::class.java
)
