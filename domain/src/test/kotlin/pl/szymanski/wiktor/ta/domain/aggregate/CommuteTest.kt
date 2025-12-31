package pl.szymanski.wiktor.ta.domain.aggregate

import pl.szymanski.wiktor.ta.domain.CommuteStatusEnum
import pl.szymanski.wiktor.ta.domain.LocationAndTime
import pl.szymanski.wiktor.ta.domain.LocationEnum
import pl.szymanski.wiktor.ta.domain.Seat
import pl.szymanski.wiktor.ta.domain.assertEventEquals
import pl.szymanski.wiktor.ta.domain.exception.*
import pl.szymanski.wiktor.ta.domain.event.CommuteAvailableEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteBookedCompensatedEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteBookedEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteBookingCanceledCompensatedEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteBookingCanceledEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteCreatedEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteExpiredEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteFullEvent
import java.time.LocalDateTime
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CommuteTest {
    private val commuteId = CommuteId.generate()
    private lateinit var commute: Commute
    private val seat1 = Seat.Picked("1", "A")
    private val seat2 = Seat.Picked("1", "B")
    private val seat3 = Seat.Picked("1", "C")
    private val bookingId = BookingId.generate()

    @BeforeTest
    fun setup() {
        val departure = LocationAndTime(LocationEnum.POZNAN, LocalDateTime.now().plusSeconds(10))
        val arrival = LocationAndTime(LocationEnum.PARIS, LocalDateTime.now().plusSeconds(20))
        val seats = listOf(seat1, seat2, seat3)
        commute = Commute(commuteId, "commute_name", departure, arrival, seats)
    }

    @Test
    fun aggregate_should_return_commute_and_created_event() {
        val departure = LocationAndTime(LocationEnum.POZNAN, LocalDateTime.now().plusSeconds(30))
        val arrival = LocationAndTime(LocationEnum.PARIS, LocalDateTime.now().plusSeconds(90))
        val seats = listOf(seat1, seat2, seat3)

        val (commute, events) = Commute.create(
            name = "commute_name",
            departure = departure,
            arrival = arrival,
            seats = seats,
        )

        assertEventEquals(
            CommuteCreatedEvent(
                commuteId = commute.id,
                name = "commute_name",
                departure = departure,
                arrival = arrival,
                seats = seats,
            ),
            events.first(),
        )
        assertEquals("commute_name", commute.name)
        assertEquals(departure, commute.departure)
        assertEquals(arrival, commute.arrival)
        assertEquals(seats, commute.seats)
    }

    @Test
    fun book_seat_successfully() {
        val events = commute.bookSeat(bookingId, seat1)

        assertEventEquals(
            CommuteBookedEvent(
                commuteId = commute.id,
                bookingId = bookingId,
                seat = seat1,
            ),
            events.first(),
        )
        assertEquals(1, commute.bookings.size)
        assertTrue(commute.bookings.contains(bookingId))
    }

    @Test
    fun booking_not_allowed_when_expired() {
        commute.status = CommuteStatusEnum.EXPIRED
        assertFailsWith<CommuteBookSeatFailedException> { commute.bookSeat(bookingId, seat1) }
    }

    @Test
    fun cannot_book_unknown_seat() {
        val unknownSeat = Seat.Picked("99", "Z")
        assertFailsWith<CommuteBookSeatFailedException> { commute.bookSeat(bookingId, unknownSeat) }
    }

    @Test
    fun cannot_book_same_seat_twice() {
        val otherBookingId = BookingId.generate()
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
        assertFalse(commute.bookings.containsKey(bookingId))
    }

    @Test
    fun cannot_cancel_non_booked_seats() {
        assertFailsWith<CommuteCancelBookedSeatFailedException> { commute.cancelBookedSeat(bookingId) }
    }

    @Test
    fun cannot_cancel_others_booking() {
        commute.bookSeat(bookingId, seat1)
        val otherUser = BookingId.generate()
        assertFailsWith<CommuteCancelBookedSeatFailedException> { commute.cancelBookedSeat(otherUser) }
    }

    @Test
    fun commute_expire_after_departure_time() {
        val commute =
            commute.copy(
                departure =
                    LocationAndTime(LocationEnum.POZNAN, LocalDateTime.now().minusMinutes(1)),
            )
        val events = commute.expire()

        assertEventEquals(CommuteExpiredEvent(commuteId = commute.id), events.first())
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
        val events = commute.bookSeat(bookingId, Seat.Any)

        assertEquals(1, events.size)
        val event = events.first() as CommuteBookedEvent
        assertEquals(commute.id, event.commuteId)
        assertEquals(bookingId, event.bookingId)
        assertEquals(seat1, event.seat) // First available seat
        assertTrue(commute.bookings.containsKey(bookingId))
    }

    @Test
    fun bookSeat_with_null_seat_fails_when_all_seats_booked() {
        val commute =
            commute.copy(
                bookings =
                    mutableMapOf(
                        BookingId.generate() to Seat.Picked("1", "A"),
                        BookingId.generate() to Seat.Picked("1", "B"),
                        BookingId.generate() to Seat.Picked("1", "C"),
                    ),
            )
        assertFailsWith<CommuteBookSeatFailedException> { commute.bookSeat(bookingId, Seat.Any) }
    }

    @Test
    fun bookSeat_fills_commute_and_emits_full_event() {
        commute.bookSeat(BookingId.generate(), seat1)
        commute.bookSeat(BookingId.generate(), seat2)
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
        val id1 = BookingId.generate()
        val id2 = BookingId.generate()
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
            CommuteBookingCanceledCompensatedEvent(
                commuteId = commute.id,
                bookingId = bookingId,
                seat = seat1,
            ),
            events.first(),
        )
        assertTrue(commute.bookings.containsKey(bookingId))
        assertEquals(seat1, commute.bookings[bookingId])
    }

    @Test
    fun compensateCancelBookedSeat_fails_when_seat_not_in_list() {
        val unknownSeat = Seat.Picked("99", "Z")
        assertFailsWith<CommuteBookSeatFailedException> { commute.compensateCancelBookedSeat(bookingId, unknownSeat) }
    }

    @Test
    fun compensateCancelBookedSeat_fails_when_seat_already_booked() {
        commute.bookSeat(BookingId.generate(), seat1)
        assertFailsWith<CommuteBookSeatFailedException> { commute.compensateCancelBookedSeat(bookingId, seat1) }
    }

    @Test
    fun compensateCancelBookedSeat_fills_commute_and_emits_full_event() {
        commute.bookSeat(BookingId.generate(), seat1)
        commute.bookSeat(BookingId.generate(), seat2)
        val events = commute.compensateCancelBookedSeat(bookingId, seat3)

        assertEquals(2, events.size)
        assertEventEquals(
            CommuteBookingCanceledCompensatedEvent(
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
            CommuteBookedCompensatedEvent(
                commuteId = commute.id,
                bookingId = bookingId,
                seat = seat1,
            ),
            events.first(),
        )
        assertFalse(commute.bookings.containsKey(bookingId))
    }

    @Test
    fun compensateBookSeat_fails_when_booking_not_found() {
        assertFailsWith<CommuteCancelBookedSeatFailedException> { commute.compensateBookSeat(bookingId) }
    }

    @Test
    fun compensateBookSeat_from_full_emits_available_event() {
        val id1 = BookingId.generate()
        val id2 = BookingId.generate()
        commute.bookSeat(id1, seat1)
        commute.bookSeat(id2, seat2)
        commute.bookSeat(bookingId, seat3)

        assertEquals(CommuteStatusEnum.FULL, commute.status)

        val events = commute.compensateBookSeat(id2)

        assertEquals(2, events.size)
        assertEventEquals(
            CommuteBookedCompensatedEvent(
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
    fun commute_apply_should_map_events_correctly() {
        val id = CommuteId.generate()
        val bookingId = BookingId.generate()
        val seat = Seat.Picked("1", "A")
        val base = Commute(id, "c", LocationAndTime(LocationEnum.MADRID, LocalDateTime.now()), LocationAndTime(LocationEnum.BARCELONA, LocalDateTime.now().plusHours(5)), listOf(seat))

        base.apply(CommuteCreatedEvent(commuteId = id, name = "n", departure = LocationAndTime(LocationEnum.POZNAN, LocalDateTime.now()), arrival = LocationAndTime(LocationEnum.ROME, LocalDateTime.now().plusHours(3)), seats = listOf(seat)))
        assertEquals(id, base.id)
        assertEquals("c", base.name)
        assertEquals(CommuteStatusEnum.SCHEDULED, base.status)
        assertEquals(0, base.bookings.size)

        base.apply(CommuteBookedEvent(commuteId = id, bookingId = bookingId, seat = seat))
        assertEquals<Map<BookingId, Seat>>(mapOf(bookingId to seat), base.bookings.toMap())

        base.apply(CommuteFullEvent(commuteId = id))
        assertEquals(CommuteStatusEnum.FULL, base.status)

        base.apply(CommuteAvailableEvent(commuteId = id))
        assertEquals(CommuteStatusEnum.SCHEDULED, base.status)

        base.apply(CommuteBookingCanceledEvent(commuteId = id, bookingId = bookingId, seat = seat))
        assertEquals(0, base.bookings.size)

        base.apply(CommuteExpiredEvent(commuteId = id))
        assertEquals(CommuteStatusEnum.EXPIRED, base.status)
    }

    @Test
    fun commute_fromEvents_should_handle_empty_and_invalid_first_event() {
        assertFailsWith<CommuteEmptyEventListException> { Commute.fromEvents(emptyList()) }
        val invalid = listOf(CommuteBookedEvent(commuteId = CommuteId.generate(), bookingId = BookingId.generate(), seat = Seat.Picked("1", "B")))
        assertFailsWith<CommuteMissingCreatedEventException> { Commute.fromEvents(invalid) }
    }

    @Test
    fun commute_fromEvents_should_rebuild_state() {
        val id = CommuteId.generate()
        val bookingId = BookingId.generate()
        val seat = Seat.Picked("2", "C")
        val dep = LocationAndTime(LocationEnum.VALENCIA, LocalDateTime.now())
        val arr = LocationAndTime(LocationEnum.MARSEILLE, LocalDateTime.now().plusHours(2))
        val events = listOf(
            CommuteCreatedEvent(commuteId = id, name = "comm", departure = dep, arrival = arr, seats = listOf(seat)),
            CommuteBookedEvent(commuteId = id, bookingId = bookingId, seat = seat),
            CommuteBookingCanceledEvent(commuteId = id, bookingId = bookingId, seat = seat),
        )

        val result = Commute.fromEvents(events)
        assertNotNull(result)
        assertEquals(id, result.id)
        assertEquals("comm", result.name)
        assertEquals(dep, result.departure)
        assertEquals(arr, result.arrival)
        assertEquals(0, result.bookings.size)
        assertEquals(CommuteStatusEnum.SCHEDULED, result.status)
    }

    @Test
    fun commute_apply_compensation_events_should_compensate() {
        commute.apply(CommuteBookedEvent(commuteId = commuteId, bookingId = bookingId, seat = seat1))
        commute.apply(CommuteFullEvent(commuteId = commuteId))
        assertEquals(CommuteStatusEnum.FULL, commute.status)

        commute.apply(CommuteBookedCompensatedEvent(commuteId = commuteId, bookingId = bookingId, seat = seat1))
        assertEquals(0, commute.bookings.size)

        commute.apply(CommuteAvailableEvent(commuteId = commuteId))
        assertEquals(CommuteStatusEnum.SCHEDULED, commute.status)

        commute.apply(CommuteBookingCanceledCompensatedEvent(commuteId = commuteId, bookingId = bookingId, seat = seat1))
        assertEquals(1, commute.bookings.size)
    }

    @Test
    fun commute_apply_compensation_events_should_update_state() {
        commute.apply(CommuteBookedEvent(commuteId = commuteId, bookingId = bookingId, seat = seat1))
        commute.apply(CommuteFullEvent(commuteId = commuteId))
        assertEquals(CommuteStatusEnum.FULL, commute.status)

        commute.apply(CommuteBookingCanceledEvent(commuteId = commuteId, bookingId = bookingId, seat = seat1))
        assertEquals(0, commute.bookings.size)

        commute.apply(CommuteAvailableEvent(commuteId = commuteId))
        assertEquals(CommuteStatusEnum.SCHEDULED, commute.status)

        commute.apply(CommuteBookingCanceledCompensatedEvent(commuteId = commuteId, bookingId = bookingId, seat = seat1))
        assertEquals(1, commute.bookings.size)
    }
}
