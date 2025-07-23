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

class CommuteCommandHandler(
    private val commuteRepository: CommuteRepository,
) {
    suspend fun handle(command: CommuteCommand): CommuteEvent =
        when (command) {
            is BookCommuteCommand -> handle(command)
            is CancelCommuteBookingCommand -> handle(command)
            is CreateCommuteCommand -> handle(command)
            is ExpireCommuteCommand -> handle(command)
        }.apply { correlationId = command.correlationId }.also { EventBus.publish(it) }

    suspend fun handle(command: CreateCommuteCommand): CommuteEvent =
        Commute.Companion.create(
            command.name,
            command.departure,
            command.arrival,
            command.seats,
        ).let { (commute, event) ->
            commuteRepository.save(commute)
            event
        }

    suspend fun handle(command: BookCommuteCommand): CommuteEvent =
        commuteRepository
            .findById(command.commuteId)
            .let { commute ->
                commute
                    .bookSeat(command.seat, command.userId)
                    .also { commuteRepository.update(commute) }
            }.apply { correlationId = command.correlationId }

    suspend fun handle(command: CancelCommuteBookingCommand): CommuteEvent =
        commuteRepository
            .findById(command.commuteId)
            .let { commute ->
                commute
                    .cancelBookedSeat(command.seat, command.userId)
                    .also { commuteRepository.update(commute) }
            }.apply { correlationId = command.correlationId }

    suspend fun handle(command: ExpireCommuteCommand): CommuteEvent =
        commuteRepository
            .findById(command.commuteId)
            .let { commute ->
                commute
                    .expire()
                    .also { commuteRepository.update(commute) }
            }.apply { correlationId = command.correlationId }

    suspend fun compensate(event: CommuteEvent): CommuteEvent =
        when (event) {
            is CommuteBookedEvent -> compensate(event)
            is CommuteBookingCanceledEvent -> compensate(event)
            else -> throw IllegalArgumentException("Unknown event type: ${event::class.simpleName}")
        }.apply { correlationId = event.correlationId }.toCompensation().also { EventBus.publish(it) }

    suspend fun compensate(event: CommuteBookedEvent): CommuteEvent =
        handle(
            BookCommuteCommand(
                event.commuteId,
                event.correlationId!!,
                event.userId,
                event.seat,
            ),
        )

    suspend fun compensate(event: CommuteBookingCanceledEvent): CommuteEvent =
        handle(
            CancelCommuteBookingCommand(
                event.commuteId,
                event.correlationId!!,
                event.userId,
                event.seat,
            ),
        )
}
