package pl.szymanski.wiktor.ta.infrastructure.outbox

import org.springframework.stereotype.Service
import pl.szymanski.wiktor.ta.EventEnvelope
import pl.szymanski.wiktor.ta.Metadata
import pl.szymanski.wiktor.ta.domain.event.PublishableEvent
import pl.szymanski.wiktor.ta.event.DateMetEvent
import pl.szymanski.wiktor.ta.outbox.OutboxEntry
import pl.szymanski.wiktor.ta.outbox.OutboxPort
import pl.szymanski.wiktor.ta.repository.Repository
import java.time.LocalDateTime
import java.util.UUID

@Service
class OutboxPortImpl(
    private val outboxRepository: OutboxRepositoryMongo,
) : OutboxPort {
    override suspend fun <T> create(entity: T, event: PublishableEvent, metadata: Metadata, repository: Repository<T>) {
        val entry = OutboxEntry(EventEnvelope(event, metadata))

        // TODO: Transactional
        repository.create(entity, metadata)
        outboxRepository.save(entry)
    }

    override suspend fun <T> save(
        entity: T,
        events: List<PublishableEvent>,
        metadata: Metadata,
        repository: Repository<T>
    ) {
        val entries = events
            .map { event -> EventEnvelope(event, metadata) }
            .map { eventEnvelope -> OutboxEntry(eventEnvelope) }

        // TODO: Transactional
        repository.save(entity, metadata)
        entries.forEach { entry -> outboxRepository.save(entry) }
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