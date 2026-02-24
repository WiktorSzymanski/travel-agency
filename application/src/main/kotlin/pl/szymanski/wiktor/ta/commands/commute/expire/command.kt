package pl.szymanski.wiktor.ta.commands.commute.expire

import pl.szymanski.wiktor.ta.commands.Command
import pl.szymanski.wiktor.ta.domain.aggregate.CommuteId
import java.util.UUID

data class ExpireCommuteCommand(
    override val correlationId: UUID,
    val commuteId: CommuteId,
) : Command
