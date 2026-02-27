package pl.szymanski.wiktor.ta.commands.commute.create

import pl.szymanski.wiktor.ta.Metadata
import pl.szymanski.wiktor.ta.commands.CommandHandler
import pl.szymanski.wiktor.ta.domain.aggregate.Commute
import pl.szymanski.wiktor.ta.outbox.OutboxPort
import pl.szymanski.wiktor.ta.repository.CommandRepository
import pl.szymanski.wiktor.ta.domain.aggregate.CommuteId

class CreateCommuteCommandHandler(
    private val commuteRepository: CommandRepository<Commute, CommuteId>,
    private val outboxPort: OutboxPort
) : CommandHandler<CreateCommuteCommand> {
    override suspend fun handle(command: CreateCommuteCommand) {
        Commute.create(
            command.commuteId,
            command.name,
            command.departure,
            command.arrival,
            command.seats,
        ).let { (commute, event) ->
            outboxPort.create(
                commute,
                event,
                Metadata(command.correlationId,0),
                commuteRepository
            )
        }
    }
}

