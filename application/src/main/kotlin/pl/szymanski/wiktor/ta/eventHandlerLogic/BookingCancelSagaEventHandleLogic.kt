package pl.szymanski.wiktor.ta.eventHandlerLogic

import pl.szymanski.wiktor.ta.EventEnvelope
import pl.szymanski.wiktor.ta.commands.booking.cancel.CancelBookingCommand
import pl.szymanski.wiktor.ta.commands.booking.cancel.CancelBookingCommandHandler
import pl.szymanski.wiktor.ta.commands.booking.failCancel.FailCancelBookingCommand
import pl.szymanski.wiktor.ta.commands.booking.failCancel.FailCancelBookingCommandHandler
import pl.szymanski.wiktor.ta.domain.aggregate.BookingId
import pl.szymanski.wiktor.ta.event.BookingCancelSagaCompletedEvent
import pl.szymanski.wiktor.ta.event.BookingCancelSagaFailedEvent

suspend fun onBookingCancelSagaCompletedEvent(
    cancelBookingCommandHandler: CancelBookingCommandHandler,
    envelope: EventEnvelope<BookingCancelSagaCompletedEvent>
) = cancelBookingCommandHandler.handle(
        CancelBookingCommand(
            envelope.metadata.correlationId,
            BookingId.from(envelope.event.bookingId),
        )
    )

suspend fun onBookingCancelSagaFailedEvent(
    failCancelBookingCommandHandler: FailCancelBookingCommandHandler,
    envelope: EventEnvelope<BookingCancelSagaFailedEvent>
) = failCancelBookingCommandHandler.handle(
        FailCancelBookingCommand(
            bookingId = BookingId.from(envelope.event.bookingId),
            correlationId = envelope.metadata.correlationId,
            message = envelope.event.message,
        )
    )