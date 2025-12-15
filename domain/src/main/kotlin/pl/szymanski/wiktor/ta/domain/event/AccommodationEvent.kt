package pl.szymanski.wiktor.ta.domain.event

import pl.szymanski.wiktor.ta.domain.LocationEnum
import pl.szymanski.wiktor.ta.domain.Rent
import java.util.UUID

sealed interface AccommodationEvent : DomainEvent {
    val accommodationId: UUID
}

data class AccommodationCreatedEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override val accommodationId: UUID,
    val name: String,
    val location: LocationEnum,
    val rent: Rent,
) : AccommodationEvent

data class AccommodationBookedEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override val accommodationId: UUID,
    val bookingId: UUID,
) : AccommodationEvent

data class AccommodationBookingCanceledEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override val accommodationId: UUID,
    val bookingId: UUID,
) : AccommodationEvent

data class AccommodationExpiredEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override val accommodationId: UUID,
) : AccommodationEvent

data class AccommodationBookedCompensatedEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override val accommodationId: UUID,
    val bookingId: UUID,
) : AccommodationEvent

data class AccommodationBookingCanceledCompensatedEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override val accommodationId: UUID,
    val bookingId: UUID,
) : AccommodationEvent
