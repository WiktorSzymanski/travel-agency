package pl.szymanski.wiktor.ta.domain.event

import pl.szymanski.wiktor.ta.domain.BookingState
import pl.szymanski.wiktor.ta.domain.Seat
import pl.szymanski.wiktor.ta.domain.aggregate.BookingId
import pl.szymanski.wiktor.ta.domain.aggregate.TravelOfferId
import java.util.UUID

sealed interface BookingEvent : DomainEvent {
    val bookingId: BookingId
}

data class BookingCreatedEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override val bookingId: BookingId,
    val travelOfferId: TravelOfferId,
    val userId: UUID,
    val seat: Seat,
    val state: BookingState = BookingState.NEW
) : BookingEvent

data class ProcessBookingEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override val bookingId: BookingId,
) : BookingEvent

data class CompleteBookingEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override val bookingId: BookingId,
) : BookingEvent

data class BookingCancelRequestedEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override val bookingId: BookingId,
    val travelOfferId: TravelOfferId,
    val seat: Seat,
) : BookingEvent

data class CancelBookingEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override val bookingId: BookingId,
) : BookingEvent

data class ProcessCancelBookingEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override val bookingId: BookingId,
) : BookingEvent

data class FailBookingEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override val bookingId: BookingId,
    val message: String,
) : BookingEvent

data class FailCancelBookingEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override val bookingId: BookingId,
    val message: String,
) : BookingEvent
