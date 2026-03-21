package pl.szymanski.wiktor.ta.commands.booking.cancel

import pl.szymanski.wiktor.ta.Metadata
import pl.szymanski.wiktor.ta.commands.CommandHandler
import pl.szymanski.wiktor.ta.outbox.OutboxPort
import pl.szymanski.wiktor.ta.repository.CommandRepository
import pl.szymanski.wiktor.ta.domain.aggregate.Booking
import pl.szymanski.wiktor.ta.domain.aggregate.BookingId

class CancelBookingCommandHandler(
    private val bookingRepository: CommandRepository<Booking, BookingId>,
    private val outboxPort: OutboxPort
) : CommandHandler<CancelBookingCommand> {
    override suspend fun handle(command: CancelBookingCommand) {
        bookingRepository
            .findById(command.bookingId)
            .let { (booking, version) ->
                val events = booking.cancel()
                outboxPort.save(booking, events, Metadata(command.correlationId, version), bookingRepository)
            }
    }
}

