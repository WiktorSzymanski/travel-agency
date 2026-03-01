package pl.szymanski.wiktor.ta.commands.commute.expire

import pl.szymanski.wiktor.ta.Metadata
import pl.szymanski.wiktor.ta.commands.CommandHandler
import pl.szymanski.wiktor.ta.outbox.OutboxPort
import pl.szymanski.wiktor.ta.repository.CommandRepository
import pl.szymanski.wiktor.ta.domain.aggregate.Commute
import pl.szymanski.wiktor.ta.domain.aggregate.CommuteId

class ExpireCommuteCommandHandler(
    private val commuteRepository: CommandRepository<Commute, CommuteId>,
    private val outboxPort: OutboxPort
) : CommandHandler<ExpireCommuteCommand> {
    override suspend fun handle(command: ExpireCommuteCommand) {
        commuteRepository
            .findById(command.commuteId)
            .let { (commute, version) ->
                val events = commute.expire()
                outboxPort.save(commute, events, Metadata(command.correlationId, version), commuteRepository)
            }
    }
}

