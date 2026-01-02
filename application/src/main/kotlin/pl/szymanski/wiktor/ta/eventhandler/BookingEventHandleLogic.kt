package pl.szymanski.wiktor.ta.eventhandler

import org.slf4j.LoggerFactory
import pl.szymanski.wiktor.ta.CommandBus
import pl.szymanski.wiktor.ta.EventEnvelope
import pl.szymanski.wiktor.ta.command.ReleaseTravelOfferCommand
import pl.szymanski.wiktor.ta.command.ReserveTravelOfferCommand
import pl.szymanski.wiktor.ta.command.TravelOfferCommand
import pl.szymanski.wiktor.ta.domain.aggregate.TravelOffer
import pl.szymanski.wiktor.ta.domain.event.BookingCancelRequestedEvent
import pl.szymanski.wiktor.ta.domain.event.BookingCreatedEvent

class BookingEventHandleLogic(
    private val commandBus: CommandBus
) {
    private val log = LoggerFactory.getLogger(this::class.java)

    suspend fun onCreatedEvent(envelope: EventEnvelope<BookingCreatedEvent>) =
        commandBus.dispatch<TravelOfferCommand, TravelOffer>(
            ReserveTravelOfferCommand(
                envelope.event.travelOfferId,
                envelope.metadata.correlationId,
                envelope.event.bookingId,
                envelope.event.seat,
            )
        )

    suspend fun onCancelRequestedEvent(envelope: EventEnvelope<BookingCancelRequestedEvent>) =
        runCatching {
            commandBus.dispatch<TravelOfferCommand, TravelOffer>(
                ReleaseTravelOfferCommand(
                    envelope.event.travelOfferId,
                    envelope.metadata.correlationId,
                    envelope.event.bookingId,
                    envelope.event.seat,
                )
            )
        }.onFailure { e ->
            log.error("Failed to release travel offer event: $envelope", e)
        }
}
