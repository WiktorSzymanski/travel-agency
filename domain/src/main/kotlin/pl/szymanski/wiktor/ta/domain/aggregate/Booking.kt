package pl.szymanski.wiktor.ta.domain.aggregate

import kotlinx.serialization.Serializable
import pl.szymanski.wiktor.ta.domain.BookingState
import pl.szymanski.wiktor.ta.domain.LocalDateTimeSerializer
import pl.szymanski.wiktor.ta.domain.Seat
import pl.szymanski.wiktor.ta.domain.UUIDSerializer
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
import pl.szymanski.wiktor.ta.domain.exception.BookingEmptyEventListException
import pl.szymanski.wiktor.ta.domain.exception.BookingMissingCreatedEventException
import java.time.LocalDateTime
import java.util.UUID

@Serializable
data class Booking(
    val id: BookingId = BookingId.generate(),
    @Serializable(with = UUIDSerializer::class)
    val userId: UUID,
    val travelOffer: TravelOffer,
    val seat: Seat,
    var status: BookingState = BookingState.NEW,
    var message: String? = null,
    @Serializable(with = LocalDateTimeSerializer::class)
    val timestamp: LocalDateTime = LocalDateTime.now(),
) {
    companion object {
        fun create(
            userId: UUID,
            seat: Seat,
            travelOffer: TravelOffer,
        ): Pair<Booking, BookingCreatedEvent> {
            val booking =
                Booking(
                    userId = userId,
                    travelOffer = travelOffer,
                    seat = seat,
                )

            val event =
                BookingCreatedEvent(
                    bookingId = booking.id,
                    travelOffer = travelOffer,
                    userId = userId,
                    seat = seat
                )

            return booking to event
        }

        fun fromEvents(events: List<BookingEvent>): Booking {
            if (events.isEmpty()) throw BookingEmptyEventListException()

            val createdEvent = events.first()
            if (createdEvent !is BookingCreatedEvent) throw BookingMissingCreatedEventException(events.first().bookingId)

            val booking = Booking(
                id = createdEvent.bookingId,
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
            is ProcessBookingEvent -> this.status = BookingState.PROCESSING
            is CompleteBookingEvent -> this.status = BookingState.BOOKED
            is BookingCancelRequestedEvent -> this.status = BookingState.CANCEL_REQUESTED
            is ProcessCancelBookingEvent -> this.status = BookingState.PROCESSING_CANCELLATION
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

    fun process(): List<BookingEvent> {
        if (this.status != BookingState.NEW) {
            throw BookingProcessFailedException(this.id)
        }

        this.status = BookingState.PROCESSING
        return listOf(
            ProcessBookingEvent(bookingId = id)
        )
    }

    fun complete(): List<BookingEvent> {
        if (!listOf(BookingState.PROCESSING, BookingState.NEW).contains(this.status)) {
            throw BookingCompleteFailedException(this.id)
        }

        this.status = BookingState.BOOKED
        return listOf(CompleteBookingEvent(
            bookingId = id,
        ))
    }

    fun requestCancel(): List<BookingEvent> {
        if (this.status != BookingState.BOOKED) {
            throw BookingCancelRequestFailedException(this.id)
        }

        this.status = BookingState.CANCEL_REQUESTED

        return listOf(BookingCancelRequestedEvent(
            bookingId = id,
            travelOffer = travelOffer,
            seat = seat,
        ))
    }

    fun cancel(): List<BookingEvent> {
        if (this.status != BookingState.PROCESSING_CANCELLATION) {
            throw BookingCancelFailedException(this.id)
        }

        this.status = BookingState.CANCELED
        return listOf(CancelBookingEvent(
            bookingId = id,
        ))
    }

    fun processCancellation(): List<BookingEvent> {
        if (this.status != BookingState.CANCEL_REQUESTED) {
            throw BookingProcessCancellationFailedException(this.id)
        }

        this.status = BookingState.PROCESSING_CANCELLATION

        return listOf(ProcessCancelBookingEvent(
            bookingId = id,
        ))
    }

    fun fail(message: String): List<BookingEvent> {
        if (!listOf(BookingState.PROCESSING, BookingState.NEW).contains(this.status)) {
            throw BookingFailFailedException(this.id, message)
        }

        this.status = BookingState.FAILED
        this.message = message

        return listOf(FailBookingEvent(
            bookingId = id,
            message = message,
        ))
    }

    fun failCancellation(message: String): List<BookingEvent> {
        if (!listOf(BookingState.PROCESSING_CANCELLATION, BookingState.CANCEL_REQUESTED).contains(this.status)) {
            throw BookingFailCancellationFailedException(this.id, message)
        }

        this.status = BookingState.BOOKED
        this.message = message

        return listOf(FailCancelBookingEvent(
            bookingId = id,
            message = message,
        ))
    }
}
