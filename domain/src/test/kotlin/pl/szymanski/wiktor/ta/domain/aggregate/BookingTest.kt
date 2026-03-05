package pl.szymanski.wiktor.ta.domain.aggregate

import pl.szymanski.wiktor.ta.domain.BookingState
import pl.szymanski.wiktor.ta.domain.PickedSeat
import pl.szymanski.wiktor.ta.domain.Seat
import pl.szymanski.wiktor.ta.domain.assertEventEquals
import pl.szymanski.wiktor.ta.domain.event.BookingCancelRequestedEvent
import pl.szymanski.wiktor.ta.domain.event.BookingCreatedEvent
import pl.szymanski.wiktor.ta.domain.event.CancelBookingEvent
import pl.szymanski.wiktor.ta.domain.event.CompleteBookingEvent
import pl.szymanski.wiktor.ta.domain.event.FailBookingEvent
import pl.szymanski.wiktor.ta.domain.event.FailCancelBookingEvent
import pl.szymanski.wiktor.ta.domain.event.ProcessBookingEvent
import pl.szymanski.wiktor.ta.domain.event.ProcessCancelBookingEvent
import pl.szymanski.wiktor.ta.domain.exception.BookingCancelFailedException
import pl.szymanski.wiktor.ta.domain.exception.BookingCancelRequestFailedException
import pl.szymanski.wiktor.ta.domain.exception.BookingCompleteFailedException
import pl.szymanski.wiktor.ta.domain.exception.BookingEmptyEventListException
import pl.szymanski.wiktor.ta.domain.exception.BookingFailCancellationFailedException
import pl.szymanski.wiktor.ta.domain.exception.BookingFailFailedException
import pl.szymanski.wiktor.ta.domain.exception.BookingMissingCreatedEventException
import pl.szymanski.wiktor.ta.domain.exception.BookingProcessCancellationFailedException
import pl.szymanski.wiktor.ta.domain.exception.BookingProcessFailedException
import java.util.UUID
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class BookingTest {

    private var bookingId: BookingId = BookingId.generate()
    private  var userId: UUID = UUID.randomUUID()
    private val travelOffer: TravelOffer = TravelOffer(
        CommuteId.generate(),
        AccommodationId.generate(),
        AttractionId.generate(),
    )
    private var seat: Seat = PickedSeat("1", "A")

    private val booking: Booking =
        Booking(
            id = bookingId,
            userId = userId,
            travelOffer = travelOffer,
            seat = seat,
        )

    @Test
    fun aggregate_should_return_booking_and_created_event() {
        val (booking, events) = Booking.create(bookingId, userId, seat, travelOffer)

        assertEventEquals(
            BookingCreatedEvent(
                bookingId = booking.id.value!!,
                userId = userId,
                travelOffer = travelOffer,
                seat = seat
            ),
            events,
        )

        assertEquals(BookingState.NEW, booking.status)
        assertEquals(userId, booking.userId)
        assertEquals(travelOffer, booking.travelOffer)
        assertEquals(seat, booking.seat)
    }

    @Test
    fun process_successfully_from_new() {
        val events = booking.process()

        assertEventEquals(
            ProcessBookingEvent(
                bookingId = bookingId.value!!,
            ),
            events.first(),
        )
        assertEquals(BookingState.PROCESSING, booking.status)
    }

    @Test
    fun process_fails_when_not_in_new_state() {
        val booking = booking.copy(status = BookingState.PROCESSING)

        assertFailsWith<BookingProcessFailedException> { booking.process() }
    }

    @Test
    fun complete_successfully_from_processing() {
        val booking = booking.copy(status = BookingState.PROCESSING)
        val events = booking.complete()

        assertEventEquals(
            CompleteBookingEvent(
                bookingId = bookingId.value!!,
            ),
            events.first(),
        )
        assertEquals(BookingState.BOOKED, booking.status)
    }

    @Test
    fun complete_successfully_from_new() {
        val events = booking.complete()

        assertEventEquals(
            CompleteBookingEvent(
                bookingId = bookingId.value!!,
            ),
            events.first(),
        )
        assertEquals(BookingState.BOOKED, booking.status)
    }

    @Test
    fun complete_fails_from_succeeded() {
        val booking = booking.copy(status = BookingState.BOOKED)
        assertFailsWith<BookingCompleteFailedException> { booking.complete() }
    }

    @Test
    fun complete_fails_from_failed() {
        val booking = booking.copy(status = BookingState.FAILED)
        assertFailsWith<BookingCompleteFailedException> { booking.complete() }
    }

    @Test
    fun requestCancel_successfully_from_succeeded() {
        val booking = booking.copy(status = BookingState.BOOKED)
        val events = booking.requestCancel()

        assertEventEquals(
            BookingCancelRequestedEvent(
                bookingId = bookingId.value!!,
                travelOffer = travelOffer,
                seat = seat,
            ),
            events.first(),
        )
        assertEquals(BookingState.CANCEL_REQUESTED, booking.status)
    }

    @Test
    fun requestCancel_fails_when_not_succeeded() {
        assertFailsWith<BookingCancelRequestFailedException> { booking.requestCancel() }
    }

    @Test
    fun cancel_successfully_from_processing_cancellation() {
        val booking = booking.copy(status = BookingState.PROCESSING_CANCELLATION)
        val events = booking.cancel()

        assertEventEquals(
            CancelBookingEvent(
                bookingId = bookingId.value!!,
            ),
            events.first(),
        )
        assertEquals(BookingState.CANCELED, booking.status)
    }

    @Test
    fun cancel_fails_when_not_processing_cancellation() {
        val booking = booking.copy(status = BookingState.CANCEL_REQUESTED)
        assertFailsWith<BookingCancelFailedException> { booking.cancel() }
    }

    @Test
    fun processCancellation_successfully_from_cancel_requested() {
        val booking = booking.copy(status = BookingState.CANCEL_REQUESTED)
        val events = booking.processCancellation()

        assertEventEquals(
            ProcessCancelBookingEvent(
                bookingId = bookingId.value!!,
            ),
            events.first(),
        )
        assertEquals(BookingState.PROCESSING_CANCELLATION, booking.status)
    }

    @Test
    fun processCancellation_fails_when_not_cancel_requested() {
        assertFailsWith<BookingProcessCancellationFailedException> { booking.processCancellation() }
    }

    @Test
    fun fail_successfully_from_processing() {
        val booking = booking.copy(status = BookingState.PROCESSING)
        val message = "Something went wrong"
        val events = booking.fail(message)

        assertEventEquals(
            FailBookingEvent(
                bookingId = bookingId.value!!,
                message = message,
            ),
            events.first(),
        )
        assertEquals(BookingState.FAILED, booking.status)
        assertEquals(message, booking.message)
    }

    @Test
    fun fail_successfully_from_new() {
        val message = "Validation failed"
        val events = booking.fail(message)

        assertEventEquals(
            FailBookingEvent(
                bookingId = bookingId.value!!,
                message = message,
            ),
            events.first(),
        )
        assertEquals(BookingState.FAILED, booking.status)
        assertEquals(message, booking.message)
    }

    @Test
    fun fail_fails_from_succeeded() {
        val booking = booking.copy(status = BookingState.BOOKED)
        val message = "Test error"

        assertFailsWith<BookingFailFailedException> { booking.fail(message) }
    }

    @Test
    fun fail_fails_from_canceled() {
        val booking = booking.copy(status = BookingState.CANCELED)
        val message = "Test error"

        assertFailsWith<BookingFailFailedException> { booking.fail(message) }
    }

    @Test
    fun failCancellation_successfully_from_processing_cancellation() {
        val booking = booking.copy(status = BookingState.PROCESSING_CANCELLATION)
        val message = "Cancellation failed"
        val events = booking.failCancellation(message)

        assertEventEquals(
            FailCancelBookingEvent(
                bookingId = bookingId.value!!,
                message = message,
            ),
            events.first(),
        )
        assertEquals(BookingState.BOOKED, booking.status)
        assertEquals(message, booking.message)
    }

    @Test
    fun failCancellation_successfully_from_cancel_requested() {
        val booking = booking.copy(status = BookingState.CANCEL_REQUESTED)
        val message = "Cannot cancel"
        val events = booking.failCancellation(message)

        assertEventEquals(
            FailCancelBookingEvent(
                bookingId = bookingId.value!!,
                message = message,
            ),
            events.first(),
        )
        assertEquals(BookingState.BOOKED, booking.status)
        assertEquals(message, booking.message)
    }

    @Test
    fun failCancellation_fails_from_succeeded() {
        val booking = booking.copy(status = BookingState.BOOKED)
        val message = "Test error"
        assertFailsWith<BookingFailCancellationFailedException> { booking.failCancellation(message) }
    }

    @Test
    fun failCancellation_fails_from_new() {
        val message = "Test error"
        assertFailsWith<BookingFailCancellationFailedException> { booking.failCancellation(message) }
    }

    @Test
    fun booking_fromEvents_should_handle_empty_and_invalid_first_event() {
        assertFailsWith<BookingEmptyEventListException> { Booking.fromEvents(emptyList()) }
        val invalid = listOf(ProcessBookingEvent(bookingId = bookingId.value!!))
        assertFailsWith<BookingMissingCreatedEventException> { Booking.fromEvents(invalid) }
    }

    @Test
    fun booking_fromEvents_should_rebuild_state() {
        val events = listOf(
            BookingCreatedEvent(bookingId = bookingId.value!!, travelOffer = travelOffer, userId = userId, seat = seat),
            ProcessBookingEvent(bookingId = bookingId.value!!),
            CompleteBookingEvent(bookingId = bookingId.value!!),
            BookingCancelRequestedEvent(bookingId = bookingId.value!!, travelOffer = travelOffer, seat = seat),
            ProcessCancelBookingEvent(bookingId = bookingId.value!!),
            FailCancelBookingEvent(bookingId = bookingId.value!!, message = "Retry later")
        )

        val result = Booking.fromEvents(events)
        assertEquals(bookingId, result.id)
        assertEquals(userId, result.userId)
        assertEquals(travelOffer, result.travelOffer)
        assertEquals(seat, result.seat)
        assertEquals(BookingState.BOOKED, result.status)
        assertEquals("Retry later", result.message)
    }
}
