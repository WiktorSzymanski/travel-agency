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
import pl.szymanski.wiktor.ta.domain.AttractionStatusEnum
import pl.szymanski.wiktor.ta.domain.aggregate.Attraction
import pl.szymanski.wiktor.ta.domain.event.AttractionAvailableEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionBookFailedEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionBookedEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionBookingCancelFailedEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionBookingCanceledEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionCreatedEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionExpireFailedEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionExpiredEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionFullEvent
import pl.szymanski.wiktor.ta.domain.event.Event
import pl.szymanski.wiktor.ta.domain.repository.AttractionRepository
import pl.szymanski.wiktor.ta.event.AttractionBookedCompensatedEvent
import pl.szymanski.wiktor.ta.event.AttractionBookingCanceledCompensatedEvent
import pl.szymanski.wiktor.ta.event.AttractionDateMetEvent
import pl.szymanski.wiktor.ta.infrastructure.repository.EventJsonSerializer
import pl.szymanski.wiktor.ta.infrastructure.repository.KurrentDbProvider
import java.util.UUID

// Projection model for Attraction queries
data class AttractionProjection(
    val _id: UUID,
    val status: AttractionStatusEnum
)

// Event store model for storing events
data class AttractionEventRecord(
    val _id: UUID,
    val aggregateId: UUID,
    val eventType: String,
    val eventData: String,
    val timestamp: Long = System.currentTimeMillis()
)

class AttractionRepositoryImpl(
    database: MongoDatabase,
) : AttractionRepository {
    private val kurrentClient: KurrentDBClient = KurrentDbProvider.client

    override suspend fun findById(attractionId: UUID): Attraction {
        val streamName = "attraction-$attractionId"

        val options = ReadStreamOptions.get()
            .forwards()
            .fromStart()

        val readResult = kurrentClient.readStream(streamName, options).await()

        val events: List<Pair<AttractionEvent, Int>> = readResult.events.map { resolvedEvent ->
            val eventTypeName = resolvedEvent.event.eventType
            val eventClass: Class<*> = Class.forName(eventTypeName)

            (EventJsonSerializer.fromBytes(resolvedEvent.event.eventData, eventClass) to resolvedEvent.event.revision.toInt()) as Pair<AttractionEvent, Int>
        }

        return Attraction.fromEvents(events)
            ?: throw NoSuchElementException("Attraction with ID $attractionId not found")
    }

    override suspend fun save(event: Event) {
        if (event !is AttractionEvent) {
            throw IllegalArgumentException("Event must be an AttractionEvent")
        }
        
        try {
            val streamName = "attraction-${event.attractionId}"
            val serializedEvent = EventJsonSerializer.toBytes(event)
            val eventData = EventData.builderAsJson(event::class.simpleName, serializedEvent).build()
            kurrentClient.appendToStream(streamName, eventData)
        } catch (e: Exception) {
            println("Failed to save event to KurrentDb: ${e.message}")
            throw e
        }
    }
}
