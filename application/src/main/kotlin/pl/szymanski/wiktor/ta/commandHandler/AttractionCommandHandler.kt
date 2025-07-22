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

class AttractionCommandHandler(
    private val attractionRepository: AttractionRepository,
) {
    suspend fun handle(command: AttractionCommand): AttractionEvent =
        when (command) {
            is BookAttractionCommand -> handle(command)
            is CancelAttractionBookingCommand -> handle(command)
            is CreateAttractionCommand -> handle(command)
            is ExpireAttractionCommand -> handle(command)
        }.apply { correlationId = command.correlationId }
            .also { EventBus.publish(it) }

    private suspend fun handle(command: BookAttractionCommand): AttractionEvent =
        attractionRepository
            .findById(command.attractionId)
            .let { attraction ->
                attraction
                    .book(command.userId)
                    .also { attractionRepository.update(attraction) }
            }.apply { correlationId = command.correlationId }

    private suspend fun handle(command: CancelAttractionBookingCommand): AttractionEvent =
        attractionRepository
            .findById(command.attractionId)
            .let { attraction ->
                attraction
                    .cancelBooking(command.userId)
                    .also { attractionRepository.update(attraction) }
            }.apply { correlationId = command.correlationId }

    private suspend fun handle(command: CreateAttractionCommand): AttractionEvent =
        Attraction.Companion.create(
            command.name,
            command.location,
            command.date,
            command.capacity,
        ).let { (attraction, event) ->
            attractionRepository.save(attraction)
            event
        }

    private suspend fun handle(command: ExpireAttractionCommand): AttractionEvent =
        attractionRepository
            .findById(command.attractionId)
            .let { attraction ->
                attraction
                    .expire()
                    .also { attractionRepository.update(attraction) }
            }.apply { correlationId = command.correlationId }

    suspend fun compensate(event: AttractionEvent): AttractionEvent =
        when (event) {
            is AttractionBookedEvent -> compensate(event)
            is AttractionBookingCanceledEvent -> compensate(event)
            else -> throw IllegalArgumentException("Unknown event type: ${event::class.simpleName}")
        }.apply { correlationId = event.correlationId }.toCompensation().also { EventBus.publish(it) }

    private suspend fun compensate(event: AttractionBookedEvent): AttractionEvent =
        handle(
            CancelAttractionBookingCommand(
                event.attractionId,
                event.correlationId!!,
                event.userId,
            ),
        )

    private suspend fun compensate(event: AttractionBookingCanceledEvent): AttractionEvent =
        handle(
            BookAttractionCommand(
                event.attractionId,
                event.correlationId!!,
                event.userId,
            ),
        )
}
