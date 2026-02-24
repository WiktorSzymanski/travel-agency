package pl.szymanski.wiktor.ta.commands.attraction.create

import pl.szymanski.wiktor.ta.Metadata
import pl.szymanski.wiktor.ta.commands.CommandHandler
import pl.szymanski.wiktor.ta.domain.aggregate.Attraction
import pl.szymanski.wiktor.ta.event.AttractionDateMetEvent
import pl.szymanski.wiktor.ta.outbox.OutboxPort
import pl.szymanski.wiktor.ta.repository.AttractionRepository

class CreateAttractionCommandHandler(
    private val attractionRepository: AttractionRepository,
    private val outboxPort: OutboxPort
) : CommandHandler<CreateAttractionCommand> {
    override suspend fun handle(command: CreateAttractionCommand) {
        Attraction.create(
            command.name,
            command.location,
            command.date,
            command.capacity,
        ).let { (attraction, event) ->
            outboxPort.create(
                attraction,
                event,
                Metadata(command.correlationId, 0),
                attractionRepository
            )
        }
    }
}

