package pl.szymanski.wiktor.ta.commands.booking.requestCancel

import pl.szymanski.wiktor.ta.Metadata
import pl.szymanski.wiktor.ta.commands.CommandHandler
import pl.szymanski.wiktor.ta.outbox.OutboxPort
import pl.szymanski.wiktor.ta.repository.CommandRepository
import pl.szymanski.wiktor.ta.domain.aggregate.Booking
import pl.szymanski.wiktor.ta.domain.aggregate.BookingId

class BookingRequestCancelCommandHandler(
    private val bookingRepository: CommandRepository<Booking, BookingId>,
    private val outboxPort: OutboxPort
) : CommandHandler<BookingRequestCancelCommand> {
    override suspend fun handle(command: BookingRequestCancelCommand) {
        bookingRepository
            .findById(command.bookingId)
            .let { (booking, version) ->
                val events = booking.requestCancel()
                outboxPort.save(booking, events, Metadata(command.correlationId, version), bookingRepository)
            }
    }
}

