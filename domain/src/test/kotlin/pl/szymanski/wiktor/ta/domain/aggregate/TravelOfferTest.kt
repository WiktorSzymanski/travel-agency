package pl.szymanski.wiktor.ta.domain.aggregate

import pl.szymanski.wiktor.ta.domain.Seat
import pl.szymanski.wiktor.ta.domain.TravelOfferStatusEnum
import pl.szymanski.wiktor.ta.domain.assertEventEquals
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

class TravelOfferTest {
    private val bookingId: BookingId = BookingId.generate()

    private val commuteId: CommuteId = CommuteId.generate()
    private val accommodationId: AccommodationId = AccommodationId.generate()
    private val attractionId: AttractionId = AttractionId.generate()
    private val travelOfferId: TravelOfferId = TravelOfferId.generate(commuteId, accommodationId, attractionId)

    private lateinit var now: LocalDateTime

    private val seat: Seat = Seat.Picked("1", "A")

    private var offer: TravelOffer = TravelOffer(travelOfferId, "travelOffer_name", commuteId, accommodationId, attractionId)

    @BeforeTest
    fun setup() {
        now = LocalDateTime.now()
    }

    @Test
    fun aggregate_should_return_travelOffer_and_created_event() {
        val commuteId = CommuteId.generate()
        val accommodationId = AccommodationId.generate()
        val attractionId = AttractionId.generate()

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
        val otherBookingId = BookingId.generate()

        val reservedOffer = offer.copy()
        reservedOffer.reserve(otherBookingId, seat)

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
        val booking = BookingId.generate()
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
        assertEquals(offer.bookingId, BookingId.Empty)
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
        val booking = BookingId.generate()
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
        assertEquals(offer.bookingId, BookingId.Empty)
    }

    @Test
    fun cancelReservation_fails_when_not_reserved() {
        assertFailsWith<TravelOfferReservationCancelFailedException> { offer.cancelReservation(bookingId, seat) }
    }

    @Test
    fun cancelReservation_fails_when_wrong_booking() {
        val differentBookingId = BookingId.generate()
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
        offer.apply(TravelOfferMadeUnavailableEvent(travelOfferId = travelOfferId))
        assertEquals(TravelOfferStatusEnum.UNAVAILABLE, offer.status)

        offer.apply(TravelOfferMadeAvailableEvent(travelOfferId = travelOfferId))
        assertEquals(TravelOfferStatusEnum.AVAILABLE, offer.status)

        offer.apply(TravelOfferReservedEvent(travelOfferId = travelOfferId, accommodationId = accommodationId, commuteId = commuteId, attractionId = attractionId, bookingId = bookingId, seat = Seat.Any))
        assertEquals(TravelOfferStatusEnum.RESERVED, offer.status)
        assertEquals(bookingId, offer.bookingId)

        offer.apply(TravelOfferBookedEvent(travelOfferId = travelOfferId, accommodationId = accommodationId, commuteId = commuteId, attractionId = attractionId, bookingId = bookingId, seat = Seat.Any))
        assertEquals(TravelOfferStatusEnum.BOOKED, offer.status)
        assertEquals(bookingId, offer.bookingId)

        offer.apply(TravelOfferReleaseEvent(travelOfferId = travelOfferId, accommodationId = accommodationId, commuteId = commuteId, attractionId = attractionId, bookingId = bookingId, seat = Seat.Any))
        assertEquals(TravelOfferStatusEnum.RELEASING, offer.status)

        offer.apply(TravelOfferRebookedEvent(travelOfferId = travelOfferId, bookingId = bookingId))
        assertEquals(TravelOfferStatusEnum.BOOKED, offer.status)

        offer.apply(TravelOfferBookingCanceledEvent(travelOfferId = travelOfferId, accommodationId = accommodationId, commuteId = commuteId, attractionId = attractionId, bookingId = bookingId, seat = Seat.Any))
        assertEquals(TravelOfferStatusEnum.AVAILABLE, offer.status)
        assertEquals(offer.bookingId, BookingId.Empty)

        offer.apply(TravelOfferExpiredEvent(travelOfferId = travelOfferId, accommodationId = accommodationId, commuteId = commuteId, attractionId = attractionId))
        assertEquals(TravelOfferStatusEnum.EXPIRED, offer.status)
    }

    @Test
    fun travelOffer_apply_reservation_canceled_should_clear_reservation() {
        offer.apply(
            TravelOfferReservedEvent(
                travelOfferId = travelOfferId,
                accommodationId = accommodationId,
                commuteId = commuteId,
                attractionId = attractionId,
                bookingId = bookingId,
                seat = Seat.Any,
            ),
        )
        assertEquals(TravelOfferStatusEnum.RESERVED, offer.status)
        assertEquals(bookingId, offer.bookingId)

        offer.apply(
            TravelOfferReservationCanceledEvent(
                travelOfferId = travelOfferId,
                accommodationId = accommodationId,
                commuteId = commuteId,
                attractionId = attractionId,
                bookingId = bookingId,
                seat = Seat.Any,
            ),
        )
        assertEquals(TravelOfferStatusEnum.AVAILABLE, offer.status)
        assertEquals(offer.bookingId, BookingId.Empty)
    }

