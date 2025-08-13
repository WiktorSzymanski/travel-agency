package pl.szymanski.wiktor.ta.commandHandler

import pl.szymanski.wiktor.ta.EventBus
import pl.szymanski.wiktor.ta.command.BookTravelOfferCommand
import pl.szymanski.wiktor.ta.command.CancelBookTravelOfferCommand
import pl.szymanski.wiktor.ta.command.CancelReserveTravelOfferCommand
import pl.szymanski.wiktor.ta.command.CreateTravelOfferCommand
import pl.szymanski.wiktor.ta.command.ExpireTravelOfferCommand
import pl.szymanski.wiktor.ta.command.MakeTravelOfferAvailableCommand
import pl.szymanski.wiktor.ta.command.MakeTravelOfferUnavailableCommand
import pl.szymanski.wiktor.ta.command.RebookTravelOfferCommand
import pl.szymanski.wiktor.ta.command.ReleaseTravelOfferCommand
import pl.szymanski.wiktor.ta.command.ReserveTravelOfferCommand
import pl.szymanski.wiktor.ta.command.TravelOfferCommand
import pl.szymanski.wiktor.ta.domain.aggregate.TravelOffer
import pl.szymanski.wiktor.ta.domain.event.TravelOfferBookedEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferBookingCanceledEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferFailedEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferReleaseEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferReservationCanceledEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferReservedEvent
import pl.szymanski.wiktor.ta.domain.repository.TravelOfferRepository
import pl.szymanski.wiktor.ta.event.toCompensation
import pl.szymanski.wiktor.ta.withRetry

class TravelOfferCommandHandler(
    private val travelOfferRepository: TravelOfferRepository,
) {
    val maxRetries = 10

    suspend fun handle(command: TravelOfferCommand): TravelOfferEvent =
        withRetry(maxRetries) {
            when (command) {
                is BookTravelOfferCommand -> handle(command)
                is CancelBookTravelOfferCommand -> handle(command)
                is ReserveTravelOfferCommand -> handle(command)
                is ReleaseTravelOfferCommand -> handle(command)
                is RebookTravelOfferCommand -> handle(command)
                is CancelReserveTravelOfferCommand -> handle(command)
                is CreateTravelOfferCommand -> handle(command)
                is ExpireTravelOfferCommand -> handle(command)
                is MakeTravelOfferAvailableCommand -> handle(command)
                is MakeTravelOfferUnavailableCommand -> handle(command)
            }
        }.apply { correlationId = command.correlationId }.also { EventBus.publish(it) }

    private suspend fun handle(command: CreateTravelOfferCommand): TravelOfferEvent =
        TravelOffer.create(
            command.name,
            command.commuteId,
            command.accommodationId,
            command.attractionId,
        ).let { (travelOffer, event) ->
            event
        }

    private suspend fun handle(command: BookTravelOfferCommand): TravelOfferEvent =
        travelOfferRepository
            .findById(command.travelOfferId)
            .let { travelOffer ->
                travelOffer
                    .book(command.bookingId, command.seat)
            }

    private suspend fun handle(command: CancelBookTravelOfferCommand): TravelOfferEvent =
        travelOfferRepository
            .findById(command.travelOfferId)
            .let { travelOffer ->
                travelOffer
                    .cancelBooking(command.bookingId, command.seat)
            }

    private suspend fun handle(command: ReleaseTravelOfferCommand): TravelOfferEvent =
        travelOfferRepository
            .findById(command.travelOfferId)
            .let { travelOffer ->
                travelOffer
                    .releaseBooking(command.bookingId, command.seat)
            }

    private suspend fun handle(command: RebookTravelOfferCommand): TravelOfferEvent =
        travelOfferRepository
            .findById(command.travelOfferId)
            .let { travelOffer ->
                travelOffer
                    .rebook(command.bookingId)
            }

    private suspend fun handle(command: ExpireTravelOfferCommand): TravelOfferEvent =
        travelOfferRepository
            .findById(command.travelOfferId)
            .let { travelOffer ->
                travelOffer
                    .expire()
            }

    private suspend fun handle(command: MakeTravelOfferUnavailableCommand): TravelOfferEvent =
        travelOfferRepository
            .findById(command.travelOfferId)
            .let { travelOffer ->
                travelOffer
                    .makeUnavailable()
            }

    private suspend fun handle(command: MakeTravelOfferAvailableCommand): TravelOfferEvent =
        travelOfferRepository
            .findById(command.travelOfferId)
            .let { travelOffer ->
                travelOffer
                    .makeAvailable()
            }

    private suspend fun handle(command: ReserveTravelOfferCommand): TravelOfferEvent =
        travelOfferRepository
            .findById(command.travelOfferId)
            .let { travelOffer ->
                travelOffer
                    .reserve(command.bookingId, command.seat)
            }

    private suspend fun handle(command: CancelReserveTravelOfferCommand): TravelOfferEvent =
        travelOfferRepository
            .findById(command.travelOfferId)
            .let { travelOffer ->
                travelOffer
                    .cancelReservation(command.bookingId, command.seat)
            }

    suspend fun compensate(event: TravelOfferEvent): TravelOfferEvent =
        when (event) {
            is TravelOfferBookedEvent -> compensate(event)
            is TravelOfferBookingCanceledEvent -> compensate(event)
            is TravelOfferReservedEvent -> compensate(event)
            is TravelOfferReservationCanceledEvent -> compensate(event)
            is TravelOfferReleaseEvent -> compensate(event)
            else -> throw IllegalArgumentException("Unknown event type: ${event::class.simpleName}")
        }.apply { correlationId = event.correlationId }.toCompensation().also { EventBus.publish(it) }

    suspend fun compensate(event: TravelOfferReleaseEvent): TravelOfferEvent =
        handle(
            RebookTravelOfferCommand(
                event.travelOfferId,
                event.correlationId!!,
                event.bookingId,
            )
        )

    suspend fun compensate(event: TravelOfferBookedEvent): TravelOfferEvent =
        handle(
            CancelBookTravelOfferCommand(
                event.travelOfferId,
                event.correlationId!!,
                event.bookingId,
                event.seat,
            ),
        )

    suspend fun compensate(event: TravelOfferBookingCanceledEvent): TravelOfferEvent =
        handle(
            BookTravelOfferCommand(
                event.travelOfferId,
                event.correlationId!!,
                event.bookingId,
                event.seat,
            ),
        )

    suspend fun compensate(event: TravelOfferReservedEvent): TravelOfferEvent =
        handle(
            CancelReserveTravelOfferCommand(
                event.travelOfferId,
                event.correlationId!!,
                event.bookingId,
                event.seat,
            ),
        )

    suspend fun compensate(event: TravelOfferReservationCanceledEvent): TravelOfferEvent =
        handle(
            ReserveTravelOfferCommand(
                event.travelOfferId,
                event.correlationId!!,
                event.bookingId,
                event.seat,
            ),
        )
}
