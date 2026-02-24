package pl.szymanski.wiktor.ta

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import pl.szymanski.wiktor.ta.domain.event.PublishableEvent

@Serializable
data class EventEnvelope<T: PublishableEvent> (
    val eventType: String,
    @Contextual val event: T,
    val metadata: Metadata,
) {
    constructor(event: T, metadata: Metadata) : this(
        event::class.simpleName!!,
        event,
        metadata
    )
}
