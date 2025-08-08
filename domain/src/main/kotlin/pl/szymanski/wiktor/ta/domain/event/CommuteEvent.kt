package pl.szymanski.wiktor.ta.domain.event

import pl.szymanski.wiktor.ta.domain.LocationAndTime
import pl.szymanski.wiktor.ta.domain.Seat
import java.util.UUID

interface CommuteFailedEvent : CommuteEvent, FailedEvent

data class CommuteBookedEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override var correlationId: UUID? = null,
    override val commuteId: UUID,
    val bookingId: UUID,
    val seat: Seat,
) : CommuteEvent

data class CommuteFullEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override var correlationId: UUID? = null,
    override val commuteId: UUID,
) : CommuteEvent

data class CommuteAvailableEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override var correlationId: UUID? = null,
    override val commuteId: UUID,
) : CommuteEvent

data class CommuteBookingCanceledEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override var correlationId: UUID? = null,
    override val commuteId: UUID,
    val bookingId: UUID,
    val seat: Seat,
) : CommuteEvent

data class CommuteExpiredEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override var correlationId: UUID? = null,
    override val commuteId: UUID,
) : CommuteEvent

data class CommuteCreatedEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override var correlationId: UUID? = null,
    override val commuteId: UUID,
    val name: String,
    val departure: LocationAndTime,
    val arrival: LocationAndTime,
    val seats: List<Seat>,
) : CommuteEvent

data class CommuteExpireFailedEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override var correlationId: UUID? = null,
    override val commuteId: UUID,
    override val message: String,
) : CommuteFailedEvent

data class CommuteBookSeatFailedEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override var correlationId: UUID? = null,
    override val commuteId: UUID,
    val bookingId: UUID,
    val seat: Seat,
    override val message: String,
) : CommuteFailedEvent

data class CommuteCancelBookedSeatFailedEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override var correlationId: UUID? = null,
    override val commuteId: UUID,
    val bookingId: UUID,
    override val message: String,
) : CommuteFailedEvent
