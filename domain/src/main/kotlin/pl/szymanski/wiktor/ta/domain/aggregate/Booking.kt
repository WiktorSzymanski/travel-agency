package pl.szymanski.wiktor.ta.domain.aggregate

import pl.szymanski.wiktor.ta.domain.BookingState
import pl.szymanski.wiktor.ta.domain.Seat
import pl.szymanski.wiktor.ta.domain.event.BookingCancelRequestedEvent
import pl.szymanski.wiktor.ta.domain.event.BookingCancelRequestedFailedEvent
import pl.szymanski.wiktor.ta.domain.event.BookingCreatedEvent
import pl.szymanski.wiktor.ta.domain.event.BookingEvent
import pl.szymanski.wiktor.ta.domain.event.BookingStateChangedEvent
import java.time.LocalDateTime
import java.util.UUID

data class Booking(
    val _id: UUID = UUID.randomUUID(),
    val userId: UUID,
    val travelOfferId: UUID,
    val seat: Seat,
    var status: BookingState = BookingState.NEW,
    var message: String? = null,
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val version: Int = 1,
) {
    companion object {
        fun create(
            userId: UUID,
            seat: Seat,
            travelOfferId: UUID,
        ): Pair<Booking, BookingCreatedEvent> {
            val booking = Booking(
                userId = userId,
                travelOfferId = travelOfferId,
                seat = seat,
            )

            val event = BookingCreatedEvent(
                bookingId = booking._id,
                travelOfferId = travelOfferId,
                userId = userId,
                seat = seat,
                state = booking.status,
            )

            return Pair(booking, event)
        }
    }

    fun requestCancel() : BookingEvent {
        if (this.status != BookingState.SUCCEEDED) {
            return BookingCancelRequestedFailedEvent(
                bookingId = _id,
                message = "Booking can only be cancelled in SUCCEEDED state"
            )
        }

        this.status = BookingState.CANCEL_REQUESTED

        return BookingCancelRequestedEvent(
            bookingId = _id,
            travelOfferId = travelOfferId,
            seat = seat
        )
    }

    fun changeState(newState: BookingState, message: String? = null): BookingEvent {
        this.status = newState
        this.message = message

        return BookingStateChangedEvent(
            bookingId = _id,
            userId = userId,
            state = newState,
            message = message,
        )
    }
}