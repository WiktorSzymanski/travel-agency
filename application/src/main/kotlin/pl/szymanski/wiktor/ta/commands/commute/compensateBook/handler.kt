package pl.szymanski.wiktor.ta.commands.commute.compensateBook

import pl.szymanski.wiktor.ta.Metadata
import pl.szymanski.wiktor.ta.commands.CommandHandler
import pl.szymanski.wiktor.ta.outbox.OutboxPort
import pl.szymanski.wiktor.ta.repository.CommandRepository
import pl.szymanski.wiktor.ta.domain.aggregate.Commute
import pl.szymanski.wiktor.ta.domain.aggregate.CommuteId

class CompensateBookCommuteCommandHandler(
    private val commuteRepository: CommandRepository<Commute, CommuteId>,
    private val outboxPort: OutboxPort
) : CommandHandler<CompensateBookCommuteCommand> {
    override suspend fun handle(command: CompensateBookCommuteCommand) {
        commuteRepository
            .findById(command.commuteId)
            .let { (commute, version) ->
                val events = commute.compensateBookSeat(command.bookingId)
                outboxPort.save(commute, events, Metadata(command.correlationId, version + 1), commuteRepository)
            }
    }
}

