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
import pl.szymanski.wiktor.ta.domain.repository.AttractionRepository
import pl.szymanski.wiktor.ta.event.toCompensation
import pl.szymanski.wiktor.ta.withRetry

class AttractionCommandHandler(
    private val attractionRepository: AttractionRepository,
) {
    val maxRetries = 1

    suspend fun handle(command: AttractionCommand): AttractionEvent =
        withRetry (maxRetries) {
            when (command) {
                is BookAttractionCommand -> handle(command)
                is CancelAttractionBookingCommand -> handle(command)
                is CreateAttractionCommand -> handle(command)
                is ExpireAttractionCommand -> handle(command)
            }.let {
                it.first.map { event -> event.correlationId = command.correlationId }
                EventBus.publish(it.first, it.second.toLong(), it.third)
                it.first.first()
            }
        }

    private suspend fun handle(command: CreateAttractionCommand): Triple<List<AttractionEvent>, Int, String?> =
        Attraction.create(
            command.name,
            command.location,
            command.date,
            command.capacity,
        ).let { (attraction, events) ->
            Triple(events, attraction.lastRevision, attraction.lastEtag)
        }

    private suspend fun handle(command: BookAttractionCommand): Triple<List<AttractionEvent>, Int, String> =
        attractionRepository
            .findById(command.attractionId)
            .let { attraction ->
                attraction
                    .book(command.bookingId)
                    .let {
                        Triple(it, attraction.lastRevision, attraction.lastEtag!!)
                    }
            }

    private suspend fun handle(command: CancelAttractionBookingCommand): Triple<List<AttractionEvent>, Int, String> =
        attractionRepository
            .findById(command.attractionId)
            .let { attraction ->
                attraction
                    .cancelBooking(command.bookingId)
                    .let {
                        Triple(it, attraction.lastRevision, attraction.lastEtag!!)
                    }
            }

    private suspend fun handle(command: ExpireAttractionCommand): Triple<List<AttractionEvent>, Int, String> =
        attractionRepository
            .findById(command.attractionId)
            .let { attraction ->
                attraction
                    .expire()
                    .let {
                        Triple(it, attraction.lastRevision, attraction.lastEtag!!)
                    }
            }

    suspend fun compensate(event: AttractionEvent): AttractionEvent =
        withRetry(maxRetries) {
            when (event) {
                is AttractionBookedEvent -> compensate(event)
                is AttractionBookingCanceledEvent -> compensate(event)
                else -> throw IllegalArgumentException("Unknown event type: ${event::class.simpleName}")
            }.let {
                it.copy(first = listOf(it.first.first().toCompensation()) + it.first.drop(1))
            }.let {
                it.first.map { event -> event.correlationId = event.correlationId }
                EventBus.publish(it.first, it.second.toLong(), it.third)
                it.first.first()
            }
        }

    private suspend fun compensate(event: AttractionBookedEvent): Triple<List<AttractionEvent>, Int, String> =
        attractionRepository
            .findById(event.attractionId)
            .let { attraction ->
                attraction
                    .compensateBook(event.bookingId)
                    .let {
                        Triple(it, attraction.lastRevision, attraction.lastEtag!!)
                    }
            }

    private suspend fun compensate(event: AttractionBookingCanceledEvent): Triple<List<AttractionEvent>, Int, String> =
        attractionRepository
            .findById(event.attractionId)
            .let { attraction ->
                attraction
                    .compensateCancelBooking(event.bookingId)
                    .let {
                        Triple(it, attraction.lastRevision, attraction.lastEtag!!)
                    }
            }
}
