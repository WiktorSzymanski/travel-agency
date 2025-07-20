package pl.szymanski.wiktor.ta.event

import pl.szymanski.wiktor.ta.domain.Seat
import pl.szymanski.wiktor.ta.domain.event.AccommodationBookedEvent
import pl.szymanski.wiktor.ta.domain.event.AccommodationBookingCanceledEvent
import pl.szymanski.wiktor.ta.domain.event.AccommodationExpiredEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionBookedEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionBookingCanceledEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionExpiredEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteBookedEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteBookingCanceledEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteExpiredEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferBookedEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferBookingCanceledEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferExpiredEvent
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class CompensationEventsTest {
    private val correlationId = UUID.randomUUID()
    private val userId = UUID.randomUUID()
    private val accommodationId = UUID.randomUUID()
    private val attractionId = UUID.randomUUID()
    private val commuteId = UUID.randomUUID()
    private val travelOfferId = UUID.randomUUID()
    private val seat = Seat("1", "A")

    @Test
    fun `AccommodationBookedEvent toCompensation should return AccommodationBookedCompensatedEvent`() {
        // Given
        val event =
            AccommodationBookedEvent(
                correlationId = correlationId,
                accommodationId = accommodationId,
                userId = userId,
            )

        // When
        val compensationEvent = event.toCompensation()

        // Then
        assertTrue(compensationEvent is AccommodationBookedCompensatedEvent)
        compensationEvent
        assertEquals(correlationId, compensationEvent.correlationId)
        assertEquals(accommodationId, compensationEvent.accommodationId)
        assertEquals(userId, compensationEvent.userId)
    }

    @Test
    fun `AccommodationBookingCanceledEvent toCompensation should return AccommodationBookingCanceledCompensatedEvent`() {
        // Given
        val event =
            AccommodationBookingCanceledEvent(
                correlationId = correlationId,
                accommodationId = accommodationId,
                userId = userId,
            )

        // When
        val compensationEvent = event.toCompensation()

        // Then
        assertTrue(compensationEvent is AccommodationBookingCanceledCompensatedEvent)
        compensationEvent
        assertEquals(correlationId, compensationEvent.correlationId)
        assertEquals(accommodationId, compensationEvent.accommodationId)
        assertEquals(userId, compensationEvent.userId)
    }

    @Test
    fun `AccommodationExpiredEvent toCompensation should throw IllegalArgumentException`() {
        // Given
        val event =
            AccommodationExpiredEvent(
                correlationId = correlationId,
                accommodationId = accommodationId,
            )

        // When/Then
        assertFailsWith<IllegalArgumentException> {
            event.toCompensation()
        }
    }

    @Test
    fun `AttractionBookedEvent toCompensation should return AttractionBookedCompensatedEvent`() {
        // Given
        val event =
            AttractionBookedEvent(
                correlationId = correlationId,
                attractionId = attractionId,
                userId = userId,
            )

        // When
        val compensationEvent = event.toCompensation()

        // Then
        assertTrue(compensationEvent is AttractionBookedCompensatedEvent)
        compensationEvent
        assertEquals(correlationId, compensationEvent.correlationId)
        assertEquals(attractionId, compensationEvent.attractionId)
        assertEquals(userId, compensationEvent.userId)
    }

    @Test
    fun `AttractionBookingCanceledEvent toCompensation should return AttractionBookingCanceledCompensatedEvent`() {
        // Given
        val event =
            AttractionBookingCanceledEvent(
                correlationId = correlationId,
                attractionId = attractionId,
                userId = userId,
            )

        // When
        val compensationEvent = event.toCompensation()

        // Then
        assertTrue(compensationEvent is AttractionBookingCanceledCompensatedEvent)
        compensationEvent
        assertEquals(correlationId, compensationEvent.correlationId)
        assertEquals(attractionId, compensationEvent.attractionId)
        assertEquals(userId, compensationEvent.userId)
    }

    @Test
    fun `AttractionExpiredEvent toCompensation should throw IllegalArgumentException`() {
        // Given
        val event =
            AttractionExpiredEvent(
                correlationId = correlationId,
                attractionId = attractionId,
            )

        // When/Then
        assertFailsWith<IllegalArgumentException> {
            event.toCompensation()
        }
    }

    @Test
    fun `CommuteBookedEvent toCompensation should return CommuteBookedCompensatedEvent`() {
        // Given
        val event =
            CommuteBookedEvent(
                correlationId = correlationId,
                commuteId = commuteId,
                userId = userId,
                seat = seat,
            )

        // When
        val compensationEvent = event.toCompensation()

        // Then
        assertTrue(compensationEvent is CommuteBookedCompensatedEvent)
        compensationEvent
        assertEquals(correlationId, compensationEvent.correlationId)
        assertEquals(commuteId, compensationEvent.commuteId)
        assertEquals(userId, compensationEvent.userId)
        assertEquals(seat, compensationEvent.seat)
    }

    @Test
    fun `CommuteBookingCanceledEvent toCompensation should return CommuteBookingCanceledCompensatedEvent`() {
        // Given
        val event =
            CommuteBookingCanceledEvent(
                correlationId = correlationId,
                commuteId = commuteId,
                userId = userId,
                seat = seat,
            )

        // When
        val compensationEvent = event.toCompensation()

        // Then
        assertTrue(compensationEvent is CommuteBookingCanceledCompensatedEvent)
        compensationEvent
        assertEquals(correlationId, compensationEvent.correlationId)
        assertEquals(commuteId, compensationEvent.commuteId)
        assertEquals(userId, compensationEvent.userId)
        assertEquals(seat, compensationEvent.seat)
    }

    @Test
    fun `CommuteExpiredEvent toCompensation should throw IllegalArgumentException`() {
        // Given
        val event =
            CommuteExpiredEvent(
                correlationId = correlationId,
                commuteId = commuteId,
            )

        // When/Then
        assertFailsWith<IllegalArgumentException> {
            event.toCompensation()
        }
    }

    @Test
    fun `TravelOfferBookedEvent toCompensation should return TravelOfferBookedCompensatedEvent`() {
        // Given
        val event =
            TravelOfferBookedEvent(
                correlationId = correlationId,
                travelOfferId = travelOfferId,
                accommodationId = accommodationId,
                commuteId = commuteId,
                attractionId = attractionId,
                userId = userId,
                seat = seat,
            )

        // When
        val compensationEvent = event.toCompensation()

        // Then
        assertTrue(compensationEvent is TravelOfferBookedCompensatedEvent)
        compensationEvent
        assertEquals(correlationId, compensationEvent.correlationId)
        assertEquals(travelOfferId, compensationEvent.travelOfferId)
        assertEquals(accommodationId, compensationEvent.accommodationId)
        assertEquals(commuteId, compensationEvent.commuteId)
        assertEquals(attractionId, compensationEvent.attractionId)
        assertEquals(userId, compensationEvent.userId)
        assertEquals(seat, compensationEvent.seat)
    }

    @Test
    fun `TravelOfferBookingCanceledEvent toCompensation should return TravelOfferBookingCanceledCompensatedEvent`() {
        // Given
        val event =
            TravelOfferBookingCanceledEvent(
                correlationId = correlationId,
                travelOfferId = travelOfferId,
                accommodationId = accommodationId,
                commuteId = commuteId,
                attractionId = attractionId,
                userId = userId,
                seat = seat,
            )

        // When
        val compensationEvent = event.toCompensation()

        // Then
        assertTrue(compensationEvent is TravelOfferBookingCanceledCompensatedEvent)
        compensationEvent
        assertEquals(correlationId, compensationEvent.correlationId)
        assertEquals(travelOfferId, compensationEvent.travelOfferId)
        assertEquals(accommodationId, compensationEvent.accommodationId)
        assertEquals(commuteId, compensationEvent.commuteId)
        assertEquals(attractionId, compensationEvent.attractionId)
        assertEquals(userId, compensationEvent.userId)
        assertEquals(seat, compensationEvent.seat)
    }

    @Test
    fun `TravelOfferExpiredEvent toCompensation should throw IllegalArgumentException`() {
        // Given
        val event =
            TravelOfferExpiredEvent(
                correlationId = correlationId,
                travelOfferId = travelOfferId,
            )

        // When/Then
        assertFailsWith<IllegalArgumentException> {
            event.toCompensation()
        }
    }
}
