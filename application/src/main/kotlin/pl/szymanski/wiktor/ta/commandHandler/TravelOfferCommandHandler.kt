package pl.szymanski.wiktor.ta.commandHandler

import pl.szymanski.wiktor.ta.command.BookTravelOfferCommand
import pl.szymanski.wiktor.ta.command.CancelBookTravelOfferCommand
import pl.szymanski.wiktor.ta.command.CancelReserveTravelOfferCommand
import pl.szymanski.wiktor.ta.command.CompensateBookTravelOfferCommand
import pl.szymanski.wiktor.ta.command.CompensateCancelBookTravelOfferCommand
import pl.szymanski.wiktor.ta.command.CompensateCancelReserveTravelOfferCommand
import pl.szymanski.wiktor.ta.command.CompensateReleaseTravelOfferCommand
import pl.szymanski.wiktor.ta.command.CompensateReserveTravelOfferCommand
import pl.szymanski.wiktor.ta.command.CreateTravelOfferCommand
import pl.szymanski.wiktor.ta.command.ExpireTravelOfferCommand
import pl.szymanski.wiktor.ta.command.MakeTravelOfferAvailableCommand
import pl.szymanski.wiktor.ta.command.MakeTravelOfferUnavailableCommand
import pl.szymanski.wiktor.ta.command.RebookTravelOfferCommand
import pl.szymanski.wiktor.ta.command.ReleaseTravelOfferCommand
import pl.szymanski.wiktor.ta.command.ReserveTravelOfferCommand
import pl.szymanski.wiktor.ta.command.TravelOfferCommand
import pl.szymanski.wiktor.ta.domain.aggregate.TravelOffer
import pl.szymanski.wiktor.ta.domain.event.TravelOfferEvent
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
            is CompensateBookTravelOfferCommand -> compensate(command)
            is CompensateCancelBookTravelOfferCommand -> compensate(command)
            is CompensateReserveTravelOfferCommand -> compensate(command)
            is CompensateCancelReserveTravelOfferCommand -> compensate(command)
            is CompensateReleaseTravelOfferCommand -> compensate(command)
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

    // Note: compensation uses domain methods; consider dedicated compensation paths if business rules diverge.
    private suspend fun compensate(command: CompensateReleaseTravelOfferCommand): Pair<TravelOffer, List<TravelOfferEvent>> =
        travelOfferRepository
            .findById(command.travelOfferId)
            .let {
                val events = it.rebook(command.bookingId)
                it to events.map { ev -> ev.toCompensation() }
            }

    private suspend fun compensate(command: CompensateBookTravelOfferCommand): Pair<TravelOffer, List<TravelOfferEvent>> =
        travelOfferRepository
            .findById(command.travelOfferId)
            .let {
                val releaseEvents = it.releaseBooking(command.bookingId, command.seat)
                val cancelEvents = it.cancelBooking(command.bookingId, command.seat)
                it to (releaseEvents + cancelEvents).map { ev -> ev.toCompensation() }
            }

    private suspend fun compensate(command: CompensateCancelBookTravelOfferCommand): Pair<TravelOffer, List<TravelOfferEvent>> =
        travelOfferRepository
            .findById(command.travelOfferId)
            .let {
                val reserveEvents = it.reserve(command.bookingId, command.seat)
                val bookEvents = it.book(command.bookingId, command.seat)
                it to (reserveEvents + bookEvents).map { ev -> ev.toCompensation() }
            }

    private suspend fun compensate(command: CompensateReserveTravelOfferCommand): Pair<TravelOffer, List<TravelOfferEvent>> =
        travelOfferRepository
            .findById(command.travelOfferId)
            .let {
                val events = it.cancelReservation(command.bookingId, command.seat)
                it to events.map { ev -> ev.toCompensation() }
            }

    private suspend fun compensate(command: CompensateCancelReserveTravelOfferCommand): Pair<TravelOffer, List<TravelOfferEvent>> =
        travelOfferRepository
            .findById(command.travelOfferId)
            .let {
                val events = it.reserve(command.bookingId, command.seat)
                it to events.map { ev -> ev.toCompensation() }
            }
}
