package pl.szymanski.wiktor.ta.commands.commute.book

import pl.szymanski.wiktor.ta.Metadata
import pl.szymanski.wiktor.ta.commands.CommandHandler
import pl.szymanski.wiktor.ta.outbox.OutboxPort
import pl.szymanski.wiktor.ta.repository.CommandRepository
import pl.szymanski.wiktor.ta.domain.aggregate.Commute
import pl.szymanski.wiktor.ta.domain.aggregate.CommuteId

class BookCommuteCommandHandler(
    private val commuteRepository: CommandRepository<Commute, CommuteId>,
    private val outboxPort: OutboxPort
) : CommandHandler<BookCommuteCommand> {
    override suspend fun handle(command: BookCommuteCommand) {
        commuteRepository
            .findById(command.commuteId)
            .let { (commute, version) ->
                val events = commute.bookSeat(command.bookingId, command.seat)
                outboxPort.save(commute, events, Metadata(command.correlationId, version + 1), commuteRepository)
            }
    }
}

