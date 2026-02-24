package pl.szymanski.wiktor.ta.commands.attraction.expire

import pl.szymanski.wiktor.ta.commands.Command
import pl.szymanski.wiktor.ta.domain.aggregate.AttractionId
import java.util.UUID

data class ExpireAttractionCommand(
    val attractionId: AttractionId,
    override val correlationId: UUID,
) : Command
