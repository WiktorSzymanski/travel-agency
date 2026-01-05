package pl.szymanski.wiktor.ta.eventHandlerLogic

import pl.szymanski.wiktor.ta.CommandBus
import pl.szymanski.wiktor.ta.EventEnvelope
import pl.szymanski.wiktor.ta.command.BookTravelOfferCommand
import pl.szymanski.wiktor.ta.command.BookingCommand
import pl.szymanski.wiktor.ta.command.CompleteBookingCommand
import pl.szymanski.wiktor.ta.command.FailBookingCommand
import pl.szymanski.wiktor.ta.command.ProcessBookingCommand
import pl.szymanski.wiktor.ta.command.TravelOfferCommand
import pl.szymanski.wiktor.ta.domain.aggregate.Booking
import pl.szymanski.wiktor.ta.domain.aggregate.TravelOffer
import pl.szymanski.wiktor.ta.event.BookingSagaCompletedEvent
import pl.szymanski.wiktor.ta.event.BookingSagaFailedEvent
import pl.szymanski.wiktor.ta.event.BookingSagaStartedEvent

class BookingSagaEventHandleLogic(
    private val commandBus: CommandBus
) {
    suspend fun onStartedEvent(envelope: EventEnvelope<BookingSagaStartedEvent>) =
        commandBus.dispatch<BookingCommand, Booking>(
            ProcessBookingCommand(
                envelope.event.bookingId,
                envelope.metadata.correlationId,
            )
        )

    suspend fun onCompletedEvent(envelope: EventEnvelope<BookingSagaCompletedEvent>) =
        commandBus.dispatch<BookingCommand, Booking>(
            CompleteBookingCommand(
                envelope.event.bookingId,
                envelope.metadata.correlationId,
            )
        )

    suspend fun onCompletedEvent2(envelope: EventEnvelope<BookingSagaCompletedEvent>) =
        commandBus.dispatch<TravelOfferCommand, TravelOffer>(
            BookTravelOfferCommand(
                envelope.event.travelOfferId,
                envelope.metadata.correlationId,
                bookingId = envelope.event.bookingId,
                seat = envelope.event.seat,
            )
        )

    suspend fun onFailedEvent(envelope: EventEnvelope<BookingSagaFailedEvent>) =
        commandBus.dispatch<BookingCommand, Booking>(
            FailBookingCommand(
                envelope.event.bookingId,
                envelope.metadata.correlationId,
                message = envelope.event.message,
            )
        )
}
