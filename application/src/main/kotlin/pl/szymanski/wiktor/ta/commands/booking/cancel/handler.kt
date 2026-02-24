package pl.szymanski.wiktor.ta.commands.booking.cancel

import pl.szymanski.wiktor.ta.Metadata
import pl.szymanski.wiktor.ta.commands.CommandHandler
import pl.szymanski.wiktor.ta.outbox.OutboxPort
import pl.szymanski.wiktor.ta.repository.BookingRepository

class CancelBookingCommandHandler(
    private val bookingRepository: BookingRepository,
    private val outboxPort: OutboxPort
) : CommandHandler<CancelBookingCommand> {
    override suspend fun handle(command: CancelBookingCommand) {
        bookingRepository
            .findById(command.bookingId)
            .let { (booking, version) ->
                val events = booking.cancel()
                outboxPort.save(booking, events, Metadata(command.correlationId, version + 1), bookingRepository)
            }
    }
}

