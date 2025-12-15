package pl.szymanski.wiktor.ta

import pl.szymanski.wiktor.ta.domain.event.PublishableEvent
import java.util.UUID

data class Metadata (
    val correlationId: UUID,
    val revision: Long,
)

data class EventEnvelope<T: PublishableEvent> (
    val eventType: String,
    val event: T,
    val metadata: Metadata,
) {
    constructor(event: T, metadata: Metadata) : this(
        event::class.simpleName!!,
        event,
        metadata
    )
}
