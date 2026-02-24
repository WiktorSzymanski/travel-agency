package pl.szymanski.wiktor.ta.commands.attraction.book

import pl.szymanski.wiktor.ta.commands.Command
import pl.szymanski.wiktor.ta.domain.aggregate.AttractionId
import pl.szymanski.wiktor.ta.domain.aggregate.BookingId
import java.util.UUID

data class BookAttractionCommand(
    val attractionId: AttractionId,
    override val correlationId: UUID,
    val bookingId: BookingId,
) : Command
