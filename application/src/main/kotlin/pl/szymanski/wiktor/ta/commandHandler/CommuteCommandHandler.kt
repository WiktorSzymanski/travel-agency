package pl.szymanski.wiktor.ta.commandHandler

import pl.szymanski.wiktor.ta.EventBus
import pl.szymanski.wiktor.ta.command.BookCommuteCommand
import pl.szymanski.wiktor.ta.command.CancelCommuteBookingCommand
import pl.szymanski.wiktor.ta.command.CommuteCommand
import pl.szymanski.wiktor.ta.command.CreateCommuteCommand
import pl.szymanski.wiktor.ta.command.ExpireCommuteCommand
import pl.szymanski.wiktor.ta.domain.aggregate.Commute
import pl.szymanski.wiktor.ta.domain.event.AttractionEvent
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
                it
            }.let {
                EventBus.publish(it[0].first, it[0].second)
                if (it.size > 1) EventBus.ignoreRevisionPublish(it[1].first)
                it[0].first
            }
        }

    private suspend fun handle(command: CreateCommuteCommand): List<Pair<CommuteEvent, String?>> =
        Commute.create(
            command.name,
            command.departure,
            command.arrival,
            command.seats,
        ).let { (commute, event) ->
            listOf(event[0] to commute.lastEtag)
        }

    private suspend fun handle(command: BookCommuteCommand): List<Pair<CommuteEvent, String?>> =
        commuteRepository
            .findById(command.commuteId)
            .let { commute ->
                commute
                    .bookSeat(command.bookingId, command.seat)
                    .let { lst ->
                        listOfNotNull(
                            lst[0] to commute.lastEtag,
                            takeIf { lst.size > 1 }?.let { lst[1] to null}
                        )
                    } as List<Pair<CommuteEvent, String?>>
            }

    private suspend fun handle(command: CancelCommuteBookingCommand): List<Pair<CommuteEvent, String?>> =
        commuteRepository
            .findById(command.commuteId)
            .let { commute ->
                commute
                    .cancelBookedSeat(command.bookingId)
                    .let { lst ->
                        listOfNotNull(
                            lst[0] to commute.lastEtag,
                            takeIf { lst.size > 1 }?.let { lst[1] to null}
                        )
                    } as List<Pair<CommuteEvent, String?>>
            }

    private suspend fun handle(command: ExpireCommuteCommand): List<Pair<CommuteEvent, String?>> =
        commuteRepository
            .findById(command.commuteId)
            .let { commute ->
                commute
                    .expire()
                    .let { lst ->
                        listOfNotNull(
                            lst[0] to commute.lastEtag,
                            takeIf { lst.size > 1 }?.let { lst[1] to null}
                        )
                    } as List<Pair<CommuteEvent, String?>>
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
                it
            }.let {
                EventBus.publish(it[0].first, it[0].second!!)
                if (it.size > 1) EventBus.ignoreRevisionPublish(it[1].first)
                it[0].first
            }
        }

    private suspend fun compensate(event: CommuteBookedEvent): List<Pair<CommuteEvent, String?>> =
        commuteRepository
            .findById(event.commuteId)
            .let { commute ->
                commute
                    .compensateBookSeat(event.bookingId)
                    .let { lst ->
                        listOfNotNull(
                            lst[0] to commute.lastEtag,
                            takeIf { lst.size > 1 }?.let { lst[1] to null}
                        )
                    } as List<Pair<CommuteEvent, String?>>
            }

    private suspend fun compensate(event: CommuteBookingCanceledEvent): List<Pair<CommuteEvent, String?>> =
        commuteRepository
            .findById(event.commuteId)
            .let { commute ->
                commute
                    .compensateCancelBookedSeat(event.bookingId, event.seat)
                    .let { lst ->
                        listOfNotNull(
                            lst[0] to commute.lastEtag,
                            takeIf { lst.size > 1 }?.let { lst[1] to null}
                        )
                    } as List<Pair<CommuteEvent, String?>>
            }
}
