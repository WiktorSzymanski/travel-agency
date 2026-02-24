package pl.szymanski.wiktor.ta.commands.booking.requestCancel

import pl.szymanski.wiktor.ta.Metadata
import pl.szymanski.wiktor.ta.commands.CommandHandler
import pl.szymanski.wiktor.ta.outbox.OutboxPort
import pl.szymanski.wiktor.ta.repository.BookingRepository

class BookingRequestCancelCommandHandler(
    private val bookingRepository: BookingRepository,
    private val outboxPort: OutboxPort
) : CommandHandler<BookingRequestCancelCommand> {
    override suspend fun handle(command: BookingRequestCancelCommand) {
        bookingRepository
            .findById(command.bookingId)
            .let { (booking, version) ->
                val events = booking.requestCancel()
                outboxPort.save(booking, events, Metadata(command.correlationId, version + 1), bookingRepository)
            }
    }
}

