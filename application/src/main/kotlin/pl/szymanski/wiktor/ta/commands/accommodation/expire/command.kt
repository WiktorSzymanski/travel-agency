package pl.szymanski.wiktor.ta.commands.accommodation.expire

import pl.szymanski.wiktor.ta.commands.Command
import pl.szymanski.wiktor.ta.domain.aggregate.AccommodationId
import java.util.UUID

data class ExpireAccommodationCommand(
    val accommodationId: AccommodationId,
    override val correlationId: UUID,
) : Command
