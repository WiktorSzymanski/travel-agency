package pl.szymanski.wiktor.ta.event

import pl.szymanski.wiktor.ta.domain.Seat
import pl.szymanski.wiktor.ta.domain.event.AccommodationBookedCompensatedEvent
import pl.szymanski.wiktor.ta.domain.event.AccommodationBookedEvent
import pl.szymanski.wiktor.ta.domain.event.AccommodationBookingCanceledCompensatedEvent
import pl.szymanski.wiktor.ta.domain.event.AccommodationBookingCanceledEvent
import pl.szymanski.wiktor.ta.domain.event.AccommodationExpiredEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionBookedCompensatedEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionBookedEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionBookingCanceledCompensatedEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionBookingCanceledEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionExpiredEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteBookedCompensatedEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteBookedEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteBookingCanceledCompensatedEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteBookingCanceledEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteExpiredEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferBookedCompensatedEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferBookedEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferBookingCanceledCompensatedEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferBookingCanceledEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferExpiredEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferRebookedEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferReservationCanceledEvent
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class CompensationEventsNewTest {
    private val correlationId = UUID.randomUUID()
    private val bookingId = UUID.randomUUID()
    private val accommodationId = UUID.randomUUID()
    private val attractionId = UUID.randomUUID()
    private val commuteId = UUID.randomUUID()
    private val travelOfferId = UUID.randomUUID()
    private val seat = Seat.Picked("1", "A")

    // Accommodation
    @Test
    fun accommodationBooked_toCompensation_should_map_fields() {
        val event =
            AccommodationBookedEvent(
                accommodationId = accommodationId,
                bookingId = bookingId,
            )

        val compensationEvent = event.toCompensation()

        assertTrue(compensationEvent is AccommodationBookedCompensatedEvent)
        assertEquals(accommodationId, compensationEvent.accommodationId)
        assertEquals(bookingId, compensationEvent.bookingId)
    }

    @Test
    fun accommodationBookingCanceled_toCompensation_should_map_fields() {
        val event =
            AccommodationBookingCanceledEvent(
                accommodationId = accommodationId,
                bookingId = bookingId,
            )

        val compensationEvent = event.toCompensation()

        assertTrue(compensationEvent is AccommodationBookingCanceledCompensatedEvent)
        assertEquals(accommodationId, compensationEvent.accommodationId)
        assertEquals(bookingId, compensationEvent.bookingId)
    }

    @Test
    fun accommodationExpired_toCompensation_should_throw() {
        val event =
            AccommodationExpiredEvent(
                accommodationId = accommodationId,
            )

        assertFailsWith<IllegalArgumentException> {
            event.toCompensation()
        }
    }

    // Attraction
    @Test
    fun attractionBooked_toCompensation_should_map_fields() {
        val event =
            AttractionBookedEvent(
                attractionId = attractionId,
                bookingId = bookingId,
            )

        val compensationEvent = event.toCompensation()

        assertTrue(compensationEvent is AttractionBookedCompensatedEvent)
        assertEquals(attractionId, compensationEvent.attractionId)
        assertEquals(bookingId, compensationEvent.bookingId)
    }

    @Test
    fun attractionBookingCanceled_toCompensation_should_map_fields() {
        val event =
            AttractionBookingCanceledEvent(
                attractionId = attractionId,
                bookingId = bookingId,
            )

        val compensationEvent = event.toCompensation()

        assertTrue(compensationEvent is AttractionBookingCanceledCompensatedEvent)
        assertEquals(attractionId, compensationEvent.attractionId)
        assertEquals(bookingId, compensationEvent.bookingId)
    }

    @Test
    fun attractionExpired_toCompensation_should_throw() {
        val event =
            AttractionExpiredEvent(
                attractionId = attractionId,
            )

        assertFailsWith<IllegalArgumentException> {
            event.toCompensation()
        }
    }

    // Commute
    @Test
    fun commuteBooked_toCompensation_should_map_fields() {
        val event =
            CommuteBookedEvent(
                commuteId = commuteId,
                bookingId = bookingId,
                seat = seat,
            )

        val compensationEvent = event.toCompensation()

        assertTrue(compensationEvent is CommuteBookedCompensatedEvent)
        assertEquals(commuteId, compensationEvent.commuteId)
        assertEquals(bookingId, compensationEvent.bookingId)
        assertEquals(seat, compensationEvent.seat)
    }

    @Test
    fun commuteBookingCanceled_toCompensation_should_map_fields() {
        val event =
            CommuteBookingCanceledEvent(
                commuteId = commuteId,
                bookingId = bookingId,
                seat = seat,
            )

        val compensationEvent = event.toCompensation()

        assertTrue(compensationEvent is CommuteBookingCanceledCompensatedEvent)
        assertEquals(commuteId, compensationEvent.commuteId)
        assertEquals(bookingId, compensationEvent.bookingId)
        assertEquals(seat, compensationEvent.seat)
    }

    @Test
    fun commuteExpired_toCompensation_should_return_original_event() {
        val event =
            CommuteExpiredEvent(
                commuteId = commuteId,
            )

        val compensationEvent = event.toCompensation()

        assertEquals(event, compensationEvent)
    }

    // Travel Offer
    @Test
    fun travelOfferBooked_toCompensation_should_map_fields() {
        val event =
            TravelOfferBookedEvent(
                travelOfferId = travelOfferId,
                accommodationId = accommodationId,
                commuteId = commuteId,
                attractionId = attractionId,
                bookingId = bookingId,
                seat = seat,
            )

        val compensationEvent = event.toCompensation()

        assertTrue(compensationEvent is TravelOfferBookedCompensatedEvent)
        assertEquals(travelOfferId, compensationEvent.travelOfferId)
        assertEquals(accommodationId, compensationEvent.accommodationId)
        assertEquals(commuteId, compensationEvent.commuteId)
        assertEquals(attractionId, compensationEvent.attractionId)
        assertEquals(bookingId, compensationEvent.bookingId)
        assertEquals(seat, compensationEvent.seat)
    }

    @Test
    fun travelOfferBookingCanceled_toCompensation_should_map_fields() {
        val event =
            TravelOfferBookingCanceledEvent(
                travelOfferId = travelOfferId,
                accommodationId = accommodationId,
                commuteId = commuteId,
                attractionId = attractionId,
                bookingId = bookingId,
                seat = seat,
            )

        val compensationEvent = event.toCompensation()

        assertTrue(compensationEvent is TravelOfferBookingCanceledCompensatedEvent)
        assertEquals(travelOfferId, compensationEvent.travelOfferId)
        assertEquals(accommodationId, compensationEvent.accommodationId)
        assertEquals(commuteId, compensationEvent.commuteId)
        assertEquals(attractionId, compensationEvent.attractionId)
        assertEquals(bookingId, compensationEvent.bookingId)
        assertEquals(seat, compensationEvent.seat)
    }

    @Test
    fun travelOfferExpired_toCompensation_should_return_original_event() {
        val event =
            TravelOfferExpiredEvent(
                travelOfferId = travelOfferId,
                accommodationId = accommodationId,
                commuteId = commuteId,
                attractionId = attractionId,
            )

        val result = event.toCompensation()

        assertEquals(event, result)
    }

    @Test
    fun travelOfferRebooked_toCompensation_should_return_original_event() {
        val event = TravelOfferRebookedEvent(travelOfferId = travelOfferId, bookingId = bookingId)

        val result = event.toCompensation()

        assertEquals(event, result)
    }

    @Test
    fun travelOfferReservationCanceled_toCompensation_should_return_original_event() {
        val event = TravelOfferReservationCanceledEvent(
            travelOfferId = travelOfferId,
            accommodationId = accommodationId,
            commuteId = commuteId,
            attractionId = attractionId,
            bookingId = bookingId,
            seat = seat,
        )

        val result = event.toCompensation()

        assertEquals(event, result)
    }
}
