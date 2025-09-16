package pl.szymanski.wiktor.ta.infrastructure.scheduler

import com.azure.cosmos.CosmosAsyncContainer
import com.azure.cosmos.models.CosmosQueryRequestOptions
import com.azure.cosmos.models.PartitionKey
import com.azure.cosmos.models.SqlParameter
import com.azure.cosmos.models.SqlQuerySpec
import com.azure.cosmos.util.CosmosPagedFlux
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import pl.szymanski.wiktor.ta.domain.event.Event
import pl.szymanski.wiktor.ta.infrastructure.repository.EventJsonSerializer
import java.time.LocalDateTime
import kotlin.apply
import kotlin.collections.getValue
import kotlin.collections.map
import kotlin.jvm.java
import kotlin.to

class CoroutineCosmosEventRepository(private val container: CosmosAsyncContainer) {

    // Save event asynchronously with coroutines
    suspend fun saveEvent(event: Event, stream: String, revision: Long): Unit {
        val persistedEvent = PersistedEvent(
            id = event.eventId,
            stream = stream,
            type = event::class.java.name,
            correlationid = event.correlationId!!,
            timestamp = LocalDateTime.now().toString(),
            revision = revision,
            domainevent = EventJsonSerializer.toJSON(event),
        )
        container
            .createItem(persistedEvent)
            .awaitSingle()
    }

    suspend fun getEventsForAggregate(aggregateId: String): List<PersistedEvent> {
        val query = "SELECT * FROM c WHERE c.aggregateId = @aggregateId ORDER BY c.revision ASC"
        val querySpec = SqlQuerySpec(query, listOf(SqlParameter("@aggregateId", aggregateId)))
        val options = CosmosQueryRequestOptions().apply { partitionKey = PartitionKey(aggregateId) }

        return container.queryItems(querySpec, options, PersistedEvent::class.java).getAllItems()
    }

    suspend fun getAllEvents(): List<Pair<Event, Long>> {
        val query = "SELECT * FROM c"
        val ret = container.queryItems(query, Map::class.java).getAllItems() as List<Map<String, Any>>

        return ret.map { serializedEvent ->
            val eventTypeName = serializedEvent.getValue("type") as String
            val eventClass: Class<*> = Class.forName(eventTypeName)
            (EventJsonSerializer.fromJSON(
                serializedEvent.getValue("domainEvent"),
                eventClass)
                    to serializedEvent.getValue("revision")) as Pair<Event, Long>
        }
    }


    suspend fun <T> CosmosPagedFlux<T>.getAllItems(): List<T> {
        val allItems = mutableListOf<T>()
        var continuationToken: String? = null

        do {
            val pagedFlux = if (continuationToken == null) this.byPage() else this.byPage(continuationToken)
            val page = pagedFlux.next().awaitSingleOrNull() ?: break
            allItems.addAll(page.results)
            continuationToken = page.continuationToken
        } while (continuationToken != null)

        return allItems
    }
}
