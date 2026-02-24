package pl.szymanski.wiktor.ta.eventHandlerLogic

import pl.szymanski.wiktor.ta.EventEnvelope
import pl.szymanski.wiktor.ta.commands.booking.cancel.CancelBookingCommand
import pl.szymanski.wiktor.ta.commands.booking.cancel.CancelBookingCommandHandler
import pl.szymanski.wiktor.ta.commands.booking.failCancel.FailCancelBookingCommand
import pl.szymanski.wiktor.ta.commands.booking.failCancel.FailCancelBookingCommandHandler
import pl.szymanski.wiktor.ta.commands.booking.processCancel.ProcessCancelBookingCommand
import pl.szymanski.wiktor.ta.commands.booking.processCancel.ProcessCancelBookingCommandHandler
import pl.szymanski.wiktor.ta.event.BookingCancelSagaCompletedEvent
import pl.szymanski.wiktor.ta.event.BookingCancelSagaFailedEvent
import pl.szymanski.wiktor.ta.event.BookingCancelSagaStartedEvent


suspend fun onBookingCancelSagaStartedEvent(
    processCancelBookingCommandHandler: ProcessCancelBookingCommandHandler,
    envelope: EventEnvelope<BookingCancelSagaStartedEvent>
) = processCancelBookingCommandHandler.handle(
    ProcessCancelBookingCommand(
        envelope.metadata.correlationId,
        envelope.event.bookingId,
        )
    )

suspend fun onBookingCancelSagaCompletedEvent(
    cancelBookingCommandHandler: CancelBookingCommandHandler,
    envelope: EventEnvelope<BookingCancelSagaCompletedEvent>
) = cancelBookingCommandHandler.handle(
        CancelBookingCommand(
            envelope.metadata.correlationId,
            envelope.event.bookingId,
        )
    )

suspend fun onBookingCancelSagaFailedEvent(
    failCancelBookingCommandHandler: FailCancelBookingCommandHandler,
    envelope: EventEnvelope<BookingCancelSagaFailedEvent>
) = failCancelBookingCommandHandler.handle(
        FailCancelBookingCommand(
            bookingId = envelope.event.bookingId,
            correlationId = envelope.metadata.correlationId,
            message = envelope.event.message,
        )
    )