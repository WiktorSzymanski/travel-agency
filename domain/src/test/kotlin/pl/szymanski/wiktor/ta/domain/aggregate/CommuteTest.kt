package pl.szymanski.wiktor.ta.domain.aggregate

import pl.szymanski.wiktor.ta.domain.CommuteStatusEnum
import pl.szymanski.wiktor.ta.domain.LocationAndTime
import pl.szymanski.wiktor.ta.domain.LocationEnum
import pl.szymanski.wiktor.ta.domain.Seat
import pl.szymanski.wiktor.ta.domain.assertEventEquals
import pl.szymanski.wiktor.ta.domain.event.CommuteBookedEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteBookingCanceledEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteExpiredEvent
import java.lang.Thread.sleep
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
        val departure = LocationAndTime(LocationEnum.POZNAN, LocalDateTime.now().plusSeconds(10))
        val arrival = LocationAndTime(LocationEnum.PARIS, LocalDateTime.now().plusSeconds(20))
        val seats = listOf(seat1, seat2, seat3)
        commute = Commute(UUID.randomUUID(), "commute_name", departure, arrival, seats)
    }

    @Test
    fun book_seat_successfully() {
        val event = commute.bookSeat(seat1, userId)

        assertEventEquals(CommuteBookedEvent(
            commuteId = commute._id,
            userId = userId,
            seat = seat1), event)
        assertEquals(1, commute.bookings.size)
        assertEquals(userId, commute.bookings[seat1.toString()]?.userId)
    }

    @Test
    fun booking_not_allowed_when_expired() {
        commute.status = CommuteStatusEnum.EXPIRED
        val ex =
            assertFailsWith<IllegalArgumentException> {
                commute.bookSeat(seat1, userId)
            }
        assertEquals(
            "Seat cannot be booked when Commute ${commute._id} not in SCHEDULED status",
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
        assertEquals("Seat $unknownSeat not found in Commute ${commute._id}", ex.message)
    }

    @Test
    fun cannot_book_same_seat_twice() {
        commute.bookSeat(seat1, userId)
        val ex =
            assertFailsWith<IllegalArgumentException> {
                commute.bookSeat(seat1, UUID.randomUUID())
            }
        assertTrue(ex.message!!.contains("already booked"))
        assertEquals("Seat $seat1 already booked in Commute ${commute._id}", ex.message)
    }

    @Test
    fun can_cancel_own_booking() {
        val bEvent = commute.bookSeat(seat1, userId)
        val cEvent = commute.cancelBookedSeat(seat1, userId)

        assertEventEquals(CommuteBookedEvent(
            commuteId = commute._id,
            userId = userId,
            seat = seat1), bEvent)
        assertEventEquals(CommuteBookingCanceledEvent(
            commuteId = commute._id,
            userId = userId,
            seat = seat1,
        ), cEvent)
        assertFalse(commute.bookings.containsKey(seat1.toString()))
    }

    @Test
    fun cannot_cancel_non_booked_seats() {
        val ex =
            assertFailsWith<IllegalArgumentException> {
                commute.cancelBookedSeat(seat1, userId)
            }
        assertEquals("Booking for seat $seat1 not found in Commute ${commute._id}", ex.message)
    }

    @Test
    fun cannot_cancel_others_booking() {
        commute.bookSeat(seat1, userId)
        val otherUser = UUID.randomUUID()
        val ex =
            assertFailsWith<IllegalArgumentException> {
                commute.cancelBookedSeat(seat1, otherUser)
            }
        assertEquals("Booking for seat $seat1 in Commute ${commute._id} is owned by other user", ex.message)
    }

    @Test
    fun commute_expire_after_departure_time() {
        val commute =
            commute.copy(
                departure =
                    LocationAndTime(LocationEnum.POZNAN, LocalDateTime.now().minusMinutes(1)),
            )
        val event = commute.expire()

        assertEventEquals(CommuteExpiredEvent(commuteId = commute._id), event)
        assertEquals(CommuteStatusEnum.EXPIRED, commute.status)
    }

    @Test
    fun cannot_expire_before_departure_time() {
        commute = commute.copy(departure = LocationAndTime(LocationEnum.POZNAN, LocalDateTime.now().plusMinutes(5)))
        val ex =
            assertFailsWith<IllegalArgumentException> {
                commute.expire()
            }
        assertEquals("Commute ${commute._id} cannot expire before its departure time", ex.message)
    }
}
