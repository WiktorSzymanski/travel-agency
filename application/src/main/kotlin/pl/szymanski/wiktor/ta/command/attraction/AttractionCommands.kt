package pl.szymanski.wiktor.ta.command.attraction

import pl.szymanski.wiktor.ta.command.Command
import java.util.UUID

sealed interface AttractionCommand: Command {
    val attractionId: UUID
}

data class BookAttractionCommand(
    override val attractionId: UUID,
    override val correlationId: UUID,
    val userId: UUID
) : AttractionCommand

data class CancelAttractionBookingCommand(
    override val attractionId: UUID,
    override val correlationId: UUID,
    val userId: UUID
) : AttractionCommand