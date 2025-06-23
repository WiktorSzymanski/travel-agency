package aggregates

import Booking
import Rent
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

    @BeforeTest
    fun setup() {
        accommodationId = UUID.randomUUID()
        userId = UUID.randomUUID()
        now = LocalDateTime.now()
        rentFuture = Rent(from = now.plusDays(1), till = now.plusDays(5))
        rentPast = Rent(from = now.minusDays(5), till = now.minusDays(1))
    }

    @Test
    fun book_should_succeed_when_available() {
        val acc = Accommodation(accommodationId, LocationEnum.PARIS, rentFuture)
        acc.book(userId)

        assertEquals(AccommodationStatusEnum.BOOKED, acc.status)
        assertEquals(userId, acc.booking?.userId)
    }

    @Test
    fun book_should_fail_when_not_available() {
        val acc =
            Accommodation(accommodationId, LocationEnum.PARIS, rentFuture, status = AccommodationStatusEnum.BOOKED)

        val ex =
            assertFailsWith<IllegalArgumentException> {
                acc.book(userId)
            }

        assertEquals("Accommodation $accommodationId in not AVAILABLE", ex.message)
    }

    @Test
    fun cancelBooking_should_clear_booking_if_user_matches() {
        val acc =
            Accommodation(
                accommodationId,
                LocationEnum.PARIS,
                rentFuture,
                booking = Booking(userId, now),
                status = AccommodationStatusEnum.BOOKED,
            )

        acc.cancelBooking(userId)
        assertNull(acc.booking)
    }

    @Test
    fun cancelBooking_should_fail_if_not_booked() {
        val acc =
            Accommodation(accommodationId, LocationEnum.PARIS, rentFuture, status = AccommodationStatusEnum.AVAILABLE)

        val ex =
            assertFailsWith<IllegalArgumentException> {
                acc.cancelBooking(userId)
            }

        assertEquals("Accommodation $accommodationId is not BOOKED", ex.message)
    }

    @Test
    fun cancelBooking_should_fail_if_wrong_user() {
        val acc =
            Accommodation(
                accommodationId,
                LocationEnum.PARIS,
                rentFuture,
                booking = Booking(UUID.randomUUID(), now),
                status = AccommodationStatusEnum.BOOKED,
            )

        val ex =
            assertFailsWith<IllegalArgumentException> {
                acc.cancelBooking(userId)
            }

        assertEquals("Accommodation $accommodationId is not BOOKED by user $userId", ex.message)
    }

    @Test
    fun expire_should_succeed_if_available_and_from_is_past() {
        val acc = Accommodation(accommodationId, LocationEnum.PARIS, rentPast)
        acc.expire()
        assertEquals(AccommodationStatusEnum.EXPIRED, acc.status)
    }

    @Test
    fun expire_should_fail_if_available_but_from_in_future() {
        val acc = Accommodation(accommodationId, LocationEnum.PARIS, rentFuture)

        val ex =
            assertFailsWith<IllegalArgumentException> {
                acc.expire()
            }

        assertEquals("Accommodation $accommodationId cannot be expired before its from date", ex.message)
    }

    @Test
    fun expire_should_fail_if_renting_and_till_is_future() {
        val acc =
            Accommodation(accommodationId, LocationEnum.PARIS, rentFuture, status = AccommodationStatusEnum.RENTING)

        val ex =
            assertFailsWith<IllegalArgumentException> {
                acc.expire()
            }

        assertEquals("Accommodation $accommodationId cannot expire while RENTING", ex.message)
    }

    @Test
    fun expire_should_fail_if_in_unexpected_status() {
        val acc =
            Accommodation(accommodationId, LocationEnum.PARIS, rentFuture, status = AccommodationStatusEnum.BOOKED)

        val ex =
            assertFailsWith<IllegalArgumentException> {
                acc.expire()
            }

        assertEquals("Accommodation $accommodationId cannot expire in status BOOKED", ex.message)
    }

    @Test
    fun renting_should_succeed_if_from_in_past_and_booked() {
        val acc =
            Accommodation(
                accommodationId,
                LocationEnum.PARIS,
                rentPast,
                booking = Booking(userId, now),
                status = AccommodationStatusEnum.BOOKED,
            )

        acc.renting()
        assertEquals(AccommodationStatusEnum.RENTING, acc.status)
    }

    @Test
    fun renting_should_fail_if_from_in_future() {
        val acc =
            Accommodation(
                accommodationId,
                LocationEnum.PARIS,
                rentFuture,
                booking = Booking(userId, now),
                status = AccommodationStatusEnum.BOOKED,
            )

        val ex =
            assertFailsWith<IllegalArgumentException> {
                acc.renting()
            }

        assertEquals("Accommodation $accommodationId cannot be in RENTING before ${rentFuture.from}", ex.message)
    }

    @Test
    fun renting_should_fail_if_not_booked() {
        val acc =
            Accommodation(accommodationId, LocationEnum.PARIS, rentPast, status = AccommodationStatusEnum.AVAILABLE)

        val ex =
            assertFailsWith<IllegalArgumentException> {
                acc.renting()
            }

        assertEquals("Accommodation $accommodationId is not BOOKED so it cannot be in RENTING", ex.message)
    }
}
