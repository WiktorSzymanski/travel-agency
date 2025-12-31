package pl.szymanski.wiktor.ta.commandhandler

import pl.szymanski.wiktor.ta.command.BookCommuteCommand
import pl.szymanski.wiktor.ta.command.CancelCommuteBookingCommand
import pl.szymanski.wiktor.ta.command.CommuteCommand
import pl.szymanski.wiktor.ta.command.CompensateBookCommuteCommand
import pl.szymanski.wiktor.ta.command.CompensateCancelCommuteBookingCommand
import pl.szymanski.wiktor.ta.command.CreateCommuteCommand
import pl.szymanski.wiktor.ta.command.ExpireCommuteCommand
import pl.szymanski.wiktor.ta.domain.aggregate.BookingId
import pl.szymanski.wiktor.ta.domain.aggregate.Commute
import pl.szymanski.wiktor.ta.domain.event.CommuteEvent
import pl.szymanski.wiktor.ta.domain.repository.CommuteRepository

class CommuteCommandHandler(
    private val commuteRepository: CommuteRepository,
) {
    suspend fun handle(command: CommuteCommand): Pair<Commute, List<CommuteEvent>> =
        when (command) {
            is CreateCommuteCommand -> handle(command)
            is BookCommuteCommand -> handle(command)
            is CancelCommuteBookingCommand -> handle(command)
            is ExpireCommuteCommand -> handle(command)
            is CompensateBookCommuteCommand -> compensate(command)
            is CompensateCancelCommuteBookingCommand -> compensate(command)
        }

    private fun handle(command: CreateCommuteCommand): Pair<Commute, List<CommuteEvent>> =
        Commute.create(
            command.name,
            command.departure,
            command.arrival,
            command.seats,
        )

    private suspend fun handle(command: BookCommuteCommand): Pair<Commute, List<CommuteEvent>> =
        commuteRepository
            .findById(command.commuteId)
            .let {
                val events = it.bookSeat(command.bookingId, command.seat)
                it to events
            }

    private suspend fun handle(command: CancelCommuteBookingCommand): Pair<Commute, List<CommuteEvent>> =
        commuteRepository
            .findById(command.commuteId)
            .let {
                val events = it.cancelBookedSeat(command.bookingId)
                it to events
            }

    private suspend fun handle(command: ExpireCommuteCommand): Pair<Commute, List<CommuteEvent>> =
        commuteRepository
            .findById(command.commuteId)
            .let {
                val events = it.expire()
                it to events
            }

    private suspend fun compensate(command: CompensateBookCommuteCommand): Pair<Commute, List<CommuteEvent>> =
        commuteRepository
            .findById(command.commuteId)
            .let {
                val events = it.compensateBookSeat(command.bookingId)
                it to events
            }

    private suspend fun compensate(command: CompensateCancelCommuteBookingCommand): Pair<Commute, List<CommuteEvent>> =
        commuteRepository
            .findById(command.commuteId)
            .let {
                val events = it.compensateCancelBookedSeat(command.bookingId, command.seat)
                it to events
            }
}
