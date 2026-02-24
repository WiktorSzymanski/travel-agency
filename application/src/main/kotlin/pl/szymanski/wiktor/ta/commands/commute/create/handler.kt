package pl.szymanski.wiktor.ta.commands.commute.create

import pl.szymanski.wiktor.ta.Metadata
import pl.szymanski.wiktor.ta.commands.CommandHandler
import pl.szymanski.wiktor.ta.domain.aggregate.Commute
import pl.szymanski.wiktor.ta.event.CommuteDateMetEvent
import pl.szymanski.wiktor.ta.outbox.OutboxPort
import pl.szymanski.wiktor.ta.repository.CommuteRepository

class CreateCommuteCommandHandler(
    private val commuteRepository: CommuteRepository,
    private val outboxPort: OutboxPort
) : CommandHandler<CreateCommuteCommand> {
    override suspend fun handle(command: CreateCommuteCommand) {
        Commute.create(
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

