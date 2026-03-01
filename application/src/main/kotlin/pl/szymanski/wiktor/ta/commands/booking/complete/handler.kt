package pl.szymanski.wiktor.ta.commands.booking.complete

import pl.szymanski.wiktor.ta.Metadata
import pl.szymanski.wiktor.ta.commands.CommandHandler
import pl.szymanski.wiktor.ta.outbox.OutboxPort
import pl.szymanski.wiktor.ta.repository.CommandRepository
import pl.szymanski.wiktor.ta.domain.aggregate.Booking
import pl.szymanski.wiktor.ta.domain.aggregate.BookingId

class CompleteBookingCommandHandler(
    private val bookingRepository: CommandRepository<Booking, BookingId>,
    private val outboxPort: OutboxPort
) : CommandHandler<CompleteBookingCommand> {
    override suspend fun handle(command: CompleteBookingCommand) {
        bookingRepository
            .findById(command.bookingId)
            .let { (booking, version) ->
                val events = booking.complete()
                outboxPort.save(booking, events, Metadata(command.correlationId, version), bookingRepository)
            }
    }
}

