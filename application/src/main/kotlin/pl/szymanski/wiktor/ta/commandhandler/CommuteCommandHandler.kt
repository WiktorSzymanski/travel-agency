package pl.szymanski.wiktor.ta.commandhandler

import pl.szymanski.wiktor.ta.Metadata
import pl.szymanski.wiktor.ta.command.BookCommuteCommand
import pl.szymanski.wiktor.ta.command.CancelCommuteBookingCommand
import pl.szymanski.wiktor.ta.command.CommuteCommand
import pl.szymanski.wiktor.ta.command.CompensateBookCommuteCommand
import pl.szymanski.wiktor.ta.command.CompensateCancelCommuteBookingCommand
import pl.szymanski.wiktor.ta.command.CreateCommuteCommand
import pl.szymanski.wiktor.ta.command.ExpireCommuteCommand
import pl.szymanski.wiktor.ta.domain.aggregate.Commute
import pl.szymanski.wiktor.ta.domain.event.CommuteEvent
import pl.szymanski.wiktor.ta.domain.repository.CommuteRepository
import java.util.UUID

class CommuteCommandHandler(
    private val commuteRepository: CommuteRepository,
) {
    suspend fun handle(command: CommuteCommand): Triple<Commute, List<CommuteEvent>, Metadata> =
        when (command) {
            is CreateCommuteCommand -> handle(command)
            is BookCommuteCommand -> handle(command)
            is CancelCommuteBookingCommand -> handle(command)
            is ExpireCommuteCommand -> handle(command)
            is CompensateBookCommuteCommand -> compensate(command)
            is CompensateCancelCommuteBookingCommand -> compensate(command)
        }

    private fun handle(command: CreateCommuteCommand): Triple<Commute, List<CommuteEvent>, Metadata> =
        Commute.create(
            command.name,
            command.departure,
            command.arrival,
            command.seats,
        ).let { Triple(it.first, it.second, Metadata(command.correlationId, 0)) }

    private suspend fun handle(command: BookCommuteCommand): Triple<Commute, List<CommuteEvent>, Metadata> =
        commuteRepository
            .findById(command.commuteId)
            .let {
                val events = it.first.bookSeat(command.bookingId, command.seat)
                Triple(it.first, events, Metadata(command.correlationId, it.second + 1))
            }

    private suspend fun handle(command: CancelCommuteBookingCommand): Triple<Commute, List<CommuteEvent>, Metadata> =
        commuteRepository
            .findById(command.commuteId)
            .let {
                val events = it.first.cancelBookedSeat(command.bookingId)
                Triple(it.first, events, Metadata(command.correlationId, it.second + 1))
            }

    private suspend fun handle(command: ExpireCommuteCommand): Triple<Commute, List<CommuteEvent>, Metadata> =
        commuteRepository
            .findById(command.commuteId)
            .let {
                val events = it.first.expire()
                Triple(it.first, events, Metadata(command.correlationId, it.second + 1))
            }

    private suspend fun compensate(command: CompensateBookCommuteCommand): Triple<Commute, List<CommuteEvent>, Metadata> =
        commuteRepository
            .findById(command.commuteId)
            .let {
                val events = it.first.compensateBookSeat(command.bookingId)
                Triple(it.first, events, Metadata(command.correlationId, it.second + 1))
            }

    private suspend fun compensate(command: CompensateCancelCommuteBookingCommand): Triple<Commute, List<CommuteEvent>, Metadata> =
        commuteRepository
            .findById(command.commuteId)
            .let {
                val events = it.first.compensateCancelBookedSeat(command.bookingId, command.seat)
                Triple(it.first, events, Metadata(command.correlationId, it.second + 1))
            }
}
