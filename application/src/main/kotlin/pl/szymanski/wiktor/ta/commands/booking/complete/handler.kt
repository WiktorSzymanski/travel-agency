package pl.szymanski.wiktor.ta.commands.booking.complete

import pl.szymanski.wiktor.ta.Metadata
import pl.szymanski.wiktor.ta.commands.CommandHandler
import pl.szymanski.wiktor.ta.outbox.OutboxPort
import pl.szymanski.wiktor.ta.repository.BookingRepository

class CompleteBookingCommandHandler(
    private val bookingRepository: BookingRepository,
    private val outboxPort: OutboxPort
) : CommandHandler<CompleteBookingCommand> {
    override suspend fun handle(command: CompleteBookingCommand) {
        bookingRepository
            .findById(command.bookingId)
            .let { (booking, version) ->
                val events = booking.complete()
                outboxPort.save(booking, events, Metadata(command.correlationId, version + 1), bookingRepository)
            }
    }
}

