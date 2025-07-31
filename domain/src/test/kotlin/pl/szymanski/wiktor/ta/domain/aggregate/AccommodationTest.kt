package pl.szymanski.wiktor.ta.domain.aggregate

import pl.szymanski.wiktor.ta.domain.AccommodationStatusEnum
import pl.szymanski.wiktor.ta.domain.LocationEnum
import pl.szymanski.wiktor.ta.domain.Rent
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
    private lateinit var userId: UUID
    private lateinit var now: LocalDateTime
    private lateinit var rentFuture: Rent
    private lateinit var rentPast: Rent
    private lateinit var accommodation: Accommodation

    @BeforeTest
    fun setup() {
        accommodationId = UUID.randomUUID()
        userId = UUID.randomUUID()
        now = LocalDateTime.now()
        rentFuture = Rent(from = now.plusSeconds(1), till = now.plusSeconds(5))
        rentPast = Rent(from = now.minusSeconds(5), till = now.minusSeconds(1))

        accommodation = Accommodation(accommodationId, "accommodation_name", LocationEnum.PARIS, rentFuture)
    }

    @Test
    fun book_should_succeed_when_available() {
        val event = accommodation.book(userId)

        assertEventEquals(
            AccommodationBookedEvent(
                accommodationId = accommodationId,
                userId = userId,
            ),
            event,
        )
        assertEquals(AccommodationStatusEnum.BOOKED, accommodation.status)
        assertEquals(userId, accommodation.booking?.userId)
    }

    @Test
    fun book_should_fail_when_not_available() {
        val accommodation = accommodation.copy(status = AccommodationStatusEnum.BOOKED)

        val ex =
            assertFailsWith<IllegalArgumentException> {
                accommodation.book(userId)
            }

        assertEquals("Accommodation $accommodationId is not AVAILABLE", ex.message)
    }

    @Test
    fun cancelBooking_should_clear_booking_if_user_matches() {
        accommodation.book(userId)
        val event = accommodation.cancelBooking(userId)

        assertEventEquals(
            AccommodationBookingCanceledEvent(
                accommodationId = accommodationId,
                userId = userId,
            ),
            event,
        )
        assertNull(accommodation.booking)
    }

    @Test
    fun cancelBooking_should_fail_if_not_booked() {
        val ex =
            assertFailsWith<IllegalArgumentException> {
                accommodation.cancelBooking(userId)
            }

        assertEquals("Accommodation $accommodationId is not BOOKED", ex.message)
    }

    @Test
    fun cancelBooking_should_fail_if_wrong_user() {
        accommodation.book(UUID.randomUUID())

        val ex =
            assertFailsWith<IllegalArgumentException> {
                accommodation.cancelBooking(userId)
            }

        assertEquals("Accommodation $accommodationId is not BOOKED by user $userId", ex.message)
    }

    @Test
    fun expire_should_succeed_if_available_and_from_is_past() {
        val accommodation = accommodation.copy(rent = rentPast)
        val event = accommodation.expire()

        assertEventEquals(
            AccommodationExpiredEvent(
                accommodationId = accommodationId,
            ),
            event,
        )
        assertEquals(AccommodationStatusEnum.EXPIRED, accommodation.status)
    }

    @Test
    fun expire_should_fail_if_available_but_rent_date_not_met() {
        val ex =
            assertFailsWith<IllegalArgumentException> {
                accommodation.expire()
            }

        assertEquals("Accommodation $accommodationId cannot be expired before its rent start", ex.message)
    }

    @Test
    fun expire_should_fail_if_in_unexpected_status() {
        val accommodation = accommodation.copy(status = AccommodationStatusEnum.BOOKED)

        val ex =
            assertFailsWith<IllegalArgumentException> {
                accommodation.expire()
            }

        assertEquals("Accommodation $accommodationId cannot expire in status BOOKED", ex.message)
    }
}
