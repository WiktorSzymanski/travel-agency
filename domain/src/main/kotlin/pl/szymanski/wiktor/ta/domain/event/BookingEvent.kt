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
    val state: BookingState = BookingState.NEW
) : BookingEvent

data class ProcessBookingEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override var correlationId: UUID? = null,
    override val bookingId: UUID,
) : BookingEvent

data class CompleteBookingEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override var correlationId: UUID? = null,
    override val bookingId: UUID,
) : BookingEvent

data class BookingCancelRequestedEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override var correlationId: UUID? = null,
    override val bookingId: UUID,
    val travelOfferId: UUID,
    val seat: Seat,
) : BookingEvent

data class CancelBookingEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override var correlationId: UUID? = null,
    override val bookingId: UUID,
) : BookingEvent

data class ProcessCancelBookingEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override var correlationId: UUID? = null,
    override val bookingId: UUID,
) : BookingEvent

data class FailBookingEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override var correlationId: UUID? = null,
    override val bookingId: UUID,
    val message: String,
) : BookingEvent

data class FailCancelBookingEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override var correlationId: UUID? = null,
    override val bookingId: UUID,
    val message: String,
) : BookingEvent
