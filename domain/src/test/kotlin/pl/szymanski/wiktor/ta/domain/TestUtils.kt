package pl.szymanski.wiktor.ta.domain

import pl.szymanski.wiktor.ta.domain.event.AccommodationBookedEvent
import pl.szymanski.wiktor.ta.domain.event.AccommodationBookedCompensatedEvent
import pl.szymanski.wiktor.ta.domain.event.AccommodationBookingCanceledEvent
import pl.szymanski.wiktor.ta.domain.event.AccommodationBookingCanceledCompensatedEvent
import pl.szymanski.wiktor.ta.domain.event.AccommodationCreatedEvent
import pl.szymanski.wiktor.ta.domain.event.AccommodationExpiredEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionAvailableEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionBookedCompensatedEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionBookedEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionBookingCanceledCompensatedEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionBookingCanceledEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionCreatedEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionExpiredEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionFullEvent
import pl.szymanski.wiktor.ta.domain.event.BookingCancelRequestedEvent
import pl.szymanski.wiktor.ta.domain.event.BookingCreatedEvent
import pl.szymanski.wiktor.ta.domain.event.CancelBookingEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteAvailableEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteBookedCompensatedEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteBookedEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteBookingCanceledCompensatedEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteBookingCanceledEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteCreatedEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteExpiredEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteFullEvent
import pl.szymanski.wiktor.ta.domain.event.CompleteBookingEvent
import pl.szymanski.wiktor.ta.domain.event.DomainEvent
import pl.szymanski.wiktor.ta.domain.event.FailBookingEvent
import pl.szymanski.wiktor.ta.domain.event.FailCancelBookingEvent
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.fail

fun DomainEvent.copy(
    eventId: UUID,
): DomainEvent =
    when (this) {
        is AccommodationCreatedEvent -> copy(eventId = eventId)
        is AccommodationBookedEvent -> copy(eventId = eventId)
        is AccommodationBookingCanceledEvent -> copy(eventId = eventId)
        is AccommodationExpiredEvent -> copy(eventId = eventId)
        is AccommodationBookedCompensatedEvent -> copy(eventId = eventId)
        is AccommodationBookingCanceledCompensatedEvent -> copy(eventId = eventId)
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
        is BookingCreatedEvent -> copy(eventId = eventId)
        is CompleteBookingEvent -> copy(eventId = eventId)
        is BookingCancelRequestedEvent -> copy(eventId = eventId)
        is CancelBookingEvent -> copy(eventId = eventId)
        is FailBookingEvent -> copy(eventId = eventId)
        is FailCancelBookingEvent -> copy(eventId = eventId)
        is AttractionBookedCompensatedEvent -> copy(eventId = eventId)
        is AttractionBookingCanceledCompensatedEvent -> copy(eventId = eventId)
        is CommuteBookedCompensatedEvent -> copy(eventId = eventId)
        is CommuteBookingCanceledCompensatedEvent -> copy(eventId = eventId)
    }

fun assertEventEquals(
    expected: DomainEvent,
    actual: DomainEvent,
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
