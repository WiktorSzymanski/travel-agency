package pl.szymanski.wiktor.ta.domain.aggregate

import pl.szymanski.wiktor.ta.domain.AccommodationStatusEnum
import pl.szymanski.wiktor.ta.domain.LocationEnum
import pl.szymanski.wiktor.ta.domain.Rent
import pl.szymanski.wiktor.ta.domain.exception.*
import pl.szymanski.wiktor.ta.domain.assertEventEquals
import pl.szymanski.wiktor.ta.domain.event.AccommodationBookedEvent
import pl.szymanski.wiktor.ta.domain.event.AccommodationBookingCanceledEvent
import pl.szymanski.wiktor.ta.domain.event.AccommodationExpiredEvent
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class AccommodationTest {
    private lateinit var accommodationId: UUID
    private lateinit var bookingId: UUID
    private lateinit var now: LocalDateTime
    private lateinit var rentFuture: Rent
    private lateinit var rentPast: Rent
    private lateinit var accommodation: Accommodation

    @BeforeTest
    fun setup() {
        accommodationId = UUID.randomUUID()
        bookingId = UUID.randomUUID()
        now = LocalDateTime.now()
        rentFuture = Rent(from = now.plusSeconds(1), till = now.plusSeconds(5))
        rentPast = Rent(from = now.minusSeconds(5), till = now.minusSeconds(1))

        accommodation = Accommodation(accommodationId, "accommodation_name", LocationEnum.PARIS, rentFuture)
    }

    @Test
    fun book_should_succeed_when_available() {
        val events = accommodation.book(bookingId)

        assertEquals(1, events.size)
        assertEventEquals(
            AccommodationBookedEvent(
                accommodationId = accommodationId,
                bookingId = bookingId,
            ),
            events.first(),
        )
        assertEquals(AccommodationStatusEnum.BOOKED, accommodation.status)
        assertEquals(bookingId, accommodation.bookingId)
    }

    @Test
    fun book_should_fail_when_not_available() {
        val accommodation = accommodation.copy(status = AccommodationStatusEnum.BOOKED)
        assertFailsWith<AccommodationBookingFailedException> { accommodation.book(bookingId) }
    }

    @Test
    fun cancelBooking_should_clear_booking_if_user_matches() {
        accommodation.book(bookingId)
        val events = accommodation.cancelBooking(bookingId)

        assertEquals(1, events.size)
        assertEventEquals(
            AccommodationBookingCanceledEvent(
                accommodationId = accommodationId,
                bookingId = bookingId,
            ),
            events.first(),
        )
        assertNull(accommodation.bookingId)
    }

    @Test
    fun cancelBooking_should_fail_if_not_booked() {
        assertFailsWith<AccommodationBookingCancelFailedException> { accommodation.cancelBooking(bookingId) }
    }

    @Test
    fun cancelBooking_should_fail_if_wrong_user() {
        val randomBookingId = UUID.randomUUID()
        accommodation.book(randomBookingId)

        assertFailsWith<AccommodationBookingCancelFailedException> { accommodation.cancelBooking(bookingId) }
    }

    @Test
    fun expire_should_succeed_if_available_and_from_is_past() {
        val accommodation = accommodation.copy(rent = rentPast)
        val events = accommodation.expire()

        assertEquals(1, events.size)
        assertEventEquals(
            AccommodationExpiredEvent(
                accommodationId = accommodationId,
            ),
            events.first(),
        )
        assertEquals(AccommodationStatusEnum.EXPIRED, accommodation.status)
    }

    @Test
    fun expire_should_fail_if_available_but_rent_date_not_met() {
        assertFailsWith<DomainException> { accommodation.expire() }
    }

    @Test
    fun expire_should_fail_if_in_unexpected_status() {
        val accommodation = accommodation.copy(status = AccommodationStatusEnum.BOOKED)

        assertFailsWith<DomainException> { accommodation.expire() }
    }

    @Test
    fun compensateBook_should_succeed_when_bookingId_matches() {
        accommodation.book(bookingId)
        val events = accommodation.compensateBook(bookingId)

        assertEquals(1, events.size)
        assertEventEquals(
            AccommodationBookingCanceledEvent(
                accommodationId = accommodationId,
                bookingId = bookingId,
            ),
            events.first(),
        )
        assertEquals(AccommodationStatusEnum.AVAILABLE, accommodation.status)
        assertNull(accommodation.bookingId)
    }

    @Test
    fun compensateBook_should_fail_when_bookingId_does_not_match() {
        val randomBookingId = UUID.randomUUID()
        accommodation.book(randomBookingId)

        assertFailsWith<AccommodationBookingCancelFailedException> { accommodation.compensateBook(bookingId) }
    }

    @Test
    fun compensateBook_should_fail_when_no_booking_exists() {
        assertFailsWith<AccommodationBookingCancelFailedException> { accommodation.compensateBook(bookingId) }
    }

    @Test
    fun compensateCancelBooking_should_succeed_when_no_existing_booking() {
        val events = accommodation.compensateCancelBooking(bookingId)

        assertEquals(1, events.size)
        assertEventEquals(
            AccommodationBookedEvent(
                accommodationId = accommodationId,
                bookingId = bookingId,
            ),
            events.first(),
        )
        assertEquals(AccommodationStatusEnum.BOOKED, accommodation.status)
        assertEquals(bookingId, accommodation.bookingId)
    }

    @Test
    fun compensateCancelBooking_should_fail_when_booking_already_exists() {
        val randomBookingId = UUID.randomUUID()
        accommodation.book(randomBookingId)

        assertFailsWith<AccommodationBookingFailedException> { accommodation.compensateCancelBooking(bookingId) }
    }
}
