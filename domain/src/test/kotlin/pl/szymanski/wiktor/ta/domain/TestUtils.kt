package pl.szymanski.wiktor.ta.domain

import pl.szymanski.wiktor.ta.domain.event.AccommodationBookedEvent
import pl.szymanski.wiktor.ta.domain.event.AccommodationBookingCanceledEvent
import pl.szymanski.wiktor.ta.domain.event.AccommodationEvent
import pl.szymanski.wiktor.ta.domain.event.AccommodationExpiredEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionBookedEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionBookingCanceledEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionExpiredEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteBookedEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteBookingCanceledEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteExpiredEvent
import pl.szymanski.wiktor.ta.domain.event.Event
import pl.szymanski.wiktor.ta.domain.event.TravelOfferBookedEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferBookingCanceledEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferExpiredEvent
import kotlin.test.assertEquals
import kotlin.test.fail

private fun assertCommuteEventEquals(expected: CommuteEvent, actual: CommuteEvent, message: String?) {
    assertEquals(expected.commuteId, actual.commuteId, message ?: "commuteId differs")
    
    when (expected) {
        is CommuteBookedEvent -> {
            actual as CommuteBookedEvent
            assertEquals(expected.userId, actual.userId, message ?: "userId differs")
            assertEquals(expected.seat, actual.seat, message ?: "seat differs")
        }
        is CommuteBookingCanceledEvent -> {
            actual as CommuteBookingCanceledEvent
            assertEquals(expected.userId, actual.userId, message ?: "userId differs")
            assertEquals(expected.seat, actual.seat, message ?: "seat differs")
        }
        is CommuteExpiredEvent -> {
            // Only commuteId needs to be checked, which is already done above
        }
        else -> throw IllegalArgumentException("Unsupported CommuteEvent type: ${expected::class}")
    }
}

private fun assertAccommodationEventEquals(expected: AccommodationEvent, actual: AccommodationEvent, message: String?) {
    assertEquals(expected.accommodationId, actual.accommodationId, message ?: "accommodationId differs")
    
    when (expected) {
        is AccommodationBookedEvent -> {
            actual as AccommodationBookedEvent
            assertEquals(expected.userId, actual.userId, message ?: "userId differs")
        }
        is AccommodationBookingCanceledEvent -> {
            actual as AccommodationBookingCanceledEvent
            assertEquals(expected.userId, actual.userId, message ?: "userId differs")
        }
        is AccommodationExpiredEvent -> {
            // Only accommodationId needs to be checked, which is already done above
        }
        else -> throw IllegalArgumentException("Unsupported AccommodationEvent type: ${expected::class}")
    }
}

private fun assertAttractionEventEquals(expected: AttractionEvent, actual: AttractionEvent, message: String?) {
    assertEquals(expected.attractionId, actual.attractionId, message ?: "attractionId differs")
    
    when (expected) {
        is AttractionBookedEvent -> {
            actual as AttractionBookedEvent
            assertEquals(expected.userId, actual.userId, message ?: "userId differs")
        }
        is AttractionBookingCanceledEvent -> {
            actual as AttractionBookingCanceledEvent
            assertEquals(expected.userId, actual.userId, message ?: "userId differs")
        }
        is AttractionExpiredEvent -> {
            // Only attractionId needs to be checked, which is already done above
        }
        else -> throw IllegalArgumentException("Unsupported AttractionEvent type: ${expected::class}")
    }
}

private fun assertTravelOfferEventEquals(expected: TravelOfferEvent, actual: TravelOfferEvent, message: String?) {
    assertEquals(expected.travelOfferId, actual.travelOfferId, message ?: "travelOfferId differs")
    
    when (expected) {
        is TravelOfferBookedEvent -> {
            actual as TravelOfferBookedEvent
            assertEquals(expected.accommodationId, actual.accommodationId, message ?: "accommodationId differs")
            assertEquals(expected.commuteId, actual.commuteId, message ?: "commuteId differs")
            assertEquals(expected.attractionId, actual.attractionId, message ?: "attractionId differs")
            assertEquals(expected.userId, actual.userId, message ?: "userId differs")
            assertEquals(expected.seat, actual.seat, message ?: "seat differs")
        }
        is TravelOfferBookingCanceledEvent -> {
            actual as TravelOfferBookingCanceledEvent
            assertEquals(expected.accommodationId, actual.accommodationId, message ?: "accommodationId differs")
            assertEquals(expected.commuteId, actual.commuteId, message ?: "commuteId differs")
            assertEquals(expected.attractionId, actual.attractionId, message ?: "attractionId differs")
            assertEquals(expected.userId, actual.userId, message ?: "userId differs")
            assertEquals(expected.seat, actual.seat, message ?: "seat differs")
        }
        is TravelOfferExpiredEvent -> {
            // Only travelOfferId needs to be checked, which is already done above
        }
        else -> throw IllegalArgumentException("Unsupported TravelOfferEvent type: ${expected::class}")
    }
}

fun assertEventEquals(expected: Event, actual: Event, message: String? = null) {
    if (expected === actual) {
        return
    }

    if (expected::class != actual::class) {
        fail(message ?: "Events are not of the same type: expected ${expected::class}, actual ${actual::class}")
    }

    when (expected) {
        is CommuteEvent -> assertCommuteEventEquals(expected, actual as CommuteEvent, message)
        is AccommodationEvent -> assertAccommodationEventEquals(expected, actual as AccommodationEvent, message)
        is AttractionEvent -> assertAttractionEventEquals(expected, actual as AttractionEvent, message)
        is TravelOfferEvent -> assertTravelOfferEventEquals(expected, actual as TravelOfferEvent, message)
        else -> fail(message ?: "Unsupported event type: ${expected::class}")
    }
}
