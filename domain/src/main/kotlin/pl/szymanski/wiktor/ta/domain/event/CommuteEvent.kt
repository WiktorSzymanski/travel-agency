package pl.szymanski.wiktor.ta.domain.event

import pl.szymanski.wiktor.ta.domain.LocationAndTime
import pl.szymanski.wiktor.ta.domain.Seat
import pl.szymanski.wiktor.ta.domain.aggregate.BookingId
import pl.szymanski.wiktor.ta.domain.aggregate.CommuteId
import java.util.UUID

sealed interface CommuteEvent : DomainEvent {
    val commuteId: CommuteId
}

data class CommuteCreatedEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override val commuteId: CommuteId,
    val name: String,
    val departure: LocationAndTime,
    val arrival: LocationAndTime,
    val seats: List<Seat>,
) : CommuteEvent

data class CommuteBookedEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override val commuteId: CommuteId,
    val bookingId: BookingId,
    val seat: Seat,
) : CommuteEvent

data class CommuteBookingCanceledEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override val commuteId: CommuteId,
    val bookingId: BookingId,
    val seat: Seat,
) : CommuteEvent

data class CommuteExpiredEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override val commuteId: CommuteId,
) : CommuteEvent

data class CommuteFullEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override val commuteId: CommuteId,
) : CommuteEvent

data class CommuteAvailableEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override val commuteId: CommuteId,
) : CommuteEvent

data class CommuteBookedCompensatedEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override val commuteId: CommuteId,
    val bookingId: BookingId,
    val seat: Seat,
) : CommuteEvent

data class CommuteBookingCanceledCompensatedEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override val commuteId: CommuteId,
    val bookingId: BookingId,
    val seat: Seat,
) : CommuteEvent