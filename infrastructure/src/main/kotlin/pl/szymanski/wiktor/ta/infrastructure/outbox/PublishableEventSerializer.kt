package pl.szymanski.wiktor.ta.infrastructure.outbox

import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import pl.szymanski.wiktor.ta.domain.event.AccommodationBookedCompensatedEvent
import pl.szymanski.wiktor.ta.domain.event.AccommodationBookedEvent
import pl.szymanski.wiktor.ta.domain.event.AccommodationBookingCanceledCompensatedEvent
import pl.szymanski.wiktor.ta.domain.event.AccommodationBookingCanceledEvent
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
import pl.szymanski.wiktor.ta.domain.event.CompleteBookingEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteAvailableEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteBookedCompensatedEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteBookedEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteBookingCanceledCompensatedEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteBookingCanceledEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteCreatedEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteExpiredEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteFullEvent
import pl.szymanski.wiktor.ta.domain.event.FailBookingEvent
import pl.szymanski.wiktor.ta.domain.event.FailCancelBookingEvent
import pl.szymanski.wiktor.ta.domain.event.ProcessBookingEvent
import pl.szymanski.wiktor.ta.domain.event.ProcessCancelBookingEvent
import pl.szymanski.wiktor.ta.domain.event.PublishableEvent
import pl.szymanski.wiktor.ta.event.AccommodationDateMetEvent
import pl.szymanski.wiktor.ta.event.AttractionDateMetEvent
import pl.szymanski.wiktor.ta.event.BookingCancelSagaCompletedEvent
import pl.szymanski.wiktor.ta.event.BookingCancelSagaFailedEvent
import pl.szymanski.wiktor.ta.event.BookingCancelSagaStartedEvent
import pl.szymanski.wiktor.ta.event.BookingSagaCompletedEvent
import pl.szymanski.wiktor.ta.event.BookingSagaFailedEvent
import pl.szymanski.wiktor.ta.event.BookingSagaStartedEvent
import pl.szymanski.wiktor.ta.event.CommuteDateMetEvent

val publishableEventModule = SerializersModule {
    polymorphic(PublishableEvent::class) {
        // CommuteEvent
        subclass(CommuteCreatedEvent::class)
        subclass(CommuteBookedEvent::class)
        subclass(CommuteBookingCanceledEvent::class)
        subclass(CommuteExpiredEvent::class)
        subclass(CommuteFullEvent::class)
        subclass(CommuteAvailableEvent::class)
        subclass(CommuteBookedCompensatedEvent::class)
        subclass(CommuteBookingCanceledCompensatedEvent::class)
        // AccommodationEvent
        subclass(AccommodationCreatedEvent::class)
        subclass(AccommodationBookedEvent::class)
        subclass(AccommodationBookingCanceledEvent::class)
        subclass(AccommodationExpiredEvent::class)
        subclass(AccommodationBookedCompensatedEvent::class)
        subclass(AccommodationBookingCanceledCompensatedEvent::class)
        // AttractionEvent
        subclass(AttractionCreatedEvent::class)
        subclass(AttractionBookedEvent::class)
        subclass(AttractionBookingCanceledEvent::class)
        subclass(AttractionExpiredEvent::class)
        subclass(AttractionFullEvent::class)
        subclass(AttractionAvailableEvent::class)
        subclass(AttractionBookedCompensatedEvent::class)
        subclass(AttractionBookingCanceledCompensatedEvent::class)
        // BookingEvent
        subclass(BookingCreatedEvent::class)
        subclass(ProcessBookingEvent::class)
        subclass(CompleteBookingEvent::class)
        subclass(BookingCancelRequestedEvent::class)
        subclass(CancelBookingEvent::class)
        subclass(ProcessCancelBookingEvent::class)
        subclass(FailBookingEvent::class)
        subclass(FailCancelBookingEvent::class)
        // SagaEvent
        subclass(BookingSagaStartedEvent::class)
        subclass(BookingSagaFailedEvent::class)
        subclass(BookingSagaCompletedEvent::class)
        subclass(BookingCancelSagaStartedEvent::class)
        subclass(BookingCancelSagaFailedEvent::class)
        subclass(BookingCancelSagaCompletedEvent::class)
        // DateMetEvent
        subclass(CommuteDateMetEvent::class)
        subclass(AccommodationDateMetEvent::class)
        subclass(AttractionDateMetEvent::class)
    }
}
