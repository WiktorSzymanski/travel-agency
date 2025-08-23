package pl.szymanski.wiktor.ta.commandHandler

import pl.szymanski.wiktor.ta.EventBus
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
import pl.szymanski.wiktor.ta.withRetry

class CommuteCommandHandler(
    private val commuteRepository: CommuteRepository,
) {
    val maxRetries = 30

    suspend fun handle(command: CommuteCommand): CommuteEvent =
        withRetry (maxRetries) {
            when (command) {
                is BookCommuteCommand -> handle(command)
                is CancelCommuteBookingCommand -> handle(command)
                is CreateCommuteCommand -> handle(command)
                is ExpireCommuteCommand -> handle(command)
            }.map {
                it.first.correlationId = command.correlationId
                EventBus.publish(it.first, it.second)
                it
            }.let { it[0].first }
        }

    private suspend fun handle(command: CreateCommuteCommand): List<Pair<CommuteEvent, Int>> =
        Commute.create(
            command.name,
            command.departure,
            command.arrival,
            command.seats,
        ).let { (commute, event) ->
            listOf(event[0] to commute.lastRevision)
        }

    private suspend fun handle(command: BookCommuteCommand): List<Pair<CommuteEvent, Int>> =
        commuteRepository
            .findById(command.commuteId)
            .let { commute ->
                commute
                    .bookSeat(command.bookingId, command.seat)
                    .mapIndexed { index, event -> event to commute.lastRevision + index }
            }

    private suspend fun handle(command: CancelCommuteBookingCommand): List<Pair<CommuteEvent, Int>> =
        commuteRepository
            .findById(command.commuteId)
            .let { commute ->
                commute
                    .cancelBookedSeat(command.bookingId)
                    .mapIndexed { index, event -> event to commute.lastRevision + index }
            }

    private suspend fun handle(command: ExpireCommuteCommand): List<Pair<CommuteEvent, Int>> =
        commuteRepository
            .findById(command.commuteId)
            .let { commute ->
                commute
                    .expire()
                    .mapIndexed { index, event -> event to commute.lastRevision + index }
            }

    suspend fun compensate(event: CommuteEvent): CommuteEvent =
        withRetry(maxRetries) {
            when (event) {
                is CommuteBookedEvent -> compensate(event)
                is CommuteBookingCanceledEvent -> compensate(event)
                else -> throw IllegalArgumentException("Unknown event type: ${event::class.simpleName}")
            }.let {
                it[0].first.toCompensation()
                it
            }.map {
                it.first.correlationId = event.correlationId
                EventBus.publish(it.first, it.second)
                it
            }.let { it[0].first }
        }

    private suspend fun compensate(event: CommuteBookedEvent): List<Pair<CommuteEvent, Int>> =
        commuteRepository
            .findById(event.commuteId)
            .let { commute ->
                commute
                    .compensateBookSeat(event.bookingId)
                    .mapIndexed { index, event -> event to commute.lastRevision + index }
            }

    private suspend fun compensate(event: CommuteBookingCanceledEvent): List<Pair<CommuteEvent, Int>> =
        commuteRepository
            .findById(event.commuteId)
            .let { commute ->
                commute
                    .compensateCancelBookedSeat(event.bookingId, event.seat)
                    .mapIndexed { index, event -> event to commute.lastRevision + index }
            }
}
