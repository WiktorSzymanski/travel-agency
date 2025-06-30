package pl.szymanski.wiktor.ta.domain.aggregate

import pl.szymanski.wiktor.ta.domain.CommuteStatusEnum
import pl.szymanski.wiktor.ta.domain.LocationAndTime
import pl.szymanski.wiktor.ta.domain.LocationEnum
import pl.szymanski.wiktor.ta.domain.Seat
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CommuteTest {
    private lateinit var commute: Commute
    private val seat1 = Seat("1", "A")
    private val seat2 = Seat("1", "B")
    private val seat3 = Seat("1", "C")
    private val userId = UUID.randomUUID()

    @BeforeTest
    fun setup() {
        val departure = LocationAndTime(LocationEnum.POZNAN, LocalDateTime.now().plusMinutes(10))
        val arrival = LocationAndTime(LocationEnum.PARIS, LocalDateTime.now().plusHours(2))
        val seats = listOf(seat1, seat2, seat3)
        commute = Commute(UUID.randomUUID(), "commute_name", departure, arrival, seats)
    }

    @Test
    fun book_seat_successfully() {
        commute.bookSeat(seat1, userId)
        assertEquals(1, commute.bookings.size)
        assertEquals(userId, commute.bookings[seat1]?.userId)
    }

    @Test
    fun booking_not_allowed_when_not_scheduled() {
        commute.status = CommuteStatusEnum.CANCELLED
        val ex =
            assertFailsWith<IllegalArgumentException> {
                commute.bookSeat(seat1, userId)
            }
        assertEquals(
            "Seat cannot be booked when Commute ${commute.commuteId} not in SCHEDULED status",
            ex.message,
        )
    }

    @Test
    fun cannot_book_unknown_seat() {
        val unknownSeat = Seat("99", "Z")
        val ex =
            assertFailsWith<IllegalArgumentException> {
                commute.bookSeat(unknownSeat, userId)
            }
        assertEquals("Seat $unknownSeat not found in Commute ${commute.commuteId}", ex.message)
    }

    @Test
    fun cannot_book_same_seat_twice() {
        commute.bookSeat(seat1, userId)
        val ex =
            assertFailsWith<IllegalArgumentException> {
                commute.bookSeat(seat1, UUID.randomUUID())
            }
        assertTrue(ex.message!!.contains("already booked"))
        assertEquals("Seat $seat1 already booked in Commute ${commute.commuteId}", ex.message)
    }

    @Test
    fun can_cancel_own_booking() {
        commute.bookSeat(seat1, userId)
        commute.cancelBookedSeat(seat1, userId)
        assertFalse(commute.bookings.containsKey(seat1))
    }

    @Test
    fun cannot_cancel_non_booked_seats() {
        val ex =
            assertFailsWith<IllegalArgumentException> {
                commute.cancelBookedSeat(seat1, userId)
            }
        assertEquals("Booking for seat $seat1 not found in Commute ${commute.commuteId}", ex.message)
    }

    @Test
    fun cannot_cancel_others_booking() {
        commute.bookSeat(seat1, userId)
        val otherUser = UUID.randomUUID()
        val ex =
            assertFailsWith<IllegalArgumentException> {
                commute.cancelBookedSeat(seat1, otherUser)
            }
        assertEquals("Booking for seat $seat1 in Commute ${commute.commuteId} is owned by other user", ex.message)
    }

    @Test
    fun can_cancel_commute_if_less_than_half_booked() {
        commute.bookSeat(seat1, userId)
        commute.cancel()
        assertEquals(CommuteStatusEnum.CANCELLED, commute.status)
    }

    @Test
    fun cannot_cancel_commute_if_half_or_more_booked() {
        commute.bookSeat(seat1, userId)
        commute.bookSeat(seat2, UUID.randomUUID())
        val ex =
            assertFailsWith<IllegalArgumentException> {
                commute.cancel()
            }
        assertEquals(
            "Commute ${commute.commuteId} cannot be cancelled when more than half of seats are booked",
            ex.message,
        )
    }

    @Test
    fun commute_can_depart_after_departure_time() {
        val commute =
            commute.copy(
                departure =
                    LocationAndTime(LocationEnum.POZNAN, LocalDateTime.now().minusMinutes(1)),
            )
        commute.depart()
        assertEquals(CommuteStatusEnum.DEPARTED, commute.status)
    }

    @Test
    fun cannot_depart_before_departure_time() {
        commute = commute.copy(departure = LocationAndTime(LocationEnum.POZNAN, LocalDateTime.now().plusMinutes(5)))
        val ex =
            assertFailsWith<IllegalArgumentException> {
                commute.depart()
            }
        assertTrue(ex.message!!.contains("cannot depart"))
        assertEquals("Commute ${commute.commuteId} cannot depart before its departure time", ex.message)
    }
}
