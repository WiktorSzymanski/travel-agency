package pl.szymanski.wiktor.ta.infrastructure.outbox

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import pl.szymanski.wiktor.ta.EventEnvelope
import pl.szymanski.wiktor.ta.Metadata
import pl.szymanski.wiktor.ta.domain.event.PublishableEvent
import pl.szymanski.wiktor.ta.outbox.OutboxEntry
import pl.szymanski.wiktor.ta.outbox.OutboxPort
import pl.szymanski.wiktor.ta.repository.CommandRepository
import java.time.LocalDateTime
import java.util.UUID

@Service
class OutboxPortImpl(
    private val outboxRepository: OutboxRepositoryMongo,
    private val transactionTemplate: TransactionTemplate,
) : OutboxPort {
    override suspend fun <T, R> create(entity: T, event: PublishableEvent, metadata: Metadata, repository: CommandRepository<T, R>) {
        val entry = OutboxEntry(EventEnvelope(event, metadata))

        withContext(Dispatchers.IO) {
            transactionTemplate.execute {
                repository.createBlocking(entity, metadata)
                outboxRepository.saveBlocking(entry)
            }
        }
    }

    override suspend fun <T, R> save(
        entity: T,
        events: List<PublishableEvent>,
        metadata: Metadata,
        repository: CommandRepository<T, R>
    ) {
        val entries = events
            .map { event -> EventEnvelope(event, metadata) }
            .map { eventEnvelope -> OutboxEntry(eventEnvelope) }

        withContext(Dispatchers.IO) {
            transactionTemplate.execute {
                repository.saveBlocking(entity, metadata)
                entries.forEach { entry -> outboxRepository.saveBlocking(entry) }
            }
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