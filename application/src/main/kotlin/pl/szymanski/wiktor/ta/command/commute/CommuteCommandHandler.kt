package pl.szymanski.wiktor.ta.command.commute

import pl.szymanski.wiktor.ta.EventBus
import pl.szymanski.wiktor.ta.domain.event.CommuteBookedEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteBookingCanceledEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteEvent
import pl.szymanski.wiktor.ta.domain.repository.CommuteRepository
import pl.szymanski.wiktor.ta.event.toCompensationEvent

class CommuteCommandHandler(
    private val commuteRepository: CommuteRepository,
) {
    suspend fun handle(command: CommuteCommand) {
        EventBus.publish(
            when (command) {
                is BookCommuteCommand -> handle(command)
                is CancelCommuteBookingCommand -> handle(command)
            }.apply { correlationId = command.correlationId },
        )
    }

    suspend fun compensate(event: CommuteEvent) {
        EventBus.publish(
            when (event) {
                is CommuteBookedEvent ->
                    handle(
                        CancelCommuteBookingCommand(
                            event.commuteId,
                            event.correlationId!!,
                            event.userId,
                            event.seat,
                        ),
                    )
                is CommuteBookingCanceledEvent ->
                    handle(
                        BookCommuteCommand(
                            event.commuteId,
                            event.correlationId!!,
                            event.userId,
                            event.seat,
                        ),
                    )
                else -> throw IllegalArgumentException("Unknown event type: ${event::class.simpleName}")
            }.apply { correlationId = event.correlationId }.toCompensationEvent(),
        )
    }

    suspend fun handle(command: BookCommuteCommand): CommuteEvent =
        commuteRepository
            .findById(command.commuteId)
            .let { commute ->
                commute
                    .bookSeat(command.seat, command.userId)
                    .also { commuteRepository.update(commute) }
            }
            .apply { correlationId = command.correlationId }

    suspend fun handle(command: CancelCommuteBookingCommand): CommuteEvent =
        commuteRepository
            .findById(command.commuteId)
            .let { commute ->
                commute
                    .cancelBookedSeat(command.seat, command.userId)
                    .also { commuteRepository.update(commute) }
            }
            .apply { correlationId = command.correlationId }
}
