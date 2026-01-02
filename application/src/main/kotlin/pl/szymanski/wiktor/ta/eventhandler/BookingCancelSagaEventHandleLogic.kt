package pl.szymanski.wiktor.ta.eventhandler

import pl.szymanski.wiktor.ta.CommandBus
import pl.szymanski.wiktor.ta.EventEnvelope
import pl.szymanski.wiktor.ta.command.BookingCommand
import pl.szymanski.wiktor.ta.command.CancelBookTravelOfferCommand
import pl.szymanski.wiktor.ta.command.CancelBookingCommand
import pl.szymanski.wiktor.ta.command.FailCancelBookingCommand
import pl.szymanski.wiktor.ta.command.ProcessCancelBookingCommand
import pl.szymanski.wiktor.ta.command.TravelOfferCommand
import pl.szymanski.wiktor.ta.domain.aggregate.Booking
import pl.szymanski.wiktor.ta.domain.aggregate.TravelOffer
import pl.szymanski.wiktor.ta.event.BookingCancelSagaCompletedEvent
import pl.szymanski.wiktor.ta.event.BookingCancelSagaFailedEvent
import pl.szymanski.wiktor.ta.event.BookingCancelSagaStartedEvent

class BookingCancelSagaEventHandleLogic(
    private val commandBus: CommandBus
) {
    suspend fun onStartedEvent(envelope: EventEnvelope<BookingCancelSagaStartedEvent>) =
        commandBus.dispatch<BookingCommand, Booking>(
            ProcessCancelBookingCommand(
                envelope.event.bookingId,
                envelope.metadata.correlationId,
            )
        )

    suspend fun onCompletedEvent(envelope: EventEnvelope<BookingCancelSagaCompletedEvent>) =
        commandBus.dispatch<BookingCommand, Booking>(
            CancelBookingCommand(
                envelope.event.bookingId,
                envelope.metadata.correlationId,
            )
        )

    suspend fun onCompletedEvent2(envelope: EventEnvelope<BookingCancelSagaCompletedEvent>) =
        commandBus.dispatch<TravelOfferCommand, TravelOffer>(
            CancelBookTravelOfferCommand(
                envelope.event.travelOfferId,
                envelope.metadata.correlationId,
                bookingId = envelope.event.bookingId,
                seat = envelope.event.seat,
            )
        )

    suspend fun onFailedEvent(envelope: EventEnvelope<BookingCancelSagaFailedEvent>) =
        commandBus.dispatch<BookingCommand, Booking>(
            FailCancelBookingCommand(
                bookingId = envelope.event.bookingId,
                correlationId = envelope.metadata.correlationId,
                message = envelope.event.message,
            )
        )
}
