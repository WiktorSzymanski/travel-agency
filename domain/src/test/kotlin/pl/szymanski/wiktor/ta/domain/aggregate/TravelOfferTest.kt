package pl.szymanski.wiktor.ta.domain.aggregate

import pl.szymanski.wiktor.ta.domain.Seat
import pl.szymanski.wiktor.ta.domain.TravelOfferStatusEnum
import pl.szymanski.wiktor.ta.domain.assertEventEquals
import pl.szymanski.wiktor.ta.domain.event.TravelOfferBookFailedEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferBookedEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferBookingCancelFailedEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferBookingCanceledEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferExpireFailedEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferExpiredEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferReleaseCompleteFailedEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferReleaseEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferReserveFailedEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferReservedEvent
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
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

        val event = offer.reserve(bookingId, seat)

        assertEventEquals(
            TravelOfferReserveFailedEvent(
                travelOfferId = travelOfferId,
                bookingId = bookingId,
                message = "TravelOffer is not open for reservation, current status is ${TravelOfferStatusEnum.BOOKED}",
            ),
            event,
        )
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

        val event = offer.book(bookingId, seat)

        assertEventEquals(
            TravelOfferBookFailedEvent(
                travelOfferId = travelOfferId,
                bookingId = bookingId,
                message = "TravelOffer can not be booked if not RESERVED prior, current status is ${TravelOfferStatusEnum.AVAILABLE}",
            ),
            event,
        )
    }

    @Test
    fun book_should_fail_when_offer_reserved_by_different_user() {
        val differentbookingId = UUID.randomUUID()

        // First reserve the offer for a different user
        val reservedOffer = offer.copy()
        reservedOffer.reserve(differentbookingId, seat)

        val event = reservedOffer.book(bookingId, seat)

        assertEventEquals(
            TravelOfferBookFailedEvent(
                travelOfferId = travelOfferId,
                bookingId = bookingId,
                message = "TravelOffer $travelOfferId is not RESERVED by booking $bookingId",
            ),
            event,
        )
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

        val event = offer.expire()

        assertEventEquals(
            TravelOfferExpireFailedEvent(
                travelOfferId = travelOfferId,
                message = "TravelOffer $travelOfferId cannot be expired when in BOOKED status",
            ),
            event,
        )
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

        val event = offer.releaseBooking(bookingId, seat)

        assertEventEquals(
            TravelOfferBookingCancelFailedEvent(
                travelOfferId = travelOfferId,
                bookingId = bookingId,
                message = "Cannot cancel booking for TravelOffer $travelOfferId when in AVAILABLE status",
            ),
            event,
        )
    }

    @Test
    fun cancelBooking_2_should_fail_if_wrong_user() {
        val booking = UUID.randomUUID()
        val offer =
            offer.copy(
                bookingId = booking,
                status = TravelOfferStatusEnum.BOOKED,
            )

        val event = offer.releaseBooking(bookingId, seat)

        assertEventEquals(
            TravelOfferBookingCancelFailedEvent(
                travelOfferId = travelOfferId,
                bookingId = bookingId,
                message = "TravelOffer $travelOfferId is not BOOKED for Booking $bookingId",
            ),
            event,
        )
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

        val event = offer.cancelBooking(bookingId, seat)

        assertEventEquals(
            TravelOfferReleaseCompleteFailedEvent(
                travelOfferId = travelOfferId,
                bookingId = bookingId,
                message = "Cannot complete release for TravelOffer $travelOfferId when in AVAILABLE status",
            ),
            event,
        )
    }

    @Test
    fun cancelBooking_should_fail_if_wrong_user() {
        val booking = UUID.randomUUID()
        val offer =
            offer.copy(
                bookingId = booking,
                status = TravelOfferStatusEnum.RELEASING,
            )

        val event = offer.cancelBooking(bookingId, seat)

        assertEventEquals(
            TravelOfferReleaseCompleteFailedEvent(
                travelOfferId = travelOfferId,
                bookingId = bookingId,
                message = "TravelOffer $travelOfferId is not being released by Booking $bookingId",
            ),
            event,
        )
    }
}
