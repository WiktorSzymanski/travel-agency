package pl.szymanski.wiktor.ta.outbox

import pl.szymanski.wiktor.ta.Metadata
import pl.szymanski.wiktor.ta.domain.event.PublishableEvent
import pl.szymanski.wiktor.ta.repository.CommandRepository
import java.time.LocalDateTime
import java.util.UUID

interface OutboxPort {
    suspend fun <T, R> create(entity: T, event: PublishableEvent, metadata: Metadata, repository: CommandRepository<T, R>)
    suspend fun <T, R> save(entity: T, events: List<PublishableEvent>, metadata: Metadata, repository: CommandRepository<T, R>)
    suspend fun delayEvent(event: PublishableEvent, processAfter: LocalDateTime)
    suspend fun getPendingEntries(limit: Int = 100): List<OutboxEntry>
    suspend fun markAsPublished(eventId: UUID)
}