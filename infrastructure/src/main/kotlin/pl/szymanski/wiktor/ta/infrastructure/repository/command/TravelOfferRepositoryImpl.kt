package pl.szymanski.wiktor.ta.infrastructure.repository.command

import com.azure.cosmos.models.CosmosQueryRequestOptions
import com.azure.cosmos.models.PartitionKey
import com.azure.cosmos.models.SqlQuerySpec
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.runBlocking
import pl.szymanski.wiktor.ta.domain.aggregate.Commute
import pl.szymanski.wiktor.ta.domain.aggregate.TravelOffer
import pl.szymanski.wiktor.ta.domain.event.CommuteEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferEvent
import pl.szymanski.wiktor.ta.domain.repository.TravelOfferRepository
import pl.szymanski.wiktor.ta.infrastructure.repository.EventJsonSerializer
import pl.szymanski.wiktor.ta.infrastructure.scheduler.CosmosClientProvider
import pl.szymanski.wiktor.ta.infrastructure.scheduler.PersistedEvent
import java.util.*

// Projection model for TravelOffer queries
data class TravelOfferProjection(
    val id: UUID,
    val commuteId: UUID,
    val accommodationId: UUID,
    val attractionId: UUID?
)

// Event store model for storing events
data class EventRecord(
    val id: UUID,
    val aggregateId: UUID,
    val eventType: String,
    val eventData: String,
    val timestamp: Long = System.currentTimeMillis()
)

class TravelOfferRepositoryImpl() : TravelOfferRepository {
    companion object {
        private val log = org.slf4j.LoggerFactory.getLogger(this::class.java)
    }
    private val container = runBlocking { CosmosClientProvider.getContainer() }

    override suspend fun findById(travelOfferId: UUID): TravelOffer {
        val streamName = "travelOffer-$travelOfferId"
        val query = "SELECT * FROM c"
        val options = CosmosQueryRequestOptions().apply { partitionKey = PartitionKey(streamName) }

        val querySpec = SqlQuerySpec(query)

        val readResult = container.queryItems(querySpec, options, PersistedEvent::class.java)
            .collectList()
            .awaitSingle()

        val events: List<Triple<TravelOfferEvent, Int, String>> = readResult.map { persistedEvent ->
            val eventTypeName = persistedEvent.type
            val eventClass: Class<*> = Class.forName(eventTypeName)

            Triple(
                EventJsonSerializer.fromJSON(persistedEvent.domainevent, eventClass),
                persistedEvent.revision.toInt(),
                persistedEvent.etag) as Triple<TravelOfferEvent, Int, String>
        }

        return TravelOffer.fromEvents(events)
            ?: throw NoSuchElementException("Commute with ID $travelOfferId not found")
    }
}
