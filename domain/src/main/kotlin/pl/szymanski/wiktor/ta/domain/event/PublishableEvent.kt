package pl.szymanski.wiktor.ta.domain.event

import kotlinx.serialization.Serializable
import pl.szymanski.wiktor.ta.domain.UUIDSerializer
import java.util.UUID

interface PublishableEvent {
    @Serializable(with = UUIDSerializer::class)
    val eventId: UUID
}
