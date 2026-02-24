package pl.szymanski.wiktor.ta.commands.attraction.compensateBook

import pl.szymanski.wiktor.ta.commands.Command
import pl.szymanski.wiktor.ta.domain.aggregate.AttractionId
import pl.szymanski.wiktor.ta.domain.aggregate.BookingId
import java.util.UUID

data class CompensateBookAttractionCommand(
    val attractionId: AttractionId,
    override val correlationId: UUID,
    val eventId: UUID,
    val bookingId: BookingId,
) : Command
