package pl.szymanski.wiktor.ta

import kotlinx.serialization.Serializable
import pl.szymanski.wiktor.ta.domain.event.PublishableEvent
import java.util.UUID

@Serializable
data class Metadata (
    val correlationId: @Serializable(with = pl.szymanski.wiktor.ta.domain.UUIDSerializer::class) UUID,
    val revision: Long,
)

@Serializable
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
