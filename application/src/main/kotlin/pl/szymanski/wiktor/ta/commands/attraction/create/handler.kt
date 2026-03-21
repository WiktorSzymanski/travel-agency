package pl.szymanski.wiktor.ta.commands.attraction.create

import pl.szymanski.wiktor.ta.Metadata
import pl.szymanski.wiktor.ta.commands.CommandHandler
import pl.szymanski.wiktor.ta.domain.aggregate.Attraction
import pl.szymanski.wiktor.ta.outbox.OutboxPort
import pl.szymanski.wiktor.ta.repository.CommandRepository
import pl.szymanski.wiktor.ta.domain.aggregate.AttractionId

class CreateAttractionCommandHandler(
    private val attractionRepository: CommandRepository<Attraction, AttractionId>,
    private val outboxPort: OutboxPort
) : CommandHandler<CreateAttractionCommand> {
    override suspend fun handle(command: CreateAttractionCommand) {
        Attraction.create(
            command.attractionId,
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

