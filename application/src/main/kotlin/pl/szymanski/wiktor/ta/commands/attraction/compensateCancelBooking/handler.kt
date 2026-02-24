package pl.szymanski.wiktor.ta.commands.attraction.compensateCancelBooking

import pl.szymanski.wiktor.ta.Metadata
import pl.szymanski.wiktor.ta.commands.CommandHandler
import pl.szymanski.wiktor.ta.outbox.OutboxPort
import pl.szymanski.wiktor.ta.repository.AttractionRepository

class CompensateCancelAttractionBookingCommandHandler(
    private val attractionRepository: AttractionRepository,
    private val outboxPort: OutboxPort
) : CommandHandler<CompensateCancelAttractionBookingCommand> {
    override suspend fun handle(command: CompensateCancelAttractionBookingCommand) {
        attractionRepository
            .findById(command.attractionId)
            .let { (attraction, version) ->
                val events = attraction.compensateCancelBooking(command.bookingId)
                outboxPort.save(attraction, events, Metadata(command.correlationId, version + 1), attractionRepository)
            }
    }
}

