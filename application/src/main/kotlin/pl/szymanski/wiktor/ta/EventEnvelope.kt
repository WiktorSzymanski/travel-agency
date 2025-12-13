package pl.szymanski.wiktor.ta

import pl.szymanski.wiktor.ta.domain.event.Event
import java.util.UUID

data class Metadata (
    val correlationId: UUID,
    val revision: Long,
)

data class EventEnvelope<T: Event> (
    val eventType: String,
    val domainEvent: T,
    val metadata: Metadata,
) {
    constructor(domainEvent: T, metadata: Metadata) : this(
        domainEvent::class.simpleName!!,
        domainEvent,
        metadata
    )
}
