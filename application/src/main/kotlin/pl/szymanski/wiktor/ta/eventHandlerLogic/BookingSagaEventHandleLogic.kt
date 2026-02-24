package pl.szymanski.wiktor.ta.eventHandlerLogic

import pl.szymanski.wiktor.ta.EventEnvelope
import pl.szymanski.wiktor.ta.commands.booking.complete.CompleteBookingCommand
import pl.szymanski.wiktor.ta.commands.booking.complete.CompleteBookingCommandHandler
import pl.szymanski.wiktor.ta.commands.booking.fail.FailBookingCommand
import pl.szymanski.wiktor.ta.commands.booking.fail.FailBookingCommandHandler
import pl.szymanski.wiktor.ta.commands.booking.process.ProcessBookingCommand
import pl.szymanski.wiktor.ta.commands.booking.process.ProcessBookingCommandHandler
import pl.szymanski.wiktor.ta.event.BookingSagaCompletedEvent
import pl.szymanski.wiktor.ta.event.BookingSagaFailedEvent
import pl.szymanski.wiktor.ta.event.BookingSagaStartedEvent

suspend fun onBookingSagaStartedEvent(
    processBookingCommandHandler: ProcessBookingCommandHandler,
    envelope: EventEnvelope<BookingSagaStartedEvent>
) = processBookingCommandHandler.handle(
    ProcessBookingCommand(
        envelope.metadata.correlationId,
        envelope.event.bookingId,
    )
)

suspend fun onBookingSagaCompletedEvent(
    completeBookingCommandHandler: CompleteBookingCommandHandler,
    envelope: EventEnvelope<BookingSagaCompletedEvent>
) = completeBookingCommandHandler.handle(
    CompleteBookingCommand(
        envelope.metadata.correlationId,
        envelope.event.bookingId,
    )
)

suspend fun onBookingSagaFailedEvent(
    failBookingCommandHandler: FailBookingCommandHandler,
    envelope: EventEnvelope<BookingSagaFailedEvent>
) = failBookingCommandHandler.handle(
    FailBookingCommand(
        envelope.metadata.correlationId,
        envelope.event.bookingId,
        message = envelope.event.message,
    )
)
