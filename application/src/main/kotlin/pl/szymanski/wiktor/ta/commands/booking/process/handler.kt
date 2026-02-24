package pl.szymanski.wiktor.ta.commands.booking.process

import pl.szymanski.wiktor.ta.Metadata
import pl.szymanski.wiktor.ta.commands.CommandHandler
import pl.szymanski.wiktor.ta.outbox.OutboxPort
import pl.szymanski.wiktor.ta.repository.BookingRepository

class ProcessBookingCommandHandler(
    private val bookingRepository: BookingRepository,
    private val outboxPort: OutboxPort
) : CommandHandler<ProcessBookingCommand> {
    override suspend fun handle(command: ProcessBookingCommand) {
        bookingRepository
            .findById(command.bookingId)
            .let { (booking, version) ->
                val events = booking.process()
                outboxPort.save(booking, events, Metadata(command.correlationId, version + 1), bookingRepository)
            }
    }
}

