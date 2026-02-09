package pl.szymanski.wiktor.ta.domain.event

import kotlinx.serialization.Serializable
import pl.szymanski.wiktor.ta.domain.UUIDSerializer
import pl.szymanski.wiktor.ta.domain.LocationEnum
import pl.szymanski.wiktor.ta.domain.Rent
import pl.szymanski.wiktor.ta.domain.aggregate.AccommodationId
import pl.szymanski.wiktor.ta.domain.aggregate.BookingId
import java.util.UUID

@Serializable
sealed interface AccommodationEvent : DomainEvent {
    val accommodationId: AccommodationId
}

@Serializable
data class AccommodationCreatedEvent(
    @Serializable(with = UUIDSerializer::class)
    override val eventId: UUID = UUID.randomUUID(),
    override val accommodationId: AccommodationId,
    val name: String,
    val location: LocationEnum,
    val rent: Rent,
) : AccommodationEvent

@Serializable
data class AccommodationBookedEvent(
    @Serializable(with = UUIDSerializer::class)
    override val eventId: UUID = UUID.randomUUID(),
    override val accommodationId: AccommodationId,
    val bookingId: BookingId,
) : AccommodationEvent

@Serializable
data class AccommodationBookingCanceledEvent(
    @Serializable(with = UUIDSerializer::class)
    override val eventId: UUID = UUID.randomUUID(),
    override val accommodationId: AccommodationId,
    val bookingId: BookingId,
) : AccommodationEvent

@Serializable
data class AccommodationExpiredEvent(
    @Serializable(with = UUIDSerializer::class)
    override val eventId: UUID = UUID.randomUUID(),
    override val accommodationId: AccommodationId,
) : AccommodationEvent

@Serializable
data class AccommodationBookedCompensatedEvent(
    @Serializable(with = UUIDSerializer::class)
    override val eventId: UUID = UUID.randomUUID(),
    override val accommodationId: AccommodationId,
    val bookingId: BookingId,
) : AccommodationEvent

@Serializable
data class AccommodationBookingCanceledCompensatedEvent(
    @Serializable(with = UUIDSerializer::class)
    override val eventId: UUID = UUID.randomUUID(),
    override val accommodationId: AccommodationId,
    val bookingId: BookingId,
) : AccommodationEvent
