package pl.szymanski.wiktor.ta.infrastructure.outbox

import io.kurrent.dbclient.AppendToStreamOptions
import io.kurrent.dbclient.EventData
import io.kurrent.dbclient.KurrentDBClient
import io.kurrent.dbclient.StreamState
import tools.jackson.databind.ObjectMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withContext
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service
import pl.szymanski.wiktor.ta.EventEnvelope
import pl.szymanski.wiktor.ta.Metadata
import pl.szymanski.wiktor.ta.domain.event.PublishableEvent
import pl.szymanski.wiktor.ta.eventstore.EventStore
import pl.szymanski.wiktor.ta.outbox.OutboxEntry
import pl.szymanski.wiktor.ta.outbox.OutboxPort
import pl.szymanski.wiktor.ta.repository.CommandRepository
import java.time.LocalDateTime
import java.util.UUID

@Service
@Primary
class OutboxPortImpl(
    private val eventStore: EventStore,
    private val objectMapper: ObjectMapper,
    private val outboxRepository: OutboxRepositoryMongo,
) : OutboxPort {

    override suspend fun <T, R> create(
        entity: T,
        event: PublishableEvent,
        metadata: Metadata,
        repository: CommandRepository<T, R>
    ) {
        save(entity, listOf(event), metadata, repository)
    }

    override suspend fun <T, R> save(
        entity: T,
        events: List<PublishableEvent>,
        metadata: Metadata,
        repository: CommandRepository<T, R>
    ) {
        if (events.isEmpty()) return

        for (event in events) {
            val envelope = EventEnvelope(event, metadata)
            eventStore.publish(envelope)
        }
    }

    override suspend fun delayEvent(event: PublishableEvent, processAfter: LocalDateTime) {
        val entry = OutboxEntry(EventEnvelope(event, Metadata(UUID.randomUUID(), 0)), processAfter)

        outboxRepository.save(entry)
    }

    override suspend fun getPendingEntries(limit: Int): List<OutboxEntry> {
        return outboxRepository.getPendingEntries(limit)
    }

    override suspend fun markAsPublished(eventId: UUID) {
        outboxRepository.markAsPublished(eventId)
    }
}
