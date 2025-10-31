package pl.szymanski.wiktor.ta.domain.aggregate

import pl.szymanski.wiktor.ta.domain.Seat
import pl.szymanski.wiktor.ta.domain.TravelOfferStatusEnum
import pl.szymanski.wiktor.ta.domain.assertEventEquals
import pl.szymanski.wiktor.ta.domain.exception.*
import pl.szymanski.wiktor.ta.domain.event.TravelOfferBookedEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferBookingCanceledEvent
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

        seat = Seat("1", "A")

        offer = TravelOffer(travelOfferId, "travelOffer_name", commuteId, accommodationId, attractionId)
    }

    @Test
    fun reserve_should_succeed_when_offer_is_available() {
        val event = offer.reserve(bookingId, seat)

        assertEventEquals(
            TravelOfferReservedEvent(
                travelOfferId = travelOfferId,
                accommodationId = accommodationId,
                commuteId = commuteId,
                attractionId = attractionId,
                bookingId = bookingId,
                seat = seat,
            ),
            event,
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
        val event = offer.book(bookingId, seat)

        assertEventEquals(
            TravelOfferBookedEvent(
                travelOfferId = travelOfferId,
                accommodationId = accommodationId,
                commuteId = commuteId,
                attractionId = attractionId,
                bookingId = bookingId,
                seat = seat,
            ),
            event,
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
        val event = offer.expire()

        assertEventEquals(
            TravelOfferExpiredEvent(
                travelOfferId = travelOfferId,
                commuteId = commuteId,
                accommodationId = accommodationId,
                attractionId = attractionId,
            ),
            event,
        )
        assertEquals(TravelOfferStatusEnum.EXPIRED, offer.status)
    }

    @Test
    fun expire_should_succeed_if_status_available() {
        val event = offer.expire()

        assertEventEquals(
            TravelOfferExpiredEvent(
                travelOfferId = travelOfferId,
                commuteId = commuteId,
                accommodationId = accommodationId,
                attractionId = attractionId,
            ),
            event,
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

        val event = offer.releaseBooking(bookingId, seat)

        assertEventEquals(
            TravelOfferReleaseEvent(
                travelOfferId = travelOfferId,
                accommodationId = accommodationId,
                commuteId = commuteId,
                attractionId = attractionId,
                bookingId = bookingId,
                seat = seat,
            ),
            event,
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

        val event = offer.cancelBooking(bookingId, seat)

        assertEventEquals(
            TravelOfferBookingCanceledEvent(
                travelOfferId = travelOfferId,
                accommodationId = accommodationId,
                commuteId = commuteId,
                attractionId = attractionId,
                bookingId = bookingId,
                seat = seat,
            ),
            event,
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
        val event = offer.makeUnavailable()

        assertEventEquals(
            TravelOfferMadeUnavailableEvent(
                travelOfferId = travelOfferId,
            ),
            event,
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
        val event = offer.makeAvailable()

        assertEventEquals(
            TravelOfferMadeAvailableEvent(
                travelOfferId = travelOfferId,
            ),
            event,
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
        val event = offer.cancelReservation(bookingId, seat)

        assertEventEquals(
            TravelOfferReservationCanceledEvent(
                travelOfferId = travelOfferId,
                accommodationId = accommodationId,
                commuteId = commuteId,
                attractionId = attractionId,
                bookingId = bookingId,
                seat = seat,
            ),
            event,
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
        offer.reserve(bookingId, null)
        val event = offer.cancelReservation(bookingId, null)

        assertEventEquals(
            TravelOfferReservationCanceledEvent(
                travelOfferId = travelOfferId,
                accommodationId = accommodationId,
                commuteId = commuteId,
                attractionId = attractionId,
                bookingId = bookingId,
                seat = null,
            ),
            event,
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
        val event = offer.rebook(bookingId)

        assertEventEquals(
            TravelOfferRebookedEvent(
                travelOfferId = travelOfferId,
                bookingId = bookingId,
            ),
            event,
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
        val event = offer.reserve(bookingId, null)

        assertEventEquals(
            TravelOfferReservedEvent(
                travelOfferId = travelOfferId,
                accommodationId = accommodationId,
                commuteId = commuteId,
                attractionId = attractionId,
                bookingId = bookingId,
                seat = null,
            ),
            event,
        )
        assertEquals(TravelOfferStatusEnum.RESERVED, offer.status)
    }

    @Test
    fun book_with_null_seat() {
        offer.reserve(bookingId, null)
        val event = offer.book(bookingId, null)

        assertEventEquals(
            TravelOfferBookedEvent(
                travelOfferId = travelOfferId,
                accommodationId = accommodationId,
                commuteId = commuteId,
                attractionId = attractionId,
                bookingId = bookingId,
                seat = null,
            ),
            event,
        )
        assertEquals(TravelOfferStatusEnum.BOOKED, offer.status)
    }
}
