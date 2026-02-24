package pl.szymanski.wiktor.ta.commands.commute.expire

import pl.szymanski.wiktor.ta.Metadata
import pl.szymanski.wiktor.ta.commands.CommandHandler
import pl.szymanski.wiktor.ta.outbox.OutboxPort
import pl.szymanski.wiktor.ta.repository.CommuteRepository

class ExpireCommuteCommandHandler(
    private val commuteRepository: CommuteRepository,
    private val outboxPort: OutboxPort
) : CommandHandler<ExpireCommuteCommand> {
    override suspend fun handle(command: ExpireCommuteCommand) {
        commuteRepository
            .findById(command.commuteId)
            .let { (commute, version) ->
                val events = commute.expire()
                outboxPort.save(commute, events, Metadata(command.correlationId, version + 1), commuteRepository)
            }
    }
}

