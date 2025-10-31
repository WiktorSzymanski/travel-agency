package pl.szymanski.wiktor.ta.commandHandler

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
import pl.szymanski.wiktor.ta.domain.event.TravelOfferReleaseEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferReservationCanceledEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferReservedEvent
import pl.szymanski.wiktor.ta.domain.repository.TravelOfferRepository
import pl.szymanski.wiktor.ta.event.toCompensation

class TravelOfferCommandHandler(
    private val travelOfferRepository: TravelOfferRepository,
) {
    suspend fun handle(command: TravelOfferCommand): Pair<TravelOffer, TravelOfferEvent> =
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

    private fun handle(command: CreateTravelOfferCommand): Pair<TravelOffer, TravelOfferEvent> =
        TravelOffer.create(
            command.name,
            command.commuteId,
            command.accommodationId,
            command.attractionId,
        )

    private suspend fun handle(command: BookTravelOfferCommand): Pair<TravelOffer, TravelOfferEvent> =
        travelOfferRepository
            .findById(command.travelOfferId)
            .let {
                val event = it.book(command.bookingId, command.seat)
                it to event
            }

    private suspend fun handle(command: CancelBookTravelOfferCommand): Pair<TravelOffer, TravelOfferEvent> =
        travelOfferRepository
            .findById(command.travelOfferId)
            .let {
                val event = it.cancelBooking(command.bookingId, command.seat)
                it to event
            }

    private suspend fun handle(command: ReleaseTravelOfferCommand): Pair<TravelOffer, TravelOfferEvent> =
        travelOfferRepository
            .findById(command.travelOfferId)
            .let {
                val event = it.releaseBooking(command.bookingId, command.seat)
                it to event
            }

    private suspend fun handle(command: RebookTravelOfferCommand): Pair<TravelOffer, TravelOfferEvent> =
        travelOfferRepository
            .findById(command.travelOfferId)
            .let {
                val event = it.rebook(command.bookingId)
                it to event
            }

    private suspend fun handle(command: ExpireTravelOfferCommand): Pair<TravelOffer, TravelOfferEvent> =
        travelOfferRepository
            .findById(command.travelOfferId)
            .let {
                val event = it.expire()
                it to event
            }

    private suspend fun handle(command: MakeTravelOfferUnavailableCommand): Pair<TravelOffer, TravelOfferEvent> =
        travelOfferRepository
            .findById(command.travelOfferId)
            .let {
                val event = it.makeUnavailable()
                it to event
            }

    private suspend fun handle(command: MakeTravelOfferAvailableCommand): Pair<TravelOffer, TravelOfferEvent> =
        travelOfferRepository
            .findById(command.travelOfferId)
            .let {
                val event = it.makeAvailable()
                it to event
            }

    private suspend fun handle(command: ReserveTravelOfferCommand): Pair<TravelOffer, TravelOfferEvent> =
        travelOfferRepository
            .findById(command.travelOfferId)
            .let {
                val event = it.reserve(command.bookingId, command.seat)
                it to event
            }

    private suspend fun handle(command: CancelReserveTravelOfferCommand): Pair<TravelOffer, TravelOfferEvent> =
        travelOfferRepository
            .findById(command.travelOfferId)
            .let {
                val event = it.cancelReservation(command.bookingId, command.seat)
                it to event
            }

    suspend fun compensate(event: TravelOfferEvent): Pair<TravelOffer, TravelOfferEvent> =
        when (event) {
            is TravelOfferBookedEvent -> compensate(event)
            is TravelOfferBookingCanceledEvent -> compensate(event)
            is TravelOfferReservedEvent -> compensate(event)
            is TravelOfferReservationCanceledEvent -> compensate(event)
            is TravelOfferReleaseEvent -> compensate(event)
            else -> throw IllegalArgumentException("Non compensatable event type: ${event::class.simpleName}")
        }.let {
            val compensationEvent = it.second.toCompensation()
            it.first to compensationEvent
        }

    private suspend fun compensate(event: TravelOfferReleaseEvent): Pair<TravelOffer, TravelOfferEvent> =
        handle(
            RebookTravelOfferCommand(
                event.travelOfferId,
                event.correlationId!!,
                event.bookingId,
            )
        )

    private suspend fun compensate(event: TravelOfferBookedEvent): Pair<TravelOffer, TravelOfferEvent> =
        handle(
            CancelBookTravelOfferCommand(
                event.travelOfferId,
                event.correlationId!!,
                event.bookingId,
                event.seat,
            ),
        )

    private suspend fun compensate(event: TravelOfferBookingCanceledEvent): Pair<TravelOffer, TravelOfferEvent> =
        handle(
            BookTravelOfferCommand(
                event.travelOfferId,
                event.correlationId!!,
                event.bookingId,
                event.seat,
            ),
        )

    private suspend fun compensate(event: TravelOfferReservedEvent): Pair<TravelOffer, TravelOfferEvent> =
        handle(
            CancelReserveTravelOfferCommand(
                event.travelOfferId,
                event.correlationId!!,
                event.bookingId,
                event.seat,
            ),
        )

    private suspend fun compensate(event: TravelOfferReservationCanceledEvent): Pair<TravelOffer, TravelOfferEvent> =
        handle(
            ReserveTravelOfferCommand(
                event.travelOfferId,
                event.correlationId!!,
                event.bookingId,
                event.seat,
            ),
        )
}
