package pl.szymanski.wiktor.ta.domain.aggregate

import pl.szymanski.wiktor.ta.domain.BookingState
import pl.szymanski.wiktor.ta.domain.Seat
import pl.szymanski.wiktor.ta.domain.event.*
import pl.szymanski.wiktor.ta.domain.exception.*
import java.time.LocalDateTime
import java.util.*

data class Booking(
    val id: BookingId = BookingId.generate(),
    val userId: UUID,
    val travelOffer: TravelOffer,
    val seat: Seat,
    var status: BookingState = BookingState.NEW,
    var message: String? = null,
    val timestamp: LocalDateTime = LocalDateTime.now(),
) {
    companion object {
        fun create(
            id: BookingId,
            userId: UUID,
            seat: Seat,
            travelOffer: TravelOffer,
        ): Pair<Booking, BookingCreatedEvent> {
            val booking =
                Booking(
                    id = id,
                    userId = userId,
                    travelOffer = travelOffer,
                    seat = seat,
                )

            val event =
                BookingCreatedEvent(
                    bookingId = booking.id.value!!,
                    travelOffer = travelOffer,
                    userId = userId,
                    seat = seat
                )

            return booking to event
        }

        fun fromEvents(events: List<BookingEvent>): Booking {
            if (events.isEmpty()) throw BookingEmptyEventListException()

            val createdEvent = events.first()
            if (createdEvent !is BookingCreatedEvent) throw BookingMissingCreatedEventException(BookingId.from(events.first().bookingId))

            val booking = Booking(
                id = BookingId.from(createdEvent.bookingId),
                userId = createdEvent.userId,
                travelOffer = createdEvent.travelOffer,
                seat = createdEvent.seat,
            )

            for (event in events.drop(1)) {
                booking.apply(event)
            }

            return booking
        }
    }

    fun apply(event: BookingEvent) {
        when (event) {
            is BookingCreatedEvent -> {}
            is CompleteBookingEvent -> this.status = BookingState.BOOKED
            is BookingCancelRequestedEvent -> this.status = BookingState.CANCEL_REQUESTED
            is CancelBookingEvent -> this.status = BookingState.CANCELED
            is FailBookingEvent -> {
                this.status = BookingState.FAILED
                this.message = event.message
            }
            is FailCancelBookingEvent -> {
                this.status = BookingState.BOOKED
                this.message = event.message
            }
        }
    }

    fun complete(): List<BookingEvent> {
        if (this.status != BookingState.NEW) {
            throw BookingCompleteFailedException(this.id)
        }

        this.status = BookingState.BOOKED
        return listOf(CompleteBookingEvent(
            bookingId = id.value!!,
        ))
    }

    fun requestCancel(): List<BookingEvent> {
        if (this.status != BookingState.BOOKED) {
            throw BookingCancelRequestFailedException(this.id)
        }

        this.status = BookingState.CANCEL_REQUESTED

        return listOf(BookingCancelRequestedEvent(
            bookingId = id.value!!,
            travelOffer = travelOffer,
            seat = seat,
        ))
    }

    fun cancel(): List<BookingEvent> {
        if (this.status != BookingState.CANCEL_REQUESTED) {
            throw BookingCancelFailedException(this.id)
        }

        this.status = BookingState.CANCELED
        return listOf(CancelBookingEvent(
            bookingId = id.value!!,
        ))
    }


    fun fail(message: String): List<BookingEvent> {
        if (this.status != BookingState.NEW) {
            throw BookingFailFailedException(this.id, message)
        }

        this.status = BookingState.FAILED
        this.message = message

        return listOf(FailBookingEvent(
            bookingId = id.value!!,
            message = message,
        ))
    }

    fun failCancellation(message: String): List<BookingEvent> {
        if (this.status != BookingState.CANCEL_REQUESTED) {
            throw BookingFailCancellationFailedException(this.id, message)
        }

        this.status = BookingState.BOOKED
        this.message = message

        return listOf(FailCancelBookingEvent(
            bookingId = id.value!!,
            message = message,
        ))
    }
}
