package pl.szymanski.wiktor.ta.commands.booking.create

import pl.szymanski.wiktor.ta.Metadata
import pl.szymanski.wiktor.ta.commands.CommandHandler
import pl.szymanski.wiktor.ta.domain.aggregate.Booking
import pl.szymanski.wiktor.ta.outbox.OutboxPort
import pl.szymanski.wiktor.ta.repository.BookingRepository

class CreateBookingCommandHandler(
    private val bookingRepository: BookingRepository,
    private val outboxPort: OutboxPort
) : CommandHandler<CreateBookingCommand> {
    override suspend fun handle(command: CreateBookingCommand) {
        Booking.create(
            userId = command.userId,
            travelOffer = command.travelOffer,
            seat = command.seat,
        ).let { (booking, event) ->
            outboxPort.create(booking, event, Metadata(command.correlationId, 0), bookingRepository)
        }
    }
}

