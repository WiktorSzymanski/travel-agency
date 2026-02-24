package pl.szymanski.wiktor.ta.commands.attraction.cancelBooking

import pl.szymanski.wiktor.ta.commands.Command
import pl.szymanski.wiktor.ta.domain.aggregate.AttractionId
import pl.szymanski.wiktor.ta.domain.aggregate.BookingId
import java.util.UUID

data class CancelAttractionBookingCommand(
    val attractionId: AttractionId,
    override val correlationId: UUID,
    val bookingId: BookingId,
) : Command
