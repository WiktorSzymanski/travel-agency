package pl.szymanski.wiktor.ta.command.attraction

import pl.szymanski.wiktor.ta.EventBus
import pl.szymanski.wiktor.ta.domain.event.AttractionBookedEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionBookingCanceledEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionEvent
import pl.szymanski.wiktor.ta.domain.repository.AttractionRepository
import pl.szymanski.wiktor.ta.event.toCompensationEvent

class AttractionCommandHandler(
    private val attractionRepository: AttractionRepository,
) {
    suspend fun handle(command: AttractionCommand) {
        EventBus.publish(
            when (command) {
                is BookAttractionCommand -> handle(command)
                is CancelAttractionBookingCommand -> handle(command)
            }.apply { correlationId = command.correlationId },
        )
    }

    suspend fun compensate(event: AttractionEvent) {
        EventBus.publish(
            when (event) {
                is AttractionBookedEvent -> handle(CancelAttractionBookingCommand(event.attractionId, event.correlationId!!, event.userId))
                is AttractionBookingCanceledEvent -> handle(BookAttractionCommand(event.attractionId, event.correlationId!!, event.userId))
                else -> throw IllegalArgumentException("Unknown event type: ${event::class.simpleName}")
            }.apply { correlationId = event.correlationId }.toCompensationEvent(),
        )
    }

    suspend fun handle(command: BookAttractionCommand): AttractionEvent =
        attractionRepository
            .findById(command.attractionId)
            .let { attraction ->
                attraction
                    .book(command.userId)
                    .also { attractionRepository.update(attraction) }
            }
            .apply { correlationId = command.correlationId }

    suspend fun handle(command: CancelAttractionBookingCommand): AttractionEvent =
        attractionRepository
            .findById(command.attractionId)
            .let { attraction ->
                attraction
                    .cancelBooking(command.userId)
                    .also { attractionRepository.update(attraction) }
            }
            .apply { correlationId = command.correlationId }
}
