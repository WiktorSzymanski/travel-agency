package pl.szymanski.wiktor.ta.commands.accommodation.cancelBooking

import pl.szymanski.wiktor.ta.Metadata
import pl.szymanski.wiktor.ta.commands.CommandHandler
import pl.szymanski.wiktor.ta.outbox.OutboxPort
import pl.szymanski.wiktor.ta.repository.AccommodationRepository

class CancelAccommodationBookingCommandHandler (
    private val accommodationRepository: AccommodationRepository,
    private val outboxPort: OutboxPort
) : CommandHandler<CancelAccommodationBookingCommand> {
    override suspend fun handle(command: CancelAccommodationBookingCommand) {
        accommodationRepository
            .findById(command.accommodationId)
            .let { (accommodation, version) ->
                val events = accommodation.cancelBooking(command.bookingId)

                outboxPort.save(
                    accommodation,
                    events,
                    Metadata(command.correlationId, version + 1),
                    accommodationRepository
                )
            }
        }
}