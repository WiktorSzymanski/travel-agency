package pl.szymanski.wiktor.ta.command.commute

import pl.szymanski.wiktor.ta.EventBus
import pl.szymanski.wiktor.ta.command.travelOffer.CreateTravelOfferCommand
import pl.szymanski.wiktor.ta.domain.aggregate.Commute
import pl.szymanski.wiktor.ta.domain.aggregate.TravelOffer
import pl.szymanski.wiktor.ta.domain.event.CommuteBookedEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteBookingCanceledEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteCreatedEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteEvent
import pl.szymanski.wiktor.ta.domain.repository.CommuteRepository
import pl.szymanski.wiktor.ta.event.toCompensation

class CommuteCommandHandler(
    private val commuteRepository: CommuteRepository,
) {
    suspend fun handle(command: CommuteCommand): CommuteEvent {
        val event =
            when (command) {
                is BookCommuteCommand -> handle(command)
                is CancelCommuteBookingCommand -> handle(command)
                is CreateCommuteCommand -> handle(command)
                is ExpireCommuteCommand -> handle(command)
            }.apply { correlationId = command.correlationId }

        EventBus.publish(event)
        return event
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
            }.apply { correlationId = event.correlationId }.toCompensation(),
        )
    }

    suspend fun handle(command: CreateCommuteCommand): CommuteEvent =
        Commute.create(
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
}
