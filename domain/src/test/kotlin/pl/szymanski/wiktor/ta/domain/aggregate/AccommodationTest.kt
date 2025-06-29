package pl.szymanski.wiktor.ta.domain.aggregate

import pl.szymanski.wiktor.ta.domain.AccommodationStatusEnum
import pl.szymanski.wiktor.ta.domain.Booking
import pl.szymanski.wiktor.ta.domain.LocationEnum
import pl.szymanski.wiktor.ta.domain.Rent
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
        rentFuture = Rent(from = now.plusDays(1), till = now.plusDays(5))
        rentPast = Rent(from = now.minusDays(5), till = now.minusDays(1))

        accommodation = Accommodation(accommodationId, "accommodation_name", LocationEnum.PARIS, rentFuture)
    }

    @Test
    fun book_should_succeed_when_available() {
        accommodation.book(userId)

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
        accommodation.cancelBooking(userId)

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
        accommodation.expire()

        assertEquals(AccommodationStatusEnum.EXPIRED, accommodation.status)
    }

    @Test
    fun expire_should_fail_if_available_but_from_in_future() {
        val ex =
            assertFailsWith<IllegalArgumentException> {
                accommodation.expire()
            }

        assertEquals("Accommodation $accommodationId cannot be expired before its from date", ex.message)
    }

    @Test
    fun expire_should_fail_if_renting_and_till_is_future() {
        val accommodation = accommodation.copy(status = AccommodationStatusEnum.RENTING)

        val ex =
            assertFailsWith<IllegalArgumentException> {
                accommodation.expire()
            }

        assertEquals("Accommodation $accommodationId cannot expire while RENTING", ex.message)
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

    @Test
    fun renting_should_succeed_if_from_in_past_and_booked() {
        val accommodation =
            accommodation.copy(
                rent = rentPast,
                booking = Booking(userId, now),
                status = AccommodationStatusEnum.BOOKED,
            )

        accommodation.renting()
        assertEquals(AccommodationStatusEnum.RENTING, accommodation.status)
    }

    @Test
    fun renting_should_fail_if_from_in_future() {
        val accommodation =
            accommodation.copy(
                rent = rentFuture,
                booking = Booking(userId, now),
                status = AccommodationStatusEnum.BOOKED,
            )

        val ex =
            assertFailsWith<IllegalArgumentException> {
                accommodation.renting()
            }

        assertEquals("Accommodation $accommodationId cannot be in RENTING before ${rentFuture.from}", ex.message)
    }

    @Test
    fun renting_should_fail_if_not_booked() {
        val accommodation = accommodation.copy(rent = rentPast)

        val ex =
            assertFailsWith<IllegalArgumentException> {
                accommodation.renting()
            }

        assertEquals("Accommodation $accommodationId is not BOOKED so it cannot be in RENTING", ex.message)
    }
}
