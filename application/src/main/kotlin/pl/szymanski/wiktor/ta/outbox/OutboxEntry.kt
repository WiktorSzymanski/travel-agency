package pl.szymanski.wiktor.ta.outbox

import pl.szymanski.wiktor.ta.EventEnvelope
import pl.szymanski.wiktor.ta.domain.event.PublishableEvent
import java.time.Instant
import java.time.LocalDateTime
import java.util.UUID

data class OutboxEntry (
    val eventId: UUID,
    val eventEnvelope: EventEnvelope<PublishableEvent>,
    val published: Boolean,
    val publishedAt: Instant?,
    val createdAt: Instant,
    val processAfter: LocalDateTime?,
) {
    constructor(event: EventEnvelope<PublishableEvent>) : this(
        eventId = event.event.eventId,
        eventEnvelope = event,
        published = false,
        publishedAt = null,
        createdAt = Instant.now(),
        processAfter = null
    )

    constructor(event: EventEnvelope<PublishableEvent>, processAfter: LocalDateTime) : this(
        eventId = event.event.eventId,
        eventEnvelope = event,
        published = false,
        publishedAt = null,
        createdAt = Instant.now(),
        processAfter = null
    )
}