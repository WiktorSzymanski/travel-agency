@file:Suppress("WildcardImport")

package pl.szymanski.wiktor.ta.domain.aggregate

import pl.szymanski.wiktor.ta.domain.AttractionStatusEnum
import pl.szymanski.wiktor.ta.domain.LocationEnum
import pl.szymanski.wiktor.ta.domain.assertEventEquals
import pl.szymanski.wiktor.ta.domain.exception.*
import pl.szymanski.wiktor.ta.domain.event.AttractionAvailableEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionBookedCompensatedEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionBookedEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionBookingCanceledCompensatedEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionBookingCanceledEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionCreatedEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionExpiredEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionFullEvent
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AttractionTest {
    private lateinit var attraction: Attraction
    private lateinit var bookingId: UUID

    @BeforeTest
    fun setup() {
        attraction =
            Attraction(
                id = UUID.randomUUID(),
                name = "attraction_name",
                location = LocationEnum.POZNAN,
                date = LocalDateTime.now().plusHours(1),
                capacity = 3,
            )
        bookingId = UUID.randomUUID()
    }

    @Test
    fun aggregate_should_return_attraction_and_created_event() {
        val (attraction, events) = Attraction.create("attraction_name", LocationEnum.POZNAN, LocalDateTime.now().plusHours(1), 3)

        assertEventEquals(
            AttractionCreatedEvent(
                attractionId = attraction.id,
                name = "attraction_name",
                location = LocationEnum.POZNAN,
                date = attraction.date,
                capacity = 3,
            ),
            events.first()
        )

        assertEquals("attraction_name", attraction.name)
        assertEquals(LocationEnum.POZNAN, attraction.location)
        assertEquals(3, attraction.capacity)
        assertEquals(AttractionStatusEnum.SCHEDULED, attraction.status)
        assertEquals(0, attraction.bookings.size)
    }

    @Test
    fun book_successfully() {
        val events = attraction.book(bookingId)

        assertEventEquals(
            AttractionBookedEvent(
                attractionId = attraction.id,
                bookingId = bookingId,
            ),
            events.first(),
        )
        assertEquals(1, attraction.bookings.size)
        assertEquals(bookingId, attraction.bookings.first())
    }

    @Test
    fun cannot_book_when_expired() {
        attraction.status = AttractionStatusEnum.EXPIRED
        assertFailsWith<AttractionBookFailedException> { attraction.book(bookingId) }
    }

    @Test
    fun cannot_book_twice_by_same_user() {
        attraction.book(bookingId)
        assertFailsWith<AttractionBookFailedException> { attraction.book(bookingId) }
    }

    @Test
    fun cannot_book_when_full() {
        attraction.book(UUID.randomUUID())
        attraction.book(UUID.randomUUID())
        attraction.book(UUID.randomUUID())
        assertFailsWith<AttractionBookFailedException> { attraction.book(bookingId) }
    }

    @Test
    fun cancel_booking_successfully() {
        attraction.book(bookingId)
        val events = attraction.cancelBooking(bookingId)

        assertEventEquals(
            AttractionBookingCanceledEvent(
                attractionId = attraction.id,
                bookingId = bookingId,
            ),
            events.first(),
        )
        assertTrue(attraction.bookings.none { it == bookingId })
    }

    @Test
    fun cannot_cancel_booking_when_expired() {
        attraction.status = AttractionStatusEnum.EXPIRED
        assertFailsWith<AttractionBookingCancelFailedException> { attraction.cancelBooking(bookingId) }
    }

    @Test
    fun cannot_cancel_nonexistent_booking() {
        assertFailsWith<AttractionBookingCancelFailedException> { attraction.cancelBooking(bookingId) }
    }

    @Test
    fun expire_successfully_after_date() {
        attraction = attraction.copy(date = LocalDateTime.now().minusMinutes(1))
        val events = attraction.expire()

        assertEventEquals(
            AttractionExpiredEvent(
                attractionId = attraction.id,
            ),
            events.first(),
        )
        assertEquals(AttractionStatusEnum.EXPIRED, attraction.status)
    }

    @Test
    fun cannot_expire_before_date() {
        attraction = attraction.copy(date = LocalDateTime.now().plusMinutes(5))
        assertFailsWith<AttractionExpireFailedException> { attraction.expire() }
    }

    @Test
    fun booking_fills_attraction_and_emits_full_event() {
        val attraction =
            attraction.copy(
                bookings =
                    mutableListOf(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                    ),
            )
        val someBookingId = UUID.randomUUID()
        val events = attraction.book(someBookingId)

        assertEquals(2, events.size)
        assertEventEquals(
            AttractionBookedEvent(
                attractionId = attraction.id,
                bookingId = someBookingId,
            ),
            events.first(),
        )
        assertEventEquals(
            AttractionFullEvent(
                attractionId = attraction.id,
            ),
            events.last(),
        )
        assertEquals(AttractionStatusEnum.FULL, attraction.status)
        assertEquals(3, attraction.bookings.size)
    }

    @Test
    fun canceling_booking_from_full_attraction_emits_available_event() {
        val id1 = UUID.randomUUID()
        val id2 = UUID.randomUUID()
        val id3 = UUID.randomUUID()
        attraction.book(id1)
        attraction.book(id2)
        attraction.book(id3)

        assertEquals(AttractionStatusEnum.FULL, attraction.status)

        val events = attraction.cancelBooking(id2)

        assertEquals(2, events.size)
        assertEventEquals(
            AttractionBookingCanceledEvent(
                attractionId = attraction.id,
                bookingId = id2,
            ),
            events.first(),
        )
        assertEventEquals(
            AttractionAvailableEvent(
                attractionId = attraction.id,
            ),
            events.last(),
        )
        assertEquals(AttractionStatusEnum.SCHEDULED, attraction.status)
        assertEquals(2, attraction.bookings.size)
    }

    @Test
    fun can_cancel_booking_when_attraction_is_full() {
        val id1 = UUID.randomUUID()
        attraction.book(UUID.randomUUID())
        attraction.book(UUID.randomUUID())
        attraction.book(id1)

        assertEquals(AttractionStatusEnum.FULL, attraction.status)

        val events = attraction.cancelBooking(id1)

        assertEquals(2, events.size)
        assertTrue(attraction.bookings.none { it == id1 })
    }

    @Test
    fun booking_after_date_causes_automatic_expiration() {
        attraction = attraction.copy(date = LocalDateTime.now().minusMinutes(1))

        assertFailsWith<AttractionBookFailedException> { attraction.book(bookingId) }
        assertEquals(AttractionStatusEnum.EXPIRED, attraction.status)
    }

    @Test
    fun canceling_booking_after_date_causes_automatic_expiration() {
        attraction = attraction.copy(date = LocalDateTime.now().plusMinutes(5))
        attraction.book(bookingId)

        attraction = attraction.copy(date = LocalDateTime.now().minusMinutes(1))

        assertFailsWith<AttractionBookingCancelFailedException> {
            attraction.cancelBooking(UUID.randomUUID())
        }
        assertEquals(AttractionStatusEnum.EXPIRED, attraction.status)
    }

    @Test
    fun compensate_book_successfully() {
        attraction.book(bookingId)
        val events = attraction.compensateBook(bookingId)

        assertEventEquals(
            AttractionBookedCompensatedEvent(
                attractionId = attraction.id,
                bookingId = bookingId,
            ),
            events.first(),
        )
        assertTrue(attraction.bookings.none { it == bookingId })
    }

    @Test
    fun compensate_book_fails_when_booking_not_found() {
        assertFailsWith<AttractionBookingCancelFailedException> {
            attraction.compensateBook(bookingId)
        }
    }

    @Test
    fun compensate_book_from_full_attraction_emits_available_event() {
        val id1 = UUID.randomUUID()
        val id2 = UUID.randomUUID()
        val id3 = UUID.randomUUID()
        attraction.book(id1)
        attraction.book(id2)
        attraction.book(id3)

        assertEquals(AttractionStatusEnum.FULL, attraction.status)

        val events = attraction.compensateBook(id2)

        assertEquals(2, events.size)
        assertEventEquals(
            AttractionBookedCompensatedEvent(
                attractionId = attraction.id,
                bookingId = id2,
            ),
            events.first(),
        )
        assertEventEquals(
            AttractionAvailableEvent(
                attractionId = attraction.id,
            ),
            events.last(),
        )
        assertEquals(AttractionStatusEnum.SCHEDULED, attraction.status)
        assertEquals(2, attraction.bookings.size)
    }

    @Test
    fun compensate_cancel_booking_successfully() {
        val events = attraction.compensateCancelBooking(bookingId)

        assertEventEquals(
            AttractionBookingCanceledCompensatedEvent(
                attractionId = attraction.id,
                bookingId = bookingId,
            ),
            events.first(),
        )
        assertEquals(1, attraction.bookings.size)
        assertEquals(bookingId, attraction.bookings.first())
    }

    @Test
    fun compensate_cancel_booking_fails_when_booking_already_exists() {
        attraction.book(bookingId)
        assertFailsWith<AttractionBookFailedException> {
            attraction.compensateCancelBooking(bookingId)
        }
    }

    @Test
    fun compensate_cancel_booking_fails_when_at_capacity() {
        attraction.book(UUID.randomUUID())
        attraction.book(UUID.randomUUID())
        attraction.book(UUID.randomUUID())

        assertFailsWith<AttractionBookFailedException> {
            attraction.compensateCancelBooking(bookingId)
        }
    }

    @Test
    fun compensate_cancel_booking_fills_attraction_and_emits_full_event() {
        attraction.book(UUID.randomUUID())
        attraction.book(UUID.randomUUID())
        val events = attraction.compensateCancelBooking(bookingId)

        assertEquals(2, events.size)
        assertEventEquals(
            AttractionBookingCanceledCompensatedEvent(
                attractionId = attraction.id,
                bookingId = bookingId,
            ),
            events.first(),
        )
        assertEventEquals(
            AttractionFullEvent(
                attractionId = attraction.id,
            ),
            events.last(),
        )
        assertEquals(AttractionStatusEnum.FULL, attraction.status)
        assertEquals(3, attraction.bookings.size)
    }

    @Test
    fun sequential_bookings_up_to_capacity() {
        val attraction = attraction.copy()
        val id1 = UUID.randomUUID()
        val id2 = UUID.randomUUID()
        val id3 = UUID.randomUUID()

        var events = attraction.book(id1)
        assertEquals(1, events.size)
        assertEquals(AttractionStatusEnum.SCHEDULED, attraction.status)

        events = attraction.book(id2)
        assertEquals(1, events.size)
        assertEquals(AttractionStatusEnum.SCHEDULED, attraction.status)

        events = attraction.book(id3)
        assertEquals(2, events.size)
        assertEquals(AttractionStatusEnum.FULL, attraction.status)

        assertEquals(3, attraction.bookings.size)
    }

    @Test
    fun booking_at_exact_capacity_minus_one_then_final_booking() {
        attraction = attraction.copy(capacity = 2)

        val id1 = UUID.randomUUID()
        val id2 = UUID.randomUUID()

        var events = attraction.book(id1)
        assertEquals(1, events.size)
        assertEquals(AttractionStatusEnum.SCHEDULED, attraction.status)

        events = attraction.book(id2)
        assertEquals(2, events.size)
        assertEventEquals(
            AttractionBookedEvent(
                attractionId = attraction.id,
                bookingId = id2,
            ),
            events.first(),
        )
        assertEventEquals(
            AttractionFullEvent(
                attractionId = attraction.id,
            ),
            events.last(),
        )
        assertEquals(AttractionStatusEnum.FULL, attraction.status)
    }

    @Test
    fun attraction_apply_should_map_events_correctly() {
        val id = UUID.randomUUID()
        val bookingId = UUID.randomUUID()
        val base = Attraction(id, "a", LocationEnum.VENICE, LocalDateTime.now().plusDays(5), 2)

        base.apply(AttractionCreatedEvent(attractionId = id, name = "n", location = LocationEnum.ZERMATT, date = LocalDateTime.now().plusDays(1), capacity = 3))
        assertEquals(id, base.id)
        assertEquals("a", base.name)
        assertEquals(AttractionStatusEnum.SCHEDULED, base.status)
        assertEquals(0, base.bookings.size)

        base.apply(AttractionBookedEvent(attractionId = id, bookingId = bookingId))
        assertEquals(listOf(bookingId), base.bookings)

        base.apply(AttractionFullEvent(attractionId = id))
        assertEquals(AttractionStatusEnum.FULL, base.status)

        base.apply(AttractionAvailableEvent(attractionId = id))
        assertEquals(AttractionStatusEnum.SCHEDULED, base.status)

        base.apply(AttractionBookingCanceledEvent(attractionId = id, bookingId = bookingId))
        assertEquals(0, base.bookings.size)

        base.apply(AttractionExpiredEvent(attractionId = id))
        assertEquals(AttractionStatusEnum.EXPIRED, base.status)
    }

    @Test
    fun attraction_fromEvents_should_handle_empty_and_invalid_first_event() {
        assertNull(Attraction.fromEvents(emptyList()))
        val id = UUID.randomUUID()
        val invalid = listOf(AttractionBookedEvent(attractionId = id, bookingId = UUID.randomUUID()))
        assertFailsWith<AttractionMissingCreatedEventException> { Attraction.fromEvents(invalid) }
    }

    @Test
    fun attraction_fromEvents_should_rebuild_state() {
        val id = UUID.randomUUID()
        val bookingId = UUID.randomUUID()
        val createdDate = LocalDateTime.now().plusDays(3)
        val events = listOf(
            AttractionCreatedEvent(attractionId = id, name = "tours", location = LocationEnum.PARIS, date = createdDate, capacity = 2),
            AttractionBookedEvent(attractionId = id, bookingId = bookingId),
            AttractionFullEvent(attractionId = id),
            AttractionBookingCanceledEvent(attractionId = id, bookingId = bookingId),
            AttractionAvailableEvent(attractionId = id),
        )

        val result = Attraction.fromEvents(events)
        assertNotNull(result)
        assertEquals(id, result.id)
        assertEquals("tours", result.name)
        assertEquals(LocationEnum.PARIS, result.location)
        assertEquals(createdDate, result.date)
        assertEquals(2, result.capacity)
        assertEquals(AttractionStatusEnum.SCHEDULED, result.status)
        assertEquals(0, result.bookings.size)
    }
}
