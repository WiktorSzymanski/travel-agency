package pl.szymanski.wiktor.ta.domain.aggregate

import pl.szymanski.wiktor.ta.domain.CommuteStatusEnum
import pl.szymanski.wiktor.ta.domain.LocationAndTime
import pl.szymanski.wiktor.ta.domain.LocationEnum
import pl.szymanski.wiktor.ta.domain.Seat
import pl.szymanski.wiktor.ta.domain.assertEventEquals
import pl.szymanski.wiktor.ta.domain.event.CommuteAvailableEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteBookedEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteBookingCanceledEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteExpiredEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteFullEvent
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
                commuteId = commute.id,
                bookingId = bookingId,
                seat = seat1,
            ),
            event.first(),
        )
        assertEquals(1, commute.bookings.size)
        assertTrue(commute.bookings.contains(bookingId.toString()))
    }

    @Test
    fun booking_not_allowed_when_expired() {
        commute.status = CommuteStatusEnum.EXPIRED
        assertFailsWith<CommuteBookSeatFailedException> { commute.bookSeat(bookingId, seat1) }
    }

    @Test
    fun cannot_book_unknown_seat() {
        val unknownSeat = Seat("99", "Z")
        assertFailsWith<CommuteBookSeatFailedException> { commute.bookSeat(bookingId, unknownSeat) }
    }

    @Test
    fun cannot_book_same_seat_twice() {
        val otherBookingId = UUID.randomUUID()
        commute.bookSeat(bookingId, seat1)
        assertFailsWith<CommuteBookSeatFailedException> { commute.bookSeat(otherBookingId, seat1) }
    }

    @Test
    fun can_cancel_own_booking() {
        val bEvent = commute.bookSeat(bookingId, seat1)
        val cEvent = commute.cancelBookedSeat(bookingId)

        assertEventEquals(
            CommuteBookedEvent(
                commuteId = commute.id,
                bookingId = bookingId,
                seat = seat1,
            ),
            bEvent.first(),
        )
        assertEventEquals(
            CommuteBookingCanceledEvent(
                commuteId = commute.id,
                bookingId = bookingId,
                seat = seat1,
            ),
            cEvent.first(),
        )
        assertFalse(commute.bookings.containsKey(bookingId.toString()))
    }

    @Test
    fun cannot_cancel_non_booked_seats() {
        assertFailsWith<CommuteCancelBookedSeatFailedException> { commute.cancelBookedSeat(bookingId) }
    }

    @Test
    fun cannot_cancel_others_booking() {
        commute.bookSeat(bookingId, seat1)
        val otherUser = UUID.randomUUID()
        assertFailsWith<CommuteCancelBookedSeatFailedException> { commute.cancelBookedSeat(otherUser) }
    }

    @Test
    fun commute_expire_after_departure_time() {
        val commute =
            commute.copy(
                departure =
                    LocationAndTime(LocationEnum.POZNAN, LocalDateTime.now().minusMinutes(1)),
            )
        val event = commute.expire()

        assertEventEquals(CommuteExpiredEvent(commuteId = commute.id), event.first())
        assertEquals(CommuteStatusEnum.EXPIRED, commute.status)
    }

    @Test
    fun cannot_expire_before_departure_time() {
        commute = commute.copy(departure = LocationAndTime(LocationEnum.POZNAN, LocalDateTime.now().plusMinutes(5)))
        assertFailsWith<CommuteExpireFailedException> { commute.expire() }
    }

    @Test
    fun cannot_expire_when_already_expired() {
        commute =
            commute.copy(
                departure = LocationAndTime(LocationEnum.POZNAN, LocalDateTime.now().minusMinutes(1)),
                status = CommuteStatusEnum.EXPIRED,
            )
        assertFailsWith<CommuteExpireFailedException> { commute.expire() }
    }

    @Test
    fun bookSeat_with_null_seat_assigns_automatically() {
        val events = commute.bookSeat(bookingId, null)

        assertEquals(1, events.size)
        val event = events.first() as CommuteBookedEvent
        assertEquals(commute.id, event.commuteId)
        assertEquals(bookingId, event.bookingId)
        assertEquals(seat1, event.seat) // First available seat
        assertTrue(commute.bookings.containsKey(bookingId.toString()))
    }

    @Test
    fun bookSeat_with_null_seat_fails_when_all_seats_booked() {
        val commute =
            commute.copy(
                bookings =
                    mutableMapOf(
                        UUID.randomUUID().toString() to "1|A",
                        UUID.randomUUID().toString() to "1|B",
                        UUID.randomUUID().toString() to "1|C",
                    ),
            )
        assertFailsWith<CommuteBookSeatFailedException> { commute.bookSeat(bookingId, null) }
    }

    @Test
    fun bookSeat_fills_commute_and_emits_full_event() {
        commute.bookSeat(UUID.randomUUID(), seat1)
        commute.bookSeat(UUID.randomUUID(), seat2)
        val events = commute.bookSeat(bookingId, seat3)

        assertEquals(2, events.size)
        assertEventEquals(
            CommuteBookedEvent(
                commuteId = commute.id,
                bookingId = bookingId,
                seat = seat3,
            ),
            events[0],
        )
        assertEventEquals(
            CommuteFullEvent(
                commuteId = commute.id,
            ),
            events[1],
        )
        assertEquals(CommuteStatusEnum.FULL, commute.status)
    }

    @Test
    fun cancelBookedSeat_from_full_emits_available_event() {
        val id1 = UUID.randomUUID()
        val id2 = UUID.randomUUID()
        commute.bookSeat(id1, seat1)
        commute.bookSeat(id2, seat2)
        commute.bookSeat(bookingId, seat3)

        assertEquals(CommuteStatusEnum.FULL, commute.status)

        val events = commute.cancelBookedSeat(id2)

        assertEquals(2, events.size)
        assertEventEquals(
            CommuteBookingCanceledEvent(
                commuteId = commute.id,
                bookingId = id2,
                seat = seat2,
            ),
            events[0],
        )
        assertEventEquals(
            CommuteAvailableEvent(
                commuteId = commute.id,
            ),
            events[1],
        )
        assertEquals(CommuteStatusEnum.SCHEDULED, commute.status)
    }

    @Test
    fun cancelBookedSeat_fails_when_expired() {
        commute.bookSeat(bookingId, seat1)
        commute.status = CommuteStatusEnum.EXPIRED
        assertFailsWith<CommuteCancelBookedSeatFailedException> { commute.cancelBookedSeat(bookingId) }
    }

    @Test
    fun bookSeat_after_departure_causes_automatic_expiration() {
        commute = commute.copy(departure = LocationAndTime(LocationEnum.POZNAN, LocalDateTime.now().minusMinutes(1)))

        assertFailsWith<CommuteBookSeatFailedException> { commute.bookSeat(bookingId, seat1) }
        assertEquals(CommuteStatusEnum.EXPIRED, commute.status)
    }

    @Test
    fun cancelBookedSeat_after_departure_causes_automatic_expiration() {
        commute.bookSeat(bookingId, seat1)
        commute =
            commute.copy(
                departure = LocationAndTime(LocationEnum.POZNAN, LocalDateTime.now().minusMinutes(1)),
                bookings = commute.bookings,
            )

        assertFailsWith<CommuteCancelBookedSeatFailedException> { commute.cancelBookedSeat(bookingId) }
        assertEquals(CommuteStatusEnum.EXPIRED, commute.status)
    }

    @Test
    fun compensateCancelBookedSeat_successfully() {
        val events = commute.compensateCancelBookedSeat(bookingId, seat1)

        assertEquals(1, events.size)
        assertEventEquals(
            CommuteBookedEvent(
                commuteId = commute.id,
                bookingId = bookingId,
                seat = seat1,
            ),
            events.first(),
        )
        assertTrue(commute.bookings.containsKey(bookingId.toString()))
        assertEquals(seat1.toString(), commute.bookings[bookingId.toString()])
    }

    @Test
    fun compensateCancelBookedSeat_fails_when_seat_not_in_list() {
        val unknownSeat = Seat("99", "Z")
        assertFailsWith<CommuteBookSeatFailedException> { commute.compensateCancelBookedSeat(bookingId, unknownSeat) }
    }

    @Test
    fun compensateCancelBookedSeat_fails_when_seat_already_booked() {
        commute.bookSeat(UUID.randomUUID(), seat1)
        assertFailsWith<CommuteBookSeatFailedException> { commute.compensateCancelBookedSeat(bookingId, seat1) }
    }

    @Test
    fun compensateCancelBookedSeat_fills_commute_and_emits_full_event() {
        commute.bookSeat(UUID.randomUUID(), seat1)
        commute.bookSeat(UUID.randomUUID(), seat2)
        val events = commute.compensateCancelBookedSeat(bookingId, seat3)

        assertEquals(2, events.size)
        assertEventEquals(
            CommuteBookedEvent(
                commuteId = commute.id,
                bookingId = bookingId,
                seat = seat3,
            ),
            events[0],
        )
        assertEventEquals(
            CommuteFullEvent(
                commuteId = commute.id,
            ),
            events[1],
        )
        assertEquals(CommuteStatusEnum.FULL, commute.status)
    }

    @Test
    fun compensateBookSeat_successfully() {
        commute.bookSeat(bookingId, seat1)
        val events = commute.compensateBookSeat(bookingId)

        assertEquals(1, events.size)
        assertEventEquals(
            CommuteBookingCanceledEvent(
                commuteId = commute.id,
                bookingId = bookingId,
                seat = seat1,
            ),
            events.first(),
        )
        assertFalse(commute.bookings.containsKey(bookingId.toString()))
    }

    @Test
    fun compensateBookSeat_fails_when_booking_not_found() {
        assertFailsWith<CommuteCancelBookedSeatFailedException> { commute.compensateBookSeat(bookingId) }
    }

    @Test
    fun compensateBookSeat_from_full_emits_available_event() {
        val id1 = UUID.randomUUID()
        val id2 = UUID.randomUUID()
        commute.bookSeat(id1, seat1)
        commute.bookSeat(id2, seat2)
        commute.bookSeat(bookingId, seat3)

        assertEquals(CommuteStatusEnum.FULL, commute.status)

        val events = commute.compensateBookSeat(id2)

        assertEquals(2, events.size)
        assertEventEquals(
            CommuteBookingCanceledEvent(
                commuteId = commute.id,
                bookingId = id2,
                seat = seat2,
            ),
            events[0],
        )
        assertEventEquals(
            CommuteAvailableEvent(
                commuteId = commute.id,
            ),
            events[1],
        )
        assertEquals(CommuteStatusEnum.SCHEDULED, commute.status)
    }
}
