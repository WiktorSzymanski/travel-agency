package pl.szymanski.wiktor.ta.commands.accommodation.compensateCancelBooking

import pl.szymanski.wiktor.ta.Metadata
import pl.szymanski.wiktor.ta.commands.CommandHandler
import pl.szymanski.wiktor.ta.outbox.OutboxPort
import pl.szymanski.wiktor.ta.domain.aggregate.Accommodation
import pl.szymanski.wiktor.ta.domain.aggregate.AccommodationId
import pl.szymanski.wiktor.ta.repository.CommandRepository

class CompensateCancelAccommodationBookingCommandHandler(
    private val accommodationRepository: CommandRepository<Accommodation, AccommodationId>,
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
                    Metadata(command.correlationId, version),
                    accommodationRepository
                )
            }
    }
}