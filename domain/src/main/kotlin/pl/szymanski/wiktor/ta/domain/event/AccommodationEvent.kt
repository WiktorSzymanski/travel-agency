package pl.szymanski.wiktor.ta.domain.event

import pl.szymanski.wiktor.ta.domain.LocationEnum
import pl.szymanski.wiktor.ta.domain.Rent
import java.util.UUID

interface AccommodationFailedEvent : AccommodationEvent, FailedEvent

data class AccommodationBookedEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override var correlationId: UUID? = null,
    override val accommodationId: UUID,
    val bookingId: UUID,
) : AccommodationEvent

data class AccommodationBookingCanceledEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override var correlationId: UUID? = null,
    override val accommodationId: UUID,
    val bookingId: UUID,
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

data class AccommodationExpireFailedEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override var correlationId: UUID? = null,
    override val accommodationId: UUID,
    override val message: String,
) : AccommodationFailedEvent

data class AccommodationBookFailedEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override var correlationId: UUID? = null,
    override val accommodationId: UUID,
    val bookingId: UUID,
    override val message: String,
) : AccommodationFailedEvent

data class AccommodationBookingCancelFailedEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override var correlationId: UUID? = null,
    override val accommodationId: UUID,
    val bookingId: UUID,
    override val message: String,
) : AccommodationFailedEvent
