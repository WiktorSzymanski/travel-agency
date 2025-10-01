package pl.szymanski.wiktor.ta.infrastructure.repository.command

import com.azure.cosmos.models.CosmosQueryRequestOptions
import com.azure.cosmos.models.PartitionKey
import com.azure.cosmos.models.SqlQuerySpec
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.runBlocking
import pl.szymanski.wiktor.ta.domain.aggregate.Commute
import pl.szymanski.wiktor.ta.domain.event.CommuteEvent
import pl.szymanski.wiktor.ta.domain.repository.CommuteRepository
import pl.szymanski.wiktor.ta.infrastructure.repository.EventJsonSerializer
import pl.szymanski.wiktor.ta.infrastructure.scheduler.CosmosClientProvider
import pl.szymanski.wiktor.ta.infrastructure.scheduler.PersistedEvent
import java.util.*

class CommuteRepositoryImpl() : CommuteRepository {
    companion object {
        private val log = org.slf4j.LoggerFactory.getLogger(this::class.java)
    }
    private val container = runBlocking { CosmosClientProvider.getContainer() }

    override suspend fun findById(commuteId: UUID): Commute {
        val streamName = "commute-$commuteId"
        val query = "SELECT * FROM c"
        val options = CosmosQueryRequestOptions().apply { partitionKey = PartitionKey(streamName) }

        val querySpec = SqlQuerySpec(query)

        val readResult = container.queryItems(querySpec, options, PersistedEvent::class.java)
            .collectList()
            .awaitSingle()

        val events: List<Triple<CommuteEvent, Int, String>> = readResult.map { persistedEvent ->
            val eventTypeName = persistedEvent.type
            val eventClass: Class<*> = Class.forName(eventTypeName)

            Triple(
                EventJsonSerializer.fromJSON(persistedEvent.domainevent, eventClass),
                persistedEvent.revision.toInt(),
                persistedEvent.etag) as Triple<CommuteEvent, Int, String>
        }

        return Commute.fromEvents(events)
            ?: throw NoSuchElementException("Commute with ID $commuteId not found")
    }
//    override suspend fun findById(commuteId: UUID): Commute {
//        val streamName = "commute-$commuteId"
//
//        val options = ReadStreamOptions.get()
//            .forwards()
//            .fromStart()
//
//        try {
//            val readResult = retryOnUnavailable {
//                kurrentClient.readStream(streamName, options).await()
//            }
//
//            val events: List<Pair<CommuteEvent, Int>> = readResult.events.map { resolvedEvent ->
//                val eventTypeName = resolvedEvent.event.eventType
//                val eventClass: Class<*> = Class.forName(eventTypeName)
//
//                (EventJsonSerializer.fromBytes(resolvedEvent.event.eventData, eventClass) to resolvedEvent.event.revision.toInt()) as Pair<CommuteEvent, Int>
//            }
//
//            return Commute.fromEvents(events)
//                ?: throw NoSuchElementException("Commute with ID $commuteId not found")
//        } catch (e: StatusRuntimeException) {
//            if (e.status.code == Status.Code.DEADLINE_EXCEEDED) {
//                log.error("Call failed with ${e.status.code} for stream $streamName")
//            }
//            throw e
//        }
//        throw UnsupportedOperationException("Not yet implemented")
//    }
}
