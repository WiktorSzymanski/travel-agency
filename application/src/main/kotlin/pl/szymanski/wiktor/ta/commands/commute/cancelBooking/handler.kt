package pl.szymanski.wiktor.ta.commands.commute.cancelBooking

import pl.szymanski.wiktor.ta.Metadata
import pl.szymanski.wiktor.ta.commands.CommandHandler
import pl.szymanski.wiktor.ta.outbox.OutboxPort
import pl.szymanski.wiktor.ta.repository.CommuteRepository

class CancelCommuteBookingCommandHandler(
    private val commuteRepository: CommuteRepository,
    private val outboxPort: OutboxPort
) : CommandHandler<CancelCommuteBookingCommand> {
    override suspend fun handle(command: CancelCommuteBookingCommand) {
        commuteRepository
            .findById(command.commuteId)
            .let { (commute, version) ->
                val events = commute.cancelBookedSeat(command.bookingId)
                outboxPort.save(commute, events, Metadata(command.correlationId, version + 1), commuteRepository)
            }
    }
}

