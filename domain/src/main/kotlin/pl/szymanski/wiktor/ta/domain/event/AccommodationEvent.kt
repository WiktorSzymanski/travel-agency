package pl.szymanski.wiktor.ta.domain.event

import pl.szymanski.wiktor.ta.domain.LocationEnum
import pl.szymanski.wiktor.ta.domain.Rent
import java.util.UUID

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
    val userId: UUID,
) : AccommodationEvent

data class AccommodationExpiredEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override var correlationId: UUID? = null,
    override val accommodationId: UUID,
) : AccommodationEvent

data class AccommodationCreatedEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override var correlationId: UUID? = null,
    override val accommodationId: UUID,
    val name: String,
    val location: LocationEnum,
    val rent: Rent,
) : AccommodationEvent
