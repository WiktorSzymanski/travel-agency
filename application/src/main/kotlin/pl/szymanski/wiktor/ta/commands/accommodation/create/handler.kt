package pl.szymanski.wiktor.ta.commands.accommodation.create

import pl.szymanski.wiktor.ta.Metadata
import pl.szymanski.wiktor.ta.commands.CommandHandler
import pl.szymanski.wiktor.ta.domain.aggregate.Accommodation
import pl.szymanski.wiktor.ta.domain.aggregate.AccommodationId
import pl.szymanski.wiktor.ta.outbox.OutboxPort
import pl.szymanski.wiktor.ta.repository.CommandRepository

class CreateAccommodationCommandHandler(
    private val accommodationRepository: CommandRepository<Accommodation, AccommodationId>,
    private val outboxPort: OutboxPort
) : CommandHandler<CreateAccommodationCommand> {
    override suspend fun handle(command: CreateAccommodationCommand) {
        Accommodation.create(
            command.accommodationId,
            command.name,
            command.location,
            command.rent,
        ).let { (accommodation, event) ->
            outboxPort.create(
                accommodation,
                event,
                Metadata(command.correlationId, 0),
                accommodationRepository
            )
        }
    }
}