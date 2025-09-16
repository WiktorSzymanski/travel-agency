package pl.szymanski.wiktor.ta.infrastructure.repository.command

import com.azure.cosmos.models.CosmosQueryRequestOptions
import com.azure.cosmos.models.PartitionKey
import com.azure.cosmos.models.SqlQuerySpec
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.runBlocking
import pl.szymanski.wiktor.ta.domain.aggregate.Attraction
import pl.szymanski.wiktor.ta.domain.event.AttractionEvent
import pl.szymanski.wiktor.ta.domain.repository.AttractionRepository
import pl.szymanski.wiktor.ta.infrastructure.repository.EventJsonSerializer
import pl.szymanski.wiktor.ta.infrastructure.scheduler.CosmosClientProvider
import pl.szymanski.wiktor.ta.infrastructure.scheduler.PersistedEvent
import java.util.*

class AttractionRepositoryImpl() : AttractionRepository {
    companion object {
        private val log = org.slf4j.LoggerFactory.getLogger(this::class.java)
    }

    private val container = runBlocking { CosmosClientProvider.getContainer() }

    override suspend fun findById(attractionId: UUID): Attraction {
        val streamName = "attraction-$attractionId"
        val query = "SELECT * FROM c"
        val options = CosmosQueryRequestOptions().apply { partitionKey = PartitionKey(streamName) }

        val querySpec = SqlQuerySpec(query)

        val readResult = container.queryItems(querySpec, options, PersistedEvent::class.java)
            .collectList()
            .awaitSingle()

        val events: List<Triple<AttractionEvent, Int, String>> = readResult.map { persistedEvent ->
            val eventTypeName = persistedEvent.type
            val eventClass: Class<*> = Class.forName(eventTypeName)

            Triple(
                EventJsonSerializer.fromJSON(persistedEvent.domainevent, eventClass),
                persistedEvent.revision.toInt(),
                persistedEvent.etag) as Triple<AttractionEvent, Int, String>
        }

        return Attraction.fromEvents(events)
                ?: throw NoSuchElementException("Attraction with ID $attractionId not found")
    }
}
