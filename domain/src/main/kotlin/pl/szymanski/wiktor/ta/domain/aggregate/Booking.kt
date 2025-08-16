package pl.szymanski.wiktor.ta.domain.aggregate

import pl.szymanski.wiktor.ta.domain.BookingState
import pl.szymanski.wiktor.ta.domain.Seat
import pl.szymanski.wiktor.ta.domain.event.BookingCancelRequestedEvent
import pl.szymanski.wiktor.ta.domain.event.BookingCancelRequestedFailedEvent
import pl.szymanski.wiktor.ta.domain.event.BookingCreatedEvent
import pl.szymanski.wiktor.ta.domain.event.BookingEvent
import pl.szymanski.wiktor.ta.domain.event.CancelBookingEvent
import pl.szymanski.wiktor.ta.domain.event.CancelBookingFailedEvent
import pl.szymanski.wiktor.ta.domain.event.CompleteBookingEvent
import pl.szymanski.wiktor.ta.domain.event.CompleteBookingFailedEvent
import pl.szymanski.wiktor.ta.domain.event.FailBookingEvent
import pl.szymanski.wiktor.ta.domain.event.FailBookingFailedEvent
import pl.szymanski.wiktor.ta.domain.event.FailCancelBookingEvent
import pl.szymanski.wiktor.ta.domain.event.FailCancelBookingFailedEvent
import pl.szymanski.wiktor.ta.domain.event.ProcessBookingEvent
import pl.szymanski.wiktor.ta.domain.event.ProcessBookingFailedEvent
import pl.szymanski.wiktor.ta.domain.event.ProcessCancelBookingEvent
import pl.szymanski.wiktor.ta.domain.event.ProcessCancelBookingFailedEvent
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
            return ProcessBookingFailedEvent(
                bookingId = _id,
                message = "Booking can only be processed in NEW state"
            )
        }

        this.status = BookingState.PROCESSING
        return ProcessBookingEvent(
            bookingId = _id,
        )
    }

    fun complete() : BookingEvent {
        if (!listOf(BookingState.PROCESSING, BookingState.NEW).contains(this.status)) {
            return CompleteBookingFailedEvent(
                bookingId = _id,
                message = "Booking can only be completed in PROCESSING state"
            )
        }

        this.status = BookingState.SUCCEEDED
        return CompleteBookingEvent(
            bookingId = _id,
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
            return CancelBookingFailedEvent(
                bookingId = _id,
                message = "Booking can only be cancelled in PROCESSING or NEW state"
            )
        }

        this.status = BookingState.CANCELED
        return CancelBookingEvent(
            bookingId = _id,
        )
    }

    fun processCancellation() : BookingEvent {
        if (this.status != BookingState.CANCEL_REQUESTED) {
            return ProcessCancelBookingFailedEvent(
                bookingId = _id,
                message = "Booking cancelation can only be process in CANCEL_REQUESTED state"
            )
        }

        this.status = BookingState.PROCESSING_CANCELLATION

        return ProcessCancelBookingEvent(
            bookingId = _id,
        )
    }

    fun fail(message: String) : BookingEvent {
        if (!listOf(BookingState.PROCESSING, BookingState.NEW).contains(this.status)) {
            return FailBookingFailedEvent(
                bookingId = _id,
                message = "Booking can only be failed in PROCESSING or NEW state. Original message: $message"
            )
        }

        this.status = BookingState.FAILED
        this.message = message

        return FailBookingEvent(
            bookingId = _id,
            message = message
        )
    }

    fun failCancellation(message: String) : BookingEvent {
        if (!listOf(BookingState.PROCESSING_CANCELLATION, BookingState.CANCEL_REQUESTED).contains(this.status)) {
            return FailCancelBookingFailedEvent(
                bookingId = _id,
                message = "Cancel Booking can only be failed in PROCESSING_CANCELLATION or CANCEL_REQUESTED state. Original message: $message"
            )
        }

        this.status = BookingState.SUCCEEDED
        this.message = message

        return FailCancelBookingEvent(
            bookingId = _id,
            message = message
        )
    }
}