package pl.szymanski.wiktor.ta.command.attraction

import pl.szymanski.wiktor.ta.command.Command
import pl.szymanski.wiktor.ta.domain.LocationEnum
import java.time.LocalDateTime
import java.util.UUID

sealed interface AttractionCommand : Command {
    val attractionId: UUID
}

data class BookAttractionCommand(
    override val attractionId: UUID,
    override val correlationId: UUID,
    val userId: UUID,
) : AttractionCommand

data class CancelAttractionBookingCommand(
    override val attractionId: UUID,
    override val correlationId: UUID,
    val userId: UUID,
) : AttractionCommand

data class CreateAttractionCommand(
    override val attractionId: UUID,
    override val correlationId: UUID,
    val name: String,
    val location: LocationEnum,
    val date: LocalDateTime,
    val capacity: Int,
) : AttractionCommand

data class ExpireAttractionCommand(
    override val attractionId: UUID,
    override val correlationId: UUID,
) : AttractionCommand

