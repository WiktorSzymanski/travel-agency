package pl.szymanski.wiktor.ta.commands.booking.processCancel

import pl.szymanski.wiktor.ta.Metadata
import pl.szymanski.wiktor.ta.commands.CommandHandler
import pl.szymanski.wiktor.ta.outbox.OutboxPort
import pl.szymanski.wiktor.ta.repository.BookingRepository

class ProcessCancelBookingCommandHandler(
    private val bookingRepository: BookingRepository,
    private val outboxPort: OutboxPort
) : CommandHandler<ProcessCancelBookingCommand> {
    override suspend fun handle(command: ProcessCancelBookingCommand) {
        bookingRepository
            .findById(command.bookingId)
            .let { (booking, version) ->
                val events = booking.processCancellation()
                outboxPort.save(booking, events, Metadata(command.correlationId, version + 1), bookingRepository)
            }
    }
}

