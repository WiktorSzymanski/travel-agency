package pl.szymanski.wiktor.ta.eventHandlerLogic

import pl.szymanski.wiktor.ta.EventEnvelope
import pl.szymanski.wiktor.ta.commands.booking.complete.CompleteBookingCommand
import pl.szymanski.wiktor.ta.commands.booking.complete.CompleteBookingCommandHandler
import pl.szymanski.wiktor.ta.commands.booking.fail.FailBookingCommand
import pl.szymanski.wiktor.ta.commands.booking.fail.FailBookingCommandHandler
import pl.szymanski.wiktor.ta.domain.aggregate.BookingId
import pl.szymanski.wiktor.ta.event.BookingSagaCompletedEvent
import pl.szymanski.wiktor.ta.event.BookingSagaFailedEvent

suspend fun onBookingSagaCompletedEvent(
    completeBookingCommandHandler: CompleteBookingCommandHandler,
    envelope: EventEnvelope<BookingSagaCompletedEvent>
) = completeBookingCommandHandler.handle(
    CompleteBookingCommand(
        envelope.metadata.correlationId,
        BookingId.from(envelope.event.bookingId),
    )
)

suspend fun onBookingSagaFailedEvent(
    failBookingCommandHandler: FailBookingCommandHandler,
    envelope: EventEnvelope<BookingSagaFailedEvent>
) = failBookingCommandHandler.handle(
    FailBookingCommand(
        envelope.metadata.correlationId,
        BookingId.from(envelope.event.bookingId),
        message = envelope.event.message,
    )
)
