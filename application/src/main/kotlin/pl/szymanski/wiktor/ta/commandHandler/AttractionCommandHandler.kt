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
import pl.szymanski.wiktor.ta.domain.event.AttractionFailedEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteFailedEvent
import pl.szymanski.wiktor.ta.domain.repository.AttractionRepository
import pl.szymanski.wiktor.ta.event.toCompensation
import pl.szymanski.wiktor.ta.withRetry

class AttractionCommandHandler(
    private val attractionRepository: AttractionRepository,
) {
    val maxRetries = 10

    suspend fun handle(command: AttractionCommand): AttractionEvent =
        withRetry (maxRetries) {
            when (command) {
                is BookAttractionCommand -> handle(command)
                is CancelAttractionBookingCommand -> handle(command)
                is CreateAttractionCommand -> handle(command)
                is ExpireAttractionCommand -> handle(command)
            }
        }.map {
            it.correlationId = command.correlationId
            EventBus.publish(it)
            it
        }.let { it[0] }

    private suspend fun handle(command: BookAttractionCommand): List<AttractionEvent> =
        attractionRepository
            .findById(command.attractionId)
            .let { attraction ->
                attraction
                    .book(command.bookingId)
                    .also {
                        if (it[0] !is AttractionFailedEvent)
                            attractionRepository.update(attraction)
                    }
            }

    private suspend fun handle(command: CancelAttractionBookingCommand): List<AttractionEvent> =
        attractionRepository
            .findById(command.attractionId)
            .let { attraction ->
                attraction
                    .cancelBooking(command.bookingId)
                    .also {
                        if (it[0] !is AttractionFailedEvent)
                            attractionRepository.update(attraction)
                    }
            }

    private suspend fun handle(command: CreateAttractionCommand): List<AttractionEvent> =
        Attraction.create(
            command.name,
            command.location,
            command.date,
            command.capacity,
        ).let { (attraction, event) ->
            attractionRepository.save(attraction)
            event
        }

    private suspend fun handle(command: ExpireAttractionCommand): List<AttractionEvent> =
        attractionRepository
            .findById(command.attractionId)
            .let { attraction ->
                attraction
                    .expire()
                    .also {
                        if (it[0] !is AttractionFailedEvent)
                            attractionRepository.update(attraction)
                    }
            }

    suspend fun compensate(event: AttractionEvent): AttractionEvent =
        when (event) {
            is AttractionBookedEvent -> compensate(event)
            is AttractionBookingCanceledEvent -> compensate(event)
            else -> throw IllegalArgumentException("Unknown event type: ${event::class.simpleName}")
        }.let {
            it[0].toCompensation()
            it
        }.map {
            it.correlationId = event.correlationId
            EventBus.publish(it)
            it
        }.let { it[0] }

    private suspend fun compensate(event: AttractionBookedEvent): List<AttractionEvent> =
        attractionRepository
            .findById(event.attractionId)
            .let { attraction ->
                attraction
                    .compensateBook(event.bookingId)
                    .also {
                        if (it[0] !is AttractionFailedEvent)
                            attractionRepository.update(attraction)
                    }
            }

    private suspend fun compensate(event: AttractionBookingCanceledEvent): List<AttractionEvent> =
        attractionRepository
            .findById(event.attractionId)
            .let { attraction ->
                attraction
                    .compensateCancelBooking(event.bookingId)
                    .also {
                        if (it[0] !is AttractionFailedEvent)
                            attractionRepository.update(attraction)
                    }
            }
}
