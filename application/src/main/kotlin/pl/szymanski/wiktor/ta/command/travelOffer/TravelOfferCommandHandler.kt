package pl.szymanski.wiktor.ta.command.travelOffer

import pl.szymanski.wiktor.ta.EventBus
import pl.szymanski.wiktor.ta.domain.event.TravelOfferBookedEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferBookingCanceledEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferEvent
import pl.szymanski.wiktor.ta.domain.repository.TravelOfferRepository
import pl.szymanski.wiktor.ta.event.toCompensation

class TravelOfferCommandHandler(
    private val travelOfferRepository: TravelOfferRepository,
) {
    suspend fun handle(command: TravelOfferCommand): TravelOfferEvent {
        val event =
            when (command) {
                is BookTravelOfferCommand -> handle(command)
                is CancelBookTravelOfferCommand -> handle(command)
            }.apply { correlationId = command.correlationId }

        EventBus.publish(event)
        return event
    }

    suspend fun compensate(event: TravelOfferEvent) {
        EventBus.publish(
            when (event) {
                is TravelOfferBookedEvent ->
                    handle(
                        CancelBookTravelOfferCommand(event.travelOfferId, event.correlationId!!, event.userId, event.seat),
                    )
                is TravelOfferBookingCanceledEvent ->
                    handle(
                        BookTravelOfferCommand(event.travelOfferId, event.correlationId!!, event.userId, event.seat),
                    )
                else -> throw IllegalArgumentException("Unknown event type: ${event::class.simpleName}")
            }.apply { correlationId = event.correlationId }.toCompensation(),
        )
    }

    suspend fun handle(command: BookTravelOfferCommand): TravelOfferEvent =
        travelOfferRepository
            .findById(command.travelOfferId)
            .let { travelOffer ->
                travelOffer
                    .book(command.userId, command.seat)
                    .also { travelOfferRepository.update(travelOffer) }
            }.apply { correlationId = command.correlationId }

    suspend fun handle(command: CancelBookTravelOfferCommand): TravelOfferEvent =
        travelOfferRepository
            .findById(command.travelOfferId)
            .let { travelOffer ->
                travelOffer
                    .cancelBooking(command.userId, command.seat)
                    .also { travelOfferRepository.update(travelOffer) }
            }.apply { correlationId = command.correlationId }
}
