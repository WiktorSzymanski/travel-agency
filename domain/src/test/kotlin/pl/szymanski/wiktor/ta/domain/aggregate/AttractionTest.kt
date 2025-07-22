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
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class AttractionTest {
    private lateinit var attraction: Attraction
    private lateinit var userId: UUID

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
        userId = UUID.randomUUID()
    }

    @Test
    fun book_successfully() {
        val event = attraction.book(userId)

        assertEventEquals(
            AttractionBookedEvent(
                attractionId = attraction._id,
                userId = userId,
            ),
            event,
        )
        assertEquals(1, attraction.bookings.size)
        assertEquals(userId, attraction.bookings.first().userId)
    }

    @Test
    fun cannot_book_when_expired() {
        attraction.status = AttractionStatusEnum.EXPIRED
        val ex =
            assertFailsWith<IllegalArgumentException> {
                attraction.book(userId)
            }
        assertEquals("Attraction ${attraction._id} is not open for booking", ex.message)
    }

    @Test
    fun cannot_book_twice_by_same_user() {
        attraction.book(userId)
        val ex =
            assertFailsWith<IllegalArgumentException> {
                attraction.book(userId)
            }
        assertEquals("User $userId already booked Attraction ${attraction._id}", ex.message)
    }

    @Test
    fun cannot_book_when_full() {
        attraction.book(UUID.randomUUID())
        attraction.book(UUID.randomUUID())
        attraction.book(UUID.randomUUID())
        val ex =
            assertFailsWith<IllegalArgumentException> {
                attraction.book(userId)
            }
        assertEquals("Attraction ${attraction._id} is fully booked", ex.message)
    }

    @Test
    fun cancel_booking_successfully() {
        attraction.book(userId)
        val event = attraction.cancelBooking(userId)

        assertEventEquals(
            AttractionBookingCanceledEvent(
                attractionId = attraction._id,
                userId = userId,
            ),
            event,
        )
        assertTrue(attraction.bookings.none { it.userId == userId })
    }

    @Test
    fun cannot_cancel_booking_when_expired() {
        attraction.status = AttractionStatusEnum.EXPIRED
        val ex =
            assertFailsWith<IllegalArgumentException> {
                attraction.cancelBooking(userId)
            }
        assertEquals(
            "Cannot cancel booking for Attraction ${attraction._id} not in SCHEDULED status",
            ex.message,
        )
    }

    @Test
    fun cannot_cancel_nonexistent_booking() {
        val ex =
            assertFailsWith<IllegalArgumentException> {
                attraction.cancelBooking(userId)
            }
        assertEquals("User $userId has no booking for Attraction ${attraction._id}", ex.message)
    }

    @Test
    fun expire_successfully_after_date() {
        attraction = attraction.copy(date = LocalDateTime.now().minusMinutes(1))
        val event = attraction.expire()

        assertEventEquals(
            AttractionExpiredEvent(
                attractionId = attraction._id,
            ),
            event,
        )
        assertEquals(AttractionStatusEnum.EXPIRED, attraction.status)
    }

    @Test
    fun cannot_expire_before_date() {
        attraction = attraction.copy(date = LocalDateTime.now().plusMinutes(5))
        val ex =
            assertFailsWith<IllegalArgumentException> {
                attraction.expire()
            }
        assertEquals(
            "Attraction ${attraction._id} cannot expire before its date",
            ex.message,
        )
    }
}
