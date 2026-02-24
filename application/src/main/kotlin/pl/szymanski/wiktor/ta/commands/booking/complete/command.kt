package pl.szymanski.wiktor.ta.commands.booking.complete

import pl.szymanski.wiktor.ta.commands.Command
import pl.szymanski.wiktor.ta.domain.aggregate.BookingId
import java.util.UUID

data class CompleteBookingCommand(
    override val correlationId: UUID,
    val bookingId: BookingId,
) : Command
