package pl.szymanski.wiktor.ta.commands.attraction.create

import pl.szymanski.wiktor.ta.commands.Command
import pl.szymanski.wiktor.ta.domain.LocationEnum
import pl.szymanski.wiktor.ta.domain.aggregate.AttractionId
import java.time.LocalDateTime
import java.util.UUID

data class CreateAttractionCommand(
    val attractionId: AttractionId,
    override val correlationId: UUID,
    val name: String,
    val location: LocationEnum,
    val date: LocalDateTime,
    val capacity: Int,
) : Command
