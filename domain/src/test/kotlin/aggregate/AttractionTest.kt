package aggregate

import AttractionStatusEnum
import LocationEnum
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
                attractionId = UUID.randomUUID(),
                name = "attraction_name",
                location = LocationEnum.POZNAN,
                date = LocalDateTime.now().plusHours(1),
                capacity = 3,
            )
        userId = UUID.randomUUID()
    }

    @Test
    fun book_successfully() {
        attraction.book(userId)
        assertEquals(1, attraction.bookings.size)
        assertEquals(userId, attraction.bookings.first().userId)
    }

    @Test
    fun cannot_book_when_not_scheduled() {
        attraction.status = AttractionStatusEnum.CANCELLED
        val ex =
            assertFailsWith<IllegalArgumentException> {
                attraction.book(userId)
            }
        assertEquals("Attraction ${attraction.attractionId} is not open for booking", ex.message)
    }

    @Test
    fun cannot_book_twice_by_same_user() {
        attraction.book(userId)
        val ex =
            assertFailsWith<IllegalArgumentException> {
                attraction.book(userId)
            }
        assertEquals("User $userId already booked Attraction ${attraction.attractionId}", ex.message)
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
        assertEquals("Attraction ${attraction.attractionId} is fully booked", ex.message)
    }

    @Test
    fun cancel_booking_successfully() {
        attraction.book(userId)
        attraction.cancelBooking(userId)
        assertTrue(attraction.bookings.none { it.userId == userId })
    }

    @Test
    fun cannot_cancel_booking_when_not_scheduled() {
        attraction.status = AttractionStatusEnum.CANCELLED
        val ex =
            assertFailsWith<IllegalArgumentException> {
                attraction.cancelBooking(userId)
            }
        assertEquals(
            "Cannot cancel booking for Attraction ${attraction.attractionId} not in SCHEDULED status",
            ex.message,
        )
    }

    @Test
    fun cannot_cancel_nonexistent_booking() {
        val ex =
            assertFailsWith<IllegalArgumentException> {
                attraction.cancelBooking(userId)
            }
        assertEquals("User $userId has no booking for Attraction ${attraction.attractionId}", ex.message)
    }

    @Test
    fun cancel_attraction_successfully_when_less_than_half_booked() {
        attraction.book(userId)
        attraction.cancel()
        assertEquals(AttractionStatusEnum.CANCELLED, attraction.status)
    }

    @Test
    fun cannot_cancel_attraction_when_not_scheduled() {
        attraction.status = AttractionStatusEnum.EXPIRED
        val ex =
            assertFailsWith<IllegalArgumentException> {
                attraction.cancel()
            }
        assertEquals(
            "Attraction ${attraction.attractionId} cannot be cancelled when not in SCHEDULED status",
            ex.message,
        )
    }

    @Test
    fun cannot_cancel_attraction_when_half_or_more_booked() {
        attraction.book(UUID.randomUUID())
        attraction.book(UUID.randomUUID())
        val ex =
            assertFailsWith<IllegalArgumentException> {
                attraction.cancel()
            }
        assertEquals(
            "Attraction ${attraction.attractionId} cannot be cancelled when more than half of seats are booked",
            ex.message,
        )
    }

    @Test
    fun expire_successfully_after_date() {
        attraction = attraction.copy(date = LocalDateTime.now().minusMinutes(1))
        attraction.expire()
        assertEquals(AttractionStatusEnum.EXPIRED, attraction.status)
    }

    @Test
    fun cannot_expire_when_not_scheduled() {
        attraction.status = AttractionStatusEnum.CANCELLED
        val ex =
            assertFailsWith<IllegalArgumentException> {
                attraction.expire()
            }
        assertEquals(
            "Attraction ${attraction.attractionId} cannot be cancelled when not in SCHEDULED status",
            ex.message,
        )
    }

    @Test
    fun cannot_expire_before_date() {
        attraction = attraction.copy(date = LocalDateTime.now().plusMinutes(5))
        val ex =
            assertFailsWith<IllegalArgumentException> {
                attraction.expire()
            }
        assertEquals(
            "Attraction ${attraction.attractionId} cannot expire before its date",
            ex.message,
        )
    }
}
