package pl.szymanski.wiktor.ta.domain.event

import com.fasterxml.jackson.annotation.JsonIgnore
import pl.szymanski.wiktor.ta.domain.BookingState
import pl.szymanski.wiktor.ta.domain.Seat
import pl.szymanski.wiktor.ta.domain.aggregate.TravelOffer
import java.util.UUID

sealed interface BookingEvent : DomainEvent {
    val bookingId: UUID
    @get:JsonIgnore
    override val entityId: UUID get() = bookingId
}

data class BookingCreatedEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override val bookingId: UUID,
    val travelOffer: TravelOffer,
    val userId: UUID,
    val seat: Seat,
    val state: BookingState = BookingState.NEW
) : BookingEvent

data class CompleteBookingEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override val bookingId: UUID,
) : BookingEvent

data class BookingCancelRequestedEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override val bookingId: UUID,
    val travelOffer: TravelOffer,
    val seat: Seat,
) : BookingEvent

data class CancelBookingEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override val bookingId: UUID,
) : BookingEvent

data class FailBookingEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override val bookingId: UUID,
    val message: String,
) : BookingEvent

data class FailCancelBookingEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override val bookingId: UUID,
    val message: String,
) : BookingEvent
