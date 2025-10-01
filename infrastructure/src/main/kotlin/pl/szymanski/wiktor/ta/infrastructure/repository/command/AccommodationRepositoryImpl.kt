package pl.szymanski.wiktor.ta.infrastructure.repository.command

import com.azure.cosmos.models.CosmosQueryRequestOptions
import com.azure.cosmos.models.PartitionKey
import com.azure.cosmos.models.SqlQuerySpec
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.runBlocking
import pl.szymanski.wiktor.ta.domain.AccommodationStatusEnum
import pl.szymanski.wiktor.ta.domain.aggregate.Accommodation
import pl.szymanski.wiktor.ta.domain.event.AccommodationEvent
import pl.szymanski.wiktor.ta.domain.repository.AccommodationRepository
import pl.szymanski.wiktor.ta.infrastructure.repository.EventJsonSerializer
import pl.szymanski.wiktor.ta.infrastructure.scheduler.CosmosClientProvider
import pl.szymanski.wiktor.ta.infrastructure.scheduler.PersistedEvent
import java.util.NoSuchElementException
import java.util.UUID

// Projection model for Accommodation queries
data class AccommodationProjection(
    val id: UUID,
    val status: AccommodationStatusEnum
)

// Event store model for storing events
data class AccommodationEventRecord(
    val id: UUID,
    val aggregateId: UUID,
    val eventType: String,
    val eventData: String,
    val timestamp: Long = System.currentTimeMillis()
)

class AccommodationRepositoryImpl() : AccommodationRepository {
    companion object {
        private val log = org.slf4j.LoggerFactory.getLogger(this::class.java)
    }

    private val container = runBlocking { CosmosClientProvider.getContainer() }

    override suspend fun findById(accommodationId: UUID): Accommodation {
        val streamName = "accommodation-$accommodationId"
        val query = "SELECT * FROM c"
        val options = CosmosQueryRequestOptions().apply { partitionKey = PartitionKey(streamName) }

        val querySpec = SqlQuerySpec(query)

        val readResult = container.queryItems(querySpec, options, PersistedEvent::class.java)
            .collectList()
            .awaitSingle()

        val events: List<Triple<AccommodationEvent, Int, String>> = readResult.map { persistedEvent ->
            val eventTypeName = persistedEvent.type
            val eventClass: Class<*> = Class.forName(eventTypeName)

            Triple(
                EventJsonSerializer.fromJSON(persistedEvent.domainevent, eventClass),
                persistedEvent.revision.toInt(),
                persistedEvent.etag) as Triple<AccommodationEvent, Int, String>
        }

        return Accommodation.fromEvents(events)
            ?: throw NoSuchElementException("Attraction with ID $accommodationId not found")
    }
//    override suspend fun findById(accommodationId: UUID): Accommodation {
//        val streamName = "accommodation-$accommodationId"
//
//        val options = ReadStreamOptions.get()
//            .forwards()
//            .fromStart()
//
//            try {
//                val readResult = retryOnUnavailable { kurrentClient.readStream(streamName, options).await() }
//                val events: List<Pair<AccommodationEvent, Int>> = readResult.events.map { resolvedEvent ->
//                    val eventTypeName = resolvedEvent.event.eventType
//                    val eventClass: Class<*> = Class.forName(eventTypeName)
//
//                    (EventJsonSerializer.fromBytes(resolvedEvent.event.eventData, eventClass) to resolvedEvent.event.revision.toInt()) as Pair<AccommodationEvent, Int>
//                }
//
//                return Accommodation.fromEvents(events)
//                    ?: throw NoSuchElementException("Accommodation with ID $accommodationId not found")
//
//            } catch (e: StatusRuntimeException) {
//                if (e.status.code == Status.Code.DEADLINE_EXCEEDED) {
//                    log.error("Call failed with ${e.status.code} for stream $streamName")
//                }
//                throw e
//            }
//        throw UnsupportedOperationException("Not yet implemented")
//    }

//    override suspend fun save(event: Event) {
//        if (event !is AccommodationEvent) {
//            throw IllegalArgumentException("Event must be an AccommodationEvent")
//        }
//
//        try {
//            val streamName = "accommodation-${event.accommodationId}"
//            val serializedEvent = EventJsonSerializer.toBytes(event)
//            val eventData = EventData.builderAsJson(event::class.simpleName, serializedEvent).build()
//            retryOnUnavailable {
//                kurrentClient.appendToStream(streamName, eventData)
//            }
//        } catch (e: Exception) {
//            println("Failed to save event to KurrentDb: ${e.message}")
//            throw e
//        }
//    }
}
