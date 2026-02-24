package pl.szymanski.wiktor.ta.commands.accommodation.create

import pl.szymanski.wiktor.ta.Metadata
import pl.szymanski.wiktor.ta.commands.CommandHandler
import pl.szymanski.wiktor.ta.domain.aggregate.Accommodation
import pl.szymanski.wiktor.ta.event.AccommodationDateMetEvent
import pl.szymanski.wiktor.ta.outbox.OutboxPort
import pl.szymanski.wiktor.ta.repository.AccommodationRepository

class CreateAccommodationCommandHandler(
    private val accommodationRepository: AccommodationRepository,
    private val outboxPort: OutboxPort
) : CommandHandler<CreateAccommodationCommand> {
    override suspend fun handle(command: CreateAccommodationCommand) {
        Accommodation.create(
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