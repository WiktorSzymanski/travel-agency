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
import pl.szymanski.wiktor.ta.domain.exception.BookingCancelFailedException
import pl.szymanski.wiktor.ta.domain.exception.BookingCancelRequestFailedException
import pl.szymanski.wiktor.ta.domain.exception.BookingCompleteFailedException
import pl.szymanski.wiktor.ta.domain.exception.BookingFailCancellationFailedException
import pl.szymanski.wiktor.ta.domain.exception.BookingFailFailedException
import pl.szymanski.wiktor.ta.domain.exception.BookingProcessCancellationFailedException
import pl.szymanski.wiktor.ta.domain.exception.BookingProcessFailedException
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
) {
    companion object {
        fun create(
            userId: UUID,
            seat: Seat? = null,
            travelOfferId: UUID,
        ): Pair<Booking, BookingCreatedEvent> {
            val booking =
                Booking(
                    userId = userId,
                    travelOfferId = travelOfferId,
                    seat = seat,
                )

            val event =
                BookingCreatedEvent(
                    bookingId = booking.id,
                    travelOfferId = travelOfferId,
                    userId = userId,
                    seat = seat,
                    state = booking.status,
                )

            return Pair(booking, event)
        }
    }

    fun process(): BookingEvent {
        if (this.status != BookingState.NEW) {
            throw BookingProcessFailedException()
        }

        this.status = BookingState.PROCESSING
        return ProcessBookingEvent(
            bookingId = id,
        )
    }

    fun complete(): BookingEvent {
        if (!listOf(BookingState.PROCESSING, BookingState.NEW).contains(this.status)) {
            throw BookingCompleteFailedException()
        }

        this.status = BookingState.SUCCEEDED
        return CompleteBookingEvent(
            bookingId = id,
        )
    }

    fun requestCancel(): BookingEvent {
        if (this.status != BookingState.SUCCEEDED) {
            throw BookingCancelRequestFailedException()
        }

        this.status = BookingState.CANCEL_REQUESTED

        return BookingCancelRequestedEvent(
            bookingId = id,
            travelOfferId = travelOfferId,
            seat = seat,
        )
    }

    fun cancel(): BookingEvent {
        if (this.status != BookingState.PROCESSING_CANCELLATION) {
            throw BookingCancelFailedException()
        }

        this.status = BookingState.CANCELED
        return CancelBookingEvent(
            bookingId = id,
        )
    }

    fun processCancellation(): BookingEvent {
        if (this.status != BookingState.CANCEL_REQUESTED) {
            throw BookingProcessCancellationFailedException()
        }

        this.status = BookingState.PROCESSING_CANCELLATION

        return ProcessCancelBookingEvent(
            bookingId = id,
        )
    }

    fun fail(message: String): BookingEvent {
        if (!listOf(BookingState.PROCESSING, BookingState.NEW).contains(this.status)) {
            throw BookingFailFailedException(message)
        }

        this.status = BookingState.FAILED
        this.message = message

        return FailBookingEvent(
            bookingId = id,
            message = message,
        )
    }

    fun failCancellation(message: String): BookingEvent {
        if (!listOf(BookingState.PROCESSING_CANCELLATION, BookingState.CANCEL_REQUESTED).contains(this.status)) {
            throw BookingFailCancellationFailedException(message)
        }

        this.status = BookingState.SUCCEEDED
        this.message = message

        return FailCancelBookingEvent(
            bookingId = id,
            message = message,
        )
    }
}
