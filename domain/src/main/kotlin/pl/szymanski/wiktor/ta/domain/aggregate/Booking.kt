package pl.szymanski.wiktor.ta.domain.aggregate

import pl.szymanski.wiktor.ta.domain.BookingState
import pl.szymanski.wiktor.ta.domain.Seat
import pl.szymanski.wiktor.ta.domain.event.BookingCancelRequestedEvent
import pl.szymanski.wiktor.ta.domain.event.BookingCancelRequestedFailedEvent
import pl.szymanski.wiktor.ta.domain.event.BookingCreatedEvent
import pl.szymanski.wiktor.ta.domain.event.BookingEvent
import pl.szymanski.wiktor.ta.domain.event.BookingStateChangeFailedEvent
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

    fun process() : BookingEvent {
        if (this.status != BookingState.NEW) {
            return BookingStateChangeFailedEvent(
                bookingId = _id,
                userId = userId,
                state = this.status,
                message = "Booking can only be processed in NEW state"
            )
        }

        this.status = BookingState.PROCESSING
        return BookingStateChangedEvent(
            bookingId = _id,
            userId = userId,
            state = BookingState.PROCESSING,
            message = null
        )
    }

    fun complete() : BookingEvent {
        if (this.status != BookingState.PROCESSING) {
            return BookingStateChangeFailedEvent(
                bookingId = _id,
                userId = userId,
                state = this.status,
                message = "Booking can only be completed in PROCESSING state"
            )
        }

        this.status = BookingState.SUCCEEDED
        return BookingStateChangedEvent(
            bookingId = _id,
            userId = userId,
            state = BookingState.SUCCEEDED,
            message = null
        )
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

    fun cancel() : BookingEvent {
        if (this.status != BookingState.PROCESSING_CANCELLATION) {
            return BookingStateChangeFailedEvent(
                bookingId = _id,
                userId = userId,
                state = this.status,
                message = "Booking can only be cancelled in PROCESSING or NEW state"
            )
        }

        this.status = BookingState.CANCELED
        return BookingStateChangedEvent(
            bookingId = _id,
            userId = userId,
            state = BookingState.CANCELED,
            message = null
        )
    }

    fun processCancellation() : BookingEvent {
        if (this.status != BookingState.CANCEL_REQUESTED) {
            return BookingStateChangeFailedEvent(
                bookingId = _id,
                userId = userId,
                state = this.status,
                message = "Booking cancelation can only be process in CANCEL_REQUESTED state"
            )
        }

        this.status = BookingState.PROCESSING_CANCELLATION
        return BookingStateChangedEvent(
            bookingId = _id,
            userId = userId,
            state = BookingState.PROCESSING_CANCELLATION,
            message = null
        )
    }

    fun fail(message: String?) : BookingEvent {
        if (this.status != BookingState.PROCESSING || this.status != BookingState.NEW) {
            return BookingStateChangeFailedEvent(
                bookingId = _id,
                userId = userId,
                state = this.status,
                message = "Booking can only be failed in PROCESSING or NEW state"
            )
        }

        this.status = BookingState.FAILED
        return BookingStateChangedEvent(
            bookingId = _id,
            userId = userId,
            state = BookingState.FAILED,
            message = message
        )
    }

    fun failCancellation(message: String?) : BookingEvent {
        if (this.status != BookingState.PROCESSING_CANCELLATION || this.status != BookingState.CANCEL_REQUESTED) {
            return BookingStateChangeFailedEvent(
                bookingId = _id,
                userId = userId,
                state = this.status,
            )
        }

        this.status = BookingState.SUCCEEDED
        return BookingStateChangedEvent(
            bookingId = _id,
            userId = userId,
            state = BookingState.SUCCEEDED,
            message = message
        )
    }
}