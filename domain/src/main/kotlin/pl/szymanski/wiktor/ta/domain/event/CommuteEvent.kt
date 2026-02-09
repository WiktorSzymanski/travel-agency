package pl.szymanski.wiktor.ta.domain.event

import kotlinx.serialization.Serializable
import pl.szymanski.wiktor.ta.domain.UUIDSerializer
import pl.szymanski.wiktor.ta.domain.LocationAndTime
import pl.szymanski.wiktor.ta.domain.Seat
import pl.szymanski.wiktor.ta.domain.aggregate.BookingId
import pl.szymanski.wiktor.ta.domain.aggregate.CommuteId
import java.util.UUID

@Serializable
sealed interface CommuteEvent : DomainEvent {
    val commuteId: CommuteId
}

@Serializable
data class CommuteCreatedEvent(
    @Serializable(with = UUIDSerializer::class)
    override val eventId: UUID = UUID.randomUUID(),
    override val commuteId: CommuteId,
    val name: String,
    val departure: LocationAndTime,
    val arrival: LocationAndTime,
    val seats: List<Seat>,
) : CommuteEvent

@Serializable
data class CommuteBookedEvent(
    @Serializable(with = UUIDSerializer::class)
    override val eventId: UUID = UUID.randomUUID(),
    override val commuteId: CommuteId,
    val bookingId: BookingId,
    val seat: Seat,
) : CommuteEvent

@Serializable
data class CommuteBookingCanceledEvent(
    @Serializable(with = UUIDSerializer::class)
    override val eventId: UUID = UUID.randomUUID(),
    override val commuteId: CommuteId,
    val bookingId: BookingId,
    val seat: Seat,
) : CommuteEvent

@Serializable
data class CommuteExpiredEvent(
    @Serializable(with = UUIDSerializer::class)
    override val eventId: UUID = UUID.randomUUID(),
    override val commuteId: CommuteId,
) : CommuteEvent

@Serializable
data class CommuteFullEvent(
    @Serializable(with = UUIDSerializer::class)
    override val eventId: UUID = UUID.randomUUID(),
    override val commuteId: CommuteId,
) : CommuteEvent

@Serializable
data class CommuteAvailableEvent(
    @Serializable(with = UUIDSerializer::class)
    override val eventId: UUID = UUID.randomUUID(),
    override val commuteId: CommuteId,
) : CommuteEvent

@Serializable
data class CommuteBookedCompensatedEvent(
    @Serializable(with = UUIDSerializer::class)
    override val eventId: UUID = UUID.randomUUID(),
    override val commuteId: CommuteId,
    val bookingId: BookingId,
    val seat: Seat,
) : CommuteEvent

@Serializable
data class CommuteBookingCanceledCompensatedEvent(
    @Serializable(with = UUIDSerializer::class)
    override val eventId: UUID = UUID.randomUUID(),
    override val commuteId: CommuteId,
    val bookingId: BookingId,
    val seat: Seat,
) : CommuteEvent
