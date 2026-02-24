package pl.szymanski.wiktor.ta.outbox

import pl.szymanski.wiktor.ta.Metadata
import pl.szymanski.wiktor.ta.domain.event.PublishableEvent
import pl.szymanski.wiktor.ta.event.DateMetEvent
import pl.szymanski.wiktor.ta.repository.Repository
import java.time.LocalDateTime
import java.util.UUID

interface OutboxPort {
    suspend fun <T> create(entity: T, event: PublishableEvent, metadata: Metadata, repository: Repository<T>)
    suspend fun <T> save(entity: T, events: List<PublishableEvent>, metadata: Metadata, repository: Repository<T>)
    suspend fun delayEvent(event: PublishableEvent, processAfter: LocalDateTime)
    suspend fun getPendingEntries(limit: Int = 100): List<OutboxEntry>
    suspend fun markAsPublished(eventId: UUID)
}