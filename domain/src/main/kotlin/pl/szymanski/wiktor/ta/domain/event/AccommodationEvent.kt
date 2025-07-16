package pl.szymanski.wiktor.ta.domain.event

import java.util.UUID

interface AccommodationEvent: Event {
    val accommodationId: UUID
}

data class AccommodationBookedEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override var correlationId: UUID? = null,
    override val accommodationId: UUID,
    val userId: UUID,
) : AccommodationEvent

data class AccommodationBookingCanceledEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override var correlationId: UUID? = null,
    override val accommodationId: UUID,
    val userId: UUID
) : AccommodationEvent