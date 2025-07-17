package pl.szymanski.wiktor.ta.domain.event

import java.util.UUID

interface AttractionEvent : Event {
    val attractionId: UUID
}

data class AttractionBookedEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override var correlationId: UUID? = null,
    override val attractionId: UUID,
    val userId: UUID,
) : AttractionEvent

data class AttractionBookingCanceledEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override var correlationId: UUID? = null,
    override val attractionId: UUID,
    val userId: UUID,
) : AttractionEvent
