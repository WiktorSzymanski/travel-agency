package pl.szymanski.wiktor.ta.commands.accommodation.book

import pl.szymanski.wiktor.ta.Metadata
import pl.szymanski.wiktor.ta.commands.CommandHandler
import pl.szymanski.wiktor.ta.outbox.OutboxPort
import pl.szymanski.wiktor.ta.repository.AccommodationRepository

class BookAccommodationCommandHandler (
    private val accommodationRepository: AccommodationRepository,
    private val outboxPort: OutboxPort
) : CommandHandler<BookAccommodationCommand> {
    override suspend fun handle(command: BookAccommodationCommand) {
        accommodationRepository
            .findById(command.accommodationId)
            .let { (accommodation, version) ->
                val events = accommodation.book(command.bookingId)

                outboxPort.save(
                    accommodation,
                    events,
                    Metadata(command.correlationId, version + 1),
                    accommodationRepository
                )
            }
        }
}