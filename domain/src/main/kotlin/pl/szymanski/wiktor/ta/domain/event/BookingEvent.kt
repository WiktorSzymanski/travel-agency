package pl.szymanski.wiktor.ta.domain.event

import pl.szymanski.wiktor.ta.domain.BookingState
import pl.szymanski.wiktor.ta.domain.Seat
import java.util.UUID

interface BookingEvent : Event {
    val bookingId: UUID
}

data class BookingCreatedEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override var correlationId: UUID? = null,
    override val bookingId: UUID,
    val travelOfferId: UUID,
    val userId: UUID,
    val seat: Seat,
    val state: BookingState,
) : BookingEvent

data class BookingStateChangedEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override var correlationId: UUID? = null,
    override val bookingId: UUID,
    val userId: UUID,
    val state: BookingState,
    val message: String? = null,
) : BookingEvent

data class BookingCancelRequestedFailedEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override var correlationId: UUID? = null,
    override val bookingId: UUID,
    val message: String,
) : BookingEvent

data class BookingCancelRequestedEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override var correlationId: UUID? = null,
    override val bookingId: UUID,
    val travelOfferId: UUID,
    val seat: Seat,
) : BookingEvent