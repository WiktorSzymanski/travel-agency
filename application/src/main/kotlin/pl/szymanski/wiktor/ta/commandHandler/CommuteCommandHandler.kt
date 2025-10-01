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
    val maxRetries = 1

    suspend fun handle(command: CommuteCommand): CommuteEvent =
        withRetry (maxRetries) {
            when (command) {
                is BookCommuteCommand -> handle(command)
                is CancelCommuteBookingCommand -> handle(command)
                is CreateCommuteCommand -> handle(command)
                is ExpireCommuteCommand -> handle(command)
            }.let {
                it.first.map { event -> event.correlationId = command.correlationId }
                EventBus.publish(it.first, it.second.toLong(), it.third)
                it.first.first()
            }
        }

    private suspend fun handle(command: CreateCommuteCommand): Triple<List<CommuteEvent>, Int, String?> =
        Commute.create(
            command.name,
            command.departure,
            command.arrival,
            command.seats,
        ).let { (commute, events) ->
            Triple(events, commute.lastRevision, commute.lastEtag)
        }

    private suspend fun handle(command: BookCommuteCommand): Triple<List<CommuteEvent>, Int, String> =
        commuteRepository
            .findById(command.commuteId)
            .let { commute ->
                commute
                    .bookSeat(command.bookingId, command.seat)
                    .let {
                        Triple(it, commute.lastRevision, commute.lastEtag!!)
                    }
            }

    private suspend fun handle(command: CancelCommuteBookingCommand): Triple<List<CommuteEvent>, Int, String> =
        commuteRepository
            .findById(command.commuteId)
            .let { commute ->
                commute
                    .cancelBookedSeat(command.bookingId)
                    .let {
                        Triple(it, commute.lastRevision, commute.lastEtag!!)
                    }
            }

    private suspend fun handle(command: ExpireCommuteCommand): Triple<List<CommuteEvent>, Int, String> =
        commuteRepository
            .findById(command.commuteId)
            .let { commute ->
                commute
                    .expire()
                    .let {
                        Triple(it, commute.lastRevision, commute.lastEtag!!)
                    }
            }

    suspend fun compensate(event: CommuteEvent): CommuteEvent =
        withRetry(maxRetries) {
            when (event) {
                is CommuteBookedEvent -> compensate(event)
                is CommuteBookingCanceledEvent -> compensate(event)
                else -> throw IllegalArgumentException("Unknown event type: ${event::class.simpleName}")
            }.let {
                it.copy(first = listOf(it.first.first().toCompensation()) + it.first.drop(1))
            }.let {
                it.first.map { event -> event.correlationId = event.correlationId }
                EventBus.publish(it.first, it.second.toLong(), it.third)
                it.first.first()
            }
        }

    private suspend fun compensate(event: CommuteBookedEvent): Triple<List<CommuteEvent>, Int, String> =
        commuteRepository
            .findById(event.commuteId)
            .let { commute ->
                commute
                    .compensateBookSeat(event.bookingId)
                    .let {
                        Triple(it, commute.lastRevision, commute.lastEtag!!)
                    }
            }

    private suspend fun compensate(event: CommuteBookingCanceledEvent): Triple<List<CommuteEvent>, Int, String> =
        commuteRepository
            .findById(event.commuteId)
            .let { commute ->
                commute
                    .compensateCancelBookedSeat(event.bookingId, event.seat)
                    .let {
                        Triple(it, commute.lastRevision, commute.lastEtag!!)
                    }
            }
}
