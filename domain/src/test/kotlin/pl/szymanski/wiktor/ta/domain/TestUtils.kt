package pl.szymanski.wiktor.ta.domain

import pl.szymanski.wiktor.ta.domain.event.AccommodationBookedEvent
import pl.szymanski.wiktor.ta.domain.event.AccommodationBookingCanceledEvent
import pl.szymanski.wiktor.ta.domain.event.AccommodationCreatedEvent
import pl.szymanski.wiktor.ta.domain.event.AccommodationExpiredEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionAvailableEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionBookedEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionBookingCanceledEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionCreatedEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionExpiredEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionFullEvent
import pl.szymanski.wiktor.ta.domain.event.BookingCancelRequestedEvent
import pl.szymanski.wiktor.ta.domain.event.BookingCreatedEvent
import pl.szymanski.wiktor.ta.domain.event.CancelBookingEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteAvailableEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteBookedEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteBookingCanceledEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteCreatedEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteExpiredEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteFullEvent
import pl.szymanski.wiktor.ta.domain.event.CompleteBookingEvent
import pl.szymanski.wiktor.ta.domain.event.Event
import pl.szymanski.wiktor.ta.domain.event.FailBookingEvent
import pl.szymanski.wiktor.ta.domain.event.FailCancelBookingEvent
import pl.szymanski.wiktor.ta.domain.event.ProcessBookingEvent
import pl.szymanski.wiktor.ta.domain.event.ProcessCancelBookingEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferBookedEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferBookingCanceledEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferCreatedEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferExpiredEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferMadeAvailableEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferMadeUnavailableEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferRebookedEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferReleaseEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferReservationCanceledEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferReservedEvent
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.fail

fun Event.copy(
    eventId: UUID,
    correlationId: UUID?,
): Event =
    when (this) {
        is AccommodationCreatedEvent -> copy(eventId = eventId, correlationId = correlationId)
        is AccommodationBookedEvent -> copy(eventId = eventId, correlationId = correlationId)
        is AccommodationBookingCanceledEvent -> copy(eventId = eventId, correlationId = correlationId)
        is AccommodationExpiredEvent -> copy(eventId = eventId, correlationId = correlationId)
        is AttractionCreatedEvent -> copy(eventId = eventId, correlationId = correlationId)
        is AttractionBookedEvent -> copy(eventId = eventId, correlationId = correlationId)
        is AttractionBookingCanceledEvent -> copy(eventId = eventId, correlationId = correlationId)
        is AttractionExpiredEvent -> copy(eventId = eventId, correlationId = correlationId)
        is AttractionFullEvent -> copy(eventId = eventId, correlationId = correlationId)
        is AttractionAvailableEvent -> copy(eventId = eventId, correlationId = correlationId)
        is CommuteCreatedEvent -> copy(eventId = eventId, correlationId = correlationId)
        is CommuteBookedEvent -> copy(eventId = eventId, correlationId = correlationId)
        is CommuteBookingCanceledEvent -> copy(eventId = eventId, correlationId = correlationId)
        is CommuteExpiredEvent -> copy(eventId = eventId, correlationId = correlationId)
        is CommuteFullEvent -> copy(eventId = eventId, correlationId = correlationId)
        is CommuteAvailableEvent -> copy(eventId = eventId, correlationId = correlationId)
        is TravelOfferCreatedEvent -> copy(eventId = eventId, correlationId = correlationId)
        is TravelOfferReservedEvent -> copy(eventId = eventId, correlationId = correlationId)
        is TravelOfferReservationCanceledEvent -> copy(eventId = eventId, correlationId = correlationId)
        is TravelOfferBookedEvent -> copy(eventId = eventId, correlationId = correlationId)
        is TravelOfferReleaseEvent -> copy(eventId = eventId, correlationId = correlationId)
        is TravelOfferBookingCanceledEvent -> copy(eventId = eventId, correlationId = correlationId)
        is TravelOfferRebookedEvent -> copy(eventId = eventId, correlationId = correlationId)
        is TravelOfferExpiredEvent -> copy(eventId = eventId, correlationId = correlationId)
        is TravelOfferMadeUnavailableEvent -> copy(eventId = eventId, correlationId = correlationId)
        is TravelOfferMadeAvailableEvent -> copy(eventId = eventId, correlationId = correlationId)
        is BookingCreatedEvent -> copy(eventId = eventId, correlationId = correlationId)
        is ProcessBookingEvent -> copy(eventId = eventId, correlationId = correlationId)
        is CompleteBookingEvent -> copy(eventId = eventId, correlationId = correlationId)
        is BookingCancelRequestedEvent -> copy(eventId = eventId, correlationId = correlationId)
        is CancelBookingEvent -> copy(eventId = eventId, correlationId = correlationId)
        is ProcessCancelBookingEvent -> copy(eventId = eventId, correlationId = correlationId)
        is FailBookingEvent -> copy(eventId = eventId, correlationId = correlationId)
        is FailCancelBookingEvent -> copy(eventId = eventId, correlationId = correlationId)
        else -> {
            fail("Unsupported event type: ${this::class}")
        }
    }

fun assertEventEquals(
    expected: Event,
    actual: Event,
    message: String? = null,
) {
    if (expected === actual) return

    if (expected::class != actual::class) {
        fail(message ?: "Events are not of the same type: expected ${expected::class}, actual ${actual::class}")
    }

    val properExpected =
        expected.copy(
            eventId = actual.eventId,
            correlationId = actual.correlationId,
        )

    assertEquals(properExpected, actual, message)
}
