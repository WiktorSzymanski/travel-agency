package pl.szymanski.wiktor.ta.domain.event

import pl.szymanski.wiktor.ta.domain.BookingState
import pl.szymanski.wiktor.ta.domain.Seat
import java.util.UUID

interface BookingEvent : Event {
    val bookingId: UUID
}

interface BookingFailedEvent : BookingEvent, FailedEvent

data class BookingCreatedEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override var correlationId: UUID? = null,
    override val bookingId: UUID,
    val travelOfferId: UUID,
    val userId: UUID,
    val seat: Seat?,
    val state: BookingState,
) : BookingEvent

data class ProcessBookingEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override var correlationId: UUID? = null,
    override val bookingId: UUID,
) : BookingEvent

data class ProcessBookingFailedEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override var correlationId: UUID? = null,
    override val bookingId: UUID,
    override val message: String,
) : BookingFailedEvent

data class CompleteBookingEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override var correlationId: UUID? = null,
    override val bookingId: UUID,
) : BookingEvent

data class CompleteBookingFailedEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override var correlationId: UUID? = null,
    override val bookingId: UUID,
    override val message: String,
) : BookingFailedEvent

data class BookingCancelRequestedEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override var correlationId: UUID? = null,
    override val bookingId: UUID,
    val travelOfferId: UUID,
    val seat: Seat?,
) : BookingEvent

data class BookingCancelRequestedFailedEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override var correlationId: UUID? = null,
    override val bookingId: UUID,
    val message: String,
) : BookingEvent

data class CancelBookingEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override var correlationId: UUID? = null,
    override val bookingId: UUID,
) : BookingEvent

data class CancelBookingFailedEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override var correlationId: UUID? = null,
    override val bookingId: UUID,
    override val message: String,
) : BookingFailedEvent

data class ProcessCancelBookingEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override var correlationId: UUID? = null,
    override val bookingId: UUID,
) : BookingEvent

data class ProcessCancelBookingFailedEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override var correlationId: UUID? = null,
    override val bookingId: UUID,
    override val message: String,
) : BookingFailedEvent

data class FailBookingEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override var correlationId: UUID? = null,
    override val bookingId: UUID,
    val message: String,
) : BookingEvent

data class FailBookingFailedEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override var correlationId: UUID? = null,
    override val bookingId: UUID,
    override val message: String,
) : BookingFailedEvent

data class FailCancelBookingEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override var correlationId: UUID? = null,
    override val bookingId: UUID,
    val message: String,
) : BookingEvent

data class FailCancelBookingFailedEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override var correlationId: UUID? = null,
    override val bookingId: UUID,
    override val message: String,
) : BookingFailedEvent
