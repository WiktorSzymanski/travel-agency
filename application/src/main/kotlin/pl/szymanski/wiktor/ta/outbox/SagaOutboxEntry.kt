package pl.szymanski.wiktor.ta.outbox

import pl.szymanski.wiktor.ta.EventEnvelope
import pl.szymanski.wiktor.ta.domain.event.PublishableEvent
import java.time.Instant
import java.util.UUID

data class SagaOutboxEntry(
    val eventId: UUID,
    val sagaId: UUID,
    val eventEnvelope: EventEnvelope<PublishableEvent>,
    val published: Boolean,
    val publishedAt: Instant?
)
