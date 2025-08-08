package pl.szymanski.wiktor.ta.domain.aggregate

import pl.szymanski.wiktor.ta.domain.CommuteStatusEnum
import pl.szymanski.wiktor.ta.domain.LocationAndTime
import pl.szymanski.wiktor.ta.domain.LocationEnum
import pl.szymanski.wiktor.ta.domain.Seat
import pl.szymanski.wiktor.ta.domain.assertEventEquals
import pl.szymanski.wiktor.ta.domain.event.CommuteBookSeatFailedEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteBookedEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteBookingCanceledEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteCancelBookedSeatFailedEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteExpireFailedEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteExpiredEvent
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CommuteTest {
    private lateinit var commute: Commute
    private val seat1 = Seat("1", "A")
    private val seat2 = Seat("1", "B")
    private val seat3 = Seat("1", "C")
    private val bookingId = UUID.randomUUID()

    @BeforeTest
    fun setup() {
        val departure = LocationAndTime(LocationEnum.POZNAN, LocalDateTime.now().plusSeconds(10))
        val arrival = LocationAndTime(LocationEnum.PARIS, LocalDateTime.now().plusSeconds(20))
        val seats = listOf(seat1, seat2, seat3)
        commute = Commute(UUID.randomUUID(), "commute_name", departure, arrival, seats)
    }

    @Test
    fun book_seat_successfully() {
        val event = commute.bookSeat(bookingId, seat1)

        assertEventEquals(
            CommuteBookedEvent(
                commuteId = commute._id,
                bookingId = bookingId,
                seat = seat1,
            ),
            event,
        )
        assertEquals(1, commute.bookings.size)
        assertTrue(commute.bookings.contains(bookingId.toString()))
    }

    @Test
    fun booking_not_allowed_when_expired() {
        commute.status = CommuteStatusEnum.EXPIRED
        val event = commute.bookSeat(bookingId, seat1)

        assertEventEquals(
            CommuteBookSeatFailedEvent(
                commuteId = commute._id,
                bookingId = bookingId,
                seat = seat1,
                message = "Seat cannot be booked when Commute ${commute._id} not in SCHEDULED status, current status is ${commute.status}",
            ),
            event,
        )
    }

    @Test
    fun cannot_book_unknown_seat() {
        val unknownSeat = Seat("99", "Z")
        val event = commute.bookSeat(bookingId, unknownSeat)

        assertEventEquals(
            CommuteBookSeatFailedEvent(
                commuteId = commute._id,
                bookingId = bookingId,
                seat = unknownSeat,
                message = "Seat $unknownSeat not found in Commute ${commute._id}",
            ),
            event,
        )
    }

    @Test
    fun cannot_book_same_seat_twice() {
        val otherBookingId = UUID.randomUUID()
        commute.bookSeat(bookingId, seat1)
        val event = commute.bookSeat(otherBookingId, seat1)

        assertEventEquals(
            CommuteBookSeatFailedEvent(
                commuteId = commute._id,
                bookingId = otherBookingId,
                seat = seat1,
                message = "Seat $seat1 already booked in Commute ${commute._id}",
            ),
            event,
        )
    }

    @Test
    fun can_cancel_own_booking() {
        val bEvent = commute.bookSeat(bookingId, seat1)
        val cEvent = commute.cancelBookedSeat(bookingId)

        assertEventEquals(
            CommuteBookedEvent(
                commuteId = commute._id,
                bookingId = bookingId,
                seat = seat1,
            ),
            bEvent,
        )
        assertEventEquals(
            CommuteBookingCanceledEvent(
                commuteId = commute._id,
                bookingId = bookingId,
                seat = seat1,
            ),
            cEvent,
        )
        assertFalse(commute.bookings.containsKey(bookingId.toString()))
    }

    @Test
    fun cannot_cancel_non_booked_seats() {
        val event = commute.cancelBookedSeat(bookingId)

        assertEventEquals(
            CommuteCancelBookedSeatFailedEvent(
                commuteId = commute._id,
                bookingId = bookingId,
                message = "No seat assigned for booking $bookingId in Commute ${commute._id}",
            ),
            event,
        )
    }

    @Test
    fun cannot_cancel_others_booking() {
        commute.bookSeat(bookingId, seat1)
        val otherUser = UUID.randomUUID()
        val event = commute.cancelBookedSeat(otherUser)

        assertEventEquals(
            CommuteCancelBookedSeatFailedEvent(
                commuteId = commute._id,
                bookingId = otherUser,
                message = "No seat assigned for booking $otherUser in Commute ${commute._id}",
            ),
            event,
        )
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
        val event = commute.expire()

        assertEventEquals(
            CommuteExpireFailedEvent(
                commuteId = commute._id,
                message = "Commute ${commute._id} cannot expire before its departure time",
            ),
            event,
        )
    }
}
