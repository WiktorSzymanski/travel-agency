package pl.szymanski.wiktor.ta.domain.aggregate

import pl.szymanski.wiktor.ta.domain.Booking
import pl.szymanski.wiktor.ta.domain.OfferStatusEnum
import pl.szymanski.wiktor.ta.domain.Seat
import pl.szymanski.wiktor.ta.domain.assertEventEquals
import pl.szymanski.wiktor.ta.domain.event.TravelOfferBookedEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferBookingCanceledEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferExpiredEvent
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class TravelOfferTest {
    private lateinit var travelOfferId: UUID
    private lateinit var userId: UUID
    private lateinit var commuteId: UUID
    private lateinit var accommodationId: UUID
    private lateinit var attractionId: UUID
    private lateinit var now: LocalDateTime

    private lateinit var seat: Seat

    private lateinit var offer: TravelOffer

    @BeforeTest
    fun setup() {
        travelOfferId = UUID.randomUUID()
        userId = UUID.randomUUID()
        commuteId = UUID.randomUUID()
        accommodationId = UUID.randomUUID()
        attractionId = UUID.randomUUID()
        now = LocalDateTime.now()

        seat = Seat("1", "A")

        offer = TravelOffer(travelOfferId, "travelOffer_name", commuteId, accommodationId, attractionId)
    }

    @Test
    fun book_should_succeed_when_offer_is_available() {
        val event = offer.book(userId, seat)

        assertEventEquals(TravelOfferBookedEvent(
            travelOfferId = travelOfferId,
            accommodationId = accommodationId,
            commuteId = commuteId,
            attractionId = attractionId,
            userId = userId,
            seat = seat
        ), event)
        assertEquals(OfferStatusEnum.BOOKED, offer.status)
        assertEquals(userId, offer.booking?.userId)
    }

    @Test
    fun book_should_fail_when_offer_not_available() {
        val offer =
            offer.copy(
                status = OfferStatusEnum.BOOKED,
            )

        val ex =
            assertFailsWith<IllegalArgumentException> {
                offer.book(userId, seat)
            }

        assertEquals("TravelOffer $travelOfferId is not open for booking", ex.message)
    }

    @Test
    fun expire_should_change_status_to_expired() {
        val event = offer.expire()

        assertEventEquals(TravelOfferExpiredEvent(
            travelOfferId = travelOfferId
        ), event)
        assertEquals(OfferStatusEnum.EXPIRED, offer.status)
    }

    @Test
    fun expire_should_succeed_if_status_available() {
        val event = offer.expire()

        assertEventEquals(TravelOfferExpiredEvent(
            travelOfferId = travelOfferId
        ), event)
        assertEquals(OfferStatusEnum.EXPIRED, offer.status)
    }

    @Test
    fun expire_should_fail_if_status_not_available() {
        val offer =
            offer.copy(
                status = OfferStatusEnum.BOOKED,
            )

        val ex =
            assertFailsWith<IllegalArgumentException> {
                offer.expire()
            }

        assertEquals("TravelOffer $travelOfferId cannot be cancelled when not in AVAILABLE status", ex.message)
    }

    @Test
    fun cancelBooking_should_succeed_when_booked_by_same_user() {
        val booking = Booking(userId, now)
        val offer =
            offer.copy(
                booking = booking,
                status = OfferStatusEnum.BOOKED,
            )

        val event = offer.cancelBooking(userId, seat)

        assertEventEquals(TravelOfferBookingCanceledEvent(
            travelOfferId = travelOfferId,
            accommodationId = accommodationId,
            commuteId = commuteId,
            attractionId = attractionId,
            userId = userId,
            seat = seat
        ), event)
        assertNull(offer.booking)
    }

    @Test
    fun cancelBooking_should_fail_if_status_not_booked() {
        val offer =
            offer.copy(
                status = OfferStatusEnum.AVAILABLE,
            )

        val ex =
            assertFailsWith<IllegalArgumentException> {
                offer.cancelBooking(userId, seat)
            }

        assertEquals("Cannot cancel booking for TravelOffer $travelOfferId when not in BOOKED status", ex.message)
    }

    @Test
    fun cancelBooking_should_fail_if_wrong_user() {
        val booking = Booking(UUID.randomUUID(), now)
        val offer =
            offer.copy(
                booking = booking,
                status = OfferStatusEnum.BOOKED,
            )

        val ex =
            assertFailsWith<IllegalArgumentException> {
                offer.cancelBooking(userId, seat)
            }

        assertEquals("TravelOffer $travelOfferId is not BOOKED by user $userId", ex.message)
    }
}
