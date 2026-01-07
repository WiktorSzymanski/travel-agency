package pl.szymanski.wiktor.ta.eventHandlerLogic

import pl.szymanski.wiktor.ta.CommandBus
import pl.szymanski.wiktor.ta.EventBus
import pl.szymanski.wiktor.ta.EventEnvelope
import pl.szymanski.wiktor.ta.domain.event.BookingCancelRequestedEvent
import pl.szymanski.wiktor.ta.domain.event.BookingCreatedEvent
import pl.szymanski.wiktor.ta.saga.BookingSaga
import pl.szymanski.wiktor.ta.saga.CancelBookingSaga

class BookingEventHandleLogic(
    private val eventBus: EventBus,
    private val commandBus: CommandBus
) {
    suspend fun onCreatedEvent(envelope: EventEnvelope<BookingCreatedEvent>) =
        BookingSaga(
            eventBus,
            commandBus,
            envelope.event.travelOffer,
            envelope.event.seat,
            envelope.event.bookingId,
            envelope.metadata,
        ).execute()

    suspend fun onCancelRequestedEvent(envelope: EventEnvelope<BookingCancelRequestedEvent>) =
        CancelBookingSaga(
            eventBus,
            commandBus,
            envelope.event.travelOffer,
            envelope.event.seat,
            envelope.event.bookingId,
            envelope.metadata,
        ).execute()
}
