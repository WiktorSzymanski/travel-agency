package pl.szymanski.wiktor.ta.command

import pl.szymanski.wiktor.ta.domain.LocationEnum
import java.time.LocalDateTime
import java.util.UUID

sealed class AttractionCommand : Command {
    abstract val attractionId: UUID
}

data class BookAttractionCommand(
    override val attractionId: UUID,
    override val correlationId: UUID,
    val bookingId: UUID,
) : AttractionCommand()

data class CancelAttractionBookingCommand(
    override val attractionId: UUID,
    override val correlationId: UUID,
    val bookingId: UUID,
) : AttractionCommand()

data class CreateAttractionCommand(
    override val attractionId: UUID,
    override val correlationId: UUID,
    val name: String,
    val location: LocationEnum,
    val date: LocalDateTime,
    val capacity: Int,
) : AttractionCommand()

data class ExpireAttractionCommand(
    override val attractionId: UUID,
    override val correlationId: UUID,
) : AttractionCommand()

sealed class CompensateAttractionCommand : AttractionCommand() {
    abstract val eventId: UUID
}

data class CompensateBookAttractionCommand(
    override val attractionId: UUID,
    override val correlationId: UUID,
    override val eventId: UUID,
    val bookingId: UUID,
) : CompensateAttractionCommand()

data class CompensateCancelAttractionBookingCommand(
    override val attractionId: UUID,
    override val correlationId: UUID,
    override val eventId: UUID,
    val bookingId: UUID,
) : CompensateAttractionCommand()
