package pl.szymanski.wiktor.ta.commands.attraction.cancelBooking

import pl.szymanski.wiktor.ta.Metadata
import pl.szymanski.wiktor.ta.commands.CommandHandler
import pl.szymanski.wiktor.ta.outbox.OutboxPort
import pl.szymanski.wiktor.ta.repository.CommandRepository
import pl.szymanski.wiktor.ta.domain.aggregate.Attraction
import pl.szymanski.wiktor.ta.domain.aggregate.AttractionId

class CancelAttractionBookingCommandHandler(
    private val attractionRepository: CommandRepository<Attraction, AttractionId>,
    private val outboxPort: OutboxPort
) : CommandHandler<CancelAttractionBookingCommand> {
    override suspend fun handle(command: CancelAttractionBookingCommand) {
        attractionRepository
            .findById(command.attractionId)
            .let { (attraction, version) ->
                val events = attraction.cancelBooking(command.bookingId)
                outboxPort.save(attraction, events, Metadata(command.correlationId, version + 1), attractionRepository)
            }
    }
}

