package pl.szymanski.wiktor.ta.commands.accommodation.expire

import pl.szymanski.wiktor.ta.Metadata
import pl.szymanski.wiktor.ta.commands.CommandHandler
import pl.szymanski.wiktor.ta.outbox.OutboxPort
import pl.szymanski.wiktor.ta.repository.AccommodationRepository

class ExpireAccommodationCommandHandler(
    private val accommodationRepository: AccommodationRepository,
    private val outboxPort: OutboxPort
) : CommandHandler<ExpireAccommodationCommand> {
    override suspend fun handle(command: ExpireAccommodationCommand) {
        accommodationRepository
            .findById(command.accommodationId)
            .let { (accommodation, version) ->
                val events = accommodation.expire()

                outboxPort.save(
                    accommodation,
                    events,
                    Metadata(command.correlationId, version + 1),
                    accommodationRepository
                )
            }
    }
}