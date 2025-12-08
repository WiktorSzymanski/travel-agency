package pl.szymanski.wiktor.ta

import pl.szymanski.wiktor.ta.domain.event.Event
import java.util.UUID

data class Metadata(
    val eventId: UUID,
    val correlationId: UUID,
    val revision: Long,
)

data class EventEnvelope<out T : Event>(
    val domainEvent: Event,
    val metadata: Metadata,
)
