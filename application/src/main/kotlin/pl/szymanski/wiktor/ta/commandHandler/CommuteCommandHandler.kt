package pl.szymanski.wiktor.ta.commandHandler

import pl.szymanski.wiktor.ta.command.BookCommuteCommand
import pl.szymanski.wiktor.ta.command.CancelCommuteBookingCommand
import pl.szymanski.wiktor.ta.command.CommuteCommand
import pl.szymanski.wiktor.ta.command.CreateCommuteCommand
import pl.szymanski.wiktor.ta.command.ExpireCommuteCommand
import pl.szymanski.wiktor.ta.domain.aggregate.Commute
import pl.szymanski.wiktor.ta.domain.event.CommuteBookedEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteBookingCanceledEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteEvent
import pl.szymanski.wiktor.ta.domain.repository.CommuteRepository
import pl.szymanski.wiktor.ta.event.toCompensation

class CommuteCommandHandler(
    private val commuteRepository: CommuteRepository,
) {
    suspend fun handle(command: CommuteCommand): Pair<Commute, List<CommuteEvent>> =
        when (command) {
            is CreateCommuteCommand -> handle(command)
            is BookCommuteCommand -> handle(command)
            is CancelCommuteBookingCommand -> handle(command)
            is ExpireCommuteCommand -> handle(command)
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

    suspend fun compensate(event: CommuteEvent): Pair<Commute, List<CommuteEvent>> =
        when (event) {
            is CommuteBookedEvent -> compensate(event)
            is CommuteBookingCanceledEvent -> compensate(event)
            else -> throw IllegalArgumentException("Non compensatable event type: ${event::class.simpleName}")
        }.let {
            val compensateEvents = it.second.map { e -> e.toCompensation() }
            it.first to compensateEvents
        }

    private suspend fun compensate(event: CommuteBookedEvent): Pair<Commute, List<CommuteEvent>> =
        commuteRepository
            .findById(event.commuteId)
            .let {
                val events = it.compensateBookSeat(event.bookingId)
                it to events
            }

    private suspend fun compensate(event: CommuteBookingCanceledEvent): Pair<Commute, List<CommuteEvent>> =
        commuteRepository
            .findById(event.commuteId)
            .let {
                val events = it.compensateCancelBookedSeat(event.bookingId, event.seat)
                it to events
            }
}