    @Test
    fun travelOffer_fromEvents_should_handle_empty_and_invalid_first_event() {
        assertFailsWith<TravelOfferEmptyEventListException> { TravelOffer.fromEvents(emptyList()) }

        val invalidEventList = listOf(
            TravelOfferReservedEvent(
                travelOfferId = TravelOfferId.from(UUID.randomUUID()),
                accommodationId = AccommodationId.generate(),
                commuteId = CommuteId.generate(),
                attractionId = AttractionId.generate(),
                bookingId = BookingId.generate(),
                seat = Seat.Any
            )
        )

        assertFailsWith<TravelOfferMissingCreatedEventException> { TravelOffer.fromEvents(invalidEventList) }
    }

    @Test
    fun travelOffer_fromEvents_should_rebuild_state() {
        val events = listOf(
            TravelOfferCreatedEvent(travelOfferId = travelOfferId, name = "offer", commuteId = commuteId, accommodationId = accommodationId, attractionId = attractionId),
            TravelOfferReservedEvent(travelOfferId = travelOfferId, accommodationId = accommodationId, commuteId = commuteId, attractionId = attractionId, bookingId = bookingId, seat = Seat.Any),
            TravelOfferBookedEvent(travelOfferId = travelOfferId, accommodationId = accommodationId, commuteId = commuteId, attractionId = attractionId, bookingId = bookingId, seat = Seat.Any),
            TravelOfferBookingCanceledEvent(travelOfferId = travelOfferId, accommodationId = accommodationId, commuteId = commuteId, attractionId = attractionId, bookingId = bookingId, seat = Seat.Any),
            TravelOfferMadeUnavailableEvent(travelOfferId = travelOfferId),
            TravelOfferMadeAvailableEvent(travelOfferId = travelOfferId),
        )

        val result = TravelOffer.fromEvents(events)
        assertEquals(travelOfferId, result.id)
        assertEquals("offer", result.name)
        assertEquals(TravelOfferStatusEnum.AVAILABLE, result.status)
        assertEquals(result.bookingId, BookingId.Empty)
        assertEquals(accommodationId, result.accommodationId)
        assertEquals(commuteId, result.commuteId)
        assertEquals(attractionId, result.attractionId)
    }

    @Test
    fun travelOffer_apply_compensation_events_should_update_state() {
        offer.apply(TravelOfferBookedEvent(travelOfferId = travelOfferId, accommodationId = accommodationId, commuteId = commuteId, attractionId = attractionId, bookingId = bookingId, seat = seat))
        assertEquals(TravelOfferStatusEnum.BOOKED, offer.status)

        offer.apply(
            TravelOfferBookedCompensatedEvent(
                travelOfferId = travelOfferId,
                accommodationId = accommodationId,
                commuteId = commuteId,
                attractionId = attractionId,
                bookingId = bookingId,
                seat = seat
            )
        )
        assertEquals(TravelOfferStatusEnum.AVAILABLE, offer.status)
        assertEquals(offer.bookingId, BookingId.Empty)

        offer.apply(
            TravelOfferBookingCanceledCompensatedEvent(
                travelOfferId = travelOfferId,
                accommodationId = accommodationId,
                commuteId = commuteId,
                attractionId = attractionId,
                bookingId = bookingId,
                seat = seat
            )
        )
        assertEquals(TravelOfferStatusEnum.BOOKED, offer.status)
        assertEquals(bookingId, offer.bookingId)
    }

    @Test
    fun compensateBook_should_succeed_when_bookingId_matches() {
        // Given
        offer.reserve(bookingId, seat)
        offer.book(bookingId, seat)

        // When
        val events = offer.compensateBook(bookingId, seat)

        // Then
        assertEventEquals(
            TravelOfferBookedCompensatedEvent(
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
        assertEquals(offer.bookingId, BookingId.Empty)
    }

    @Test
    fun compensateBook_should_fail_when_bookingId_does_not_match() {
        // Given
        val other = BookingId.generate()
        offer.reserve(other, seat)
        offer.book(other, seat)

        // Then
        assertFailsWith<TravelOfferBookingCancelFailedException> { offer.compensateBook(bookingId, seat) }
    }

    @Test
    fun compensateCancelBooking_should_succeed_when_no_active_booking() {
        // Given
        assertEquals(offer.bookingId, BookingId.Empty)
        assertEquals(TravelOfferStatusEnum.AVAILABLE, offer.status)

        // When
        val events = offer.compensateCancelBooking(bookingId, seat)

        // Then
        assertEventEquals(
            TravelOfferBookingCanceledCompensatedEvent(
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
    fun compensateCancelBooking_should_fail_when_active_booking_exists() {
        // Given
        offer.reserve(bookingId, seat)

        // Then
        assertFailsWith<TravelOfferBookFailedException> { offer.compensateCancelBooking(BookingId.generate(), seat) }
    }
}
