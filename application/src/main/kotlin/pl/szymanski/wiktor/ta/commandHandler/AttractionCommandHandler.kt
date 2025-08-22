package pl.szymanski.wiktor.ta.commandHandler

import pl.szymanski.wiktor.ta.EventBus
import pl.szymanski.wiktor.ta.command.AttractionCommand
import pl.szymanski.wiktor.ta.command.BookAttractionCommand
import pl.szymanski.wiktor.ta.command.CancelAttractionBookingCommand
import pl.szymanski.wiktor.ta.command.CreateAttractionCommand
import pl.szymanski.wiktor.ta.command.ExpireAttractionCommand
import pl.szymanski.wiktor.ta.domain.aggregate.Attraction
import pl.szymanski.wiktor.ta.domain.event.AttractionBookedEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionBookingCanceledEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionEvent
import pl.szymanski.wiktor.ta.domain.event.FailedEvent
import pl.szymanski.wiktor.ta.domain.repository.AttractionRepository
import pl.szymanski.wiktor.ta.event.toCompensation
import pl.szymanski.wiktor.ta.withRetry

class AttractionCommandHandler(
    private val attractionRepository: AttractionRepository,
) {
    val maxRetries = 30

    suspend fun handle(command: AttractionCommand): AttractionEvent =
        withRetry (maxRetries) {
            when (command) {
                is BookAttractionCommand -> handle(command)
                is CancelAttractionBookingCommand -> handle(command)
                is CreateAttractionCommand -> handle(command)
                is ExpireAttractionCommand -> handle(command)
            }.map {
                it.first.correlationId = command.correlationId
                EventBus.publish(it.first, it.second)
                it
            }.let { it[0].first }
        }

    private suspend fun handle(command: BookAttractionCommand): List<Pair<AttractionEvent, Int>> =
        attractionRepository
            .findById(command.attractionId)
            .let { attraction ->
                attraction
                    .book(command.bookingId)
                    .mapIndexed { index, event -> event to attraction.lastRevision + index }
            }

    private suspend fun handle(command: CancelAttractionBookingCommand): List<Pair<AttractionEvent, Int>> =
        attractionRepository
            .findById(command.attractionId)
            .let { attraction ->
                attraction
                    .cancelBooking(command.bookingId)
                    .mapIndexed { index, event -> event to attraction.lastRevision + index }
            }

    private suspend fun handle(command: CreateAttractionCommand): List<Pair<AttractionEvent, Int>> =
        Attraction.create(
            command.name,
            command.location,
            command.date,
            command.capacity,
        ).let { (attraction, event) ->
            listOf(event[0] to attraction.lastRevision)
        }

    private suspend fun handle(command: ExpireAttractionCommand): List<Pair<AttractionEvent, Int>> =
        attractionRepository
            .findById(command.attractionId)
            .let { attraction ->
                attraction
                    .expire()
                    .mapIndexed { index, event -> event to attraction.lastRevision + index }
            }

    suspend fun compensate(event: AttractionEvent): AttractionEvent =
        withRetry(maxRetries) {
            when (event) {
                is AttractionBookedEvent -> compensate(event)
                is AttractionBookingCanceledEvent -> compensate(event)
                else -> throw IllegalArgumentException("Unknown event type: ${event::class.simpleName}")
            }.let {
                it[0].first.toCompensation()
                it
            }.map {
                it.first.correlationId = event.correlationId
                if (it.first !is FailedEvent) EventBus.publish(it.first, it.second)
                it
            }.let { it[0].first }
        }

    private suspend fun compensate(event: AttractionBookedEvent): List<Pair<AttractionEvent, Int>> =
        attractionRepository
            .findById(event.attractionId)
            .let { attraction ->
                attraction
                    .compensateBook(event.bookingId)
                    .mapIndexed { index, event -> event to attraction.lastRevision + index }
            }

    private suspend fun compensate(event: AttractionBookingCanceledEvent): List<Pair<AttractionEvent, Int>> =
        attractionRepository
            .findById(event.attractionId)
            .let { attraction ->
                attraction
                    .compensateCancelBooking(event.bookingId)
                    .mapIndexed { index, event -> event to attraction.lastRevision + index }
            }
}
