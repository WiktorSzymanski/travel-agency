package pl.szymanski.wiktor.ta.command

import pl.szymanski.wiktor.ta.domain.LocationEnum
import pl.szymanski.wiktor.ta.domain.aggregate.AttractionId
import pl.szymanski.wiktor.ta.domain.aggregate.BookingId
import java.time.LocalDateTime
import java.util.UUID

sealed class AttractionCommand : Command {
    abstract val attractionId: AttractionId
}

data class BookAttractionCommand(
    override val attractionId: AttractionId,
    override val correlationId: UUID,
    val bookingId: BookingId,
) : AttractionCommand()

data class CancelAttractionBookingCommand(
    override val attractionId: AttractionId,
    override val correlationId: UUID,
    val bookingId: BookingId,
) : AttractionCommand()

data class CreateAttractionCommand(
    override val attractionId: AttractionId,
    override val correlationId: UUID,
    val name: String,
    val location: LocationEnum,
    val date: LocalDateTime,
    val capacity: Int,
) : AttractionCommand()

data class ExpireAttractionCommand(
    override val attractionId: AttractionId,
    override val correlationId: UUID,
) : AttractionCommand()

sealed class CompensateAttractionCommand : AttractionCommand() {
    abstract val eventId: UUID
}

data class CompensateBookAttractionCommand(
    override val attractionId: AttractionId,
    override val correlationId: UUID,
    override val eventId: UUID,
    val bookingId: BookingId,
) : CompensateAttractionCommand()

data class CompensateCancelAttractionBookingCommand(
    override val attractionId: AttractionId,
    override val correlationId: UUID,
    override val eventId: UUID,
    val bookingId: BookingId,
) : CompensateAttractionCommand()
