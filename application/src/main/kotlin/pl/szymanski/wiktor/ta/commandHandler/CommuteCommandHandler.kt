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
import pl.szymanski.wiktor.ta.domain.event.CommuteFailedEvent
import pl.szymanski.wiktor.ta.domain.repository.CommuteRepository
import pl.szymanski.wiktor.ta.event.toCompensation
import pl.szymanski.wiktor.ta.withRetry

class CommuteCommandHandler(
    private val commuteRepository: CommuteRepository,
) {
    val maxRetries = 10

    suspend fun handle(command: CommuteCommand): CommuteEvent =
        withRetry (maxRetries) {
            when (command) {
                is BookCommuteCommand -> handle(command)
                is CancelCommuteBookingCommand -> handle(command)
                is CreateCommuteCommand -> handle(command)
                is ExpireCommuteCommand -> handle(command)
            }
        }.map {
            it.correlationId = command.correlationId
            EventBus.publish(it)
            it
        }.let { it[0] }

    private suspend fun handle(command: CreateCommuteCommand): List<CommuteEvent> =
        Commute.create(
            command.name,
            command.departure,
            command.arrival,
            command.seats,
        ).let { (commute, event) ->
            event
        }

    private suspend fun handle(command: BookCommuteCommand): List<CommuteEvent> =
        commuteRepository
            .findById(command.commuteId)
            .let { commute ->
                commute
                    .bookSeat(command.bookingId, command.seat)
            }

    private suspend fun handle(command: CancelCommuteBookingCommand): List<CommuteEvent> =
        commuteRepository
            .findById(command.commuteId)
            .let { commute ->
                commute
                    .cancelBookedSeat(command.bookingId)
            }

    private suspend fun handle(command: ExpireCommuteCommand): List<CommuteEvent> =
        commuteRepository
            .findById(command.commuteId)
            .let { commute ->
                commute
                    .expire()
            }

    suspend fun compensate(event: CommuteEvent): CommuteEvent =
        when (event) {
            is CommuteBookedEvent -> compensate(event)
            is CommuteBookingCanceledEvent -> compensate(event)
            else -> throw IllegalArgumentException("Unknown event type: ${event::class.simpleName}")
        }.let {
            it[0].toCompensation()
            it
        }.map {
            it.correlationId = event.correlationId
            EventBus.publish(it)
            it
        }.let { it[0] }

    private suspend fun compensate(event: CommuteBookedEvent): List<CommuteEvent> =
        commuteRepository
            .findById(event.commuteId)
            .let { commute ->
                commute
                    .compensateBookSeat(event.bookingId)
            }

    private suspend fun compensate(event: CommuteBookingCanceledEvent): List<CommuteEvent> =
        commuteRepository
            .findById(event.commuteId)
            .let { commute ->
                commute
                    .compensateCancelBookedSeat(event.bookingId, event.seat)
            }
}
