package pl.szymanski.wiktor.ta.commandHandler

import pl.szymanski.wiktor.ta.EventBus
import pl.szymanski.wiktor.ta.command.BookTravelOfferCommand
import pl.szymanski.wiktor.ta.command.CancelBookTravelOfferCommand
import pl.szymanski.wiktor.ta.command.CreateTravelOfferCommand
import pl.szymanski.wiktor.ta.command.ExpireTravelOfferCommand
import pl.szymanski.wiktor.ta.command.TravelOfferCommand
import pl.szymanski.wiktor.ta.domain.aggregate.TravelOffer
import pl.szymanski.wiktor.ta.domain.event.Event
import pl.szymanski.wiktor.ta.domain.event.TravelOfferBookedEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferBookingCanceledEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferExpiredEvent
import pl.szymanski.wiktor.ta.domain.repository.TravelOfferRepository
import pl.szymanski.wiktor.ta.event.toCompensation
import java.util.UUID

class TravelOfferCommandHandler(
    private val travelOfferRepository: TravelOfferRepository,
) {
    suspend fun handle(command: TravelOfferCommand): TravelOfferEvent =
        when (command) {
            is BookTravelOfferCommand -> handle(command)
            is CancelBookTravelOfferCommand -> handle(command)
            is CreateTravelOfferCommand -> handle(command)
            is ExpireTravelOfferCommand -> handle(command)
        }.apply { correlationId = command.correlationId }.also { EventBus.publish(it) }

    private suspend fun handle(command: CreateTravelOfferCommand): TravelOfferEvent =
        TravelOffer.create(
            command.name,
            command.commuteId,
            command.accommodationId,
            command.attractionId,
        ).let { (travelOffer, event) ->
            travelOfferRepository.save(travelOffer)
            event
        }

    private suspend fun handle(command: BookTravelOfferCommand): TravelOfferEvent =
        travelOfferRepository
            .findById(command.travelOfferId)
            .let { travelOffer ->
                travelOffer
                    .book(command.userId, command.seat)
                    .also { travelOfferRepository.update(travelOffer) }
            }.apply { correlationId = command.correlationId }

    private suspend fun handle(command: CancelBookTravelOfferCommand): TravelOfferEvent =
        travelOfferRepository
            .findById(command.travelOfferId)
            .let { travelOffer ->
                travelOffer
                    .cancelBooking(command.userId, command.seat)
                    .also { travelOfferRepository.update(travelOffer) }
            }.apply { correlationId = command.correlationId }

    private suspend fun handle(command: ExpireTravelOfferCommand): TravelOfferEvent =
        travelOfferRepository
            .findById(command.travelOfferId)
            .let { travelOffer ->
                travelOffer
                    .expire()
                    .also { travelOfferRepository.update(travelOffer) }
            }.apply { correlationId = command.correlationId }

    suspend fun compensate(event: TravelOfferEvent): TravelOfferEvent =
        when (event) {
            is TravelOfferBookedEvent -> compensate(event)
            is TravelOfferBookingCanceledEvent -> compensate(event)
            else -> throw IllegalArgumentException("Unknown event type: ${event::class.simpleName}")
        }.apply { correlationId = event.correlationId }.toCompensation().also { EventBus.publish(it) }

    private suspend fun compensate(event: TravelOfferBookedEvent): TravelOfferEvent =
        handle(
            CancelBookTravelOfferCommand(
                event.travelOfferId,
                event.correlationId!!,
                event.userId,
                event.seat
            ),
        )

    private suspend fun compensate(event: TravelOfferBookingCanceledEvent): TravelOfferEvent =
        handle(
            BookTravelOfferCommand(
                event.travelOfferId,
                event.correlationId!!,
                event.userId,
                event.seat),
        )
}
