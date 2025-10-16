package pl.szymanski.wiktor.ta.domain.aggregate

import pl.szymanski.wiktor.ta.domain.BookingState
import pl.szymanski.wiktor.ta.domain.Seat
import pl.szymanski.wiktor.ta.domain.assertEventEquals
import pl.szymanski.wiktor.ta.domain.event.BookingCancelRequestedEvent
import pl.szymanski.wiktor.ta.domain.event.CancelBookingEvent
import pl.szymanski.wiktor.ta.domain.event.CompleteBookingEvent
import pl.szymanski.wiktor.ta.domain.event.FailBookingEvent
import pl.szymanski.wiktor.ta.domain.event.FailCancelBookingEvent
import pl.szymanski.wiktor.ta.domain.event.ProcessBookingEvent
import pl.szymanski.wiktor.ta.domain.event.ProcessCancelBookingEvent
import java.util.UUID
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class BookingTest {
    private lateinit var booking: Booking
    private lateinit var bookingId: UUID
    private lateinit var userId: UUID
    private lateinit var travelOfferId: UUID
    private lateinit var seat: Seat

    @BeforeTest
    fun setup() {
        bookingId = UUID.randomUUID()
        userId = UUID.randomUUID()
        travelOfferId = UUID.randomUUID()
        seat = Seat("1", "A")
        booking =
            Booking(
                id = bookingId,
                userId = userId,
                travelOfferId = travelOfferId,
                seat = seat,
            )
    }

    @Test
    fun process_successfully_from_new() {
        val event = booking.process()

        assertEventEquals(
            ProcessBookingEvent(
                bookingId = bookingId,
            ),
            event,
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
        val event = booking.complete()

        assertEventEquals(
            CompleteBookingEvent(
                bookingId = bookingId,
            ),
            event,
        )
        assertEquals(BookingState.SUCCEEDED, booking.status)
    }

    @Test
    fun complete_successfully_from_new() {
        val event = booking.complete()

        assertEventEquals(
            CompleteBookingEvent(
                bookingId = bookingId,
            ),
            event,
        )
        assertEquals(BookingState.SUCCEEDED, booking.status)
    }

    @Test
    fun complete_fails_from_succeeded() {
        val booking = booking.copy(status = BookingState.SUCCEEDED)
        assertFailsWith<BookingCompleteFailedException> { booking.complete() }
    }

    @Test
    fun complete_fails_from_failed() {
        val booking = booking.copy(status = BookingState.FAILED)
        assertFailsWith<BookingCompleteFailedException> { booking.complete() }
    }

    @Test
    fun requestCancel_successfully_from_succeeded() {
        val booking = booking.copy(status = BookingState.SUCCEEDED)
        val event = booking.requestCancel()

        assertEventEquals(
            BookingCancelRequestedEvent(
                bookingId = bookingId,
                travelOfferId = travelOfferId,
                seat = seat,
            ),
            event,
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
        val event = booking.cancel()

        assertEventEquals(
            CancelBookingEvent(
                bookingId = bookingId,
            ),
            event,
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
        val event = booking.processCancellation()

        assertEventEquals(
            ProcessCancelBookingEvent(
                bookingId = bookingId,
            ),
            event,
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
        val event = booking.fail(message)

        assertEventEquals(
            FailBookingEvent(
                bookingId = bookingId,
                message = message,
            ),
            event,
        )
        assertEquals(BookingState.FAILED, booking.status)
        assertEquals(message, booking.message)
    }

    @Test
    fun fail_successfully_from_new() {
        val message = "Validation failed"
        val event = booking.fail(message)

        assertEventEquals(
            FailBookingEvent(
                bookingId = bookingId,
                message = message,
            ),
            event,
        )
        assertEquals(BookingState.FAILED, booking.status)
        assertEquals(message, booking.message)
    }

    @Test
    fun fail_fails_from_succeeded() {
        val booking = booking.copy(status = BookingState.SUCCEEDED)
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
        val event = booking.failCancellation(message)

        assertEventEquals(
            FailCancelBookingEvent(
                bookingId = bookingId,
                message = message,
            ),
            event,
        )
        assertEquals(BookingState.SUCCEEDED, booking.status)
        assertEquals(message, booking.message)
    }

    @Test
    fun failCancellation_successfully_from_cancel_requested() {
        val booking = booking.copy(status = BookingState.CANCEL_REQUESTED)
        val message = "Cannot cancel"
        val event = booking.failCancellation(message)

        assertEventEquals(
            FailCancelBookingEvent(
                bookingId = bookingId,
                message = message,
            ),
            event,
        )
        assertEquals(BookingState.SUCCEEDED, booking.status)
        assertEquals(message, booking.message)
    }

    @Test
    fun failCancellation_fails_from_succeeded() {
        val booking = booking.copy(status = BookingState.SUCCEEDED)
        val message = "Test error"
        assertFailsWith<BookingFailCancellationFailedException> { booking.failCancellation(message) }
    }

    @Test
    fun failCancellation_fails_from_new() {
        val message = "Test error"
        assertFailsWith<BookingFailCancellationFailedException> { booking.failCancellation(message) }
    }
}
