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
import pl.szymanski.wiktor.ta.domain.event.TravelOfferBookedCompensatedEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferBookingCanceledEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferBookingCanceledCompensatedEvent
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
): Event =
    when (this) {
        is AccommodationCreatedEvent -> copy(eventId = eventId)
        is AccommodationBookedEvent -> copy(eventId = eventId)
        is AccommodationBookingCanceledEvent -> copy(eventId = eventId)
        is AccommodationExpiredEvent -> copy(eventId = eventId)
        is AttractionCreatedEvent -> copy(eventId = eventId)
        is AttractionBookedEvent -> copy(eventId = eventId)
        is AttractionBookingCanceledEvent -> copy(eventId = eventId)
        is AttractionExpiredEvent -> copy(eventId = eventId)
        is AttractionFullEvent -> copy(eventId = eventId)
        is AttractionAvailableEvent -> copy(eventId = eventId)
        is CommuteCreatedEvent -> copy(eventId = eventId)
        is CommuteBookedEvent -> copy(eventId = eventId)
        is CommuteBookingCanceledEvent -> copy(eventId = eventId)
        is CommuteExpiredEvent -> copy(eventId = eventId)
        is CommuteFullEvent -> copy(eventId = eventId)
        is CommuteAvailableEvent -> copy(eventId = eventId)
        is TravelOfferCreatedEvent -> copy(eventId = eventId)
        is TravelOfferReservedEvent -> copy(eventId = eventId)
        is TravelOfferReservationCanceledEvent -> copy(eventId = eventId)
        is TravelOfferBookedEvent -> copy(eventId = eventId)
        is TravelOfferBookedCompensatedEvent -> copy(eventId = eventId)
        is TravelOfferReleaseEvent -> copy(eventId = eventId)
        is TravelOfferBookingCanceledEvent -> copy(eventId = eventId)
        is TravelOfferBookingCanceledCompensatedEvent -> copy(eventId = eventId)
        is TravelOfferRebookedEvent -> copy(eventId = eventId)
        is TravelOfferExpiredEvent -> copy(eventId = eventId)
        is TravelOfferMadeUnavailableEvent -> copy(eventId = eventId)
        is TravelOfferMadeAvailableEvent -> copy(eventId = eventId)
        is BookingCreatedEvent -> copy(eventId = eventId)
        is ProcessBookingEvent -> copy(eventId = eventId)
        is CompleteBookingEvent -> copy(eventId = eventId)
        is BookingCancelRequestedEvent -> copy(eventId = eventId)
        is CancelBookingEvent -> copy(eventId = eventId)
        is ProcessCancelBookingEvent -> copy(eventId = eventId)
        is FailBookingEvent -> copy(eventId = eventId)
        is FailCancelBookingEvent -> copy(eventId = eventId)
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
        )

    assertEquals(properExpected, actual, message)
}
