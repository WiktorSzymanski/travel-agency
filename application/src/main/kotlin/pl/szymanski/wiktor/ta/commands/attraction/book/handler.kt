package pl.szymanski.wiktor.ta.commands.attraction.book

import pl.szymanski.wiktor.ta.Metadata
import pl.szymanski.wiktor.ta.commands.CommandHandler
import pl.szymanski.wiktor.ta.outbox.OutboxPort
import pl.szymanski.wiktor.ta.repository.AttractionRepository

class BookAttractionCommandHandler(
    private val attractionRepository: AttractionRepository,
    private val outboxPort: OutboxPort
) : CommandHandler<BookAttractionCommand> {
    override suspend fun handle(command: BookAttractionCommand) {
        attractionRepository
            .findById(command.attractionId)
            .let { (attraction, version) ->
                val events = attraction.book(command.bookingId)
                outboxPort.save(attraction, events, Metadata(command.correlationId, version + 1), attractionRepository)
            }
    }
}

