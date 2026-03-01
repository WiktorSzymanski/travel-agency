package pl.szymanski.wiktor.ta.commands.accommodation.compensateBook

import pl.szymanski.wiktor.ta.Metadata
import pl.szymanski.wiktor.ta.commands.CommandHandler
import pl.szymanski.wiktor.ta.outbox.OutboxPort
import pl.szymanski.wiktor.ta.domain.aggregate.Accommodation
import pl.szymanski.wiktor.ta.domain.aggregate.AccommodationId
import pl.szymanski.wiktor.ta.repository.CommandRepository

class CompensateBookAccommodationCommandHandler(
    private val accommodationRepository: CommandRepository<Accommodation, AccommodationId>,
    private val outboxPort: OutboxPort
) : CommandHandler<CompensateBookAccommodationCommand> {
    override suspend fun handle(command: CompensateBookAccommodationCommand) {
        accommodationRepository
            .findById(command.accommodationId)
            .let { (accommodation, version) ->
                val events = accommodation.compensateBook(command.bookingId)

                outboxPort.save(
                    accommodation,
                    events,
                    Metadata(command.correlationId, version),
                    accommodationRepository
                )
            }
    }
}