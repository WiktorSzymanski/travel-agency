package pl.szymanski.wiktor.ta.domain.aggregate

import pl.szymanski.wiktor.ta.domain.AttractionStatusEnum
import pl.szymanski.wiktor.ta.domain.LocationEnum
import pl.szymanski.wiktor.ta.domain.assertEventEquals
import pl.szymanski.wiktor.ta.domain.event.AttractionBookedEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionBookingCanceledEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionExpiredEvent
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

class AttractionTest {
    private lateinit var attraction: Attraction
    private lateinit var bookingId: UUID

    @BeforeTest
    fun setup() {
        attraction =
            Attraction(
                _id = UUID.randomUUID(),
                name = "attraction_name",
                location = LocationEnum.POZNAN,
                date = LocalDateTime.now().plusHours(1),
                capacity = 3,
            )
        bookingId = UUID.randomUUID()
    }

    @Test
    fun book_successfully() {
        val events = attraction.book(bookingId)

        assertEventEquals(
            AttractionBookedEvent(
                attractionId = attraction._id,
                bookingId = bookingId,
            ),
            events.first(),
        )
        assertEquals(1, attraction.bookings.size)
        assertEquals(bookingId, attraction.bookings.first())
    }

    @Test
    fun cannot_book_when_expired() {
        attraction.status = AttractionStatusEnum.EXPIRED
        val ex = assertFailsWith<IllegalArgumentException> { attraction.book(bookingId) }
        assertTrue(ex.message?.contains("not open for booking") == true)
    }

    @Test
    fun cannot_book_twice_by_same_user() {
        attraction.book(bookingId)
        val ex = assertFailsWith<IllegalArgumentException> { attraction.book(bookingId) }
        assertTrue(ex.message?.contains("already signed") == true)
    }

    @Test
    fun cannot_book_when_full() {
        attraction.book(UUID.randomUUID())
        attraction.book(UUID.randomUUID())
        attraction.book(UUID.randomUUID())
        val ex = assertFailsWith<IllegalArgumentException> { attraction.book(bookingId) }
        assertTrue(ex.message?.contains("fully booked") == true)
    }

    @Test
    fun cancel_booking_successfully() {
        attraction.book(bookingId)
        val events = attraction.cancelBooking(bookingId)

        assertEventEquals(
            AttractionBookingCanceledEvent(
                attractionId = attraction._id,
                bookingId = bookingId,
            ),
            events.first(),
        )
        assertTrue(attraction.bookings.none { it == bookingId })
    }

    @Test
    fun cannot_cancel_booking_when_expired() {
        attraction.status = AttractionStatusEnum.EXPIRED
        val ex = assertFailsWith<IllegalArgumentException> { attraction.cancelBooking(bookingId) }
        assertTrue(ex.message?.contains("not in SCHEDULED or FULL status") == true)
    }

    @Test
    fun cannot_cancel_nonexistent_booking() {
        val ex = assertFailsWith<IllegalArgumentException> { attraction.cancelBooking(bookingId) }
        assertTrue(ex.message?.contains("was not signed") == true)
    }

    @Test
    fun expire_successfully_after_date() {
        attraction = attraction.copy(date = LocalDateTime.now().minusMinutes(1))
        val events = attraction.expire()

        assertEventEquals(
            AttractionExpiredEvent(
                attractionId = attraction._id,
            ),
            events.first(),
        )
        assertEquals(AttractionStatusEnum.EXPIRED, attraction.status)
    }

    @Test
    fun cannot_expire_before_date() {
        attraction = attraction.copy(date = LocalDateTime.now().plusMinutes(5))
        val ex = assertFailsWith<IllegalArgumentException> { attraction.expire() }
        assertTrue(ex.message?.contains("cannot expire before its date") == true)
    }
}
