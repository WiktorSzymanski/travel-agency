package pl.szymanski.wiktor.ta.commands.accommodation.compensateCancelBooking

import pl.szymanski.wiktor.ta.Metadata
import pl.szymanski.wiktor.ta.commands.CommandHandler
import pl.szymanski.wiktor.ta.outbox.OutboxPort
import pl.szymanski.wiktor.ta.repository.AccommodationRepository

class CompensateCancelAccommodationBookingCommandHandler(
    private val accommodationRepository: AccommodationRepository,
    private val outboxPort: OutboxPort
) : CommandHandler<CompensateCancelAccommodationBookingCommand> {
    override suspend fun handle(command: CompensateCancelAccommodationBookingCommand) {
        accommodationRepository
            .findById(command.accommodationId)
            .let { (accommodation, version) ->
                val events = accommodation.compensateCancelBooking(command.bookingId)

                outboxPort.save(
                    accommodation,
                    events,
                    Metadata(command.correlationId, version + 1),
                    accommodationRepository
                )
            }
    }
}