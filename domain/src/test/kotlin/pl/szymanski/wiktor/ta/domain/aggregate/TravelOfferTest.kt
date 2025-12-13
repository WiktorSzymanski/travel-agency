@file:Suppress("WildcardImport")

package pl.szymanski.wiktor.ta.domain.aggregate

import pl.szymanski.wiktor.ta.domain.AccommodationStatusEnum
import pl.szymanski.wiktor.ta.domain.AttractionStatusEnum
import pl.szymanski.wiktor.ta.domain.CommuteStatusEnum
import pl.szymanski.wiktor.ta.domain.LocationAndTime
import pl.szymanski.wiktor.ta.domain.LocationEnum
import pl.szymanski.wiktor.ta.domain.Rent
import pl.szymanski.wiktor.ta.domain.Seat
import pl.szymanski.wiktor.ta.domain.TravelOfferStatusEnum
import pl.szymanski.wiktor.ta.domain.assertEventEquals
import pl.szymanski.wiktor.ta.domain.event.AccommodationBookedCompensatedEvent
import pl.szymanski.wiktor.ta.domain.event.AccommodationBookedEvent
import pl.szymanski.wiktor.ta.domain.event.AccommodationBookingCanceledCompensatedEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionAvailableEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionBookedCompensatedEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionBookedEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionBookingCanceledCompensatedEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionFullEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteAvailableEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteBookedCompensatedEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteBookedEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteBookingCanceledCompensatedEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteBookingCanceledEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteFullEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferBookedCompensatedEvent
import pl.szymanski.wiktor.ta.domain.exception.*
import pl.szymanski.wiktor.ta.domain.event.TravelOfferBookedEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferBookingCanceledCompensatedEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferBookingCanceledEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferCreatedEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferExpiredEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferMadeAvailableEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferMadeUnavailableEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferRebookedEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferReleaseEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferReservationCanceledEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferReservedEvent
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class TravelOfferTest {
    private lateinit var travelOfferId: UUID
    private lateinit var bookingId: UUID
    private lateinit var commuteId: UUID
    private lateinit var accommodationId: UUID
    private lateinit var attractionId: UUID
    private lateinit var now: LocalDateTime

    private lateinit var seat: Seat

    private lateinit var offer: TravelOffer

    @BeforeTest
    fun setup() {
        travelOfferId = UUID.randomUUID()
        bookingId = UUID.randomUUID()
        commuteId = UUID.randomUUID()
        accommodationId = UUID.randomUUID()
        attractionId = UUID.randomUUID()
        now = LocalDateTime.now()

        seat = Seat.Picked("1", "A")

        offer = TravelOffer(travelOfferId, "travelOffer_name", commuteId, accommodationId, attractionId)
    }

    @Test
    fun aggregate_should_return_travelOffer_and_created_event() {
        val commuteId = UUID.randomUUID()
        val accommodationId = UUID.randomUUID()
        val attractionId = UUID.randomUUID()

        val (offer, events) = TravelOffer.create(
            name = "offer_name",
            commuteId = commuteId,
            accommodationId = accommodationId,
            attractionId = attractionId,
        )

        assertEventEquals(
            TravelOfferCreatedEvent(
                travelOfferId = offer.id,
                name = "offer_name",
                commuteId = commuteId,
                accommodationId = accommodationId,
                attractionId = attractionId,
            ),
            events.first(),
        )
        assertEquals("offer_name", offer.name)
        assertEquals(commuteId, offer.commuteId)
        assertEquals(accommodationId, offer.accommodationId)
        assertEquals(attractionId, offer.attractionId)
    }

    @Test
    fun reserve_should_succeed_when_offer_is_available() {
        val events = offer.reserve(bookingId, seat)

        assertEventEquals(
            TravelOfferReservedEvent(
                travelOfferId = travelOfferId,
                accommodationId = accommodationId,
                commuteId = commuteId,
                attractionId = attractionId,
                bookingId = bookingId,
                seat = seat,
            ),
            events.first(),
        )
        assertEquals(TravelOfferStatusEnum.RESERVED, offer.status)
        assertEquals(bookingId, offer.bookingId)
    }

    @Test
    fun reserve_should_fail_when_offer_not_available() {
        val offer =
            offer.copy(
                status = TravelOfferStatusEnum.BOOKED,
            )

        assertFailsWith<TravelOfferReserveFailedException> { offer.reserve(bookingId, seat) }
    }

    @Test
    fun book_should_succeed_when_offer_is_reserved_by_same_user() {
        // First reserve the offer
        offer.reserve(bookingId, seat)

        // Then book it
        val events = offer.book(bookingId, seat)

        assertEventEquals(
            TravelOfferBookedEvent(
                travelOfferId = travelOfferId,
                accommodationId = accommodationId,
                commuteId = commuteId,
                attractionId = attractionId,
                bookingId = bookingId,
                seat = seat,
            ),
            events.first(),
        )
        assertEquals(TravelOfferStatusEnum.BOOKED, offer.status)
        assertEquals(bookingId, offer.bookingId)
    }

    @Test
    fun book_should_fail_when_offer_not_reserved() {
        val offer =
            offer.copy(
                status = TravelOfferStatusEnum.AVAILABLE,
            )

        assertFailsWith<TravelOfferBookFailedException> { offer.book(bookingId, seat) }
    }

    @Test
    fun book_should_fail_when_offer_reserved_by_different_user() {
        val differentbookingId = UUID.randomUUID()

        // First reserve the offer for a different user
        val reservedOffer = offer.copy()
        reservedOffer.reserve(differentbookingId, seat)

        assertFailsWith<TravelOfferBookFailedException> { reservedOffer.book(bookingId, seat) }
    }

    @Test
    fun expire_should_change_status_to_expired() {
        val events = offer.expire()

        assertEventEquals(
            TravelOfferExpiredEvent(
                travelOfferId = travelOfferId,
                commuteId = commuteId,
                accommodationId = accommodationId,
                attractionId = attractionId,
            ),
            events.first(),
        )
        assertEquals(TravelOfferStatusEnum.EXPIRED, offer.status)
    }

    @Test
    fun expire_should_succeed_if_status_available() {
        val events = offer.expire()

        assertEventEquals(
            TravelOfferExpiredEvent(
                travelOfferId = travelOfferId,
                commuteId = commuteId,
                accommodationId = accommodationId,
                attractionId = attractionId,
            ),
            events.first(),
        )
        assertEquals(TravelOfferStatusEnum.EXPIRED, offer.status)
    }

    @Test
    fun expire_should_fail_if_status_not_available() {
        val offer =
            offer.copy(
                status = TravelOfferStatusEnum.BOOKED,
            )

        assertFailsWith<TravelOfferExpireFailedException> { offer.expire() }
    }

    @Test
    fun cancelBooking_2_should_succeed_when_booked_by_same_user() {
        val booking = bookingId
        val offer =
            offer.copy(
                bookingId = booking,
                status = TravelOfferStatusEnum.BOOKED,
            )

        val events = offer.releaseBooking(bookingId, seat)

        assertEventEquals(
            TravelOfferReleaseEvent(
                travelOfferId = travelOfferId,
                accommodationId = accommodationId,
                commuteId = commuteId,
                attractionId = attractionId,
                bookingId = bookingId,
                seat = seat,
            ),
            events.first(),
        )
        assertEquals(TravelOfferStatusEnum.RELEASING, offer.status)
    }

    @Test
    fun cancelBooking_2_should_fail_if_status_not_booked() {
        val offer =
            offer.copy(
                status = TravelOfferStatusEnum.AVAILABLE,
            )

        assertFailsWith<TravelOfferBookingCancelFailedException> { offer.releaseBooking(bookingId, seat) }
    }

    @Test
    fun cancelBooking_2_should_fail_if_wrong_user() {
        val booking = UUID.randomUUID()
        val offer =
            offer.copy(
                bookingId = booking,
                status = TravelOfferStatusEnum.BOOKED,
            )

        assertFailsWith<TravelOfferBookingCancelFailedException> { offer.releaseBooking(bookingId, seat) }
    }

    @Test
    fun cancelBooking_should_succeed_when_releasing_by_same_user() {
        val booking = bookingId
        val offer =
            offer.copy(
                bookingId = booking,
                status = TravelOfferStatusEnum.RELEASING,
            )

        val events = offer.cancelBooking(bookingId, seat)

        assertEventEquals(
            TravelOfferBookingCanceledEvent(
                travelOfferId = travelOfferId,
                accommodationId = accommodationId,
                commuteId = commuteId,
                attractionId = attractionId,
                bookingId = bookingId,
                seat = seat,
            ),
            events.first(),
        )
        assertEquals(TravelOfferStatusEnum.AVAILABLE, offer.status)
        assertNull(offer.bookingId)
    }

    @Test
    fun cancelBooking_should_fail_if_status_not_releasing() {
        val offer =
            offer.copy(
                status = TravelOfferStatusEnum.AVAILABLE,
            )

        assertFailsWith<TravelOfferReleaseCompleteFailedException> { offer.cancelBooking(bookingId, seat) }
    }

    @Test
    fun cancelBooking_should_fail_if_wrong_user() {
        val booking = UUID.randomUUID()
        val offer =
            offer.copy(
                bookingId = booking,
                status = TravelOfferStatusEnum.RELEASING,
            )

        assertFailsWith<TravelOfferReleaseCompleteFailedException> { offer.cancelBooking(bookingId, seat) }
    }

    @Test
    fun makeUnavailable_successfully_from_available() {
        val events = offer.makeUnavailable()

        assertEventEquals(
            TravelOfferMadeUnavailableEvent(
                travelOfferId = travelOfferId,
            ),
            events.first(),
        )
        assertEquals(TravelOfferStatusEnum.UNAVAILABLE, offer.status)
    }

    @Test
    fun makeUnavailable_fails_when_not_available() {
        offer = offer.copy(status = TravelOfferStatusEnum.BOOKED)

        assertFailsWith<TravelOfferMakeUnavailableFailedException> { offer.makeUnavailable() }
    }

    @Test
    fun makeAvailable_successfully_from_unavailable() {
        offer = offer.copy(status = TravelOfferStatusEnum.UNAVAILABLE)
        val events = offer.makeAvailable()

        assertEventEquals(
            TravelOfferMadeAvailableEvent(
                travelOfferId = travelOfferId,
            ),
            events.first(),
        )
        assertEquals(TravelOfferStatusEnum.AVAILABLE, offer.status)
    }

    @Test
    fun makeAvailable_fails_when_not_unavailable() {
        assertFailsWith<TravelOfferMakeAvailableFailedException> { offer.makeAvailable() }
    }

    @Test
    fun cancelReservation_successfully() {
        offer.reserve(bookingId, seat)
        val events = offer.cancelReservation(bookingId, seat)

        assertEventEquals(
            TravelOfferReservationCanceledEvent(
                travelOfferId = travelOfferId,
                accommodationId = accommodationId,
                commuteId = commuteId,
                attractionId = attractionId,
                bookingId = bookingId,
                seat = seat,
            ),
            events.first(),
        )
        assertEquals(TravelOfferStatusEnum.AVAILABLE, offer.status)
        assertNull(offer.bookingId)
    }

    @Test
    fun cancelReservation_fails_when_not_reserved() {
        assertFailsWith<TravelOfferReservationCancelFailedException> { offer.cancelReservation(bookingId, seat) }
    }

    @Test
    fun cancelReservation_fails_when_wrong_booking() {
        val differentBookingId = UUID.randomUUID()
        offer.reserve(differentBookingId, seat)

        assertFailsWith<TravelOfferReservationCancelFailedException> { offer.cancelReservation(bookingId, seat) }
    }

    @Test
    fun cancelReservation_with_null_seat() {
        offer.reserve(bookingId, Seat.Any)
        val events = offer.cancelReservation(bookingId, Seat.Any)

        assertEventEquals(
            TravelOfferReservationCanceledEvent(
                travelOfferId = travelOfferId,
                accommodationId = accommodationId,
                commuteId = commuteId,
                attractionId = attractionId,
                bookingId = bookingId,
                seat = Seat.Any,
            ),
            events.first(),
        )
        assertEquals(TravelOfferStatusEnum.AVAILABLE, offer.status)
    }

    @Test
    fun rebook_successfully_from_releasing() {
        offer =
            offer.copy(
                bookingId = bookingId,
                status = TravelOfferStatusEnum.RELEASING,
            )
        val events = offer.rebook(bookingId)

        assertEventEquals(
            TravelOfferRebookedEvent(
                travelOfferId = travelOfferId,
                bookingId = bookingId,
            ),
            events.first(),
        )
        assertEquals(TravelOfferStatusEnum.BOOKED, offer.status)
    }

    @Test
    fun rebook_fails_when_not_releasing() {
        offer = offer.copy(status = TravelOfferStatusEnum.BOOKED)

        assertFailsWith<TravelOfferRebookFailedException> { offer.rebook(bookingId) }
    }

    @Test
    fun reserve_with_null_seat() {
        val events = offer.reserve(bookingId, Seat.Any)

        assertEventEquals(
            TravelOfferReservedEvent(
                travelOfferId = travelOfferId,
                accommodationId = accommodationId,
                commuteId = commuteId,
                attractionId = attractionId,
                bookingId = bookingId,
                seat = Seat.Any,
            ),
            events.first(),
        )
        assertEquals(TravelOfferStatusEnum.RESERVED, offer.status)
    }

    @Test
    fun book_with_null_seat() {
        offer.reserve(bookingId, Seat.Any)
        val events = offer.book(bookingId, Seat.Any)

        assertEventEquals(
            TravelOfferBookedEvent(
                travelOfferId = travelOfferId,
                accommodationId = accommodationId,
                commuteId = commuteId,
                attractionId = attractionId,
                bookingId = bookingId,
                seat = Seat.Any,
            ),
            events.first(),
        )
        assertEquals(TravelOfferStatusEnum.BOOKED, offer.status)
    }

    @Test
    fun travelOffer_apply_should_map_events_correctly() {
        val id = UUID.randomUUID()
        val accId = UUID.randomUUID()
        val comId = UUID.randomUUID()
        val attrId = UUID.randomUUID()
        val bookingId = UUID.randomUUID()

        val base = TravelOffer(id, "t", comId, accId, attrId)

        // Created event is ignored by in-aggregate apply; state is set on creation or fromEvents
        base.apply(TravelOfferCreatedEvent(travelOfferId = id, name = "X", commuteId = comId, accommodationId = accId, attractionId = attrId))
        assertEquals(id, base.id)
        assertEquals("t", base.name)
        assertEquals(TravelOfferStatusEnum.AVAILABLE, base.status)

        base.apply(TravelOfferMadeUnavailableEvent(travelOfferId = id))
        assertEquals(TravelOfferStatusEnum.UNAVAILABLE, base.status)

        base.apply(TravelOfferMadeAvailableEvent(travelOfferId = id))
        assertEquals(TravelOfferStatusEnum.AVAILABLE, base.status)

        base.apply(TravelOfferReservedEvent(travelOfferId = id, accommodationId = accId, commuteId = comId, attractionId = attrId, bookingId = bookingId, seat = Seat.Any))
        assertEquals(TravelOfferStatusEnum.RESERVED, base.status)
        assertEquals(bookingId, base.bookingId)

        base.apply(TravelOfferBookedEvent(travelOfferId = id, accommodationId = accId, commuteId = comId, attractionId = attrId, bookingId = bookingId, seat = Seat.Any))
        assertEquals(TravelOfferStatusEnum.BOOKED, base.status)
        assertEquals(bookingId, base.bookingId)

        base.apply(TravelOfferReleaseEvent(travelOfferId = id, accommodationId = accId, commuteId = comId, attractionId = attrId, bookingId = bookingId, seat = Seat.Any))
        assertEquals(TravelOfferStatusEnum.RELEASING, base.status)

        base.apply(TravelOfferRebookedEvent(travelOfferId = id, bookingId = bookingId))
        assertEquals(TravelOfferStatusEnum.BOOKED, base.status)

        base.apply(TravelOfferBookingCanceledEvent(travelOfferId = id, accommodationId = accId, commuteId = comId, attractionId = attrId, bookingId = bookingId, seat = Seat.Any))
        assertEquals(TravelOfferStatusEnum.AVAILABLE, base.status)
        assertNull(base.bookingId)

        base.apply(TravelOfferExpiredEvent(travelOfferId = id, accommodationId = accId, commuteId = comId, attractionId = attrId))
        assertEquals(TravelOfferStatusEnum.EXPIRED, base.status)
    }

    @Test
    fun travelOffer_apply_reservation_canceled_should_clear_reservation() {
        val id = UUID.randomUUID()
        val accId = UUID.randomUUID()
        val comId = UUID.randomUUID()
        val attrId = UUID.randomUUID()
        val bookingId = UUID.randomUUID()

        val base = TravelOffer(id, "t", comId, accId, attrId)

        // First apply a reservation to set RESERVED state with bookingId
        base.apply(
            TravelOfferReservedEvent(
                travelOfferId = id,
                accommodationId = accId,
                commuteId = comId,
                attractionId = attrId,
                bookingId = bookingId,
                seat = Seat.Any,
            ),
        )
        assertEquals(TravelOfferStatusEnum.RESERVED, base.status)
        assertEquals(bookingId, base.bookingId)

        // Then apply ReservationCanceled -> should become AVAILABLE and clear bookingId
        base.apply(
            TravelOfferReservationCanceledEvent(
                travelOfferId = id,
                accommodationId = accId,
                commuteId = comId,
                attractionId = attrId,
                bookingId = bookingId,
                seat = Seat.Any,
            ),
        )
        assertEquals(TravelOfferStatusEnum.AVAILABLE, base.status)
        assertNull(base.bookingId)
    }

    @Test
    fun travelOffer_fromEvents_should_handle_empty_and_invalid_first_event() {
        assertNull(TravelOffer.fromEvents(emptyList()))
        val dummy = TravelOffer(UUID.randomUUID(), "t", UUID.randomUUID(), UUID.randomUUID(), null)
        val invalid = listOf(TravelOfferReservedEvent(travelOfferId = dummy.id, accommodationId = dummy.accommodationId, commuteId = dummy.commuteId, attractionId = dummy.attractionId, bookingId = UUID.randomUUID(), seat = Seat.Any))
        assertFailsWith<TravelOfferMissingCreatedEventException> { TravelOffer.fromEvents(invalid) }
    }

    @Test
    fun travelOffer_fromEvents_should_rebuild_state() {
        val id = UUID.randomUUID()
        val accId = UUID.randomUUID()
        val comId = UUID.randomUUID()
        val attrId = UUID.randomUUID()
        val bookingId = UUID.randomUUID()

        val events = listOf(
            TravelOfferCreatedEvent(travelOfferId = id, name = "offer", commuteId = comId, accommodationId = accId, attractionId = attrId),
            TravelOfferReservedEvent(travelOfferId = id, accommodationId = accId, commuteId = comId, attractionId = attrId, bookingId = bookingId, seat = Seat.Any),
            TravelOfferBookedEvent(travelOfferId = id, accommodationId = accId, commuteId = comId, attractionId = attrId, bookingId = bookingId, seat = Seat.Any),
            TravelOfferBookingCanceledEvent(travelOfferId = id, accommodationId = accId, commuteId = comId, attractionId = attrId, bookingId = bookingId, seat = Seat.Any),
            TravelOfferMadeUnavailableEvent(travelOfferId = id),
            TravelOfferMadeAvailableEvent(travelOfferId = id),
        )

        val result = TravelOffer.fromEvents(events)
        assertNotNull(result)
        assertEquals(id, result.id)
        assertEquals("offer", result.name)
        assertEquals(TravelOfferStatusEnum.AVAILABLE, result.status)
        assertNull(result.bookingId)
        assertEquals(accId, result.accommodationId)
        assertEquals(comId, result.commuteId)
        assertEquals(attrId, result.attractionId)
    }

    // Compensation branches (apply)
    @Test
    fun accommodation_apply_compensation_events_should_update_state() {
        val id = UUID.randomUUID()
        val bookingId = UUID.randomUUID()
        val base = Accommodation(id, "acc", LocationEnum.PARIS, Rent(LocalDateTime.now(), LocalDateTime.now().plusDays(1)))

        // Given currently booked -> applying BookedCompensated should free it
        base.apply(AccommodationBookedEvent(accommodationId = id, bookingId = bookingId))
        base.apply(AccommodationBookedCompensatedEvent(accommodationId = id, bookingId = bookingId))
        assertEquals(AccommodationStatusEnum.AVAILABLE, base.status)
        assertNull(base.bookingId)

        // Applying BookingCanceledCompensated should set it back to booked
        base.apply(AccommodationBookingCanceledCompensatedEvent(accommodationId = id, bookingId = bookingId))
        assertEquals(AccommodationStatusEnum.BOOKED, base.status)
        assertEquals(bookingId, base.bookingId)
    }

    @Test
    fun attraction_apply_compensation_events_should_compensate() {
        val id = UUID.randomUUID()
        val bookingId = UUID.randomUUID()
        val a = Attraction(id, "a", LocationEnum.VENICE, LocalDateTime.now().plusDays(1), 1)

        // Fill to FULL
        a.apply(AttractionBookedEvent(attractionId = id, bookingId = bookingId))
        a.apply(AttractionFullEvent(attractionId = id))

        // Applying BookedCompensated removes booking and makes it SCHEDULED again
        a.apply(AttractionBookedCompensatedEvent(attractionId = id, bookingId = bookingId))
        assertEquals(0, a.bookings.size)

        a.apply(AttractionAvailableEvent(attractionId = id))
        assertEquals(AttractionStatusEnum.SCHEDULED, a.status)

        // Applying BookingCanceledCompensated adds a booking and makes it FULL (capacity=1)
        a.apply(AttractionBookingCanceledCompensatedEvent(attractionId = id, bookingId = bookingId))
        assertEquals(1, a.bookings.size)
        // Status change (to FULL) is applied by a separate event
        a.apply(AttractionFullEvent(attractionId = id))
        assertEquals(AttractionStatusEnum.FULL, a.status)
    }

    @Test
    fun commute_apply_compensation_events_should_compensate() {
        val id = UUID.randomUUID()
        val bookingId = UUID.randomUUID()
        val seat = Seat.Picked("1", "A")
        val c = Commute(id, "c", LocationAndTime(LocationEnum.MADRID, LocalDateTime.now()), LocationAndTime(LocationEnum.BARCELONA, LocalDateTime.now().plusHours(1)), listOf(seat))

        c.apply(CommuteBookedEvent(commuteId = id, bookingId = bookingId, seat = seat))
        c.apply(CommuteFullEvent(commuteId = id))
        assertEquals(CommuteStatusEnum.FULL, c.status)

        c.apply(CommuteBookedCompensatedEvent(commuteId = id, bookingId = bookingId, seat = seat))
        assertEquals(0, c.bookings.size)

        c.apply(CommuteAvailableEvent(commuteId = id))
        assertEquals(CommuteStatusEnum.SCHEDULED, c.status)

        c.apply(CommuteBookingCanceledCompensatedEvent(commuteId = id, bookingId = bookingId, seat = seat))
        assertEquals(1, c.bookings.size)
        // Note: Full status is derived by domain logic separately; apply only reflects event state
    }

    @Test
    fun commute_apply_compensation_events_should_update_state() {
        val id = UUID.randomUUID()
        val bookingId = UUID.randomUUID()
        val seat = Seat.Picked("1", "A")
        val c2 = Commute(id, "c", LocationAndTime(LocationEnum.MADRID, LocalDateTime.now()), LocationAndTime(LocationEnum.BARCELONA, LocalDateTime.now().plusHours(1)), listOf(seat))

        c2.apply(CommuteBookedEvent(commuteId = id, bookingId = bookingId, seat = seat))
        c2.apply(CommuteFullEvent(commuteId = id))
        assertEquals(CommuteStatusEnum.FULL, c2.status)

        c2.apply(CommuteBookingCanceledEvent(commuteId = id, bookingId = bookingId, seat = seat))
        assertEquals(0, c2.bookings.size)

        c2.apply(CommuteAvailableEvent(commuteId = id))
        assertEquals(CommuteStatusEnum.SCHEDULED, c2.status)

        c2.apply(CommuteBookingCanceledCompensatedEvent(commuteId = id, bookingId = bookingId, seat = seat))
        assertEquals(1, c2.bookings.size)
    }

    @Test
    fun travelOffer_apply_compensation_events_should_update_state() {
        val id = UUID.randomUUID()
        val accId = UUID.randomUUID()
        val comId = UUID.randomUUID()
        val attrId = UUID.randomUUID()
        val bookingId = UUID.randomUUID()
        val seat = Seat.Picked("2", "B")

        val offer = TravelOffer(id, "t", comId, accId, attrId)
        offer.apply(TravelOfferBookedEvent(travelOfferId = id, accommodationId = accId, commuteId = comId, attractionId = attrId, bookingId = bookingId, seat = seat))
        assertEquals(TravelOfferStatusEnum.BOOKED, offer.status)

        // Applying BookedCompensated should free booking and set AVAILABLE
        offer.apply(TravelOfferBookedCompensatedEvent(travelOfferId = id, accommodationId = accId, commuteId = comId, attractionId = attrId, bookingId = bookingId, seat = seat))
        assertEquals(TravelOfferStatusEnum.AVAILABLE, offer.status)
        assertNull(offer.bookingId)

        // Applying BookingCanceledCompensated should go back to BOOKED
        offer.apply(TravelOfferBookingCanceledCompensatedEvent(travelOfferId = id, accommodationId = accId, commuteId = comId, attractionId = attrId, bookingId = bookingId, seat = seat))
        assertEquals(TravelOfferStatusEnum.BOOKED, offer.status)
        assertEquals(bookingId, offer.bookingId)
    }
}
