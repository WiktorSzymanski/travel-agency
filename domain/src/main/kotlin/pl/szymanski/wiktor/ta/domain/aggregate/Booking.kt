package pl.szymanski.wiktor.ta.domain.aggregate

import pl.szymanski.wiktor.ta.domain.BookingState
import pl.szymanski.wiktor.ta.domain.Seat
import pl.szymanski.wiktor.ta.domain.event.BookingCancelRequestedEvent
import pl.szymanski.wiktor.ta.domain.event.BookingCreatedEvent
import pl.szymanski.wiktor.ta.domain.event.BookingEvent
import pl.szymanski.wiktor.ta.domain.event.CancelBookingEvent
import pl.szymanski.wiktor.ta.domain.event.CompleteBookingEvent
import pl.szymanski.wiktor.ta.domain.event.FailBookingEvent
import pl.szymanski.wiktor.ta.domain.event.FailCancelBookingEvent
import pl.szymanski.wiktor.ta.domain.event.ProcessBookingEvent
import pl.szymanski.wiktor.ta.domain.event.ProcessCancelBookingEvent
import java.time.LocalDateTime
import java.util.UUID

data class Booking(
    val id: UUID = UUID.randomUUID(),
    val userId: UUID,
    val travelOfferId: UUID,
    val seat: Seat?,
    var status: BookingState = BookingState.NEW,
    var message: String? = null,
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val version: Int = 1,
) {
    companion object {
        fun create(
            userId: UUID,
            seat: Seat? = null,
            travelOfferId: UUID,
        ): Pair<Booking, BookingCreatedEvent> {
            val booking = Booking(
                userId = userId,
                travelOfferId = travelOfferId,
                seat = seat,
            )

            val event = BookingCreatedEvent(
                bookingId = booking.id,
                travelOfferId = travelOfferId,
                userId = userId,
                seat = seat,
                state = booking.status,
            )

            return Pair(booking, event)
        }
    }

    fun process() : BookingEvent {
        require(this.status == BookingState.NEW) {
            "Booking can only be processed in NEW state"
        }

        this.status = BookingState.PROCESSING
        return ProcessBookingEvent(
            bookingId = id,
        )
    }

    fun complete() : BookingEvent {
        require(listOf(BookingState.PROCESSING, BookingState.NEW).contains(this.status)) {
            "Booking can only be completed in PROCESSING state"
        }

        this.status = BookingState.SUCCEEDED
        return CompleteBookingEvent(
            bookingId = id,
        )
    }

    fun requestCancel() : BookingEvent {
        require(this.status == BookingState.SUCCEEDED) {
            "Booking can only be cancelled in SUCCEEDED state"
        }

        this.status = BookingState.CANCEL_REQUESTED

        return BookingCancelRequestedEvent(
            bookingId = id,
            travelOfferId = travelOfferId,
            seat = seat
        )
    }

    fun cancel() : BookingEvent {
        require(this.status == BookingState.PROCESSING_CANCELLATION) {
            "Booking can only be cancelled in PROCESSING or NEW state"
        }

        this.status = BookingState.CANCELED
        return CancelBookingEvent(
            bookingId = id,
        )
    }

    fun processCancellation() : BookingEvent {
        require(this.status == BookingState.CANCEL_REQUESTED) {
            "Booking cancelation can only be process in CANCEL_REQUESTED state"
        }

        this.status = BookingState.PROCESSING_CANCELLATION

        return ProcessCancelBookingEvent(
            bookingId = id,
        )
    }

    fun fail(message: String) : BookingEvent {
        require(listOf(BookingState.PROCESSING, BookingState.NEW).contains(this.status)) {
            "Booking can only be failed in PROCESSING or NEW state. Original message: $message"
        }

        this.status = BookingState.FAILED
        this.message = message

        return FailBookingEvent(
            bookingId = id,
            message = message
        )
    }

    fun failCancellation(message: String) : BookingEvent {
        require(listOf(BookingState.PROCESSING_CANCELLATION, BookingState.CANCEL_REQUESTED).contains(this.status)) {
            "Cancel Booking can only be failed in PROCESSING_CANCELLATION or CANCEL_REQUESTED state. Original message: $message"
        }

        this.status = BookingState.SUCCEEDED
        this.message = message

        return FailCancelBookingEvent(
            bookingId = id,
            message = message
        )
    }
}