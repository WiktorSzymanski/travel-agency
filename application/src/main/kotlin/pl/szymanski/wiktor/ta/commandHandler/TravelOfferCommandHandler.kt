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
    suspend fun handle(command: TravelOfferCommand): Pair<TravelOffer, List<TravelOfferEvent>> =
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

    private fun handle(command: CreateTravelOfferCommand): Pair<TravelOffer, List<TravelOfferEvent>> =
        TravelOffer.create(
            command.name,
            command.commuteId,
            command.accommodationId,
            command.attractionId,
        )

    private suspend fun handle(command: BookTravelOfferCommand): Pair<TravelOffer, List<TravelOfferEvent>> =
        travelOfferRepository
            .findById(command.travelOfferId)
            .let {
                val events = it.book(command.bookingId, command.seat)
                it to events
            }

    private suspend fun handle(command: CancelBookTravelOfferCommand): Pair<TravelOffer, List<TravelOfferEvent>> =
        travelOfferRepository
            .findById(command.travelOfferId)
            .let {
                val events = it.cancelBooking(command.bookingId, command.seat)
                it to events
            }

    private suspend fun handle(command: ReleaseTravelOfferCommand): Pair<TravelOffer, List<TravelOfferEvent>> =
        travelOfferRepository
            .findById(command.travelOfferId)
            .let {
                val events = it.releaseBooking(command.bookingId, command.seat)
                it to events
            }

    private suspend fun handle(command: RebookTravelOfferCommand): Pair<TravelOffer, List<TravelOfferEvent>> =
        travelOfferRepository
            .findById(command.travelOfferId)
            .let {
                val events = it.rebook(command.bookingId)
                it to events
            }

    private suspend fun handle(command: ExpireTravelOfferCommand): Pair<TravelOffer, List<TravelOfferEvent>> =
        travelOfferRepository
            .findById(command.travelOfferId)
            .let {
                val events = it.expire()
                it to events
            }

    private suspend fun handle(command: MakeTravelOfferUnavailableCommand): Pair<TravelOffer, List<TravelOfferEvent>> =
        travelOfferRepository
            .findById(command.travelOfferId)
            .let {
                val events = it.makeUnavailable()
                it to events
            }

    private suspend fun handle(command: MakeTravelOfferAvailableCommand): Pair<TravelOffer, List<TravelOfferEvent>> =
        travelOfferRepository
            .findById(command.travelOfferId)
            .let {
                val events = it.makeAvailable()
                it to events
            }

    private suspend fun handle(command: ReserveTravelOfferCommand): Pair<TravelOffer, List<TravelOfferEvent>> =
        travelOfferRepository
            .findById(command.travelOfferId)
            .let {
                val events = it.reserve(command.bookingId, command.seat)
                it to events
            }

    private suspend fun handle(command: CancelReserveTravelOfferCommand): Pair<TravelOffer, List<TravelOfferEvent>> =
        travelOfferRepository
            .findById(command.travelOfferId)
            .let {
                val events = it.cancelReservation(command.bookingId, command.seat)
                it to events
            }

    suspend fun compensate(event: TravelOfferEvent): Pair<TravelOffer, List<TravelOfferEvent>> =
        when (event) {
            is TravelOfferBookedEvent -> compensate(event)
            is TravelOfferBookingCanceledEvent -> compensate(event)
            is TravelOfferReservedEvent -> compensate(event)
            is TravelOfferReservationCanceledEvent -> compensate(event)
            is TravelOfferReleaseEvent -> compensate(event)
            else -> throw IllegalArgumentException("Non compensatable event type: ${event::class.simpleName}")
        }.let {
            val compensationEvents = it.second.map { ev -> ev.toCompensation() }
            it.first to compensationEvents
        }

    // TODO: compensate should propably use it's own aggregate methods to be able to revert something even if date is past, on the other hand travelOffer does not know anything about dates
    private suspend fun compensate(event: TravelOfferReleaseEvent): Pair<TravelOffer, List<TravelOfferEvent>> =
        handle(
            RebookTravelOfferCommand(
                event.travelOfferId,
                event.correlationId!!,
                event.bookingId,
            )
        )

    private suspend fun compensate(event: TravelOfferBookedEvent): Pair<TravelOffer, List<TravelOfferEvent>> =
        handle(
            CancelBookTravelOfferCommand(
                event.travelOfferId,
                event.correlationId!!,
                event.bookingId,
                event.seat,
            ),
        )

    private suspend fun compensate(event: TravelOfferBookingCanceledEvent): Pair<TravelOffer, List<TravelOfferEvent>> =
        handle(
            BookTravelOfferCommand(
                event.travelOfferId,
                event.correlationId!!,
                event.bookingId,
                event.seat,
            ),
        )

    private suspend fun compensate(event: TravelOfferReservedEvent): Pair<TravelOffer, List<TravelOfferEvent>> =
        handle(
            CancelReserveTravelOfferCommand(
                event.travelOfferId,
                event.correlationId!!,
                event.bookingId,
                event.seat,
            ),
        )

    private suspend fun compensate(event: TravelOfferReservationCanceledEvent): Pair<TravelOffer, List<TravelOfferEvent>> =
        handle(
            ReserveTravelOfferCommand(
                event.travelOfferId,
                event.correlationId!!,
                event.bookingId,
                event.seat,
            ),
        )
}
